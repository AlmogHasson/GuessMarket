package ui;
import api.GMController;

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
        System.out.println("to exit program press 5");
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

    private String getValidUserPath(String prompt) {
        Scanner input = new Scanner(System.in);
        String userInput;
        boolean isValid = false;
        do {
            System.out.println(prompt);
            userInput = input.nextLine();
            isValid = isUserInputValid(userInput);
            if (!isValid) {
                System.out.println("Invalid Input, please input a valid path");
            }
        } while (!isValid);
        return userInput;
    }

    private void executeOption(MenuOption option) {
        switch (option){
            case LOAD_FILE -> {
                try {
                    controller.loadFile(getValidUserPath("Please input a valid file path:"));
                } catch (Exception e) {
                    System.out.println("Error occurred while loading file: " + e.getMessage());
                }
            }
            case GET_EVENTS -> {
                controller.getEvents();
            }
            case GET_EVENT_TRADING_STATUS -> {
                controller.getEventTradingStatus();
            }
            case PARTICIPATE_IN_EVENT -> {
                controller.participateInEvent();
            }
            case CLOSE_EVENT -> {
                controller.closeEvent();
            }
            case EXIT -> {
//                engine.exit();
            }
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
        boolean isInputValid = false;
        Scanner input = new Scanner(System.in);
        displayMenu();
        String option = input.next();

        //TODO: implement outer loop until user selects exit option
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
