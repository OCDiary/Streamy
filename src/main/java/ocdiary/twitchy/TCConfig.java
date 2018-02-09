package ocdiary.twitchy;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ocdiary.twitchy.util.EnumIconSize;
import ocdiary.twitchy.util.EnumPreviewSize;

@Config(modid = Twitchy.MODID)
@Config.LangKey(Twitchy.MODID + ".config.title")
public class TCConfig {
    @Config.Comment("Twitch channel configs")
    public static final ChannelConfig CHANNELS = new ChannelConfig();

    @Config.Comment("In-game icon configs")
    public static final Icon ICON = new Icon();

    @Config.Comment("The quality of the previewUrl image")
    public static EnumPreviewSize quality = EnumPreviewSize.LARGE;

    public static class ChannelConfig {

        @Config.Comment("Please enter your Twitch CHANNELS here:")
        public String[] channels = new String[]{"OCDiary"};

        @Config.Comment("Please enter the time (in seconds) between checking the Twitch CHANNELS status:")
        @Config.RangeInt(min = 5)
        public int interval = 30;
    }

    public static class Icon {

        enum State {
            ALWAYS,
            LIVE_ONLY
        }

        @Config.Comment("Should the Twitch ICON always be shown on screen, or only when a channel is live?")
        public State iconState = State.ALWAYS;

        @Config.Comment("Change the twitch icon size")
        public EnumIconSize iconSize = EnumIconSize.MEDIUM;

        @Config.Comment("Icon X position (from the left)")
        @Config.RangeInt(min = 0)
        public int posX = 1;

        @Config.Comment("Icon Y position (from the top)")
        @Config.RangeInt(min = 0)
        public int posY = 1;
    }

    @Mod.EventBusSubscriber(modid = Twitchy.MODID)
    private static class Handler {

        @SubscribeEvent
        public static void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
            if(event.getModID().equalsIgnoreCase(Twitchy.MODID)) {
                ConfigManager.sync(Twitchy.MODID, Config.Type.INSTANCE);
                TwitchHandler.startStreamChecker();
            }
        }
    }
}
