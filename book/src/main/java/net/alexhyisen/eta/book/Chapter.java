package net.alexhyisen.eta.book;

import net.alexhyisen.Utility;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Alex on 2017/2/11.
 * Chapter is what converted from chapter pages as content String.
 */
public class Chapter {
    static private String getIndent(int depth) {
        char[] rtn = new char[depth];
        Arrays.fill(rtn, '\t');
        return new String(rtn);
    }

    @SuppressWarnings({"WeakerAccess"})
    static void expand(Node node, int depth) {
        NodeList subs = node.getChildNodes();

        String identity = node.getNodeName();
        NamedNodeMap attr = node.getAttributes();
        if (attr != null) {
            Node id = attr.getNamedItem("id");
            if (id != null) {
                identity += " id#" + id.getNodeValue();
            }
            id = attr.getNamedItem("class");
            if (id != null) {
                identity += " class#" + id.getNodeValue();
            }
        }
        switch (subs.getLength()) {
            case 0:
                System.out.println(getIndent(depth) + identity + ":" + node.getNodeValue());
                break;
            case 1:
                System.out.println(getIndent(depth) + identity + "=" + subs.item(0).getNodeValue());
                break;
            default:
                System.out.println(getIndent(depth) + identity + "->");
                for (int k = 0; k != subs.getLength(); ++k) {
                    expand(subs.item(k), depth + 1);
                }
                break;
        }
    }

    //a whole deepest search
    //for parallel optimization, I can not use the strategy that searches one by one.
    @SuppressWarnings("WeakerAccess")
    static List<Node> search(Node root, String name) {
        List<Node> rtn = new LinkedList<>();
        if (name.equals(root.getNodeName())) {
            rtn.add(root);
        } else if (root.getChildNodes().getLength() > 1) {
            NodeList subs = root.getChildNodes();
            IntStream.range(0, subs.getLength())
                    .mapToObj(subs::item)
                    .flatMap(v -> search(v, name).stream())
                    .sequential()//List is not an container that supports parallel operations.
                    .forEach(rtn::add);
        }
        return rtn;
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    static Node findOne(@Nullable Node from, String name) {
        if (from != null) {
            Node node = from;
            do {
                if (node.getNodeName().equals(name)) {
                    return node;
                }
            } while ((node = node.getNextSibling()) != null);
        }
        return null;
    }

    @SuppressWarnings("unused")
    static List<Node> findAll(@Nullable Node from, String name) {
        List<Node> rtn = new ArrayList<>();//LL is cheaper, but AL is better for parallel optimization.
        Node one = findOne(from, name);
        while (one != null) {
            rtn.add(one);
            one = findOne(one.getNextSibling(), name);
        }
        return rtn;
    }

    //the name of root is exclusive in the path
    @SuppressWarnings("WeakerAccess")
    @Nullable
    static Node getNode(@Nullable Node root, String... path) {
        Node one = root;
        Iterator<String> it = Arrays.asList(path).iterator();
        while (it.hasNext() && one != null) {
            //check for attendance is not necessary, but I guess it benefits the performance.
            one = findOne(one.getFirstChild(), it.next());
        }
        return one;
    }

    @SuppressWarnings("unused")
    static Optional<Node> getNodeOptional(@Nullable Node root, String... path) {
        return Optional.ofNullable(getNode(root, path));
    }

    @SuppressWarnings("unused")
    static void setValue(Node target, String value) {
        assert target.getChildNodes().getLength() == 1;
        target.getFirstChild().setNodeValue(value);
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    static String getValue(Node source) {
        assert source.getChildNodes().getLength() == 1;
        return source.getFirstChild().getNodeValue();
    }

    @SuppressWarnings("unused")
    static Optional<String> getValueOptional(@Nullable Node source) {
        if (source == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(getValue(source));
        }
    }

    private static DocumentBuilder db;

    static {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        try {
            dbf.setFeature("http://xml.org/sax/features/namespaces", false);
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /*
     * @return  the content Node, or
     *          {@code null} if failed to select.
     */
    @SuppressWarnings("WeakerAccess")
    static Node select(byte[] source) {
        try {
            Document doc = db.parse(new ByteArrayInputStream(source));
            //System.out.println("GO");

            Optional<Map.Entry<Node, Long>> result;
            result = searchRichestNode(doc, "br");
            if (result.isEmpty()) {
                //some have multiple <p> rather than <br>
                //like "https://www.miaobige.com/read/17843/11958498.html"
                result = searchRichestNode(doc, "p");
            }

            if (result.isPresent()) {
                return result.get().getKey();
            }
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        //expand(doc.getDocumentElement(),0);
        return null;
    }

    static String[] read(Node content) {
        List<String> rtn = new LinkedList<>();
        if (content == null) {
            rtn.add("nothing");
        } else {
            NodeList nl = content.getChildNodes();
            for (int k = 0; k != nl.getLength(); ++k) {
                Node it = nl.item(k);
                String name = it.getNodeName();
                //expand(it,0);
                switch (name) {
                    case "#text":
                        //replace all blanks to space.
                        //String line=it.getNodeValue().replace('　',' ')..replace(' ',' ').trim();
                        String line = it.getNodeValue().replaceAll("[　 ]", " ").trim();
                        //The previous one is dirty but workable,
                        //while the next one looks elegant but doesn't work.
                        //String line=it.getNodeValue().replaceAll("\\p{Blank}"," ").trim();

                        if (!line.isEmpty()) {
                            rtn.add(line);
                        }
                        break;
                    case "br":
                        //rtn.add("");
                        break;
                    case "p":
                        //for some passage that wrapped in <P>
                        rtn.add(it.getFirstChild().getNodeValue());
                        break;
                }
            }
        }
        //Intellij inspects that pre-defined size is now no longer beneficial.
        return rtn.toArray(new String[0]);
    }

    private int code;
    private Future<byte[]> raw;
    private String source;
    private String name;
    private String[] data;
    private String path;
    private boolean cached;

    public Chapter(String source, int code, String name, String path) {
        this.source = source;
        this.code = code;
        this.name = name;
        this.path = path;
        cached = Files.exists(Paths.get(path, String.valueOf(code)));
    }

    public int getCode() {
        return code;
    }

    // It's definitely a bad idea to compress the cached information into name message.
    // But it's convenient, comparing to modify data structure in both front-end and back-end.
    // And I'm quite confident that name is nothing but a hint message, which would not matter even if broken.
    public String getName() {
        var composited = name;
        if (!cached) {
            composited += " *";
        }
        return composited;
    }

    @SuppressWarnings("WeakerAccess")
    public Future<byte[]> getRaw() {
        return raw;
    }

    public String[] getData() {
        if (data == null) {
            if (isCached()) {
                Utility.log("read one offline");
                try {
                    List<String> lines = Files.readAllLines(Paths.get(getPath(), String.valueOf(getCode())));
                    data = lines.toArray(new String[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Utility.log("load the online one");
                if (raw == null) {
                    Utility.log("start downloading");
                    download();
                } else {
                    Utility.log("downloading is started");
                }
                try {
                    //If download failed, try again.
                    //If failed again, throw Exception.
                    if (raw.get() == null) {
                        Utility.log("failed to download, try again.");
                        download();
                    }
                    //at present, the very first Future.get() is the most time consuming procedure (~0.5s),
                    //however, after the first get(), the following get() s to the same Future take little time.
                    //Though deleting the not-null confirming, I can only delay, rather than remove the cost,
                    //which is already testified, so I must accept the result as the burden of asynchronous schedule.
                    if (raw.get() == null) {
                        throw new RuntimeException();
                    }
                    data = read(select(Utils.clean(raw.get())));
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
        return data;
    }

    public void download() {
        if (!cached) {
            raw = CompletableFuture.supplyAsync(() -> Utils.download(source));
        }
    }

    public void download(Executor exec) {
        if (!cached) {
            raw = CompletableFuture.supplyAsync(() -> Utils.download(source), exec);
        }
    }

    //cached means whether the data has already been downloaded and stored in local disk,
    //which also implies that it has been mailed to user.
    @SuppressWarnings("WeakerAccess")
    public boolean isCached() {
        return cached;
    }

    //loaded only indicate whether you can get the data offline rather than online,
    //which can not only occurred by a file in local disk, but also a temp in memory.
    @SuppressWarnings("unused")
    public boolean isLoaded() {
        return isCached() || raw != null;
    }

    public String getPath() {
        return path;
    }

    public boolean write() {
        try {
            if (!isCached()) {
                Files.write(Paths.get(getPath(), String.valueOf(getCode())), Arrays.asList(getData()), StandardOpenOption.CREATE_NEW);
                Utility.log("save " + getCode() + " " + getName());
                //To be honest, I don't know the meaning of the index file.
                Files.write(Paths.get(getPath(), "index"),
                        (getCode() + "," + getName() + "\n").getBytes(),
                        StandardOpenOption.APPEND);
                cached = true;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utility.log("skip " + getCode() + " " + getName());
        return false;
    }

    static Optional<Map.Entry<Node, Long>> searchRichestNode(Document doc, String tag) {
        return Chapter.search(doc.getDocumentElement(), tag).stream()
                .map(Node::getParentNode)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                //.peek(v-> System.out.println(v.getKey()+"="+v.getValue()))
                .max(Map.Entry.comparingByValue());
    }
}
