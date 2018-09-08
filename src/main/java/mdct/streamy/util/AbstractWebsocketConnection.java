package mdct.streamy.util;

import mdct.streamy.Streamy;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by bright_spark on 08/09/2018.
 */
public abstract class AbstractWebsocketConnection {
    private final String host;
    private final int port;
    private final Object outLock = new Object();
    private final Random random = new Random();

    private Socket socket;
    private Executor readExecutor = Executors.newSingleThreadExecutor();
    private PrintWriter out;
    private boolean reconnect = true;
    private boolean reconnectScheduled = false;

    private String heartbeatMessageSend;
    private int heartbeatDelaySecs, heartbeatTimeoutMillis, heartbeatJitter;
    private ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private long lastHeartbeatSent, lastHeartbeatReceived;

    public AbstractWebsocketConnection(String host) {
        this(host, 0);
    }

    public AbstractWebsocketConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    protected void setupHeartbeat(String messageSend, int delaySecs, int timeoutSecs, int jitterMillis) {
        heartbeatMessageSend = messageSend;
        heartbeatDelaySecs = delaySecs;
        heartbeatTimeoutMillis = timeoutSecs * 1000;
        heartbeatJitter = jitterMillis;
    }

    public void connect() {
        if (isConnected())
            //If already connected, then do nothing
            return;
        else
            //If not properly connected, then reconnect
            disconnect(true);

        int reconnectBackoffSecs = 1;

        //Connect to the websocket
        //Just to bypass the current reconnection state to get into the loop
        boolean wasReconnect = reconnect;
        reconnect = true;
        while (reconnect) {
            reconnect = wasReconnect;

            try {
                socket = SSLSocketFactory.getDefault().createSocket();
                //Timeout for reading - 0 means it will never time out
                socket.setSoTimeout(0);
                socket.connect(new InetSocketAddress(host, port), 30000);
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            } catch (IOException e) {
                Streamy.LOGGER.error("Error connecting to " + host + " on port " + port, e);
            }

            if (!isConnected())
                return;

            //Start sending heartbeats
            if (heartbeatMessageSend != null) {
                heartbeatExecutor.scheduleWithFixedDelay(new Heartbeat(), heartbeatDelaySecs, heartbeatDelaySecs, TimeUnit.SECONDS);
                heartbeatExecutor.scheduleAtFixedRate(new HeartbeatCheck(), 2, 2, TimeUnit.SECONDS);
            }

            onConnect();

            //Listen for messages
            readExecutor.execute(new ReadHandler());

            //Disconnect and reconnect if needed
            disconnect(reconnect);

            if (reconnect) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(reconnectBackoffSecs));
                } catch (InterruptedException ignored) {}
                reconnectBackoffSecs = Math.min(120, reconnectBackoffSecs * 2);
            }
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    protected abstract void onConnect();

    /**
     * Checks if the received message is a heartbeat response
     */
    protected abstract boolean isHeartbeatResponse(String message);

    /**
     * Handles received messages apart from heartbeats
     */
    protected abstract void handleMessage(String message);

    /**
     * Send a message through the websocket
     */
    public void sendMessage(String message) {
        CompletableFuture.runAsync(() -> {
            synchronized (outLock) {
                Streamy.LOGGER.info("Sending to %s:\n%s", host, message);
                out.write(message);
            }
        });
    }

    /**
     * Disconnect the websocket
     */
    public void disconnect(boolean reconnect) {
        this.reconnect = reconnect;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                socket = null;
            }
        }
        if (out != null) {
            out.close();
            out = null;
        }
        if (readExecutor != null) {
            readExecutor = null;
        }
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdownNow();
        }
        lastHeartbeatSent = 0;
        lastHeartbeatReceived = 0;
    }

    /**
     * Disconnects the websocket and the reconnects after the delay specified
     */
    public void scheduleReconnect(long delay, TimeUnit unit) {
        if(reconnectScheduled)
            return;
        reconnectScheduled = true;
        disconnect(false);
        new Thread(new Reconnect(delay, unit)).start();
    }

    private class Heartbeat implements Runnable {
        @Override
        public void run() {
            if (!isConnected())
                return;
            sendMessage(heartbeatMessageSend);
            lastHeartbeatSent = System.currentTimeMillis();
            if (heartbeatJitter > 0) {
                try {
                    //Sleep for a short "jitter"
                    Thread.sleep((long) random.nextInt(heartbeatJitter));
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private class HeartbeatCheck implements Runnable {
        @Override
        public void run() {
            if (!isConnected())
                return;
            //If we don't get a heartbeat reply within the timeout, reconnect the websocket
            if (lastHeartbeatReceived < lastHeartbeatSent && System.currentTimeMillis() - lastHeartbeatSent > heartbeatTimeoutMillis) {
                disconnect(true);
            }
        }
    }

    private class Reconnect implements Runnable {
        private final long delay;
        private final TimeUnit unit;

        public Reconnect(long delay, TimeUnit unit) {
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        public void run() {
            Streamy.LOGGER.info("Scheduling reconnection for host %s after %s %s", host, delay, unit.toString().toLowerCase(Locale.ROOT));
            try {
                unit.sleep(delay);
            } catch (InterruptedException e) {
                Streamy.LOGGER.error("Reconnect thread interrupted for host " + host + "!", e);
            }
            connect();
            reconnectScheduled = false;
        }
    }

    private class ReadHandler implements Runnable {
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message = in.readLine();
                Streamy.LOGGER.info("Received from %s:\n%s", host, message);
                if (isHeartbeatResponse(message))
                    lastHeartbeatReceived = System.currentTimeMillis();
                else
                    handleMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
