package net.alexhyisen.eta.model.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import net.alexhyisen.eta.model.Chapter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.alexhyisen.eta.model.server.TextWebSocketFrameHandler.TEXT_DELIMITER;

/**
 * Created by Alex on 2017/6/5.
 * encode Chapter to a TextWebSocketFrame to send.
 */
public class ChapterEncoder extends MessageToMessageEncoder<Chapter> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Chapter msg, List<Object> out) throws Exception {
//        String title = msg.getName();
//        System.out.println("push  " + title);
//        String content = Arrays.stream(msg.getData()).collect(Collectors.joining(TEXT_DELIMITER));
//        content += "\n" + new Envelope(msg).toJson();
//        content = title + TEXT_DELIMITER + content;

        out.add(new TextWebSocketFrame(new Envelope(msg).toJson()));
    }
}
