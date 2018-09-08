package mdct.streamy.streams.impl.twitch.messages;

/**
 * Created by bright_spark on 08/09/2018.
 */
public class TwitchMessageRequest {
    public String type;
    public String nonce;
    public Data data;

    public TwitchMessageRequest(String type, String dataAuthToken, String... dataTopics) {
        this.type = type;
        data = new Data(dataAuthToken, dataTopics);
    }

    public class Data {
        public String[] topics;
        public String auth_token;

        public Data(String authToken, String... topics) {
            this.auth_token = authToken;
            this.topics = topics;
        }
    }
}
