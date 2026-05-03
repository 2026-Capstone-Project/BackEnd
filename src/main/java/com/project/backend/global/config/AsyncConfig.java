package com.project.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "vectorSyncExecutor")
    public Executor vectorSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // 평상시 유지할 스레드 수
        executor.setMaxPoolSize(5);       // 최대 생성 가능한 스레드 수
        executor.setQueueCapacity(100);   // 대기 큐 크기
        executor.setThreadNamePrefix("vector-sync-");  // 스레드 이름 prefix
        executor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 실행 중인 작업 완료 대기
        executor.setAwaitTerminationSeconds(30);            // 최대 30초까지 대기
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("[AsyncError] method={}, params={}, message={}",
                        method.getName(), Arrays.toString(params), ex.getMessage(), ex);
    }
}