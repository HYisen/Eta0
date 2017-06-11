package net.alexhyisen.eta.model.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import net.alexhyisen.eta.model.Book;

import java.util.List;

import static net.alexhyisen.eta.model.server.TextWebSocketFrameHandler.TEXT_DELIMITER;

/**
 * Created by Alex on 2017/6/7.
 * An encoder to offer information about the shelf.
 */
public class ShelfEncoder extends MessageToMessageEncoder<List<Book>> {
    @Override
    protected void encode(ChannelHandlerContext ctx, List<Book> msg, List<Object> out) throws Exception {
        System.out.println("warp shelf");
        StringBuilder sb = new StringBuilder();
//        sb.append("Books").append(TEXT_DELIMITER);
//        for (int k = 0; k != msg.size(); ++k) {
//            sb.append(String.format("%d 《%s》", k, msg.get(k).getName())).append(TEXT_DELIMITER);
//        }
        System.out.println("add json");
//        sb.append("\n");
        sb.append(new Envelope(msg).toJson());
        out.add(new TextWebSocketFrame(sb.toString()));
        System.out.println("send shelf");
    }
}
