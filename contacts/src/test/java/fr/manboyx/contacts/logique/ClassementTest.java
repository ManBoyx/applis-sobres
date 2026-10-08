package fr.manboyx.contacts.logique;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassementTest {
    private static Fiche fiche(long id, String nom, boolean favori) {
        return new Fiche(id, nom, favori);
    }

    /** Écrit les lignes sous une forme lisible : « [Titre] » ou le nom du contact. */
    private static List<String> lire(List<Fiche> fiches, String recherche) {
        List<String> sortie = new ArrayList<>();
        for (Classement.Ligne ligne : Classement.lignes(fiches, recherche)) {
            sortie.add(ligne.titre != null ? "[" + ligne.titre + "]" : ligne.fiche.nom);
        }
        return sortie;
    }

    private static final List<Fiche> CARNET = Arrays.asList(
            fiche(1, "Zoé Martin", false),
            fiche(2, "élodie Durand", true),
            fiche(3, "Bruno", false),
            fiche(4, "Émile", false),
            fiche(5, "3615 Service", false),
            fiche(6, "bernard", true));

    @Test
    public void lesContactsSontRangesParLettreSansTenirCompteDesAccents() {
        assertEquals(
                Arrays.asList("[Favoris]", "bernard", "élodie Durand",
                        "[B]", "bernard", "Bruno", "[E]", "élodie Durand", "Émile", "[Z]", "Zoé Martin",
                        "[#]", "3615 Service"),
                lire(CARNET, ""));
    }

    @Test
    public void sansFavoriIlNYAPasDeSectionFavoris() {
        assertEquals(Arrays.asList("[A]", "Alice"), lire(Arrays.asList(fiche(1, "Alice", false)), null));
    }

    @Test
    public void unCarnetVideNeDonneAucuneLigne() {
        assertEquals(Arrays.asList(), lire(new ArrayList<>(), ""));
    }

    @Test
    public void laRechercheDonneLesContactsTrouvesSansTitres() {
        assertEquals(Arrays.asList("élodie Durand"), lire(CARNET, "ELO"));
        assertEquals(Arrays.asList("bernard", "Bruno"), lire(CARNET, "b"));
        assertEquals(Arrays.asList("Zoé Martin"), lire(CARNET, "zoe"));
        assertEquals(Arrays.asList(), lire(CARNET, "inconnu"));
    }

    @Test
    public void deuxContactsDeMemeNomGardentToujoursLeMemeOrdre() {
        List<Fiche> fiches = Arrays.asList(fiche(9, "Paul", false), fiche(2, "Paul", false));
        List<Classement.Ligne> lignes = Classement.lignes(fiches, "");
        assertEquals(2, lignes.get(1).fiche.id);
        assertEquals(9, lignes.get(2).fiche.id);
    }
}
