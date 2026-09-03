package fx;

import dto.EventSummaryDTO;
import dto.EventTradingStatusDTO;
import dto.LMSRDTO;
import dto.OptionDTO;
import dto.PurchaseDTO;
import dto.TradeDTO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Controller for right.fxml - event details, the two option panels, bets, close. */
public class RightSideController {

    private static final String EMPTY = "\u2014";   // em dash

    private MainController main;

    /**
     * The event currently on screen, as a property so the bet buttons can bind to
     * it. Treat the DTO as a snapshot: read its id, never trust its state after
     * the engine has been mutated.
     */
    private final ObjectProperty<EventSummaryDTO> currentEvent = new SimpleObjectProperty<>();

    @FXML private Label     eventDetailsTitle;
    @FXML private Button    closeEventBtn;
    @FXML private StackPane eventDetailsContent;

    // ---------- LMSR view ----------
    @FXML private VBox     lmsrDetailsPane;
    @FXML private Label    lmsrEventName;
    @FXML private Label    lmsrEventStatus;
    @FXML private Label    lmsrAccountBalance;
    @FXML private Label    lmsrTotalCommissionPaid;
    @FXML private TextArea lmsrEventDescription;
    @FXML private GridPane lmsrOptionsGrid;

    @FXML private VBox lmsrOption1Box;
    @FXML private Label lmsrOption1Label;
    @FXML private TableView<OptionDTO> lmsrOption1Table;
    @FXML private TableColumn<OptionDTO, Double>  lmsrOption1ValueCol;
    @FXML private TableColumn<OptionDTO, Integer> lmsrOption1TotalSharesCol;
    @FXML private TextField lmsrOption1BetField;
    @FXML private Button    lmsrOption1BetBtn;

    @FXML private VBox lmsrOption2Box;
    @FXML private Label lmsrOption2Label;
    @FXML private TableView<OptionDTO> lmsrOption2Table;
    @FXML private TableColumn<OptionDTO, Double>  lmsrOption2ValueCol;
    @FXML private TableColumn<OptionDTO, Integer> lmsrOption2TotalSharesCol;
    @FXML private TextField lmsrOption2BetField;
    @FXML private Button    lmsrOption2BetBtn;

    @FXML private TableView<TradeDTO> lmsrParticipationTable;
    @FXML private TableColumn<TradeDTO, String>  lmsrParticipationUserCol;
    @FXML private TableColumn<TradeDTO, String>  lmsrParticipationOptionCol;
    @FXML private TableColumn<TradeDTO, Integer> lmsrParticipationSharesCol;
    @FXML private TableColumn<TradeDTO, String>  lmsrParticipationPaidCol;

    // ---------- order-book view (kept in fx:define until needed) ----------
    @FXML private GridPane orderBookDetailsPane;
    @FXML private Label eventBalance;
    @FXML private Label comissionPaid;
    @FXML private TableView<?> participationTable;
    @FXML private TableColumn<?, ?> participationUserCol;
    @FXML private TableColumn<?, ?> participationOptionCol;
    @FXML private TableColumn<?, ?> participationSharesCol;
    @FXML private TableColumn<?, ?> participationPaidCol;
    @FXML private TableColumn<?, ?> participationCommissionCol;

    @FXML private VBox  option1VBox;
    @FXML private Label option1Label;
    @FXML private Label option1Value;
    @FXML private Label option1Shares;
    @FXML private TableView<TradeDTO> option1Table;
    @FXML private TableColumn<?, ?> option1UserCol;
    @FXML private TableColumn<TradeDTO, Integer> option1SharesCol;
    @FXML private TableColumn<TradeDTO, Double>  option1PaidCol;

    @FXML private VBox  option2VBox;
    @FXML private Label option2Label;
    @FXML private Label option2Value;
    @FXML private Label option2Shares;
    @FXML private TableView<?> option2Table;
    @FXML private TableColumn<?, ?> option2UserCol;
    @FXML private TableColumn<?, ?> option2SharesCol;
    @FXML private TableColumn<?, ?> option2PaidCol;

    @FXML
    public void initialize() {
        initLmsrColumns();
        restrictToPositiveInteger(lmsrOption1BetField);
        restrictToPositiveInteger(lmsrOption2BetField);

        // read-only tables
        lmsrParticipationTable.setSelectionModel(null);
        lmsrOption1Table.setSelectionModel(null);
        lmsrOption2Table.setSelectionModel(null);
        participationTable.setSelectionModel(null);

        closeEventBtn.setDisable(true);
        showPlaceholders();
    }

    public void init(MainController main) {
        this.main = main;
        bindBetButtons();
    }

    // ---------------- called by MainController ----------------

    /** A row was selected in the left table (or the selection was cleared). */
    public void showEvent(EventSummaryDTO selected) {
        currentEvent.set(selected);
        updateCloseEventButton();

        if (selected == null) {
            clear();
            return;
        }

        lmsrOption1BetField.clear();
        lmsrOption2BetField.clear();

        EventTradingStatusDTO status = main.getEngine().getEventTradingStatus(selected.getId());

        if (selected.getMethod() instanceof LMSRDTO) {
            showLmsrView(status);
        } else {
            showOrderBookView(status);
        }
    }

    /**
     * No event selected. The pane still shows its full structure with placeholder
     * values rather than going blank, so the layout never jumps when a row is
     * picked and the user can see what the panel will contain.
     */
    public void clear() {
        currentEvent.set(null);
        updateCloseEventButton();
        showPlaceholders();
    }

    private void showPlaceholders() {
        Platform.runLater(() -> {
            eventDetailsContent.getChildren().setAll(lmsrDetailsPane);

            eventDetailsTitle.setText("Event details and trade");
            lmsrEventName.setText(EMPTY);
            lmsrEventStatus.setText(EMPTY);
            lmsrAccountBalance.setText("0.00");
            lmsrTotalCommissionPaid.setText("0.00");
            lmsrEventDescription.setText("");

            clearOptionBox(lmsrOption1Box, "Option 1");
            clearOptionBox(lmsrOption2Box, "Option 2");

            lmsrOption1BetField.clear();
            lmsrOption2BetField.clear();

            lmsrParticipationTable.setItems(FXCollections.observableArrayList());
            lmsrParticipationTable.setPlaceholder(new Label("Select an event to see its participations"));
        });
    }

    private void clearOptionBox(VBox optionBox, String captionWhenEmpty) {
        Label title = (Label) optionBox.getChildren().getFirst();
        title.setText(captionWhenEmpty);

        @SuppressWarnings("unchecked")
        TableView<OptionDTO> table = (TableView<OptionDTO>) optionBox.getChildren().get(1);
        table.setItems(FXCollections.observableArrayList());
        table.setPlaceholder(new Label(EMPTY));
    }

    // ---------------- close event ----------------

    @FXML
    void closeEvent(ActionEvent event) {
        if (currentEvent.get() == null) {
            return;
        }

        int eventId = currentEvent.get().getId();

        List<String> optionNames = currentEvent.get().getOptions().stream()
                .map(OptionDTO::optionName)
                .toList();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(optionNames.getFirst(), optionNames);
        dialog.setTitle("Close Event");
        dialog.setHeaderText("Closing \"" + currentEvent.get().getName() + "\"");
        dialog.setContentText("Winning option:");

        Optional<String> chosen = dialog.showAndWait();
        if (chosen.isEmpty()) {
            return;
        }

        int winningOption = optionNames.indexOf(chosen.get()) + 1;

        try {
            main.getEngine().closeEvent(eventId, winningOption);
            // Main rebuilds the left table and re-selects, which calls showEvent()
            // again with a DTO that actually has isOpen == false.
            main.onEventChanged(eventId);
        } catch (IllegalArgumentException ex) {
            DialogHelper.showErrorAlert("Could not close the event", ex.getMessage());
        }
    }

    private void updateCloseEventButton() {
        Platform.runLater(() -> {
            EventSummaryDTO event = currentEvent.get();
            closeEventBtn.setDisable(event == null || !event.isOpen());
            closeEventBtn.setText(
                    event != null && !event.isOpen() ? "Event Closed" : "Close Event");
        });
    }

    // ---------------- bets ----------------

    @FXML void placeBetOption1(ActionEvent event) { placeBet(1, lmsrOption1BetField); }
    @FXML void placeBetOption2(ActionEvent event) { placeBet(2, lmsrOption2BetField); }

    private void placeBet(int optionNumber, TextField field) {
        if (currentEvent.get() == null) {
            return;
        }

        int shares;
        try {
            shares = Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException ex) {
            DialogHelper.showErrorAlert("Could not place the bet", "Enter the number of shares to buy.");
            return;
        }

        int eventId = currentEvent.get().getId();

        try {
            PurchaseDTO purchase = main.getEngine().participateInEvent(eventId, optionNumber, shares);

            // the engine returns null when the event is already closed
            if (purchase == null) {
                DialogHelper.showErrorAlert("Could not place the bet",
                        "This event is closed - no further bets can be placed.");
                return;
            }

            field.clear();
            main.onEventChanged(eventId);
        } catch (IllegalArgumentException ex) {
            DialogHelper.showErrorAlert("Could not place the bet", ex.getMessage());
        }
    }

    private void bindBetButtons() {
        // depends on currentEvent, so it recomputes on every selection change
        BooleanBinding noOpenEvent = Bindings.createBooleanBinding(
                () -> currentEvent.get() == null || !currentEvent.get().isOpen(),
                currentEvent);

        // no file loaded, nothing selected, or the event is closed -> no betting
        BooleanBinding bettingUnavailable = main.fileLoadedProperty().not().or(noOpenEvent);

        // the fields go dead on the same condition as the buttons
        lmsrOption1BetField.disableProperty().bind(bettingUnavailable);
        lmsrOption2BetField.disableProperty().bind(bettingUnavailable);

        // the buttons need the extra condition of actually having a value typed
        lmsrOption1BetBtn.disableProperty()
                .bind(bettingUnavailable.or(lmsrOption1BetField.textProperty().isEmpty()));
        lmsrOption2BetBtn.disableProperty()
                .bind(bettingUnavailable.or(lmsrOption2BetField.textProperty().isEmpty()));
    }

    /**
     * Rejects any keystroke that would leave the field holding something other
     * than a positive integer. The TextFormatter filters the change BEFORE it is
     * applied, so no invalid text ever reaches the field.
     */
    private void restrictToPositiveInteger(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) {
                return change;
            }
            if (!next.matches("\\d+")) {
                return null;
            }
            if (next.length() > 9) {
                return null;                 // would overflow int
            }
            if (Integer.parseInt(next) < 1) {
                return null;
            }
            return change;
        }));
    }

    // ---------------- views ----------------

    private void showLmsrView(EventTradingStatusDTO event) {
        Platform.runLater(() -> {
            eventDetailsContent.getChildren().setAll(lmsrDetailsPane);

            lmsrEventName.setText(event.eventName());
            lmsrEventStatus.setText(event.isOpen() ? "Open" : "Closed");
            lmsrAccountBalance.setText(String.format("%.2f", event.accountBalance()));
            lmsrTotalCommissionPaid.setText(String.format("%.2f", event.totalCommissionPaid()));
            EventSummaryDTO selected = currentEvent.get();
            lmsrEventDescription.setText(selected == null ? "" : selected.getDescription());

            List<OptionDTO> options = event.optionTradingStatus();
            displayLmsrOptionDetails(options.getFirst(), lmsrOption1Box);
            displayLmsrOptionDetails(options.get(1), lmsrOption2Box);

            lmsrParticipationTable.setItems(
                    FXCollections.observableArrayList(event.tradingHistory()));
        });
    }

    private void displayLmsrOptionDetails(OptionDTO option, VBox optionBox) {
        Label title = (Label) optionBox.getChildren().getFirst();
        title.setText(String.valueOf(option.optionName()));

        @SuppressWarnings("unchecked")
        TableView<OptionDTO> table = (TableView<OptionDTO>) optionBox.getChildren().get(1);

        @SuppressWarnings("unchecked")
        TableColumn<OptionDTO, String> valueCol =
                (TableColumn<OptionDTO, String>) table.getColumns().get(0);
        @SuppressWarnings("unchecked")
        TableColumn<OptionDTO, String> sharesCol =
                (TableColumn<OptionDTO, String>) table.getColumns().get(1);

        valueCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%.2f", c.getValue().currentValue())));
        sharesCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().totalSharesBought())));

        ObservableList<OptionDTO> items = FXCollections.observableArrayList(option);
        table.setItems(items);
    }

    private void showOrderBookView(EventTradingStatusDTO event) {
        Platform.runLater(() -> {
            eventDetailsContent.getChildren().setAll(orderBookDetailsPane);
            displayOrderBookEventDetails(event);
        });
    }

    private void displayOrderBookEventDetails(EventTradingStatusDTO singleEvent) {
        OptionDTO first = singleEvent.optionTradingStatus().getFirst();
        OptionDTO second = singleEvent.optionTradingStatus().get(1);

        displayOrderBookOptionDetails(first, option1VBox, option1Label,
                singleEvent.tradingHistory().stream()
                        .filter(t -> Objects.equals(t.optionName(), first.optionName()))
                        .toList());

        displayOrderBookOptionDetails(second, option2VBox, option2Label,
                singleEvent.tradingHistory().stream()
                        .filter(t -> Objects.equals(t.optionName(), second.optionName()))
                        .toList());
    }

    private void displayOrderBookOptionDetails(OptionDTO option, VBox optionBox,
                                               Label optionLabel, List<TradeDTO> trades) {
        optionLabel.setText(option.getOptionName());

        HBox hBox = (HBox) optionBox.getChildren().getFirst();
        ((Label) hBox.getChildren().get(1)).setText(String.valueOf(option.getCurrentValue()));
        ((Label) hBox.getChildren().get(3)).setText(String.valueOf(option.getTotalSharesBought()));

        option1PaidCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().pricePaid()));
        option1SharesCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().sharesBought()));
        // TODO enable when users exist:
        // option1UserCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUserName()));
    }

    private void initLmsrColumns() {
        lmsrOption1ValueCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().currentValue()));
        lmsrOption1TotalSharesCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().totalSharesBought()));

        lmsrOption2ValueCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().currentValue()));
        lmsrOption2TotalSharesCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().totalSharesBought()));

        lmsrParticipationOptionCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().optionName()));
        lmsrParticipationSharesCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().sharesBought()));
        lmsrParticipationPaidCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%.2f", c.getValue().pricePaid())));
    }
}