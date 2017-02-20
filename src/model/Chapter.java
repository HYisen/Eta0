package model;

import com.sun.istack.internal.Nullable;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by Alex on 2017/2/11.
 * Chapter is what converted from chapter pages as content String.
 */
public class Chapter {
    static private String getIndent(int depth){
        char[] rtn=new char[depth];
        Arrays.fill(rtn,'\t');
        return new String(rtn);
    }

    static void expand(Node node, int depth){
        NodeList subs=node.getChildNodes();

        String identity=node.getNodeName();
        NamedNodeMap attr=node.getAttributes();
        if(attr!=null){
            Node id=attr.getNamedItem("id");
            if(id!=null){
                identity+=" id#"+id.getNodeValue();
            }
            id=attr.getNamedItem("class");
            if(id!=null){
                identity+=" class#"+id.getNodeValue();
            }
        }
        switch (subs.getLength()){
            case 0:
                System.out.println(getIndent(depth)+identity+":"+node.getNodeValue());
                break;
            case 1:
                System.out.println(getIndent(depth)+identity+"="+subs.item(0).getNodeValue());
                break;
            default:
                System.out.println(getIndent(depth)+identity+"->");
                for(int k=0;k!=subs.getLength();++k){
                    expand(subs.item(k),depth+1);
                }
                break;
        }
    }

    //a whole deepest search
    //for parallel optimization, I can not use the strategy that searches one by one.
    static List<Node> search(Node root, String name){
        List<Node> rtn=new LinkedList<>();
        if(name.equals(root.getNodeName())){
            rtn.add(root);
        }else if(root.getChildNodes().getLength()>1){
            NodeList subs=root.getChildNodes();
            IntStream.range(0,subs.getLength())
                    .mapToObj(subs::item)
                    .parallel()
                    .flatMap(v->search(v,name).stream())
                    .sequential()//List is not an container that supports parallel operations.
                    .forEach(rtn::add);
        }
        return rtn;
    }

    @Nullable
    static Node findOne(@Nullable Node from, String name){
        if(from!=null){
            Node node=from;
            do {
                if(node.getNodeName().equals(name)){
                    return node;
                }
            }while ((node=node.getNextSibling())!=null);
        }
        return null;
    }

    static List<Node> findAll(@Nullable Node from,String name){
        List<Node> rtn=new ArrayList<>();//LL is cheaper, but AL is better for parallel optimization.
        Node one=findOne(from,name);
        while (one!=null){
            rtn.add(one);
            one=findOne(one.getNextSibling(),name);
        }
        return rtn;
    }

    //the name of root is exclusive in the path
    @Nullable static Node getNode(@Nullable Node root,String... path){
        Node one=root;
        Iterator<String> it=Arrays.asList(path).iterator();
        while(it.hasNext()&&one!=null){
            //check for attendance is not necessary, but I guess it benefits the performance.
            one=findOne(one.getFirstChild(),it.next());
        }
        return one;
    }

    static Optional<Node> getNodeOptional(@Nullable Node root,String... path){
        return Optional.ofNullable(getNode(root,path));
    }

    static void setValue(Node target,String value){
        assert target.getChildNodes().getLength()==1;
        target.getFirstChild().setNodeValue(value);
    }

    @Nullable static String getValue(Node source){
        assert source.getChildNodes().getLength()==1;
        return source.getFirstChild().getNodeValue();
    }

    static Optional<String> getValueOptional(@Nullable Node source){
        if(source==null){
            return Optional.empty();
        }else {
            return Optional.of(getValue(source));
        }
    }
    static DocumentBuilder db;

    static {
        DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        try {
            dbf.setFeature("http://xml.org/sax/features/namespaces", false);
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            db=dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /*
     * @return  the content Node, or
     *          {@code null} if failed to select.
    */
    static Node select(byte[] source){
        try {
            Document doc=db.parse(new ByteArrayInputStream(source));
            List<Node> result=search(doc.getDocumentElement(),"br");
            if(!result.isEmpty()){
                return result.get(0).getParentNode();
            }
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        //expand(doc.getDocumentElement(),0);

        return null;
    }

    static String read(Node content){
        if(content==null){
            return "nothing";
        }
        StringBuilder sb=new StringBuilder();
        NodeList nl=content.getChildNodes();
        for(int k=0;k!=nl.getLength();++k){
            Node it=nl.item(k);
            String name=it.getNodeName();
            switch (name){
                case "#text":
                    String line=it.getNodeValue().trim();
                    int anchor=line.indexOf(" ");
                    if(anchor!=-1){
                        sb.append(line.substring(anchor)).append("\n");
                    }
                    break;
                case  "br":
                    //sb.append("\n");
                    break;
            }
        }
        return sb.toString();
    }

    private int code;
    private String source;
    private String name;
    private String data;
    private String path;
    private boolean cached;

    public Chapter(String source ,int code, String name,String path) {
        this.source = source;
        this.code = code;
        this.name = name;
        this.path = path;
        cached=Files.exists(Paths.get(path+code));
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        if(data==null){
            if (isCached()) {
                Utility.log("read one offline");
                try {
                    data=new String(Files.readAllBytes(Paths.get(getPath()+getCode())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Utility.log("load one online");
                data=read(select(Utility.clean(source)));
            }
        }
        return data;
    }

    public boolean isCached() {
        return cached;
    }

    public String getPath() {
        return path;
    }

    public boolean write(){
        try {
            if(!isCached())
            {
                Files.write(Paths.get(getPath()+getCode()),getData().getBytes(), StandardOpenOption.CREATE_NEW);
                Utility.log("save "+getCode()+" "+getName());
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utility.log("skip "+getCode()+" "+getName());
        return false;
    }

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        Utility.log("start");
        byte[] source=Utility.clean("http://www.23us.cc/html/136/136194/6911853.html");
        Utility.log("cleaned");
        DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        dbf.setFeature("http://xml.org/sax/features/namespaces", false);
        dbf.setFeature("http://xml.org/sax/features/validation", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder db=dbf.newDocumentBuilder();
        Utility.log("parse alpha");
        Document doc=db.parse(new BufferedInputStream(new ByteArrayInputStream(source)));
        Utility.log("parse omega");
        //expand(doc.getDocumentElement(),0);

        Node content=search(doc.getDocumentElement(),"br").get(0).getParentNode();
        Utility.log("selected");
        //expand(content,0);
//        FileWriter writer=new FileWriter("D:\\Code\\str");
//        writer.write(read(content));
        expand(content,0);
        System.out.println(read(content));
        Utility.log("read");
//        writer.close();

        Utility.log("finish");
    }
}
