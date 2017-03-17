package net.alexhyisen.eta.view;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.SimpleStringProperty;
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

    @FXML private TableView<Map.Entry<String,String>> configTableView;
    @FXML private Label msgLabel;

    private void refreshConfigTableView(){
        TableColumn<Map.Entry<String,String>, String> keyColumn = new TableColumn<>("key");
        keyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getKey()));

        TableColumn<Map.Entry<String,String>, String> valueColumn = new TableColumn<>("value");
        valueColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue()));
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(event -> config.put(event.getRowValue().getKey(),event.getNewValue()));
        valueColumn.setEditable(true);

        //noinspection unchecked
        configTableView.getColumns().setAll(keyColumn,valueColumn);

        configTableView.setItems(FXCollections.observableArrayList(config.getData().entrySet()));

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
