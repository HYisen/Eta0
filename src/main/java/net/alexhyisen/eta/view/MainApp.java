package net.alexhyisen.eta.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Chapter;

import java.io.IOException;

public class MainApp extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Eta");

        FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/main.fxml"));

        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.show();

        loader.<MainController>getController().setMainApp(this);
    }

    public void showPage(Book book, Chapter chapter) {
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/page.fxml"));

            Stage pageStage = new Stage();
            pageStage.setTitle("Chapter Page");
            pageStage.initModality(Modality.NONE);
            pageStage.initOwner(primaryStage);
            pageStage.setScene(new Scene(loader.load()));

            PageController controller = loader.getController();
            controller.setBook(book);
            controller.setChapter(chapter);
            controller.refresh();

            pageStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
