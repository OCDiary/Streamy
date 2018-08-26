package ocdiary.streamy;

import com.google.common.collect.Maps;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import ocdiary.streamy.util.ImageUtil;
import ocdiary.streamy.util.StreamInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (StreamyConfig.GENERAL.enabled)
            StreamHandler.startStreamChecker();
    }
}