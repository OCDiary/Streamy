package ocdiary.twitchy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.Scanner;

public class StreamCheck implements Runnable {

    private static final String CLIENT_ID = "n3w3pptkwczocn9gw2r8pbjfe76xzr";

    @Override
    public void run()
    {
        TCDrawScreen.invalidatePreviewCache();
        try {
            String url = "https://api.twitch.tv/kraken/streams/" + TCConfig.CHANNELS.channel + "?client_id=" + CLIENT_ID;
            Scanner sc = new Scanner(new URL(url).openStream());
            StringBuilder sb = new StringBuilder();
            while(sc.hasNextLine())
                sb.append(sc.nextLine());
            String json = sb.toString();
            JsonObject jsonData = new JsonParser().parse(json).getAsJsonObject();
            JsonElement streamElement = jsonData.get("stream");
            boolean isLive = !streamElement.isJsonNull();
            Twitchy.isLive = isLive;
            if(!isLive)
            {
                Twitchy.streamGame = "";
                Twitchy.streamViewers = 0;
                Twitchy.streamTitle = "";
            }
            else
            {
                JsonObject stream = streamElement.getAsJsonObject();
                Twitchy.streamGame = getJsonString(stream.get("game"));
                Twitchy.streamViewers = getJsonInt(stream.get("viewers"));
                Twitchy.streamTitle = getJsonString(stream.get("channel").getAsJsonObject().get("status"));
                Twitchy.streamPreview = getJsonString(stream.get("preview").getAsJsonObject().get("medium"));
            }
        } catch (Exception e) {
            Twitchy.LOGGER.error("Error getting stream info", e);
        }
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
