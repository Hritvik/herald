package com.vik.herald.aspect;

import com.vik.herald.annotation.MeasureLatency;
import com.vik.herald.utils.MetricsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LatencyAspect {

    private final MetricsService metricsService;

    public LatencyAspect(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Around("@annotation(com.vik.herald.annotation.MeasureLatency)")
    public Object measureLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        MeasureLatency annotation = signature.getMethod().getAnnotation(MeasureLatency.class);
        
        String metricName = annotation.metricName();
        if (metricName.isEmpty()) {
            metricName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        }

        return metricsService.recordDuration(metricName, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }, annotation.tags());
    }
} 