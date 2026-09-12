package fx;

//TODO: display order book events selected differently

import dto.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for usersRight.fxml - one user's balance, their events and the
 * trades inside a chosen event.
 *
 * Scaffolding for the same reason as UsersLeftController.
 */
public class UsersRightController {

    private static final String EMPTY = "\u2014";

    private MainController main;
    private UsersTabController tab;

    @FXML private Label balance;
    @FXML private TableView<UserEventDTO> userEventsTable;
    @FXML private TableColumn<UserEventDTO, String> userEventCol;
    @FXML private TableColumn<UserEventDTO, String> userEventRoleCol;
    @FXML private TableColumn<UserEventDTO, String> userEventStatusCol;
    @FXML private TableColumn<UserEventDTO, String> userEventSharesCol;

    //--------------LMSR VIEW-------------------
    @FXML private TableView<TradeDTO> lmsrEventTable;
    @FXML private TableColumn<TradeDTO, String> lmsrEventOptionCol;
    @FXML private TableColumn<TradeDTO, String> lmsrSharesCol;
    @FXML private TableColumn<TradeDTO, String> lmsrPaidCol;
    @FXML private TableColumn<TradeDTO, String> lmsrCommissionCol;

    //-----------ORDER BOOK VIEW----------------
    @FXML private TableView<HoldingDTO> orderBookPositionTable;
    @FXML private TableColumn<HoldingDTO, String> obOptionCol;
    @FXML private TableColumn<HoldingDTO, String> obSharesHeldCol;
    @FXML private TableColumn<HoldingDTO, String> obPaidCol;
    @FXML private TableColumn<HoldingDTO, String> obCommissionCol;

    //-----------SHARED SUMMARY----------------
    @FXML private VBox participationSummary;
    @FXML private Label commissionSummaryLabel;
    @FXML private Label closedSummaryLabel;

    @FXML
    public void initialize() {
        lmsrEventTable.setPlaceholder(new Label("Select an event"));
        orderBookPositionTable.setPlaceholder(new Label("Select an event"));
        userEventsTable.setPlaceholder(new Label("Select a user"));
        lmsrEventTable.setItems(FXCollections.observableArrayList());
        userEventsTable.setItems(FXCollections.observableArrayList());
        orderBookPositionTable.setItems(FXCollections.observableArrayList());
    }

    public void init(MainController main, UsersTabController tab) {
        this.main = main;
        this.tab = tab;

        // Participation rows are display-only
        lmsrEventTable.setSelectionModel(null);
        orderBookPositionTable.setSelectionModel(null);

        initUserEventsTableColumns();
        initLmsrParticipationTableColumns();
        initOrderBookParticipationTableColumns();
    }

    private void initOrderBookParticipationTableColumns() {
        obOptionCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().optionName()));

        obSharesHeldCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().shares())));

        obPaidCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.format("%.2f", cellData.getValue().totalPaid())));

        obCommissionCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.format("%.2f", cellData.getValue().commissionPaid())));
    }

    private void initLmsrParticipationTableColumns() {
        lmsrEventOptionCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().optionName()));

        lmsrSharesCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().sharesBought())));

        lmsrPaidCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.format("%.2f", cellData.getValue().pricePaid())));

        lmsrCommissionCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.format("%.2f", cellData.getValue().commissionPaid())));
    }

    private void initUserEventsTableColumns() {
        userEventsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldRow, newRow)
                        -> showUserParticipation(newRow));

        userEventCol.setCellValueFactory(cellData ->
                        new ReadOnlyStringWrapper(cellData.getValue().eventName()));

        userEventRoleCol.setCellValueFactory(cellData ->
                        new ReadOnlyStringWrapper(
                                //if the user eventIds contains the eventId,
                                // then the user is a market maker, otherwise they are a player
                                cellData.getValue().role())
                        );

        userEventStatusCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(formatStatus(cellData.getValue().eventStatus())));

        userEventSharesCol.setCellValueFactory(cellData ->
                        new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().totalShares())));
    }

    /** No user selected - show the structure with placeholders, not a blank pane. */
    public void clear() {
        balance.setText("0.00");
        lmsrEventTable.getItems().clear();
        userEventsTable.getItems().clear();
        userEventsTable.setPlaceholder(new Label("Select a user"));
        lmsrEventTable.setPlaceholder(new Label("Select an event"));
        orderBookPositionTable.getItems().clear();
        orderBookPositionTable.setPlaceholder(new Label("Select an event"));
        setSummary(null, null);
    }

    public void showUserDetails(UserDTO user) {
        if (user == null) {
            clear();
        }
        else {
            Platform.runLater(() -> {
                balance.setText(String.format("%.2f", (double) user.getAccountBalance()));
                var userEvents = FXCollections.observableArrayList(
                        main.getEngine().getUserEvents(user.name())
                );
                UserEventDTO previous = userEventsTable.getSelectionModel().getSelectedItem();
                if (userEvents.isEmpty()) {
                    userEventsTable.getItems().clear();
                    userEventsTable.setPlaceholder(new Label("No events"));
                }
                userEventsTable.setItems(userEvents);

                if (previous != null) {
                    userEvents.stream()
                            .filter(e -> e.eventName().equals(previous.eventName()))
                            .findFirst()
                            .ifPresent(e ->
                                    {
                                        userEventsTable.getSelectionModel().select(e);
                                        //refresh the table to show the new data for this event
                                        showUserParticipation(e);
                                    }
                            );

                }
            });

        }
    }

    private void showUserParticipation(UserEventDTO row) {
        boolean hasActiveUser = main.getActiveUser() != null;
        boolean eventNotStarted = row != null && row.eventStatus() == EventStatus.NOT_STARTED;

        if (row == null || !hasActiveUser || eventNotStarted) {
            lmsrEventTable.getItems().clear();
            orderBookPositionTable.getItems().clear();
            lmsrEventTable.setPlaceholder(new Label(eventNotStarted? "Event not started yet" :"Select an event"));
            orderBookPositionTable.setPlaceholder(new Label(eventNotStarted? "Event not started yet" :"Select an event"));
            setSummary(null, null);
            return;
        }

        String userName = main.getActiveUser().getName();

        if (row.isOrderBook()) {
            showOrderBookPosition(row, userName);
        } else {
            showLmsrHistory(row, userName);
        }
    }

    private void showLmsrHistory(UserEventDTO row, String userName) {
        showTable(true);

        EventTradingStatusDTO status = main.getEngine().getEventTradingStatus(row.eventId());

        var userTrades = status.tradingHistory().stream()
                .filter(t -> t.userName().equals(userName))
                .toList();

        lmsrEventTable.getItems().setAll(userTrades);

        setOrderBookParticipationTablePlaceHolder(userTrades, status);

        String closedText = null;

        if (status.status() == EventStatus.CLOSED) {
            String perOption = status.optionTradingStatus().stream()
                    .map(o -> o.optionName() + ": " + o.totalSharesBought())
                    .collect(Collectors.joining(", "));

            String winner = status.optionTradingStatus()
                    .stream()
                    .filter(o -> o instanceof LMSROptionDTO l && l.isWinner())
                    .map(OptionDTO::optionName)
                    .findFirst().orElse(EMPTY);

            closedText = "Total shares per option: " + perOption + "  —  Winning Option: " + winner;
        }
        setSummary(null, closedText); // LMSR shows commission per-row, no need to repeat it
    }

    private void setOrderBookParticipationTablePlaceHolder(List<TradeDTO> userTrades, EventTradingStatusDTO status) {
        if (userTrades.isEmpty()) {
            switch (status.status()) {
                case NOT_STARTED ->
                        lmsrEventTable.setPlaceholder(
                                new Label("Event not started yet")
                        );

                case OPEN ->
                        lmsrEventTable.setPlaceholder(
                                new Label("No participation yet")
                        );

                case CLOSED ->
                        lmsrEventTable.setPlaceholder(
                                new Label("No participation in this event")
                        );
            }
        }
    }

    private void showOrderBookPosition(UserEventDTO row, String userName) {
        showTable(false);

        UserOrderBookPositionDTO position = main.getEngine().getUserOrderBookPosition(userName, row.eventId());
        orderBookPositionTable.getItems().setAll(position.holdings());

        String commissionText = String.format("Total commission Paid: %.2f", position.commissionPaid());

        String closedText = null;

        if (position.eventClosed()) {
            String winner = main.getEngine().getEventTradingStatus(row.eventId()).optionTradingStatus()
                    .stream()
                    .filter(OBOptionDTO.class::isInstance)
                    .map(OBOptionDTO.class::cast)
                    .filter(OBOptionDTO::isWinner)
                    .map(OptionDTO::optionName)
                    .findFirst()
                    .orElse(EMPTY);

            closedText = String.format(
                    "Winning Option: %s   —   Profit / Loss: %.2f",
                    winner,
                    position.profitLoss()
            );
        }
        setSummary(commissionText, closedText);
    }

    /** Switches which of the two tables is visible/managed. Only one is ever shown. */
    private void showTable(boolean showLmsr) {
        lmsrEventTable.setVisible(showLmsr);
        lmsrEventTable.setManaged(showLmsr);
        orderBookPositionTable.setVisible(!showLmsr);
        orderBookPositionTable.setManaged(!showLmsr);
    }

    private void setSummary(String commissionText, String closedText) {
        boolean hasCommission = commissionText != null;
        boolean hasClosedText = closedText != null;
        boolean hasAnything = hasCommission || hasClosedText;

        participationSummary.setVisible(hasAnything);
        participationSummary.setManaged(hasAnything);

        commissionSummaryLabel.setText(
                hasCommission ? commissionText : ""
        );
        commissionSummaryLabel.setVisible(hasCommission);
        commissionSummaryLabel.setManaged(hasCommission);

        closedSummaryLabel.setText(
                hasClosedText ? closedText : ""
        );
        closedSummaryLabel.setVisible(hasClosedText);
        closedSummaryLabel.setManaged(hasClosedText);
    }

    private String formatStatus(EventStatus status) {
        return switch (status) {
            case NOT_STARTED -> "Not Started";
            case OPEN -> "Open";
            case CLOSED -> "Closed";
        };
    }

    public void refresh() {
        UserDTO current = main.getActiveUser();
        if (current == null)
            return;

        UserDTO fresh = main.getEngine().getUsers().get(current.name());
        if (fresh == null)
            return;

        main.setActiveUser(fresh);
        showUserDetails(fresh);
    }
}
