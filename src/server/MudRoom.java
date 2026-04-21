package server;

import game.Planet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One server-side room built from the existing planet map.
 */
public class MudRoom {

    private final Planet planet;
    private final String description;
    private final List<String> items;
    private final Map<String, MudNpc> npcs;

    public MudRoom(Planet planet, String description, List<String> items, List<MudNpc> npcs) {
        this.planet = planet;
        this.description = description;
        this.items = new ArrayList<>(items);
        this.npcs = new LinkedHashMap<>();
        for (MudNpc npc : npcs) {
            this.npcs.put(MudWorld.normalize(npc.getName()), npc);
        }
    }

    public Planet getPlanet() {
        return planet;
    }

    public String getDescription() {
        return description;
    }

    public synchronized List<String> getItemsSnapshot() {
        return List.copyOf(items);
    }

    public synchronized String takeItem(String requestedItem) {
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            if (MudWorld.normalize(item).equals(MudWorld.normalize(requestedItem))) {
                items.remove(i);
                return item;
            }
        }
        return null;
    }

    public synchronized void dropItem(String item) {
        items.add(item);
        items.sort(String.CASE_INSENSITIVE_ORDER);
    }

    public List<String> getNpcNames() {
        return npcs.values().stream()
                .map(MudNpc::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public String talkToNpc(String requestedNpc) {
        String normalizedRequest = MudWorld.normalize(requestedNpc);
        MudNpc exactNpc = npcs.get(normalizedRequest);
        if (exactNpc != null) {
            return exactNpc.getDialogue();
        }

        if (npcs.size() == 1 && List.of("npc", "keeper", "gatekeeper", "postava").contains(normalizedRequest)) {
            return npcs.values().iterator().next().getDialogue();
        }

        return null;
    }

    public List<String> getExitNames() {
        return planet.getConnections().values().stream()
                .map(Planet::getName)
                .distinct()
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }
}
