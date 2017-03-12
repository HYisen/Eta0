package net.alexhyisen.eta.model;

import net.alexhyisen.eta.model.mailer.Mail;
import net.alexhyisen.eta.model.mailer.MailService;

import javax.xml.bind.annotation.XmlElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    public Book() {
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Book && this.source.equals(((Book) obj).source) && this.path.equals(((Book) obj).path) && this.name.equals(((Book) obj).name);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @XmlElement(name = "link")
    public String getSource() {
        return source;
    }

    @XmlElement(name = "path")
    public String getPath() {
        return path;
    }

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    //What's the difference between open() and read()?
    //You open a book first and then get its index, later can you read the chapters.
    //To make it easier, open() init index & chapters, while read() preload chapters' data
    //from the Internet, which is the most time consuming procedure.

    public void open(){
        index=new Index(source,path);
        chapters=index.getData().entrySet().stream()
                .map(v->new Chapter(v.getValue().getHref(),v.getKey(),v.getValue().getText(),path))
                .collect(Collectors.toList());
    }

    public void read(int nThreads){
        ExecutorService exec=Executors.newFixedThreadPool(nThreads);
        if(index==null){
            open();
        }
        chapters.forEach(v->v.download(exec));
        exec.shutdown();
        cached=true;
    }

    public List<Chapter> save(){
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
        config.load();
        MailService ms=new MailService(
                config.get("client"),
                config.get("server"),
                config.get("username"),
                config.get("password")
        );
        for(Book book:books){
            book.read(20);
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

        System.out.println("END");
    }
}
