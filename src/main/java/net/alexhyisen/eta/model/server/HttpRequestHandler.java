package net.alexhyisen.eta.model.server;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.RandomAccessFile;

/**
 * Created by Alex on 2017/5/28.
 * offer a HTML debug client that can be used in a browser.
 */
class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String mark;

    private static final String INDEX_PAGE_PATH =
            "D:\\Code\\Netty\\netty-in-action-cn-ChineseVersion\\chapter12\\src\\main\\resources\\index.html";

    HttpRequestHandler(String mark) {
        this.mark = mark;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        System.out.println("get request");
        if (mark.equalsIgnoreCase(request.uri())) {
            ctx.fireChannelRead(request.retain());
            System.out.println("pass to WebSocket");
        } else {
            //manage HTTP1.1 100 Continue situation
            if (HttpUtil.is100ContinueExpected(request)) {
                ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }

            //manage default homepage response
            System.out.println("generate index");
            RandomAccessFile file = new RandomAccessFile(INDEX_PAGE_PATH, "r");
            HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.write(response);
            System.out.println("write response header");
            //a better index page cache strategy could be used there
            if (ctx.pipeline().get(SslHandler.class) == null) {
                ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
                System.out.println("write index.html size = "+file.length());
            } else {
                ctx.write(new ChunkedNioFile((file.getChannel())));
            }
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            System.out.println("transmit finished");
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}