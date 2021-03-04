package com.adobe.dx.xeng.cortexmetrics.config;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

/**
 * Provides folder level Gauntlet configuration.
 */
public class CortexMetricsFolderConfig extends AbstractFolderProperty<AbstractFolder<?>> {
    /**
     * The Cortext URL to push metrics.
     */
    private String url;
    /**
     * The bearer token to use.
     */
    private Secret bearerToken;
    /**
     * The namespace to use.
     */
    private String namespace;

    /**
     * Constructor.
     */
    @DataBoundConstructor
    public CortexMetricsFolderConfig() {}

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
     * Return the plain text bearer token.
     *
     * @return the plain text bearer token
     */
    public String getPlainTextBearerToken() {
        if (bearerToken != null) {
            return bearerToken.getPlainText();
        }
        return null;
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
     * Descriptor class.
     */
    @Extension
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Cortex Metrics Configuration";
        }
    }

    /**
     * Configuration provider for folder config.
     */
    // This will be asked first
    @Extension(ordinal = 10000)
    public static class FolderCortexMetricsConfigProvider extends CortexMetricsConfigProvider {

        @Override
        public String getUrl(Item item) {
            if (item != null) {
                ItemGroup parent = item.getParent();
                while (parent != null) {
                    if (parent instanceof AbstractFolder) {
                        AbstractFolder folder = (AbstractFolder) parent;
                        CortexMetricsFolderConfig config = (CortexMetricsFolderConfig) folder.getProperties().get(
                                CortexMetricsFolderConfig.class);
                        if (config != null) {
                            String url = config.getUrl();
                            if (!StringUtils.isBlank(url)) {
                                return url;
                            }
                        }
                    }

                    if (parent instanceof Item) {
                        parent = ((Item) parent).getParent();
                    } else {
                        parent = null;
                    }
                }
            }
            return null;
        }

        @Override
        public String getBearerToken(Item item) {
            if (item != null) {
                ItemGroup parent = item.getParent();
                while (parent != null) {
                    if (parent instanceof AbstractFolder) {
                        AbstractFolder folder = (AbstractFolder) parent;
                        CortexMetricsFolderConfig config = (CortexMetricsFolderConfig) folder.getProperties().get(
                                CortexMetricsFolderConfig.class);
                        if (config != null) {
                            String bearerToken = config.getPlainTextBearerToken();
                            if (!StringUtils.isBlank(bearerToken)) {
                                return bearerToken;
                            }
                        }
                    }

                    if (parent instanceof Item) {
                        parent = ((Item) parent).getParent();
                    } else {
                        parent = null;
                    }
                }
            }
            return null;
        }

        @Override
        public String getNamespace(Item item) {
            if (item != null) {
                ItemGroup parent = item.getParent();
                while (parent != null) {
                    if (parent instanceof AbstractFolder) {
                        AbstractFolder folder = (AbstractFolder) parent;
                        CortexMetricsFolderConfig config = (CortexMetricsFolderConfig) folder.getProperties().get(
                                CortexMetricsFolderConfig.class);
                        if (config != null) {
                            String namespace = config.getNamespace();
                            if (!StringUtils.isBlank(namespace)) {
                                return namespace;
                            }
                        }
                    }

                    if (parent instanceof Item) {
                        parent = ((Item) parent).getParent();
                    } else {
                        parent = null;
                    }
                }
            }
            return null;
        }
    }
}
