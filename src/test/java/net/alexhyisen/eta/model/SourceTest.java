package net.alexhyisen.eta.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alex on 2017/3/12.
 * Test whether Source works properly.
 */
public class SourceTest {
    private Path path;
    private List<Book> data;

    @Before
    public void setUp() throws Exception {
        path = Paths.get(".", "test_config");

        data = new LinkedList<>();
        data.add(new Book("http://www.biqudao.com/bqge1081/", ".\\0\\", "重生之神级学霸"));
        data.add(new Book("http://www.fhxiaoshuo.com/read/67/67220/", ".\\1\\", "铁十字"));
        data.add(new Book("http://www.23us.cc/html/136/136194/", ".\\2\\", "崛起之第三帝国"));
    }

    @Test
    public void persistence() throws Exception {
        Source source = new Source(path);
        source.setData(data);
        source.save();

        //use a brand new cs to ensure the isolation
        source = new Source(path);
        source.load();

        source.getData().forEach(v -> {
            //System.out.println("load book "+v.getName()+ " in "+v.getPath()+ " at "+v.getSource());
            assert data.contains(v);
        });
    }

    @After
    public void tearDown() throws Exception {
        Files.delete(path);
    }
}