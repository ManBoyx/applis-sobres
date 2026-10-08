package fr.manboyx.commun;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AffichageTest {
    @Test
    public void unNumeroFrancaisEstEcritParPaires() {
        assertEquals("06 12 34 56 78", Affichage.numero("0612345678"));
        assertEquals("06 12 34 56 78", Affichage.numero("06.12.34.56.78"));
    }

    @Test
    public void lIndicatifFrancaisEstSepare() {
        assertEquals("+33 6 12 34 56 78", Affichage.numero("+33612345678"));
    }

    @Test
    public void unNumeroEnCoursDeSaisieOuEtrangerResteTelQuel() {
        assertEquals("0612", Affichage.numero("0612"));
        assertEquals("+4915112345678", Affichage.numero("+4915112345678"));
        assertEquals("*123#", Affichage.numero("*123#"));
        assertEquals("3615", Affichage.numero("3615"));
    }

    @Test
    public void unNumeroAbsentDonneUnTexteVide() {
        assertEquals("", Affichage.numero(null));
    }
}
