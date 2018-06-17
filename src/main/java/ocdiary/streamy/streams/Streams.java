package ocdiary.streamy.streams;

import ocdiary.streamy.util.StreamInfo;

import java.util.HashMap;
import java.util.Map;

public class Streams {

    private static final Map<StreamSource, Stream> STREAMS = new HashMap<>();

    static {
        //TODO: Add other streams
        STREAMS.put(StreamSource.TWITCH, new TwitchStream());
    }

    public static Stream getStream(StreamInfo info)
    {
        return getStream(info.source);
    }

    public static Stream getStream(StreamSource source)
    {
        return STREAMS.get(source);
    }
}
