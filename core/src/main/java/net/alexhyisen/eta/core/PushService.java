package net.alexhyisen.eta.core;

import io.netty.channel.ChannelFuture;
import net.alexhyisen.Config;
import net.alexhyisen.Utility;

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
        Integer port = Integer.valueOf(config.get("port"));
        Utility.log(Utility.LogCls.INFO, "port = " + port.toString());
        ChannelFuture future = new NettyServer().start(new InetSocketAddress(port));
        future.syncUninterruptibly();
        Utility.log(Utility.LogCls.INFO, "launched in " + Duration.between(timestamp, Instant.now()).toMillis() + " ms");
    }

    public static void main(String[] args) {
        new PushService().run();
    }
}
