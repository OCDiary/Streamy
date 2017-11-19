package ocdiary.twitchy.registry;

import net.minecraftforge.fml.common.Mod;
import ocdiary.twitchy.Twitchy;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

@Mod.EventBusSubscriber
public class TCConfig {
    private static Configuration config;

    public static void init(File file){
        config = new Configuration(file);
        syncConfig();
    }

    private static void syncConfig(){

        String CHANNEL = "channel";
        Twitchy.twitchChannelId = config.getString("channel", CHANNEL, Twitchy.twitchChannelId, "Please enter your Twitch channel here: ");
        Twitchy.interval = config.getInt("interval", CHANNEL, Twitchy.interval, 1, 28000000, "Please enter the delay time (in seconds) for checking the Twitch channel live status: ");

        String ICON = "icon";
        Twitchy.persistantIcon = config.getBoolean("persistantIcon", ICON, true, "Should the Twitch icon be always shown on screen (true), or only when channel is live (false)?");
        //refs.posX = config.getInt("posX", ICON, refs.posX, 1,500, "Twitch icon 'x' position: ");
        //refs.posY = config.getInt("posY", ICON,  refs.posY, 1,500, "Twitch icon 'y' position: ");
        Twitchy.tIconSize = config.getInt("tIconSize", ICON, Twitchy.tIconSize, 1, 3,"Change the twitch icon size; 1 = small, 2 = medium, 3 = big:");

        if(config.hasChanged())
        config.save();
    }

    @SubscribeEvent
    public static void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if(event.getModID().equalsIgnoreCase(Twitchy.MODID))
            syncConfig();
    }
}
