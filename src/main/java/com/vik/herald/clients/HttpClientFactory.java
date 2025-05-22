package com.vik.herald.clients;

import com.vik.herald.clients.filter.DownstreamRequestInterceptor;
import com.vik.herald.config.*;
import com.vik.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class HttpClientFactory {

    MetricsService metricsService;
    private final Map<String, CloseableHttpClient> clientMap;
    private final Map<String, PoolingHttpClientConnectionManager> connectionManagers;
    private final DownstreamRequestInterceptor interceptor;
    private final RestClientConfigProperties configProperties;

    public HttpClientFactory(
            DownstreamRequestInterceptor interceptor,
            RestClientConfigProperties configProperties) {
        clientMap = new ConcurrentHashMap<>();
        connectionManagers = new ConcurrentHashMap<>();
        this.interceptor = interceptor;
        this.configProperties = configProperties;
    }

    public CloseableHttpClient getClient(String name) {
        if (clientMap.isEmpty()){
            rebuildAll();
        }
        else if (!clientMap.containsKey(name)) {
            rebuildClient(name);
        }
        // Record metrics when client is accessed
        recordClientMetrics(name);
        return clientMap.get(name);
    }

    public void rebuildAll() {
        Map<String, RestClientConfigProperties.RestServiceConfig> services = configProperties.getServices();
        clientMap.clear();
        connectionManagers.clear();
        for (Map.Entry<String, RestClientConfigProperties.RestServiceConfig> entry : services.entrySet()) {
            String serviceName = entry.getKey();
            rebuildClient(serviceName);
        }
        log.info("Rebuilt HTTP clients for all services: {}", clientMap.keySet());
    }

    public void rebuildClient(String serviceName) {
        Map<String, RestClientConfigProperties.RestServiceConfig> services = configProperties.getServices();
        RestClientConfigProperties.RestServiceConfig serviceConfig = services.get(serviceName);

        if (serviceConfig == null) {
            throw new IllegalArgumentException("No configuration found for service: " + serviceName);
        }

        RestClientConfigProperties.HttpPoolConfigs poolConfigs = serviceConfig.getConnectionPool();
        CloseableHttpClient newClient = buildClient(serviceName, poolConfigs, interceptor);
        clientMap.put(serviceName, newClient);
        recordClientMetrics(serviceName);
        log.info("Rebuilt HTTP client for service: {}", serviceName);
    }

    CloseableHttpClient buildClient(
            String serviceName,
            RestClientConfigProperties.HttpPoolConfigs httpPoolConfigs,
            DownstreamRequestInterceptor downstreamRequestInterceptor) {
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", SSLConnectionSocketFactory.getSocketFactory())
                        .build();

        PoolingHttpClientConnectionManager pooledConnectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        pooledConnectionManager.setDefaultMaxPerRoute(httpPoolConfigs.getDefaultMaxPerRoute());
        pooledConnectionManager.setMaxTotal(httpPoolConfigs.getMaxTotal());

        connectionManagers.put(serviceName, pooledConnectionManager);

        RestClientConfigProperties.RestServiceConfig serviceConfig = configProperties.getServices().get(serviceName);
        RestClientConfigProperties.Timeout timeout = serviceConfig.getTimeout();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(timeout.getSocketTimeout()))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeout.getRequestTimeout()))
                .build();

        return HttpClientBuilder.create()
                .evictIdleConnections(TimeValue.ofSeconds(httpPoolConfigs.getMaxIdleTime()))
                .addRequestInterceptorFirst(downstreamRequestInterceptor)
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(pooledConnectionManager)
                .build();
    }

    private void recordClientMetrics(String serviceName) {
        PoolingHttpClientConnectionManager manager = connectionManagers.get(serviceName);
        if (manager != null) {
            recordConnectionPoolMetrics(serviceName, manager.getTotalStats());
        }
    }


    public void recordConnectionPoolMetrics(String clientName, PoolStats poolStats) {
        try {
            metricsService.incrementCounter("Client_Connection_Pool",
                    "client", clientName,
                    "active", String.valueOf(poolStats.getLeased()),
                    "idle", String.valueOf(poolStats.getAvailable()),
                    "total", String.valueOf(poolStats.getMax()));
        } catch (Exception e) {
            log.error("Error recording connection pool metrics for client {}: {}", clientName, e.getMessage());
        }
    }
}
