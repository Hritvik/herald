package com.vik.herald.utils;

public class CommonConstants {
    public static class FilterConstants {
        public static final String TRACE_ID_HEADER = "X-B3-TraceId";
        public static final String SPAN_ID_HEADER = "X-B3-SpanId";
    }
    public static class MetricNames {
        public static final String DOWNSTREAM_CALL = "DOWNSTREAM_CALL";
    }

    public static class HttpBin {
        public static final String IDENTIFIER = "http-bin";
        public static class Endpoints {
            public static final String POST = "post";
        }
    }

    public static class BeanNames {
        public static final String REST_CLIENT_EXECUTOR = "restClientExecutor";
    }

}
