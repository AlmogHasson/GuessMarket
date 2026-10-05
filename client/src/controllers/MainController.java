package controllers;

import api.GMController;
import controllers.eventsTab.EventTabController;
import controllers.usersTab.UsersTabController;
import controllers.util.DialogHelper;
import controllers.util.KeycapButton;
import controllers.util.RowAnimations;
import controllers.util.SlidingTabs;
import dto.UserDTO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.Constants;
import util.HttpClientUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private int lastVersion = -1;
    private ScheduledService<Integer> poller;

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
    @FXML private FileLoadController fileLoadPaneController;

    @FXML private Tab eventsTab;
    @FXML private Tab usersTab;

    /**
     * Runs AFTER every included controller's own initialize(), which is why the
     * cross-pane wiring lives here and not in the panes themselves.
     */
    @FXML
    public void initialize() {
        topPaneController.init(this);
        fileLoadPaneController.init(this);
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



    private void startPolling() {
        if (poller != null) {
            return; // already running
        }

        poller = new ScheduledService<>() {
            @Override
            protected Task<Integer> createTask() {
                return new Task<>() {
                    @Override
                    protected Integer call() throws Exception {
                        return HttpClientUtil.getVersion();
                    }
                };
            }
        };

        poller.setPeriod(Duration.millis(Constants.REFRESH_RATE_MILLIS));
        poller.setOnSucceeded(e -> {
            int version = poller.getValue();
            boolean changed = lastVersion != -1 && version != lastVersion;
            lastVersion = version;
            if (changed) {
                onServerChanged();
            }
        });
        poller.start();
    }

    private void onServerChanged() {
        if (eventsTab.isSelected()) {
            eventPaneController.refresh();
        } else if (usersTab.isSelected()) {
            usersPaneController.refresh();
        }
        refreshActiveUser();
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


    /** Called after login: the server may already hold events uploaded by someone else. */
    public void syncWithServer() {
        rowAnimations.reset();
        fileLoadedProperty.set(true);
        eventPaneController.onFileLoaded();
        usersPaneController.onFileLoaded();

        refreshActiveUser();
    }

    private void refreshActiveUser() {
        Task<UserDTO> task = new Task<>() {
            @Override
            protected UserDTO call() throws Exception {
                return HttpClientUtil.getCurrentUser();
            }
        };
        task.setOnSucceeded(e -> setActiveUser(task.getValue()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void installKeycap(Button button) {
        keycapButtons.add(
                new KeycapButton(button, this::isAnimationsEnabled)
        );
    }

    public void setUserName(String userName) {
        if (userName != null && !userName.isBlank()) {
            // balance and events are placeholders until the server sends the real UserDTO
            setActiveUser(new UserDTO(0, List.of(), userName));
            syncWithServer();
            startPolling();
        } else {
            clearActiveUser();
        }
    }


    /**
     * Swaps the scene's root back to the login page. The scene (and so the
     * current theme) is kept; this MainController is simply dropped, which is
     * what resets all per-session state.
     */
    public void showLoginScreen() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxmls/login.fxml")));
            Stage stage = (Stage) rootPane.getScene().getWindow();

            stage.getScene().setRoot(root);
            stage.setTitle("Guess Market - Login");
            stage.setMinWidth(460);
            stage.setMinHeight(520);
            stage.setWidth(520);
            stage.setHeight(560);
            stage.centerOnScreen();
        } catch (IOException e) {
            DialogHelper.showErrorAlert("Logout", "Could not load the login screen: " + e.getMessage());
        }
    }
}
