package net.alexhyisen.eta.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

class Utils {
    private static final String ALLOW_HEADERS = String.join(",",
            HttpHeaderNames.CONTENT_TYPE,
            RestfulRequestHandler.HEADER_CREDENTIAL_NAME);
    private static final String ALLOW_METHODS = String.join(",",
            HttpMethod.GET.asciiName(),
            HttpMethod.POST.asciiName(),
            HttpMethod.PUT.asciiName(),
            HttpMethod.DELETE.asciiName());

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

    static void respond(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status, String contentType, byte[] payload) {
        ByteBuf content = Unpooled.wrappedBuffer(payload);

        FullHttpResponse response = new DefaultFullHttpResponse(
                request.protocolVersion(), status, content);

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        setupHeaders(response.headers(), contentType,
                keepAlive, content.readableBytes(), request.headers().get(HttpHeaderNames.ORIGIN));
        sendResponse(keepAlive, response, ctx, null);
    }

    static void respondOkJson(ChannelHandlerContext ctx, FullHttpRequest request, byte[] payload) {
        respond(ctx, request, HttpResponseStatus.OK, "application/json; charset=UTF-8", payload);
    }

    static void guaranteeCorsPreflight(ChannelHandlerContext ctx, FullHttpRequest request) {
        DefaultHttpResponse resp = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.ACCEPTED);
        resp.headers()
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, ALLOW_METHODS)
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, ALLOW_HEADERS)
                .set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, 3600);

        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        setupHeaders(resp.headers(), "text/plain", keepAlive, 0, origin);
        sendResponse(keepAlive, resp, ctx, null);
        Utility.log(LogCls.AUTH, String.format("guarantee CORS %s -> %s", ctx.channel().remoteAddress(), origin));
    }

    static Map<String, String> extractParamFromUrlTail(String questionMarkStartedTail) {
        if (!questionMarkStartedTail.startsWith("?")) {
            return Collections.emptyMap();
        }
        return Arrays
                .stream(questionMarkStartedTail.substring(1).split("&"))
                .map(v -> v.split("="))
                .collect(Collectors.toMap(v -> v[0], v -> v[1]));
    }
}
