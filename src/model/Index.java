package model;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Alex on 2017/1/26.
 * Index is the avatar of an index page.
 */
public class Index {
    private static class ddReader extends DefaultHandler {
        String name=null;
        String link=null;
        boolean isValid=false;
        List<Hyperlink> data;

        //Those names come from the structure of the raw XML data.


        ddReader(List<Hyperlink> data) {
            this.data = data;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            name=localName;
            //System.out.println("start "+localName);
            link=attributes.getValue("href");
            if("dd".equals(name)){
                isValid=true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            //System.out.println("end "+localName);
            if("a".equals(name)){
                isValid=false;
            }
            name=null;
            link=null;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if(isValid&&Objects.equals(name,"a")&&link!=null){
                String content=new String(ch,start,length);
                data.add(new Hyperlink(content,link));
                //System.out.println(content+"="+link);
            }
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
            final String path="D:\\Code\\";
            Utility.log("solve p="+publicId+" s="+systemId);
            String name=systemId.substring(systemId.lastIndexOf('/'));
            if(!(new File(path+name).exists())){
                Utility.log("fail to find the local schema, going to creat one.");
                URL website = new URL(systemId);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(path+name);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                Utility.log("scheme is downloaded to "+path+name);
            }

            return new InputSource(new FileInputStream(path+name));
        }

        public List<Hyperlink> getData() {
            return data;
        }
    }

    static List<Hyperlink> parse(byte[] source){
        List<Hyperlink> data=new ArrayList<>();
        try {
            SAXParserFactory spf=SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser saxParser=spf.newSAXParser();
            //Utility.log("parse alpha");
            saxParser.parse(new InputSource(new ByteArrayInputStream(source)),new ddReader(data));
            //Utility.log("parse omega");
        }catch (IOException|SAXException|ParserConfigurationException e){
            e.printStackTrace();
            throw new RuntimeException(e);

        }
        //data.forEach(v-> System.out.println(v.getText()+" -> "+v.getHref()));
        return data;
    }

    private Map<Integer,Hyperlink> data;
    private String source;
    private String path;

    Index(String source, String path) {
        this.source = source;
        this.path = path;
    }

    void process(){
        data=new TreeMap<>();
        parse(Utility.clean(source)).forEach(v->{
            String link=v.getHref();
            //noinspection StatementWithEmptyBody
            if(link.startsWith("http")){
                //like "http://www.fhxiaoshuo.com/read/67/67220/11125326.shtml"
            }else if(link.startsWith("/")){
                //like "/read/67/67220/11125326.shtml"
                try {
                    URL host=new URL(source);
                    link=new URL(host.getProtocol(),host.getHost(),link).toString();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }else{
                //like "11125326.shtml"
                link= source +link;
            }

            int code=Integer.valueOf(link.substring(link.lastIndexOf('/')+1,link.lastIndexOf('.')));

            data.put(code,new Hyperlink(v.getText(),link));
        });
    }

    Map<Integer, Hyperlink> getData() {
        return data;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }


    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
//        URL oracle = new URL("http://www.biqudao.com/bqge1081/");
//        BufferedReader in = new BufferedReader(
//                new InputStreamReader(oracle.openStream(),"UTF-8"));
//
//        System.out.println("GO");
//        String inputLine;
//        PrintStream os=new PrintStream("D:\\Code\\out");
//        while ((inputLine = in.readLine()) != null){
//            os.println(inputLine);
//            System.out.println(inputLine);
//        }
//        in.close();

        CleanerProperties props = new CleanerProperties();

        props.setTranslateSpecialEntities(true);
        props.setTransResCharsToNCR(true);
        props.setOmitComments(true);

//        HtmlCleaner cleaner=new HtmlCleaner();
//        TagNode tg=cleaner.clean(new URL("http://www.biqudao.com/bqge1081/"));
//        new PrettyXmlSerializer(props).writeToFile(
//                tg, "D:\\Code\\out", "utf-8"
//        );

        System.out.println("cleaned and output");


        Utility.log("SPF initiate");
        SAXParserFactory spf=SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        //It doesn't work to serValidating(false)
        //spf.setValidating(false);
        Utility.log("SP initiate");
        SAXParser saxParser=spf.newSAXParser();
        //XMLReader xmlReader=saxParser.getXMLReader();
        List<Hyperlink> data=new ArrayList<>();
        //xmlReader.setContentHandler(new ddReader(data));

        Utility.log("parse alpha");
        //xmlReader.parse("D:\\Code\\out");
        saxParser.parse("D:\\Code\\out",new ddReader(data));
        Utility.log("parse omega");

//        data.forEach(v-> System.out.println(v.getText()+" -> "+v.getHref()));
//
//        HtmlCleaner cleaner=new HtmlCleaner();
//        TagNode tg=cleaner.clean(new URL("http://www.biqudao.com"+data.get(10).getHref()));
//        new PrettyXmlSerializer(props).writeToFile(
//                tg, "D:\\Code\\chapter", "utf-8"
//        );

        System.out.println("END");
    }
}
