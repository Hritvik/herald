package com.vik.herald.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vik.herald.aop.annotations.ControlleraInstrumentation;
import com.vik.herald.services.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Slf4j
@RestController
@RequestMapping("/api/1.0/herald/dev/downstream")
@RequiredArgsConstructor
public class DownstreamController {

    @Autowired private final ObjectMapper objectMapper;

    @Autowired HttpBin httpBin;

    // Cache for method lookups
    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> parameterTypeCache = new ConcurrentHashMap<>();
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @PostMapping(value = "/{service}/{method}")
    @ControlleraInstrumentation(apiPath = "/{service}/{method}")
    public <I, O> ResponseEntity<Object> hit(@PathVariable String service, @PathVariable String method, @RequestBody Map<String, Object> request) {
        try {
            // Input validation
            if (!StringUtils.hasText(service) || !StringUtils.hasText(method)) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Service and method names cannot be empty");
            }

            // Get service instance
            Object serviceInstance = getServiceInstance(service);
            if (serviceInstance == null) {
                return createErrorResponse(HttpStatus.NOT_FOUND, "Service not found: " + service);
            }

            // Get method and parameter type
            Method serviceMethod = getServiceMethod(serviceInstance, method);
            if (serviceMethod == null) {
                return createErrorResponse(HttpStatus.NOT_FOUND, "Method not found: " + method);
            }

            // Get parameter type
            Class<?> parameterType = getParameterType(serviceMethod);
            
//            // Validate request structure against target type
//            ValidationResult validationResult = validateRequestStructure(request, parameterType);
//            if (!validationResult.isValid()) {
//                return createErrorResponse(HttpStatus.BAD_REQUEST, validationResult.getErrorMessage());
//            }

            // Convert request to target type
            Object mappedRequest;
            try {
                mappedRequest = objectMapper.convertValue(request, parameterType);
            } catch (IllegalArgumentException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request format: " + e.getMessage());
            }

//            // Validate converted object
//            Set<ConstraintViolation<Object>> violations = validator.validate(mappedRequest);
//            if (!violations.isEmpty()) {
//                String errorMessage = violations.stream()
//                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
//                    .collect(Collectors.joining(", "));
//                return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
//            }

            // Invoke method
            Object response = serviceMethod.invoke(serviceInstance, mappedRequest);

            // Handle async response
            if (response instanceof Future<?>) {
                Future<?> future = (Future<?>) response;
                Object result = future.get();
                return ResponseEntity.ok(result);
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error processing request: {}", ExceptionUtils.getStackTrace(e));
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private ValidationResult validateRequestStructure(Map<String, Object> request, Class<?> targetType) {
        if (request == null) {
            return ValidationResult.invalid("Request body cannot be null");
        }

        try {
            // Get all fields from the target type with their JSON property names
            Map<String, String> requiredFields = new HashMap<>();
            Map<String, String> allFields = new HashMap<>();
            
            for (Field field : targetType.getDeclaredFields()) {
                String jsonPropertyName = getJsonPropertyName(field);
                allFields.put(jsonPropertyName, field.getName());
                
                if (field.isAnnotationPresent(NotNull.class)) {
                    requiredFields.put(jsonPropertyName, field.getName());
                }
            }

            // Check for missing required fields
            for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
                if (!request.containsKey(entry.getKey())) {
                    return ValidationResult.invalid("Required field missing: " + entry.getKey());
                }
            }

            // Check for unknown fields if strict validation is needed
            Set<String> requestFields = new HashSet<>(request.keySet());
            requestFields.removeAll(allFields.keySet());
            if (!requestFields.isEmpty()) {
                log.warn("Unknown fields in request: {}", requestFields);
            }

            return ValidationResult.valid();
        } catch (Exception e) {
            log.warn("Error during request structure validation: {}", e.getMessage());
            return ValidationResult.invalid("Error validating request structure: " + e.getMessage());
        }
    }

    private String getJsonPropertyName(Field field) {
        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
            return jsonProperty.value();
        }
        return field.getName();
    }

    private Object getServiceInstance(String service) {
        try {
            Field field = this.getClass().getDeclaredField(service);
            field.setAccessible(true);
            return field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Service not found: {}", service);
            return null;
        }
    }

    private Method getServiceMethod(Object serviceInstance, String methodName) {
        String cacheKey = serviceInstance.getClass().getName() + "#" + methodName;
        return methodCache.computeIfAbsent(cacheKey, k -> {
            try {
                for (Method method : serviceInstance.getClass().getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        return method;
                    }
                }
                return null;
            } catch (SecurityException e) {
                log.error("Security exception while accessing method", e);
                return null;
            }
        });
    }

    private Class<?> getParameterType(Method method) {
        return parameterTypeCache.computeIfAbsent(
            method.toString(),
            k -> method.getParameterTypes()[0]
        );
    }

    private ResponseEntity<Object> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @Value
    private static class ValidationResult {
        boolean valid;
        String errorMessage;

        static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}
