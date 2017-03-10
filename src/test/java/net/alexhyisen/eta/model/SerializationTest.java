package net.alexhyisen.eta.model;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alex on 2017/3/10.
 * This tests all the serialization methods that used in the project.
 * Currently, it includes
 * Book resource info through XML
 */
public class SerializationTest {
    @Test
    public void testBook() throws Exception {
        //init original data
        List<Book> books=new LinkedList<>();
        String path="D:\\Code\\test\\output2\\";
        books.add(new Book("http://www.biqudao.com/bqge1081/",path+"0\\","重生之神级学霸"));
        books.add(new Book("http://www.fhxiaoshuo.com/read/67/67220/",path+"1\\","铁十字"));
        books.add(new Book("http://www.23us.cc/html/136/136194/",path+"2\\","崛起之第三帝国"));

        CrawlerService cs=new CrawlerService();
        cs.setBooks(books);
        cs.saveBookData();

        //use a brand new cs to ensure the isolation
        cs=new CrawlerService();
        cs.loadBookData();

        cs.getBooks().forEach(v->{
            //System.out.println("load book "+v.getName()+ " in "+v.getPath()+ " at "+v.getSource());
            assert books.contains(v);
        });
    }
}
