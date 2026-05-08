package server;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MudGameText {
    private static final LinkedHashMap<String, String> PLANET_SUMMARIES = new LinkedHashMap<>();
    private static final Map<String, String> ROOM_DESCRIPTIONS = createRoomDescriptions();

    static {
        PLANET_SUMMARIES.put("Station", "The main station where your space journey begins.");
        PLANET_SUMMARIES.put("Colverde", "A green planet full of vegetation and oxygen.");
        PLANET_SUMMARIES.put("Luminara", "A glowing planet filled with light sources.");
        PLANET_SUMMARIES.put("Rosetta", "A planet with pink rocks and a strange atmosphere.");
        PLANET_SUMMARIES.put("Crystalia", "A planet covered in icy crystals.");
        PLANET_SUMMARIES.put("Lavatron", "A hot planet with active volcanoes.");
        PLANET_SUMMARIES.put("Aquarix", "A water world with endless oceans.");
        PLANET_SUMMARIES.put("Solaria", "A sun-scorched desert planet.");
        PLANET_SUMMARIES.put("Glacius", "An icy planet with extremely low temperatures.");
        PLANET_SUMMARIES.put("Verdania", "A planet covered in dense forests and exotic creatures.");
        PLANET_SUMMARIES.put("Mysterra", "A mysterious planet shrouded in mist and secrets.");
    }

    private MudGameText() {
    }

    public static String intro(String playerName, String currentPlanet, boolean includeEarth) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nWelcome to the Space Adventure Game called The Beginning! 🚀\n");
        sb.append("\n Your name: ").append(playerName)
                .append(" \n Astrokoala: your friend and helper! Astrokoala gives you random hints on the game and also checks whether your comets are ready for the Big bang!\n");
        sb.append("\n").append(rulesBody());
        sb.append("\nYou are currently on ").append(currentPlanet).append("\n");
        sb.append("\n🌍 Available Planets:\n");
        appendPlanetList(sb, includeEarth);
        sb.append("\n Available Commands:\n");
        sb.append("""
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
                - save              → Saves your current multiplayer progress.
                - load              → Loads your saved multiplayer progress.
                """);
        sb.append("Type a command to begin your adventure!\n");
        return sb.toString();
    }

    public static String help(boolean includeEarth) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n Available Commands:\n");
        sb.append("""
                - fly [planet]      → Travel to a new planet.
                - talk              → Interact with someone on the planet.
                - take              → Collect a crystal from the planet.
                - position          → Place a all collected crystals at the base station.
                - hint              → Astrokoala gives you a random hint of the game.
                - check             → Astrokoala checks if the crystals are placed correctly plus displays are at the BaseStation.
                                      You can use this ONLY when youre at the base station since Astrokoala doesnt travel with you.
                - rules             → Display game rules. Use this whenever you feel lost!
                - help              → Show available commands and planets you can travel to.
                - show              → Displays the crystals in your crystal bag or at the base station. You can use this whenever you want.
                - leave             → Exit the game.
                - bigbang           → Trigger the Big Bang event when two comets are ready.
                - comet             → Show all informations about the comets.
                - cometplan         → Displays comet crystal distribution.
                - save              → Saves your current multiplayer progress.
                - load              → Loads your saved multiplayer progress.
                """);

        sb.append("\n🌍 Available Planets:\n");
        appendPlanetList(sb, includeEarth);
        return sb.toString();
    }

    public static String rules() {
        return rulesBody() + "\n";
    }

    public static String stationArrival() {
        return """
                You are now on Station. Use 'position' to place all crystals from your Crystalbag on the base station.
                Your friend, Astrokoala is here, when you think your comets are ready use 'check' and Astrokoala will check if you have all the crystals!
                """;
    }

    public static String astrokoalaDialogue() {
        return """
                Astrokoala: your friend and helper! Astrokoala gives you random hints on the game and also checks whether your comets are ready for the Big bang!
                Use 'hint' for a random tip, 'check' when both comets are full, and 'bigbang' once everything is ready.
                """;
    }

    public static String earthArrival() {
        return """
                🌍 Earth has been born from your Big Bang!
                Blue oceans, white clouds, and the first signs of life now shine before you.
                """;
    }

    public static String keeperArrival(String planetName) {
        return """
                🛸 Flying to %s...

                👽 : Welcome to %s!
                If you wish to take one of our crystals, you must answer all my questions correctly.
                To answer questions, use the 'talk' command.
                If you need help with an answer, try using the 'prompter' command.
                """.formatted(planetName, planetName);
    }

    public static String roomDescription(String planetName) {
        return ROOM_DESCRIPTIONS.getOrDefault(planetName, "Silence and the blinking of onboard sensors fill this unexplored location.");
    }

    private static String rulesBody() {
        return """
                --------------- .𖥔 🪐˖ Game Rules - The Beginning: .𖥔 🪐˖ --------------------------

                Travel between planets using the command 'fly [planet name]'.
                Answer the gatekeepers questions correctly to collect a crystal. If you dont know the answer try using 'prompter'.
                Bring the crystals to the base station and place them using 'position'.
                Arrange two stacks of 5 crystals correctly.
                You can always use 'cometplan' to see your crystals arrangement in your comets!
                Use 'comet' and then type in the the name of the crystal to add crystal to the comet.
                Activate 'bigbang' and witness the Big Bang!
                You can always use 'help' to see available commands and planets you can travel to.
                """;
    }

    private static void appendPlanetList(StringBuilder sb, boolean includeEarth) {
        for (Map.Entry<String, String> entry : PLANET_SUMMARIES.entrySet()) {
            sb.append("- ").append(String.format("%-10s", entry.getKey()))
                    .append(" - ").append(entry.getValue()).append("\n");
        }
        if (includeEarth) {
            sb.append("- Earth      - A newborn blue planet created by your successful Big Bang.\n");
        }
    }

    private static Map<String, String> createRoomDescriptions() {
        LinkedHashMap<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("Station", "The main station where your space journey begins.");
        descriptions.put("Eucastarship", "A vast hangar with a starship ready for launch and blinking guidance lights.");
        descriptions.put("Colverde", "A green planet full of vegetation and oxygen.");
        descriptions.put("Luminara", "A glowing planet filled with light sources.");
        descriptions.put("Rosetta", "A planet with pink rocks and a strange atmosphere.");
        descriptions.put("Crystalia", "A planet covered in icy crystals.");
        descriptions.put("Lavatron", "A hot planet with active volcanoes.");
        descriptions.put("Aquarix", "A water world with endless oceans.");
        descriptions.put("Solaria", "A sun-scorched desert planet.");
        descriptions.put("Glacius", "An icy planet with extremely low temperatures.");
        descriptions.put("Verdania", "A planet covered in dense forests and exotic creatures.");
        descriptions.put("Mysterra", "A mysterious planet shrouded in mist and secrets.");
        descriptions.put("Earth", "A newborn blue planet created by your successful Big Bang.");
        return Map.copyOf(descriptions);
    }
}
