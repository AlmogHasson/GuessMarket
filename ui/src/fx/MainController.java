package fx;

import api.GMController;
import dto.EventSummaryDTO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/**
 * Coordinator for the three included panes.
 *
 * It owns the things the panes must agree on - the engine controller, whether a
 * file has been loaded, and whether animations are enabled - and forwards events
 * between them. The panes never talk to each other directly; everything goes
 * through here, so there is exactly one place to look when a change in one pane
 * has to be reflected in another.
 */
public class MainController {

    /** The single engine instance for the whole application. */
    private final GMController controller = new GMController();

    private final BooleanProperty fileLoadedProperty = new SimpleBooleanProperty(false);

    private boolean animationsEnabled = true;

    @FXML private BorderPane rootPane;

    /*
     * FXMLLoader injects an included file's controller into a field named
     * "<fx:id>Controller". These three names must match the fx:id values on the
     * <fx:include> elements in main.fxml.
     */
    @FXML private TopController       topPaneController;
    @FXML private LeftSideController  leftPaneController;
    @FXML private RightSideController rightPaneController;

    // ---------- Users tab (still owned by this controller) ----------
    @FXML private Tab eventsTab;
    @FXML private Tab usersTab;
    @FXML private TableView<?> usersTable;
    @FXML private TableColumn<?, ?> userListIdCol;
    @FXML private TableColumn<?, ?> userListUserCol;
    @FXML private TableColumn<?, ?> userListTypeCol;
    @FXML private Label balance;
    @FXML private TableView<?> userEventsTable;
    @FXML private TableColumn<?, ?> userEventCol;
    @FXML private TableColumn<?, ?> userEventRoleCol;
    @FXML private TableColumn<?, ?> userEventSharesCol;
    @FXML private TableColumn<?, ?> userEventInvestmentCol;
    @FXML private TableView<?> singleEventTable;
    @FXML private TableColumn<?, ?> singleEventOptionCol;
    @FXML private TableColumn<?, ?> singleEventSharesCol;
    @FXML private TableColumn<?, ?> singleEventPaidCol;
    @FXML private TableColumn<?, ?> singleEventCommissionCol;

    /**
     * Runs AFTER every included controller's own initialize(), which is why the
     * cross-pane wiring lives here and not in the panes themselves.
     */
    @FXML
    public void initialize() {
        topPaneController.init(this);
        leftPaneController.init(this);
        rightPaneController.init(this);
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

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
    }

    // ---------------- events forwarded between the panes ----------------

    /** Top finished loading a file. */
    public void onFileLoaded() {
        fileLoadedProperty.set(true);
        leftPaneController.refreshCommissionFilterValues();
        leftPaneController.reloadEvents();
        rightPaneController.clear();
    }

    /** Left changed its table selection (selected may be null). */
    public void onEventSelected(EventSummaryDTO selected) {
        rightPaneController.showEvent(selected);
    }

    /**
     * Right changed engine state (a bet was placed, an event was closed).
     * The left table is rebuilt and the same row re-selected, so the details pane
     * and the events table never disagree.
     */
    public void onEventChanged(int eventId) {
        leftPaneController.reloadEventsAndSelect(eventId);
    }
}
