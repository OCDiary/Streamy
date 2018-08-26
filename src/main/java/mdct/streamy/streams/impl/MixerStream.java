package p3psie.streamy.streams.impl;

import p3psie.streamy.streams.Stream;
import p3psie.streamy.util.StreamInfo;

//TODO: Mixer integration -> https://dev.mixer.com/rest.html
public class MixerStream implements Stream {
    @Override
    public StreamInfo getStreamInfo(String channel) throws Exception {
        return null;
    }

    @Override
    public void openStreamURL(String channel) {

    }
}
