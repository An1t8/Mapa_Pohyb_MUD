package game;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The CrystalBag class represents a collection of crystals that the player holds.
 * Crystals can be added or removed from the bag, and it allows checking if the bag is empty.
 */
public class CrystalBag implements Serializable {

    private ArrayList<Crystal> crystals;

    public CrystalBag() {
        this.crystals = new ArrayList<>();
    }

    /**
     * Adds a crystal to the crystal bag.
     * @param crystal The crystal to be added to the bag.
     */
    public void addCrystal(Crystal crystal) {
        if (crystal != null) {
            crystals.add(crystal);
        }
    }

    /**
     * Removes and returns the first crystal in the bag.
     * @return The first crystal in the bag, or null if the bag is empty.
     */
    public Crystal removeCrystal() {
        if (!crystals.isEmpty()) {
            return crystals.remove(0);
        }
        return null;
    }

    /**
     * Checks if the crystal bag is empty.
     * @return true if the bag is empty, false otherwise.
     */
    public boolean isEmpty() {
        return crystals.isEmpty();
    }

    /**
     * Retrieves the list of crystals in the bag.
     * @return The list of crystals in the bag.
     */
    public ArrayList<Crystal> getCrystals() {
        return crystals;
    }
}
