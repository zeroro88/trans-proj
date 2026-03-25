package com.transproj.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transproj.config.AppProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP adapter for a local MinerU service. Placeholder contract:
 * {@code POST {baseUrl}/v1/parse} multipart field {@code file}, JSON body with {@code markdown}, {@code text}, or {@code blocks[]}.
 */
@Component
public class HttpMinerUClient implements MinerUClient {

    private final AppProperties appProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public HttpMinerUClient(AppProperties appProperties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String parseToPlainText(Path pdfFile) throws MinerUException {
        if (!Files.isRegularFile(pdfFile)) {
            throw new MinerUException("MINERU_BAD_INPUT", "PDF file missing: " + pdfFile);
        }
        String base = appProperties.getMineru().getBaseUrl().replaceAll("/$", "");
        String url = base + "/v1/parse";
        Duration timeout = Duration.ofSeconds(appProperties.getMineru().getTimeoutSeconds());

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(pdfFile.toFile()))
                .contentType(MediaType.APPLICATION_PDF);

        try {
            String body = webClient.post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout);

            if (body == null || body.isBlank()) {
                throw new MinerUException("MINERU_BAD_OUTPUT", "Empty response from MinerU");
            }
            return textFromMinerUJson(body);
        } catch (WebClientResponseException e) {
            throw new MinerUException("MINERU_HTTP_" + e.getStatusCode().value(),
                    e.getResponseBodyAsString(), e);
        } catch (MinerUException e) {
            throw e;
        } catch (Exception e) {
            throw new MinerUException("MINERU_TIMEOUT", "MinerU request failed: " + e.getMessage(), e);
        }
    }

    private String textFromMinerUJson(String json) throws MinerUException {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.hasNonNull("markdown") && root.get("markdown").isTextual()) {
                return root.get("markdown").asText();
            }
            if (root.hasNonNull("text") && root.get("text").isTextual()) {
                return root.get("text").asText();
            }
            if (root.has("blocks") && root.get("blocks").isArray()) {
                List<String> parts = new ArrayList<>();
                for (JsonNode b : root.get("blocks")) {
                    if (b.hasNonNull("text")) {
                        parts.add(b.get("text").asText());
                    } else if (b.isTextual()) {
                        parts.add(b.asText());
                    }
                }
                return String.join("\n\n", parts);
            }
            throw new MinerUException("MINERU_BAD_OUTPUT", "Unrecognized MinerU JSON shape");
        } catch (MinerUException e) {
            throw e;
        } catch (Exception e) {
            throw new MinerUException("MINERU_BAD_OUTPUT", "Invalid JSON from MinerU: " + e.getMessage(), e);
        }
    }
}
