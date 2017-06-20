package net.alexhyisen.eta.model.mailer;

import net.alexhyisen.eta.model.Config;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Created by Alex on 2017/3/5.
 * MailService is the service that sending Mails.
 */
public class MailService {
    private String client;
    private String server;
    private String username;
    private String password;

    private String indent;

    public MailService(String client, String server, String username, String password,String indent) {
        this.client = client;
        this.server = server;
        this.username = username;
        this.password = password;
        this.indent = indent;
    }

    public MailService(Config config) {
        this(
                config.get("client"),
                config.get("server"),
                config.get("username"),
                config.get("password"),
                config.get("textIndent")
        );
    }

    public void setClient(String client) {
        this.client = client;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private boolean passCheckpoint(String orig, String code, String msg){
        if(!orig.startsWith(code)){
            System.out.println("failed to pass checkpoint "+code+" "+msg+
                    "\nbut receive "+orig);
            return false;
        }
        return true;
    }

    private String composeIdentity(@Nullable String name, String addr){
        if(name==null){
            return addr;
        }else {
            return String.format("%s <%s>",name,addr);
        }
    }

    public boolean send(Mail mail) throws IOException {
        try (Client client=new NettyClient()){
            client.link(server,25);

            //shake hand
            client.receive();
            client.send("EHLO "+ this.client);
            client.receive(7);

            //authentication
            client.send("AUTH LOGIN");
            client.receive();
            client.send(new String(Base64.getEncoder().encode(username.getBytes())));
            client.receive();
            client.send(new String(Base64.getEncoder().encode(password.getBytes())));
            if(!passCheckpoint(client.receive(),"235","Authentication successful")){
                return false;
            }

            //transmit mail envelope
            client.send(String.format("MAIL FROM:<%s>",mail.getSenderAddr()));
            passCheckpoint(client.receive(), "250", "Mail OK");
            client.send(String.format("RCPT TO:<%s>",mail.getRecipientAddr()));
            passCheckpoint(client.receive(), "250", "Mail OK");

            //transmit mail content
            client.send("DATA");
            client.receive();
            client.send("Date: "+
                    ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
            client.send(String.format("From: %s",
                    composeIdentity(mail.getSenderName(),mail.getSenderAddr())));
            client.send("Subject: "+mail.getSubject());
            client.send(String.format("To: %s",
                    composeIdentity(mail.getRecipientName(),mail.getRecipientAddr())));
            client.send("");
            for(String line:mail.getContent()){
                client.send(indent +line);
            }
            client.send(".");
            if(!passCheckpoint(client.receive(), "250", "Mail OK")){
                return false;
            }

            //bye
            client.send("QUIT");
            return passCheckpoint(client.receive(), "221", "Bye");
        }
    }

    public static void main(String[] args) throws IOException {
        Config config=new Config();
        config.load();

        MailService ms=new MailService(config);

        String[] content={
                "Nothing serious",
                "没什么大不了的",
                "なんでもないや"
        };

        Mail mail=new Mail(
                config.get("senderName"),config.get("senderAddr"),
                config.get("recipientName"),config.get("recipientAddr"),
                "SMTP_测试邮件",content);
        ms.send(mail);
    }
}
