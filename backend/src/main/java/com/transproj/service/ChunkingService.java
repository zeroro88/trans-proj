package com.transproj.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    /**
     * Greedy chunking by character count, preferring breaks at newlines then Chinese full stop.
     */
    public List<String> splitToChunks(String text, int maxChars) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").trim();
        List<String> out = new ArrayList<>();
        if (normalized.isEmpty()) {
            return out;
        }
        int i = 0;
        while (i < normalized.length()) {
            int end = Math.min(i + maxChars, normalized.length());
            if (end < normalized.length()) {
                int sliceEnd = findBetterBreak(normalized, i, end);
                if (sliceEnd > i) {
                    end = sliceEnd;
                }
            }
            String chunk = normalized.substring(i, end).trim();
            if (!chunk.isEmpty()) {
                out.add(chunk);
            }
            i = end;
        }
        return out;
    }

    private static int findBetterBreak(String s, int start, int preferredEnd) {
        int windowStart = Math.max(start, preferredEnd - 200);
        int nl = s.lastIndexOf('\n', preferredEnd - 1);
        if (nl >= windowStart) {
            return nl + 1;
        }
        int stop = -1;
        for (int p = preferredEnd - 1; p >= windowStart; p--) {
            char c = s.charAt(p);
            if (c == '。' || c == '．' || c == '.' || c == '!' || c == '！' || c == '?' || c == '？') {
                stop = p + 1;
                break;
            }
        }
        if (stop > start) {
            return stop;
        }
        return preferredEnd;
    }
}
