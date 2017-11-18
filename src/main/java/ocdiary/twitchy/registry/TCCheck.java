package ocdiary.twitchy.registry;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ocdiary.twitchy.Twitchy;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;


import java.net.URL;
import java.util.Scanner;


public class TCCheck implements Runnable {
    private String channel, streamLink;
    private int timeBetweenChecks;
    public boolean running = true;
    private String json;

    public TCCheck(String channel, int timeBetweenChecks)
    {
        this.channel = channel;
        streamLink = "https://api.twitch.tv/kraken/streams/" + channel + "?client_id=n3w3pptkwczocn9gw2r8pbjfe76xzr";
        this.timeBetweenChecks = timeBetweenChecks;
    }

    private void checkStream()
    {
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
    
    
    
    @Override
    public void run()
    {
        running = true;
        System.out.println("Checking stream: " + channel + " every " + timeBetweenChecks + " seconds");
        while(running)
        {
            checkStream();
            try
            {
                Thread.sleep(timeBetweenChecks * 1000);
            }
            catch(InterruptedException e)
            {
                System.out.println("Stream Checker Interrupted, Stopping Thread.");
                running = false;
            }
        }
    }

    private String addQuotes(String text)
    {
        return "\"" + text + "\"";
    }
}
