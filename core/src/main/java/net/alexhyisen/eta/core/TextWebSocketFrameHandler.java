package net.alexhyisen.eta.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.alexhyisen.Utility;
import net.alexhyisen.Web;
import net.alexhyisen.eta.book.Book;
import net.alexhyisen.eta.book.Chapter;
import net.alexhyisen.eta.book.Source;
import net.alexhyisen.eta.sale.Task;
import net.alexhyisen.log.LogCls;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Alex on 2017/5/28.
 * where the data service is provided through TextFrame
 */
class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private List<Book> data;
    private List<Task> jobs;
    private Web web;
    private final ChannelGroup group;
    private final Closeable shutdownHandler;
    //a singleton retriever ExecutorService used to do the disk IO jobs.
    private static ExecutorService retriever = Executors.newSingleThreadExecutor();

    private static Path JOBS_SAVE_PATH = Path.of(".", "jobs");


    TextWebSocketFrameHandler(List<Book> data, List<Task> jobs, Web web, ChannelGroup group, Closeable shutdownHandler) {
        this.data = data;
        this.jobs = jobs;
        this.web = web;
        this.group = group;
        this.shutdownHandler = shutdownHandler;
    }

    private static void manageTaskArg(ChannelHandlerContext ctx, String arg, List<Task> jobs) {
        String[] args;
        args = arg.split("_");
        Utility.log(LogCls.LOOP, "Start-Task expects 4 and get " + args.length);
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
            task.setArgument(arg);
        }
    }

//    static final String TEXT_DELIMITER = "\n";

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            Utility.log(LogCls.LOOP, "HandshakeComplete");
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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        String text = msg.text();
        Utility.log(LogCls.LOOP, "read text " + text);

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
                        data.stream().parallel().forEach(Book::open);
                        ctx.writeAndFlush(new TextWebSocketFrame("Refreshing of " + data.size() + " completed"));
                        Utility.log("all data refreshed ");
                    } else {
                        Book book = data.get(Integer.valueOf(arg));
                        book.open();
                        ctx.writeAndFlush(new TextWebSocketFrame("Refreshing of " + book.getName() + " completed"));
                        Utility.log("book " + book.getName() + " refreshed ");
                    }
                    break;
                case "Start-Task":
                    manageTaskArg(ctx, arg, jobs);
                    break;
                case "Stop-Task":
                    if ("*".equals(arg)) {
                        int size = this.jobs.size();
                        ctx.writeAndFlush(new TextWebSocketFrame("stop all " + Utility.genDesc(size, "task")));
                        this.jobs.forEach(Task::stop);
                    } else {
                        try {
                            int id = Integer.valueOf(arg);
                            this.jobs.get(id).stop();
                            this.jobs.remove(id);
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
                        IntStream.range(0, jobs.size())
                                .mapToObj(v -> String.format("%4d | %s", v, jobs.get(v).toString()))
                                .map(TextWebSocketFrame::new)
                                .forEach(ctx::writeAndFlush);
                    }
                    break;
                case "Resume-Task":
                    if (jobs.isEmpty()) {
                        if (Files.exists(JOBS_SAVE_PATH)) {
                            try {
                                Files.lines(JOBS_SAVE_PATH).forEach(v -> manageTaskArg(ctx, v, jobs));
                            } catch (IOException e) {
                                e.printStackTrace();
                                Utility.log(LogCls.SALE, "failed to resume tasks");
                            }
                        }
                        ctx.writeAndFlush(new TextWebSocketFrame(Utility.genDesc(
                                jobs.size(), "task has", "tasks have") + " been resumed"));
                    } else {
                        ctx.writeAndFlush(new TextWebSocketFrame("An empty Task List is a must."));
                    }
                    break;
                case "reload":
                    //Possible duplicates in NettyService::init, shall be merged into one.
                    Source source = new Source(Paths.get("sourceAll"));
                    source.load();
                    data.clear();//Remember, data doesn't belong to this.
                    data.addAll(source.getData());
                    ctx.writeAndFlush(new TextWebSocketFrame("reloaded"));
                    break;
                case "rescan":
                    try {
                        web.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Utility.log(LogCls.LOOP, "failed to rescan");
                    }
                    ctx.writeAndFlush(new TextWebSocketFrame("complete"));
                    break;
                case "balus":
                    final String info = "shutdown as " + ctx.channel() + " required.";
                    Utility.log(LogCls.INFO, info);
                    group.writeAndFlush(new TextWebSocketFrame(info));

                    jobs.forEach(Task::stop);
                    ctx.writeAndFlush(new TextWebSocketFrame("killed " + Utility.genDesc(jobs.size(), "task")));
                    Utility.log(LogCls.SALE, "all the tasks stopped.");

                    final var lines = jobs
                            .stream()
                            .map(Task::getArgumentOptional)
                            .flatMap(Optional::stream)
                            .collect(Collectors.toUnmodifiableList());
                    try {
                        Files.deleteIfExists(JOBS_SAVE_PATH);
                        Files.createFile(JOBS_SAVE_PATH);
                        Files.write(JOBS_SAVE_PATH, lines, StandardOpenOption.APPEND);
                        ctx.writeAndFlush(new TextWebSocketFrame(String.format("saved %d out of %s.",
                                lines.size(), Utility.genDesc(jobs.size(), "task"))));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Utility.log(LogCls.SALE, "failed to save jobs.");
                        ctx.writeAndFlush(new TextWebSocketFrame("failed to save jobs."));
                    }

                    retriever.shutdown();
                    try {
                        retriever.awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Utility.log(LogCls.BOOK, "retriever terminated.");

                    try {
                        shutdownHandler.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                default:
                    group.writeAndFlush(msg.retain());
            }
        }
    }
}

