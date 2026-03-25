package com.transproj.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Segment(int index, String source, String target, String blockType) {

    public Segment(int index, String source, String target) {
        this(index, source, target, null);
    }
}
