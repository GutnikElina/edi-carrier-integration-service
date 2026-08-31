package com.innowise.edi_carrier_integration_service.edi.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableRetry
public class AsyncVirtualThreadConfig {

    @Bean(name = "ediAsyncTaskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("edi-vt-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
