package com.transproj.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transproj.client.LlmException;
import com.transproj.client.MinerUClient;
import com.transproj.client.MinerUException;
import com.transproj.client.OpenAiCompatibleLlmClient;
import com.transproj.config.AppProperties;
import com.transproj.domain.JobStatus;
import com.transproj.domain.Segment;
import com.transproj.domain.TranslationJob;
import com.transproj.repo.TranslationJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TranslationJobService {

    private static final Logger log = LoggerFactory.getLogger(TranslationJobService.class);

    private final TranslationJobRepository jobRepository;
    private final AppProperties appProperties;
    private final StoragePaths storagePaths;
    private final MinerUClient minerUClient;
    private final OpenAiCompatibleLlmClient llmClient;
    private final ChunkingService chunkingService;
    private final ObjectMapper objectMapper;

    public TranslationJobService(
            TranslationJobRepository jobRepository,
            AppProperties appProperties,
            StoragePaths storagePaths,
            MinerUClient minerUClient,
            OpenAiCompatibleLlmClient llmClient,
            ChunkingService chunkingService,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.appProperties = appProperties;
        this.storagePaths = storagePaths;
        this.minerUClient = minerUClient;
        this.llmClient = llmClient;
        this.chunkingService = chunkingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TranslationJob createJob(String id, String sourceLang, String targetLang, Path savedFile, String originalFilename) {
        TranslationJob job = new TranslationJob();
        job.setId(id);
        job.setSourceLang(sourceLang);
        job.setTargetLang(targetLang);
        job.setOriginalFilename(originalFilename);
        job.setStoredRelativePath(storagePaths.toStoredRelativePath(savedFile));
        job.setStatus(JobStatus.QUEUED);
        job.setProgress(0);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        return job;
    }

    @Async("jobTaskExecutor")
    public void runPipelineAsync(String jobId) {
        TranslationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        try {
            if (appProperties.isMockPipeline()) {
                runMock(job);
                return;
            }

            Path p = storagePaths.resolveStored(job.getStoredRelativePath());
            if (!Files.isRegularFile(p)) {
                fail(job, "FILE_MISSING", "Stored upload not found: " + p);
                return;
            }

            touch(job, JobStatus.PARSING, 10);
            String plain;
            try {
                plain = minerUClient.parseToPlainText(p);
            } catch (MinerUException e) {
                fail(job, e.getErrorCode(), e.getMessage());
                return;
            }

            touch(job, JobStatus.CHUNKING, 35);
            List<String> chunks = chunkingService.splitToChunks(plain, appProperties.getTranslation().getChunkSize());
            if (chunks.isEmpty()) {
                fail(job, "EMPTY_TEXT", "No extractable text from MinerU output");
                return;
            }

            touch(job, JobStatus.TRANSLATING, 40);
            String system = TranslationPrompts.systemPromptMedical(job.getSourceLang(), job.getTargetLang());
            List<Segment> segments = new ArrayList<>();
            int n = chunks.size();
            for (int i = 0; i < n; i++) {
                String srcChunk = chunks.get(i);
                String tgt;
                try {
                    tgt = llmClient.chat(system, srcChunk);
                } catch (LlmException e) {
                    fail(job, e.getErrorCode(), "Chunk " + i + ": " + e.getMessage());
                    return;
                }
                segments.add(new Segment(i, srcChunk, tgt));
                job.setProgress(40 + (int) ((50.0 * (i + 1)) / n));
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
            }

            touch(job, JobStatus.MERGING, 95);
            job.setSegmentsJson(objectMapper.writeValueAsString(segments));
            job.setStatus(JobStatus.DONE);
            job.setProgress(100);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
        } catch (Exception e) {
            log.error("Job {} failed", jobId, e);
            TranslationJob j = jobRepository.findById(jobId).orElse(null);
            if (j != null) {
                fail(j, "INTERNAL", e.getMessage());
            }
        }
    }

    private void runMock(TranslationJob job) throws JsonProcessingException {
        List<Segment> segments = List.of(
                new Segment(0,
                        "（示例原文）Mock pipeline 已启用。请关闭 app.mock-pipeline 并启动 MinerU 与 vLLM。",
                        "（サンプル）モックパイプラインが有効です。app.mock-pipeline を無効にし、MinerU と vLLM を起動してください。",
                        "paragraph")
        );
        job.setSegmentsJson(objectMapper.writeValueAsString(segments));
        job.setStatus(JobStatus.DONE);
        job.setProgress(100);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    private void touch(TranslationJob job, JobStatus status, int progress) {
        job.setStatus(status);
        job.setProgress(progress);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    private void fail(TranslationJob job, String code, String message) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorCode(code);
        job.setErrorMessage(message);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public TranslationJob getJob(String id) {
        return jobRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TranslationJob> listJobsOrderByCreatedDesc() {
        return jobRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<Segment> segmentsFor(TranslationJob job) {
        if (job.getSegmentsJson() == null || job.getSegmentsJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(job.getSegmentsJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
