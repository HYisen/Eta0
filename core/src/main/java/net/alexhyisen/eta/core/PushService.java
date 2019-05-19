package net.alexhyisen.eta.core;

import io.netty.channel.ChannelFuture;
import net.alexhyisen.Config;
import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;

/**
 * Created by Alex on 2017/5/17.
 * A service used to cooperate with Android client Eta1.
 */
public class PushService {
    public void run() {
        Instant timestamp = Instant.now();
        Config config = new Config();
        config.load();
        int port = Integer.parseInt(config.get("port"));
        Utility.log(LogCls.INFO, "port = " + port);
        ChannelFuture future = new NettyServer().start(new InetSocketAddress(port));
        future.syncUninterruptibly();
        Utility.log(LogCls.INFO, "launched in " + Duration.between(timestamp, Instant.now()).toMillis() + " ms");
    }

    public static void main(String[] args) {
        new PushService().run();
    }
}
