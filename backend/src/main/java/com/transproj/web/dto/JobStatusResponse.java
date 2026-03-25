package com.transproj.web.dto;

import com.transproj.domain.JobStatus;
import com.transproj.domain.Segment;

import java.util.List;

public record JobStatusResponse(
        String id,
        JobStatus status,
        int progress,
        String sourceLang,
        String targetLang,
        String errorCode,
        String errorMessage,
        List<Segment> segments
) {
}
