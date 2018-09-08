package mdct.streamy.streams.impl.twitch.messages;

/**
 * Created by bright_spark on 08/09/2018.
 */
public class SubscriptionMessage {
    public String user_name,
            display_name,
            channel_name,
            user_id,
            channel_id,
            time,
            sub_plan,
            sub_plan_name,
            context,
            recipient_id,
            recipient_user_name,
            recipient_display_name;
    public Integer months;
    public SubMessage sub_message;

    public class SubMessage {
        public String message;
        public Emote[] emotes;

        public class Emote {
            public Integer start, end, id;
        }
    }
}
