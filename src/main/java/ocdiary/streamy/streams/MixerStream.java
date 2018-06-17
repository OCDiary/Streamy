package ocdiary.streamy.streams;

import ocdiary.streamy.util.StreamInfo;

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
