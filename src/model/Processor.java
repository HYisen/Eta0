package model;

import java.io.*;
import java.nio.file.*;
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
        String path="D:\\Code\\test\\output0\\";
        List<String> books=new ArrayList<>();
        //books.add("http://www.biqudao.com/bqge1081/");
        //books.add("http://www.fhxiaoshuo.com/read/67/67220/");
        books.add("http://www.23us.cc/html/136/136194/");
        Path p=Paths.get(path,"index");
        if(!Files.exists(p)){
            Utility.log("creat index");
            Files.createDirectories(p.getParent());
            Files.createFile(p);
        }
        books.stream()
                .map(url->new Index(url,path))
                .peek(Index::process)
                .map(Index::getData)
                .flatMap(v->v.entrySet().stream())
                .parallel()
                .map(v->new Chapter(v.getValue().getHref(),v.getKey(),v.getValue().getText(),path))
                .sequential()
                .filter(Chapter::write)
                .forEach(v->{
                    try {
                        String line=v.getCode()+","+v.getName()+"\n";
                        Files.write(p,line.getBytes(),StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
