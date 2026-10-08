package fr.manboyx.commun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextesTest {
    @Test
    public void laCleIgnoreAccentsCasseEtEspacesAutour() {
        assertEquals("elodie ca", Textes.cle("  Élodie ÇÀ "));
        assertEquals("", Textes.cle(null));
    }

    @Test
    public void laRechercheIgnoreAccentsEtCasse() {
        assertTrue(Textes.contient("Hélène Müller", "helene"));
        assertTrue(Textes.contient("Hélène Müller", "MULL"));
        assertFalse(Textes.contient("Hélène Müller", "martin"));
    }

    @Test
    public void uneRechercheVideTrouveTout() {
        assertTrue(Textes.contient("Hélène", ""));
        assertTrue(Textes.contient("Hélène", "   "));
        assertTrue(Textes.contient(null, null));
    }

    @Test
    public void lInitialeEstUneLettreSansAccentEnMajuscule() {
        assertEquals("E", Textes.initiale("élodie"));
        assertEquals("Z", Textes.initiale(" zoé"));
    }

    @Test
    public void unNomQuiNeCommencePasParUneLettreEstRangeSousDiese() {
        assertEquals("#", Textes.initiale("3615 Ulla"));
        assertEquals("#", Textes.initiale(""));
        assertEquals("#", Textes.initiale(null));
    }
}
