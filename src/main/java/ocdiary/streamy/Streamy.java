package ocdiary.streamy;

import com.google.common.collect.Maps;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import ocdiary.streamy.util.ImageUtil;
import ocdiary.streamy.util.StreamInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;

@Mod(
        modid = Streamy.MODID,
        name = Streamy.NAME,
        version = Streamy.VERSION,
        acceptedMinecraftVersions = Streamy.MCVERSIONS,
        dependencies = Streamy.DEPENDENCIES,
        clientSideOnly = true
)
public class Streamy {

    public static final String MODID = "streamy";
    public static final String NAME = "Streamy";
    public static final String VERSION = "@VERSION@"; //This is replaced in the build.gradle
    public static final String MCVERSIONS = "[1.12,1.13)";
    public static final String DEPENDENCIES = "forge@[14.21.1.2387,)";

    public static final Logger LOGGER = LogManager.getLogger(MODID);
    /**
     * All streamers that are currently live
     * <b>keys must be all lowercase!</b>
     */
    public static final Map<String, StreamInfo> LIVE_STREAMERS = Maps.newConcurrentMap();
    public static boolean isLive, isSelfStreaming, isIconDismissed;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ImageUtil.init();
        ClientRegistry.registerKeyBinding(KeyHandler.keyDismiss);
        if (StreamyConfig.GENERAL.enabled) StreamHandler.startStreamChecker();
    }

    /**
     * Gets the specified config element so that it can be changed
     * Throws an exception if none found
     */
    public static IConfigElement getConfig(String configPath) {
        IConfigElement config = getConfig(ConfigElement.from(StreamyConfig.class), configPath.split("\\."), 0);
        if(config == null) throw new RuntimeException(String.format("No config found for path %s!", configPath));
        return config;
    }

    //Recursive method to find the config
    private static IConfigElement getConfig(IConfigElement element, String[] path, int level) {
        String name = path[level];
        if(element.getName().equalsIgnoreCase(name)) {
            if (element.isProperty())
                return element;
            else if (level < path.length) {
                int nextLevel = level + 1;
                for (IConfigElement e : element.getChildElements()) {
                    IConfigElement result = getConfig(e, path, nextLevel);
                    if(result != null) return result;
                }
            }
        }
        return null;
    }

    /**
     * Post the config changed events
     * This will cause the configs to be saved to file and the StreamyConfig class to be updated
     */
    public static void configChanged(@Nullable String configID, boolean isWorldRunning, boolean requiresMcRestart) {
        ConfigChangedEvent event = new ConfigChangedEvent.OnConfigChangedEvent(MODID, configID, isWorldRunning, requiresMcRestart);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.getResult().equals(Event.Result.DENY))
            MinecraftForge.EVENT_BUS.post(new ConfigChangedEvent.PostConfigChangedEvent(MODID, configID, isWorldRunning, requiresMcRestart));
    }
}