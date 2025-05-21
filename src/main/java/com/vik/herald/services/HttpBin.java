package com.vik.herald.services;

import com.fasterxml.jackson.core.type.*;
import com.vik.herald.annotation.*;
import com.vik.herald.clients.*;
import com.vik.herald.data.request.*;
import com.vik.herald.data.responses.*;
import com.vik.herald.utils.*;
import lombok.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.util.concurrent.*;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class HttpBin {
    private final RestClient restClient;

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
