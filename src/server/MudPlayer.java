package server;

import java.util.ArrayList;
import java.util.List;

public class MudPlayer {

    private static final int MAX_INVENTORY_CAPACITY = 12;
    private static final int COMET_CAPACITY = 5;

    private final String name;
    private final List<String> inventory;
    private final List<String> stationCrystals;
    private final List<String> cometOneCrystals;
    private final List<String> cometTwoCrystals;
    private String currentRoomName;
    private boolean finished;
    private String activeQuestionPlanet;
    private int currentQuestionIndex;
    private boolean questionSessionCompleted;
    private boolean awaitingCometSelection;

    public MudPlayer(String name, String startRoomName) {
        this.name = name;
        this.currentRoomName = startRoomName;
        this.inventory = new ArrayList<>();
        this.stationCrystals = new ArrayList<>();
        this.cometOneCrystals = new ArrayList<>();
        this.cometTwoCrystals = new ArrayList<>();
        this.finished = false;
    }

    public String getName() {
        return name;
    }

    public synchronized String getCurrentRoomName() {
        return currentRoomName;
    }

    public synchronized void setCurrentRoomName(String currentRoomName) {
        this.currentRoomName = currentRoomName;
    }

    public synchronized boolean addItem(String itemName) {
        if (inventory.size() >= MAX_INVENTORY_CAPACITY) {
            return false;
        }
        inventory.add(itemName);
        return true;
    }

    public synchronized String removeItem(String requestedItem) {
        return removeByName(inventory, requestedItem);
    }

    public synchronized boolean isInventoryFull() {
        return inventory.size() >= MAX_INVENTORY_CAPACITY;
    }

    public synchronized boolean isInventoryEmpty() {
        return inventory.isEmpty();
    }

    public synchronized List<String> getInventorySnapshot() {
        return List.copyOf(inventory);
    }

    public synchronized void loadInventory(List<String> items) {
        inventory.clear();
        for (String item : items) {
            if (inventory.size() >= MAX_INVENTORY_CAPACITY) {
                break;
            }
            inventory.add(item);
        }
    }

    public synchronized List<String> getStationCrystalsSnapshot() {
        return List.copyOf(stationCrystals);
    }

    public synchronized void loadStationCrystals(List<String> items) {
        stationCrystals.clear();
        stationCrystals.addAll(items);
    }

    public synchronized List<String> getCometOneSnapshot() {
        return List.copyOf(cometOneCrystals);
    }

    public synchronized List<String> getCometTwoSnapshot() {
        return List.copyOf(cometTwoCrystals);
    }

    public synchronized void loadComets(List<String> cometOne, List<String> cometTwo) {
        cometOneCrystals.clear();
        cometTwoCrystals.clear();
        for (String crystal : cometOne) {
            if (cometOneCrystals.size() >= COMET_CAPACITY) {
                break;
            }
            cometOneCrystals.add(crystal);
        }
        for (String crystal : cometTwo) {
            if (cometTwoCrystals.size() >= COMET_CAPACITY) {
                break;
            }
            cometTwoCrystals.add(crystal);
        }
    }

    public synchronized void moveInventoryToStation() {
        while (!inventory.isEmpty()) {
            stationCrystals.add(inventory.remove(0));
        }
    }

    public synchronized void clearInventory() {
        inventory.clear();
    }

    public synchronized void clearStationCrystals() {
        stationCrystals.clear();
    }

    public synchronized boolean hasCrystal(String requestedCrystal) {
        return containsCrystal(inventory, requestedCrystal)
                || containsCrystal(stationCrystals, requestedCrystal)
                || containsCrystal(cometOneCrystals, requestedCrystal)
                || containsCrystal(cometTwoCrystals, requestedCrystal);
    }

    public synchronized String addCrystalToNextComet(String requestedCrystal) {
        String removedCrystal = removeByName(stationCrystals, requestedCrystal);
        if (removedCrystal == null) {
            return "'" + requestedCrystal + "' not found at the base station.";
        }
        if (cometOneCrystals.size() < COMET_CAPACITY) {
            cometOneCrystals.add(removedCrystal);
            return "'" + removedCrystal + "' has been added to Comet 1.";
        }
        if (cometTwoCrystals.size() < COMET_CAPACITY) {
            cometTwoCrystals.add(removedCrystal);
            return "'" + removedCrystal + "' has been added to Comet 2.";
        }
        stationCrystals.add(removedCrystal);
        return "Both comets are already full!";
    }

    public synchronized boolean areBothCometsFull() {
        return cometOneCrystals.size() == COMET_CAPACITY && cometTwoCrystals.size() == COMET_CAPACITY;
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized void setFinished(boolean finished) {
        this.finished = finished;
    }

    public synchronized void startQuestionSession(String planetName) {
        activeQuestionPlanet = planetName;
        currentQuestionIndex = 0;
        questionSessionCompleted = false;
    }

    public synchronized void clearQuestionSession() {
        activeQuestionPlanet = null;
        currentQuestionIndex = 0;
        questionSessionCompleted = false;
    }

    public synchronized boolean isQuestionSessionInProgress() {
        return activeQuestionPlanet != null && !questionSessionCompleted;
    }

    public synchronized boolean isQuestionSessionCompletedFor(String planetName) {
        return questionSessionCompleted && matchesPlanet(planetName);
    }

    public synchronized boolean isQuestionSessionFor(String planetName) {
        return activeQuestionPlanet != null && matchesPlanet(planetName);
    }

    public synchronized int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public synchronized void advanceQuestionIndex() {
        currentQuestionIndex++;
    }

    public synchronized void completeQuestionSession() {
        questionSessionCompleted = true;
    }

    public synchronized boolean isAwaitingCometSelection() {
        return awaitingCometSelection;
    }

    public synchronized void setAwaitingCometSelection(boolean awaitingCometSelection) {
        this.awaitingCometSelection = awaitingCometSelection;
    }

    public synchronized void clearTransientState() {
        clearQuestionSession();
        awaitingCometSelection = false;
    }

    public int getMaxInventoryCapacity() {
        return MAX_INVENTORY_CAPACITY;
    }

    private boolean matchesPlanet(String planetName) {
        return MudWorld.normalize(activeQuestionPlanet).equals(MudWorld.normalize(planetName));
    }

    private boolean containsCrystal(List<String> crystals, String requestedCrystal) {
        return crystals.stream().anyMatch(crystal -> MudWorld.normalize(crystal).equals(MudWorld.normalize(requestedCrystal)));
    }

    private String removeByName(List<String> source, String requestedCrystal) {
        for (int i = 0; i < source.size(); i++) {
            String crystal = source.get(i);
            if (MudWorld.normalize(crystal).equals(MudWorld.normalize(requestedCrystal))) {
                source.remove(i);
                return crystal;
            }
        }
        return null;
    }
}
