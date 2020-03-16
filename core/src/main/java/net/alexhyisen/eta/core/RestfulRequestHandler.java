package net.alexhyisen.eta.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import net.alexhyisen.Keeper;
import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

public class RestfulRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String prefix;

    private static Keeper keeper = new Keeper();

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

        if (request.method().equals(HttpMethod.OPTIONS)) {
            Utils.guaranteeCorsPreflight(ctx, request);
        } else if (uri.equals("/info") && request.method().equals(HttpMethod.GET)) {
            String infoMsg = "{\"desc\":\"info message\"}";
            Utils.respondJson(ctx, request, infoMsg.getBytes());
        } else if (uri.equals("/auth") && request.method().equals(HttpMethod.PUT)) {
            System.out.println(request.headers().get("credential"));
        }
    }
}
