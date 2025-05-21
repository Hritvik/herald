package com.vik.herald.aop.aspects;


import com.vik.herald.aop.annotations.ControlleraInstrumentation;
import com.vik.herald.data.responses.BaseResponse;
import com.vik.herald.exceptions.BadRequestException;
import com.vik.herald.utils.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ControllerInstrumentationAspect {

    private final MetricsService metricsService;

    @Around("@annotation(controlleraInstrumentation)")
    public Object logAndMetrics(
            ProceedingJoinPoint joinPoint, ControlleraInstrumentation controlleraInstrumentation)
            throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String apiPath = controlleraInstrumentation.apiPath();
        Object[] args = joinPoint.getArgs();

        log.info("Request to API: {}, Method: {} with arguments: {}", apiPath, methodName, args);
        metricsService.incrementCounter("Controller_Request", "api", apiPath);
        try {
            Object result = joinPoint.proceed();
            log.info(
                    "Response from API: {}, Method: {}. Result: {}, arguments: {}",
                    apiPath,
                    methodName,
                    result,
                    args);
            return result;
        } catch (BadRequestException e) {
            logError(apiPath, methodName, args, e);
            BaseResponse errorResponse = new BaseResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logError(apiPath, methodName, args, e);
            BaseResponse errorResponse = new BaseResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private void logError(
            String apiPath, String methodName, Object[] args, Exception e) {
        log.error(
                "Exception in API {} ({}): Arguments: {} - Error: {}",
                apiPath,
                methodName,
                args,
                ExceptionUtils.getStackTrace(e));
        metricsService.incrementCounter("Controller_Exception", "api", apiPath, "error", e.getClass().getSimpleName());
    }
}
