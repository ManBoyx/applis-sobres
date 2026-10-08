package fr.manboyx.galerie.logique;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EchantillonTest {
    @Test
    public void uneImageDejaPetiteNEstPasReduite() {
        assertEquals(1, Echantillon.pour(800, 600, 1000));
        assertEquals(1, Echantillon.pour(1999, 1500, 1000));
    }

    @Test
    public void uneGrandeImageEstReduiteSansPasserSousLaCible() {
        assertEquals(2, Echantillon.pour(2000, 1500, 1000));
        assertEquals(4, Echantillon.pour(4000, 3000, 1000));
        assertEquals(4, Echantillon.pour(7999, 6000, 1000));
        assertEquals(8, Echantillon.pour(8000, 6000, 1000));
    }

    @Test
    public void cEstLePlusGrandCoteQuiCompte() {
        assertEquals(4, Echantillon.pour(1000, 4000, 1000));
    }

    @Test
    public void desDimensionsInconnuesNeReduisentRien() {
        assertEquals(1, Echantillon.pour(0, 0, 1000));
        assertEquals(1, Echantillon.pour(4000, 3000, 0));
    }
}
