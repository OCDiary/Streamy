package ocdiary.twitchy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StreamHandler {

    private static ScheduledExecutorService executorService = null;
    private static final Object threadLock = new Object();

    public static void startStreamChecker() {
        synchronized (threadLock) {
            stopStreamChecker();
            Twitchy.LOGGER.info("Starting Twitch stream status checker...");
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(new StreamChecker(), 0, TwitchyConfig.CHANNELS.interval, TimeUnit.SECONDS);
        }
    }

    public static void stopStreamChecker() {
        synchronized (threadLock) {
            if (isCheckerRunning()) {
                Twitchy.LOGGER.info("Stopping Twitch stream status checker...");
                executorService.shutdown();
            }
        }
    }

    public static boolean isCheckerRunning() {
        return executorService != null && !executorService.isShutdown();
    }

}
