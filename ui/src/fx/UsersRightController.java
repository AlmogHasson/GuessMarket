package fx;

//TODO: display order book events selected differently

import dto.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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
    @FXML private TableColumn<UserEventDTO, String> userEventSharesCol;
//    @FXML private TableColumn<UserEventDTO, String> userEventInvestmentCol;

    @FXML private TableView<TradeDTO> singleEventTable;
    @FXML private TableColumn<TradeDTO, String> singleEventOptionCol;
    @FXML private TableColumn<TradeDTO, String> singleEventSharesCol;
    @FXML private TableColumn<TradeDTO, String> singleEventPaidCol;
    @FXML private TableColumn<TradeDTO, String> singleEventCommissionCol;

    @FXML
    public void initialize() {
        singleEventTable.setPlaceholder(new Label(EMPTY));
        userEventsTable.setPlaceholder(new Label(EMPTY));
        singleEventTable.setItems(FXCollections.observableArrayList());
        userEventsTable.setItems(FXCollections.observableArrayList());
    }

    public void init(MainController main, UsersTabController tab) {
        this.main = main;
        this.tab = tab;

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
        userEventSharesCol.setCellValueFactory(cellData ->
                        new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().totalShares())));

        singleEventOptionCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().optionName()));

        singleEventSharesCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().sharesBought())));

        singleEventPaidCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.format("%.2f", cellData.getValue().pricePaid())));

    }

    /** No user selected - show the structure with placeholders, not a blank pane. */
    public void clear() {
        balance.setText("0.00");
        singleEventTable.getItems().clear();
        userEventsTable.getItems().clear();
        userEventsTable.setPlaceholder(new Label("Select a user"));
        singleEventTable.setPlaceholder(new Label(EMPTY));
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
                            .ifPresent(e -> userEventsTable.getSelectionModel().select(e));
                }
            });

        }
    }

    private void showUserParticipation(UserEventDTO row) {
        if (row == null || main.getActiveUser() == null) {
            singleEventTable.getItems().clear();
            singleEventTable.setPlaceholder(new Label("Select an event"));
            return;
        }
        singleEventTable.getItems().setAll(
                main.getEngine().getEventTradingStatus(row.eventId()).tradingHistory()
                        .stream()
                        .filter(t -> t.userName().equals(main.getActiveUser().getName()))
                        .toList());
    }

    public void refresh() {
        UserDTO current = main.getActiveUser();
        if (current == null) return;

        UserDTO fresh = main.getEngine().getUsers().get(current.name()); // adjust key if id-based
        if (fresh == null) return;

        main.setActiveUser(fresh);
        showUserDetails(fresh);
    }
}
