package jenkins.plugins.glip.webhook;


import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.plugins.glip.webhook.model.GlipMessage;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

import java.util.List;


public class ListProjectsCommand extends GlipRouterCommand implements RouterCommand<GlipMessage> {

    public ListProjectsCommand(GlipMessage message) {
        super(message);
    }

    @Override
    public GlipMessage execute(String... args) {

        SecurityContext ctx = ACL.impersonate(ACL.SYSTEM);

        String response = "*Projects:*\n";

        List<AbstractProject> jobs =
            Jenkins.getInstance().getAllItems(AbstractProject.class);

        SecurityContextHolder.setContext(ctx);

        for (AbstractProject job : jobs) {
            if (job.isBuildable()) {
                AbstractBuild lastBuild = job.getLastBuild();
                String buildNumber = "TBD";
                String status = "TBD";
                if (lastBuild != null) {

                    buildNumber = Integer.toString(lastBuild.getNumber());

                    if (lastBuild.isBuilding()) {
                        status = "BUILDING";
                    }

                    Result result = lastBuild.getResult();

                    if (result != null) {
                        status = result.toString();
                    }
                }

                if (jobs.size() <= 10) {
                    response += ">*"+job.getDisplayName() + "*\n>*Last Build:* #"+buildNumber+"\n>*Status:* "+status;
                    response += "\n\n\n";
                } else {
                    response += ">*"+job.getDisplayName() + "* :: *Last Build:* #"+buildNumber+" :: *Status:* "+status;
                    response += "\n\n";
                }
            }
        }

        if (jobs == null || jobs.size() == 0)
            response += ">_No projects found_";

        return new GlipMessage(response);
    }
}

