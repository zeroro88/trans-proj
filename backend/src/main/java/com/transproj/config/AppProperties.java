package com.transproj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Storage storage = new Storage();
    private MinerU mineru = new MinerU();
    private Llm llm = new Llm();
    private Translation translation = new Translation();
    private Deployment deployment = new Deployment();
    /** When true, skips MinerU/vLLM and fills placeholder segments (for UI dev). */
    private boolean mockPipeline = true;

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public MinerU getMineru() {
        return mineru;
    }

    public void setMineru(MinerU mineru) {
        this.mineru = mineru;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public Translation getTranslation() {
        return translation;
    }

    public void setTranslation(Translation translation) {
        this.translation = translation;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }

    public boolean isMockPipeline() {
        return mockPipeline;
    }

    public void setMockPipeline(boolean mockPipeline) {
        this.mockPipeline = mockPipeline;
    }

    public static class Storage {
        /** Root directory for uploads and job data */
        private String baseDir = "./data";

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }
    }

    public static class MinerU {
        /** Base URL of MinerU HTTP service (no trailing slash). */
        private String baseUrl = "http://127.0.0.1:8001";
        private int timeoutSeconds = 600;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Llm {
        /**
         * OpenAI-compatible API root, e.g. {@code http://127.0.0.1:8000/v1}
         * (chat completions will be POST to .../chat/completions).
         */
        private String baseUrl = "http://127.0.0.1:8000/v1";
        private String model = "Qwen/Qwen2.5-7B-Instruct";
        private String apiKey = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Translation {
        private int chunkSize = 1200;
        private int maxConcurrentChunks = 1;

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getMaxConcurrentChunks() {
            return maxConcurrentChunks;
        }

        public void setMaxConcurrentChunks(int maxConcurrentChunks) {
            this.maxConcurrentChunks = maxConcurrentChunks;
        }
    }

    public static class Deployment {
        private boolean gpuSerialParseThenTranslate = true;

        public boolean isGpuSerialParseThenTranslate() {
            return gpuSerialParseThenTranslate;
        }

        public void setGpuSerialParseThenTranslate(boolean gpuSerialParseThenTranslate) {
            this.gpuSerialParseThenTranslate = gpuSerialParseThenTranslate;
        }
    }
}
