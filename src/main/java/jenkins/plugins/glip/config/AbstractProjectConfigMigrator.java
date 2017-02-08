package jenkins.plugins.glip.config;

import hudson.model.AbstractProject;
import jenkins.plugins.glip.CommitInfoChoice;
import jenkins.plugins.glip.GlipNotifier;
import jenkins.plugins.glip.GlipNotifier.GlipJobProperty;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration migrator for migrating the Glip plugin configuration for a
 * {@link AbstractProject} from the 1.8 format to the 2.0 format. It does so by
 * removing the GlipJobProperty from the job properties (if there is one) and
 * moving the Glip notification settings to a {@link GlipNotifier} in the list
 * of publishers (if there is one).
 */
@SuppressWarnings("deprecation")
public class AbstractProjectConfigMigrator {

    private static final Logger logger = Logger.getLogger(AbstractProjectConfigMigrator.class
            .getName());

    public void migrate(final AbstractProject<?, ?> project) {

        logger.info(String.format("Migrating project \"%s\" with type %s", project.getName(),
                project.getClass().getName()));

        final GlipJobProperty glipJobProperty = project.getProperty(GlipJobProperty.class);

        if (glipJobProperty == null) {
            logger.info(String.format(
                    "Configuration is already up to date for \"%s\", skipping migration",
                    project.getName()));
            return;
        }

        GlipNotifier glipNotifier = project.getPublishersList().get(GlipNotifier.class);

        if (glipNotifier == null) {
            logger.info(String.format(
                    "Configuration does not have a notifier for \"%s\", not migrating settings",
                    project.getName()));
        } else {
            updateGlipNotifier(glipNotifier, glipJobProperty);
        }

        try {
            // property section is not used anymore - remove
            project.removeProperty(GlipJobProperty.class);
            project.save();
            logger.info("Configuration updated successfully");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void updateGlipNotifier(final GlipNotifier glipNotifier,
            final GlipJobProperty glipJobProperty) {

        if (StringUtils.isBlank(glipNotifier.getWebhookId())) {
            glipNotifier.setWebhookId(glipNotifier.getWebhookId());
        }

        glipNotifier.setStartNotification(glipJobProperty.isStartNotification());

        glipNotifier.setNotifyAborted(glipJobProperty.isNotifyAborted());
        glipNotifier.setNotifyFailure(glipJobProperty.isNotifyFailure());
        glipNotifier.setNotifyNotBuilt(glipJobProperty.isNotifyNotBuilt());
        glipNotifier.setNotifySuccess(glipJobProperty.isNotifySuccess());
        glipNotifier.setNotifyUnstable(glipJobProperty.isNotifyUnstable());
        glipNotifier.setNotifyBackToNormal(glipJobProperty.isNotifyBackToNormal());
        glipNotifier.setNotifyRepeatedFailure(glipJobProperty.isNotifyRepeatedFailure());

        glipNotifier.setIncludeTestSummary(glipJobProperty.isIncludeTestSummary());
        glipNotifier.setCommitInfoChoice(getCommitInfoChoice(glipJobProperty));
        glipNotifier.setIncludeCustomMessage(glipJobProperty.isIncludeCustomMessage());
        glipNotifier.setCustomMessage(glipJobProperty.getCustomMessage());
    }

    private CommitInfoChoice getCommitInfoChoice(final GlipJobProperty glipJobProperty) {
        if (glipJobProperty.isShowCommitList()) {
            return CommitInfoChoice.AUTHORS_AND_TITLES;
        } else {
            return CommitInfoChoice.NONE;
        }
    }
}
