package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles one TCP client from name selection to disconnect.
 */
public class MudClientSession implements Runnable {

    private final Socket socket;
    private final MudWorld world;

    private MudPlayer player;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean running;

    public MudClientSession(Socket socket, MudWorld world) {
        this.socket = socket;
        this.world = world;
        this.running = true;
    }

    @Override
    public void run() {
        try (socket) {
            socket.setKeepAlive(true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            writeLine("Vitej v Mapa_Pohyb_MUD.");
            writeLine("Pripojil(a) ses k textovemu hernimu svetu pres TCP.");
            player = registerPlayer();
            if (player == null) {
                return;
            }

            writeLine("Ahoj, " + player.getName() + ".");
            writeLine("Pouzij 'pomoc' nebo 'help' pro seznam prikazu.");
            writeLine(world.describeRoom(player));

            while (running) {
                write("> ");
                String input = reader.readLine();
                if (input == null) {
                    break;
                }

                String response = handleCommand(input);
                if (!response.isBlank()) {
                    writeLine(response);
                }
            }
        } catch (IOException ignored) {
            // Sudden disconnect should not crash the server.
        } finally {
            world.removePlayer(player);
        }
    }

    private MudPlayer registerPlayer() throws IOException {
        while (true) {
            write("Zadej sve jmeno: ");
            String requestedName = reader.readLine();
            if (requestedName == null) {
                return null;
            }

            MudPlayer registeredPlayer = world.addPlayer(requestedName);
            if (registeredPlayer != null) {
                return registeredPlayer;
            }

            writeLine("Tohle jmeno je prazdne nebo uz ho pouziva jiny hrac. Zkus to znovu.");
        }
    }

    private String handleCommand(String input) {
        String trimmedInput = input == null ? "" : input.trim();
        if (trimmedInput.isEmpty()) {
            return "Zadej prikaz. Pouzij 'pomoc' pro napovedu.";
        }

        String[] parts = trimmedInput.split("\\s+", 2);
        String command = MudWorld.normalize(parts[0]);
        String argument = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {
            case "pomoc", "help" -> helpText();
            case "prozkoumej", "look", "rozhledni" -> world.describeRoom(player);
            case "jdi", "go", "fly" -> world.movePlayer(player, argument);
            case "vezmi", "take", "get" -> world.takeItem(player, argument);
            case "odloz", "drop" -> world.dropItem(player, argument);
            case "inventar", "inventory", "bag" -> world.describeInventory(player);
            case "mluv", "talk" -> world.talkToNpc(player, argument);
            case "konec", "quit", "leave", "exit" -> {
                running = false;
                yield "Spojeni se serverem se ukoncuje. Ahoj!";
            }
            default -> "Neznamy prikaz. Pouzij 'pomoc' pro seznam podporovanych prikazu.";
        };
    }

    private String helpText() {
        return """
                Dostupne prikazy:
                - pomoc | help
                  Zobrazi seznam prikazu a kratky popis pouziti.
                - prozkoumej | look
                  Vypise nazev mistnosti, popis, vychody, predmety, NPC a ostatni hrace.
                - jdi <mistnost> | fly <planet>
                  Presune hrace do sousedni mistnosti.
                - vezmi <predmet> | take <item>
                  Vezme predmet z aktualni mistnosti do inventare.
                - odloz <predmet> | drop <item>
                  Odlozi predmet z inventare zpet do mistnosti.
                - inventar | inventory
                  Zobrazi obsah inventare a maximalni kapacitu.
                - mluv <npc> | talk <npc>
                  Promluvi s NPC postavou v aktualni mistnosti.
                - konec | quit
                  Bezpecne ukonci spojeni se serverem.
                """;
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
