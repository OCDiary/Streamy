package ocdiary.twitchy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URL;
import java.util.Scanner;

public class StreamCheck implements Runnable {
    private static final String clientId = "n3w3pptkwczocn9gw2r8pbjfe76xzr";

    @Override
    public void run()
    {
        String json;
        try {
            String url = "https://api.twitch.tv/kraken/streams/" + TCConfig.channel.twitchChannelId + "?client_id=" + clientId;
            Scanner sc = new Scanner(new URL(url).openStream());
            StringBuilder sb = new StringBuilder();
            while(sc.hasNextLine())
                sb.append(sc.nextLine());
            json = sb.toString();
        } catch (Exception e) {
            Twitchy.LOGGER.error("Failed getting stream info", e);
            return;
        }

        //Twitchy.LOGGER.info(json);

        JsonObject jsonData = new JsonParser().parse(json).getAsJsonObject();
        JsonElement streamElement = jsonData.get("stream");
        Twitchy.isLive = !streamElement.isJsonNull();
        if(!Twitchy.isLive)
        {
            Twitchy.streamGame = "";
            Twitchy.streamViewers = 0;
            Twitchy.streamTitle = "";
        }
        else
        {
            JsonObject stream = streamElement.getAsJsonObject();
            Twitchy.streamGame = getJsonString(stream.get("game"), "");
            Twitchy.streamViewers = getJsonInt(stream.get("viewers"), 0);
            Twitchy.streamTitle = getJsonString(stream.get("channel").getAsJsonObject().get("status"), "");
            Twitchy.streamPreview = getJsonString(stream.get("preview").getAsJsonObject().get("medium"), "");
            TCDrawScreen.updatePreview(Twitchy.streamPreview);
        }
    }

    private static String getJsonString(JsonElement element, String defaultValue)
    {
        return element == null ? defaultValue : element.getAsString();
    }

    private static int getJsonInt(JsonElement element, int defaultValue)
    {
        return element == null ? defaultValue : element.getAsInt();
    }
}
