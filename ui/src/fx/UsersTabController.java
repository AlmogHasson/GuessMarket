package fx;

import dto.UserDTO;
import javafx.fxml.FXML;

/**
 * Controller for users.fxml - the Users tab.
 *
 * Mirrors EventTabController: it wires its two halves and passes selection
 * between them. The engine has no user API yet, so the halves are scaffolding.
 */
public class UsersTabController {

    private MainController main;

    @FXML private UsersLeftController  usersLeftPaneController;
    @FXML private UsersRightController usersRightPaneController;

    public void init(MainController main) {
        this.main = main;
        usersLeftPaneController.init(main, this);
        usersRightPaneController.init(main, this);
    }

    public void onFileLoaded() {
        usersLeftPaneController.loadUsers();
        usersRightPaneController.clear();
    }

    /** Left changed its selection. user is null when nothing is selected. */
    public void onUserSelected(UserDTO user) {
        usersRightPaneController.showUserDetails(user);
    }

    public void refresh() {
        usersRightPaneController.refresh();
        usersLeftPaneController.refresh();
    }
}
