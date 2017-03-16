package net.alexhyisen.eta.model;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2017/3/8.
 * Because of the laziness to offer local samples as inputs,
 * such as websites for crawler and servers for mailers.
 * Lacking from inputs, there is no traditional unit test.
 * Only demos based on online resources is provided as functions.
 * As a result of the uncertainty of inputs, automatically asserts are impossible.
 * Manual examination is a necessity, pick up functions that you want to test,
 * reading the console output and check them yourself.
 */
public class Tester {
    void testChapter(){
        System.out.println("GO Chapter TEST");
        int code=400;
        String path="D:\\Code\\test\\Tester\\";
        Path pf= Paths.get(path+code);
        Path pi= Paths.get(path,"index");
        try {
            Files.createDirectories(pf.getParent());
            Files.deleteIfExists(pi);
            Files.createFile(pi);
            Files.deleteIfExists(pf);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Chapter one=new Chapter(
                "http://www.biqudao.com/bqge1081/2431367.html",
                code,
                "TestChapter400",
                path
        );
        System.out.println("chapter "+one.getName()+" with code "+one.getCode());
        System.out.println("output content");
        Arrays.stream(one.getData()).forEach(System.out::println);
        System.out.println("end of content");
        System.out.println("write to "+one.getPath());
        one.write();
        System.out.println("END Chapter TEST");
    }

    void testIndex(){
        System.out.println("GO Index TEST");
        String path="D:\\Code\\test\\Tester\\";
        Index one=new Index("http://www.biqudao.com/bqge1081/",path);
        one.getData().entrySet().stream()
                .map(entry->"code "+entry.getKey() +
                        " -> "+entry.getValue().getText()+
                        " at "+entry.getValue().getHref())
                .forEach(System.out::println);
        System.out.println("END Index TEST");
    }

    void examineMultiThreadDownload(){
        Timer t=new Timer();

        t.resume("TimerTest");
        t.report();

        String path="D:\\Code\\test\\Tester\\";
        Index one=new Index("http://www.biqudao.com/bqge1081/",path);
        Map<Integer,Hyperlink> links=one.getData();

        int nThread=64;
        int size=128;
        t.resume("MTD "+size+" file(s) with "+nThread+" thread(s)");
        ExecutorService exec= Executors.newFixedThreadPool(nThread);
        long cnt= links.entrySet().stream()
                .skip(42)//skip the possible vacuum header pages.
                .limit(size)
                //.peek(v-> System.out.println(v.getValue().getText()+" from "+v.getValue().getHref()))
                .map(v -> new Chapter(v.getValue().getHref(), v.getKey(), v.getValue().getText(), path))
                .peek(v -> v.download(exec))
                .collect(Collectors.toList())//join
                .stream()
                //do not process
                .map(Chapter::getRaw)
                .peek(v->Utility.log("get one raw"))
                .map(v->{
                    byte[] rtn;
                    try {
                        rtn=v.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        throw new RuntimeException();
                    }
                    return rtn;
                })
//                //and then process
//                .map(Chapter::getData)
//                //.peek(v-> Arrays.stream(v).forEach(System.out::println))
                .mapToLong(v -> v.length)
                .sum();

        exec.shutdown();//need to be shutdown manually.
        t.report();
        System.out.println(cnt+" lines are read.");
    }

    void benchmarkChapter(){
        Timer t=new Timer();

        t.resume("TimerTest");
        t.report();

        t.resume("download");
        byte[] raw=Utility.download("http://www.biqudao.com/bqge1081/2431001.html");
        t.report();

        t.resume("clean");
        //byte[] source=Utility.clean("http://www.biqudao.com/bqge1081/2541300.html");
        byte[] xml=Utility.clean(raw);
        t.report();
        if(xml==null){
            throw new RuntimeException("Failed to download and clean the online input.");
        }

        t.resume("init db");
        DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db;
        try {
            dbf.setFeature("http://xml.org/sax/features/namespaces", false);
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            db=dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        t.report();

        Node content=null;
        try {
            t.resume("parse");
            Document doc=db.parse(new ByteArrayInputStream(xml));
            t.report();

            t.resume("search");
            Optional<Map.Entry<Node, Long>> result=Chapter.search(doc.getDocumentElement(),"br").stream()
                    .map(Node::getParentNode)
                    .collect(Collectors.groupingBy(Function.identity(),Collectors.counting()))
                    .entrySet().stream()
                    //.peek(v-> System.out.println(v.getKey()+"="+v.getValue()))
                    .max(Map.Entry.comparingByValue());
            //noinspection OptionalIsPresent
            if(result.isPresent()){
                content=result.get().getKey();
            }
            t.report();
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        /*
        //replace all in the front try closure
        t.resume("select");
        content=Chapter.select(xml);
        t.report();
        */
        if(content==null){
            throw new RuntimeException("Failed to select the content node");
        }

        //Chapter.expand(content,0);

        t.resume("read");
        String[] lines=Chapter.read(content);
        t.report();

        Path pc=Paths.get(".","chapter");
        Path pi=Paths.get(".","index");
        try {
            Files.deleteIfExists(pc);
            Files.deleteIfExists(pi);
            Files.createFile(pi);

            t.resume("save");
            Files.write(pc,Arrays.asList(lines), StandardOpenOption.CREATE_NEW);
            Files.write(pi, (4444+","+"测试章节"+"\n").getBytes(), StandardOpenOption.APPEND);
            t.report();

            Files.deleteIfExists(pc);
            Files.deleteIfExists(pi);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Arrays.stream(lines).forEach(System.out::println);

        Utility.log("finish");
    }

    void benchmarkIndex(){
        Timer t=new Timer();

        t.resume("TimerTest");
        t.report();

        String source="http://www.23us.cc/html/136/136194/";

        t.resume("download");
        byte[] raw=Utility.download(source);
        t.report();

        t.resume("clean");
        byte[] xml=Utility.clean(raw);
        t.report();

        if(xml==null){
            throw new RuntimeException("Failed to download and clean the online input.");
        }

        t.resume("init sp");
        SAXParserFactory spf=SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        //It doesn't work to serValidating(false)
        //spf.setValidating(false);
        SAXParser sp= null;
        try {
            sp = spf.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initiate");
        }
        t.report();

        List<Hyperlink> links=new ArrayList<>();

        t.resume("parse");
        try {
            sp.parse("D:\\Code\\out",new Index.ddReader(links));
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
        t.report();

        Map<Integer,Hyperlink> data=new TreeMap<>();
        t.resume("standardize");
        links.forEach(v-> {
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

            int code = Integer.valueOf(link.substring(link.lastIndexOf('/') + 1, link.lastIndexOf('.')));
            data.put(code,new Hyperlink(v.getText(),link));
        });
        t.report();

        data.forEach((code,link)-> System.out.println(code+" "+link.getText()+"->"+link.getHref()));
    }

    public static void main(String[] args) {
        System.out.println("GO Tester");

        Tester t=new Tester();
        //t.testChapter();
        //t.testIndex();
        t.examineMultiThreadDownload();
        //t.benchmarkChapter();
        //t.benchmarkIndex();

        System.out.println("END Tester");
    }
}
