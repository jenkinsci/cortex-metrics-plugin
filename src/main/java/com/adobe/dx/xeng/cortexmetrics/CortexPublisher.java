package com.adobe.dx.xeng.cortexmetrics;

import com.adobe.dx.xeng.cortexmetrics.config.CortexMetricsConfigProvider;
import com.adobe.dx.xeng.cortexmetrics.proto.Prometheus;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
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
    private static final int TIMEOUT = 60000;

    private static final RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(TIMEOUT)
            .setConnectionRequestTimeout(TIMEOUT)
            .setSocketTimeout(TIMEOUT)
            .build();
    private static HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

    private final String url;
    private final Secret bearerToken;
    private final String namespace;
    private final Run<?, ?> run;
    private final Map<String, String> labels;

    CortexPublisher(Run<?, ?> run, String url, Secret bearerToken, String namespace, Map<String, String> labels) {
        // Pull configuration from global config if not specific directly
        if (StringUtils.isBlank(url)) {
            this.url = CortexMetricsConfigProvider.getConfiguredUrl(run.getParent());
        } else {
            this.url = url;
        }
        if (bearerToken == null || StringUtils.isBlank(bearerToken.getPlainText())) {
            this.bearerToken = CortexMetricsConfigProvider.getConfiguredBearerToken(run.getParent());
        } else {
            this.bearerToken = bearerToken;
        }
        if (StringUtils.isBlank(namespace)) {
            this.namespace = CortexMetricsConfigProvider.getConfiguredNamespace(run.getParent());
        } else {
            this.namespace = namespace;
        }

        // Validate parameters
        if (StringUtils.isBlank(this.url)) {
            throw new IllegalArgumentException("Cortex URL is not set, cannot publish metrics");
        }
        if (this.bearerToken == null || StringUtils.isBlank(this.bearerToken.getPlainText())) {
            throw new IllegalArgumentException("Cortex bearer token is not set, cannot publish metrics");
        }
        if (StringUtils.isBlank(this.namespace)) {
            throw new IllegalArgumentException("Cortex namespace is not set, cannot publish metrics");
        }

        this.run = run;
        this.labels = labels;
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
        Prometheus.WriteRequest writeRequest = writeRequestBuilder.addAllTimeseries(timeSeriesList).build();
        byte[] compressed = Snappy.compress(writeRequest.toByteArray());
        ByteArrayEntity byteArrayEntity = new ByteArrayEntity(compressed);

        httpPost.setEntity(byteArrayEntity);
        httpClient.execute(httpPost);
    }

    void send(TaskListener listener) throws Exception {
        listener.getLogger().println("Publishing metrics to Cortex at " + url + " with namespace " + namespace);
        Map<String, Number> sendMetrics = CortexRunHelper.getMetrics(run, namespace);
        Map<String, String> sendLabels = CortexRunHelper.getLabels(run, labels);
        listener.getLogger().println("Metrics: " + sendMetrics + ", labels: " + sendLabels);

        List<Prometheus.TimeSeries> timeSeriesList = createTimeSeries(sendMetrics, sendLabels);
        write(timeSeriesList);
        listener.getLogger().println("Successfully sent metrics to Cortex");
    }
}
