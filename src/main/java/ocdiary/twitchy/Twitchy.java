package ocdiary.twitchy;

import com.google.common.collect.Maps;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import ocdiary.twitchy.util.StreamInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@Mod(modid = Twitchy.MODID, name = Twitchy.NAME, version = Twitchy.VERSION, acceptedMinecraftVersions = Twitchy.AVERSION, clientSideOnly = true)
public class Twitchy {

    public static final String MODID = "twitchy";
    public static final String NAME = "Twitchy";
    public static final String VERSION = "@VERSION@"; //This is replaced in the build.gradle
    public static final String AVERSION = "[1.12, 1.12.2]";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static boolean isLive = false, isSelfStreaming;

    /**
     * All streamers that are currently live
     * <b>keys must be all lowercase!</b>
     */
    public static final Map<String, StreamInfo> LIVE_STREAMERS = Maps.newConcurrentMap();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if(TwitchyConfig.GENERAL.enabled) StreamHandler.startStreamChecker();
    }

}