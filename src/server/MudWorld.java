package server;

import game.Planet;
import game.Universe;
import questions.Question;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MudWorld {
    private static final String START_PLANET = "Station";
    private static final String EARTH = "Earth";

    private final Universe universe;
    private final Map<String, MudRoom> rooms;
    private final Map<String, MudPlayer> activePlayers;
    private final Map<String, List<Question>> questionsByPlanet;

    public MudWorld(String mapFile) {
        this.universe = new Universe();
        this.universe.loadMap(mapFile);
        this.rooms = new ConcurrentHashMap<>();
        this.activePlayers = new ConcurrentHashMap<>();
        this.questionsByPlanet = loadQuestions("res/questions.txt");
        initializeRooms();
        initializeEarthRoom();
    }

    public synchronized MudPlayer addPlayer(String requestedName, PlayerAccountStore.AccountData data) {
        String cleanedName = cleanName(requestedName);
        if (cleanedName.isBlank()) return null;
        String key = normalize(cleanedName);
        if (activePlayers.containsKey(key)) return null;
        String startPlanet = data != null && isKnownRoom(data.planet) ? data.planet : START_PLANET;
        MudPlayer player = new MudPlayer(cleanedName, startPlanet);
        if (data != null) {
            player.loadInventory(data.inventory);
            player.loadStationCrystals(data.stationCrystals);
            player.loadComets(data.cometOne, data.cometTwo);
            player.setFinished(data.finished);
        }
        activePlayers.put(key, player);
        return player;
    }

    public synchronized void removePlayer(MudPlayer player) {
        if (player != null) activePlayers.remove(normalize(player.getName()));
    }

    public String intro(MudPlayer player) {
        return MudGameText.intro(player.getName(), player.getCurrentRoomName(), player.isFinished());
    }

    public String helpText(MudPlayer player) {
        return MudGameText.help(player.isFinished());
    }

    public String rulesText() {
        return MudGameText.rules();
    }

    public String describeRoom(MudPlayer player) {
        MudRoom room = getCurrentRoom(player);
        return "You are currently on " + room.getPlanet().getName() + "\n" +
                "Description: " + room.getDescription() + "\n" +
                "NPCs here: " + formatList(room.getNpcNames()) + "\n" +
                "Available flights: " + formatList(getVisibleExitNames(player, room)) + "\n" +
                "Crystal on this planet: " + crystalOnCurrentPlanet(player) + "\n" +
                "Other players here: " + formatList(getOtherPlayersInRoom(player));
    }

    public String movePlayer(MudPlayer player, String destination) {
        if (destination == null || destination.isBlank()) return "Please specify a destination planet. 'fly [planet]'";
        if (normalize(destination).equals(normalize(player.getCurrentRoomName()))) {
            return "You are already on a " + player.getCurrentRoomName() + ".";
        }

        if (normalize(destination).equals(normalize(EARTH)) && !player.isFinished()) {
            return "Earth has not been created yet. Fill both comets and use 'bigbang'.";
        }

        MudRoom currentRoom = getCurrentRoom(player);
        Planet targetPlanet = currentRoom.getPlanet().getConnections().values().stream()
                .filter(planet -> normalize(planet.getName()).equals(normalize(destination)))
                .filter(planet -> player.isFinished() || !normalize(planet.getName()).equals(normalize(EARTH)))
                .findFirst()
                .orElse(null);
        if (targetPlanet == null) return "this planet doesn't exist in this universe";

        player.setCurrentRoomName(targetPlanet.getName());
        player.clearTransientState();

        if (START_PLANET.equalsIgnoreCase(targetPlanet.getName())) {
            return "\n🛸 Flying to Station... \n" + MudGameText.stationArrival();
        }
        if (EARTH.equalsIgnoreCase(targetPlanet.getName())) {
            return "\n" + MudGameText.earthArrival();
        }
        if (questionsByPlanet.containsKey(targetPlanet.getName())) {
            return "\n" + MudGameText.keeperArrival(targetPlanet.getName());
        }
        return "\n🛸 Flying to " + targetPlanet.getName() + "... \n" + describeRoom(player);
    }

    public String talkToNpc(MudPlayer player, String npcName) {
        String currentPlanet = player.getCurrentRoomName();
        if (START_PLANET.equalsIgnoreCase(currentPlanet)) {
            return MudGameText.astrokoalaDialogue();
        }

        List<Question> questions = questionsByPlanet.get(currentPlanet);
        if (questions == null || questions.isEmpty()) {
            String fallbackNpc = npcName == null || npcName.isBlank() ? "npc" : npcName;
            String dialogue = getCurrentRoom(player).talkToNpc(fallbackNpc);
            return dialogue == null ? "There's no one to talk to on this planet." : dialogue;
        }

        String crystalName = currentPlanet + " Crystal";
        if (player.hasCrystal(crystalName)) {
            return "You've already taken the crystal from this planet.";
        }

        if (!player.isQuestionSessionFor(currentPlanet)) {
            player.startQuestionSession(currentPlanet);
        }

        if (player.isQuestionSessionCompletedFor(currentPlanet)) {
            return "You've already answered all questions correctly! You can use the 'take' command to collect a crystal.";
        }

        Question question = getCurrentQuestion(player);
        if (question == null) {
            return "No more questions available.";
        }
        return "Question: " + question.getQuestionText();
    }

    public String answerQuestion(MudPlayer player, String answer) {
        if (!player.isQuestionSessionInProgress()) {
            return "There are no active questions to answer right now.";
        }

        if ("prompter".equals(normalize(answer))) {
            return prompterHint(player);
        }

        Question question = getCurrentQuestion(player);
        if (question == null) {
            player.clearQuestionSession();
            return "No more questions available.";
        }

        if (question.isCorrect(answer)) {
            player.advanceQuestionIndex();
            Question nextQuestion = getCurrentQuestion(player);
            if (nextQuestion == null) {
                player.completeQuestionSession();
                return "Correct! You've answered all questions correctly. You can now take a crystal using the 'take' command.";
            }
            return "Correct!\nQuestion: " + nextQuestion.getQuestionText();
        }

        return "Incorrect answer. Try again or use the 'prompter' command for hints.";
    }

    public String prompterHint(MudPlayer player) {
        if (!player.isQuestionSessionInProgress()) {
            return "No active questions to provide hints for.";
        }

        Question currentQuestion = getCurrentQuestion(player);
        if (currentQuestion == null) {
            return "No questions available at the moment.";
        }

        String correctAnswer = currentQuestion.getCorrectAnswer();
        return "Hints for the current question:\n" +
                "1. The answer has " + correctAnswer.length() + " characters.\n" +
                "2. The answer starts with the letter '" + correctAnswer.charAt(0) + "'.";
    }

    public String takeCrystal(MudPlayer player, String itemName) {
        String currentPlanet = player.getCurrentRoomName();
        List<Question> questions = questionsByPlanet.get(currentPlanet);
        if (questions == null || questions.isEmpty()) {
            return "There's no Planet Gate Keeper here to allow you to take a crystal.";
        }
        if (!player.isQuestionSessionCompletedFor(currentPlanet)) {
            return "You must correctly answer all the Planet Gate Keeper's questions before taking a crystal.";
        }

        String crystalName = currentPlanet + " Crystal";
        if (itemName != null && !itemName.isBlank() && !normalize(crystalName).equals(normalize(itemName))) {
            return "You can only take the crystal from the current planet: " + crystalName;
        }
        if (player.hasCrystal(crystalName)) {
            return "You've already taken the crystal from this planet.";
        }
        if (!player.addItem(crystalName)) {
            return "Your inventory is full. Capacity is " + player.getMaxInventoryCapacity() + " items.";
        }
        player.clearQuestionSession();
        return "You have collected a " + crystalName;
    }

    public String positionCrystals(MudPlayer player) {
        if (player.isInventoryEmpty()) {
            return "You don't have any crystals to position.";
        }
        if (!START_PLANET.equalsIgnoreCase(player.getCurrentRoomName())) {
            return "You can't place crystals here! You must be at the Base Station.";
        }

        player.moveInventoryToStation();
        return "All your crystals have been placed at the base station. \n Use 'comet' to start adding crystals to your comets";
    }

    public String describeInventory(MudPlayer player) {
        return showCrystals(player);
    }

    public String showCrystals(MudPlayer player) {
        StringBuilder sb = new StringBuilder();
        List<String> bag = player.getInventorySnapshot();
        if (bag.isEmpty()) {
            sb.append(" Your crystal bag is empty.\n");
        } else {
            sb.append("You have the following crystals in your bag:\n");
            for (String crystal : bag) {
                sb.append("- ").append(crystal).append("\n");
            }
        }

        if (START_PLANET.equalsIgnoreCase(player.getCurrentRoomName())) {
            List<String> placedCrystals = player.getStationCrystalsSnapshot();
            if (placedCrystals.isEmpty()) {
                sb.append("\n No crystals placed at the base station.");
            } else {
                sb.append("\n Crystals placed at the base station:\n");
                for (String crystal : placedCrystals) {
                    sb.append("- ").append(crystal).append("\n");
                }
                sb.append("\nUse 'comet' to start adding crystals to the comets!");
            }
        } else {
            sb.append("\n You are not at the base station, so you can only see your crystal bag.");
        }

        return sb.toString();
    }

    public String giveHint() {
        return new game.Astrokoala().giveHint();
    }

    public String startCometSelection(MudPlayer player, String crystalName) {
        if (!START_PLANET.equalsIgnoreCase(player.getCurrentRoomName())) {
            return "You can only use the comets while at the Base Station.";
        }
        if (crystalName != null && !crystalName.isBlank()) {
            return addCrystalToComet(player, crystalName);
        }
        if (player.getStationCrystalsSnapshot().isEmpty()) {
            return describeComets(player) + "\nNo crystals are currently placed at the base station.";
        }
        if (player.areBothCometsFull()) {
            return describeComets(player) + "\nBoth comets are already full!";
        }
        player.setAwaitingCometSelection(true);
        return describeComets(player);
    }

    public String addCrystalToComet(MudPlayer player, String crystalName) {
        player.setAwaitingCometSelection(false);
        if (!START_PLANET.equalsIgnoreCase(player.getCurrentRoomName())) {
            return "You can only use the comets while at the Base Station.";
        }
        String result = player.addCrystalToNextComet(crystalName);
        return result + "\n" + describeComets(player);
    }

    public String describeComets(MudPlayer player) {
        StringBuilder sb = new StringBuilder();
        sb.append("🌠 Comet crystal distribution:\n");
        sb.append("\n Comet 1:\n");
        appendCrystalList(sb, player.getCometOneSnapshot());
        sb.append("\n Comet 2:\n");
        appendCrystalList(sb, player.getCometTwoSnapshot());
        sb.append("\nuse 'comet' to add crystals to the comet.");
        return sb.toString();
    }

    public String checkCrystals(MudPlayer player) {
        if (!START_PLANET.equalsIgnoreCase(player.getCurrentRoomName())) {
            return "You cant have your comets checked while not at the Base Station! Fly to your station and have your friend Astrokoala check the placement of your crystals!\n You can use 'show' to see the crystals in your crystalBag";
        }
        if (player.areBothCometsFull()) {
            return "Both comets are ready for the Big Bang!\nUse 'bigbang' to create Earth!";
        }
        return "❗ Some crystals are still missing or misplaced. Try using 'cometPlan' to see the missing crystals!";
    }

    public String triggerBigBang(MudPlayer player) {
        if (!START_PLANET.equalsIgnoreCase(player.getCurrentRoomName())) {
            return "you can only use this command while on base station!";
        }
        if (!player.areBothCometsFull()) {
            return "Both comets are not full yet. Add more crystals to each comet.";
        }
        player.clearInventory();
        player.clearStationCrystals();
        player.setFinished(true);
        return "🌍 Big Bang has occurred! Congratulations you won the game the Earth has been created! \n Thank you for playing 'The Beginning'! You can now use 'leave' to exit the game.";
    }

    public String loadProgress(MudPlayer player, PlayerAccountStore.AccountData data) {
        if (data == null) {
            return "No saved progress was found for this player.";
        }
        player.loadInventory(data.inventory);
        player.loadStationCrystals(data.stationCrystals);
        player.loadComets(data.cometOne, data.cometTwo);
        player.setFinished(data.finished);
        player.setCurrentRoomName(isKnownRoom(data.planet) ? data.planet : START_PLANET);
        player.clearTransientState();
        return "Progress loaded successfully.\n" + describeRoom(player);
    }

    public boolean isQuestionPromptActive(MudPlayer player) {
        return player.isQuestionSessionInProgress();
    }

    public boolean isCometPromptActive(MudPlayer player) {
        return player.isAwaitingCometSelection();
    }

    public String promptFor(MudPlayer player) {
        if (player.isAwaitingCometSelection()) {
            return "Enter crystal name: [planet name] + 'crystal':  ";
        }
        if (player.isQuestionSessionInProgress()) {
            return "Your answer (or type 'prompter' for a hint): ";
        }
        return ">> ";
    }

    public static String normalize(String text) { return text == null ? "" : text.trim().replaceAll("\\s+", " ").toLowerCase(); }

    private void initializeRooms() {
        for (Planet planet : universe.getPlanets().values()) {
            List<MudNpc> npcs = new ArrayList<>();
            if (START_PLANET.equalsIgnoreCase(planet.getName())) {
                npcs.add(new MudNpc("Astrokoala", MudGameText.astrokoalaDialogue()));
            } else if (questionsByPlanet.containsKey(planet.getName())) {
                npcs.add(new MudNpc(planet.getName() + " Keeper", "Welcome to " + planet.getName() + "!"));
            }
            rooms.put(normalize(planet.getName()), new MudRoom(planet, MudGameText.roomDescription(planet.getName()), List.of(), npcs));
        }
    }

    private void initializeEarthRoom() {
        Planet station = universe.getPlanet(START_PLANET);
        if (station == null) {
            return;
        }
        Planet earth = new Planet(EARTH);
        station.connect(EARTH, earth);
        rooms.put(normalize(EARTH), new MudRoom(earth, MudGameText.roomDescription(EARTH), List.of(), List.of()));
    }

    private MudRoom getCurrentRoom(MudPlayer player) { return getRoom(player.getCurrentRoomName()); }

    private MudRoom getRoom(String roomName) {
        MudRoom room = rooms.get(normalize(roomName));
        if (room != null) {
            return room;
        }
        return rooms.get(normalize(START_PLANET));
    }

    private List<String> getOtherPlayersInRoom(MudPlayer currentPlayer) {
        String currentRoomName = currentPlayer.getCurrentRoomName();
        return activePlayers.values().stream().filter(p -> !normalize(p.getName()).equals(normalize(currentPlayer.getName())))
                .filter(p -> normalize(p.getCurrentRoomName()).equals(normalize(currentRoomName)))
                .map(MudPlayer::getName).sorted(Comparator.comparing(String::toLowerCase)).toList();
    }

    private List<String> getVisibleExitNames(MudPlayer player, MudRoom room) {
        return room.getExitNames().stream()
                .filter(exit -> player.isFinished() || !normalize(exit).equals(normalize(EARTH)))
                .toList();
    }

    private String crystalOnCurrentPlanet(MudPlayer player) {
        String currentPlanet = player.getCurrentRoomName();
        if (!questionsByPlanet.containsKey(currentPlanet)) {
            return "none";
        }
        String crystalName = currentPlanet + " Crystal";
        return player.hasCrystal(crystalName) ? "none" : crystalName;
    }

    private Question getCurrentQuestion(MudPlayer player) {
        if (!player.isQuestionSessionFor(player.getCurrentRoomName())) {
            return null;
        }
        List<Question> questions = questionsByPlanet.get(player.getCurrentRoomName());
        if (questions == null) {
            return null;
        }
        int index = player.getCurrentQuestionIndex();
        if (index < 0 || index >= questions.size()) {
            return null;
        }
        return questions.get(index);
    }

    private boolean isKnownRoom(String roomName) {
        return roomName != null && rooms.containsKey(normalize(roomName));
    }

    private Map<String, List<Question>> loadQuestions(String filename) {
        Map<String, List<Question>> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentPlanet = null;
            List<Question> currentQuestions = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("Welcome to")) {
                    if (currentPlanet != null && !currentQuestions.isEmpty()) {
                        result.put(currentPlanet, List.copyOf(currentQuestions));
                        currentQuestions.clear();
                    }
                    String[] parts = line.split(" ");
                    if (parts.length >= 3) {
                        currentPlanet = parts[2].replace(",", "").replace("!", "");
                    }
                    continue;
                }
                if (line.contains("|")) {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        currentQuestions.add(new Question(parts[0].trim(), parts[1].trim()));
                    }
                }
            }

            if (currentPlanet != null && !currentQuestions.isEmpty()) {
                result.put(currentPlanet, List.copyOf(currentQuestions));
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return Map.copyOf(result);
    }

    private void appendCrystalList(StringBuilder sb, List<String> crystals) {
        if (crystals.isEmpty()) {
            sb.append("   - Empty\n");
            return;
        }
        for (String crystal : crystals) {
            sb.append("   - ").append(crystal).append("\n");
        }
    }

    private String formatList(List<String> values) { return values == null || values.isEmpty() ? "none" : String.join(", ", values); }
    private String cleanName(String name) { return name == null ? "" : name.trim().replaceAll("\\s+", " "); }
}
