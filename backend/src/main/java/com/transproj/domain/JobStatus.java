package com.transproj.domain;

public enum JobStatus {
    QUEUED,
    PARSING,
    CHUNKING,
    TRANSLATING,
    MERGING,
    DONE,
    FAILED
}
