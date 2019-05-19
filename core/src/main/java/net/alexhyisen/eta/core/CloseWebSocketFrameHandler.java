package net.alexhyisen.eta.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

/**
 * Created by Alex on 2017/5/28.
 * react to channel close event, notice that WebSocketServerProtocolHandler also handle that event.
 */
class CloseWebSocketFrameHandler extends SimpleChannelInboundHandler<CloseWebSocketFrame> {
    private final ChannelGroup group;

    CloseWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloseWebSocketFrame msg) {
        Utility.log(LogCls.LOOP, "catch CloseFrame from Client " + ctx.channel());
        ctx.channel().writeAndFlush(new TextWebSocketFrame("Good Bye."));
        group.remove(ctx.channel());
        group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " quited."));
        ctx.fireChannelRead(msg.retain());
    }
}

