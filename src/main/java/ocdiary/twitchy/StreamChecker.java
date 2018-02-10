package ocdiary.twitchy;

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
import java.util.Locale;
import java.util.Scanner;

public class StreamChecker implements Runnable {

    private static final String CLIENT_ID = "n3w3pptkwczocn9gw2r8pbjfe76xzr";

    @Override
    public void run()
    {
        boolean live = false;
        synchronized (Twitchy.LIVE_STREAMERS) {
            ImageUtil.invalidatePreviewCache();
            Twitchy.LIVE_STREAMERS.clear();
            for(String broadcaster : TwitchyConfig.CHANNELS.channels) {
                try {
                    //Get stream info
                    JsonObject streamData = getJsonFromAPI("streams", broadcaster);
                    if(streamData == null) continue;
                    JsonElement streamElement = streamData.getAsJsonObject().get("stream");
                    if(!streamElement.isJsonNull()) {
                        JsonObject stream = streamElement.getAsJsonObject();
                        String game = getJsonString(stream.get("game"));
                        int viewerCount = getJsonInt(stream.get("viewers"));
                        JsonObject channelInfo = stream.get("channel").getAsJsonObject();
                        String title = getJsonString(channelInfo.get("status"));
                        String broadcasterName = getJsonString(channelInfo.get("display_name"));
                        String profilePic = getJsonString(channelInfo.get("logo"));
                        String preview = getJsonString(stream.get("preview").getAsJsonObject().get(TwitchyConfig.quality.getKey())).replace("{width}", String.valueOf(TwitchyConfig.quality.width)).replace("{height}", String.valueOf(TwitchyConfig.quality.height));
                        Twitchy.LIVE_STREAMERS.put(broadcaster.toLowerCase(Locale.ROOT), new StreamInfo(broadcasterName, game, title, preview, profilePic, viewerCount));

                        //only show live icon if not in streamer mode
                        if (TwitchyConfig.streamerMode == EnumStreamerMode.OFF || !broadcaster.equalsIgnoreCase(StreamerUtil.getPlayerStreamerName())) live = true;
                    }
                    else
                    {
                        //Get channel info for the profile icon
                        JsonObject channelData = getJsonFromAPI("channels", broadcaster);
                        if(channelData == null) continue;
                        String broadcasterName = getJsonString(channelData.get("display_name"));
                        String profilePic = getJsonString(channelData.get("logo"));
                        Twitchy.LIVE_STREAMERS.put(broadcaster.toLowerCase(Locale.ROOT), new StreamInfo(broadcasterName, profilePic));
                    }
                } catch (Exception e) {
                    Twitchy.LOGGER.error("Error getting stream info for channel \"" + broadcaster + "\"", e);
                }
            }
        }
        Twitchy.isLive = live;
        String username = StreamerUtil.getPlayerStreamerName();
        if(Twitchy.LIVE_STREAMERS.containsKey(username)) {
            Twitchy.isSelfStreaming = Twitchy.LIVE_STREAMERS.get(username).streaming;
        }
        else Twitchy.isSelfStreaming = false;
    }

    private static JsonObject getJsonFromAPI(String api, String broadcaster) throws Exception
    {
        String url = String.format("https://api.twitch.tv/kraken/%s/%s?client_id=%s", api, broadcaster, CLIENT_ID);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();

        //handle http errors to not clutter the log
        if(code != HttpStatus.SC_OK) {
            Twitchy.LOGGER.error("Unable to get streamer info on API {} for {}, Status Code {}: \"{}\"", api, broadcaster, String.valueOf(code), connection.getResponseMessage());
            connection.disconnect();
            return null;
        }

        Scanner sc = new Scanner(connection.getInputStream());
        StringBuilder sb = new StringBuilder();
        while(sc.hasNextLine()) sb.append(sc.nextLine());
        String json = sb.toString();
        connection.disconnect();
        return new JsonParser().parse(json).getAsJsonObject();
    }

    private static String getJsonString(JsonElement element)
    {
        return element == null ? StringUtils.EMPTY : element.getAsString();
    }

    private static int getJsonInt(JsonElement element)
    {
        return element == null ? -1 : element.getAsInt();
    }
}
