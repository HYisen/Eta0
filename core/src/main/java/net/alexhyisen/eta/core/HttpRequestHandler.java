package net.alexhyisen.eta.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        Utility.log(LogCls.LOOP, "get request to " + request.uri());
        String[] path = request.uri().split("/");
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);

        if (request.method().equals(HttpMethod.OPTIONS)) {
            DefaultHttpResponse resp = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.ACCEPTED);
            resp.headers()
                    .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
                    .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.GET.asciiName())
                    .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaderNames.CONTENT_TYPE)
                    .set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, 3600)
                    .set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                resp.headers()
                        .set(HttpHeaderNames.CONTENT_LENGTH, 0)
                        .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.write(resp);
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
            Utility.log(LogCls.LOOP, "guarantee CORS to " + origin);
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

            ByteBuf content = Unpooled.wrappedBuffer(envelope.toJson().getBytes());

            FullHttpResponse response = new DefaultFullHttpResponse(
                    request.protocolVersion(), HttpResponseStatus.OK, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            if (origin != null) {
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.write(response);
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            //manage HTTP1.1 100 Continue situation
            if (HttpUtil.is100ContinueExpected(request)) {
                ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }

            var data = web.get(request.uri());
            if (data.isPresent()) {
                RandomAccessFile file = data.get();
                HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
                if (origin != null) {
                    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                }
                if (request.uri().endsWith(".css")) {
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/css; charset=utf-8");
                } else if (request.uri().endsWith(".js")) {
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/javascript; charset=utf-8");
                } else if (request.uri().endsWith(".ico")) {
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/x-icon; charset=utf-8");
                } else if (request.uri().endsWith(".json")) {
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
                } else {
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                }
                boolean keepAlive = HttpUtil.isKeepAlive(request);
                if (keepAlive) {
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
                ctx.write(response);
                Utility.log(LogCls.LOOP, "write response header");
                //a better cache strategy could be used there
                if (ctx.pipeline().get(SslHandler.class) == null) {
                    ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
                    Utility.log(LogCls.LOOP, "write size = " + file.length());
                } else {
                    ctx.write(new ChunkedNioFile((file.getChannel())));
                }
                ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                Utility.log(LogCls.LOOP, "transmit finished");
                if (!keepAlive) {
                    future.addListener(ChannelFutureListener.CLOSE);
                }
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