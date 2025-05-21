package com.vik.herald.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vik.herald.exceptions.DownStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingMethods {
    @Autowired private final ObjectMapper objectMapper;
    @Autowired private final Environment environment;
    @Autowired private final MetricsService metricsService;

    public void logDownstreamRequest(String classMethodName,
                                     String host,
                                     String endpoint,
                                     Object... args) {
        try {
            String dns = getProperty(host);
            String path = getProperty(endpoint);
            String requestStr = getString(args);

            metricsService.incrementCounter("Outgoing_Request",
                    "dns", dns,
                    "path", path);

            log.info(
                    "Downstream Request {} :: dns: {}, path : {}. request : {}",
                    classMethodName,
                    dns,
                    path,
                    requestStr);
        } catch (Exception e) {
            log.error("Exception while logging downstream request: {}", ExceptionUtils.getStackTrace(e));
        }
    }

    public void logDownstreamRequestResponse(
            String classMethodName,
            String host,
            String endpoint,
            Object response,
            Object... args) {
        try {
            String dns = getProperty(host);
            String path = getProperty(endpoint);
            String requestStr = getString(args);
            String responseStr = getString(response);
            Map<String, String> tags = Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("dns", dns);
                put("path", path);
            }});
            metricsService.incrementCounter("Outgoing_Response",
                    "dns", dns,
                    "path", path);
            log.info(
                    "Downstream Response {} :: dns: {}, path : {}. response : {}, request : {}",
                    classMethodName,
                    dns,
                    path,
                    responseStr,
                    requestStr);
        } catch (Exception e) {
            log.error("Exception : {}", ExceptionUtils.getStackTrace(e));
        }
    }

    public void logDownstreamFallback(
            String classMethodName,
            String host,
            String endpoint,
            Throwable t,
            Object... args) {
        try {
            Throwable rootCause = getRootCause(t);
            Integer statusCode = -1;

            if (rootCause instanceof DownStreamException ex) {
                statusCode = ex.getStatusCode();
            }

            String dns = getProperty(host);
            String path = getProperty(endpoint);
            String requestStr = getString(args);

            List<String> tags = new ArrayList<>(List.of(
                    "dns", dns,
                    "path", path,
                    "error", rootCause.getClass().getSimpleName()
            ));

            if (statusCode != -1) {
                tags.add("statusCode");
                tags.add(String.valueOf(statusCode));
            }

            metricsService.incrementCounter("Outgoing_Fallback", tags.toArray(new String[0]));

            log.error(
                    "Downstream Fallback {} :: dns: {}, path : {}, statusCode: {}, request : {}, trace : {}",
                    classMethodName,
                    dns,
                    path,
                    statusCode,
                    requestStr,
                    ExceptionUtils.getStackTrace(rootCause));
        } catch (Exception e) {
            log.error("Exception : {}", ExceptionUtils.getStackTrace(e));
            metricsService.incrementCounter(
                    "OUTGOING_FALLBACK_HANDLING_FAILURE",
                    "e", e.getClass().getSimpleName(), "classMethodName", classMethodName);
        }
    }


    private String getString(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        List<String> serializedArgs = new ArrayList<>();
        for (Object arg : args) {
            try {
                serializedArgs.add(getString(arg));
            } catch (JsonProcessingException e) {
                serializedArgs.add("\"<unserializable>\"");
            }
        }
        return "[" + String.join(", ", serializedArgs) + "]";
    }

    private String getString(Object request) throws JsonProcessingException {
        if (request == null) {
            return null;
        } else if (request instanceof String) {
            return (String) request;
        } else if (request instanceof JSONArray || request instanceof JSONObject) {
            return request.toString();
        } else {
            return objectMapper.writeValueAsString(request);
        }
    }

    private String getProperty(String property) {
        if (property != null) {
            if (property.startsWith("${") && property.endsWith("}")) {
                return environment.getProperty(property.substring(2, property.length() - 1));
            }
        }
        return property;
    }

    public Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            throwable = cause;
            cause = throwable.getCause();
        }
        return throwable;
    }
}
