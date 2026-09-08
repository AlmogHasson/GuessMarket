package fx;

import dto.EventSummaryDTO;
import javafx.fxml.FXML;

/**
 * Controller for event.fxml - the Events tab.
 *
 * Owns the conversation between its own two halves. A selection on the left or a
 * bet/close on the right never leaves this tab, so MainController does not need
 * to know either happened.
 */
public class EventTabController {

    private MainController main;

    // injected by FXMLLoader from the <fx:include> fx:id values in event.fxml
    @FXML private EventLeftController  eventLeftPaneController;
    @FXML private EventRightController eventRightPaneController;

    /** Called by MainController; forwards the wiring down to both halves. */
    public void init(MainController main) {
        this.main = main;
        eventLeftPaneController.init(main, this);
        eventRightPaneController.init(main, this);
    }

    /** A file finished loading - rebuild everything in this tab. */
    public void onFileLoaded() {
        eventLeftPaneController.refreshCommissionFilterValues();
        eventLeftPaneController.loadEvents();
        eventRightPaneController.clear();
    }

    /** Left changed its selection (may be null). */
    public void onEventSelected(EventSummaryDTO selected) {
        eventRightPaneController.showEvent(selected);
    }

    /**
     * Right mutated engine state. The left table is rebuilt and the same row
     * re-selected, which calls onEventSelected again with a fresh DTO - the DTOs
     * are records, so the old one can never reflect the change.
     */
    public void onEventChanged(int eventId) {
        // Refresh the right pane immediately from the current engine state.
        // Important for Order Book because quantities, minting and event balance
        // can all change after a single order.
        eventRightPaneController.refresh();

        // Then refresh the event list on the left.
        eventLeftPaneController.reloadEventsAndSelect(eventId);
    }

    public void refresh() {
        eventRightPaneController.refresh();
    }
}
