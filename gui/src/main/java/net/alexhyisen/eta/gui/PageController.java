package net.alexhyisen.eta.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.util.converter.IntegerStringConverter;
import net.alexhyisen.Config;
import net.alexhyisen.Utility;
import net.alexhyisen.eta.book.Book;
import net.alexhyisen.eta.book.Chapter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2017/3/19.
 * The controller of a PageApp
 */
public class PageController {
    private Book book;
    private Chapter chapter;

    private Config config;

    @FXML
    private Label codeLabel;
    @FXML
    private Label bookLabel;
    @FXML
    private Label chapterLabel;
    @FXML
    private TextArea dataTextArea;
    @FXML
    private Button nextButton;
    @FXML
    private Spinner<Integer> fontSpinner;

    public void setBook(Book book) {
        this.book = book;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @SuppressWarnings("WeakerAccess")
    public void refresh() {
        codeLabel.setText(String.valueOf(chapter.getCode()));
        bookLabel.setText(book.getName());
        chapterLabel.setText(chapter.getComposedName());
        armButton();

        dataTextArea.setText(Arrays.stream(chapter.getData())
                .map(v -> config.get("textIndent") + v + "\n")
                .collect(Collectors.joining()));
    }

    void initSpinner() {
        Integer fontSize = Integer.valueOf(config.get("fontSize"));
        fontSpinner.setEditable(true);
        SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory
                (1, 96, 12);
        svf.setConverter(new IntegerStringConverter());
        fontSpinner.setValueFactory(svf);
        //svf.getValue() is not the default font size that going to be. See later comment.
        TextFormatter<Integer> formatter = new TextFormatter<>(svf.getConverter(), svf.getValue());
        formatter.valueProperty().addListener((observable, oldValue, newValue) -> {
            //Utility.log("change font size from "+oldValue+" to "+newValue);
            dataTextArea.setFont(Font.font(newValue));
            config.put("fontSize", newValue.toString());
        });
        fontSpinner.getEditor().setTextFormatter(formatter);
        svf.valueProperty().bindBidirectional(formatter.valueProperty());

        //What if setValue to the oldValue?
        //It just doesn't change, which means the Listener would not be notified.
        //Therefore, if we set the initialValue of svf to fontSize,
        //the code next line would not work properly.
        fontSpinner.getValueFactory().setValue(fontSize);
        //Utility.log("fontSize="+fontSize);
    }

    @FXML
    private void initialize() {
        dataTextArea.setEditable(false);
        dataTextArea.wrapTextProperty().set(true);
    }

    private void armButton() {
        List<Chapter> chapters = book.getChapters();
        if (chapters.lastIndexOf(chapter) == chapters.size() - 1) {
            nextButton.setDisable(true);
            Utility.log("set next button disabled");
        }
    }

    @FXML
    protected void handleNextButtonAction() {
        List<Chapter> chapters = book.getChapters();
        chapter = chapters.get(chapters.lastIndexOf(chapter) + 1);
        armButton();
        refresh();
    }
}

