package net.alexhyisen.eta.model;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alex on 2017/3/10.
 * Source is something similar to Config.
 * It is the solution to the data persistence of Book source info.
 */
public class Source {
    private List<Book> data;
    private final Path path;

    public Source(Path path) {
        this.path = path;
    }

    public Source() {
        this.path = Paths.get(".", "source");
    }

    public List<Book> getData() {
        return data;
    }

    //Very likely, this method is only used for SerializationTest.
    public void setData(List<Book> data) {
        this.data = data;
    }

    @XmlRootElement(name = "books")
    static class Wrapper {
        private List<Book> books;

        @XmlElement(name = "book")
        List<Book> getBooks() {
            return books;
        }

        void setBooks(List<Book> books) {
            this.books = books;
        }
    }

    public boolean load(Path path) {
        try {
            JAXBContext context = JAXBContext.newInstance(Wrapper.class);
            Unmarshaller um = context.createUnmarshaller();

            Wrapper wrapper = (Wrapper) um.unmarshal(path.toFile());
            data = wrapper.getBooks();
            return true;
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean load() {
        return load(path);
    }

    public boolean save(Path path) {
        try {
            JAXBContext context = JAXBContext.newInstance(Wrapper.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            Wrapper wrapper = new Wrapper();
            wrapper.setBooks(data);

            m.marshal(wrapper, path.toFile());

            return true;
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean save() {
        return save(path);
    }
}

