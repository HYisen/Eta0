package net.alexhyisen.eta.model.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.ImmediateEventExecutor;
import net.alexhyisen.eta.model.Signer;
import net.alexhyisen.eta.model.Utility;
import net.alexhyisen.eta.model.catcher.Book;
import net.alexhyisen.eta.model.catcher.Source;
import net.alexhyisen.eta.model.smzdm.Task;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alex on 2017/5/28.
 * A Server to transit data through WebSocket based on netty.
 */
class NettyServer implements Closeable {
    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    private List<Book> data;
    private List<Task> jobs = new ArrayList<>();
    private NettyServer self = this;

    private void init() {
        Source source = new Source();
        source.load(Paths.get("sourceAll"));
        data = source.getData();
//        data.forEach(Book::open);
//        System.out.println("all books are opened.");
    }

    ChannelFuture start(InetSocketAddress address) {
        init();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new ChunkedWriteHandler())
                                .addLast(new HttpObjectAggregator(65536))
                                .addLast(new HttpRequestHandler("/ws", data))
                                .addLast("ws", new WebSocketServerProtocolHandler("/ws"))
                                .addLast(new TextWebSocketFrameHandler(data, jobs, channelGroup, self));
                    }
                });
        return bootstrap.bind(address);
    }

    @Override
    public void close() throws IOException {
        try {
            channelGroup.close().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        Utility.log(Utility.LogCls.LOOP, "ChannelGroup cleaned.");
        eventGroup.shutdownGracefully();
        Utility.log(Utility.LogCls.INFO, "final");
    }

    @SuppressWarnings("unused")
    private static void makeSecure(Channel channel) {
        PrivateKey key;
        try {
            key = Signer.load(Paths.get("priKey"));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            channel.pipeline().addFirst("ssl", new SslHandler(
                    SslContextBuilder.forServer(key).build().newEngine(channel.alloc())));
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }
}
