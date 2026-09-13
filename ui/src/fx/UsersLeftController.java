package fx;

import dto.UserDTO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Map;

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

    @FXML
    public void initialize() {
        userListNameCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getName()));
        userListAccountBalanceCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        String.format("%.2f", c.getValue().getAccountBalance())
                ));
    }

    public void init(MainController main, UsersTabController tab) {
        this.main = main;
        this.tab = tab;

        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) ->
                        {
                            main.setActiveUser(newSelection);
                            tab.onUserSelected(newSelection);
                        });
    }

    /** Called when a file is loaded. */
    public void loadUsers() {

        main.rows().update(usersTable, "users", main.getEngine().getUsers().values(), UserDTO::name);
    }

    public void refresh() {
        reloadUsers();
    }

    public void reloadUsers() {
        UserDTO previous = usersTable.getSelectionModel().getSelectedItem();
        Map<String, UserDTO> users = main.getEngine().getUsers();

        if (users.isEmpty()) {
            usersTable.getItems().clear();
            return;
        }

        
        main.rows().update(usersTable, "users", users.values(), UserDTO::name);

        if (previous != null) {
            usersTable.getItems().stream()
                    .filter(u -> u.getName().equals(previous.getName()))
                    .findFirst()
                    .ifPresent(u -> usersTable.getSelectionModel().select(u));
        }
    }
}
