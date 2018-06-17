package ocdiary.streamy;

import ocdiary.streamy.streams.Stream;
import ocdiary.streamy.streams.StreamSource;
import ocdiary.streamy.streams.Streams;
import ocdiary.streamy.util.EnumStreamerMode;
import ocdiary.streamy.util.ImageUtil;
import ocdiary.streamy.util.StreamInfo;
import ocdiary.streamy.util.StreamerUtil;

import java.util.Locale;

public class StreamChecker implements Runnable {

    @Override
    public void run() {
        boolean live = false;
        String player = StreamerUtil.getPlayerStreamerName().toLowerCase(Locale.ROOT);
        synchronized (Streamy.LIVE_STREAMERS) {
            Streamy.LIVE_STREAMERS.clear();
            ImageUtil.clearPreviewCache();

            for (String broadcaster : StreamyConfig.CHANNELS.channels) {
                try {
                    //Get stream info
                    String[] split = StreamerUtil.splitChannelConfig(broadcaster);
                    if (split == null)
                        Streamy.LOGGER.error("Broadcaster %s doesn't have a source!", broadcaster);
                    else {
                        //Extract the source
                        StreamSource source = StreamSource.getSource(split[0]);
                        if (source == null)
                            Streamy.LOGGER.error("Broadcaster %s has an invalid source '%s'!", broadcaster, split[0]);
                        else {
                            Stream stream = Streams.getStream(source);
                            //Extract the channel
                            StreamInfo info = stream.getStreamInfo(split[1]);
                            if (split[1].equals(player)) {
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
