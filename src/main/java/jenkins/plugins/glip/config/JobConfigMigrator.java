package jenkins.plugins.glip.config;

import hudson.model.Job;
import jenkins.plugins.glip.GlipNotifier.GlipJobProperty;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration migrator for migrating the Glip plugin configuration for a
 * {@link Job} from the 1.8 format to the 2.0 format. It does so by removing the
 * GlipJobProperty from the job properties (if there is one).
 * 
 * <p>
 * GlipJobProperty settings are usually migrated to a publisher, but there are
 * no publishers in a Job so the settings are lost. For this reason, <strong>be
 * careful of how you use this migrator.</strong>.
 * </p>
 */
@SuppressWarnings("deprecation")
public class JobConfigMigrator {

    private static final Logger logger = Logger.getLogger(JobConfigMigrator.class.getName());

    public void migrate(final Job<?, ?> job) {

        logger.info(String.format("Migrating job \"%s\" with type %s", job.getName(), job
                .getClass().getName()));

        final GlipJobProperty slackJobProperty = job.getProperty(GlipJobProperty.class);

        if (slackJobProperty == null) {
            logger.info(String.format(
                    "Configuration is already up to date for \"%s\", skipping migration",
                    job.getName()));
            return;
        }

        try {
            // property section is not used anymore - remove
            job.removeProperty(GlipJobProperty.class);
            job.save();
            logger.info(String.format("Configuration for \"%s\" updated successfully",
                    job.getName()));
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
