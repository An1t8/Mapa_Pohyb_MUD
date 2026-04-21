package game;

import questions.Question;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a planet in the universe. Each planet can have connections to other planets,
 */
public class Planet implements Serializable {

    public String name;
    HashMap<String, Planet> connections;

    private Map<Question, List<String>> questions = new HashMap<>();

    public Planet(String name) {
        this.name = name;
        this.connections = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    /**
     * Establishes a two-way connection between this planet and another planet.
     * @param planetName The name of the connected planet.
     * @param targetPlanet The planet to connect to.
     */
    public void connect(String planetName, Planet targetPlanet) {
        connections.put(planetName.toLowerCase(), targetPlanet);
        targetPlanet.connections.put(this.name.toLowerCase(), this);
    }


    public Planet getConnection(String direction) {
        return connections.get(direction);
    }

    public Map<String, Planet> getConnections() {
        return Map.copyOf(connections);
    }


    public Map<Question, List<String>> getQuestions() {
        return questions;
    }


}
