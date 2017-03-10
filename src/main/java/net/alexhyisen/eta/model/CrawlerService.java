package net.alexhyisen.eta.model;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alex on 2017/3/10.
 * The service to deal with crawler's jobs.
 */
public class CrawlerService {
    private List<Book> books;
    Path path= Paths.get(".","source");

    public List<Book> getBooks() {
        return books;
    }

    //Very likely, this method is only used for SerializationTest.
    public void setBooks(List<Book> books) {
        this.books = books;
    }

    public void loadBookData(){
        try {
            JAXBContext context=JAXBContext.newInstance(BookListWrapper.class);
            Unmarshaller um=context.createUnmarshaller();

            BookListWrapper wrapper=(BookListWrapper)um.unmarshal(path.toFile());
            books=wrapper.getBooks();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public void saveBookData(){
        try {
            JAXBContext context=JAXBContext.newInstance(BookListWrapper.class);
            Marshaller m=context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);

            BookListWrapper wrapper=new BookListWrapper();
            wrapper.setBooks(books);

            m.marshal(wrapper,path.toFile());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CrawlerService cs=new CrawlerService();
        cs.books=new LinkedList<>();
        String path="D:\\Code\\test\\output2\\";
        cs.books.add(new Book("http://www.biqudao.com/bqge1081/",path+"0\\","重生之神级学霸"));
        cs.books.add(new Book("http://www.fhxiaoshuo.com/read/67/67220/",path+"1\\","铁十字"));
        cs.books.add(new Book("http://www.23us.cc/html/136/136194/",path+"2\\","崛起之第三帝国"));
        cs.saveBookData();
    }
}