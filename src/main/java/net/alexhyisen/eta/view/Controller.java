package net.alexhyisen.eta.view;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import net.alexhyisen.eta.model.Config;
import net.alexhyisen.eta.model.Source;

import javafx.scene.control.TableView;

import java.util.Map;

public class Controller{
    private Config config=new Config();
    private Source source=new Source();
    private Logger logger;

    @FXML private TableView<MapItem> configTableView;
    @FXML private Label msgLabel;

    private void refreshConfigTableView(){
        TableColumn<MapItem, String> keyColumn = new TableColumn<>("key");
        keyColumn.setCellValueFactory(param -> param.getValue().keyProperty());
        keyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        keyColumn.setOnEditCommit(event -> event.getRowValue().setKey(event.getNewValue()));
        keyColumn.setEditable(true);
        TableColumn<MapItem, String> valueColumn = new TableColumn<>("value");
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(event -> event.getRowValue().setValue(event.getNewValue()));
        valueColumn.setEditable(true);
        //noinspection unchecked
        configTableView.getColumns().setAll(keyColumn,valueColumn);

        ObservableList<MapItem> data=FXCollections.observableArrayList();
        config.getData().entrySet().stream()
                .map(v->new MapItem(config.getData(),v.getKey(),v.getValue()))
                .forEach(data::add);
        System.out.println("size="+data.size());
        configTableView.setItems(data);
        configTableView.getItems().forEach(v-> System.out.println(v.getKey()+" = "+v.getValue()));
        configTableView.setEditable(true);
    }

    @FXML private void initialize(){
        logger=new Logger(msgLabel);
        config.load();
        refreshConfigTableView();
    }

    @FXML protected void handleSaveButtonAction(){
        logger.push("save config");
        config.save();
    }

    @FXML protected void handleLoadButtonAction(){
        logger.push("load config");
        config.load();
    }
}
