package ui;
import api.GMController;
import dto.EventDTO;

import java.util.List;
import java.util.Scanner;

public final class UI {
    private enum MenuOption {
        LOAD_FILE(1),
        GET_EVENTS(2),
        GET_EVENT_TRADING_STATUS(3),
        PARTICIPATE_IN_EVENT(4),
        CLOSE_EVENT(5),
        EXIT(6);

        private final int value;

        MenuOption(int value) {
            this.value = value;
        }
    }

    private final GMController controller = new GMController();

    private void displayMenu() {
        System.out.println("to load file press 1");
        System.out.println("to show events press 2");
        System.out.println("to show market status 3");
        System.out.println("to participate in event press 4");
        System.out.println("to close event press 5");
        System.out.println("to exit program press 6");
    }

    private boolean isUserInputValid(String input) {
        int choice;
        try {
            choice = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return false;
        }
        for (MenuOption op : MenuOption.values()) {
            if (op.value == choice) {
                return true;
            }
        }
        return false;
    }

    private String getPath(String prompt) {
        Scanner input = new Scanner(System.in);
        System.out.println(prompt);
        return input.nextLine();
    }

    private void executeOption(MenuOption option) {
        switch (option){
            //test :   C:\Users\almog\IdeaProjects\GuessMarket\test files\test1.xml
            //test noa: /Users/noaallouche/uni/java course/project Gusss Market/multiple.xml
            case LOAD_FILE -> {
                boolean loaded = false;

                while (!loaded) { //loop until a valid file is loaded
                    try {
                        controller.loadFile(getPath("Please input a valid file path:"));
                        System.out.println("File loaded successfully! Events are now updated and available.");
                        loaded = true;
                    } catch (Exception e) {
                        System.out.println("Error occurred while loading file: " + e.getMessage());
                    }
                }
            }
            case GET_EVENTS -> {
                var events = controller.getEvents();
                if (events == null || events.isEmpty()) {
                    System.out.println("  (no events)");
                } else {
                    displayEvents(events);
                }
            }
            case GET_EVENT_TRADING_STATUS -> {
                var events = controller.getEvents();
                int eventNumber = getEventNumber(events.size());
                if (eventNumber == -1) {break;}
                EventDTO selectedEvent = events.get(eventNumber - 1);
                int eventId = selectedEvent.getId();
                controller.getEventTradingStatus(eventId);
            }
            case PARTICIPATE_IN_EVENT -> {
                controller.participateInEvent();
            }
            case CLOSE_EVENT -> {
                controller.closeEvent();
            }
            case EXIT -> {
                System.out.println("Exiting program...");
            }
        }
    }

    //helper method to get event number from user input
    private int getEventNumber(int numberOfEvents) {
        Scanner input = new Scanner(System.in);
        System.out.print("Choose event number: ");
        int eventNumber;
        try {
            eventNumber = input.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input.");
            return -1;
        }

        if (eventNumber < 1 || eventNumber > numberOfEvents) {
            System.out.println("Invalid event number.");
            return -1;
        }

        return eventNumber;
    }

    private static void displayEvents(List<EventDTO> events) {
        final int width = 64;
        final String sep = "=".repeat(width);
        for (var event : events) {
            System.out.println(sep);
            String title = "EVENT DETAILS (ID: " + event.getId() + ")";
            int leftPad = Math.max(0, (width - title.length()) / 2);
            System.out.println(" ".repeat(leftPad) + title);
            System.out.println("-".repeat(width));
            String name = event.getName() == null ? "" : event.getName().replace(", ", " ");
            System.out.printf("%-14s: %s%n", "Name", name);
            System.out.printf("%-14s: %s%n", "Description", event.getDescription() == null ? "" : event.getDescription());
            var c = event.getComission();             if (c != null) System.out.printf("%-14s: %s%n", "Commission", String.format("%d%% (%s)", c.value(), c.type()));
            if (event.getMethod() != null && event.getMethod().lmsr() != null)
                System.out.printf("%-14s: %d%n", "LMSR b", event.getMethod().lmsr().getB());
            System.out.println();
            System.out.println("---- Options ----");
            System.out.printf("%-4s %-54s%n", "#", "Option");
            System.out.println("-".repeat(width));
            int i = 1;
            if (event.getOptions() != null) {
                for (var opt : event.getOptions()) {
                    String optText = opt == null ? "" : (opt.getOption() == null ? "" : opt.getOption().replace(", ", " "));
                    System.out.printf("%-4d %-54s%n", i++, optText);
                }
            }
            System.out.println(sep);
            System.out.println();
        }
    }

    private MenuOption intToMenuOption(int num) {
        for (MenuOption option : MenuOption.values()) {
            if (option.value == num) {
                return option;
            }
        }
        return null;
    }


    static void main() {
        UI myUI = new UI();
        myUI.run();
    }

private void run() {
        Scanner input = new Scanner(System.in);
        displayMenu();
        while (true) {
            String option = input.next().trim();
            if (option.equalsIgnoreCase("back")) {
                displayMenu(); continue;
            }
            if (option.equalsIgnoreCase("exit") || option.equals(String.valueOf(MenuOption.EXIT.value)))
                break;
            boolean validNumber;
            try {
                validNumber = intToMenuOption(Integer.parseInt(option)) != null;
            }
            catch (NumberFormatException e) {
                validNumber = false;
            }
            if (!validNumber) {
                System.out.println("Invalid input. Type BACK to show the menu.");
                displayMenu(); continue;
            }
            executeOption(intToMenuOption(Integer.parseInt(option)));
            System.out.println("Type 'BACK' to show the menu, or select another option (1-6).");
        }     System.out.println("Exiting...");
    }
}
