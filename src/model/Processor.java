package model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by Alex on 2017/2/13.
 * Processor is where the procedure passes.
 */
public class Processor {
    static void functionalMethod() throws IOException {
        FileWriter writer;
        writer=new FileWriter("D:\\Code\\output\\index");

        long cnt=Stream.of("http://www.biqudao.com/bqge1081/")
                .peek(System.out::println)
                .map(Utility::clean)
                .map(Index::parse)
                .flatMap(List::stream)
                .distinct()
                .map(v->{
                    try {
                        writer.write(v.getHref().substring(v.getHref().lastIndexOf('/')+1,v.getHref().indexOf('.')));
                        writer.write("@"+v.getText());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return v;
                })
                .parallel()
                .map(v->{
                    try {
                        System.out.println("deal "+v.getHref());
                        String name=v.getHref().substring(v.getHref().lastIndexOf('/')+1,v.getHref().indexOf('.'));
                        Writer w=new FileWriter("D:\\Code\\output\\"+name);
                        String content=Stream.of("http://www.biqudao.com"+v.getHref())
                                .map(Utility::clean)
                                .map(Chapter::select)
                                .map(Chapter::read)
                                .findAny().get();
                        w.write(content);
                        w.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return v;
                })
                .sequential()
                .count();
        System.out.println(cnt+" in total");
        writer.close();
    }
    public static void main(String[] args) throws IOException {
        String path="D:\\Code\\output\\";
        List<String> books=new ArrayList<>();
        books.add("http://www.biqudao.com/bqge1081/");
        Writer iw=new FileWriter(new File(path+"index"),true);//IndexWriter
        books.stream()
                .map(url->new Index(url,path))
                .peek(Index::process)
                .map(Index::getData)
                .flatMap(v->v.entrySet().stream())
                .parallel()
                .map(v->new Chapter(v.getValue().getHref(),v.getKey(),v.getValue().getText()))
                .sequential()
                .filter(v-> v.write(path+v.getCode()))
                .forEach(v->{
                    try {
                        String line=v.getCode()+","+v.getName()+"\n";
                        iw.append(line);
                        iw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
