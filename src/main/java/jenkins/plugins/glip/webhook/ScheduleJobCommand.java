package jenkins.plugins.glip.webhook;


import hudson.model.Project;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.plugins.glip.webhook.model.GlipMessage;
import jenkins.plugins.glip.webhook.model.GlipWebhookCause;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;


public class ScheduleJobCommand extends GlipRouterCommand implements RouterCommand<GlipMessage> {

    public ScheduleJobCommand(GlipMessage message) {
        super(message);
    }

    @Override
    public GlipMessage execute(String... args) {

        String projectName = args[0];
        SecurityContext ctx = ACL.impersonate(ACL.SYSTEM);

        String response = "";

        Project project =
            Jenkins.getInstance().getItemByFullName(projectName, Project.class);

        try {
            if (project == null)
                return new GlipMessage("Could not find project ("+projectName+")\n");

            if (project.scheduleBuild(new GlipWebhookCause())) {
                return new GlipMessage("Build scheduled for project "+ projectName+"\n");
            } else {
                return new GlipMessage("Build not scheduled due to an issue with Jenkins");
            }
        } finally {
            SecurityContextHolder.setContext(ctx);
        }
    }
}
