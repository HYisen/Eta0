package net.alexhyisen.eta.core;

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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.ImmediateEventExecutor;
import net.alexhyisen.Config;
import net.alexhyisen.Utility;
import net.alexhyisen.Web;
import net.alexhyisen.eta.book.Book;
import net.alexhyisen.eta.book.Source;
import net.alexhyisen.eta.sale.Task;
import net.alexhyisen.eta.website.Overseer;
import net.alexhyisen.log.LogCls;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private Web web;
    private Overseer overseer;

    private SslContext sslContext = null;//null for HTTPS disabled situation

    private void init() {
        Source source = new Source();
        source.load(Paths.get("sourceAll"));
        data = source.getData();
        try {
            web = new Web();
            web.load();
        } catch (IOException e) {
            Utility.log(LogCls.LOOP, "failed to load web resources");
            e.printStackTrace();
        }
        try {
            var path = Path.of(".", "www", "site");
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
            overseer = new Overseer(path);
        } catch (IOException e) {
            Utility.log(LogCls.LOOP, "failed to init overseer");
            e.printStackTrace();
        }

//        data.forEach(Book::open);
//        System.out.println("all books are opened.");

        if ("true".equals(Config.getFromDefault("enableHttps"))) {
            Utility.log(LogCls.LOOP, "Enable HTTPS");

            File key = Paths.get(Config.getFromDefault("tlsKeyPath")).toFile();
            File pem = Paths.get(Config.getFromDefault("tlsPemPath")).toFile();

            try {
                sslContext = SslContextBuilder.forServer(pem, key).build();
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }

            //No provider succeeded to generate a self-signed certificate.
//            try {
//                SelfSignedCertificate ssc = new SelfSignedCertificate();
//                sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
//            } catch (CertificateException | SSLException e) {
//                e.printStackTrace();
//            }
        }
    }

    ChannelFuture start(InetSocketAddress address) {
        init();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        if (sslContext != null) {
                            channel.pipeline().addLast(sslContext.newHandler(channel.alloc()));
                        }
                        channel.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new ChunkedWriteHandler())
                                .addLast(new HttpObjectAggregator(65536))
                                .addLast(new RestfulRequestHandler("api",
                                        Config.getFromDefault("adminUsername"),
                                        Config.getFromDefault("adminPassword")))
                                .addLast(new HttpRequestHandler("/ws", data, web))
                                .addLast("ws", new WebSocketServerProtocolHandler("/ws"))
                                .addLast(new TextWebSocketFrameHandler(data, jobs, web, overseer, channelGroup, self));
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
        Utility.log(LogCls.LOOP, "ChannelGroup cleaned.");
        eventGroup.shutdownGracefully();
        Utility.log(LogCls.INFO, "final");
        Utility.shutdownGlobally();
    }
}
