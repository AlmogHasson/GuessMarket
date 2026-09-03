package fx;

import api.GMController;
import dto.*;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {
    private final GMController controller = new GMController();    //engine controller

    private BooleanProperty fileLoadedProperty;

    @FXML private ComboBox<String> themeComboBox;

    @FXML private BorderPane rootPane;

    @FXML private Label balance;

    @FXML private Label comissionPaid;

    @FXML private ComboBox<Integer> commissionFilter;

    @FXML private Label eventBalance;

    @FXML private TableColumn<EventSummaryDTO,String> eventListCommissionCol;

    @FXML private TableColumn<EventSummaryDTO, String> eventListEventCol;

    @FXML private TableColumn<EventSummaryDTO, Integer> eventListIdCol;

    @FXML private TableColumn<EventSummaryDTO, String> eventListMethodCol;

    @FXML private TableColumn<EventSummaryDTO, String> eventListStatusCol;

    @FXML private ComboBox<String> commissionTypeFilter;

    @FXML private TableColumn<EventSummaryDTO, String> eventListCommissionTypeCol;

    @FXML private Tab eventsTab;

    @FXML private TableView<EventSummaryDTO> eventsTable;

    @FXML private Label filePath;

    @FXML private Label headline;

    @FXML private Button loadFileBtn;

    @FXML private ComboBox<String> methodFilter;

    @FXML private Label option1Label;

    @FXML private TableColumn<TradeDTO, Double> option1PaidCol;

    @FXML private Label option1Shares;

    @FXML private TableColumn<TradeDTO, Integer> option1SharesCol;

    @FXML private TableView<TradeDTO> option1Table;

    @FXML private TableColumn<?, ?> option1UserCol;

    @FXML private VBox option1VBox;

    @FXML private Label option1Value;

    @FXML private Label option2Label;

    @FXML private TableColumn<?, ?> option2PaidCol;

    @FXML private Label option2Shares;

    @FXML private TableColumn<?, ?> option2SharesCol;

    @FXML private TableView<?> option2Table;

    @FXML private TableColumn<?, ?> option2UserCol;

    @FXML private VBox option2VBox;

    @FXML private Label option2Value;

    @FXML private TableColumn<?, ?> participationCommissionCol;

    @FXML private TableColumn<?, ?> participationOptionCol;

    @FXML private TableColumn<?, ?> participationPaidCol;

    @FXML private TableColumn<?, ?> participationSharesCol;

    @FXML private TableView<?> participationTable;

    @FXML private TableColumn<?, ?> participationUserCol;

    @FXML private ProgressBar progressBar;

    @FXML private TableColumn<?, ?> singleEventCommissionCol;

    @FXML private TableColumn<?, ?> singleEventOptionCol;

    @FXML private TableColumn<?, ?> singleEventPaidCol;

    @FXML private TableColumn<?, ?> singleEventSharesCol;

    @FXML private TableView<?> singleEventTable;

    @FXML private ComboBox<String> statusFilter;

    @FXML private TableColumn<?, ?> userEventCol;

    @FXML private TableColumn<?, ?> userEventInvestmentCol;

    @FXML private TableColumn<?, ?> userEventRoleCol;

    @FXML private TableColumn<?, ?> userEventSharesCol;

    @FXML private TableView<?> userEventsTable;

    @FXML private TableColumn<?, ?> userListIdCol;

    @FXML private TableColumn<?, ?> userListTypeCol;

    @FXML private TableColumn<?, ?> userListUserCol;

    @FXML private Tab usersTab;

    @FXML private TableView<?> usersTable;



    @FXML private StackPane eventDetailsContent;
    @FXML private GridPane orderBookDetailsPane;
    @FXML private VBox lmsrDetailsPane;
    @FXML private Label lmsrEventName;
    @FXML private Label lmsrEventStatus;
    @FXML private Label lmsrAccountBalance;
    @FXML private Label lmsrTotalCommissionPaid;
    @FXML private TextArea lmsrEventDescription;
    @FXML private GridPane lmsrOptionsGrid;


// ---------- LMSR OPTION 1 ----------

    @FXML private Label lmsrOption1Label;
    @FXML private TableView<OptionDTO> lmsrOption1Table;
    @FXML private TableColumn<OptionDTO, Double> lmsrOption1ValueCol;
    @FXML private TableColumn<OptionDTO, Integer> lmsrOption1TotalSharesCol;
    @FXML private VBox lmsrOption1Box;


// ---------- LMSR OPTION 2 ----------

    @FXML private Label lmsrOption2Label;
    @FXML private TableView<OptionDTO> lmsrOption2Table;
    @FXML private TableColumn<OptionDTO, Double> lmsrOption2ValueCol;
    @FXML private TableColumn<OptionDTO, Integer> lmsrOption2TotalSharesCol;
    @FXML private VBox lmsrOption2Box;


// ---------- LMSR PARTICIPATION ----------

    @FXML private TableView<TradeDTO> lmsrParticipationTable;
    @FXML private TableColumn<TradeDTO, String> lmsrParticipationOptionCol;
    @FXML private TableColumn<TradeDTO, Integer> lmsrParticipationSharesCol;
    @FXML private TableColumn<TradeDTO, String> lmsrParticipationPaidCol;
    @FXML private TextField lmsrOption1BetField;
    @FXML private Button    lmsrOption1BetBtn;
    @FXML private TextField lmsrOption2BetField;
    @FXML private Button    lmsrOption2BetBtn;
    @FXML private Button   closeEventBtn;
    @FXML private CheckBox animationsCheckBox;

    private boolean animationsEnabled = true;

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
            resetProgressBar();
            filePath.setText(xmlFilePath);
            loadEvents();
            fileLoadedProperty.setValue(true);
            setFilterCommissionOptions();
            commissionFilter.getItems().addFirst(-1);

            PauseTransition hold = new PauseTransition(Duration.millis(400));
            hold.setOnFinished(done -> resetProgressBar());
            hold.play();
        });

        task.setOnFailed(e -> {
            resetProgressBar();
            DialogHelper.showErrorAlert("Load Failed" ,task.getException().getMessage());
        });

        new Thread(task).start();
    }


    // ---------- 1. close event ----------
    @FXML
    void closeEvent(ActionEvent event) {
        EventSummaryDTO selected = eventsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return;
        }

        int eventId = selected.getId();

        List<String> optionNames = selected.getOptions()
                .stream()
                .map(OptionDTO::optionName)
                .toList();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(optionNames.getFirst(), optionNames);

        dialog.setTitle("Close Event");
        dialog.setHeaderText("Closing \"" + selected.getName() + "\"");
        dialog.setContentText("Winning option:");

        Optional<String> chosen = dialog.showAndWait();

        if (chosen.isEmpty()) {
            return; // user canceled the dialog
        }

        int winningOption = optionNames.indexOf(chosen.get()) + 1;

        try {
            controller.closeEvent(eventId, winningOption);

            // Reload updated DTOs
            loadEvents();

            Platform.runLater(() -> {
                // Find the NEW DTO for the event
                EventSummaryDTO updatedEvent = eventsTable.getItems()
                        .stream()
                        .filter(e -> e.getId() == eventId)
                        .findFirst()
                        .orElse(null);

                // Reselect it
                if (updatedEvent != null) {
                    eventsTable.getSelectionModel().select(updatedEvent);
                }
            });
            showLmsrView(controller.getEventTradingStatus(eventId));

        } catch (IllegalArgumentException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Guess Market");
            alert.setHeaderText("Could not close the event");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
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

        eventListCommissionTypeCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().comission().commissionType()));

        Platform.runLater(() -> {
            eventsTable.getItems().setAll(events);
        });
    }

    private void setupBetControls() {
        restrictToPositiveInteger(lmsrOption1BetField);
        restrictToPositiveInteger(lmsrOption2BetField);

        BooleanBinding noOpenEventSelected = Bindings.createBooleanBinding(
                () -> {
                    EventSummaryDTO selected =
                            eventsTable.getSelectionModel().getSelectedItem();

                    return selected == null || !selected.isOpen();
                },
                eventsTable.getSelectionModel().selectedItemProperty()
        );

        BooleanBinding disableBetButtons =
                fileLoadedProperty.not().or(noOpenEventSelected);

        lmsrOption1BetBtn.disableProperty().bind(
                disableBetButtons.or(
                        lmsrOption1BetField.textProperty().isEmpty()
                )
        );

        lmsrOption2BetBtn.disableProperty().bind(
                disableBetButtons.or(
                        lmsrOption2BetField.textProperty().isEmpty()
                )
        );
    }

    private void setupHeaderControls() {
        themeComboBox.getItems().setAll("Light", "Dark", "Japanese");
        themeComboBox.setValue("Light");

        animationsEnabled = animationsCheckBox.isSelected();
    }


    /**
     * Rejects any keystroke that would leave the field holding something other than
     * a positive integer. A TextFormatter filters the change BEFORE it is applied,
     * so no invalid text ever reaches the field - this is why there is no listener
     * here undoing bad input after the fact.
     */
    private void restrictToPositiveInteger(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) {
                return change;                       // allow clearing the field
            }
            if (!next.matches("\\d+")) {
                return null;                         // reject: not all digits
            }

            if (Integer.parseInt(next) < 1) {
                return null;                         // reject: zero is not a bet
            }
            return change;
        }));
    }

    @FXML
    void placeBetOption1(ActionEvent event) {
        placeBet(1, lmsrOption1BetField);
    }

    @FXML
    void placeBetOption2(ActionEvent event) {
        placeBet(2, lmsrOption2BetField);
    }

    private void placeBet(int optionNumber, TextField field) {
        EventSummaryDTO event = eventsTable.getSelectionModel().getSelectedItem();
        int shares;
        try {
            shares = Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Enter the number of shares to buy.");
            return;
        }

        try {
            PurchaseDTO purchase = controller.participateInEvent(event.getId(), optionNumber, shares);

            // the engine returns null when the event is already closed
            if (purchase == null) {
                showError("This event is closed - no further bets can be placed.");
                return;
            }

            // refresh values, shares, balance, participations
            field.clear();
            showLmsrView(controller.getEventTradingStatus(event.getId()));

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Guess Market");
        alert.setHeaderText("Could not place the bet");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void displayOrderBookEventDetails(EventTradingStatusDTO singleEvent) {
        displayOrderBookOptionDetails(singleEvent.optionTradingStatus().getFirst(), option1VBox, option1Label,
                singleEvent.tradingHistory().stream()
                        .filter(trade ->
                                Objects.equals(trade.optionName(), singleEvent.optionTradingStatus().getFirst().optionName()))
                        .toList());
        displayOrderBookOptionDetails(singleEvent.optionTradingStatus().get(1), option2VBox, option2Label,
                singleEvent.tradingHistory().stream().
                        filter(trade ->
                                Objects.equals(trade.optionName(), singleEvent.optionTradingStatus().get(1).optionName()))
                        .toList());
    }

    private void resetProgressBar() {
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
        loadFileBtn.disableProperty().unbind();
        loadFileBtn.setDisable(false);
    }

    private void displayOrderBookOptionDetails(
            OptionDTO option, VBox optionBox,
            Label optionLabel, List<TradeDTO> trades)
    {
        optionLabel.setText(option.getOptionName());
        HBox hBox = (HBox) optionBox.getChildren().getFirst();
        ((Label) hBox.getChildren().get(1)).setText(String.valueOf(option.getCurrentValue()));
        ((Label) hBox.getChildren().get(3)).setText(String.valueOf(option.getTotalSharesBought()));
        option1PaidCol.setCellValueFactory(c->
                new ReadOnlyObjectWrapper<>(c.getValue().pricePaid()));
        option1SharesCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().sharesBought()));
        //TODO::enable when we have users
//        option1UserCol.setCellValueFactory(c ->
//                new ReadOnlyStringWrapper(c.getValue().getUserName()));
    }




    // ---------------------------------INITIALIZERS-----------------------------

    public void initialize() {
        initFilters();
        themeComboBox.getItems().add("Dark");
        themeComboBox.getItems().add("Light");
        themeComboBox.getItems().add("Japanese");
        initializeLmsrTables();
        setupBetControls();
        setupHeaderControls();
        closeEventBtn.setDisable(true);
        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    updateCloseEventButton();
                });

        //disable selection for participation and option tables
        participationTable.setSelectionModel(null);
        lmsrParticipationTable.setSelectionModel(null);
        lmsrOption1Table.setSelectionModel(null);
        lmsrOption2Table.setSelectionModel(null);

        eventsTable.getSelectionModel().selectedItemProperty().addListener((
                obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Load event details into the UI
                EventTradingStatusDTO singleEvent = controller.getEventTradingStatus(newSelection.getId());
//                displayOrderBookEventDetails(singleEvent);

                if (newSelection.getMethod() instanceof LMSRDTO) {
                    showLmsrView(singleEvent);
                } else {
                    showOrderBookView(singleEvent);
                }
            }
        });
    }

    private void updateCloseEventButton() {
        Platform.runLater(()-> {
            EventSummaryDTO selected = eventsTable.getSelectionModel().getSelectedItem();

            closeEventBtn.setDisable(selected == null || !selected.isOpen());

            closeEventBtn.setText(
                    selected != null && !selected.isOpen()
                            ? "Event Closed"
                            : "Close Event"
            );
        });
    }

    private void initializeLmsrTables() {

        lmsrOption1ValueCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().currentValue()));

        lmsrOption1TotalSharesCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().totalSharesBought()));


        lmsrOption2ValueCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().currentValue()));

        lmsrOption2TotalSharesCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().totalSharesBought()));


        lmsrParticipationOptionCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().optionName()));

        lmsrParticipationSharesCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().sharesBought()));

        lmsrParticipationPaidCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        String.format("%.2f", cell.getValue().pricePaid())
                )
        );
    }

    private void initFilters() {
        fileLoadedProperty = new SimpleBooleanProperty(false);
        setAllOptionOnCommissionFilter();
        commissionFilter.disableProperty().bind(fileLoadedProperty.not());
        methodFilter.disableProperty().bind(fileLoadedProperty.not());
        statusFilter.disableProperty().bind(fileLoadedProperty.not());
        commissionTypeFilter.disableProperty().bind(fileLoadedProperty.not());
        commissionTypeFilter.getItems().add("All");
        commissionTypeFilter.getItems().add("On-Close");
        commissionTypeFilter.getItems().add("On-Purchase");
        methodFilter.getItems().add("All");
        methodFilter.getItems().add("Lmsr");
        methodFilter.getItems().add("Order Book");
        statusFilter.getItems().add("All");
        statusFilter.getItems().add("Open");
        statusFilter.getItems().add("Closed");

    }

    //------------------------------------------------------
    private void showLmsrView(EventTradingStatusDTO event) {
        Platform.runLater(() -> {
            eventDetailsContent.getChildren().setAll(lmsrDetailsPane);
            lmsrEventName.setText(event.eventName());
            lmsrEventStatus.setText(event.isOpen() ? "Open" : "Closed");

            lmsrAccountBalance.setText(String.format("%.2f",  event.accountBalance()));

            lmsrTotalCommissionPaid.setText(String.format("%.2f", event.totalCommissionPaid()));

            lmsrEventDescription.setText(eventsTable.getSelectionModel().getSelectedItem().getDescription());

            var options = event.optionTradingStatus();

            displayLmsrOptionDetails(options.getFirst(), lmsrOption1Box);
            displayLmsrOptionDetails(options.get(1), lmsrOption2Box);

            lmsrParticipationTable.setItems(
                    FXCollections.observableArrayList(
                            event.tradingHistory()
                    )
            );
        });


    }


    private void displayLmsrOptionDetails(OptionDTO option, VBox optionBox) {
        Label title = (Label) optionBox.getChildren().getFirst();
        title.setText(String.valueOf(option.optionName()));

        @SuppressWarnings("unchecked")
        TableView<OptionDTO> table = (TableView<OptionDTO>) optionBox.getChildren().get(1);

        @SuppressWarnings("unchecked")
        TableColumn<OptionDTO, String> valueCol = (TableColumn<OptionDTO, String>) table.getColumns().get(0);
        @SuppressWarnings("unchecked")
        TableColumn<OptionDTO, String> sharesCol = (TableColumn<OptionDTO, String>) table.getColumns().get(1);

        valueCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        String.format("%.2f", c.getValue().currentValue())
                )
        );

        sharesCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().totalSharesBought()))
        );

        ObservableList<OptionDTO> items = FXCollections.observableArrayList(option);
        table.setItems(items);
    }

    private void showOrderBookView(EventTradingStatusDTO event) {

        eventDetailsContent.getChildren().setAll(orderBookDetailsPane);

        // Existing ORDER BOOK population logic goes here.
    }

    private void setAllOptionOnCommissionFilter() {
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
    }

    private Task<Void> createProgressTask(String xmlFilePath) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i <= 100; i++) {
                    updateProgress(i, 100);
                    Thread.sleep(15);
                }
                controller.loadFile(xmlFilePath);
                return null;
            }
        };
    }

    private void filterEvents() {
        Integer selectedCommission = commissionFilter.getValue();
        String selectedMethod = methodFilter.getValue();
        String selectedStatus = statusFilter.getValue();
        String selectedCommissionType = commissionTypeFilter.getValue();

        ObservableList<EventSummaryDTO> filteredEvents = FXCollections.observableArrayList(
                controller.getEvents().stream()
                        .filter(evnt -> selectedCommission == null
                                || selectedCommission == -1
                                || evnt.getCommission().value() == selectedCommission)
                        .filter(evnt -> selectedMethod == null
                                || "All".equals(selectedMethod)
                                || ("Lmsr".equals(selectedMethod) && evnt.getMethod() instanceof LMSRDTO)
                                || ("Order Book".equals(selectedMethod) && !(evnt.getMethod() instanceof LMSRDTO)))
                        .filter(evnt -> selectedStatus == null
                                || "All".equals(selectedStatus)
                                || ("Open".equals(selectedStatus) && evnt.isOpen())
                                || ("Closed".equals(selectedStatus) && !evnt.isOpen()))
                        .filter(evnt -> selectedCommissionType == null
                                || "All".equals(selectedCommissionType)
                                || selectedCommissionType.equalsIgnoreCase(evnt.getCommission().commissionType()))
                        .toList()
        );

        eventsTable.setItems(filteredEvents);
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

    @FXML
    void onCommissionTypeFilterChanged(ActionEvent event) {
        filterEvents();
    }


    @FXML
    void toggleAnimations(ActionEvent event) {
        animationsEnabled = animationsCheckBox.isSelected();
    }

    /**
     * Theme swap, honouring the toggle.
     * The progress bar is deliberately untouched by this flag - its animation is
     * feedback about work in progress, not decoration.
     */
    @FXML
    void setTheme(ActionEvent event) {
        String selectedTheme = themeComboBox.getValue().toLowerCase();
        String url = Objects.requireNonNull(
                getClass().getResource("themes/" + selectedTheme + ".css")).toExternalForm();

        if (!animationsEnabled) {
            rootPane.getScene().getStylesheets().setAll(url);
            rootPane.setOpacity(1.0);          // in case a fade was interrupted
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), rootPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            try {
                rootPane.getScene().getStylesheets().setAll(url);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), rootPane);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        });
        fadeOut.play();
    }

}
