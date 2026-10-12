package controllers.usersTab;

import controllers.MainController;
import dto.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import controllers.util.DialogHelper;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import util.HttpClientUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for usersLeft.fxml - the list of users.
 *
 * The engine exposes no user API yet (EngineImpl.users is declared but never
 * populated, and GMUsers is not read in loadFile), so this is deliberately
 * scaffolding: the wiring is in place and only the data source is missing.
 */
public class UsersLeftController {

    private MainController main;
    private UsersTabController tab;

    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, String> userListNameCol;
    @FXML private TableColumn<UserDTO, String> userListAccountBalanceCol;
    public TableColumn<UserDTO, String> userListMarketMakerCol;

    // ---------------- account details (active user) ----------------
    @FXML private TableView<AccountTransactionDTO> accountDetailsTable;
    @FXML private TableColumn<AccountTransactionDTO, String> accountEventCol;
    @FXML private TableColumn<AccountTransactionDTO, String> accountActivityCol;
    @FXML private TableColumn<AccountTransactionDTO, String> accountAmountCol;
    @FXML private TableColumn<AccountTransactionDTO, String> accountBalanceCol;
    @FXML private Label accountBalanceLabel;
    @FXML private Button depositBtn;

    @FXML
    public void initialize() {
        accountEventCol.setCellValueFactory(cellData -> {
            String eventName = cellData.getValue().eventName();
            return new ReadOnlyStringWrapper(eventName == null || eventName.isBlank() ? "-" : eventName);});

        accountActivityCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().activity()));

        accountAmountCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.format("%+.2f", cellData.getValue().amount())));

        accountBalanceCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.format("%.2f", cellData.getValue().balanceAfter())));

        accountDetailsTable.setSelectionModel(null);   // display-only
        accountDetailsTable.setItems(FXCollections.observableArrayList());
        accountDetailsTable.setPlaceholder(new Label("No activity yet"));

        userListNameCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getName()));
        userListAccountBalanceCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        String.format("%.2f", c.getValue().getAccountBalance())
                ));
        userListMarketMakerCol.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        cellData.getValue().getEventsIDs().isEmpty()
                                ? "No"
                                : "Yes"
                )
        );

        usersTable.setSelectionModel(null);
    }

    public void init(MainController main, UsersTabController tab) {
        this.main = main;
        this.tab = tab;

        main.installKeycap(depositBtn);
        depositBtn.disableProperty().bind(Bindings.createBooleanBinding(
                () -> main.getActiveUser() == null || !main.fileLoadedProperty().get(),
                main.activeUserProperty(), main.fileLoadedProperty()));

        // keep the account card in sync with whoever is the active user
        main.activeUserProperty().addListener((obs, o, n) -> showAccount(n));
        showAccount(main.getActiveUser());

    }

    private void loadAccountTransactions() {
        try {
            List<AccountTransactionDTO> transactions =
                    new ArrayList<>(
                            HttpClientUtil.getAccountTransactions()
                    );

            Collections.reverse(transactions);

            main.rows().update(
                    accountDetailsTable,
                    "account-transactions",
                    transactions,
                    AccountTransactionDTO::id
            );

            if (transactions.isEmpty()) {
                accountDetailsTable.setPlaceholder(
                        new Label("No account transactions")
                );
            }

        } catch (Exception ex) {
            DialogHelper.showErrorAlert(
                    "Could not load account transactions",
                    ex.getMessage()
            );
        }
    }

    public void loadUsers() {
        try {
            List<UserDTO> users = HttpClientUtil.getUsers();

            UserDTO activeUser = main.getActiveUser();

            if (activeUser != null) {
                users.removeIf(user ->
                        user.getName().equalsIgnoreCase(activeUser.getName())
                );
            }

            main.rows().update(
                    usersTable,
                    "users",
                    users,
                    UserDTO::name
            );

        } catch (Exception ex) {
            DialogHelper.showErrorAlert(
                    "Could not load users",
                    ex.getMessage()
            );
        }
    }

    public void refresh() {
        reloadUsers();
        showAccount(main.getActiveUser());
        loadAccountTransactions();
    }

    public void reloadUsers() {
        try {
            List<UserDTO> users = HttpClientUtil.getUsers();

            UserDTO activeUser = main.getActiveUser();

            if (activeUser != null) {
                users.removeIf(user ->
                        user.getName().equalsIgnoreCase(activeUser.getName())
                );
            }

            main.rows().update(
                    usersTable,
                    "users",
                    users,
                    UserDTO::name
            );

        } catch (Exception ex) {
            DialogHelper.showErrorAlert(
                    "Could not refresh users",
                    ex.getMessage()
            );
        }
    }

    // ---------------- account details / deposit ----------------

    private void showAccount(UserDTO user) {
        if (user == null) {
            accountDetailsTable.getItems().clear();
            accountBalanceLabel.setText("0.00");
            return;
        }
        accountBalanceLabel.setText(String.format("%.2f", (double) user.getAccountBalance()));
    }

    /**
     * Account statement: every trade the user made, across all of their events.
     * Amount is negative when money left the account (buy) and positive when it
     * came in (sell); commission is always a cost, so it is shown negative.
     * Rows keep the engine's order per event, newest event activity first.
     */

    @FXML private void deposit() {

        Optional<Double> amount = DialogHelper.showDepositDialog(depositBtn.getScene().getWindow());

        if (amount.isEmpty()) {
            return;
        }

        try {

            UserDTO updatedUser = sendDeposit(amount.get());
            main.setActiveUser(updatedUser);
            loadAccountTransactions();

        } catch (Exception ex) {
            DialogHelper.showErrorAlert("Could not deposit", ex.getMessage());
        }
    }

    private UserDTO sendDeposit(double amount)
            throws Exception {
        return HttpClientUtil.deposit(amount);
    }
}
