package com.adobe.dx.xeng.cortexmetrics.config;

import hudson.ExtensionList;
import hudson.model.Item;

/**
 * Provider of Cortex Metrics configuration options.
 * @author saville
 */
public abstract class CortexMetricsConfigProvider {
    /**
     * Return the Cortex URL.
     * @return the Cortex URL
     * @param item the current item
     */
    public abstract String getUrl(Item item);

    /**
     * Return the bearer token in plain text.
     * @return the bearer token
     * @param item the current item
     */
    public abstract String getBearerToken(Item item);

    /**
     * Return the namespace for metric names.
     * @return the namespace
     * @param item the current item
     */
    public abstract String getNamespace(Item item);

    /**
     * Retrieves all Cortex metrics configuration providers.
     * @return the Cortex metrics config providers
     */
    private static ExtensionList<CortexMetricsConfigProvider> all() {
        return ExtensionList.lookup(CortexMetricsConfigProvider.class);
    }

    /**
     * Retrieves the first configured Cortex URL, either on a parent folder or in global configuration.
     * @param item the current item
     * @return the configured Cortex URL
     */
    public static String getConfiguredUrl(Item item) {
        for (CortexMetricsConfigProvider provider : all()) {
            if (provider == null) {
                continue;
            }
            String apiToken = provider.getUrl(item);
            if (apiToken != null) {
                return apiToken;
            }
        }
        return null;
    }

    /**
     * Retrieves the first configured bearer token, either on a parent folder or in global configuration.
     * @param item the current item
     * @return the configured bearer token
     */
    public static String getConfiguredBearerToken(Item item) {
        for (CortexMetricsConfigProvider provider : all()) {
            if (provider == null) {
                continue;
            }
            String apiToken = provider.getBearerToken(item);
            if (apiToken != null) {
                return apiToken;
            }
        }
        return null;
    }

    /**
     * Retrieves the first configured namespace, either on a parent folder or in global configuration.
     * @param item the current item
     * @return the configured namespace
     */
    public static String getConfiguredNamespace(Item item) {
        for (CortexMetricsConfigProvider provider : all()) {
            if (provider == null) {
                continue;
            }
            String apiToken = provider.getNamespace(item);
            if (apiToken != null) {
                return apiToken;
            }
        }
        return null;
    }
}
