package com.transproj.web.dto;

import com.transproj.domain.JobStatus;

import java.time.Instant;

public record JobSummaryResponse(
        String id,
        JobStatus status,
        int progress,
        String sourceLang,
        String targetLang,
        String originalFilename,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
