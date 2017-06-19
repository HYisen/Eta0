package net.alexhyisen.eta.model.server;

import io.netty.channel.ChannelFuture;

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
        ChannelFuture future = new NettyServer().start(new InetSocketAddress(4444));
        future.syncUninterruptibly();
        System.out.println("launched in "+ Duration.between(timestamp,Instant.now()).toMillis()+" ms");
    }

    public static void main(String[] args) {
        new PushService().run();
    }
}
