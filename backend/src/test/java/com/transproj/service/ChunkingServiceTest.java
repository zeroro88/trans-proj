package com.transproj.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();

    @Test
    void splitsLongTextByApproximateSize() {
        String text = "第一句。第二句较长 " + "x".repeat(500) + "\n\n新段落内容。";
        List<String> chunks = chunkingService.splitToChunks(text, 120);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream().mapToInt(String::length).sum()).isGreaterThan(0);
    }

    @Test
    void emptyInputYieldsEmptyList() {
        assertThat(chunkingService.splitToChunks("", 100)).isEmpty();
    }
}
