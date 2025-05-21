package com.vik.herald.clients.filter;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.MDC;
import org.springframework.stereotype.*;

import java.io.IOException;

@Component
public class DownstreamRequestInterceptor implements HttpRequestInterceptor {
    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context) throws HttpException, IOException {
        // Add trace headers if available in MDC
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            request.setHeader("X-B3-TraceId", traceId);
        }
        
        String spanId = MDC.get("spanId");
        if (spanId != null) {
            request.setHeader("X-B3-SpanId", spanId);
        }
    }
}
