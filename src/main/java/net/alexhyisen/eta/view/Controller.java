package net.alexhyisen.eta.view;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Chapter;
import net.alexhyisen.eta.model.Config;
import net.alexhyisen.eta.model.Source;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class Controller{
    private Config config;
    private Source source;
    private Logger logger;

    private ObservableList<Book> data;

    @FXML private Label msgLabel;
    @FXML private TableView<Map.Entry<String,String>> configTableView;
    @FXML private TableView<Book> sourceTableView;
    @FXML private TableView<Book> bookTableView;
    @FXML private TreeTableView<Chapter> chapterTreeTableView;

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

    private static TableColumn<Book, String>
    generateBookColumn(String property, EventHandler<TableColumn.CellEditEvent<Book, String>> handler){
        TableColumn<Book, String> col=generatePropertyColumn(property);
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(handler);
        col.setEditable(true);
        return col;
    }

    private void initSourceTableView(){
        //noinspection unchecked
        sourceTableView.getColumns().setAll(
                generateBookColumn("name", event -> event.getRowValue().setName(event.getNewValue())),
                generateBookColumn("path", event -> event.getRowValue().setPath(event.getNewValue())),
                generateBookColumn("source", event -> event.getRowValue().setSource(event.getNewValue()))
        );

        sourceTableView.setEditable(true);

        sourceTableView.setItems(data);
    }

    private static <S,T> TableColumn<S,T> generatePropertyColumn(String property){
        TableColumn<S,T> col= new TableColumn<>(property);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    private void initBookTableView(){
        //noinspection unchecked
        bookTableView.getColumns().setAll(
                Controller.<Book,String>generatePropertyColumn("name"),
                Controller.<Book,Boolean>generatePropertyColumn("opened"),
                Controller.<Book,Boolean>generatePropertyColumn("cached")
        );
        //Clarity of generic type is a fortune from C++,
        //despite the fact that the type erasure implement in Java made it worthless.

        bookTableView.setItems(data);
    }

    //As usually TreeItem can not set its column width properly.
    private static <S,T> TreeTableColumn<S,T> generatePropertyTreeColumn(String property,double width){
        TreeTableColumn<S,T> col= new TreeTableColumn<>(property);
        col.setCellValueFactory(new TreeItemPropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    private void initChapterTreeTableView(){
        //noinspection unchecked
        chapterTreeTableView.getColumns().setAll(
                generatePropertyTreeColumn("code",100),
                generatePropertyTreeColumn("name",400),
                generatePropertyTreeColumn("cached",50)
        );
        chapterTreeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML private void initialize(){
        config=new Config();
        source=new Source();

        data=FXCollections.observableArrayList();

        logger=new Logger(msgLabel);

        initConfigTableView();
        initSourceTableView();
        initBookTableView();
        initChapterTreeTableView();

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
        configTableView.setItems(FXCollections.observableArrayList(config.getData().entrySet()));
    }

    @FXML protected void handleSaveSourceButtonAction(){
        logger.push("save source");
        source.setData(data.stream().collect(Collectors.toList()));
        source.save();
    }

    @FXML protected void handleLoadSourceButtonAction(){
        logger.push("load source");
        source.load();
        data.clear();
        data.addAll(source.getData());
    }

    @FXML protected void handleAppendSourceButtonAction(){
        logger.push("append source");
        Book book=new Book("undefined link","uncertain path","untitled");
        source.getData().add(book);
        sourceTableView.getItems().add(book);
    }

    @FXML protected void handleDeleteSourceButtonAction(){
        logger.push("delete source");
        Book book=sourceTableView.getSelectionModel().getSelectedItem();
        source.getData().remove(book);
        sourceTableView.getItems().remove(book);
    }

    //I don't use synchronize() because the queue of open tasks is meaningless,
    //as data that would not ever have a chance to be showed needn't to be created.
    private static AtomicBoolean isOpening=new AtomicBoolean(false);
    //Under most circumstance, boolean is always atomic, just in case of extreme condition.
    @FXML protected void handleOpenButtonAction(){
        if (isOpening.getAndSet(true)) {
            logger.push("reject because another opening is in process");
        } else {
            Book book=bookTableView.getSelectionModel().getSelectedItem();
            logger.push("open Book "+book.getName());
            TreeItem<Chapter> root=new TreeItem<>(new Chapter("",0,book.getName(),""));
            chapterTreeTableView.setRoot(root);

            CompletableFuture.runAsync(() -> {
                book.open();
                book.getChapters().stream()
                        .map(TreeItem::new)
                        .forEach(root.getChildren()::add);
                chapterTreeTableView.getRoot().setExpanded(true);
                bookTableView.refresh();//need to force refresh so that status in isOpened column would turn true
                isOpening.set(false);
            });
        }
    }

    @FXML protected void handleReadButtonAction(){
        bookTableView.getSelectionModel().getSelectedItems().forEach(v->v.read(20));
        bookTableView.refresh();
    }

    @FXML protected void handleMailButtonAction(){

    }

    @FXML protected void handleViewButtonAction(){

    }
}
