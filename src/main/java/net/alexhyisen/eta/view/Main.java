package net.alexhyisen.eta.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Config;
import net.alexhyisen.eta.model.Source;
import net.alexhyisen.eta.model.Utility;
import net.alexhyisen.eta.model.mailer.Mail;
import net.alexhyisen.eta.model.mailer.MailService;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }

    private static void runAutoMode(){
        System.out.println("Auto Mode");
        System.out.println("read default config & source");
        System.out.println("config path = .\\config");
        System.out.println("source path = .\\source");
        System.out.println("update all the books and save them");
        System.out.println("mail the newly chapters to catcher");

        Config config=new Config();
        config.load();
        MailService ms=new MailService(
                config.get("client"),
                config.get("server"),
                config.get("username"),
                config.get("password")
        );
        Source source=new Source();
        source.load();
        for(Book book:source.getData()){
            book.read(20);
            book.save().forEach(chapter->{
                String subject=String.format("《%s》 %s",book.getName(),chapter.getName());
                Mail mail=new Mail(
                        config.get("senderName"),config.get("senderAddr"),
                        config.get("recipientName"),config.get("recipientAddr"),
                        subject,chapter.getData());
                try {
                    ms.send(mail);
                    Utility.log("transmitted "+subject);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        System.out.println("finished");
    }


    public static void main(String[] args) {
        if(args.length==0){
            launch(args);
        }else if(args[0].equals("-a")||args[0].equals("--auto")){
            runAutoMode();
        }
    }
}
