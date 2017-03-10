package net.alexhyisen.eta.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Alex on 2017/3/10.
 * This tests all the serialization methods that used in the project.
 * Currently, it includes
 * Book resource info through XML
 */
public class SerializationTest {
    @Test
    public void testSource() throws Exception {
        //init original data
        List<Book> books=new LinkedList<>();

        books.add(new Book("http://www.biqudao.com/bqge1081/",".\\0\\","重生之神级学霸"));
        books.add(new Book("http://www.fhxiaoshuo.com/read/67/67220/",".\\1\\","铁十字"));
        books.add(new Book("http://www.23us.cc/html/136/136194/",".\\2\\","崛起之第三帝国"));

        CrawlerService cs=new CrawlerService("test_source","test_config");
        cs.setData(books);
        cs.saveSource();

        //use a brand new cs to ensure the isolation
        cs=new CrawlerService("test_source","test_config");
        cs.loadSource();

        cs.getData().forEach(v->{
            //System.out.println("load book "+v.getName()+ " in "+v.getPath()+ " at "+v.getSource());
            assert books.contains(v);
        });

        Files.delete(cs.getSourcePath());
        if(Files.exists(cs.getConfigPath())){
            Files.delete(cs.getConfigPath());
        }
    }

    //Probably should appears in ConfigTest class.
    @Test
    public void testConfig() throws Exception {
        Map<String,String> data=new TreeMap<>();
        data.put("client","localhost");
        data.put("server","smtp.163.com");
        data.put("username","sender@163.com");
        data.put("password","how_would_I_tell_you");
        data.put("senderName","SMTP_Tester");
        data.put("senderAddr","sender@163.com");
        data.put("recipientName","Receiver");
        data.put("recipientAddr","receiver@gmail.com");

        Path p=Paths.get(".","test_config");
        Config saveConfig=new Config(p);
        data.forEach(saveConfig::put);
        saveConfig.save();

        Config loadConfig=new Config(p);
        data.forEach((key,value)->{
            //System.out.println("check "+key+" : "+loadConfig.get(key)+" = "+value);
            assert value.equals(loadConfig.get(key));
        });

        Files.delete(p);
    }
}
