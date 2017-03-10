package net.alexhyisen.eta.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Alex on 2017/3/10.
 * A wrapper used to serialize book sources.
 */
@XmlRootElement(name="books")
public class BookListWrapper {
    private List<Book> books;

    @XmlElement(name = "book")
    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}
