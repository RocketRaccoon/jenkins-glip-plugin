package jenkins.plugins.glip;

import jenkins.plugins.glip.webhook.model.GlipMessage;

/**
 * Created by andrey.smirnov on 17.01.2017.
 */
public interface GlipService {

    boolean publish(String body);

    boolean publish(GlipMessage message);

}
