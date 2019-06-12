package net.alexhyisen.eta.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

class Utils {
    static void respond(ChannelHandlerContext ctx, FullHttpRequest request, String contentType, byte[] payload) {
        ByteBuf content = Unpooled.wrappedBuffer(payload);

        FullHttpResponse response = new DefaultFullHttpResponse(
                request.protocolVersion(), HttpResponseStatus.OK, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
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
    }

    static void respondJson(ChannelHandlerContext ctx, FullHttpRequest request, byte[] payload) {
        respond(ctx, request, "application/json; charset=UTF-8", payload);
    }
}
