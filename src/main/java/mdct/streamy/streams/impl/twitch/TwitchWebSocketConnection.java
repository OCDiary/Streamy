package mdct.streamy.streams.impl.twitch;

import com.google.gson.Gson;
import mdct.streamy.Streamy;
import mdct.streamy.streams.impl.twitch.messages.BitsMessage;
import mdct.streamy.streams.impl.twitch.messages.SubscriptionMessage;
import mdct.streamy.streams.impl.twitch.messages.TwitchMessage;
import mdct.streamy.streams.impl.twitch.messages.TwitchMessageRequest;
import mdct.streamy.util.AbstractWebsocketConnection;
import net.minecraft.util.StringUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by bright_spark on 26/08/2018.
 */
public class TwitchWebSocketConnection extends AbstractWebsocketConnection {
    private static final Gson GSON = new Gson();
    private static final String TOPIC_BITS = "channel-bits-events-v1.%s";
    private static final String TOPIC_SUBSCRIPTIONS = "channel-subscribe-events-v1.%s";

    private final String channelId;
    private final String authToken;

    private TwitchStreamingHandler handler = null;

    public TwitchWebSocketConnection(String channelId, String authToken) {
        super("wss://pubsub-edge.twitch.tv");
        //Send heartbeat every 4 minutes (+ jitter) with a 10 second timeout
        setupHeartbeat("{\"type\": \"PING\"}", 240, 10, 2000);
        this.channelId = channelId;
        this.authToken = authToken;
    }

    public void setHandler(TwitchStreamingHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void onConnect() {
        sendMessage(new TwitchMessageRequest("LISTEN", authToken, getTopic(TOPIC_BITS), getTopic(TOPIC_SUBSCRIPTIONS)));
    }

    @Override
    protected boolean isHeartbeatResponse(String message) {
        TwitchMessage twitchMessage = GSON.fromJson(message, TwitchMessage.class);
        return twitchMessage != null && twitchMessage.type.equals("PONG");
    }

    @Override
    public void handleMessage(String message) {
        TwitchMessage twitchMessage = GSON.fromJson(message, TwitchMessage.class);
        switch (twitchMessage.type.toLowerCase(Locale.ROOT)) {
            case "reconnect":
                scheduleReconnect(30, TimeUnit.SECONDS);
                break;
            case "response":
                String error = twitchMessage.error;
                if(StringUtils.isNullOrEmpty(error))
                    Streamy.LOGGER.info("Received successful listen response");
                else
                    Streamy.LOGGER.error("Received error from listen response: %s", error);
                break;
            case "message":
                String[] topicArray = twitchMessage.data.topic.split(".");

                //Make sure the channelId in the message is valid
                if(!channelId.equals(topicArray[1]))
                    Streamy.LOGGER.error("Received message for an incorrect channel: %s", message);

                switch (topicArray[0]) {
                    case TOPIC_BITS:
                        BitsMessage bitsMessage = GSON.fromJson(twitchMessage.data.message, BitsMessage.class);
                        handler.onBitsMessage(bitsMessage);
                        break;
                    case TOPIC_SUBSCRIPTIONS:
                        SubscriptionMessage subscriptionMessage = GSON.fromJson(twitchMessage.data.message, SubscriptionMessage.class);
                        handler.onSubscriptionMessage(subscriptionMessage);
                        break;
                    default:
                        Streamy.LOGGER.error("Received message with an unknown topic: %s", twitchMessage.data.topic);
                }
                break;
        }
    }

    private String getTopic(String topic) {
        return String.format(topic, channelId);
    }

    private void sendMessage(TwitchMessageRequest message) {
        sendMessage(GSON.toJson(message));
    }
}
