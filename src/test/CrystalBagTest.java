package test;

import game.Crystal;
import game.CrystalBag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for verifying the functionality of the game.CrystalBag class.
 */
public class CrystalBagTest {
    private CrystalBag crystalBag;

    /**
     * Initializes the game.CrystalBag before each test.
     */
    @BeforeEach
    void init() {
        crystalBag = new CrystalBag();
        System.out.println("game.CrystalBag initialized");
    }


    /**
     * Tests adding a crystal to the game.CrystalBag.
     */
    @Test
    void addCrystal() {
        System.out.println("\n>> TEST: addCrystal");

        game.Crystal crystal = new game.Crystal("Rosetta");
        System.out.println("Adding a new crystal do the crystalbag");
        crystalBag.addCrystal(crystal);
        System.out.println("Number of crystals in crystalBag: " + crystalBag.getCrystals().size());
        Assertions.assertEquals(1, crystalBag.getCrystals().size());
        System.out.println("Name of the first crystal: " + crystalBag.getCrystals().get(0).getName());
        Assertions.assertEquals("Rosetta", crystalBag.getCrystals().get(0).getName());
        System.out.println("test finished successfully");
    }

    /**
     * Tests removing a crystal from the game.CrystalBag.
     */
    @Test
    void removeCrystal() {
        System.out.println("\n>> TEST: removeCrystal");
        Crystal crystal = new Crystal("Luminara crystal");
        System.out.println("Adding a new crystal do the crystalbag");

        crystalBag.addCrystal(crystal);
        System.out.println("Removing crystal");
        Crystal removedCrystal = crystalBag.removeCrystal();
        System.out.println("the name of the removed crystal: " + removedCrystal.getName());
        Assertions.assertEquals("Luminara crystal", removedCrystal.getName());
        System.out.println("checking whether the crystalBag is empty");
        Assertions.assertTrue(crystalBag.isEmpty());
        System.out.println("test finished successfully");

    }

    /**
     * Tests whether the isEmpty() method correctly detects an empty game.CrystalBag.
     */
    @Test
    void isEmpty() {
        System.out.println("\n>> TEST: isEmpty" );
        System.out.println("checking if the bag is empty");
        Assertions.assertTrue(crystalBag.isEmpty());
        System.out.println("creating a new crystal named 'Rosetta'");
        crystalBag.addCrystal(new Crystal("Rosetta"));
        System.out.println("checking whether bag with the crystal is empty");
        Assertions.assertFalse(crystalBag.isEmpty());
        System.out.println("test finished successfully");

    }
}
