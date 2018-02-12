package ocdiary.twitchy;

import com.google.common.collect.Maps;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import ocdiary.twitchy.util.StreamInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.Map;

@Mod(modid = Twitchy.MODID, name = Twitchy.NAME, version = Twitchy.VERSION, acceptedMinecraftVersions = Twitchy.AVERSION, clientSideOnly = true)
public class Twitchy {

    public static final String MODID = "twitchy";
    public static final String NAME = "Twitchy";
    public static final String VERSION = "@VERSION@"; //This is replaced in the build.gradle
    public static final String AVERSION = "[1.12, 1.12.2]";

    public static final Logger LOGGER = LogManager.getLogger(MODID);
    /**
     * All streamers that are currently live
     * <b>keys must be all lowercase!</b>
     */
    public static final Map<String, StreamInfo> LIVE_STREAMERS = Maps.newConcurrentMap();
    public static boolean isLive, isSelfStreaming, isIconDismissed;
    public static KeyBinding keyDismiss = new KeyBinding("key.twitchy.dismiss", Keyboard.KEY_Z, "key.twitchy.category");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(keyDismiss);
        if (TwitchyConfig.GENERAL.enabled) StreamHandler.startStreamChecker();
    }

}