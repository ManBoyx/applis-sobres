package fr.manboyx.commun;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RechercheTest {
    private static final List<Correspondant> CARNET = Arrays.asList(
            new Correspondant("Zoé Martin", "06 12 34 56 78", false),
            new Correspondant("Alice Durand", "+33 7 00 00 00 01", false),
            new Correspondant("Élodie Petit", "01 23 45 67 89", true),
            new Correspondant("Alice Durand", "01 99 99 99 99", false));

    private static List<String> noms(String saisie, int maximum) {
        List<String> sortie = new ArrayList<>();
        for (Correspondant c : Recherche.filtrer(CARNET, saisie, maximum)) {
            sortie.add(c.nom + " " + c.numero);
        }
        return sortie;
    }

    @Test
    public void sansSaisieToutLeCarnetFavorisDAbordPuisParNom() {
        assertEquals(Arrays.asList(
                "Élodie Petit 01 23 45 67 89",
                "Alice Durand +33 7 00 00 00 01",
                "Alice Durand 01 99 99 99 99",
                "Zoé Martin 06 12 34 56 78"), noms("", 0));
    }

    @Test
    public void leNomSeChercheSansAccentsNiMajuscules() {
        assertEquals(Arrays.asList("Élodie Petit 01 23 45 67 89"), noms("elod", 0));
        assertEquals(2, noms("DURAND", 0).size());
    }

    @Test
    public void desChiffresTrouventLesNumerosQuiLesContiennent() {
        assertEquals(Arrays.asList("Zoé Martin 06 12 34 56 78"), noms("0612", 0));
        assertEquals(Arrays.asList("Zoé Martin 06 12 34 56 78"), noms("06 12 34", 0));
    }

    @Test
    public void unNumeroTapeSansIndicatifTrouveCeluiEnregistreAvec() {
        assertEquals(Arrays.asList("Alice Durand +33 7 00 00 00 01"), noms("0700", 0));
    }

    @Test
    public void leMaximumLimiteLeNombreDeResultats() {
        assertEquals(2, noms("", 2).size());
        assertEquals("Élodie Petit 01 23 45 67 89", noms("", 2).get(0));
    }

    @Test
    public void rienNeCorrespondDonneUneListeVide() {
        assertEquals(0, noms("xyz", 0).size());
        assertEquals(0, noms("555", 0).size());
    }
}
