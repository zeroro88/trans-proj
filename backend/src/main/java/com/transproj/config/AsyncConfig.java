package com.transproj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "jobTaskExecutor")
    public TaskExecutor jobTaskExecutor() {
        var exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(1);
        exec.setMaxPoolSize(1);
        exec.setQueueCapacity(64);
        exec.setThreadNamePrefix("job-");
        exec.initialize();
        return exec;
    }
}
