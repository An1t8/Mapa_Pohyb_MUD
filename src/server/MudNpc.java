package server;

/**
 * Fixed-text NPC used by the TCP MUD server.
 */
public class MudNpc {

    private final String name;
    private final String dialogue;

    public MudNpc(String name, String dialogue) {
        this.name = name;
        this.dialogue = dialogue;
    }

    public String getName() {
        return name;
    }

    public String getDialogue() {
        return dialogue;
    }
}
