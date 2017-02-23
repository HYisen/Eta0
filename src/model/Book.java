package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2017/2/22.
 * Book is the avatar of a book, including a Index and many Chapter s.
 */
public class Book {
    private Index index;
    private List<Chapter> chapters;

    private String source;
    private String path;

    private String name;

    private boolean cached =false;

    public Book(String source, String path, String name) {
        this.source = source;
        this.path = path;
        this.name = name;
    }

    //What's the difference between open() and read()?
    //You open a book first and then get its index, later can you read the chapters.
    //To make it easier, open() init index & chapters, while read() preload chapters' data
    //from the Internet, which is the most time consuming procedure.

    private void open(){
        index=new Index(source,path);
        chapters=index.getData().entrySet().stream()
                .map(v->new Chapter(v.getValue().getHref(),v.getKey(),v.getValue().getText(),path))
                .collect(Collectors.toList());
    }

    private void read(Executor exec){
        if(index==null){
            open();
        }
        chapters.forEach(v->v.download(exec));
        cached =true;
    }

    private void save(){
        chapters.forEach(Chapter::write);
    }

    public static void main(String[] args) {
        //books.add("http://www.biqudao.com/bqge1081/");
        //books.add("http://www.fhxiaoshuo.com/read/67/67220/");
        //books.add("http://www.23us.cc/html/136/136194/");
        List<Book> books=new ArrayList<>();
        ExecutorService es= Executors.newFixedThreadPool(8);
        String path="D:\\Code\\test\\output2\\";
        books.add(new Book("http://www.biqudao.com/bqge1081/",path+"0\\","重生之神级学霸"));
        books.add(new Book("http://www.fhxiaoshuo.com/read/67/67220/",path+"1\\","铁十字"));
        books.add(new Book("http://www.23us.cc/html/136/136194/",path+"2\\","崛起之第三帝国"));
        books.forEach(v->{
            v.open();
            v.read(es);
            v.save();
        });
    }
}
