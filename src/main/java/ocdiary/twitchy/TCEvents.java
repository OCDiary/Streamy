package ocdiary.twitchy;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = Twitchy.MODID)
public class TCEvents {
    private static ScheduledExecutorService executorService;

    public static void startStreamChecker() {
        if(executorService != null) executorService.shutdown();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new TCCheck(), 1, TCConfig.channel.interval, TimeUnit.SECONDS);
        Twitchy.LOGGER.info("Twitch stream status checker is now running");
    }

    @SubscribeEvent
    public static void onLogin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        startStreamChecker();
    }

    @SubscribeEvent
    public static void onLogout(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if(executorService != null)
            executorService.shutdown();
    }
}
