package net.alexhyisen.eta.model.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Chapter;

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
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private static final String TEXT_DELIMITER = "\n";

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
                    ctx.writeAndFlush(new TextWebSocketFrame("accept request to " + arg));
                    String[] args = arg.split("\\.");
                    retriever.submit(() -> {
                        Book book = data.get(Integer.valueOf(args[0]));
                        if (!book.isOpened()) {
                            book.open();
                        }
                        Chapter chapter = book.getChapters().get(Integer.valueOf(args[1]));
                        String title = book.getName() + " " + chapter.getName();
                        System.out.println("get  " + title);
                        String content = Arrays.stream(chapter.getData()).collect(Collectors.joining(TEXT_DELIMITER));
                        ctx.writeAndFlush(new TextWebSocketFrame(title + TEXT_DELIMITER + content));
                        System.out.println("send " + title);
                    });
                    break;
                case "ls":
                    if (".".equals(arg)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Books").append(TEXT_DELIMITER);
                        for (int k = 0; k != data.size(); ++k) {
                            sb.append(String.format("%d 《%s》", k, data.get(k).getName())).append(TEXT_DELIMITER);
                        }
                        ctx.writeAndFlush(new TextWebSocketFrame(sb.toString()));
                    } else {
                        Book book = data.get(Integer.valueOf(arg));
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format("Chapters in 《%s》",book.getName())).append(TEXT_DELIMITER);
                        if (!book.isOpened()) {
                            book.open();
                        }
                        for (int k = 0; k != book.getChapters().size(); ++k) {
                            sb
                                    .append(arg)
                                    .append(String.format(".%d %s", k, book.getChapters().get(k).getName()))
                                    .append(TEXT_DELIMITER);
                        }
                        ctx.writeAndFlush(new TextWebSocketFrame(sb.toString()));
                    }
                    break;
                default:
                    group.writeAndFlush(msg.retain());
            }
        }
    }
}

