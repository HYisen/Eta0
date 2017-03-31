package net.alexhyisen.eta.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Chapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class MainApp extends Application {
    private MainController mainController;
    private Stage primaryStage;

    private static FileChooser fileChooser;
    static {
        //initiate fileChooser
        fileChooser=new FileChooser();
        fileChooser.setInitialDirectory(Paths.get(".").toFile());
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Eta");

        FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/main.fxml"));

        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.show();

        mainController=loader.getController();
        mainController.setMainApp(this);
    }

    @Override
    public void stop() throws Exception {
        mainController.handleCloseEvent();
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
            controller.initSpinner(mainController.getConfig());
            controller.refresh();

            pageStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File openFile(String title){
        fileChooser.setTitle(title);
        return fileChooser.showOpenDialog(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}