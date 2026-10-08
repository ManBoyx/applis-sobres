package fr.manboyx.telephone.logique;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JournalTest {
    private static Appel appel(String numero, int sens, long quand) {
        return new Appel(numero, "", sens, quand, 0);
    }

    @Test
    public void lesAppelsDeSuiteDuMemeNumeroDansLeMemeSensFontUneLigne() {
        List<Journal.Groupe> groupes = Journal.regrouper(Arrays.asList(
                appel("0612345678", Appel.MANQUE, 300),
                appel("+33612345678", Appel.MANQUE, 200),
                appel("06 12 34 56 78", Appel.MANQUE, 100)));
        assertEquals(1, groupes.size());
        assertEquals(3, groupes.get(0).nombre);
        assertEquals("la ligne porte la date du plus récent", 300, groupes.get(0).dernier.quand);
    }

    @Test
    public void unChangementDeSensOuDeNumeroCommenceUneNouvelleLigne() {
        List<Journal.Groupe> groupes = Journal.regrouper(Arrays.asList(
                appel("0612345678", Appel.MANQUE, 500),
                appel("0612345678", Appel.SORTANT, 400),
                appel("0700000000", Appel.SORTANT, 300),
                appel("0612345678", Appel.SORTANT, 200),
                appel("0612345678", Appel.SORTANT, 100)));
        assertEquals(4, groupes.size());
        assertEquals(1, groupes.get(0).nombre);
        assertEquals(1, groupes.get(1).nombre);
        assertEquals(1, groupes.get(2).nombre);
        assertEquals(2, groupes.get(3).nombre);
    }

    @Test
    public void lesAppelsMasquesNeSontPasReunisEntreEux() {
        // Deux appels sans numéro peuvent venir de deux personnes différentes.
        List<Journal.Groupe> groupes = Journal.regrouper(Arrays.asList(
                appel("", Appel.ENTRANT, 200), appel("", Appel.ENTRANT, 100)));
        assertEquals(2, groupes.size());
    }

    @Test
    public void unJournalVideNeDonneRien() {
        assertEquals(0, Journal.regrouper(Collections.emptyList()).size());
    }
}
