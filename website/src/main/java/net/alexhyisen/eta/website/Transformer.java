package net.alexhyisen.eta.website;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import net.alexhyisen.Utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Transformer {
    private static String transform(String markdownInput) {
        Parser parser = Holder.INSTANCE.parser;
        HtmlRenderer renderer = Holder.INSTANCE.renderer;

        Node document = parser.parse(markdownInput);
        return renderer.render(document);
    }

    static void generate(Path path) throws IOException {
        String content = transform(Files.readString(path));
        content = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>" + Utility.extractName(path) + "</title>\n" +
                "</head>\n" +
                "<body>\n" +
                content +
                "</body>\n" +
                "</html>";
        Files.writeString(Utility.changeExtName(path, "html"), content, StandardOpenOption.CREATE_NEW);
    }

    public static void main(String[] args) throws IOException {
        System.out.println(Transformer.transform(Files.readString(Path.of(".", "vehicle.md"))));
        System.out.println(Path.of(".").toAbsolutePath());
        generate(Path.of("vehicle.md"));
    }

    private enum Holder {
        INSTANCE;

        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
    }
}
