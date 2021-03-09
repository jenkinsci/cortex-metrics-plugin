package com.adobe.dx.xeng.cortexmetrics

import hudson.Launcher
import hudson.model.AbstractBuild
import hudson.model.BuildListener
import hudson.model.Result
import org.jvnet.hudson.test.MockBuilder

/**
 * @author saville
 */
class SleepBuilder extends MockBuilder {
    SleepBuilder(Result result) {
        super(result)
    }

    @Override
    boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        launcher.launch(new Launcher.ProcStarter().cmdAsSingleString("sleep 1"))
        return super.perform(build, launcher, listener)
    }
}
