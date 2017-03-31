package net.alexhyisen.eta.view;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import net.alexhyisen.eta.model.*;
import net.alexhyisen.eta.model.mailer.Mail;
import net.alexhyisen.eta.model.mailer.MailService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainController {
    private MainApp mainApp;

    private Config config;
    private Source source;

    private Logger logger;

    private ObservableList<Book> data;
    private Book currentBook;

    @FXML private Label msgLabel;
    @FXML private TextField configPathTextField;
    @FXML private TextField configNameTextField;
    @FXML private TableView<Map.Entry<String,String>> configTableView;
    @FXML private TextField sourcePathTextField;
    @FXML private TextField sourceNameTextField;
    @FXML private TableView<Book> sourceTableView;
    @FXML private TableView<Book> bookTableView;
    @FXML private TreeTableView<Chapter> chapterTreeTableView;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

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
                MainController.<Book,String>generatePropertyColumn("name"),
                MainController.<Book,Boolean>generatePropertyColumn("opened"),
                MainController.<Book,Boolean>generatePropertyColumn("cached")
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
                generatePropertyTreeColumn("name",350),
                generatePropertyTreeColumn("cached",50),
                generatePropertyTreeColumn("loaded",50)
        );
        chapterTreeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private Path getFile(TextField pathTextField,TextField nameTextField){
        return Paths.get(pathTextField.getText(),nameTextField.getText());
    }

    private void setFile(File orig,TextField pathTextField,TextField nameTextField){
        if(orig!=null){
            pathTextField.setText(orig.getParent());
            nameTextField.setText(orig.getName());
        }
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

        //initTextFields
        Config baseConfig=new Config();
        baseConfig.load();
        String lastConfig=Optional.ofNullable(baseConfig.get("lastConfig")).orElse(".\\config");
        String lastSource=Optional.ofNullable(baseConfig.get("lastSource")).orElse(".\\source");
        setFile(Paths.get(lastConfig).toAbsolutePath().toFile(), configPathTextField,configNameTextField);
        setFile(Paths.get(lastSource).toAbsolutePath().toFile(), sourcePathTextField,sourceNameTextField);

        handleLoadConfigButtonAction();
        handleLoadSourceButtonAction();
    }

    @FXML protected void handleOpenConfigButtonAction(){
        setFile(mainApp.openFile("Select Config"),configPathTextField,configNameTextField);
    }

    @FXML protected void handleSaveConfigButtonAction(){
        logger.push("save config");
        config.save(getFile(configPathTextField,configNameTextField));
    }

    @FXML protected void handleLoadConfigButtonAction(){
        logger.push("load config");
        config.load(getFile(configPathTextField,configNameTextField));
        configTableView.setItems(FXCollections.observableArrayList(config.getData().entrySet()));
    }

    @FXML protected void handleOpenSourceButtonAction(){
        setFile(mainApp.openFile("Select Source"),sourcePathTextField,sourceNameTextField);
    }

    @FXML protected void handleSaveSourceButtonAction(){
        logger.push("save source");
        source.setData(data.stream().collect(Collectors.toList()));
        source.save(getFile(sourcePathTextField,sourceNameTextField));
    }

    @FXML protected void handleLoadSourceButtonAction(){
        logger.push("load source");
        source.load(getFile(sourcePathTextField,sourceNameTextField));
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
    @FXML protected void handleOpenBookButtonAction(){
        if (isOpening.getAndSet(true)) {
            logger.push("reject because another opening is in process");
        } else {
            Book book=bookTableView.getSelectionModel().getSelectedItem();
            logger.push("open Book "+book.getName());

            CompletableFuture
                    .runAsync(book::open) //release the most time consuming procedure to avoid block
                    .thenRun(() -> isOpening.set(false))
                    .thenRun(() -> Platform.runLater(() -> updateChapterTreeTableView(book)));
        }
    }

    private void updateChapterTreeTableView(Book orig){
        TreeItem<Chapter> root=new TreeItem<>(new Chapter("",0,orig.getName(),""));
        chapterTreeTableView.setRoot(root);

        orig.getChapters().stream()
                .map(TreeItem::new)
                .forEach(root.getChildren()::add);

        chapterTreeTableView.getRoot().setExpanded(true);

        bookTableView.refresh();//need to force refresh so that status in isOpened column would turn true

        currentBook =orig;
    }

    private void checkSelectedBookThenOperateThenShow(Consumer<Book> operation, String actionName){
        Book book=bookTableView.getSelectionModel().getSelectedItem();
        if(book.isOpened()){
            logger.push(actionName+" Book "+book.getName());
            operation.accept(book);
            updateChapterTreeTableView(book);
        }else {
            logger.push("need to be opened before "+actionName+"ing");
        }
    }

    @FXML protected void handleReadBookButtonAction(){
        checkSelectedBookThenOperateThenShow(book -> book.read(20), "read");
    }

    @FXML protected void handleShowBookButtonAction(){
        checkSelectedBookThenOperateThenShow(book -> {} ,"show");
    }

    @FXML protected void handleViewChapterButtonAction(){
        chapterTreeTableView.getSelectionModel().getSelectedItems().stream()
                .map(TreeItem::getValue)
                .peek(v->{
                    try {
                        mainApp.showPage(currentBook,v);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .map(v->"chapter "+v.getName()+" is viewed")
                .forEach(logger::push);
    }

    @FXML protected void handleSaveChapterButtonAction(){
        chapterTreeTableView.getSelectionModel().getSelectedItems().stream()
                .map(TreeItem::getValue)
                .peek(Chapter::download)
                .peek(Chapter::write)
                .forEach(v->logger.push("save"+v.getName()));
    }

    @FXML protected void handleMailChapterButtonAction(){
        MailService ms=new MailService(config);
        chapterTreeTableView.getSelectionModel().getSelectedItems().stream()
                .map(TreeItem::getValue)
                .map(v->new Mail(config, currentBook.getName(),v))
                .forEach(mail -> {
                    try {
                        ms.send(mail);
                        logger.push("succeed to mail "+mail.getSubject());
                    } catch (IOException e) {
                        e.printStackTrace();
                        logger.push("failed to mail "+mail.getSubject());
                    }
                });
    }

    void handleCloseEvent(){
        Config baseConfig=new Config();
        baseConfig.load();
        baseConfig.put("lastConfig",getFile(configPathTextField,configNameTextField).toString());
        baseConfig.put("lastSource",getFile(sourcePathTextField,sourceNameTextField).toString());
        baseConfig.save();
    }
}
