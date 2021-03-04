package com.adobe.dx.xeng.cortexmetrics;

import com.adobe.dx.xeng.cortexmetrics.config.CortexMetricsConfigProvider;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author saville
 */
public class CortexMetricsNotifier extends Notifier {

    private String url;
    private String bearerToken;
    private String namespace;
    private Map<String, String> labels = new HashMap<>();

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    @DataBoundSetter
    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getNamespace() {
        return namespace;
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    @DataBoundSetter
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        // Get url, bearerToken, and namespace from configuration if not specified directly
        Item buildParent = build.getParent();
        String sendUrl = url;
        String sendBearerToken = url;
        String sendNamespace = url;
        if (StringUtils.isBlank(sendUrl)) {
            sendUrl = CortexMetricsConfigProvider.getConfiguredUrl(buildParent);
        }
        if (StringUtils.isBlank(sendBearerToken)) {
            sendBearerToken = CortexMetricsConfigProvider.getConfiguredBearerToken(buildParent);
        }
        if (StringUtils.isBlank(sendNamespace)) {
            sendNamespace = CortexMetricsConfigProvider.getConfiguredNamespace(buildParent);
        }

        CortexPublisher publisher = new CortexPublisher(sendUrl, sendBearerToken);
        try {
            publisher.send(CortexRunHelper.getMetrics(build, sendNamespace), CortexRunHelper.getLabels(build, labels));
            return true;
        } catch(Exception e) {
            listener.getLogger().println("Failed to send metrics to Cortex:");
            e.printStackTrace(listener.getLogger());
            return false;
        }
    }
}
