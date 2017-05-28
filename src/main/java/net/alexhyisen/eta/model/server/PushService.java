package net.alexhyisen.eta.model.server;

import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;

/**
 * Created by Alex on 2017/5/17.
 * A service used to cooperate with Android client Eta1.
 */
public class PushService {
    public void run() {
        ChannelFuture future = new NettyServer().start(new InetSocketAddress(4444));
        future.syncUninterruptibly();
        System.out.println("launched");
    }

    public static void main(String[] args) {
        new PushService().run();
    }
}
