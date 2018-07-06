package ocdiary.streamy;

import com.google.common.collect.Sets;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ocdiary.streamy.util.*;

import java.util.HashSet;
import java.util.Set;

@Config(modid = Streamy.MODID)
@Config.LangKey(Streamy.MODID + ".config.title")
public class StreamyConfig {
    @Config.Comment("General mod configs")
    public static final General GENERAL = new General();

    @Config.Comment("Twitch channel configs")
    public static final Channel CHANNELS = new Channel();

    @Config.Comment("In-game icon configs")
    public static final Icon ICON = new Icon();

    @Config.Comment("Stream preview configs")
    public static final Preview PREVIEW = new Preview();

    public static class General {

        @Config.Comment({
                "This acts as a global switch to disable the entire mod",
                "Useful if you are a content creator and want to disable this while streaming or recording"
        })
        public boolean enabled = true;

        @Config.Comment({
                "This is a config aimed at streamers. It will use your Minecraft username (unless overrided by the " +
                        "streamerModeNameOverride config) as your streamer name, and do the following depending on the config:",
                "OFF     -> Your own channel will always be displayed",
                "PARTIAL -> Your own channel will not be displayed if you're streaming",
                "FULL    -> Nothing will be displayed by this mod if you're streaming"
        })
        public EnumStreamerMode streamerMode = EnumStreamerMode.OFF;

        @Config.Comment("An override streamer name to use instead of your Minecraft username for the streamerMode config")
        public String streamerModeNameOverride = "";

        @Config.Comment("Enables the use of holding Alt and right clicking to toggle dismissing the icon")
        public boolean enableAltRightClickDismiss = true;
    }

    public static class Channel {

        //TODO: Update platforms in comment when we add support for more
        @Config.Comment({
                "The Twitch channels to monitor and show the status of",
                "Entries must be in the format '<platform>:<streamer>'",
                "Acceptable streaming platforms are:",
                "twitch"
        })
        public String[] channels = new String[]{"twitch:P3PSIE"};

        @Config.Comment("Whether the channels are sorted alphabetically")
        public boolean sortChannels = true;

        @Config.Comment("Whether to show offline channels")
        public boolean showOfflineChannels = true;

        @Config.Comment("The time (in seconds) between checking the Twitch channels' status")
        @Config.RangeInt(min = 5)
        public int interval = 30;

        @Config.Comment("Disable the number of viewers being displayed for a live stream")
        public boolean disableViewers = false;

        @Config.Comment("Disable the game being played being displayed for a live stream")
        public boolean disableGame = false;

        @Config.Comment("Disable the stream title being displayed for a live stream")
        public boolean disableTitle = false;
    }

    public static class Icon {

        @Config.Comment("Should the Twitch ICON always be shown on screen, or only when a channel is live?")
        public EnumIconVisibility iconState = EnumIconVisibility.ALWAYS;

        @Config.Comment("Change the twitch icon size")
        public EnumIconSize iconSize = EnumIconSize.MEDIUM;

        @Config.Comment("Icon X position (from the left)")
        @Config.RangeInt(min = 0)
        public int posX = 1;

        @Config.Comment("Icon Y position (from the top)")
        @Config.RangeInt(min = 0)
        public int posY = 1;

        @Config.Comment("The direction which the streamer list will expand in")
        public EnumDirection expandDirection = EnumDirection.DOWN;
    }

    public static class Preview {

        @Config.Comment("The quality of the previewUrl image")
        public EnumPreviewSize quality = EnumPreviewSize.LARGE;

        @Config.Comment("The width of the preview image")
        public int previewWidth = 320;

        @Config.Comment("The height of the preview image")
        public int previewHeight = 180;
    }

    @Mod.EventBusSubscriber(modid = Streamy.MODID)
    private static class Handler {

        @SubscribeEvent
        public static void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equalsIgnoreCase(Streamy.MODID)) {
                ConfigManager.sync(Streamy.MODID, Config.Type.INSTANCE);
                if (GENERAL.enabled) {
                    //Don't need to change anything if only the direction was changed
                    if (event.getConfigID() == null || !event.getConfigID().equals("expandDirection")) {
                        //Remove streamers that aren't in the config anymore
                        Set<String> toRemove = new HashSet<>();
                        Set<String> channels = Sets.newHashSet(CHANNELS.channels);
                        Streamy.LIVE_STREAMERS.keySet().forEach(s -> {
                            if (!channels.contains(s))
                                toRemove.add(s);
                        });
                        toRemove.forEach(Streamy.LIVE_STREAMERS::remove);
                        ImageUtil.clearCachesOf(toRemove);
                        //Restart stream checker
                        StreamHandler.startStreamChecker();
                    }
                } else {
                    StreamHandler.stopStreamChecker();
                    ImageUtil.clearPreviewCache();
                    Streamy.LIVE_STREAMERS.clear();
                    Streamy.isLive = false;
                }
            }
        }
    }
}
