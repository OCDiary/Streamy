package ocdiary.twitchy;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ocdiary.twitchy.util.EnumStreamerMode;
import ocdiary.twitchy.util.ImageUtil;
import ocdiary.twitchy.util.StreamInfo;
import ocdiary.twitchy.util.StreamerUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class StreamChecker implements Runnable {

    private static final String CLIENT_ID = "n3w3pptkwczocn9gw2r8pbjfe76xzr";

    @Override
    public void run() {
        boolean live = false;
        String player = StreamerUtil.getPlayerStreamerName().toLowerCase(Locale.ROOT);
        synchronized (Twitchy.LIVE_STREAMERS) {
            Twitchy.LIVE_STREAMERS.clear();
            ImageUtil.clearPreviewCache();

            List<String> streamers = Lists.newArrayList(TwitchyConfig.CHANNELS.channels);

            if (TwitchyConfig.GENERAL.streamerMode != EnumStreamerMode.OFF && !streamers.contains(player)) {
                try {
                    JsonObject streamData = getJsonFromAPI("streams", player);
                    if (streamData != null) {
                        StreamInfo playerInfo = createStreamInfoFromJson(streamData, player);
                        Twitchy.isSelfStreaming = playerInfo != null && playerInfo.streaming;
                        if (TwitchyConfig.GENERAL.streamerMode != EnumStreamerMode.PARTIAL && playerInfo.streaming)
                            addStreamer(player, playerInfo);
                    }
                } catch (Exception e) {
                    Twitchy.LOGGER.error("Error getting stream info for channel \"" + player + "\"", e);
                }
            }

            for (String broadcaster : streamers) {
                try {
                    //Get stream info
                    JsonObject streamData = getJsonFromAPI("streams", broadcaster);
                    if (streamData == null) continue;
                    StreamInfo info = createStreamInfoFromJson(streamData, broadcaster);
                    if(info != null) {
                        //only show live icon if not in streamer mode
                        if (info.streaming && (TwitchyConfig.GENERAL.streamerMode == EnumStreamerMode.OFF || !broadcaster.equalsIgnoreCase(player)))
                            live = true;
                        addStreamer(broadcaster.toLowerCase(Locale.ROOT), info);
                    }
                } catch (Exception e) {
                    Twitchy.LOGGER.error("Error getting stream info for channel \"" + broadcaster + "\"", e);
                }
            }
        }
        Twitchy.isLive = live;
    }

    private void addStreamer(String name, StreamInfo streamInfo) {
        //Show the icon if it's dismissed and a stream just went live
        if (Twitchy.isIconDismissed && streamInfo.streaming) {
            StreamInfo currentInfo = Twitchy.LIVE_STREAMERS.get(name);
            if (currentInfo != null && !currentInfo.streaming) {
                Twitchy.isIconDismissed = false;
            }
        }
        Twitchy.LIVE_STREAMERS.put(name, streamInfo);
    }

    private StreamInfo createStreamInfoFromJson(JsonObject streamJson, String broadcaster) throws Exception {
        JsonElement streamElement = streamJson.getAsJsonObject().get("stream");
        StreamInfo info;
        if (!streamElement.isJsonNull()) {
            JsonObject stream = streamElement.getAsJsonObject();
            String game = getJsonString(stream.get("game"));
            int viewerCount = getJsonInt(stream.get("viewers"));
            JsonObject channelInfo = stream.get("channel").getAsJsonObject();
            String title = getJsonString(channelInfo.get("status"));
            String broadcasterName = getJsonString(channelInfo.get("display_name"));
            String profilePic = getJsonString(channelInfo.get("logo"));
            String preview = getJsonString(stream.get("preview").getAsJsonObject().get(TwitchyConfig.PREVIEW.quality.getKey()))
                    .replace("{width}", String.valueOf(TwitchyConfig.PREVIEW.quality.width))
                    .replace("{height}", String.valueOf(TwitchyConfig.PREVIEW.quality.height));
            info = new StreamInfo(broadcasterName, game, title, preview, profilePic, viewerCount);
        } else {
            JsonObject channelData = getJsonFromAPI("channels", broadcaster);
            if (channelData == null) return null;
            String broadcasterName = getJsonString(channelData.get("display_name"));
            String profilePic = getJsonString(channelData.get("logo"));
            info = new StreamInfo(broadcasterName, profilePic);
        }
        return info;
    }

    private JsonObject getJsonFromAPI(String api, String broadcaster) throws Exception {
        String url = String.format("https://api.twitch.tv/kraken/%s/%s?client_id=%s", api, broadcaster, CLIENT_ID);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();

        //handle http errors to not clutter the log
        if (code != HttpStatus.SC_OK) {
            Twitchy.LOGGER.error("Unable to get streamer info on API {} for {}, Status Code {}: \"{}\"", api, broadcaster, String.valueOf(code), connection.getResponseMessage());
            connection.disconnect();
            return null;
        }

        Scanner sc = new Scanner(connection.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) sb.append(sc.nextLine());
        String json = sb.toString();
        connection.disconnect();
        return new JsonParser().parse(json).getAsJsonObject();
    }

    private static String getJsonString(JsonElement element) {
        return element == null ? StringUtils.EMPTY : element.getAsString();
    }

    private static int getJsonInt(JsonElement element) {
        return element == null ? -1 : element.getAsInt();
    }
}
