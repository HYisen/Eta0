package net.alexhyisen.eta.mail;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Alex on 2017/4/3.
 * A client based on Netty.
 */
class NettyClient implements Client {
    private static class NettyClientHandler extends SimpleChannelInboundHandler<String> {
        private BlockingQueue<String> lines;

        NettyClientHandler(BlockingQueue<String> lines) {
            this.lines = lines;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) {
            //Utility.log("Server:"+s);
            lines.add(s);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            Utility.log("exception caught in NettyClientHandler");
            ctx.close();
        }
    }

    private static class NettyClientInitializer extends ChannelInitializer<SocketChannel> {
        private static final StringDecoder DECODER = new StringDecoder();
        private static final StringEncoder ENCODER = new StringEncoder();

        private final NettyClientHandler handler;

        NettyClientInitializer(BlockingQueue<String> lines) {
            handler = new NettyClientHandler(lines);
        }

        @Override
        protected void initChannel(SocketChannel socketChannel) {
            ChannelPipeline pipeline = socketChannel.pipeline();

            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            pipeline.addLast(DECODER);
            pipeline.addLast(ENCODER);

            pipeline.addLast(handler);
        }
    }

    private BlockingQueue<String> lines = new LinkedBlockingQueue<>();
    //A BlockingQueue is used to simulate the behavior of Base Client, which blocks the thread when nothing in queue.

    private EventLoopGroup group;
    private Channel channel;
    private ChannelFuture lastWrite;

    @Override
    public void link(String host, int port) throws IOException {
        group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new NettyClientInitializer(lines));

            channel = bootstrap.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            throw new IOException(e);//just a relay to fit the Interface.
        }
    }

    @Override
    public void send(String content) {
        if (lastWrite != null) {
            try {
                lastWrite.sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }//finish the last sending first if available.

        Utility.log(LogCls.MAIL, "client: " + content);
        lastWrite = channel.writeAndFlush(content + "\r\n");
    }

    @Override
    public String receive() throws IOException {
        try {
            String line = lines.take();
            Utility.log(LogCls.MAIL, "server: " + line);
            return line;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (lastWrite != null) {
                lastWrite.sync();
            }
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            throw new IOException(e);//also a relay
        }
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        Client.smokeTest(new NettyClient());
    }
}
