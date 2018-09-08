package mdct.streamy.streams.impl.twitch;

import mdct.streamy.streams.impl.twitch.messages.BitsMessage;
import mdct.streamy.streams.impl.twitch.messages.SubscriptionMessage;

/**
 * Created by bright_spark on 08/09/2018.
 */
public class TwitchStreamingHandler {
    private String channelId;
    private TwitchWebSocketConnection connection;

    public TwitchStreamingHandler(String channelId) {
        this.channelId = channelId;
    }

    public void connect() {
        if (connection != null)
            return;
        //TODO: Sort out OAuth
        connection = new TwitchWebSocketConnection(channelId, "TODO");
        connection.setHandler(this);
        connection.connect();
    }

    public void onBitsMessage(BitsMessage message) {

    }

    public void onSubscriptionMessage(SubscriptionMessage message) {

    }
}
