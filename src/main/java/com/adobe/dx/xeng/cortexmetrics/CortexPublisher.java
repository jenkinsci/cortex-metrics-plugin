package com.adobe.dx.xeng.cortexmetrics;

import com.adobe.dx.xeng.cortexmetrics.proto.Prometheus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.base.JacksonJsonCore;
import com.github.wnameless.json.base.JsonValueBase;
import com.github.wnameless.json.flattener.JsonFlattener;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xerial.snappy.Snappy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author saville
 */
class CortexPublisher {
    /**
     * The timeout for connections to cortex.
     */
    private static final int TIMEOUT = 60;

    private final String url;
    private final String bearerToken;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    CortexPublisher(String url, String bearerToken) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("Cortex URL is not set, cannot publish metrics");
        }
        if (StringUtils.isBlank(bearerToken)) {
            throw new IllegalArgumentException("Cortex bearer token is not set, cannot publish metrics");
        }
        this.url = url;
        this.bearerToken = bearerToken;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setConnectionRequestTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .build();
        this.httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        this.mapper = new ObjectMapper();
    }

    private Map<String, Object> getMapFromJson(JsonValueBase json) {
        return new JsonFlattener(new JacksonJsonCore(mapper), json).flattenAsMap();
    }

    private List<Prometheus.TimeSeries> createTimeSeries(Map<String, Number> metrics, Map<String, String> labels) {
        List<Prometheus.TimeSeries> timeSeriesList = new ArrayList<>();
        for(Map.Entry<String, Number> metric : metrics.entrySet()){
            // Create new builders for this iteration
            Prometheus.TimeSeries.Builder timeSeriesBuilder = Prometheus.TimeSeries.newBuilder();
            Prometheus.Sample.Builder sampleBuilder = Prometheus.Sample.newBuilder();

            // Always add metric name (required)
            labels.put("__name__", metric.getKey());

            // Add all labels
            for (Map.Entry<String, String> label : labels.entrySet()) {
                timeSeriesBuilder.addLabels(
                        Prometheus.Label.newBuilder().setName(label.getKey()).setValue(label.getValue()).build()
                );
            }

            // Set values on sample and add to time series
            sampleBuilder.setValue(metric.getValue().doubleValue());
            sampleBuilder.setTimestamp(System.currentTimeMillis());
            timeSeriesBuilder.addSamples(sampleBuilder.build());

            // Add time series to list
            timeSeriesList.add(timeSeriesBuilder.build());
        }
        return timeSeriesList;
    }

    private void write(List<Prometheus.TimeSeries> timeSeriesList) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type","application/x-www-form-urlencoded");
        httpPost.setHeader("Content-Encoding", "snappy");
        httpPost.setHeader("X-Prometheus-Remote-Write-Version", "0.1.0");
        httpPost.setHeader("Authorization", "Bearer " + this.bearerToken);

        Prometheus.WriteRequest.Builder writeRequestBuilder = Prometheus.WriteRequest.newBuilder();
        Prometheus.WriteRequest writeRequest= writeRequestBuilder.addAllTimeseries(timeSeriesList).build();
        byte[] compressed = Snappy.compress(writeRequest.toByteArray());
        ByteArrayEntity byteArrayEntity = new ByteArrayEntity(compressed);

        httpPost.setEntity(byteArrayEntity);
        this.httpClient.execute(httpPost);
    }

    void send(Map<String, Number> metrics, Map<String, String> labels) throws Exception {
        List<Prometheus.TimeSeries> timeSeriesList = createTimeSeries(metrics, labels);
        write(timeSeriesList);
    }
}
