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
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.ImmediateEventExecutor;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Source;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by Alex on 2017/5/28.
 * A Server to transit data through WebSocket based on netty.
 */
class NettyServer {
    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    private List<Book> data;

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
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new ChunkedWriteHandler())
                                .addLast(new HttpObjectAggregator(65536))
                                .addLast(new HttpRequestHandler("/ws",data))
                                .addLast(new CloseWebSocketFrameHandler(channelGroup))
                                .addLast(new WebSocketServerProtocolHandler("/ws"))
                                .addLast(new TextWebSocketFrameHandler(data, channelGroup))
                                .addLast(new ChapterEncoder())
                                .addLast(new BookEncoder())
                                .addLast(new ShelfEncoder());
                    }
                });
        return bootstrap.bind(address);
    }
}
