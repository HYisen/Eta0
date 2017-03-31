package net.alexhyisen.eta.view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.util.converter.IntegerStringConverter;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Chapter;
import net.alexhyisen.eta.model.Config;
import net.alexhyisen.eta.model.Utility;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2017/3/19.
 * The controller of a PageApp
 */
public class PageController{
    private Book book;
    private Chapter chapter;

    @FXML private Label codeLabel;
    @FXML private Label bookLabel;
    @FXML private Label chapterLabel;
    @FXML private TextArea dataTextArea;
    @FXML private Button nextButton;
    @FXML private Spinner<Integer> fontSpinner;

    public void setBook(Book book) {
        this.book = book;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public void refresh(){
        codeLabel.setText(String.valueOf(chapter.getCode()));
        bookLabel.setText(book.getName());
        chapterLabel.setText(chapter.getName());
        armButton();

        dataTextArea.setText(Arrays.stream(chapter.getData())
                .map(v->"　　"+v+"\n")//2 GBK spaces work, it seemed that several spaces in ASCII do not exactly match.
                .collect(Collectors.joining()));
    }

    //We need to init Spinner outside explicitly to provide a Config.
    void initSpinner(Config config){
        Integer fontSize=Integer.valueOf(config.get("fontSize"));
        fontSpinner.setEditable(true);
        SpinnerValueFactory<Integer> svf= new SpinnerValueFactory.IntegerSpinnerValueFactory
                (1,96,12);
        svf.setConverter(new IntegerStringConverter());
        fontSpinner.setValueFactory(svf);
        fontSpinner.getEditor().setOnAction(event -> {
            String text=fontSpinner.getEditor().getText();
            SpinnerValueFactory<Integer> valueFactory = fontSpinner.getValueFactory();
            valueFactory.setValue(valueFactory.getConverter().fromString(text));
            //Utility.log("switch font size to "+valueFactory.getValue());
        });
        fontSpinner.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> {
            //Utility.log("change font size from "+oldValue+" to "+newValue);
            dataTextArea.setFont(Font.font(newValue));
            config.put("fontSize",newValue.toString());
        });

        //What if setValue to the oldValue?
        //It just doesn't change, which means the Listener would not be notified.
        //Therefore, if we set the initialValue of svf to fontSize,
        //the code next line would not work properly.
        fontSpinner.getValueFactory().setValue(fontSize);
        //Utility.log("fontSize="+fontSize);
    }

    @FXML private void initialize(){
        dataTextArea.setEditable(false);
        dataTextArea.wrapTextProperty().set(true);
    }

    private void armButton(){
        List<Chapter> chapters=book.getChapters();
        if(chapters.lastIndexOf(chapter)==chapters.size()-1){
            nextButton.setDisable(true);
            System.out.println("set next button disabled");
        }
    }

    @FXML protected void handleNextButtonAction() {
        List<Chapter> chapters=book.getChapters();
        chapter=chapters.get(chapters.lastIndexOf(chapter)+1);
        armButton();
        refresh();
    }
}
