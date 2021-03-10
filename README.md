# Cortex Metrics Plugin

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/cortex-metrics.svg)](https://plugins.jenkins.io/cortex-metrics)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/cortex-metrics.svg?color=blue)](https://plugins.jenkins.io/cortex-metrics)

## About this plugin

This plugin contains notifiers to send job run results to Cortex. While the
[prometheus plugin](https://github.com/jenkinsci/prometheus-plugin) exposes general
statistics about Jenkins itself, it does not scale when sending specific job results.
This plugin was created to address this need and skip prometheus to go directly to
Cortex for simplicity.

## Changelog

### 1.0.1

Initial release, because why start at 1.0.0?


## Plugin maintenance

### Releasing new versions

Use the instructions found [in the Jenkins docs](https://www.jenkins.io/doc/developer/publishing/releasing/).
