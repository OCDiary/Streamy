package ocdiary.twitchy;

import com.google.common.collect.Sets;
import ocdiary.twitchy.streams.Stream;
import ocdiary.twitchy.streams.StreamSource;
import ocdiary.twitchy.streams.TwitchStream;
import ocdiary.twitchy.util.EnumStreamerMode;
import ocdiary.twitchy.util.ImageUtil;
import ocdiary.twitchy.util.StreamInfo;
import ocdiary.twitchy.util.StreamerUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StreamChecker implements Runnable {

    private static final Map<StreamSource, Stream> STREAMS = new HashMap<>();

    static {
        //TODO: Add other streams
        STREAMS.put(StreamSource.TWITCH, new TwitchStream());
    }

    //TODO: Test this all works now
    @Override
    public void run() {
        boolean live = false;
        String player = StreamerUtil.getPlayerStreamerName().toLowerCase(Locale.ROOT);
        synchronized (Streamy.LIVE_STREAMERS) {
            Streamy.LIVE_STREAMERS.clear();
            ImageUtil.clearPreviewCache();

            Set<String> streamers = Sets.newHashSet(StreamyConfig.CHANNELS.channels);

            for (String broadcaster : streamers) {
                try {
                    //Get stream info
                    int index = broadcaster.indexOf(':');
                    if (index < 0)
                        Streamy.LOGGER.error("Broadcaster %s doesn't have a source!", broadcaster);
                    else {
                        //Extract the source
                        String sourceString = broadcaster.substring(0, index);
                        StreamSource source = StreamSource.getSource(sourceString);
                        if (source == null)
                            Streamy.LOGGER.error("Broadcaster %s has an invalid source '%s'!", broadcaster, sourceString);
                        else {
                            Stream stream = STREAMS.get(source);
                            //Extract the channel
                            String channelString = broadcaster.substring(index + 1);
                            StreamInfo info = stream.getStreamInfo(channelString);
                            if (channelString.equals(player)) {
                                Streamy.isSelfStreaming = info != null && info.streaming;
                                if (StreamyConfig.GENERAL.streamerMode != EnumStreamerMode.PARTIAL && info != null && info.streaming)
                                    addStreamer(player, info);
                            } else if (info != null) {
                                //only show live icon if not in streamer mode
                                if (info.streaming && (StreamyConfig.GENERAL.streamerMode == EnumStreamerMode.OFF || !broadcaster.equalsIgnoreCase(player)))
                                    live = true;
                                addStreamer(broadcaster.toLowerCase(Locale.ROOT), info);
                            }
                        }
                    }
                } catch (Exception e) {
                    Streamy.LOGGER.error("Error getting stream info for channel \"" + broadcaster + "\"", e);
                }
            }
        }
        Streamy.isLive = live;
    }

    private void addStreamer(String name, StreamInfo streamInfo) {
        //Show the icon if it's dismissed and a stream just went live
        if (Streamy.isIconDismissed && streamInfo.streaming) {
            StreamInfo currentInfo = Streamy.LIVE_STREAMERS.get(name);
            if (currentInfo != null && !currentInfo.streaming) {
                Streamy.isIconDismissed = false;
            }
        }
        Streamy.LIVE_STREAMERS.put(name, streamInfo);
    }
}
