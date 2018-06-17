package ocdiary.twitchy.streams;

import ocdiary.twitchy.util.StreamInfo;

public interface Stream {

    StreamInfo getStreamInfo(String channel) throws Exception;

    void openStreamURL(String channel);
}
