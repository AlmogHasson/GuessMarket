package fx;

import dto.CommissionDTO;
import dto.EventSummaryDTO;
import dto.LMSRDTO;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.util.List;
import java.util.stream.Collectors;

/** Controller for left.fxml - the four filters and the events table. */
public class LeftSideController {

    /** Sentinel for "no commission filter"; -1 can never collide with a real value. */
    private static final int ALL_COMMISSIONS = -1;

    private MainController main;

    @FXML private ComboBox<String>  methodFilter;
    @FXML private ComboBox<String>  statusFilter;
    @FXML private ComboBox<Integer> commissionFilter;
    @FXML private ComboBox<String>  commissionTypeFilter;

    @FXML private TableView<EventSummaryDTO> eventsTable;
    @FXML private TableColumn<EventSummaryDTO, Integer> eventListIdCol;
    @FXML private TableColumn<EventSummaryDTO, String>  eventListEventCol;
    @FXML private TableColumn<EventSummaryDTO, String>  eventListMethodCol;
    @FXML private TableColumn<EventSummaryDTO, String>  eventListStatusCol;
    @FXML private TableColumn<EventSummaryDTO, String>  eventListCommissionCol;
    @FXML private TableColumn<EventSummaryDTO, String>  eventListCommissionTypeCol;

    /**
     * Column factories and filter items are self-contained, so they belong here
     * rather than being redone on every load the way the old loadEvents() did.
     */
    @FXML
    public void initialize() {
        initColumns();
        initFilterItems();
        initCommissionConverter();
    }

    public void init(MainController main) {
        this.main = main;

        commissionFilter.disableProperty().bind(main.fileLoadedProperty().not());
        methodFilter.disableProperty().bind(main.fileLoadedProperty().not());
        statusFilter.disableProperty().bind(main.fileLoadedProperty().not());
        commissionTypeFilter.disableProperty().bind(main.fileLoadedProperty().not());

        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> main.onEventSelected(newSelection));
    }

    // ---------------- public API used by MainController ----------------

    /** Rebuilds the table from the engine. The setAll is queued, as before. */
    public void reloadEvents() {
        ObservableList<EventSummaryDTO> events =
                FXCollections.observableArrayList(main.getEngine().getEvents());

        Platform.runLater(() -> eventsTable.getItems().setAll(events));
    }

    /**
     * Rebuild, then re-select the same event.
     *
     * The selection has to be queued too. runLater is FIFO, so this block is
     * guaranteed to run after the setAll posted by reloadEvents() - reading the
     * table any earlier would still see the pre-change rows.
     *
     * EventSummaryDTO is a record, so the refreshed DTO is not equal() to the old
     * one once isOpen flips. Re-selecting therefore has to look the row up by id
     * in the new list rather than re-using the previously selected object.
     */
    public void reloadEventsAndSelect(int eventId) {
        reloadEvents();

        Platform.runLater(() -> {
            EventSummaryDTO updated = eventsTable.getItems().stream()
                    .filter(e -> e.getId() == eventId)
                    .findFirst()
                    .orElse(null);

            if (updated != null) {
                eventsTable.getSelectionModel().select(updated);
                main.onEventSelected(updated);
            }
        });
    }

    /** Repopulates the commission dropdown from the values present in the file. */
    public void refreshCommissionFilterValues() {
        ObservableList<Integer> values = main.getEngine().getEvents().stream()
                .map(EventSummaryDTO::getCommission)
                .map(CommissionDTO::value)
                .distinct()
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        values.addFirst(ALL_COMMISSIONS);
        commissionFilter.setItems(values);
        commissionFilter.setValue(ALL_COMMISSIONS);
    }

    public EventSummaryDTO getSelectedEvent() {
        return eventsTable.getSelectionModel().getSelectedItem();
    }

    // ---------------- filter handlers ----------------

    @FXML void onCommissionFilterChanged(ActionEvent event)     { filterEvents(); }
    @FXML void onMethodFilterChanged(ActionEvent event)         { filterEvents(); }
    @FXML void onStatusFilterChanged(ActionEvent event)         { filterEvents(); }
    @FXML void onCommissionTypeFilterChanged(ActionEvent event) { filterEvents(); }

    private void filterEvents() {
        if (main == null) {
            return;
        }

        Integer selectedCommission = commissionFilter.getValue();
        String selectedMethod = methodFilter.getValue();
        String selectedStatus = statusFilter.getValue();
        String selectedCommissionType = commissionTypeFilter.getValue();

        List<EventSummaryDTO> filtered = main.getEngine().getEvents().stream()
                .filter(evnt -> selectedCommission == null
                        || selectedCommission == ALL_COMMISSIONS
                        || evnt.getCommission().value() == selectedCommission)
                .filter(evnt -> selectedMethod == null
                        || "All".equals(selectedMethod)
                        || ("Lmsr".equals(selectedMethod) && evnt.getMethod() instanceof LMSRDTO)
                        || ("Order Book".equals(selectedMethod) && !(evnt.getMethod() instanceof LMSRDTO)))
                .filter(evnt -> selectedStatus == null
                        || "All".equals(selectedStatus)
                        || ("Open".equals(selectedStatus) && evnt.isOpen())
                        || ("Closed".equals(selectedStatus) && !evnt.isOpen()))
                .filter(evnt -> selectedCommissionType == null
                        || "All".equals(selectedCommissionType)
                        || selectedCommissionType.equalsIgnoreCase(evnt.getCommission().commissionType()))
                .toList();

        Platform.runLater(() -> eventsTable.getItems().setAll(filtered));
    }

    // ---------------- one-time setup ----------------

    private void initColumns() {
        eventListIdCol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getId()));

        eventListEventCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getName()));

        eventListMethodCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getMethod() instanceof LMSRDTO ? "Lmsr" : "Order Book"));

        eventListStatusCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().isOpen() ? "Open" : "Closed"));

        eventListCommissionCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().getCommission().value())));

        eventListCommissionTypeCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().comission().commissionType()));
    }

    private void initFilterItems() {
        methodFilter.getItems().setAll("All", "Lmsr", "Order Book");
        statusFilter.getItems().setAll("All", "Open", "Closed");
        commissionTypeFilter.getItems().setAll("All", "On-Close", "On-Purchase");
    }

    private void initCommissionConverter() {
        commissionFilter.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer value) {
                return (value == null || value == ALL_COMMISSIONS) ? "All" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                return (text == null || text.equals("All"))
                        ? ALL_COMMISSIONS
                        : Integer.parseInt(text);
            }
        });
    }
}
