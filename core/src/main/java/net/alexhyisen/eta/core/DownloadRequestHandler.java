package net.alexhyisen.eta.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import net.alexhyisen.Utility;
import net.alexhyisen.eta.book.Book;
import net.alexhyisen.eta.book.Chapter;
import net.alexhyisen.log.LogCls;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import static net.alexhyisen.eta.core.Utils.*;

public class DownloadRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final List<Book> data;
    private static final Map<Integer, LongAdder> progresses = new ConcurrentHashMap<>();

    public DownloadRequestHandler(List<Book> data) {
        this.data = data;
    }

    private static String genContentDispositionValue(String bookName) {
        // https://blog.robotshell.org/2012/deal-with-http-header-encoding-for-file-download/

        String encodedFilename;
        try {
            // It's RFC 2616 rather than RFC 3986, but it works under most circumstance, as space is rare.
            encodedFilename = new URI(bookName).toASCIIString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Utility.log(LogCls.LOOP, "failed to gen header with name " + bookName);
            return "attachment; filename=book.txt";
        }
        encodedFilename += ".txt";

        return String.format("attachment; filename=book.txt; filename*=utf-8''%s", encodedFilename);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (request.method().equals(HttpMethod.OPTIONS)) {
            Utils.guaranteeCorsPreflight(ctx, request);
            return;
        }

        String uri = request.uri();
        if (!uri.startsWith("/whole/")) {
            ctx.fireChannelRead(request.retain());
            return;
        }

        if (uri.endsWith("/progress") && request.method().equals(HttpMethod.GET)) {
            String front = uri.substring(0, uri.indexOf("/progress"));
            String[] limbs = front.split("/");
            int bookIndex = Integer.parseInt(limbs[limbs.length - 1]);
            LongAdder adder = progresses.get(bookIndex);
            if (adder == null) {
                Utils.respond(ctx, request, HttpResponseStatus.NOT_FOUND, "text/plain", "invalid id".getBytes());
                return;
            }
            byte[] payload = Long.toString(adder.sum()).getBytes();
            Utils.respond(ctx, request, HttpResponseStatus.OK, "text/plain", payload);
            return;
        }

        int endIndex = uri.indexOf('?');
        if (endIndex == -1) {
            endIndex = uri.length();
        }
        int bookIndex = Integer.parseInt(uri.substring(uri.lastIndexOf("/") + 1, endIndex));
        var book = data.get(bookIndex);

        if (request.method().equals(HttpMethod.POST)) {
            Map<String, String> params = extractParamFromUrlTail(uri.substring(endIndex));
            int nThreads = Integer.parseInt(params.getOrDefault("concurrency", "5"));

            if (!book.isOpened()) {
                book.open();
            }

            var adder = new LongAdder();
            progresses.put(bookIndex, adder);
            book.read(nThreads, adder);
            Utility.log(LogCls.BOOK, String.format("reading book %d with parallelism %d", bookIndex, nThreads));

            byte[] payload = Integer.toString(book.getChapters().size()).getBytes();
            Utils.respond(ctx, request, HttpResponseStatus.CREATED, "text/plain", payload);

            return;
        }
        if (request.method().equals(HttpMethod.GET)) {
            if (book.getChapters().stream().anyMatch(Predicate.not(Chapter::isLoaded))) {
                Utils.respond(ctx, request, HttpResponseStatus.FORBIDDEN,
                        "text/plain", String.format("cached status unmeet on %s", book.getName()).getBytes());
                return;
            }

            ByteBuf content = Unpooled.wrappedBuffer(book.build().getBytes());
            FullHttpResponse response = new DefaultFullHttpResponse(
                    request.protocolVersion(), HttpResponseStatus.OK, content);
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            // use "octet-stream" to hint download action, not for what it really is. (text/plain)
            setupHeaders(response.headers(), "application/octet-stream",
                    keepAlive, content.readableBytes(), request.headers().get(HttpHeaderNames.ORIGIN));
            response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, genContentDispositionValue(book.getName()));
            sendResponse(keepAlive, response, ctx, null);
        }
    }
}
