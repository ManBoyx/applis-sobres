package fr.manboyx.contacts.logique;

import fr.manboyx.commun.Textes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Range les contacts pour l'affichage : favoris en tête, puis par lettre. */
public final class Classement {
    private Classement() {}

    /** Une ligne de la liste : soit un titre de section, soit un contact. */
    public static final class Ligne {
        /** Non nul pour un titre de section. */
        public final String titre;
        /** Non nul pour un contact. */
        public final Fiche fiche;

        Ligne(String titre, Fiche fiche) {
            this.titre = titre;
            this.fiche = fiche;
        }
    }

    /**
     * Sans recherche : la section « Favoris », puis tous les contacts sous leur
     * lettre. Avec une recherche : les contacts trouvés, sans titres.
     */
    public static List<Ligne> lignes(List<Fiche> fiches, String recherche) {
        List<Fiche> tries = new ArrayList<>();
        for (Fiche fiche : fiches) {
            if (Textes.contient(fiche.nom, recherche)) {
                tries.add(fiche);
            }
        }
        tries.sort(Comparator.comparing((Fiche f) -> Textes.cle(f.nom)).thenComparingLong(f -> f.id));

        List<Ligne> lignes = new ArrayList<>();
        if (!Textes.cle(recherche).isEmpty()) {
            for (Fiche fiche : tries) {
                lignes.add(new Ligne(null, fiche));
            }
            return lignes;
        }
        boolean titreFavoris = false;
        for (Fiche fiche : tries) {
            if (fiche.favori) {
                if (!titreFavoris) {
                    lignes.add(new Ligne("Favoris", null));
                    titreFavoris = true;
                }
                lignes.add(new Ligne(null, fiche));
            }
        }
        // Les lettres d'abord, puis « # » pour tout ce qui ne commence pas par une lettre.
        for (boolean lettres : new boolean[] {true, false}) {
            String section = null;
            for (Fiche fiche : tries) {
                String initiale = Textes.initiale(fiche.nom);
                if (initiale.equals("#") == lettres) {
                    continue;
                }
                if (!initiale.equals(section)) {
                    section = initiale;
                    lignes.add(new Ligne(section, null));
                }
                lignes.add(new Ligne(null, fiche));
            }
        }
        return lignes;
    }
}
