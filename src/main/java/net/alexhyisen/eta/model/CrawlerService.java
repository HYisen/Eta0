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
    private List<Book> data;
    private String path=".";
    private String sourceName="source";
    private String configName="config";

    public CrawlerService(String sourceName, String configName) {
        this.sourceName = sourceName;
        this.configName = configName;
    }

    public CrawlerService() {
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Path getSourcePath() {
        return Paths.get(path,sourceName);
    }

    public Path getConfigPath() {
        return Paths.get(path,configName);
    }

    public List<Book> getData() {
        return data;
    }

    //Very likely, this method is only used for SerializationTest.
    public void setData(List<Book> data) {
        this.data = data;
    }

    public void loadSource(Path path){
        try {
            JAXBContext context=JAXBContext.newInstance(Wrapper.class);
            Unmarshaller um=context.createUnmarshaller();

            Wrapper wrapper=(Wrapper)um.unmarshal(path.toFile());
            data =wrapper.getBooks();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public void loadSource(){
        loadSource(getSourcePath());
    }

    public void saveSource(Path path){
        try {
            JAXBContext context=JAXBContext.newInstance(Wrapper.class);
            Marshaller m=context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);

            Wrapper wrapper=new Wrapper();
            wrapper.setBooks(data);

            m.marshal(wrapper, path.toFile());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public void saveSource(){
        saveSource(getSourcePath());
    }

    public static void main(String[] args) {
        CrawlerService cs=new CrawlerService();
        cs.data =new LinkedList<>();
        String path="D:\\Code\\test\\output2\\";
        cs.data.add(new Book("http://www.biqudao.com/bqge1081/",path+"0\\","重生之神级学霸"));
        cs.data.add(new Book("http://www.fhxiaoshuo.com/read/67/67220/",path+"1\\","铁十字"));
        cs.data.add(new Book("http://www.23us.cc/html/136/136194/",path+"2\\","崛起之第三帝国"));
        cs.saveSource();
    }
}