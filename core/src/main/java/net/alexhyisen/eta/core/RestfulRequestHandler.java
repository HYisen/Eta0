package net.alexhyisen.eta.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

public class RestfulRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String prefix;

    RestfulRequestHandler(String prefix) {
        this.prefix = prefix;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.uri().startsWith("/" + prefix + "/")) {
            ctx.fireChannelRead(request.retain());
            return;
        }

        String uri = request.uri().substring(request.uri().indexOf('/', 1));
        Utility.log(LogCls.LOOP, "rrh accepted " + uri);

        if (uri.equals("/info") && request.method().equals(HttpMethod.GET)) {
            String infoMsg = "{\"desc\":\"info message\"}";
            Utils.respondJson(ctx, request, infoMsg.getBytes());
        }
    }
}
