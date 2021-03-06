package net.alexhyisen.eta.book;

import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;
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
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

public class Utils {
    /**
     * stripe directory and suffix from url
     */
    static String basename(String url) {
        String tail = url.substring(url.lastIndexOf('/') + 1);

        int anchor = tail.indexOf('.');
        if (anchor == -1) {
            anchor = tail.length();
        }

        return tail.substring(0, anchor);
    }

    private static InputStream depress(String encodingNullable, InputStream compressed) throws IOException {
        if (encodingNullable == null) {
            return compressed;
        }

        switch (encodingNullable) {
            case "gzip":
                return new GZIPInputStream(compressed);
            case "deflate":
                return new DeflaterInputStream(compressed);
            default:
                throw new IOException("unsupported compression encoding " + encodingNullable);
        }
    }

    @Nullable
    static byte[] download(String url) {
        //Utility.stamp("download 0");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate"); // Brotli utils is not easy to fetch
            is = connection.getInputStream();
            is = depress(connection.getContentEncoding(), is);
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
            Utility.log(LogCls.BOOK, "data is suspiciously small from " + url);
        }

        Utility.log(LogCls.BOOK, "downloaded from " + url);
        return data;
    }

    @SuppressWarnings("unused")
    @Nullable
    public static byte[] clean(String url) {
        return clean(download(url));
    }

    private static String findCharset(TagNode tg) {
        TagNode node;

        //for <meta http-equiv="Content-Type" content="text/html; charset=gbk">
        final String KEY = "charset=";
        node = tg.findElementByAttValue(
                "http-equiv", "Content-Type", true, false);
        if (node != null) {
            String content = node.getAttributeByName("content");
            return content.substring(content.indexOf(KEY) + KEY.length());
        }

        //for <meta charset="gbk">
        node = tg.findElementHavingAttribute("charset", true);
        if (node != null) {
            return node.getAttributeByName("charset");
        }

        return "UTF-8";
    }

    @Nullable
    static byte[] clean(@Nullable byte[] source) {
//        Utility.stamp("clean start");
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
//            Utility.stamp("clean 0");
            tg = cleaner.clean(new ByteArrayInputStream(source));
//            Utility.stamp("clean 1");

//            Utility.stamp("check 0");
            String charset = findCharset(tg);
//            Utility.log(LogCls.BOOK, "charset = " + charset + " | " + props.getCharset());
            if (!"utf-8".equalsIgnoreCase(charset)) {
                tg = cleaner.clean(new ByteArrayInputStream(source), charset);
            }
//            Utility.stamp("check 1");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

//        Utility.stamp("output 0");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            new PrettyXmlSerializer(props).writeToStream(tg, os, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
//        Utility.stamp("output 1");
        return os.toByteArray();
    }
}
