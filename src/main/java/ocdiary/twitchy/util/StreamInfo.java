package ocdiary.twitchy.util;

/**
 * @author UpcraftLP
 */
public class StreamInfo {

    public final String broadcaster, game, title, previewUrl, profilePicUrl;
    public final int viewers;

    public StreamInfo(String broadcaster, String profilePic) {
        this(broadcaster, null, null, null, profilePic, 0);
    }

    public StreamInfo(String broadcaster, String game, String title, String preview, String profilePic, int viewers) {
        this.broadcaster = broadcaster;
        this.game = game;
        this.title = title;
        this.previewUrl = preview;
        this.profilePicUrl = profilePic;
        this.viewers = viewers;
    }
}
