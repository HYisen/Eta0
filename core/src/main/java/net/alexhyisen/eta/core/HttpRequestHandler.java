package net.alexhyisen.eta.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import net.alexhyisen.Utility;
import net.alexhyisen.Web;
import net.alexhyisen.eta.book.Book;
import net.alexhyisen.log.LogCls;

import java.io.RandomAccessFile;
import java.util.List;

/**
 * Created by Alex on 2017/5/28.
 * handle some http request,
 * skip requests to WebSocket path,
 * offer static resources,
 * provide similar protocol in http.
 */
class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String mark;
    private List<Book> data;
    private Web web;

    HttpRequestHandler(String mark, List<Book> data, Web web) {
        this.mark = mark;
        this.data = data;
        this.web = web;
    }

    private static String genContentType(String uri) {
        if (uri.endsWith(".css")) {
            return "text/css; charset=utf-8";
        } else if (uri.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        } else if (uri.endsWith(".ico")) {
            return "image/x-icon; charset=utf-8";
        } else if (uri.endsWith(".json")) {
            return "application/json; charset=utf-8";
        } else {
            return "text/html; charset=UTF-8";
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        Utility.log(LogCls.LOOP, "get request to " + request.uri());
        String[] path = request.uri().split("/");
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);

        if (request.method().equals(HttpMethod.OPTIONS)) {
            Utils.guaranteeCorsPreflight(ctx, request);
        } else if (mark.equalsIgnoreCase(request.uri())) {
            ctx.fireChannelRead(request.retain());
            Utility.log(LogCls.LOOP, "pass to WebSocket");
        } else if (request.method().equals(HttpMethod.GET) &&
                path.length >= 2 && path[1].equals("db")) {
            //The traditional solution may be faster.
            /*
            Integer[] ids = (Integer[]) Arrays.stream(path)
                    .skip(2)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()).toArray();
            */
            int[] ids = new int[path.length - 2];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = Integer.parseInt(path[i + 2]);
            }

            Envelope envelope;

            switch (ids.length) {
                case 2:
                    assureBookOpened(ids[0]);
                    envelope = new Envelope(data.get(ids[0]).getChapters().get(ids[1]));
                    break;
                case 1:
                    assureBookOpened(ids[0]);
                    envelope = new Envelope(data.get(ids[0]));
                    break;
                case 0:
                    envelope = new Envelope(data);
                    break;
                default:
                    throw new RuntimeException("Failed to match RESTful HTTP request");
            }

            Utils.respond(ctx, request, "application/json; charset=UTF-8", envelope.toJson().getBytes());
        } else {
            //manage HTTP1.1 100 Continue situation
            if (HttpUtil.is100ContinueExpected(request)) {
                ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }

            var data = web.get(request.uri());
            if (data.isPresent()) {
                RandomAccessFile file = data.get();

                // How shall the zero-cost optimization depends on TLS status?
                // I've ask the author of Netty In Action, that's what it write in sample code.
                Object msg = ctx.pipeline().get(SslHandler.class) == null ?
                        new DefaultFileRegion(file.getChannel(), 0, file.length()) :
                        new ChunkedNioFile((file.getChannel()));

                HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);

                boolean keepAlive = HttpUtil.isKeepAlive(request);
                Utils.setupHeaders(response.headers(), genContentType(request.uri()), keepAlive, file.length(), origin);
                Utils.sendResponse(keepAlive, response, ctx, msg);
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void assureBookOpened(int id) {
        Book book = data.get(id);
        if (!book.isOpened()) {
            book.open();
        }
    }
}