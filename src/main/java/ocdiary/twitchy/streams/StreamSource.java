package ocdiary.twitchy.streams;

public enum StreamSource {
    TWITCH("twitch"),
    MIXER("mixer"),
    YOUTUBE("youtube");

    private final String name;

    StreamSource(String name) {
        this.name = name;
    }

    public static StreamSource getSource(String name) {
        for (StreamSource source : values())
            if (source.name.equalsIgnoreCase(name))
                return source;
        return null;
    }
}
