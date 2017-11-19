package ocdiary.twitchy.registry;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import ocdiary.twitchy.Twitchy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber
public class TCEvents {
    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    public static int DELAY = Twitchy.interval;
    public static String CHANNEL = Twitchy.twitchChannelId;

    @SubscribeEvent
    public static void onLogin(FMLNetworkEvent.ClientConnectedToServerEvent event)
    {
        executorService.scheduleAtFixedRate(new TCCheck(CHANNEL), 5, DELAY, TimeUnit.SECONDS);
        System.out.println("The thread is now running");
    }

    @SubscribeEvent
    public static void onLogout(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        executorService.shutdown();
    }

    /*@SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event)
    {
        //if(event.phase == TickEvent.Phase.END && event.player.worldObj.getWorldTime() % 20 == 0)
            //System.out.println(twitchy.isLive);
    }*/
}
