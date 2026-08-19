Guess Market – Exercise 1
README
===========================
1. Submitters
    Student 1: Almog Hasson, 315080101, almogha2@mta.ac.il
    Student 2: Noa Allouche, 216369140, noaallouche10@gmail.com

GitHub repository: https://github.com/AlmogHasson/GuessMarket.git

2. Project Overview
    Guess Market is a Java 25 console application that loads market events from an XML file and allows the user to view events, inspect the trading status of an event, participate in an LMSR-based event by purchasing shares, close an event by selecting a winning option, and review the trading history and current event account state.
    Supported main operations:
    Load a Guess Market XML file.
    Display all loaded events.
    Display the current trading status of a selected event.
    Participate in an active event by purchasing shares of one of its two options.
    Close an active event and select the winning option.
    Save the current system state to an external file (bonus).
    Load a previously saved system state (bonus).
    Exit the application.
3. Project Structure and Modules
    The project is divided into separate modules in order to keep the UI independent from the business logic and to allow the engine to be reused in later exercises.
    Engine module
        Contains the system logic and market state. The UI does not print from this module.
        Engine – public engine interface.
        EngineImpl – engine implementation; loads XML files, validates input, manages events, participation, event closing, and save/load state.
        Event – represents a market event and coordinates its LMSR calculations and trading behavior.
        EventTradingStatus – stores the current event state, account balance, total commissions and trading history.
        LMSR – implements the LMSR calculations used for option values and event balance.
        Method – stores the trading method configuration.
        Option – stores option name, current value, purchased shares and winner state.
        Trade – represents a completed trade in the trading history.
        Comission – stores commission type and percentage.
    Controller module
        Contains GMController and DTO classes. It acts as the boundary between the UI and the engine and converts engine objects into DTOs that are safe and convenient for presentation.
    UI module
        Contains the console UI and the main program flow. It is responsible for displaying menus, reading user input, validating interactive input where relevant, calling GMController, and printing results and error messages.
4. Main Design Choices and Assumptions
    Java version: The project is compiled and executed with Java 25.
    User-facing numbering: All selections shown to the user are 1-based.
    XML loading: A valid XML file replaces the previously loaded valid system data. An invalid file must not replace the last valid state.
    Event validation: Event IDs must be unique and commission values must be between 0 and 90 inclusive.
    Decimal output: Calculated decimal values are displayed with up to two digits after the decimal point.
    LMSR events: Exercise 1 supports binary LMSR events with exactly two options.
    DTO separation: The UI receives DTOs through GMController instead of directly manipulating engine objects.
    Saved-state file extension: The bonus save/load implementation uses the .gm extension. The user enters the full path and file name without the extension.
5. Bonus – Save and Load System State
    The bonus is implemented using Java object serialization. The current list of Event objects is serialized to an external .gm file. The Event object graph is serializable, including the current option values, purchased shares, event open/closed state, winner state, account balance, commission totals and trading history.
    The saved-state feature is separate from the regular XML loading command. A saved state can be loaded after restarting the application without first loading an XML file.
    Example save path entered by the user: C:\GuessMarket\saves\marketState
    Actual file created: C:\GuessMarket\saves\marketState.gm
6. How to Run
    Run the application by double-clicking run.bat or by opening Command Prompt in the submission directory and executing run.bat.
7. Suggested Test Flow
    Run the program from the submitted batch file.
    Load a valid XML file using its full path.
    Display the loaded events.
    Open an event trading status.
    Purchase shares in an active event.
    Verify that the option values, account balance and trading history were updated.
    Save the current system state using the bonus command.
    Exit and restart the application.
    Load the previously saved system state without loading XML first.
    Verify that shares, values, balance, commissions, trading history, open/closed state and winner state are preserved.
8. Notes for the Tester
    The application is intended to be operated entirely through the console. All user-facing input and output is in English. File paths may contain spaces. Please enter complete paths when loading XML files or when saving/loading a bonus state file.