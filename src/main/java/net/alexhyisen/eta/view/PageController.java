package net.alexhyisen.eta.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Chapter;

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

