package mdct.streamy.streams;

import mdct.streamy.util.StreamInfo;

public interface Stream {

    StreamInfo getStreamInfo(String channel) throws Exception;

    void openStreamURL(String channel);
}
