package fx;

import dto.*;
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



/** Controller for eventRight.fxml - event details, the two option panels, bets, close. */
public class EventRightController {

    private static final String EMPTY = "\u2014";   // em dash

    private MainController main;
    private EventTabController tab;

    /**
     * The event currently on screen, as a property so the bet buttons can bind to
     * it. Treat the DTO as a snapshot: read its id, never trust its state after
     * the engine has been mutated.
     */
    private final ObjectProperty<EventSummaryDTO> currentEvent = new SimpleObjectProperty<>();

    @FXML private Label     eventDetailsTitle;
    @FXML private Button setEventStatusBtn;
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
    @FXML private TableView<LMSROptionDTO> lmsrOption1Table;
    @FXML private TableColumn<LMSROptionDTO, Double>  lmsrOption1ValueCol;
    @FXML private TableColumn<LMSROptionDTO, Integer> lmsrOption1TotalSharesCol;
    @FXML private TextField lmsrOption1BetField;
    @FXML private Button    lmsrOption1BetBtn;

    @FXML private VBox lmsrOption2Box;
    @FXML private Label lmsrOption2Label;
    @FXML private TableView<LMSROptionDTO> lmsrOption2Table;
    @FXML private TableColumn<LMSROptionDTO, Double>  lmsrOption2ValueCol;
    @FXML private TableColumn<LMSROptionDTO, Integer> lmsrOption2TotalSharesCol;
    @FXML private TextField lmsrOption2BetField;
    @FXML private Button    lmsrOption2BetBtn;

    @FXML private TableView<TradeDTO> lmsrParticipationTable;
    @FXML private TableColumn<TradeDTO, String>  lmsrParticipationUserCol;
    @FXML private TableColumn<TradeDTO, String>  lmsrParticipationOptionCol;
    @FXML private TableColumn<TradeDTO, Integer> lmsrParticipationSharesCol;
    @FXML private TableColumn<TradeDTO, String>  lmsrParticipationPaidCol;

    // ---------- order-book view (kept in fx:define until needed) ----------
    //TODO: implement order-book view, with bid/ask tables and participation table
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
    @FXML private TableColumn<TradeDTO, String>  option1UserCol;
    @FXML private TableColumn<TradeDTO, String>  option1SideCol;
    @FXML private TableColumn<TradeDTO, Integer> option1SharesCol;
    @FXML private TableColumn<TradeDTO, Double>  option1PriceCol;

    @FXML private VBox  option2VBox;
    @FXML private Label option2Label;
    @FXML private Label option2Value;
    @FXML private Label option2Shares;

    @FXML private TableView<TradeDTO> option2Table;
    @FXML private TableColumn<TradeDTO, String>  option2UserCol;
    @FXML private TableColumn<TradeDTO, String>  option2SideCol;
    @FXML private TableColumn<TradeDTO, Integer> option2SharesCol;
    @FXML private TableColumn<TradeDTO, Double>  option2PriceCol;

    // order-book participation controls (bid / ask)
    @FXML private ComboBox<String> obOption1SideBox;
    @FXML private TextField obOption1SharesField;
    @FXML private TextField obOption1PriceField;
    @FXML private Button    obOption1OrderBtn;

    @FXML private ComboBox<String> obOption2SideBox;
    @FXML private TextField obOption2SharesField;
    @FXML private TextField obOption2PriceField;
    @FXML private Button    obOption2OrderBtn;

    private static final String BID = "Bid";
    private static final String ASK = "Ask";

    @FXML
    public void initialize() {
        initLmsrColumns();
        initOrderBookColumns();
        restrictToPositiveInteger(lmsrOption1BetField);
        restrictToPositiveInteger(lmsrOption2BetField);

        setupOrderControls(obOption1SideBox, obOption1SharesField, obOption1PriceField);
        setupOrderControls(obOption2SideBox, obOption2SharesField, obOption2PriceField);

        // read-only tables
        lmsrParticipationTable.setSelectionModel(null);
        lmsrOption1Table.setSelectionModel(null);
        lmsrOption2Table.setSelectionModel(null);
        participationTable.setSelectionModel(null);
        //TODO add the order book tables

        setEventStatusBtn.setDisable(true);
        showPlaceholders();
    }

    public void init(MainController main, EventTabController tab) {
        this.main = main;
        this.tab = tab;
        bindBetButtons();
    }

    private void initOrderBookColumns() {
        initOrderBookOptionColumns(option1UserCol, option1SideCol, option1SharesCol, option1PriceCol);
        initOrderBookOptionColumns(option2UserCol, option2SideCol, option2SharesCol, option2PriceCol);
        option1Table.setSelectionModel(null);
        option2Table.setSelectionModel(null);
    }

    private void initOrderBookOptionColumns(TableColumn<TradeDTO,String> userCol,
                                            TableColumn<TradeDTO,String> sideCol,
                                            TableColumn<TradeDTO,Integer> sharesCol,
                                            TableColumn<TradeDTO,Double> priceCol) {
        userCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().userName()));

        sideCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().side() == Side.BUY ? "Buy" : "Sell"));

        sharesCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().sharesBought()));

        priceCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().sharesBought() == 0
                        ? 0.0
                        : c.getValue().pricePaid() / c.getValue().sharesBought()));
    }
    // ---------------- called by MainController ----------------

    /** A row was selected in the left table (or the selection was cleared). */
    public void showEvent(EventSummaryDTO selected) {
        currentEvent.set(selected);
        updateEventManagementButton();

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
        updateEventManagementButton();
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
        TableView<LMSROptionDTO> table = (TableView<LMSROptionDTO>) optionBox.getChildren().get(1);
        table.setItems(FXCollections.observableArrayList());
        table.setPlaceholder(new Label(EMPTY));
    }

    // ---------------- close event ----------------

    @FXML
    void manageEvent(ActionEvent event) {
        if (currentEvent.get() == null) {
            return;
        }
        switch (currentEvent.get().status()) {
            case NOT_STARTED -> activateEvent();
            case OPEN       -> closeEventDialog();
            case CLOSED       -> { /* button is disabled in this state, nothing to do */ }
        }
    }

    private void closeEventDialog() {
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
            tab.onEventChanged(eventId);
        } catch (IllegalArgumentException ex) {
            DialogHelper.showErrorAlert("Could not close the event: ", ex.getMessage());
        }
    }

    private void activateEvent() {
        int eventId = currentEvent.get().getId();
        try {
            main.getEngine().activateEvent(main.getActiveUser().getName(), eventId);
            tab.onEventChanged(eventId); // re-fetches the DTO, status flips to ACTIVE, button relabels
        } catch (IllegalArgumentException ex) {
            DialogHelper.showErrorAlert("Could not activate the event: ", ex.getMessage());
        }
    }

    private void updateEventManagementButton() {
        Platform.runLater(() -> {
            EventSummaryDTO event = currentEvent.get();
            if (event == null) {
                setEventStatusBtn.setDisable(true);
                setEventStatusBtn.setText("Close Event");
                setButtonTone("danger-button");
                return;
            }
            boolean isMaker = main.getActiveUser() != null
                    && main.getActiveUser().isEventMaker(event.getId());

            switch (event.getStatus()) {
                case NOT_STARTED -> {
                    if (isMaker) {
                        setEventStatusBtn.setText("Activate Event");
                    } else {
                        setEventStatusBtn.setText("Not Started");
                    }
                    setEventStatusBtn.setDisable(!isMaker);
                    setButtonTone("success-button");
                }
                case OPEN -> {
                    if (isMaker) {
                        setEventStatusBtn.setText("Close Event");
                    } else {
                        setEventStatusBtn.setText("Event Open");
                    }
                    setEventStatusBtn.setDisable(!isMaker);
                    setButtonTone("danger-button");
                }
                case CLOSED -> {
                    setEventStatusBtn.setText("Event Closed");
                    setEventStatusBtn.setDisable(true);
                    setButtonTone("danger-button");
                }
            }
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
            DialogHelper.showErrorAlert("Could not place the bet", "Enter the number of totalShares to buy.");
            return;
        }

        int eventId = currentEvent.get().getId();

        try {
            PurchaseDTO purchase = main.getEngine().participateInEvent(main.getActiveUser().getName(),eventId, optionNumber, shares);

            // the engine returns null when the event is already closed
            if (purchase == null) {
                DialogHelper.showErrorAlert("Could not place the bet",
                        "This event is closed - no further bets can be placed.");
                return;
            }

            field.clear();
            tab.onEventChanged(eventId);

        } catch (IllegalArgumentException ex) {
            DialogHelper.showErrorAlert("Could not place the bet", ex.getMessage());
        }
    }

    private void bindBetButtons() {
        // depends on currentEvent, so it recomputes on every selection change
        BooleanBinding noOpenEvent = Bindings.createBooleanBinding(
                () -> currentEvent.get() == null || !currentEvent.get().isOpen(),
                currentEvent);

        BooleanBinding noActiveUser = Bindings.createBooleanBinding(
                () -> main.getActiveUser() == null,
                main.activeUserProperty());

        // no file loaded, nothing selected, or the event is closed -> no betting
        BooleanBinding bettingUnavailable = main.fileLoadedProperty().not().or(noOpenEvent).or(noActiveUser);

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
            lmsrEventStatus.setText(getStatusText(event));
            lmsrAccountBalance.setText(String.format("%.2f", event.accountBalance()));
            lmsrTotalCommissionPaid.setText(String.format("%.2f", event.totalCommissionPaid()));
            EventSummaryDTO selected = currentEvent.get();
            lmsrEventDescription.setText(selected == null ? "" : selected.getDescription());

            List<LMSROptionDTO> options = event.optionTradingStatus();
            displayLmsrOptionDetails(options.getFirst(), lmsrOption1Box);
            displayLmsrOptionDetails(options.get(1), lmsrOption2Box);

            lmsrParticipationTable.setItems(
                    FXCollections.observableArrayList(event.tradingHistory()));
        });
    }

    private String getStatusText(EventTradingStatusDTO event) {
        return switch (event.status()) {
            case NOT_STARTED -> "Not started";
            case OPEN -> "Open";
            case CLOSED -> "Closed";
        };
    }

    private void displayLmsrOptionDetails(LMSROptionDTO option, VBox optionBox) {
        Label title = (Label) optionBox.getChildren().getFirst();
        title.setText(String.valueOf(option.optionName()));

        @SuppressWarnings("unchecked")
        TableView<LMSROptionDTO> table = (TableView<LMSROptionDTO>) optionBox.getChildren().get(1);

        @SuppressWarnings("unchecked")
        TableColumn<LMSROptionDTO, String> valueCol =
                (TableColumn<LMSROptionDTO, String>) table.getColumns().get(0);
        @SuppressWarnings("unchecked")
        TableColumn<LMSROptionDTO, String> sharesCol =
                (TableColumn<LMSROptionDTO, String>) table.getColumns().get(1);

        valueCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%.2f", c.getValue().currentValue())));

        sharesCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().totalSharesBought())));

        ObservableList<LMSROptionDTO> items = FXCollections.observableArrayList(option);
        table.setItems(items);
    }

    private void showOrderBookView(EventTradingStatusDTO event) {
        Platform.runLater(() -> {
            eventDetailsContent.getChildren().setAll(orderBookDetailsPane);
            displayOrderBookEventDetails(event);
        });
    }

    private void displayOrderBookEventDetails(EventTradingStatusDTO singleEvent) {
        LMSROptionDTO first  = singleEvent.optionTradingStatus().getFirst();
        LMSROptionDTO second = singleEvent.optionTradingStatus().get(1);

        displayOrderBookOptionDetails(first, option1VBox, option1Label, option1Table,
                singleEvent.tradingHistory().stream()
                        .filter(t -> Objects.equals(t.optionName(), first.optionName()))
                        .toList());

        displayOrderBookOptionDetails(second, option2VBox, option2Label, option2Table,
                singleEvent.tradingHistory().stream()
                        .filter(t -> Objects.equals(t.optionName(), second.optionName()))
                        .toList());
    }

    private void displayOrderBookOptionDetails(
            LMSROptionDTO option, VBox optionBox, Label optionLabel,
            TableView<TradeDTO> table, List<TradeDTO> trades)
    {
        optionLabel.setText(option.getOptionName());

        HBox hBox = (HBox) optionBox.getChildren().getFirst();
        ((Label) hBox.getChildren().get(1)).setText(String.valueOf(option.getCurrentValue()));
        ((Label) hBox.getChildren().get(3)).setText(String.valueOf(option.getTotalSharesBought()));

        table.setItems(FXCollections.observableArrayList(trades));
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

        lmsrParticipationUserCol.setCellValueFactory(c->
                new ReadOnlyStringWrapper(c.getValue().userName()));

        lmsrParticipationOptionCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().optionName()));

        lmsrParticipationSharesCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().sharesBought()));

        lmsrParticipationPaidCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%.2f", c.getValue().pricePaid())));
    }

    /** Green while the button activates, red while it closes. */
    private void setButtonTone(String toneClass) {
        setEventStatusBtn.getStyleClass().removeAll("danger-button", "success-button");
        setEventStatusBtn.getStyleClass().add(toneClass);
    }

    // ---------------- order book: bid / ask ----------------

    private void setupOrderControls(ComboBox<String> sideBox, TextField sharesField, TextField priceField) {
        sideBox.getItems().setAll(BID, ASK);
        sideBox.setValue(BID);
        restrictToPositiveInteger(sharesField);
        restrictToPrice(priceField);
    }

    @FXML
    void placeOrderOption1(ActionEvent event) {
        placeOrder(1, obOption1SideBox, obOption1SharesField, obOption1PriceField);
    }

    @FXML
    void placeOrderOption2(ActionEvent event) {
        placeOrder(2, obOption2SideBox, obOption2SharesField, obOption2PriceField);
    }

    private void placeOrder(int optionNumber, ComboBox<String> sideBox,
                            TextField sharesField, TextField priceField) {
        EventSummaryDTO event = currentEvent.get();
        if (event == null || main.getActiveUser() == null) {
            DialogHelper.showErrorAlert("Could not place the order",
                    "Select an event and a user first.");
            return;
        }

        int shares;
        double price;
        try {
            shares = Integer.parseInt(sharesField.getText().trim());
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException ex) {
            DialogHelper.showErrorAlert("Could not place the order",
                    "Enter both a quantity and a price per share.");
            return;
        }

        boolean isBid = BID.equals(sideBox.getValue());

        // TODO wire once the engine exposes an order-book API, e.g.
        //   main.getEngine().placeOrder(main.getActiveUser().getName(),
        //           event.getId(), optionNumber, isBid, shares, price);
        //   sharesField.clear();
        //   priceField.clear();
        //   tab.onEventChanged(event.getId());
        DialogHelper.showErrorAlert("Not implemented yet",
                String.format("%s %d shares of option %d at %.2f - "
                                + "the order book engine is not wired up yet.",
                        isBid ? "Buy" : "Sell", shares, optionNumber, price));
    }

    /** Accepts a non-negative decimal with at most two digits after the point. */
    private void restrictToPrice(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) {
                return change;
            }
            return next.matches("\\d{0,6}(\\.\\d{0,2})?") ? change : null;
        }));
    }

    public void refresh() {
        showRightView();
    }

    private void showRightView() {
        if (currentEvent.get() == null) {
            clear();
            return;
        }
        if (currentEvent.get().getMethod() instanceof LMSRDTO) {
            EventTradingStatusDTO status = main.getEngine().getEventTradingStatus(currentEvent.get().getId());
            showLmsrView(status);
        } else {
            EventTradingStatusDTO status = main.getEngine().getEventTradingStatus(currentEvent.get().getId());
            showOrderBookView(status);
        }

        updateEventManagementButton();
    }
}
