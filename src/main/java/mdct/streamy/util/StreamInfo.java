package mdct.streamy.util;

import mdct.streamy.streams.StreamSource;

/**
 * @author UpcraftLP
 */
public class StreamInfo {
    public final StreamSource source;
    public final String broadcaster, game, title, previewUrl, profilePicUrl;
    public final int viewers;
    public final boolean streaming;

    //Used when a streamer is offline
    public StreamInfo(StreamSource source, String broadcaster, String profilePic) {
        this(source, broadcaster, null, null, null, profilePic, 0);
    }

    //Used when a streamer is online
    public StreamInfo(StreamSource source, String broadcaster, String game, String title, String preview, String profilePic, int viewers) {
        this.source = source;
        this.broadcaster = broadcaster;
        this.game = game;
        this.title = title;
        this.previewUrl = preview;
        this.profilePicUrl = profilePic;
        this.viewers = viewers;
        this.streaming = title != null;
    }
}
