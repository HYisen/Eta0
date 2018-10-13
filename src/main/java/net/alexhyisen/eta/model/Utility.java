package net.alexhyisen.eta.model;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.stream.IntStream;

/**
 * Created by Alex on 2017/2/14.
 * Some valuable functions.
 */
public class Utility {
    @Nullable
    public static byte[] download(String url) {
        //Utility.stamp("download 0");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0");
            is = connection.getInputStream();
            byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                baos.write(byteChunk, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //Utility.stamp("download 1");
        byte[] data = baos.toByteArray();
        if (data.length < 1000) {
            System.out.println("data is suspiciously small from " + url);
        }
        return data;
    }

    @Nullable
    public static byte[] clean(String url) {
        return clean(download(url));
    }

    @Nullable
    public static byte[] clean(@Nullable byte[] source) {
        //Utility.stamp("clean start");
        if (source == null) {
            return null;
        }

        CleanerProperties props = new CleanerProperties();

        props.setTranslateSpecialEntities(true);
        props.setTransResCharsToNCR(true);
        props.setOmitComments(true);

        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode tg;
        try {
            //cleaner.clean(new URL(url)) failed to get the correct charset.
            //page "http://www.fhxiaoshuo.com/read/67/67220/" as an example.
            //Utility.stamp("clean 0");
            tg = cleaner.clean(new ByteArrayInputStream(source));
            //Utility.stamp("clean 1");

            //Utility.stamp("check 0");
            final String KEY = "charset=";
            TagNode node = tg
                    .findElementByAttValue("http-equiv", "Content-Type", true, false);
            String charset;
            if (node != null) {
                String content = node.getAttributeByName("content");
                charset = content.substring(content.indexOf(KEY) + KEY.length());
            } else {
                charset = "UTF-8";
            }

            //System.out.println("charset = "+charset+" | "+props.getCharset());
            if (!"utf-8".equalsIgnoreCase(charset)) {
                tg = cleaner.clean(new ByteArrayInputStream(source), charset);
            }
            //Utility.stamp("check 1");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        //Utility.stamp("output 0");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            new PrettyXmlSerializer(props).writeToStream(tg, os, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //Utility.stamp("output 1");
        return os.toByteArray();
    }

    public static void log(String msg) {
        String message = LocalDateTime.now() + " " + msg + "\n";
        System.out.print(message);

        try {
            var path = Paths.get(".", "log");
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(
                    path,
                    message.getBytes(),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static IntStream revRange(int from, int to) {
        return IntStream.range(to, from).map(i -> from - 1 - (i - to));
    }

    private static Instant record = null;

    public static void stamp(String msg) {
        if (record == null) {
            record = Instant.now();
        }
        long interval = Duration.between(record, Instant.now()).toMillis();
        record = Instant.now();
        System.out.printf("T+%4dms %s\n", interval, msg);
    }
}
