package net.alexhyisen.eta.model.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Chapter;
import net.alexhyisen.eta.model.Utility;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2017/5/28.
 * where the data service is provided through TextFrame
 */
class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private List<Book> data;
    private final ChannelGroup group;
    //a singleton retriever ExecutorService used to do the disk IO jobs.
    private static ExecutorService retriever = Executors.newSingleThreadExecutor();

    TextWebSocketFrameHandler(List<Book> data, ChannelGroup group) {
        this.data = data;
        this.group = group;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            System.out.println("HandshakeComplete");
            ctx.pipeline().remove(HttpRequestHandler.class);
            ctx.channel().writeAndFlush(new TextWebSocketFrame("Welcome.\nThis is an ExpServer.\nHave a nice day!"));
            group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " joined."));
            group.add(ctx.channel());
            ctx.channel().pipeline()
                    .addBefore("ws", "wsClose", new CloseWebSocketFrameHandler(group));
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    static final String TEXT_DELIMITER = "\n";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String text = msg.text();
        System.out.println("read text " + text);

        int index = text.indexOf(' ');
        if (index != -1) {
            String cmd = text.substring(0, index);
            String arg = text.substring(index + 1);
            switch (cmd) {
                case "get":
                    //ctx.writeAndFlush(new TextWebSocketFrame("accept request to " + arg));
                    String[] args = arg.split("\\.");
                    retriever.submit(() -> {
                        Book book = data.get(Integer.valueOf(args[0]));
                        if (!book.isOpened()) {
                            book.open();
                        }
                        Chapter chapter = book.getChapters().get(Integer.valueOf(args[1]));
                        ctx.writeAndFlush(new TextWebSocketFrame(new Envelope(chapter).toJson()));
                    });
                    break;
                case "ls":
                    if (".".equals(arg)) {
                        ctx.writeAndFlush(new TextWebSocketFrame(new Envelope(data).toJson()));
                    } else {
                        Book book = data.get(Integer.valueOf(arg));
                        ctx.writeAndFlush(new TextWebSocketFrame(new Envelope(book).toJson()));
                    }
                    break;
                case "refresh":
                    //It's reasonable to set the procedure blocking.
                    //As no data retrieve request should be allowed when it's updating.
                    if ("*".equals(arg)) {
                        ctx.writeAndFlush(new TextWebSocketFrame("Refreshing all, please wait."));
                        long cnt = data.stream().parallel().peek(Book::open).count();
                        ctx.writeAndFlush(new TextWebSocketFrame("Refreshing of " + cnt + " completed"));
                        Utility.log("all data refreshed ");
                    } else {
                        Book book = data.get(Integer.valueOf(arg));
                        book.open();
                        ctx.writeAndFlush(new TextWebSocketFrame("Refreshing of " + book.getName() + " completed"));
                        Utility.log("book " + book.getName() + " refreshed ");
                    }
                    break;
                default:
                    group.writeAndFlush(msg.retain());
            }
        }
    }
}

