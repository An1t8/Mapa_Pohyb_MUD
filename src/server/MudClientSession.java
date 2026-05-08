package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MudClientSession implements Runnable {
    private final Socket socket;
    private final MudWorld world;
    private final PlayerAccountStore accountStore;
    private final ServerLogger logger;
    private MudPlayer player;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean running = true;

    public MudClientSession(Socket socket, MudWorld world, PlayerAccountStore accountStore, ServerLogger logger) {
        this.socket = socket;
        this.world = world;
        this.accountStore = accountStore;
        this.logger = logger;
    }

    @Override
    public void run() {
        try (socket) {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            logger.log("INFO", "CONNECT " + socket.getRemoteSocketAddress());

            player = loginFlow();
            if (player == null) {
                return;
            }

            writeLine(world.intro(player));

            while (running) {
                write(world.promptFor(player));
                String input = reader.readLine();
                if (input == null) {
                    break;
                }
                logger.log("CMD", player.getName() + ": " + input);
                String response = handleInput(input);
                if (!response.isBlank()) {
                    writeLine(response);
                }
            }
        } catch (IOException e) {
            logger.log("ERROR", "SESSION " + e.getMessage());
        } finally {
            if (player != null) {
                accountStore.saveProgress(player);
                world.removePlayer(player);
            }
            logger.log("INFO", "DISCONNECT " + (player != null ? player.getName() : "unknown"));
        }
    }

    private MudPlayer loginFlow() throws IOException {
        while (true) {
            writeLine("Choose action: login | register | exit");
            write("Action: ");
            String action = reader.readLine();
            if (action == null) {
                return null;
            }

            String normalizedAction = MudWorld.normalize(action);
            if ("exit".equals(normalizedAction)) {
                return null;
            }

            if (!"login".equals(normalizedAction) && !"register".equals(normalizedAction)) {
                writeLine("Unknown action type.");
                continue;
            }

            write("Your name: ");
            String name = reader.readLine();
            if (name == null) {
                return null;
            }

            write("Password: ");
            String pass = reader.readLine();
            if (pass == null) {
                return null;
            }

            if ("register".equals(normalizedAction)) {
                if (!accountStore.register(name, pass)) {
                    writeLine("Registration failed (name already exists or password is empty).");
                    continue;
                }
                writeLine("Registration successful, continuing with login.");
            }

            PlayerAccountStore.AccountData data = accountStore.authenticate(name, pass);
            if (data == null) {
                writeLine("Incorrect login.");
                continue;
            }

            MudPlayer activePlayer = world.addPlayer(name, data);
            if (activePlayer == null) {
                writeLine("This player is already connected.");
                continue;
            }
            return activePlayer;
        }
    }

    private String handleInput(String input) {
        String trimmedInput = input == null ? "" : input.trim();
        if (trimmedInput.isEmpty()) {
            return world.isQuestionPromptActive(player)
                    ? "Please enter an answer or type 'prompter' for a hint."
                    : "Please enter a command.";
        }

        String normalizedInput = MudWorld.normalize(trimmedInput);
        if (isExitCommand(normalizedInput)) {
            running = false;
            return "Connection closing. Goodbye!";
        }

        if (world.isCometPromptActive(player)) {
            return world.addCrystalToComet(player, trimmedInput);
        }
        if (world.isQuestionPromptActive(player)) {
            return world.answerQuestion(player, trimmedInput);
        }

        String[] parts = trimmedInput.split("\\s+", 2);
        String command = MudWorld.normalize(parts[0]);
        String argument = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {
            case "help", "pomoc" -> world.helpText(player);
            case "rules" -> world.rulesText();
            case "explore", "look", "prozkoumej" -> world.describeRoom(player);
            case "fly", "jdi", "go" -> world.movePlayer(player, argument);
            case "talk", "mluv" -> world.talkToNpc(player, argument);
            case "take", "vezmi", "get" -> world.takeCrystal(player, argument);
            case "position" -> world.positionCrystals(player);
            case "show", "inventory", "inventar", "bag" -> world.showCrystals(player);
            case "hint" -> world.giveHint();
            case "prompter" -> world.prompterHint(player);
            case "check" -> world.checkCrystals(player);
            case "comet" -> world.startCometSelection(player, argument);
            case "cometplan" -> world.describeComets(player);
            case "bigbang" -> world.triggerBigBang(player);
            case "save" -> accountStore.saveProgress(player) ? "Progress saved successfully." : "Saving failed.";
            case "load" -> world.loadProgress(player, accountStore.load(player.getName()));
            default -> "Invalid command";
        };
    }

    private boolean isExitCommand(String command) {
        return "exit".equals(command) || "leave".equals(command) || "konec".equals(command) || "quit".equals(command);
    }

    private void write(String text) throws IOException {
        writer.write(text);
        writer.flush();
    }

    private void writeLine(String text) throws IOException {
        writer.write(text);
        writer.write("\r\n");
        writer.flush();
    }
}
