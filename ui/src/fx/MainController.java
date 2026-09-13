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
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Button;
import java.util.ArrayList;
import java.util.List;

/**
 * Root coordinator for the composition tree.
 * It owns the things every pane must agree on - the engine controller, whether a
 * file has been loaded, who the active user is, and whether animations are
 * enabled - and forwards events down to the two tab controllers. Anything that
 * stays inside a single tab (a row selection, a bet, closing an event) is handled
 * by that tab's own controller and never reaches this class.
 */
public class MainController {

    /** The single engine instance for the whole application. */
    private final GMController controller = new GMController();
    private SlidingTabs slidingTabs;
    private final List<KeycapButton> keycapButtons = new ArrayList<>();

    private final BooleanProperty fileLoadedProperty = new SimpleBooleanProperty(false);

    private final ObjectProperty<UserDTO> activeUser = new SimpleObjectProperty<>(null);

    private boolean animationsEnabled = false;

    private final RowAnimations rowAnimations =
            new RowAnimations(this::isAnimationsEnabled);

    public RowAnimations rows() {
        return rowAnimations;
    }

    @FXML private TabPane mainTabs;
    @FXML private BorderPane rootPane;
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

        slidingTabs = new SlidingTabs(
                mainTabs,
                this::isAnimationsEnabled
        );
    }

    @FXML
    void refreshUsersTab(Event event) {
        if (usersTab.isSelected()) {
            usersPaneController.refresh();
        }
    }

    @FXML
    void refreshEventsTab(Event event) {
        if (eventsTab.isSelected()) {
            eventPaneController.refresh();
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

    public void clearActiveUser() {
       activeUser.set(null);
    }

    public UserDTO getActiveUser() {
        return activeUser.get();
    }

    public void setActiveUser(UserDTO user) {
        activeUser.set(user);
    }

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;

        if (slidingTabs != null) {
            slidingTabs.refreshMotion();
        }

        keycapButtons.forEach(KeycapButton::refresh);

    }

    // ---------------- events forwarded between the panes ----------------

    /** Top finished loading a file - broadcast to both tabs. */
    public void onFileLoaded() {
        rowAnimations.reset();

        fileLoadedProperty.set(true);
        eventPaneController.onFileLoaded();
        usersPaneController.onFileLoaded();
        clearActiveUser();
    }

    public void installKeycap(Button button) {
        keycapButtons.add(
                new KeycapButton(button, this::isAnimationsEnabled)
        );
    }
}