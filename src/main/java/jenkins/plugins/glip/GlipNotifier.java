package jenkins.plugins.glip;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.plugins.glip.config.ItemConfigMigrator;
import jenkins.plugins.glip.webhook.GlobalConfig;
import jenkins.plugins.glip.webhook.model.GlipMessage;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

/**
 * Created by andrey.smirnov on 19.01.2017.
 */
@Getter
@Setter
public class GlipNotifier extends Notifier {

    private static final Logger logger = Logger.getLogger(GlipNotifier.class.getName());

    private String webhookId;
    private String activity;
    private String icon;
    private boolean startNotification;
    private boolean notifySuccess;
    private boolean notifyAborted;
    private boolean notifyFailure;
    private boolean notifyBackToNormal;
    private boolean notifyNotBuilt;
    private boolean notifyUnstable;
    private boolean notifyRepeatedFailure;
    private boolean includeTestSummary;
    private CommitInfoChoice commitInfoChoice;
    private boolean includeCustomMessage;
    private String customMessage;

    @Override
    public GlipNotifier.DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @DataBoundConstructor
    public GlipNotifier(String webhookId, String activity, String icon, boolean startNotification, boolean notifySuccess,
                        boolean notifyAborted, boolean notifyNotBuilt, boolean notifyUnstable, boolean notifyFailure,
                        boolean notifyBackToNormal, boolean notifyRepeatedFailure, boolean includeTestSummary,
                        CommitInfoChoice commitInfoChoice, boolean includeCustomMessage, String customMessage) {
        super();
        this.webhookId = webhookId;
        this.activity = activity;
        this.icon = icon;
        this.startNotification = startNotification;
        this.notifySuccess = notifySuccess;
        this.notifyAborted = notifyAborted;
        this.notifyNotBuilt = notifyNotBuilt;
        this.notifyUnstable = notifyUnstable;
        this.notifyFailure = notifyFailure;
        this.notifyBackToNormal = notifyBackToNormal;
        this.notifyRepeatedFailure = notifyRepeatedFailure;
        this.includeTestSummary = includeTestSummary;
        this.commitInfoChoice = commitInfoChoice;
        this.includeCustomMessage = includeCustomMessage;
        this.customMessage = customMessage;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public GlipService newGlipService(AbstractBuild r, BuildListener listener) {
        GlobalConfig globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);
        String webhookId = this.webhookId;
        String activity = this.activity;
        String icon = this.icon;
        if (StringUtils.isEmpty(webhookId)) {
            webhookId = getDescriptor().getWebhookId();
        }

        if (StringUtils.isEmpty(activity)) {
            activity = getDescriptor().getActivity();
        }

        if (StringUtils.isEmpty(icon)) {
            icon = getDescriptor().getIcon();
        }


        EnvVars env = null;
        try {
            env = r.getEnvironment(listener);
        } catch (Exception e) {
            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
            env = new EnvVars();
        }

        webhookId = env.expand(webhookId);
        return new StandardGlipService(globalConfig.getWebhookUrl(), webhookId, icon, activity);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if (startNotification) {
            Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
            for (Publisher publisher : map.values()) {
                if (publisher instanceof GlipNotifier) {
                    logger.info("Invoking Started...");
                    new ActiveNotifier((GlipNotifier) publisher, listener).started(build);
                }
            }
        }
        return super.prebuild(build, listener);
    }

    @Extension
    @Getter
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String webhookId;
        private String activity  = "Jenkins Notification";
        private String icon  = "https://d2rbro28ib85bu.cloudfront.net/images/integrations/128/jenkins-ci.png";

        public static final CommitInfoChoice[] COMMIT_INFO_CHOICES = CommitInfoChoice.values();

        public DescriptorImpl() {
            load();
        }
        //TODO delete
//        public ListBoxModel doFillTokenCredentialIdItems() {
//            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
//                return new ListBoxModel();
//            }
//            return new StandardListBoxModel()
//                    .withEmptySelection()
//                    .withAll(lookupCredentials(
//                            StringCredentials.class,
//                            Jenkins.getInstance(),
//                            ACL.SYSTEM,
//                            new HostnameRequirement("*.glip.com"))
//                    );
//        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        //TODO Where from glip message is coming????
        public GlipNotifier newInstance(StaplerRequest sr, JSONObject json) {
            String webhookId = sr.getParameter("webhookId");
            String activity = sr.getParameter("activity");
            String icon = sr.getParameter("icon");
            boolean startNotification = "true".equals(sr.getParameter("startNotification"));
            boolean notifySuccess = "true".equals(sr.getParameter("notifySuccess"));
            boolean notifyAborted = "true".equals(sr.getParameter("notifyAborted"));
            boolean notifyNotBuilt = "true".equals(sr.getParameter("notifyNotBuilt"));
            boolean notifyUnstable = "true".equals(sr.getParameter("notifyUnstable"));
            boolean notifyFailure = "true".equals(sr.getParameter("notifyFailure"));
            boolean notifyBackToNormal = "true".equals(sr.getParameter("notifyBackToNormal"));
            boolean notifyRepeatedFailure = "true".equals(sr.getParameter("notifyRepeatedFailure"));
            boolean includeTestSummary = "true".equals(sr.getParameter("includeTestSummary"));
            CommitInfoChoice commitInfoChoice = CommitInfoChoice.forDisplayName(sr.getParameter("commitInfoChoice"));
            boolean includeCustomMessage = "on".equals(sr.getParameter("includeCustomMessage"));
            String customMessage = sr.getParameter("customMessage");
            return new GlipNotifier(webhookId, activity, icon, startNotification, notifySuccess, notifyAborted,
                    notifyNotBuilt, notifyUnstable, notifyFailure, notifyBackToNormal, notifyRepeatedFailure,
                    includeTestSummary, commitInfoChoice, includeCustomMessage, customMessage);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            webhookId = sr.getParameter("webhookId");
            activity = sr.getParameter("activity");
            icon = sr.getParameter("icon");
            save();
            return super.configure(sr, formData);
        }

        GlipService getGlipService(final String webhookUrl, final String webhookId) {
            return new StandardGlipService(webhookUrl, webhookId);
        }

        @Override
        public String getDisplayName() {
            return "Glip Notifications";
        }

        public FormValidation doTestConnection(@QueryParameter("webhookId") final String webhookId,
                                               @QueryParameter("activity") final String activity,
                                               @QueryParameter("icon") final String icon)
                throws FormException {
            try {
                String targetWebhookId = webhookId;
                String targetActivity = activity;
                String targetIcon = icon;
                if (StringUtils.isEmpty(targetWebhookId)) {
                    targetWebhookId = this.webhookId;
                }
                if (StringUtils.isEmpty(targetActivity)) {
                    targetActivity = this.activity;
                }
                if (StringUtils.isEmpty(targetIcon)) {
                    targetIcon = this.icon;
                }
                GlobalConfig globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);
                GlipService testGlipService = getGlipService(globalConfig.getWebhookUrl(), targetWebhookId);
                String message = "Glip/Jenkins plugin: post to " + globalConfig.getWebhookUrl() + targetWebhookId;
                logger.log(Level.INFO, "Message prepared: " + message);
                boolean success = testGlipService.publish(new GlipMessage(targetIcon, targetActivity, "", message));
                return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
            } catch (Exception e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }
    }

    @Deprecated
    public static class GlipJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {

        private String webhookId;
        private boolean startNotification;
        private boolean notifySuccess;
        private boolean notifyAborted;
        private boolean notifyNotBuilt;
        private boolean notifyUnstable;
        private boolean notifyFailure;
        private boolean notifyBackToNormal;
        private boolean notifyRepeatedFailure;
        private boolean includeTestSummary;
        private boolean showCommitList;
        private boolean includeCustomMessage;
        private String customMessage;

        @DataBoundConstructor
        public GlipJobProperty(String webhookId,
                               boolean startNotification,
                               boolean notifyAborted,
                               boolean notifyFailure,
                               boolean notifyNotBuilt,
                               boolean notifySuccess,
                               boolean notifyUnstable,
                               boolean notifyBackToNormal,
                               boolean notifyRepeatedFailure,
                               boolean includeTestSummary,
                               boolean showCommitList,
                               boolean includeCustomMessage,
                               String customMessage) {
            this.webhookId = webhookId;
            this.startNotification = startNotification;
            this.notifyAborted = notifyAborted;
            this.notifyFailure = notifyFailure;
            this.notifyNotBuilt = notifyNotBuilt;
            this.notifySuccess = notifySuccess;
            this.notifyUnstable = notifyUnstable;
            this.notifyBackToNormal = notifyBackToNormal;
            this.notifyRepeatedFailure = notifyRepeatedFailure;
            this.includeTestSummary = includeTestSummary;
            this.showCommitList = showCommitList;
            this.includeCustomMessage = includeCustomMessage;
            this.customMessage = customMessage;
        }

        @Exported
        public String getWebhookId() {
            return webhookId;
        }

        @Exported
        public boolean isStartNotification() {
            return startNotification;
        }

        @Exported
        public boolean isNotifySuccess() {
            return notifySuccess;
        }

        @Exported
        public boolean isShowCommitList() {
            return showCommitList;
        }

        @Override
        public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
            return super.prebuild(build, listener);
        }

        @Exported
        public boolean isNotifyAborted() {
            return notifyAborted;
        }

        @Exported
        public boolean isNotifyFailure() {
            return notifyFailure;
        }

        @Exported
        public boolean isNotifyNotBuilt() {
            return notifyNotBuilt;
        }

        @Exported
        public boolean isNotifyUnstable() {
            return notifyUnstable;
        }

        @Exported
        public boolean isNotifyBackToNormal() {
            return notifyBackToNormal;
        }

        @Exported
        public boolean isIncludeTestSummary() {
            return includeTestSummary;
        }

        @Exported
        public boolean isNotifyRepeatedFailure() {
            return notifyRepeatedFailure;
        }

        @Exported
        public boolean isIncludeCustomMessage() {
            return includeCustomMessage;
        }

        @Exported
        public String getCustomMessage() {
            return customMessage;
        }

    }

    @Extension(ordinal = 100)
    public static final class Migrator extends ItemListener {
        @Override
        public void onLoaded() {
            logger.info("Starting Settings Migration Process");

            ItemConfigMigrator migrator = new ItemConfigMigrator();

            for (Item item : Jenkins.getInstance().getAllItems()) {
                if (!migrator.migrate(item)) {
                    logger.info(String.format("Skipping job \"%s\" with type %s", item.getName(),
                            item.getClass().getName()));
                    continue;
                }
            }

            logger.info("Completed Settings Migration Process");
        }
    }

}
