package net.alexhyisen.eta.book;

import net.alexhyisen.Utility;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2017/1/26.
 * Index is the avatar of an index page.
 */
public class Index {
    private Map<Integer, Hyperlink> data;
    private String source;
    private String path;

    public Index(String source, String path) {
        this.source = source;
        this.path = path;

        if (!Files.exists(Paths.get(path))) {
            try {
                Files.createDirectories(Paths.get(path));
                Files.createFile(Paths.get(path, "index"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        setupLog();
    }

    static List<Hyperlink> parse(byte[] source) {
        List<Hyperlink> data;
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            //Utility.log("parse alpha");
            ddReader reader = new ddReader();
            saxParser.parse(new InputSource(new ByteArrayInputStream(source)), reader);
            data = new ArrayList<>(reader.getData());
            //Utility.log("parse omega");
        } catch (IOException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
//        data.forEach(v -> System.out.println(v.getText() + " -> " + v.getHref()));
        return data;
    }

    boolean process() {
        data = new TreeMap<>();
        byte[] src = Utils.download(source);
        if (src == null) {
            return false;
        }
        parse(Utils.clean(src)).forEach(v -> {
            String link = v.getHref();
            //noinspection StatementWithEmptyBody
            if (link.startsWith("http")) {
                //like "http://www.fhxiaoshuo.com/read/67/67220/11125326.shtml"
            } else if (link.startsWith("/")) {
                //like "/read/67/67220/11125326.shtml"
                try {
                    URL host = new URL(source);
                    link = new URL(host.getProtocol(), host.getHost(), link).toString();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                //like "11125326.shtml"
                link = source + link;
            }

            int code = Integer.parseInt(Utils.basename(link));

            data.put(code, new Hyperlink(v.getText(), link));
        });

        return true;
    }

    boolean setupLog() {
        Path p = Paths.get(getPath(), "index");
        if (!Files.exists(p)) {
            try {
                Files.createFile(p);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    static class ddReader extends DefaultHandler {
        String name = null;
        String link = null;
        boolean isValid = false;

        //Those names come from the structure of the raw XML data.

        private final Map<String, StringBuilder> buffer = new HashMap<>();

        private static final Pattern pNumStr = Pattern.compile("\\d+");

        //a chapter link usually ends with a number string filename.
        private static boolean isChapterLink(@Nullable String orig) {
            if (orig == null) {
                return false;
            }

            return pNumStr.matcher(Utils.basename(orig)).matches();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            name = localName;
//            System.out.println("start " + localName);
            link = attributes.getValue("href");
            //As for page such as http://www.33zw.com/xiaoshuo/49492/ , li rather than dd is used.
            if ("dd".equals(name) || "li".equals(name)) {
                isValid = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
//            System.out.println("end " + localName);
            if ("a".equals(name)) {
                isValid = false;
            }
            name = null;
            link = null;
        }

        @Override
        public void characters(char[] ch, int start, int length) {
//            System.out.println(String.valueOf(ch));
//            System.out.println(isChapterLink(link) + "->" + link);
            if (isValid && Objects.equals(name, "a") && isChapterLink(link)) {
                String content = new String(ch, start, length);
                buffer.computeIfAbsent(link, StringBuilder::new).append(content);
//                System.out.printf("%s += %s\n", link, content);
            }
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws IOException {
            final String path = ".";
            //Utility.log("solve p="+publicId+" s="+systemId);
            String name = systemId.substring(systemId.lastIndexOf('/'));
            if (!(Paths.get(path, name).toFile().exists())) {
                if (systemId.startsWith("http")) {
                    String prefix = systemId.substring(0, systemId.lastIndexOf('/'));
                    Files.write(Paths.get(path, "prefix"), prefix.getBytes(), StandardOpenOption.CREATE);
                } else if (systemId.startsWith("file")) {
                    String prefix = Files.lines(Paths.get(path, "prefix"))
                            .findAny()
                            .orElse("http://www.w3.org/TR/xhtml1/DTD/");
                    systemId = prefix + systemId.substring(systemId.lastIndexOf('/'));
                    //Utility.log("reformed s="+systemId);
                }
                Utility.log("fail to find the local schema, going to creat one.");
                URL website = new URL(systemId);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(path + name);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                Utility.log("scheme is downloaded to " + Paths.get(path, name).toAbsolutePath().toString());
            }

            return new InputSource(new FileInputStream(path + name));
        }

        public List<Hyperlink> getData() {
            return buffer
                    .entrySet()
                    .stream()
                    .map(e -> new Hyperlink(e.getValue().toString(), e.getKey()))
//                    .peek(System.out::println)
                    .collect(Collectors.toList());
        }
    }

    Map<Integer, Hyperlink> getData() {
        if (data == null) {
            process();
        }
        return data;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
