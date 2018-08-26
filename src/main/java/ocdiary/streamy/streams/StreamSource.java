package ocdiary.streamy.streams;

public enum StreamSource {
    TWITCH("twitch", new TwitchStream());
    //MIXER("mixer"),
    //YOUTUBE("youtube");

    private final String name;
    private final Stream stream;

    StreamSource(String name, Stream stream) {
        this.name = name;
        this.stream = stream;
    }

    public static StreamSource getSource(String name) {
        for (StreamSource source : values())
            if (source.name.equalsIgnoreCase(name))
                return source;
        return null;
    }

    public Stream getStream() {
        return stream;
    }
}
