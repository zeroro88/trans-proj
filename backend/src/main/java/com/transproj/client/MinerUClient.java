package com.transproj.client;

import java.nio.file.Path;

/** Calls local MinerU HTTP service: PDF → structured text / markdown (contract TBD). */
public interface MinerUClient {

    /**
     * Sends PDF to MinerU and returns reading-order plain text (or markdown) for downstream chunking.
     */
    String parseToPlainText(Path pdfFile) throws MinerUException;
}
