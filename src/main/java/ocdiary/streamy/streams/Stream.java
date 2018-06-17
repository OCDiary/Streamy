package ocdiary.streamy.streams;

import ocdiary.streamy.util.StreamInfo;

public interface Stream {

    StreamInfo getStreamInfo(String channel) throws Exception;

    void openStreamURL(String channel);
}
