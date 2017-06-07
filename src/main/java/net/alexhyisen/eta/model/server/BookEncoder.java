package net.alexhyisen.eta.model.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import net.alexhyisen.eta.model.Book;

import java.util.List;

import static net.alexhyisen.eta.model.server.TextWebSocketFrameHandler.TEXT_DELIMITER;

/**
 * Created by Alex on 2017/6/7.
 * encode a Book object to a TextWebSocketFrame to show its list of content.
 */
public class BookEncoder extends MessageToMessageEncoder<Book> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Book msg, List<Object> out) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Chapters in 《%s》",msg.getName())).append(TEXT_DELIMITER);
        if (!msg.isOpened()) {
            msg.open();
        }
        for (int k = 0; k != msg.getChapters().size(); ++k) {
            sb
                    .append(String.format("%d %s", k, msg.getChapters().get(k).getName()))
                    .append(TEXT_DELIMITER);
        }
        out.add(new TextWebSocketFrame(sb.toString()));
    }
}
