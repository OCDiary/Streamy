package mdct.streamy.streams.impl.twitch.messages;

/**
 * Created by bright_spark on 08/09/2018.
 */
public class TwitchMessage {
    public String type;
    public String nonce;
    public String error;
    public Data data;

    public class Data {
        public String topic;
        public String message;
    }
}
