package fx;

import api.GMController;
import dto.UserDTO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

/**
 * Root coordinator for the composition tree.
 *
 * It owns the things every pane must agree on - the engine controller, whether a
 * file has been loaded, who the active user is, and whether animations are
 * enabled - and forwards events down to the two tab controllers. Anything that
 * stays inside a single tab (a row selection, a bet, closing an event) is handled
 * by that tab's own controller and never reaches this class.
 */
public class MainController {

    /** The single engine instance for the whole application. */
    private final GMController controller = new GMController();

    private final BooleanProperty fileLoadedProperty = new SimpleBooleanProperty(false);

    /**
     * The user the tester is currently "playing". Ex2 has no login - the tester
     * impersonates each user in turn - so this is set from the users table. In
     * ex3 it will be filled from the login page instead, and nothing downstream
     * has to change.
     *
     * Held as the DTO rather than the name because almost every consumer needs
     * more than a name: the balance for the top bar, the blocked flag to disable
     * actions, and the identity itself to compare against an event's market
     * maker. Passing the name around would mean looking the user up again on
     * every read.
     */
    private final ObjectProperty<UserDTO> activeUser = new SimpleObjectProperty<>(null);

    private boolean animationsEnabled = true;

    @FXML private BorderPane rootPane;

    /*
     * FXMLLoader injects an included file's controller into a field named
     * "<fx:id>Controller". These names must match the fx:id values on the
     * <fx:include> elements in main.fxml. The two tab controllers wire their own
     * halves, so this class never touches a left/right pane directly.
     */
    @FXML private TopController      topPaneController;
    @FXML private EventTabController eventPaneController;
    @FXML private UsersTabController usersPaneController;

    @FXML private Tab eventsTab;
    @FXML private Tab usersTab;

    /**
     * Runs AFTER every included controller's own initialize(), which is why the
     * cross-pane wiring lives here and not in the panes themselves.
     */
    @FXML
    public void initialize() {
        topPaneController.init(this);
        eventPaneController.init(this);
        usersPaneController.init(this);

    }

    @FXML
    void refreshUsersTab(Event event) {
        if (usersTab.isSelected()) {
            usersPaneController.refresh();
        }
    }

    // ---------------- shared state, read by the panes ----------------

    public GMController getEngine() {
        return controller;
    }

    public BooleanProperty fileLoadedProperty() {
        return fileLoadedProperty;
    }

    public BorderPane getRootPane() {
        return rootPane;
    }

    public ObjectProperty<UserDTO> activeUserProperty() {
        return activeUser;
    }

    public UserDTO getActiveUser() {
        return activeUser.get();
    }

    public void setActiveUser(UserDTO user) {
        activeUser.set(user);
    }

    /** True when there is an active user who is still allowed to act. */
    public boolean canActiveUserTrade() {
        UserDTO user = activeUser.get();
        return user != null && !user.isBlocked();
    }

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
    }

    // ---------------- events forwarded between the panes ----------------

    /** Top finished loading a file - broadcast to both tabs. */
    public void onFileLoaded() {
        fileLoadedProperty.set(true);
        activeUser.set(null);          // the previous file's users no longer exist
        eventPaneController.onFileLoaded();
        usersPaneController.onFileLoaded();
    }
}