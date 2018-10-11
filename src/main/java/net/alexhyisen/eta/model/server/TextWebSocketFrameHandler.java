package net.alexhyisen.eta.model.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.alexhyisen.eta.model.Utility;
import net.alexhyisen.eta.model.catcher.Book;
import net.alexhyisen.eta.model.catcher.Chapter;
import net.alexhyisen.eta.model.catcher.Source;
import net.alexhyisen.eta.model.smzdm.Task;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Alex on 2017/5/28.
 * where the data service is provided through TextFrame
 */
class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private List<Book> data;
    private List<Task> jobs;
    private final ChannelGroup group;
    //a singleton retriever ExecutorService used to do the disk IO jobs.
    private static ExecutorService retriever = Executors.newSingleThreadExecutor();

    TextWebSocketFrameHandler(List<Book> data, List<Task> jobs, ChannelGroup group) {
        this.data = data;
        this.jobs = jobs;
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
            String[] args;
            switch (cmd) {
                case "get":
                    //ctx.writeAndFlush(new TextWebSocketFrame("accept request to " + arg));
                    args = arg.split("\\.");
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
                case "Start-Task":
                    args = arg.split("_");
                    System.out.println(args.length);
                    if (args.length != 4) {
                        ctx.writeAndFlush(new TextWebSocketFrame(
                                "hint: 'Start-Task Keywords_IntervalSecs_MinPrize_MaxPrize'"));
                    } else {
                        var task = new Task(args[0], Long.valueOf(args[2]), Long.valueOf(args[3]));
                        jobs.add(task);
                        Long intervalSecs = Long.valueOf(args[1]);
                        task.start(intervalSecs);
                        ctx.writeAndFlush(new TextWebSocketFrame(String.format(
                                "started %4d | %s @ %d", jobs.size() - 1, task, intervalSecs)));
                    }
                    break;
                case "Stop-Task":
                    if ("*".equals(arg)) {
                        int size = jobs.size();
                        ctx.writeAndFlush(new TextWebSocketFrame("stop all " + size + (size > 1 ? " tasks" : "task")));
                        jobs.forEach(Task::stop);
                    } else {
                        try {
                            int id = Integer.valueOf(arg);
                            jobs.get(id).stop();
                            jobs.remove(id);
                        } catch (NumberFormatException e) {
                            ctx.writeAndFlush(new TextWebSocketFrame("bad input (NumberFormatException)"));
                        } catch (IndexOutOfBoundsException e) {
                            ctx.writeAndFlush(new TextWebSocketFrame("bad input (IndexOutOfBoundsException)"));
                        }//A wise Linux user shall not input bad input, so catch clauses are not designed to execute.
                    }
                    break;
                default:
                    group.writeAndFlush(msg.retain());
            }
        } else {
            switch (text) {
                case "Get-Task":
                    if (jobs.isEmpty()) {
                        ctx.writeAndFlush(new TextWebSocketFrame("No task is running."));
                    } else {
                        ctx.writeAndFlush(new TextWebSocketFrame(
                                IntStream.range(0, jobs.size())
                                        .mapToObj(v -> String.format("%4d | %s", v, jobs.get(v).toString()))
                                        .collect(Collectors.joining("\n"))
                        ));
                    }
                    break;
                case "reload":
                    //Possible duplicates in NettyService::init, shall be merged into one.
                    Source source = new Source(Paths.get("sourceAll"));
                    source.load();
                    data.clear();//Remember, data doesn't belong to this.
                    data.addAll(source.getData());
                    ctx.writeAndFlush(new TextWebSocketFrame("reloaded"));
                default:
                    group.writeAndFlush(msg.retain());
            }
        }
    }
}

