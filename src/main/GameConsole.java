package main;

import commands.*;
import game.*;
import questions.QuestionSession;
import questions.QuestionsControler;
import java.io.*;
import java.util.HashMap;
import java.util.Scanner;


/**
 * The main.GameConsole class manages the execution of the Space Adventure Game "The Beginning".
 * It initializes the game, handles user commands, and maintains game state.
 */
public class GameConsole {
    private boolean exit = false;
    private HashMap<String, Command> commands = new HashMap<>();
    public static String commandLogFile = "res/commands.txt";

    private Scanner scanner = new Scanner(System.in);
    private CrystalBag crystalBag = new CrystalBag();
    private Planet planet;
    private BaseStation baseStation = new BaseStation(planet);
    private Universe universe = new Universe();
    private Location playerLocation;
    private Astrokoala astroKoala;
    private BigBang bigBang;
    private Comet comet;
    private GalacticSailor galacticSailor;
    private PlanetGateKeeper pgk;
    private Prompter prompter;
    private QuestionSession questionSession;
    private QuestionsControler questionsControler;
    private ShowCrystals show;
    private RulesCommand rules;
    private SaveCommand saveCommand;
    private LoadCommand loadCommand;


    /**
     * Initializes the game, loads the universe map, and sets up game components.
     */
    public void initialize() {
        universe.loadMap("res/map.csv");

        Planet startPlanet = universe.getPlanet("Station");
        if (startPlanet == null) {
            System.out.println("Station not found in the universe.");
            return;
        }
        playerLocation = new Location(startPlanet);
        crystalBag = new CrystalBag();
        baseStation = new BaseStation(startPlanet);
        comet = new Comet(baseStation);
        rules = new RulesCommand();
        astroKoala = new Astrokoala(baseStation, comet);
        galacticSailor = new GalacticSailor(crystalBag, baseStation, playerLocation, astroKoala, bigBang, comet, pgk);

        bigBang = new BigBang(comet, galacticSailor);
        prompter = galacticSailor.getPrompter();
        show = new ShowCrystals(galacticSailor, baseStation);
        questionsControler = new QuestionsControler(galacticSailor, crystalBag);
        saveCommand = new SaveCommand(this);
        loadCommand = new LoadCommand(this);

        commands = new HashMap<>();
        commands.put("fly", new FlyCommand(playerLocation, questionsControler, universe, pgk, baseStation));
        commands.put("talk", new TalkCommand(galacticSailor));
        commands.put("take", new TakeCrystal(crystalBag, galacticSailor, universe));
        commands.put("position", new PositionCrystal(crystalBag, baseStation, galacticSailor));
        commands.put("help", new Help());
        commands.put("leave", new Exit());
        commands.put("hint", new HintCommand(astroKoala));
        commands.put("check", new CheckCrystals(astroKoala, galacticSailor, baseStation, comet));
        commands.put("rules", rules);
        commands.put("bigbang", bigBang);
        commands.put("comet", comet);
        commands.put("prompter", prompter);
        commands.put("show", show);
        commands.put("cometplan", new CometPlan(comet));
        commands.put("save", saveCommand);
        commands.put("load", loadCommand);
    }

    /**
     * Displays the introduction and game instructions.
     */
    private void showIntro() {
        System.out.println("\nWelcome to the Space Adventure Game called The Beginning! 🚀");
        System.out.println("\n Your name: Galactic Sailor \n Astrokoala: your friend and helper! Astrokoala gives you random hints on the game and also checks whether your comets are ready for the Big bang!");
        System.out.println("\n--------------- .\uD81A\uDD54 \uD83E\uDE90˖ Game Rules - The Beginning: .\uD81A\uDD54 \uD83E\uDE90˖ --------------------------\n" +
                "\nTravel between planets using the command 'fly [planet name]'.\n" +
                "Answer the gatekeepers questions correctly to collect a crystal. If you dont know the answer try using 'prompter'.\n" +
                "Bring the crystals to the base station and place them using 'position'.\n" +
                "Arrange two stacks of 5 crystals correctly.\n" +
                "You can always use 'cometplan' to see your crystals arrangement in your comets!\n"+
                "Use 'comet' and then type in the the name of the crystal to add crystal to the comet.\n" +
                "Activate 'bigbang' and witness the Big Bang!\n" +
                "You can always use 'help' to see available commands and planets you can travel to.");


        System.out.println("\nYou are currently on " + playerLocation.getCurrentLocation());
        System.out.println("\n\uD83C\uDF0D Available Planets:");
        System.out.println("""
                - Station    - The main station where your space journey begins.
                - Colverde   - A green planet full of vegetation and oxygen.
                - Luminara   - A glowing planet filled with light sources.
                - Rosetta    - A planet with pink rocks and a strange atmosphere.
                - Crystalia  - A planet covered in icy crystals.
                - Lavatron   - A hot planet with active volcanoes.
                - Aquarix    - A water world with endless oceans.
                - Solaria    - A sun-scorched desert planet.
                - Glacius    - An icy planet with extremely low temperatures.
                - Verdania   - A planet covered in dense forests and exotic creatures.
                - Mysterra   - A mysterious planet shrouded in mist and secrets.
                """);
        System.out.println("\n Available Commands:");
        System.out.println("""
                - fly [planet]      → Travel to a new planet.
                - talk              → Interact with someone on the planet.
                - take              → Collect a crystal from the Planet.
                - position          → Place a collected crystal at the base station.
                - hint              → Astrokoala gives you a random hint of the game.
                - check             → Astrokoala checks if the crystals are placed correctly plus displays are at the BaseStation.
                                      You can use this when you are at the base station ONLY since Astrokoala doesnt travel with you.
                - rules             → Display game rules. Use this whenever you feel lost!
                - show              → Displays the crystals in your crystal bag and at the base station. You can use this whenever you want.
                - help              → Show available commands and planets.
                - leave             → Exit the game.
                - bigbang           → Trigger the Big Bang event when two comets are ready.
                - comet             → Show all information about the comets.
                - prompter          → Show hints for when you dont know the answer.
                - cometplan         → Displays comet crystal distribution.
                - save              → Saves the current state of the game to the file 'game.txt'.
                - load              → Loads the game from the file: 'game.txt'.
                """);
        System.out.println("Type a command to begin your adventure!\n");
    }

    /**
     * Reads a command from the user and executes the corresponding game action.
     */
    private void executeCommand() {
        System.out.print(">> ");
        String input = scanner.nextLine().trim().toLowerCase();
        logCommand(input);

        String[] parts = input.split(" ", 2);
        String commandKey = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";

        if (commands.containsKey(commandKey)) {
            Command command = commands.get(commandKey);
            if (command instanceof FlyCommand) {
                ((FlyCommand) command).setDestination(argument);
            } else {
                command.setCommand(argument);
            }
            System.out.println(" " + command.execute());
            exit = command.exit();
        } else {
            System.out.println(">> Invalid command");
        }
    }

    /**
     * Reads and executes user commands in a loop until exit is triggered.
     */
    public void start() {
        initialize();
        showIntro();
        try {
            resetCommandLog();
            do {
                executeCommand();
            } while (!exit);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * Logs executed commands to a file for tracking user actions.
     * @param command The command entered by the user.
     */
    private void logCommand(String command) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(commandLogFile, true))) {
            bw.write(command);
            bw.newLine();
        } catch (Exception e) {
            System.out.println("Error logging command: " + e.getMessage());
        }
    }

    /**
     * Resets the command log file at the start of a new game session.
     */
    private void resetCommandLog() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(commandLogFile, false))) {
        } catch (Exception e) {
            System.out.println("Error resetting command log: " + e.getMessage());
        }
    }


    /**
     * Saves the current game state to a file.
     * @param filename The name of the file where the game state will be saved.
     *                 If null or empty, "game.txt" will be used as default.
     * @return true if the game was saved successfully, false otherwise.
     */
    public boolean saveGame(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = "res/game.txt";
        }


        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {

            GameState gameState = new GameState();
            gameState.universe = this.universe;

            gameState.crystalBag = this.crystalBag;
            gameState.currentPlanetName = this.playerLocation.getCurrentLocation();
            gameState.baseStation = this.baseStation;
            gameState.galacticSailor = this.galacticSailor;
            gameState.questionsControler = this.questionsControler;
            gameState.comet = this.comet;
            gameState.gameCompleted = this.galacticSailor.isGameCompleted();
            gameState.takenCrystals = this.universe.getTakenCrystals();
            gameState.planets = this.universe.planets;
            gameState.currentPlanetName = this.galacticSailor.getCurrentPlanetName();
            gameState.astrokoala = this.astroKoala;
            oos.writeObject(gameState);
            return true;
        } catch (IOException e) {
            System.out.println("Error while saving the game: " + e.getMessage());
            return false;
        }
    }


    /**
     * Loads the game state from a file.
     * @param filename The name of the file from which the game state will be loaded.
     *                 If null or empty, "res/game.txt" will be used as default.
     * @return true if the game was loaded successfully, false otherwise.
     */
    public boolean loadGame(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = "res/game.txt";
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            GameState gameState = (GameState) ois.readObject();

            this.universe = new Universe();
            this.universe.loadMap("res/map.csv");

            this.universe.getTakenCrystals().putAll(gameState.takenCrystals);

            Planet currentPlanet = this.universe.getPlanet(gameState.currentPlanetName);
            this.playerLocation = new Location(currentPlanet);

            this.galacticSailor.setPlayerLocation(this.playerLocation);
            this.crystalBag = gameState.crystalBag;
            this.baseStation = gameState.baseStation;
            this.galacticSailor = gameState.galacticSailor;
            this.galacticSailor.setPlayerLocation(this.playerLocation);
            this.questionsControler = gameState.questionsControler;
            this.comet = gameState.comet;

            this.astroKoala.setBaseStation(this.baseStation);
            this.astroKoala.setComet(this.comet);
            this.bigBang = new BigBang(this.comet, galacticSailor);

            if (gameState.gameCompleted) {
                System.out.println("🌍 Big Bang has occurred! Congratulations you won the game! The Earth has been created! \nThank you for playing 'The Beginning'! You can now use 'leave' to exit the game.");
            }
            updateCommands();

            System.out.println("You are currently on planet: " + this.playerLocation.getCurrentLocation());
            return true;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error while loading the game: " + e.getMessage());
            return false;
        }
    }

    private void updateCommands() {
        commands.put("fly", new FlyCommand(playerLocation, questionsControler, universe, pgk, baseStation));
        commands.put("talk", new TalkCommand(galacticSailor));
        commands.put("take", new TakeCrystal(crystalBag, galacticSailor, universe));
        commands.put("position", new PositionCrystal(crystalBag, baseStation, galacticSailor));
        commands.put("check", new CheckCrystals(astroKoala, galacticSailor, baseStation, comet));
        commands.put("bigbang", bigBang);
        commands.put("comet", comet);
        commands.put("prompter", prompter);
        commands.put("show", new ShowCrystals(galacticSailor, baseStation));
        commands.put("cometplan", new CometPlan(comet));
    }


}
