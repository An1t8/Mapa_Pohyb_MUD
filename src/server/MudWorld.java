package server;

import game.Planet;
import game.Universe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared game world for all connected TCP clients.
 */
public class MudWorld {

    private static final String START_ROOM = "Station";

    private static final Map<String, String> ROOM_DESCRIPTIONS = Map.ofEntries(
            Map.entry("station", "Hlavni zakladna plna monitoru, map a pristavnich doku pro vsechny cestovatele."),
            Map.entry("eucastarship", "Rozlehly hangar s pripravenou hvezdnou lodi a blikajicimi navadecimi svetly."),
            Map.entry("colverde", "Zeleny svet porostly lesy a skleniky, kde vzduch voni po novem dobrodruzstvi."),
            Map.entry("luminara", "Mistnost zalita jemnym svetlem z krystalu, ktere osvetluji kazdy kout."),
            Map.entry("rosetta", "Skalnaty svet s ruzovymi utesy a tichou atmosferou plnou prachu."),
            Map.entry("crystalia", "Ledove planiny a modre odlesky delaji z tohoto mista trpytivy labyrint."),
            Map.entry("lavatron", "Vulkanicky povrch syci horkem a pod nohama vibruje aktivni magma."),
            Map.entry("aquarix", "Vodnim svetem se nese sum oceanu a vzdaleny zvuk kovovych mol."),
            Map.entry("solaria", "Suchy poustni svet paleny sluncem, kde teplo prichazi ze vsech stran."),
            Map.entry("glacius", "Mrazivy svet s ledovymi jeskynemi a praskanim zamrzlych sten."),
            Map.entry("verdania", "Husty les ukryva stare stezky, mekkou mechovou pudu a zvlastni tvory."),
            Map.entry("mysterra", "Planeta zahalena mlhou, ktera meni kazdou navstevu v male tajemstvi.")
    );

    private final Universe universe;
    private final Map<String, MudRoom> rooms;
    private final Map<String, MudPlayer> activePlayers;

    public MudWorld(String mapFile) {
        this.universe = new Universe();
        this.universe.loadMap(mapFile);
        this.rooms = new ConcurrentHashMap<>();
        this.activePlayers = new ConcurrentHashMap<>();
        initializeRooms();
    }

    public synchronized MudPlayer addPlayer(String requestedName) {
        String cleanedName = cleanName(requestedName);
        if (cleanedName.isBlank()) {
            return null;
        }

        String key = normalize(cleanedName);
        if (activePlayers.containsKey(key)) {
            return null;
        }

        MudPlayer player = new MudPlayer(cleanedName, START_ROOM);
        activePlayers.put(key, player);
        return player;
    }

    public synchronized void removePlayer(MudPlayer player) {
        if (player == null) {
            return;
        }
        activePlayers.remove(normalize(player.getName()));
    }

    public String describeRoom(MudPlayer player) {
        MudRoom room = getCurrentRoom(player);
        List<String> items = room.getItemsSnapshot();
        List<String> npcNames = room.getNpcNames();
        List<String> otherPlayers = getOtherPlayersInRoom(player);

        StringBuilder description = new StringBuilder();
        description.append("Nazev mistnosti: ").append(room.getPlanet().getName()).append("\n");
        description.append("Popis: ").append(room.getDescription()).append("\n");
        description.append("Vychody: ").append(formatList(room.getExitNames())).append("\n");
        description.append("Predmety: ").append(formatList(items)).append("\n");
        description.append("NPC: ").append(formatList(npcNames)).append("\n");
        description.append("Hraci: ").append(formatList(otherPlayers));
        return description.toString();
    }

    public String movePlayer(MudPlayer player, String destination) {
        if (destination == null || destination.isBlank()) {
            return "Pouziti: jdi <mistnost> nebo fly <planet>.";
        }

        MudRoom currentRoom = getCurrentRoom(player);
        Planet targetPlanet = currentRoom.getPlanet().getConnections().values().stream()
                .filter(planet -> normalize(planet.getName()).equals(normalize(destination)))
                .findFirst()
                .orElse(null);

        if (targetPlanet == null) {
            return "Do teto mistnosti se odsud nedostanes. Pouzij 'prozkoumej' a podivej se na vychody.";
        }

        player.setCurrentRoomName(targetPlanet.getName());
        return "Presouvas se do " + targetPlanet.getName() + ".\n\n" + describeRoom(player);
    }

    public String takeItem(MudPlayer player, String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return "Pouziti: vezmi <predmet>.";
        }

        if (player.isInventoryFull()) {
            return "Inventar je plny. Kapacita je " + player.getMaxInventoryCapacity() + " predmetu.";
        }

        MudRoom room = getCurrentRoom(player);
        String item = room.takeItem(itemName);
        if (item == null) {
            return "Tento predmet tady neni.";
        }

        if (!player.addItem(item)) {
            room.dropItem(item);
            return "Inventar je plny. Kapacita je " + player.getMaxInventoryCapacity() + " predmetu.";
        }

        return "Sebral(a) jsi predmet: " + item + ".";
    }

    public String dropItem(MudPlayer player, String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return "Pouziti: odloz <predmet>.";
        }

        String removedItem = player.removeItem(itemName);
        if (removedItem == null) {
            return "Takovy predmet v inventari nemas.";
        }

        getCurrentRoom(player).dropItem(removedItem);
        return "Odlozil(a) jsi predmet: " + removedItem + ".";
    }

    public String describeInventory(MudPlayer player) {
        List<String> inventory = player.getInventorySnapshot();
        StringBuilder response = new StringBuilder();
        response.append("Inventar (").append(inventory.size()).append("/")
                .append(player.getMaxInventoryCapacity()).append("):");
        if (inventory.isEmpty()) {
            response.append("\n- prazdny");
        } else {
            for (String item : inventory) {
                response.append("\n- ").append(item);
            }
        }
        return response.toString();
    }

    public String talkToNpc(MudPlayer player, String npcName) {
        if (npcName == null || npcName.isBlank()) {
            return "Pouziti: mluv <jmeno NPC>.";
        }

        String dialogue = getCurrentRoom(player).talkToNpc(npcName);
        if (dialogue == null) {
            return "Takova postava tu neni.";
        }
        return dialogue;
    }

    public static String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private void initializeRooms() {
        for (Planet planet : universe.getPlanets().values()) {
            rooms.put(normalize(planet.getName()), createRoom(planet));
        }
    }

    private MudRoom createRoom(Planet planet) {
        String normalizedPlanetName = normalize(planet.getName());
        String description = ROOM_DESCRIPTIONS.getOrDefault(
                normalizedPlanetName,
                "Neprobadanou lokaci naplnuje ticho a blikani palubnich senzoru."
        );

        List<String> items = new ArrayList<>();
        List<MudNpc> npcs = new ArrayList<>();

        if ("station".equals(normalizedPlanetName)) {
            npcs.add(new MudNpc(
                    "Astrokoala",
                    "Astrokoala rika: Vitej na stanici. Pouzij 'prozkoumej', 'inventar' a 'jdi <mistnost>' pro dalsi cestu."
            ));
        } else {
            items.add(planet.getName() + " Crystal");
            npcs.add(new MudNpc(
                    planet.getName() + " Keeper",
                    "The " + planet.getName() + " Keeper says: I guard this world and watch over its crystal. Explore freely and take what you find."
            ));
        }

        return new MudRoom(planet, description, items, npcs);
    }

    private MudRoom getCurrentRoom(MudPlayer player) {
        return getRoom(player.getCurrentRoomName());
    }

    private MudRoom getRoom(String roomName) {
        MudRoom room = rooms.get(normalize(roomName));
        if (room == null) {
            throw new IllegalStateException("Room not found: " + roomName);
        }
        return room;
    }

    private List<String> getOtherPlayersInRoom(MudPlayer currentPlayer) {
        String currentRoomName = currentPlayer.getCurrentRoomName();

        return activePlayers.values().stream()
                .filter(player -> !normalize(player.getName()).equals(normalize(currentPlayer.getName())))
                .filter(player -> normalize(player.getCurrentRoomName()).equals(normalize(currentRoomName)))
                .map(MudPlayer::getName)
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }

    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "zadne";
        }
        return String.join(", ", values);
    }

    private String cleanName(String name) {
        if (name == null) {
            return "";
        }

        String cleanedName = name.trim().replaceAll("\\s+", " ");
        if (cleanedName.length() > 20) {
            cleanedName = cleanedName.substring(0, 20).trim();
        }
        return cleanedName;
    }
}
