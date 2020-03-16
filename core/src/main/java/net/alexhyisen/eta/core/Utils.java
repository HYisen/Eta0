package net.alexhyisen.eta.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

class Utils {
    /**
     * @param contentLength  Only if keepAlive is true would be set in headers' content-length.
     * @param originNullable If not null, would be set in headers' access-control-allow-origin.
     */
    static void setupHeaders(HttpHeaders headers, String contentType,
                             boolean keepAlive, long contentLength, String originNullable) {
        headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
        if (originNullable != null) {
            headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, originNullable);
        }
        if (keepAlive) {
            headers
                    .set(HttpHeaderNames.CONTENT_LENGTH, contentLength)
                    .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
    }

    /**
     * @param msgNullable If not null, would be transmitted after response as the payload through ctx::write.
     */
    static void sendResponse(boolean keepAlive, HttpResponse response, ChannelHandlerContext ctx, Object msgNullable) {
        ctx.write(response);
        if (msgNullable != null) {
            ctx.write(msgNullable);
        }
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    static void respond(ChannelHandlerContext ctx, FullHttpRequest request, String contentType, byte[] payload) {
        ByteBuf content = Unpooled.wrappedBuffer(payload);

        FullHttpResponse response = new DefaultFullHttpResponse(
                request.protocolVersion(), HttpResponseStatus.OK, content);

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        setupHeaders(response.headers(), contentType,
                keepAlive, content.readableBytes(), request.headers().get(HttpHeaderNames.ORIGIN));
        sendResponse(keepAlive, response, ctx, null);
    }

    static void respondJson(ChannelHandlerContext ctx, FullHttpRequest request, byte[] payload) {
        respond(ctx, request, "application/json; charset=UTF-8", payload);
    }

    static void guaranteeCorsPreflight(ChannelHandlerContext ctx, FullHttpRequest request) {
        DefaultHttpResponse resp = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.ACCEPTED);
        resp.headers()
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.GET.asciiName())
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaderNames.CONTENT_TYPE)
                .set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, 3600);

        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        setupHeaders(resp.headers(), "text/plain", keepAlive, 0, origin);
        sendResponse(keepAlive, resp, ctx, null);
        Utility.log(LogCls.LOOP, "guarantee CORS to " + origin);
    }
}
