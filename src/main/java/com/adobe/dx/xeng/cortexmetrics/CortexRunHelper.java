package com.adobe.dx.xeng.cortexmetrics;

import com.adobe.dx.xeng.cortexmetrics.config.CortexMetricsGlobalConfig;
import hudson.model.Result;
import hudson.model.Run;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods for dealing with Cortex metrics and labels.
 *
 * @author saville
 */
class CortexRunHelper {
    private static final String JENKINS_METRIC_NAME = "jenkins_job";

    private static String getMetricName(String namespace, String name) {
        String actualNamespace = namespace;
        if (namespace == null) {
            actualNamespace = CortexMetricsGlobalConfig.DEFAULT_NAMESPACE;
        }
        return actualNamespace + "_" + JENKINS_METRIC_NAME + "_" + name;
    }

    private static String getRunResult(Run<?, ?> run) {
        Result result = run.getResult();
        if (result == null) {
            // Default to success if no status is reported yet
            result = Result.SUCCESS;
        }
        return result.toString();
    }

    private static String getRunJobName(Run<?, ?> run) {
        return run.getParent().getFullName();
    }

    private static long getRunDuration(Run<?, ?> run) {
        // getDuration is only set after the build is complete, which often is not valid for us, so use
        // a workaround instead in those cases (from https://github.com/jenkinsci/workflow-support-plugin/pull/33)
        return run.getDuration() != 0 ? run.getDuration() : System.currentTimeMillis() - run.getStartTimeInMillis();
    }

    /**
     * Generates metrics for publishing to Cortex, including job count and duration.
     * @param run The job run
     * @param namespace The namespace to use for metrics, defaults to "default"
     * @return All metrics for sending to Cortex
     */
    static Map<String, Number> getMetrics(Run<?, ?> run, String namespace) {
        Map<String, Number> metrics = new HashMap<>();
        // This metric may be used to report on job run counts, result counts, etc
        metrics.put(getMetricName(namespace, "count"), 1);
        // This metric may be used to report on job run durations
        metrics.put(getMetricName(namespace, "duration"), getRunDuration(run) / 1000);
        return metrics;
    }

    /**
     * Generates labels for publishing to Cortex, automatically including some default labels such as name and result,
     * and optionally including additional labels.
     * @param run The job run
     * @param labels Additional labels to add, may be null
     * @return All labels for sending to Cortex
     */
    static Map<String, String> getLabels(Run<?, ?> run, Map<String, String> labels) {
        Map<String, String> allLabels = new HashMap<>();
        allLabels.put("job_name", getRunJobName(run));
        allLabels.put("job_result", getRunResult(run));

        // Override any default labels by doing this last
        if (labels != null) {
            allLabels.putAll(labels);
        }

        // Return the combined result
        return allLabels;
    }
}
