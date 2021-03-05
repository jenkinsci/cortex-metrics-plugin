package com.adobe.dx.xeng.cortexmetrics

import com.adobe.dx.xeng.cortexmetrics.config.CortexMetricsFolderConfig
import com.adobe.dx.xeng.cortexmetrics.config.CortexMetricsGlobalConfig
import com.adobe.dx.xeng.cortexmetrics.proto.Prometheus
import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.FreeStyleProject
import hudson.model.Result
import hudson.util.Secret
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.util.EntityUtils
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.MockBuilder
import org.xerial.snappy.Snappy
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.mop.ConfineMetaClassChanges

@Unroll
@ConfineMetaClassChanges([CortexPublisher])
class CortexPublisherSpec extends Specification {
    @Rule JenkinsRule jenkinsRule = new JenkinsRule()

    def "send event with variables from global config"() {
        given:
        def globalConfig = jenkinsRule.jenkins.getExtensionList(CortexMetricsGlobalConfig.class)[0]
        globalConfig.setUrl("http://gc-url/")
        globalConfig.setBearerToken(Secret.fromString("gc-token"))
        globalConfig.setNamespace("gc-ns")

        and:
        def folder1 = jenkinsRule.createFolder("folder1")
        def project = folder1.createProject(FreeStyleProject.class, "job1")
        project.getPublishersList().add(new CortexMetricsNotifier())

        and:
        HttpClient httpClient = Mock()
        CortexPublisher.httpClient = httpClient

        when:
        def build = project.scheduleBuild2(0).get()
        jenkinsRule.waitForCompletion(build)

        then:
        1 * httpClient.execute({ HttpPost post ->
            // Check url and headers
            assert post.getURI() == new URI("http://gc-url/")
            assert post.getFirstHeader("Authorization").value == "Bearer gc-token"

            def bytes = EntityUtils.toByteArray(post.getEntity())
            def writeRequest = Prometheus.WriteRequest.parseFrom(Snappy.uncompress(bytes))
            assert writeRequest.timeseriesCount == 2
            assert writeRequest.timeseriesList[0].labelsCount == 3
            assert writeRequest.timeseriesList[0].labelsList[1].getName() == "__name__"
            assert writeRequest.timeseriesList[0].labelsList[1].getValue() == "gc-ns_jenkins_job_duration"
            assert writeRequest.timeseriesList[1].labelsCount == 3
            return true
        })
        0 * _._
    }

    def "send event with overrides"() {
        given:
        def globalConfig = jenkinsRule.jenkins.getExtensionList(CortexMetricsGlobalConfig.class)[0]
        globalConfig.setUrl("http://gc-url/")
        globalConfig.setBearerToken(Secret.fromString("gc-token"))
        globalConfig.setNamespace("gc-ns")

        and:
        def folder1 = jenkinsRule.createFolder("folder1")
        def project = folder1.createProject(FreeStyleProject.class, "job1")
        def cortexNotifier = new CortexMetricsNotifier()
        cortexNotifier.setUrl('http://override-url/')
        cortexNotifier.setBearerToken(Secret.fromString("override-token"))
        cortexNotifier.setNamespace('override-ns')
        cortexNotifier.setLabels(['l1': 'v1'])
        project.getPublishersList().add(cortexNotifier)
        project.getBuildersList().add(new MockBuilder(Result.UNSTABLE))

        and:
        HttpClient httpClient = Mock()
        CortexPublisher.httpClient = httpClient

        when:
        def build = project.scheduleBuild2(0).get()
        jenkinsRule.waitForCompletion(build)

        then:
        1 * httpClient.execute({ HttpPost post ->
            // Check url and headers
            assert post.getURI() == new URI("http://override-url/")
            assert post.getFirstHeader("Content-Type").value == "application/x-www-form-urlencoded"
            assert post.getFirstHeader("Content-Encoding").value == "snappy"
            assert post.getFirstHeader("X-Prometheus-Remote-Write-Version").value == "0.1.0"
            assert post.getFirstHeader("Authorization").value == "Bearer override-token"

            // Parse body
            def bytes = EntityUtils.toByteArray(post.getEntity())
            def writeRequest = Prometheus.WriteRequest.parseFrom(Snappy.uncompress(bytes))

            assert writeRequest.timeseriesCount == 2
            assert writeRequest.timeseriesList[0].labelsCount == 4
            assert writeRequest.timeseriesList[0].labelsList[0].getName() == "job_name"
            assert writeRequest.timeseriesList[0].labelsList[0].getValue() == "folder1/job1"
            assert writeRequest.timeseriesList[0].labelsList[1].getName() == "l1"
            assert writeRequest.timeseriesList[0].labelsList[1].getValue() == "v1"
            assert writeRequest.timeseriesList[0].labelsList[2].getName() == "__name__"
            assert writeRequest.timeseriesList[0].labelsList[2].getValue() == "override-ns_jenkins_job_count"
            assert writeRequest.timeseriesList[0].labelsList[3].getName() == "job_result"
            assert writeRequest.timeseriesList[0].labelsList[3].getValue() == "UNSTABLE"
            assert writeRequest.timeseriesList[0].samplesCount == 1
            assert writeRequest.timeseriesList[0].samplesList[0].getValue() == 1d

            assert writeRequest.timeseriesList[1].labelsCount == 4
            assert writeRequest.timeseriesList[1].samplesCount == 1
            assert writeRequest.timeseriesList[1].samplesList[0].getValue() >= 0
            return true
        })
        0 * _._
    }

    def "send failed event with folder config"() {
        given:
        def globalConfig = jenkinsRule.jenkins.getExtensionList(CortexMetricsGlobalConfig.class)[0]
        globalConfig.setUrl("http://gc-url/")
        globalConfig.setBearerToken(Secret.fromString("gc-token"))
        globalConfig.setNamespace("gc-ns")

        and:
        def folder1 = jenkinsRule.createProject(Folder.class, "folder1")
        def folderConfig = new CortexMetricsFolderConfig()
        folderConfig.setUrl("http://folder-url/")
        // Fallback to global config for token
        folderConfig.setNamespace("folder-ns1")
        folder1.addProperty(folderConfig)
        def project = folder1.createProject(FreeStyleProject.class, "job1")
        project.getPublishersList().add(new CortexMetricsNotifier())
        project.getBuildersList().add(new MockBuilder(Result.FAILURE))

        and:
        HttpClient httpClient = Mock()
        CortexPublisher.httpClient = httpClient

        when:
        def build = project.scheduleBuild2(0).get()
        jenkinsRule.waitForCompletion(build)

        then:
        1 * httpClient.execute({ HttpPost post ->
            // Check url and headers
            assert post.getURI() == new URI("http://folder-url/")
            assert post.getFirstHeader("Authorization").value == "Bearer gc-token"

            // Parse body
            def bytes = EntityUtils.toByteArray(post.getEntity())
            def writeRequest = Prometheus.WriteRequest.parseFrom(Snappy.uncompress(bytes))

            assert writeRequest.timeseriesCount == 2
            assert writeRequest.timeseriesList[0].labelsCount == 3
            assert writeRequest.timeseriesList[0].labelsList[0].getName() == "job_name"
            assert writeRequest.timeseriesList[0].labelsList[0].getValue() == "folder1/job1"
            assert writeRequest.timeseriesList[0].labelsList[1].getName() == "__name__"
            assert writeRequest.timeseriesList[0].labelsList[1].getValue() == "folder-ns1_jenkins_job_count"
            assert writeRequest.timeseriesList[0].labelsList[2].getName() == "job_result"
            assert writeRequest.timeseriesList[0].labelsList[2].getValue() == "FAILURE"
            assert writeRequest.timeseriesList[0].samplesCount == 1
            assert writeRequest.timeseriesList[0].samplesList[0].getValue() == 1d

            assert writeRequest.timeseriesList[1].labelsCount == 3
            assert writeRequest.timeseriesList[1].samplesCount == 1
            assert writeRequest.timeseriesList[1].samplesList[0].getValue() >= 0
            return true
        })
        0 * _._
    }
}
