package jenkins.plugins.glip.workflow;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.plugins.glip.GlipNotifier;
import jenkins.plugins.glip.GlipService;
import jenkins.plugins.glip.Messages;
import jenkins.plugins.glip.StandardGlipService;
import jenkins.plugins.glip.webhook.GlobalConfig;
import lombok.Getter;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Created by andrey.smirnov on 19.01.2017.
 */
public class GlipSendStep extends AbstractStepImpl {

    @Getter
    @Nonnull
    private final String message;
    @Getter
    private String webhookId;
    @Getter
    private boolean failOnError;

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @DataBoundSetter
    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    @DataBoundConstructor
    public GlipSendStep(@Nonnull String message) {
        this.message = message;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GlipSendStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "glipSend";
        }

        @Override
        public String getDisplayName() {
            return Messages.GlipSendStepDisplayName();
        }
    }

    public static class GlipSendStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @Inject
        transient GlipSendStep step;

        @StepContextParameter
        transient TaskListener listener;

        @Override
        protected Void run() throws Exception {

            Jenkins jenkins = Jenkins.getInstance();
            GlobalConfig globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);
            GlipNotifier.DescriptorImpl glipDesc = jenkins.getDescriptorByType(GlipNotifier.DescriptorImpl.class);
            listener.getLogger().println("run glipStepSend for " + glipDesc.getWebhookId());

            GlipService glipService = new StandardGlipService(globalConfig.getWebhookUrl(), glipDesc.getWebhookId());
            boolean publishSuccess = glipService.publish(step.message);
            if (!publishSuccess && step.failOnError) {
                throw new AbortException(Messages.NotificationFailed());
            } else if (!publishSuccess) {
                listener.error(Messages.NotificationFailed());
            }
            return null;
        }

    }

}