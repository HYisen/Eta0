package net.alexhyisen.eta.model;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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
            //System.out.println("GO");
            Optional<Map.Entry<Node, Long>> result=search(doc.getDocumentElement(),"br").stream()
                    .map(Node::getParentNode)
                    .collect(Collectors.groupingBy(Function.identity(),Collectors.counting()))
                    .entrySet().stream()
                    //.peek(v-> System.out.println(v.getKey()+"="+v.getValue()))
                    .max(Map.Entry.comparingByValue());
            if(result.isPresent()){
                return result.get().getKey();
            }
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        //expand(doc.getDocumentElement(),0);

        return null;
    }

    static String[] read(Node content){
        List<String> rtn=new LinkedList<>();
        if(content==null){
            rtn.add("nothing");
        }else{
            NodeList nl=content.getChildNodes();
            for(int k=0;k!=nl.getLength();++k){
                Node it=nl.item(k);
                String name=it.getNodeName();
                switch (name){
                    case "#text":
                        //replace all blanks to space.
                        //String line=it.getNodeValue().replace('　',' ')..replace(' ',' ').trim();
                        String line=it.getNodeValue().replaceAll("[　 ]"," ").trim();
                        //The previous one is dirty but workable,
                        //while the next one looks elegant but doesn't work.
                        //String line=it.getNodeValue().replaceAll("\\p{Blank}"," ").trim();

                        //System.out.println("line = "+line);
                        if(!line.isEmpty()){
                            rtn.add(line);
                        }
                        break;
                    case  "br":
                        //rtn.add("");
                        break;
                }
            }
        }
        return rtn.toArray(new String[rtn.size()]);
    }

    private int code;
    private Future<byte[]> raw;
    private String source;
    private String name;
    private String[] data;
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

    public String[] getData() {
        if(data==null){
            if (isCached()) {
                Utility.log("read one offline");
                try {
                    List<String> lines=Files.readAllLines(Paths.get(getPath()+getCode()));
                    data=lines.toArray(new String[lines.size()]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Utility.log("load the online one");
                if(raw==null){
                    download();
                }
                try {
                    //If download failed, try again.
                    //If failed again, throw Exception.
                    if(raw.get()==null){
                        Utility.log("failed to download, try again.");
                        download();
                    }
                    if(raw.get()==null){
                        throw new RuntimeException();
                    }

                    data=read(select(Utility.clean(raw.get())));
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
        return data;
    }

    public void download(){
        if(!cached){
            raw= CompletableFuture.supplyAsync(()->Utility.download(source));
        }
    }

    public void download(Executor exec){
        if(!cached){
            raw= CompletableFuture.supplyAsync(()->Utility.download(source),exec);
        }
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
                Files.write(Paths.get(getPath()+getCode()),Arrays.asList(getData()), StandardOpenOption.CREATE_NEW);
                Utility.log("save "+getCode()+" "+getName());
                //To be honest, I don't know the meaning of the index file.
                Files.write(Paths.get(getPath(),"index"),
                        (getCode()+","+getName()+"\n").getBytes(),
                        StandardOpenOption.APPEND);
                cached=true;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utility.log("skip "+getCode()+" "+getName());
        return false;
    }
}