package model;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import sun.nio.ch.IOUtil;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = null;
            try {
                is = new URL(url).openStream ();
                byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
                int n;

                while ( (n = is.read(byteChunk)) > 0 ) {
                    baos.write(byteChunk, 0, n);
                }
            }
            catch (IOException e) {
                e.printStackTrace ();
            }
            finally {
                if (is != null) { is.close(); }
            }
            byte[] buf=baos.toByteArray();

            //cleaner.clean(new URL(url)) failed to get the correct charset.
            //page "http://www.fhxiaoshuo.com/read/67/67220/" as an example.
            tg=cleaner.clean(new ByteArrayInputStream(buf));

            final String ATT="content";
            String content=tg.findElementHavingAttribute(ATT,true).getAttributeByName(ATT);
            final String KEY="charset=";
            String charset=content.substring(content.indexOf(KEY)+KEY.length());
            //System.out.println("charset = "+charset+" | "+props.getCharset());
            if(!"utf-8".equalsIgnoreCase(charset)){
                tg=cleaner.clean(new ByteArrayInputStream(buf),charset);
            }
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

    public static void main(String[] args) throws IOException {
        byte[] data=Utility.clean("http://www.fhxiaoshuo.com/read/67/67220/");
        //byte[] data=Utility.clean("http://www.biqudao.com/bqge1081/");
        Files.write(Paths.get("D:\\Code\\test\\output1\\cs1"),data, StandardOpenOption.CREATE);
    }
}
