package net.alexhyisen.eta.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Config;
import net.alexhyisen.eta.model.Source;

import javafx.scene.control.TableView;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

public class Controller{
    private Config config=new Config();
    private Source source=new Source();
    private Logger logger;

    @FXML private TableView<Map.Entry<String,String>> configTableView;
    @FXML private TableView<Book> sourceTableView;
    @FXML private Label msgLabel;

    private void initConfigTableView(){
        TableColumn<Map.Entry<String,String>, String> keyColumn = new TableColumn<>("key");
        keyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getKey()));

        TableColumn<Map.Entry<String,String>, String> valueColumn = new TableColumn<>("value");
        valueColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue()));
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(event -> config.put(event.getRowValue().getKey(),event.getNewValue()));
        valueColumn.setEditable(true);

        //noinspection unchecked
        configTableView.getColumns().setAll(keyColumn,valueColumn);

        configTableView.setEditable(true);
    }

    private void refreshConfigTableView(){
        configTableView.setItems(FXCollections.observableArrayList(config.getData().entrySet()));
    }

    private TableColumn<Book, String> generateColumn(String property, EventHandler<TableColumn.CellEditEvent<Book, String>> handler){
        TableColumn<Book, String> col=new TableColumn<>(property);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(handler);
        col.setEditable(true);
        return col;
    }

    private void initSourceTableView(){
        //noinspection unchecked
        sourceTableView.getColumns().setAll(
                generateColumn("name",event -> event.getRowValue().setName(event.getNewValue())),
                generateColumn("path",event -> event.getRowValue().setPath(event.getNewValue())),
                generateColumn("source",event -> event.getRowValue().setSource(event.getNewValue()))
        );

        sourceTableView.setEditable(true);
    }

    private void refreshSourceTableView(){
        sourceTableView.setItems(FXCollections.observableArrayList(source.getData()));
    }

    @FXML private void initialize(){
        logger=new Logger(msgLabel);

        initConfigTableView();
        initSourceTableView();

        handleLoadConfigButtonAction();
        handleLoadSourceButtonAction();
    }

    @FXML protected void handleSaveConfigButtonAction(){
        logger.push("save config");
        config.save();
    }

    @FXML protected void handleLoadConfigButtonAction(){
        logger.push("load config");
        config.load();
        refreshConfigTableView();
    }

    @FXML protected void handleSaveSourceButtonAction(){
        logger.push("save source");
        source.save();
    }

    @FXML protected void handleLoadSourceButtonAction(){
        logger.push("load source");
        source.load();
        refreshSourceTableView();
    }

    @FXML protected void handleAppendSourceButtonAction(){
        logger.push("append source");
        Book book=new Book("undefined link","uncertain path","untitled");
        source.getData().add(book);
        sourceTableView.getItems().add(book);
    }

    @FXML protected void handleDeleteSourceButtonAction(){
        logger.push("delete source");
        Book book=sourceTableView.getFocusModel().getFocusedItem();
        source.getData().remove(book);
        sourceTableView.getItems().remove(book);
    }
}
