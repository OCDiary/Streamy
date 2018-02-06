package ocdiary.twitchy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ocdiary.twitchy.Twitchy;

import java.net.URL;
import java.util.Scanner;

public class TCCheck implements Runnable {
    private String streamLink;

    public TCCheck(String channel)
    {
        streamLink = "https://api.twitch.tv/kraken/streams/" + channel + "?client_id=n3w3pptkwczocn9gw2r8pbjfe76xzr";
    }

    @Override
    public void run()
    {
        String json = "";
        try {
            Scanner sc = new Scanner(new URL(streamLink).openStream());
            StringBuilder sb = new StringBuilder();
            while(sc.hasNextLine())
                sb.append(sc.nextLine());
            json = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(json);

        JsonObject jsonData = new JsonParser().parse(json).getAsJsonObject();
        JsonObject stream = jsonData.get("stream").getAsJsonObject();
        Twitchy.isLive = stream != null;
        if(stream != null)
        {
            Twitchy.streamGame = getJsonString(stream.get("game"), "");
            Twitchy.streamViewers = getJsonInt(stream.get("viewers"), 0);
            Twitchy.streamTitle = getJsonString(stream.get("channel").getAsJsonObject().get("status"), "");
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
