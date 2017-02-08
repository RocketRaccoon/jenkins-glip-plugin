package jenkins.plugins.glip.webhook;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
@Getter
@Setter
public class GlobalConfig extends GlobalConfiguration {

    private String webhookUrl = "https://hooks.glip.com/webhook/";

    public GlobalConfig() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest sr, JSONObject json) throws FormException {
        sr.bindJSON(this, json);
        save();
        return true;
    }

    public FormValidation doCheckWebhookUrl(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty())
            return FormValidation.warning("Please set a Glip Weebhook URL");

        return FormValidation.ok();
    }

}
