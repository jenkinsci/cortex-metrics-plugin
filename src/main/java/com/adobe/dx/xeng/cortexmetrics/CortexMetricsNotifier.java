package com.adobe.dx.xeng.cortexmetrics;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author saville
 */
public class CortexMetricsNotifier extends Notifier {

    private String url;
    private Secret bearerToken;
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

    public Secret getBearerToken() {
        return bearerToken;
    }

    @DataBoundSetter
    public void setBearerToken(Secret bearerToken) {
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
        try {
            String bearerTokenString = null;
            if (bearerToken != null) {
                bearerTokenString = bearerToken.getPlainText();
            }
            CortexPublisher publisher = new CortexPublisher(build, url, bearerTokenString, namespace, labels);
            publisher.send(listener);
            return true;
        } catch(Exception e) {
            listener.getLogger().println("Failed to send metrics to Cortex:");
            e.printStackTrace(listener.getLogger());
            return false;
        }
    }
    /**
     * Descriptor.
     */
    @Extension
    public static final class CortexMetricsNotifierDescriptor extends BuildStepDescriptor<Publisher> {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Publish metrics to Cortex";
        }
    }
}
