package fx;

import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class Launcher {
    public static void main(String[] args) {
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            Filter previousFilter = handler.getFilter();

            handler.setFilter(record -> {
                String message = record.getMessage();

                if (message != null && message.startsWith(
                        "Unsupported JavaFX configuration:"
                )) {
                    return false;
                }

                return previousFilter == null
                        || previousFilter.isLoggable(record);
            });
        }

        javafx.application.Application.launch(GuessMarketApp.class, args);
    }
}