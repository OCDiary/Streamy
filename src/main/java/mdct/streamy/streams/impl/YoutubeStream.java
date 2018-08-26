package p3psie.streamy.streams.impl;

import p3psie.streamy.streams.Stream;
import p3psie.streamy.util.StreamInfo;

//TODO: Youtube integration -> https://developers.google.com/youtube/v3/live/getting-started
public class YoutubeStream implements Stream {
    @Override
    public StreamInfo getStreamInfo(String channel) throws Exception {
        return null;
    }

    @Override
    public void openStreamURL(String channel) {

    }
}
