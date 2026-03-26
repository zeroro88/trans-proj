package com.transproj.web;

import com.transproj.domain.JobStatus;
import com.transproj.domain.TranslationJob;
import com.transproj.service.ExportService;
import com.transproj.service.StoragePaths;
import com.transproj.service.TranslationJobService;
import com.transproj.web.dto.CreateJobResponse;
import com.transproj.web.dto.JobSummaryResponse;
import com.transproj.web.dto.JobStatusResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.transproj.domain.Segment;

import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
@Validated
public class JobController {

    private final TranslationJobService translationJobService;
    private final ExportService exportService;
    private final StoragePaths storagePaths;

    public JobController(TranslationJobService translationJobService, ExportService exportService, StoragePaths storagePaths) {
        this.translationJobService = translationJobService;
        this.exportService = exportService;
        this.storagePaths = storagePaths;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<CreateJobResponse> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceLang") @NotBlank String sourceLang,
            @RequestParam("targetLang") @NotBlank String targetLang) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String id = UUID.randomUUID().toString();
        Files.createDirectories(storagePaths.uploadsDir());
        var target = storagePaths.uploadsDir().resolve(id + ".pdf");
        file.transferTo(target.toFile());

        TranslationJob job = translationJobService.createJob(
                id,
                sourceLang.trim(),
                targetLang.trim(),
                target,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf");
        translationJobService.runPipelineAsync(job.getId());
        return ResponseEntity.ok(new CreateJobResponse(job.getId()));
    }

    @GetMapping
    public ResponseEntity<List<JobSummaryResponse>> list() {
        List<JobSummaryResponse> items = translationJobService.listJobsOrderByCreatedDesc().stream()
                .map(j -> new JobSummaryResponse(
                        j.getId(),
                        j.getStatus(),
                        j.getProgress(),
                        j.getSourceLang(),
                        j.getTargetLang(),
                        j.getOriginalFilename(),
                        j.getErrorCode(),
                        j.getErrorMessage(),
                        j.getCreatedAt(),
                        j.getUpdatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobStatusResponse> status(@PathVariable String id) {
        TranslationJob job = translationJobService.getJob(id);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        List<Segment> segments =
                job.getStatus() == JobStatus.DONE ? translationJobService.segmentsFor(job) : List.of();
        return ResponseEntity.ok(new JobStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getProgress(),
                job.getSourceLang(),
                job.getTargetLang(),
                job.getErrorCode(),
                job.getErrorMessage(),
                segments));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String id,
            @RequestParam(defaultValue = "md") String format) {
        TranslationJob job = translationJobService.getJob(id);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        if (job.getStatus() != JobStatus.DONE) {
            return ResponseEntity.badRequest().build();
        }
        return exportService.export(job, translationJobService.segmentsFor(job), format);
    }
}
