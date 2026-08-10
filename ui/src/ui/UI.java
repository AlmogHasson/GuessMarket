package ui;
import engine.Engine;
import engine.EngineImpl;

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

    private final Engine engine = new EngineImpl();

    private void displayMenu() {
        System.out.println("to load file press 1");
        System.out.println("to show events press 2");
        System.out.println("to show market status 3");
        System.out.println("to participate in event press 4");
        System.out.println("to exit program press 5");
    }

    boolean isUserInputValid(String input) {
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

    void executeOption(MenuOption option) {
        switch (option){
            case LOAD_FILE -> {
                engine.loadFile();
            }
            case GET_EVENTS -> {
                engine.getEvents();
            }
            case GET_EVENT_TRADING_STATUS -> {
                engine.getEventTradingStatus();
            }
            case PARTICIPATE_IN_EVENT -> {
                engine.participateInEvent();
            }
            case CLOSE_EVENT -> {
                engine.closeEvent();
            }
            case EXIT -> {
                engine.exit();
            }
        }
    }

    MenuOption intToMenuOption(int num) {
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
        boolean isInputValid = false;
        Scanner input = new Scanner(System.in);
        displayMenu();
        String option = input.next();

        while (!isInputValid) {
            isInputValid = isUserInputValid(option);
            if (isInputValid) {
                int optionNumber = Integer.parseInt(option);
                MenuOption menuOption = intToMenuOption(optionNumber);
                executeOption(menuOption);
            }
            else {
                System.out.println("Invalid Input, please input a number between 1 to 5");
                displayMenu();
                option = input.next();
            }
        }
    }

}
