package ocdiary.twitchy;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Twitchy.MODID, name = Twitchy.NAME, version = Twitchy.VERSION, acceptedMinecraftVersions = Twitchy.AVERSION, clientSideOnly = true)
public class Twitchy {

    public static final String MODID = "twitchy";
    public static final String NAME = "Twitchy";
    public static final String VERSION = "@VERSION@"; //This is replaced in the build.gradle
    public static final String AVERSION = "[1.12, 1.12.2]";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static boolean isLive = false;
    public static String streamGame, streamTitle, streamPreview;
    public static int streamViewers;
    public static int previewWidth, previewHeight;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        TwitchHandler.startStreamChecker();
    }

}