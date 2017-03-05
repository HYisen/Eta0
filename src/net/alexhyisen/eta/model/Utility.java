package net.alexhyisen.eta.model;

import com.sun.istack.internal.Nullable;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Created by Alex on 2017/2/14.
 * Some valuable functions.
 */
public class Utility {
    @Nullable static byte[] download(String url){
        //Utility.stamp("download 0");
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
            return null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
            //Utility.stamp("download 1");
        return baos.toByteArray();
    }

    @Nullable static byte[] clean(String url){
        return clean(download(url));
    }

    @Nullable static byte[] clean(@Nullable byte[] source){
        //Utility.stamp("clean start");
        if(source==null){
            return null;
        }

        CleanerProperties props = new CleanerProperties();

        props.setTranslateSpecialEntities(true);
        props.setTransResCharsToNCR(true);
        props.setOmitComments(true);

        HtmlCleaner cleaner=new HtmlCleaner();
        TagNode tg;
        try {
            //cleaner.clean(new URL(url)) failed to get the correct charset.
            //page "http://www.fhxiaoshuo.com/read/67/67220/" as an example.
            //Utility.stamp("clean 0");
            tg=cleaner.clean(new ByteArrayInputStream(source));
            //Utility.stamp("clean 1");

            //Utility.stamp("check 0");
            final String KEY="charset=";
            String content=tg
                    .findElementByAttValue("http-equiv","Content-Type",true,false)
                    .getAttributeByName("content");
            String charset=content.substring(content.indexOf(KEY)+KEY.length());
            //System.out.println("charset = "+charset+" | "+props.getCharset());
            if(!"utf-8".equalsIgnoreCase(charset)){
                tg=cleaner.clean(new ByteArrayInputStream(source),charset);
            }
            //Utility.stamp("check 1");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        //Utility.stamp("output 0");
        ByteArrayOutputStream os=new ByteArrayOutputStream();//Maybe I should set a size, which is obviously larger than 100kb.
        try {
            new PrettyXmlSerializer(props).writeToStream(tg,os,"utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //Utility.stamp("output 1");
        return os.toByteArray();
    }

    static void log(String msg){
        System.out.println(LocalTime.now()+" "+msg);
    }

    private static Instant record=null;
    static void stamp(String msg){
        if(record==null){
            record=Instant.now();
        }
        long interval= Duration.between(record,Instant.now()).toMillis();
        record=Instant.now();
        System.out.printf("T+%4dms %s\n",interval,msg);
    }

    public static void main(String[] args) throws IOException {
        byte[] data=Utility.clean("http://www.fhxiaoshuo.com/read/67/67220/");
        //byte[] data=Utility.clean("http://www.biqudao.com/bqge1081/");
        Files.write(Paths.get("D:\\Code\\test\\output1\\cs1"),data, StandardOpenOption.CREATE);
    }
}
