package p3psie.streamy.streams;

import p3psie.streamy.util.StreamInfo;

public interface Stream {

    StreamInfo getStreamInfo(String channel) throws Exception;

    void openStreamURL(String channel);
}
