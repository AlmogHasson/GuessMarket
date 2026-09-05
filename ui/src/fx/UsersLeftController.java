package fx;

import dto.UserDTO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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
    @FXML private TableColumn<UserDTO, Double> userListAccountBalanceCol;

    @FXML
    public void initialize() {
        userListNameCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getName()));
        userListAccountBalanceCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getAccountBalance()));
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
        usersTable.getItems().setAll(main.getEngine().getUsers().values());
    }
}
