package jenkins.plugins.glip.webhook;


import jenkins.plugins.glip.webhook.model.GlipMessage;

public abstract class GlipRouterCommand {
    private GlipMessage message;

    public GlipRouterCommand(GlipMessage message) {
        this.message = message;
    }

    public GlipMessage getMessage() {
        return this.message;
    }

    public void setMessage(GlipMessage message) {
        this.message = message;
    }
}
