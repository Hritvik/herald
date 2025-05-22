package com.vik.herald.services;

import com.fasterxml.jackson.core.type.*;
import com.vik.herald.clients.*;
import com.vik.herald.utils.*;
import com.vik.utils.aop.annotations.*;
import com.vik.utils.data.request.*;
import com.vik.utils.data.responses.*;
import lombok.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class HttpBin {
    @Autowired private final RestClient restClient;

    @MeasureLatency(metricName = CommonConstants.MetricNames.DOWNSTREAM_CALL)
    public Future<BaseResponse> fetchNdrStatus(BaseRequest request) {
        return restClient.post(
                CommonConstants.HttpBin.IDENTIFIER,
                CommonConstants.HttpBin.Endpoints.POST,
                request,
                null,
                new TypeReference<BaseResponse>() {
                },
                BaseResponse::new
        );
    }
}
