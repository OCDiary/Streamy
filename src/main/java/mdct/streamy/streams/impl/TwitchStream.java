package mdct.streamy.streams.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mdct.streamy.Streamy;
import mdct.streamy.StreamyConfig;
import mdct.streamy.streams.Stream;
import mdct.streamy.streams.StreamSource;
import mdct.streamy.util.StreamInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

public class TwitchStream implements Stream {
    private static final String CLIENT_ID = "n3w3pptkwczocn9gw2r8pbjfe76xzr";
    private static final String URL = "https://twitch.tv/";
    private static final String API = "https://api.twitch.tv/kraken/%s/%s?client_id=" + CLIENT_ID;

    @Override
    public StreamInfo getStreamInfo(String channel) throws Exception {
        JsonObject streamData = getJsonFromAPI("streams", channel);
        return streamData == null ? null : createStreamInfoFromJson(streamData, channel);
    }

    @Override
    public void openStreamURL(String channel) {
        String url = URL + channel;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Streamy.LOGGER.error("Can't open browser - Desktop not supported!");
            }
        } catch (URISyntaxException e) {
            Streamy.LOGGER.error("URL \"" + url + "\" is invalid. Report this to the mod author!");
        } catch (Exception e) {
            Streamy.LOGGER.error("Can't open browser", e);
        }
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
            String preview = getJsonString(stream.get("preview").getAsJsonObject().get(StreamyConfig.PREVIEW.quality.getKey()))
                    .replace("{width}", String.valueOf(StreamyConfig.PREVIEW.quality.width))
                    .replace("{height}", String.valueOf(StreamyConfig.PREVIEW.quality.height));
            info = new StreamInfo(StreamSource.TWITCH, broadcasterName, game, title, preview, profilePic, viewerCount);
        } else {
            JsonObject channelData = getJsonFromAPI("channels", broadcaster);
            if (channelData == null) return null;
            String broadcasterName = getJsonString(channelData.get("display_name"));
            String profilePic = getJsonString(channelData.get("logo"));
            info = new StreamInfo(StreamSource.TWITCH, broadcasterName, profilePic);
        }
        return info;
    }

    private JsonObject getJsonFromAPI(String api, String broadcaster) throws Exception {
        String url = String.format(API, api, broadcaster);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();

        //handle http errors to not clutter the log
        if (code != HttpStatus.SC_OK) {
            Streamy.LOGGER.error("Unable to get streamer info on API {} for {}, Status Code {}: \"{}\"", api, broadcaster, String.valueOf(code), connection.getResponseMessage());
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
