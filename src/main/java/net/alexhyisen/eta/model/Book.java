package net.alexhyisen.eta.model;

import net.alexhyisen.eta.model.mailer.Mail;
import net.alexhyisen.eta.model.mailer.MailService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
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

    public String getName() {
        return name;
    }

    public List<Chapter> getChapters() {
        return chapters;
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

    private void read(int nThreads){
        Executor exec=Executors.newFixedThreadPool(nThreads);
        if(index==null){
            open();
        }
        chapters.forEach(v->v.download(exec));
        cached =true;
    }

    private List<Chapter> save(){
        return  getChapters().stream().filter(Chapter::write).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        //books.add("http://www.biqudao.com/bqge1081/");
        //books.add("http://www.fhxiaoshuo.com/read/67/67220/");
        //books.add("http://www.23us.cc/html/136/136194/");
        System.out.println("GO");
        List<Book> books=new ArrayList<>();
        String path="D:\\Code\\test\\output2\\";
        books.add(new Book("http://www.biqudao.com/bqge1081/",path+"0\\","重生之神级学霸"));
        books.add(new Book("http://www.fhxiaoshuo.com/read/67/67220/",path+"1\\","铁十字"));
        books.add(new Book("http://www.23us.cc/html/136/136194/",path+"2\\","崛起之第三帝国"));
//        books.stream()
//                .peek(v->v.read(16))
//                .peek(Book::save)
//                .forEach(v->Utility.log(v.getName()+" is finished."));

        Config config=new Config();
        MailService ms=new MailService(
                config.get("client"),
                config.get("server"),
                config.get("username"),
                config.get("password")
        );
        for(Book book:books){
            book.read(16);
            book.save().forEach(chapter->{
                String subject=String.format("《%s》 %s",book.getName(),chapter.getName());
                Mail mail=new Mail(
                        config.get("senderName"),config.get("senderAddr"),
                        config.get("recipientName"),config.get("recipientAddr"),
                        subject,chapter.getData());
                try {
                    ms.send(mail);
                    Utility.log("transmitted "+subject);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

//        Book book=books.get(0);
//        Chapter chapter=book.getChapters().get(200);
//
//        String subject=String.format("《%s》 %s",book.getName(),chapter.getName());
//        Mail mail=new Mail(
//                config.get("senderName"),config.get("senderAddr"),
//                config.get("recipientName"),config.get("recipientAddr"),
//                subject,chapter.getData());
//        try {
//            ms.send(mail);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        System.out.println("END");
    }
}
