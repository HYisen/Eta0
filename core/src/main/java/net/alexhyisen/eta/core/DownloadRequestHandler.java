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
import java.util.function.Predicate;

import static net.alexhyisen.eta.core.Utils.sendResponse;
import static net.alexhyisen.eta.core.Utils.setupHeaders;

public class DownloadRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final List<Book> data;

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
        String uri = request.uri();
        if (!uri.startsWith("/whole/")) {
            ctx.fireChannelRead(request.retain());
            return;
        }

        var book = data.get(Integer.parseInt(uri.substring(uri.lastIndexOf("/") + 1)));

        if (request.method().equals(HttpMethod.OPTIONS)) {
            Utils.guaranteeCorsPreflight(ctx, request);
            return;
        }
        if (request.method().equals(HttpMethod.GET)) {
            if (book.getChapters().stream().allMatch(Predicate.not(Chapter::isCached))) {
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
