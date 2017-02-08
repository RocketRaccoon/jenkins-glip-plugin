package jenkins.plugins.glip.webhook;


import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.plugins.glip.webhook.model.GlipMessage;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GetProjectLogCommand extends GlipRouterCommand implements RouterCommand<GlipMessage> {

    public GetProjectLogCommand(GlipMessage message) {
        super(message);
    }

    @Override
    public GlipMessage execute(String... args) {
        String projectName = args[0];
        String buildNumber = args[1];

        SecurityContext ctx = ACL.impersonate(ACL.SYSTEM);

        List<String> log = new ArrayList<String>();

        try {
            Project project =
                Jenkins.getInstance().getItemByFullName(projectName, Project.class);

            if (project == null)
                return new GlipMessage("Could not find project ("+projectName+")\n");

            AbstractBuild build =
                project.getBuildByNumber(Integer.parseInt(buildNumber));

            if (build == null)
                return new GlipMessage("Could not find build #"+buildNumber+" for ("+projectName+")\n");

            log = build.getLog(25);

        } catch (IOException ex) {
            return new GlipMessage("Error occured returning log: "+ex.getMessage());
        } finally {
            SecurityContextHolder.setContext(ctx);
        }

        String response = "*"+projectName+"* *#"+buildNumber+"*\n";
        response += "```";
        for (String line : log) {
            response += line + "\n";
        }
        response += "```";

        return new GlipMessage(response);
    }
}
