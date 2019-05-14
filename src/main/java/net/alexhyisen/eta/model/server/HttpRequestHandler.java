package net.alexhyisen.eta.model.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import net.alexhyisen.eta.model.Utility;
import net.alexhyisen.eta.model.catcher.Book;

import java.io.RandomAccessFile;
import java.util.List;

/**
 * Created by Alex on 2017/5/28.
 * offer a HTML debug client that can be used in a browser.
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
        Utility.log(Utility.LogCls.LOOP, "get request to " + request.uri());
        String[] path = request.uri().split("/");
        if (mark.equalsIgnoreCase(request.uri())) {
            ctx.fireChannelRead(request.retain());
            Utility.log(Utility.LogCls.LOOP, "pass to WebSocket");
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
                Utility.log(Utility.LogCls.LOOP, "write response header");
                //a better cache strategy could be used there
                if (ctx.pipeline().get(SslHandler.class) == null) {
                    ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
                    Utility.log(Utility.LogCls.LOOP, "write size = " + file.length());
                } else {
                    ctx.write(new ChunkedNioFile((file.getChannel())));
                }
                ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                Utility.log(Utility.LogCls.LOOP, "transmit finished");
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