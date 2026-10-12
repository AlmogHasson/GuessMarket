package controllers;

import controllers.util.DialogHelper;
import dto.ChatMessageDTO;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import util.HttpClientUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatController {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    /** Distance from the left edge of the window when not moved. Must match the margin in chat.fxml. */
    private static final double HOME_MARGIN_LEFT = 16;

    /** Mouse movement below this many pixels is a click, not a drag. */
    private static final double DRAG_THRESHOLD = 4;

    @FXML private VBox chatRoot;
    @FXML private VBox chatBody;
    @FXML private Label chevron;
    @FXML private Label unreadBadge;
    @FXML private ListView<ChatMessageDTO> messagesList;
    @FXML private TextField inputField;
    @FXML private Button sendBtn;

    private MainController main;

    private int lastId = 0;
    private ScheduledService<List<ChatMessageDTO>> poller;

    private boolean expanded = false;
    private boolean historyLoaded = false;   // first poll = old history, not "unread"
    private int unread = 0;

    // drag state
    private double pressSceneX;
    private double pressSceneY;
    private double startTranslateX;
    private double startTranslateY;
    private boolean dragged = false;

    @FXML
    public void initialize() {
        messagesList.setPlaceholder(new Label("No messages yet"));
        messagesList.setSelectionModel(null);
        messagesList.setCellFactory(lv -> new ListCell<>() {
            {
                setWrapText(true);
                setPrefWidth(0);          // wrap instead of growing a horizontal scrollbar
            }

            @Override
            protected void updateItem(ChatMessageDTO m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null
                        : "[" + TIME.format(Instant.ofEpochMilli(m.timestamp())) + "] "
                          + m.userName() + ": " + m.text());
            }
        });
    }

    /** Called by MainController after the whole scene graph is built. */
    public void init(MainController main) {
        this.main = main;

        // keep the window inside the main window when it grows/shrinks or the window is resized
        chatRoot.heightProperty().addListener((obs, o, n) -> clampToParent());
        Parent parent = chatRoot.getParent();
        if (parent instanceof Region region) {
            region.widthProperty().addListener((obs, o, n) -> clampToParent());
            region.heightProperty().addListener((obs, o, n) -> clampToParent());
        }
    }

    // ---------------- polling ----------------

    /** Call after login. */
    public void start() {
        if (poller != null) return;

        lastId = 0;
        historyLoaded = false;
        setUnread(0);
        messagesList.getItems().clear();

        poller = new ScheduledService<>() {
            @Override
            protected Task<List<ChatMessageDTO>> createTask() {
                final int since = lastId;
                return new Task<>() {
                    @Override
                    protected List<ChatMessageDTO> call() throws Exception {
                        return HttpClientUtil.getChatMessages(since);
                    }
                };
            }
        };
        poller.setPeriod(Duration.seconds(1));
        poller.setOnSucceeded(e -> append(poller.getValue()));
        poller.start();
    }

    /** Call on logout / window close. */
    public void stop() {
        if (poller != null) {
            poller.cancel();
            poller = null;
        }
    }

    private void append(List<ChatMessageDTO> msgs) {
        boolean first = !historyLoaded;
        historyLoaded = true;
        if (msgs == null || msgs.isEmpty()) return;

        messagesList.getItems().addAll(msgs);
        lastId = msgs.get(msgs.size() - 1).id();

        if (expanded) {
            messagesList.scrollTo(messagesList.getItems().size() - 1);
        } else if (!first) {
            String me = main.getActiveUser() == null ? null : main.getActiveUser().getName();
            long fromOthers = msgs.stream().filter(m -> !m.userName().equals(me)).count();
            if (fromOthers > 0) {
                setUnread(unread + (int) fromOthers);
            }
        }
    }

    // ---------------- expand / collapse ----------------

    @FXML
    void toggle() {
        if (dragged) {          // the mouse release after a drag is not a click
            dragged = false;
            return;
        }

        expanded = !expanded;
        chatBody.setVisible(expanded);
        chatBody.setManaged(expanded);
        chevron.setText(expanded ? "▼" : "▲");

        if (expanded) {
            setUnread(0);
            messagesList.scrollTo(messagesList.getItems().size() - 1);
            inputField.requestFocus();
        }
    }

    private void setUnread(int n) {
        unread = n;
        unreadBadge.setText(String.valueOf(n));
        unreadBadge.setVisible(n > 0);
        unreadBadge.setManaged(n > 0);
    }

    // ---------------- dragging ----------------

    @FXML
    void onHeaderPressed(MouseEvent e) {
        pressSceneX = e.getSceneX();
        pressSceneY = e.getSceneY();
        startTranslateX = chatRoot.getTranslateX();
        startTranslateY = chatRoot.getTranslateY();
        dragged = false;
    }

    @FXML
    void onHeaderDragged(MouseEvent e) {
        double dx = e.getSceneX() - pressSceneX;
        double dy = e.getSceneY() - pressSceneY;

        if (!dragged && Math.hypot(dx, dy) < DRAG_THRESHOLD) {
            return;
        }
        dragged = true;

        chatRoot.setTranslateX(startTranslateX + dx);
        chatRoot.setTranslateY(startTranslateY + dy);
        clampToParent();
    }

    /**
     * Keeps the whole chat window inside its parent. The window is anchored
     * bottom-left, so its un-moved position is x = HOME_MARGIN_LEFT, y = parentHeight - height,
     * and translateX/Y are offsets from that position.
     */
    private void clampToParent() {
        Parent parent = chatRoot.getParent();
        if (parent == null) return;

        double w = chatRoot.getWidth();
        double h = chatRoot.getHeight();
        double pw = parent.getLayoutBounds().getWidth();
        double ph = parent.getLayoutBounds().getHeight();

        double minX = -HOME_MARGIN_LEFT;
        double maxX = pw - w - HOME_MARGIN_LEFT;
        double minY = -(ph - h);
        double maxY = 0;

        chatRoot.setTranslateX(Math.max(minX, Math.min(maxX, chatRoot.getTranslateX())));
        chatRoot.setTranslateY(Math.max(minY, Math.min(maxY, chatRoot.getTranslateY())));
    }

    // ---------------- sending ----------------

    @FXML
    void send() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        sendBtn.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                HttpClientUtil.sendChatMessage(text);
            } catch (Exception ex) {
                Platform.runLater(() ->
                        DialogHelper.showErrorAlert("Could not send message", ex.getMessage()));
            } finally {
                Platform.runLater(() -> sendBtn.setDisable(false));
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
