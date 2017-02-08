package jenkins.plugins.glip.webhook.model;

import hudson.model.Cause;


public class GlipWebhookCause extends Cause {

    public GlipWebhookCause() {super();}

    @Override
    public String getShortDescription() {
        return "Build started by via GlipWebhookPlugin";
    }
}
