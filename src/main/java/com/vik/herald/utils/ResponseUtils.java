package com.vik.herald.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vik.herald.exceptions.BadRequestException;
import com.vik.herald.exceptions.InternalServerErrorException;
import com.vik.herald.exceptions.ResolveFutureException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
public class ResponseUtils {

    private ResponseUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static ObjectMapper objectMapper;

    static {
        ResponseUtils.objectMapper = new ObjectMapper();
    }

    private static boolean is2xx(Integer httpStatusCode) {
        return (httpStatusCode >= 200 && httpStatusCode < 300);
    }

    private static boolean is4xx(Integer httpStatusCode) {
        return (httpStatusCode >= 400 && httpStatusCode < 500);
    }

    public static <T> Object commandResponseHandlerErrorString(Pair<Integer, String> resp, TypeReference<T> typeReference) throws IOException {
        int statusCode = resp.getLeft();
        String responseBody = resp.getRight();
        if (is2xx(statusCode) || is4xx(statusCode)) {
            return parseResponseBody(responseBody, typeReference);
        } else {
            throw new InternalServerErrorException(resp.getLeft(), resp.getRight());
        }
    }

    public static <T> T commandResponseHandlerV3(Pair<Integer, String> resp, TypeReference<T> typeReference) throws IOException {
        int statusCode = resp.getLeft();
        String responseBody = resp.getRight();
        if (is2xx(statusCode)) {
            return parseResponseBody(responseBody, typeReference);
        } else if (is4xx(statusCode)) {
            throw new BadRequestException(statusCode, responseBody);
        } else {
            throw new InternalServerErrorException(statusCode, responseBody);
        }
    }

    private static <T> T parseResponseBody(String responseBody, TypeReference<T> typeReference) throws IOException {
        if (!StringUtils.hasLength(responseBody)) {
            return null;
        }
        return objectMapper.readValue(responseBody, typeReference);
    }


    @SneakyThrows
    public static <T> T resolve(Future<T> future) {
        if (future == null) {
            return null;
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResolveFutureException(e);
        } catch (ExecutionException e) {
            if (e.getCause().getCause() != null)
                throw e.getCause().getCause();
            throw e.getCause();
        }
    }
}
