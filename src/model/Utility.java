package model;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;

/**
 * Created by Alex on 2017/2/14.
 * Some valuable functions.
 */
public class Utility {
    static byte[] clean(String url){
        CleanerProperties props = new CleanerProperties();

        props.setTranslateSpecialEntities(true);
        props.setTransResCharsToNCR(true);
        props.setOmitComments(true);

        HtmlCleaner cleaner=new HtmlCleaner();
        TagNode tg;
        try {
            tg = cleaner.clean(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream os=new ByteArrayOutputStream();//Maybe I should set a size, which is obviously larger than 100kb.
        try {
            new PrettyXmlSerializer(props).writeToStream(tg,os,"utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return os.toByteArray();
    }

    static void log(String msg){
        System.out.println(LocalTime.now()+" "+msg);
    }
}
