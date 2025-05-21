package com.vik.herald.config;

import com.vik.herald.utils.*;
import org.springframework.context.annotation.*;

import java.util.concurrent.*;

@Configuration
public class ExecutorConfig {

    @Bean(name = CommonConstants.BeanNames.REST_CLIENT_EXECUTOR, destroyMethod = "close")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
