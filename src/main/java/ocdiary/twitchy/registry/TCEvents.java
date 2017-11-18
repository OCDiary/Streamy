package ocdiary.twitchy.registry;


import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import ocdiary.twitchy.Twitchy;


//@Mod.EventBusSubscriber
public class TCEvents {
    private static Thread streamThread;
    public static int DELAY = Twitchy.interval;
    public static String CHANNEL = Twitchy.twitchChannelId;

    @SubscribeEvent
    public void onLogin(FMLNetworkEvent.ClientConnectedToServerEvent event)
    {
        start();
        System.out.println("The thread is now running");
    }

    @SubscribeEvent
    public void onLogout(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        stop();
    }

    /*@SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event)
    {
        //if(event.phase == TickEvent.Phase.END && event.player.worldObj.getWorldTime() % 20 == 0)
            //System.out.println(twitchy.isLive);
    }*/

    public static void start()
    {
        if(streamThread == null || !streamThread.isAlive())
        {
            streamThread = new Thread(new TCCheck(CHANNEL, DELAY), "StreamChecker");
            streamThread.start();
        }
    }

    public static void stop()
    {
        if(streamThread != null && streamThread.isAlive())
            streamThread.interrupt();
    }


}
