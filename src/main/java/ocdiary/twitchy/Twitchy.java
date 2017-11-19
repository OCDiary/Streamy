package ocdiary.twitchy;

import ocdiary.twitchy.proxies.common;
import ocdiary.twitchy.registry.TCConfig;
import ocdiary.twitchy.registry.TCDrawScreen;
import ocdiary.twitchy.registry.TCEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Twitchy.MODID, name = Twitchy.NAME, version = Twitchy.VERSION, acceptedMinecraftVersions = Twitchy.AVERSION)
public class Twitchy {

    public static final String MODID = "twitchy";
    public static final String NAME = "Twitchy";
    public static final String VERSION = "1.0.1";
    public static final String AVERSION = "[1.12, 1.12.2]";

    public static final String CPROX = "ocdiary.twitchy.proxies.client";
    public static final String SPROX = "ocdiary.twitchy.proxies.common";

    public static String twitchChannelId = "ocdiary";
    public static boolean persistantIcon = true;
    public static int posX = 1;
    public static int posY = 1;
    public static int interval = 30;
    public static String iconSize = "big";
    public static int tIconSize = 3;

    public static boolean isLive = false;
    public static String streamGame, streamTitle;
    public static int streamViewers;

    @Instance
    private static Twitchy instance;

    @SidedProxy(clientSide = Twitchy.CPROX, serverSide = Twitchy.SPROX)
    private static common proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e){
        TCConfig.init(e.getSuggestedConfigurationFile());

        proxy.preInit(e);
    }

    @EventHandler
    public void init(FMLInitializationEvent e){
        proxy.init(e);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e){
        proxy.postInit(e);
    }
}