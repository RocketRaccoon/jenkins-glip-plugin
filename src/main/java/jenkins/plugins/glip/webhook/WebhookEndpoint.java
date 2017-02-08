package jenkins.plugins.glip.webhook;


import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import jenkins.model.GlobalConfiguration;
import jenkins.plugins.glip.webhook.exception.CommandRouterException;
import jenkins.plugins.glip.webhook.exception.RouteNotFoundException;
import jenkins.plugins.glip.webhook.model.GlipMessage;
import jenkins.plugins.glip.webhook.model.JsonResponse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;


@Extension
public class WebhookEndpoint implements UnprotectedRootAction {

    private GlobalConfig globalConfig;

    private static final Logger LOGGER =
        Logger.getLogger(WebhookEndpoint.class.getName());

    public WebhookEndpoint() {
        globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);
    }

    @Override
    public String getUrlName() {
        return globalConfig.getWebhookUrl();
    }

    @RequirePOST
    public HttpResponse doIndex(StaplerRequest req) throws IOException,
        ServletException {

        GlipMessage message = new GlipMessage("");
        req.bindParameters(message);

        String commandText = message.getBody();
        if (commandText == null || commandText.isEmpty())
            return new JsonResponse(new GlipMessage("Invalid command, body field required"),
                StaplerResponse.SC_OK);

        CommandRouter<GlipMessage> router =
            new CommandRouter<GlipMessage>();

        try {
            router.addRoute("^list projects",
                "TODO"+" list projects",
                "Return a list of buildable projects",
                new ListProjectsCommand(message))
            .addRoute("^run ([\\p{L}\\p{N}\\p{ASCII}\\W]+)",
                    "TODO"+" run <project_name>",
                "Schedule a run for <project_name>",
                new ScheduleJobCommand(message))
            .addRoute("^get ([\\p{L}\\p{N}\\p{ASCII}\\W]+) #([0-9]+) log",
                    "TODO"+" get <project-name> #<build_number> log",
                "Return a truncated log for build #<build_number> of <project_name>",
                new GetProjectLogCommand(message));

            GlipMessage msg = router.route(commandText);

            return new JsonResponse(msg, StaplerResponse.SC_OK);
            
        } catch (RouteNotFoundException ex) {

            LOGGER.warning(ex.getMessage());

            String command = ex.getRouteCommand();

            String response = "*Help:*\n";
            if (command.split("\\s+").length > 1)
                response += "`"+command+"` _is an unknown command, try one of the following:_\n\n";
            else
                response += "\n";

            for (CommandRouter.Route route : router.getRoutes()) {
                response += "`"+route.command+"`\n```"+route.commandDescription+"```";
                response += "\n\n";
            }

            return new JsonResponse(new GlipMessage(response), StaplerResponse.SC_OK);

        } catch (CommandRouterException ex) {
            LOGGER.warning(ex.getMessage());
            return new JsonResponse(new GlipMessage(ex.getMessage()), StaplerResponse.SC_OK);

        } catch (Exception ex) {
            LOGGER.warning(ex.getMessage());
            return new JsonResponse(new GlipMessage("An error occured: "+ ex.getMessage()), StaplerResponse.SC_OK);
        }
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }
}
