package jenkins.plugins.glip;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import jenkins.plugins.glip.webhook.model.GlipMessage;
import lombok.NonNull;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrey.smirnov on 17.01.2017.
 */

public class StandardGlipService implements GlipService {

    private static final Logger logger = Logger.getLogger(StandardGlipService.class.getName());

    private List<String> webhookIds;
    private String webhookUrl;
    private String activity;
    private String icon;

    public StandardGlipService(@NonNull String webhookUrl, @NonNull String webhookIds) {
        super();
        this.webhookIds = Arrays.asList(webhookIds.split("[,; ]+"));
        this.webhookUrl = webhookUrl;
    }

    public StandardGlipService(@NonNull String webhookUrl, @NonNull String webhookIds, String icon, String activity) {
        super();
        this.webhookIds = Arrays.asList(webhookIds.split("[,; ]+"));
        this.webhookUrl = webhookUrl;
        this.activity = activity;
        this.icon = icon;
    }

    @Override
    public boolean publish(String body) {
        return publish(new GlipMessage(body));
    }

    //TODO
    @Override
    public boolean publish(GlipMessage message) {

        if (StringUtils.isEmpty(message.getIcon())) {
            message.setIcon(icon);
        }

        if (StringUtils.isEmpty(message.getActivity())) {
            message.setActivity(activity);
        }

        JSONObject json = new JSONObject();
        boolean result = true;
        for (String webhookId : webhookIds) {
            json
                    .put("icon", message.getIcon())
                    .put("activity", message.getActivity())
                    .put("title", message.getTitle())
                    .put("body", message.getBody());

            PostMethod post;
            String url;
            url =  webhookUrl + webhookId;
            post = new PostMethod(url);
            post.setRequestHeader("Content-Type", "application/json");
            try {
                post.setRequestEntity(new StringRequestEntity(message.getJsonAsString(), "application/json", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.ALL, "Error while encoding attachments: " + e.getMessage());
            }
            logger.fine("Posting: to " + url +  ": " + message.getJsonAsString());
            HttpClient client = getHttpClient();
            post.getParams().setContentCharset("UTF-8");
            try {
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if (responseCode != HttpStatus.SC_OK) {
                    logger.log(Level.WARNING, "Glip post may have failed. Response: " + response);
                    result = false;
                } else {
                    logger.info("Posting message succeeded");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to Glip", e);
                result = false;
            } finally {
                post.releaseConnection();
            }
        }
        return result;

    }

    protected HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        ProxyConfiguration proxy = Jenkins.getInstance().proxy;
        if (proxy != null) {
            client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            String username = proxy.getUserName();
            String password = proxy.getPassword();
            if (username != null && !"".equals(username.trim())) {
                client.getState().setProxyCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
            }
        }
        return client;
    }
}
