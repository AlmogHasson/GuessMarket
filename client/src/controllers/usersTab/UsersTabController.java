package controllers.usersTab;

import controllers.MainController;
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

        main.activeUserProperty().addListener((obs, oldUser, newUser) -> {
            usersRightPaneController.showUserDetails(newUser);
        });
    }

    public void onFileLoaded() {
        usersLeftPaneController.loadUsers();
        usersRightPaneController.showUserDetails(main.getActiveUser());
    }


    public void refresh() {
        usersRightPaneController.refresh();
        usersLeftPaneController.refresh();
    }
}
