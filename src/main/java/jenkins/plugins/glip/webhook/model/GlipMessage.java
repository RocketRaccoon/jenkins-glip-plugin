package jenkins.plugins.glip.webhook.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.*;

/**
 * Created by andrey.smirnov on 17.01.2017.
 */

@Data
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class GlipMessage {

    private String icon;
    private String activity;
    private String title;
    private String body;

    public GlipMessage(@NonNull String body) {
        this(null, null, null, body);
    }

    public String getJsonAsString() {
        ObjectMapper mapper = new ObjectMapper();
        Gson g = new Gson();
        return g.toJson(this);
    }

    public static GlipMessage getGlipMessageFromJson(String json) {
        Gson g = new Gson();
        return g.fromJson(json, GlipMessage.class);
    }
}
