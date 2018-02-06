package ocdiary.twitchy;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Twitchy.MODID)
@Config.LangKey(Twitchy.MODID + ".config.title")
public class TCConfig {
    @Config.Comment("Twitch channel configs")
    public static final Channel channel = new Channel();

    @Config.Comment("In-game icon configs")
    public static final Icon icon = new Icon();

    public static class Channel {
        @Config.Comment("Please enter your Twitch channel here:")
        public String twitchChannelId = "OCDiary";

        @Config.Comment("Please enter the time (in seconds) between checking the Twitch channel status:")
        @Config.RangeInt(min = 1)
        public int interval = 30;
    }

    public static class Icon {
        @Config.Comment("Should the Twitch icon be always shown on screen (true), or only when channel is live (false)?")
        public boolean persistantIcon = true;

        @Config.Comment("Change the twitch icon size; 1 = small, 2 = medium, 3 = big:")
        @Config.RangeInt(min = 1, max = 3)
        public int iconSize = 3;

        @Config.Comment("Icon X position (from the left)")
        public int posX = 1;

        @Config.Comment("Icon Y position (from the top)")
        public int posY = 1;
    }

    @Mod.EventBusSubscriber(modid = Twitchy.MODID)
    private static class Handler {
        private static String prevTwitchChannelId = channel.twitchChannelId;
        private static int prevInterval = channel.interval;
        private static int prevIconSize = icon.iconSize;

        @SubscribeEvent
        public static void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
            if(event.getModID().equalsIgnoreCase(Twitchy.MODID)) {
                ConfigManager.sync(Twitchy.MODID, Config.Type.INSTANCE);

                if(event.isWorldRunning() && (!channel.twitchChannelId.equals(prevTwitchChannelId) || channel.interval != prevInterval)) {
                    prevTwitchChannelId = channel.twitchChannelId;
                    prevInterval = channel.interval;
                    Twitchy.LOGGER.info("Channel config changed - restarting stream checker...");
                    TCEvents.startStreamChecker();
                }
                if(icon.iconSize != prevIconSize) {
                    prevIconSize = icon.iconSize;
                    TCDrawScreen.updateIconSize();
                }
            }
        }
    }
}
