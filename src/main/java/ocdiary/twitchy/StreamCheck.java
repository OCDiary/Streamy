package ocdiary.twitchy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ocdiary.twitchy.util.ImageUtil;
import ocdiary.twitchy.util.StreamInfo;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.Scanner;

public class StreamCheck implements Runnable {

    private static final String CLIENT_ID = "n3w3pptkwczocn9gw2r8pbjfe76xzr";

    @Override
    public void run()
    {
        boolean live = false;
        synchronized (Twitchy.LIVE_STREAMERS) {
            ImageUtil.invalidatePreviewCache();
            Twitchy.LIVE_STREAMERS.clear();
            for(String broadcaster : TCConfig.CHANNELS.channels) {
                try {
                    String url = "https://api.twitch.tv/kraken/streams/" + broadcaster + "?client_id=" + CLIENT_ID;
                    Scanner sc = new Scanner(new URL(url).openStream());
                    StringBuilder sb = new StringBuilder();
                    while(sc.hasNextLine())
                        sb.append(sc.nextLine());
                    String json = sb.toString();
                    JsonObject jsonData = new JsonParser().parse(json).getAsJsonObject();
                    JsonElement streamElement = jsonData.get("stream");
                    if(!streamElement.isJsonNull())
                    {
                        live = true;
                        JsonObject stream = streamElement.getAsJsonObject();
                        String game = getJsonString(stream.get("game"));
                        int viewerCount = getJsonInt(stream.get("viewers"));
                        JsonObject channelInfo = stream.get("channel").getAsJsonObject();
                        String title = getJsonString(channelInfo.get("status"));
                        String broadcasterName = getJsonString(channelInfo.get("display_name"));
                        String logo = getJsonString(channelInfo.get("logo"));
                        String preview = getJsonString(stream.get("preview").getAsJsonObject().get(TCConfig.Quality.getKey())).replace("{width}", String.valueOf(TCConfig.Quality.width)).replace("{height}", String.valueOf(TCConfig.Quality.height));
                        Twitchy.LIVE_STREAMERS.put(broadcaster, new StreamInfo(broadcasterName, game, title, preview, logo, viewerCount));
                    }
                } catch (Exception e) {
                    Twitchy.LOGGER.error("Error getting stream info for channel \"" + broadcaster + "\"", e);
                }
            }
        }
        Twitchy.isLive = live;
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
