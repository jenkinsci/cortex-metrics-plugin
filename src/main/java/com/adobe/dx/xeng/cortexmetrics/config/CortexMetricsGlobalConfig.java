package com.adobe.dx.xeng.cortexmetrics.config;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Item;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Stores the global configuration options.
 * @since 0.1.0
 */
@Extension
public class CortexMetricsGlobalConfig extends GlobalConfiguration {
    public static final String DEFAULT_NAMESPACE = "default";

    /**
     * The Cortext URL to push metrics.
     */
    private String url;
    /**
     * The bearer token to use.
     */
    private Secret bearerToken;
    /**
     * The namespace to use, defaults to "default".
     */
    private String namespace = DEFAULT_NAMESPACE;

    /**
     * Constructor.
     */
    public CortexMetricsGlobalConfig() {
        load();
    }

    /**
     * Get the URL for publishing metrics to Cortex.
     * @return the Cortex write URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL for publishing metrics to Cortex.
     * @param url the Cortex write URL
     */
    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get the bearer token used to authenticate to Cortex.
     * @return the bearer token for authentication
     */
    public Secret getBearerToken() {
        return bearerToken;
    }

    /**
     * Set the bearer token used to authenticate to Cortex.
     * @param bearerToken the bearer token for authentication
     */
    @DataBoundSetter
    public void setBearerToken(Secret bearerToken) {
        this.bearerToken = bearerToken;
    }

    /**
     * Get the namespace for metric names (the first token in each metric sent to Cortex).
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Set the namespace for metric names.
     * @param namespace the namespace
     */
    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Retrieve the global configuration instance.
     * @return the global configuration
     */
    public static CortexMetricsGlobalConfig get() {
        return ExtensionList.lookup(CortexMetricsGlobalConfig.class).get(CortexMetricsGlobalConfig.class);
    }

    // Override to save XML data
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    /**
     * Provider for global configuration.
     */
    // Last one to be asked
    @Extension(ordinal = -10000)
    public static class GlobalCortexMetricsConfigProvider extends CortexMetricsConfigProvider {
        /**
         * Configuration, injected by jenkins.
         */
        @Inject
        private CortexMetricsGlobalConfig config;

        @Override
        public String getUrl(Item item) {
            String url = config.getUrl();
            if (!StringUtils.isBlank(url)) {
                return url;
            }
            return null;
        }

        @Override
        public Secret getBearerToken(Item item) {
            return config.getBearerToken();
        }

        @Override
        public String getNamespace(Item item) {
            return config.getNamespace();
        }
    }
}
