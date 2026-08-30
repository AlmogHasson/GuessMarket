package fx;

import api.GMController;
import dto.CommissionDTO;
import dto.EventSummaryDTO;
import dto.LMSRDTO;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainController {
    private final GMController controller = new GMController();    //engine controller

    private BooleanProperty fileLoadedProperty;

    @FXML
    private Label balance;

    @FXML
    private Label comissionPaid;

    @FXML
    private ComboBox<Integer> commissionFilter;

    @FXML
    private Label eventBalance;

    @FXML
    private TableColumn<EventSummaryDTO,String> eventListCommissionCol;

    @FXML
    private TableColumn<EventSummaryDTO, String> eventListEventCol;

    @FXML
    private TableColumn<EventSummaryDTO, Integer> eventListIdCol;

    @FXML
    private TableColumn<EventSummaryDTO, String> eventListMethodCol;

    @FXML
    private TableColumn<EventSummaryDTO, String> eventListStatusCol;

    @FXML
    private Tab eventsTab;

    @FXML
    private TableView<EventSummaryDTO> eventsTable;

    @FXML
    private Label filePath;

    @FXML
    private Label headline;

    @FXML
    private Button loadFileBtn;

    @FXML
    private ComboBox<String> methodFilter;

    @FXML
    private Label option1Label;

    @FXML
    private TableColumn<?, ?> option1PaidCol;

    @FXML
    private Label option1Shares;

    @FXML
    private TableColumn<?, ?> option1SharesCol;

    @FXML
    private TableView<?> option1Table;

    @FXML
    private TableColumn<?, ?> option1UserCol;

    @FXML
    private VBox option1VBox;

    @FXML
    private Label option1Value;

    @FXML
    private Label option2Label;

    @FXML
    private TableColumn<?, ?> option2PaidCol;

    @FXML
    private Label option2Shares;

    @FXML
    private TableColumn<?, ?> option2SharesCol;

    @FXML
    private TableView<?> option2Table;

    @FXML
    private TableColumn<?, ?> option2UserCol;

    @FXML
    private VBox option2VBox;

    @FXML
    private Label option2Value;

    @FXML
    private TableColumn<?, ?> participationCommissionCol;

    @FXML
    private TableColumn<?, ?> participationOptionCol;

    @FXML
    private TableColumn<?, ?> participationPaidCol;

    @FXML
    private TableColumn<?, ?> participationSharesCol;

    @FXML
    private TableView<?> participationTable;

    @FXML
    private TableColumn<?, ?> participationUserCol;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private TableColumn<?, ?> singleEventCommissionCol;

    @FXML
    private TableColumn<?, ?> singleEventOptionCol;

    @FXML
    private TableColumn<?, ?> singleEventPaidCol;

    @FXML
    private TableColumn<?, ?> singleEventSharesCol;

    @FXML
    private TableView<?> singleEventTable;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private TableColumn<?, ?> userEventCol;

    @FXML
    private TableColumn<?, ?> userEventInvestmentCol;

    @FXML
    private TableColumn<?, ?> userEventRoleCol;

    @FXML
    private TableColumn<?, ?> userEventSharesCol;

    @FXML
    private TableView<?> userEventsTable;

    @FXML
    private TableColumn<?, ?> userListIdCol;

    @FXML
    private TableColumn<?, ?> userListTypeCol;

    @FXML
    private TableColumn<?, ?> userListUserCol;

    @FXML
    private Tab usersTab;

    @FXML
    private TableView<?> usersTable;

    @FXML
    void loadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open GuessMarket XML File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        File selectedFile = fileChooser.showOpenDialog(loadFileBtn.getScene().getWindow());
        if (selectedFile == null)
            return;

        String xmlFilePath = selectedFile.getAbsolutePath();

        Task<Void> task = createProgressTask(xmlFilePath);

        progressBar.progressProperty().bind(task.progressProperty());
        loadFileBtn.disableProperty().bind(task.runningProperty());


        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1);
            loadFileBtn.disableProperty().unbind();
            filePath.setText(xmlFilePath);
            loadEvents();
            fileLoadedProperty.setValue(true);
            setFilterCommissionOptions();
            setAllOptionOnCommissionFilter();
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            loadFileBtn.disableProperty().unbind();
            DialogHelper.showErrorAlert("Load Failed" ,task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void setFilterCommissionOptions() {
        commissionFilter.setItems(controller.getEvents().stream()
                .map(EventSummaryDTO::getCommission)
                .map(CommissionDTO::value)
                .distinct()
                .collect(Collectors.toCollection(FXCollections::observableArrayList))
        );
    }

    private void loadEvents() {
        ObservableList<EventSummaryDTO> events =
                javafx.collections.FXCollections.observableArrayList(controller.getEvents());

        eventListIdCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getId())
        );

        eventListEventCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getName()));

        eventListMethodCol.setCellValueFactory(c -> {
            var method = c.getValue().getMethod();
            String name = method instanceof LMSRDTO ? "Lmsr" : "Order Book";
            return new ReadOnlyStringWrapper(name);
        });

        eventListStatusCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().isOpen() ? "Open" : "Closed"));

        eventListCommissionCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().getCommission().value())));

        eventsTable.getItems().clear();
        eventsTable.setItems(events);
    }

    @FXML
    public void initialize() {
        fileLoadedProperty = new SimpleBooleanProperty(false);
        commissionFilter.disableProperty().bind(fileLoadedProperty.not());
        setAllOptionOnCommissionFilter();
        commissionFilter.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                if (object == null || object == -1) {
                    return "All"; // מציג "ALL" במקום null או 1-
                }
                return object.toString(); // עבור כל מספר אחר, מציג את המספר עצמו
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.equals("All")) {
                    return -1;
                }
                return Integer.parseInt(string);
            }
        });
        methodFilter.disableProperty().bind(fileLoadedProperty.not());
        statusFilter.disableProperty().bind(fileLoadedProperty.not());
        methodFilter.getItems().add("All");
        methodFilter.getItems().add("Lmsr");
        methodFilter.getItems().add("Order Book");
        statusFilter.getItems().add("All");
        statusFilter.getItems().add("Open");
        statusFilter.getItems().add("Closed");
    }

    private void setAllOptionOnCommissionFilter() {
        commissionFilter.getItems().addFirst(-1);
        commissionFilter.setCellFactory(cell -> new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("Commission");
                } else if (item == -1) {
                    setText("All");
                } else {
                    setText(item.toString());
                }
            }
        });
    }

    private Task<Void> createProgressTask(String xmlFilePath) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i <= 100; i++) {
                    updateProgress(i, 100);
                    Thread.sleep(20);
                }
                controller.loadFile(xmlFilePath);
                return null;
            }
        };
    }

    public void filterEvents(){
        eventsTable.getItems().clear();
        controller.getEvents().stream()
                .filter(evnt -> {
                    boolean commissionMatches = commissionFilter.getValue() == -1
                            || evnt.getCommission().value() == commissionFilter.getValue();
                    boolean methodMatches = methodFilter.getValue() == null
                            || evnt.getMethod().getName().equalsIgnoreCase(methodFilter.getValue())
                            || (methodFilter.getValue().equalsIgnoreCase("all"));
                    boolean statusMatches = statusFilter.getValue() == null
                            || (statusFilter.getValue().equalsIgnoreCase("open") && evnt.isOpen())
                            || (statusFilter.getValue().equalsIgnoreCase("closed") && !evnt.isOpen())
                            || (statusFilter.getValue().equalsIgnoreCase("all"));
                    return commissionMatches && methodMatches && statusMatches;
                })
                .forEach(evnt -> {
                    eventsTable.getItems().add(evnt);
                });

    }


    @FXML
    void onCommissionFilterChanged(ActionEvent event) {
        filterEvents();
    }


    @FXML
    void onMethodFilterChanged(ActionEvent event) {
        filterEvents();
    }

    @FXML
    void onStatusFilterChanged(ActionEvent event) {
        filterEvents();
    }



}
