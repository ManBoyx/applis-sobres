package fr.manboyx.fichiers.logique;

import fr.manboyx.commun.Textes;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lit le contenu d'un dossier, trié pour l'affichage. */
public final class Dossier {
    private Dossier() {}

    public enum Tri {
        NOM, DATE, TAILLE
    }

    /**
     * Les dossiers d'abord, puis les fichiers, chacun dans l'ordre demandé
     * (nom de A à Z, date la plus récente d'abord, taille la plus grosse
     * d'abord). `caches` : montrer aussi ce dont le nom commence par un point.
     * Un dossier illisible donne une liste vide.
     */
    public static List<Entree> lister(File dossier, Tri tri, boolean caches) {
        List<Entree> entrees = new ArrayList<>();
        File[] contenu = dossier.listFiles();
        if (contenu == null) {
            return entrees;
        }
        for (File fichier : contenu) {
            if (caches || !fichier.getName().startsWith(".")) {
                entrees.add(new Entree(fichier));
            }
        }
        Comparator<Entree> parNom = Comparator.comparing((Entree e) -> Textes.cle(e.nom)).thenComparing(e -> e.nom);
        Comparator<Entree> ordre = parNom;
        if (tri == Tri.DATE) {
            ordre = Comparator.comparingLong((Entree e) -> e.modifie).reversed().thenComparing(parNom);
        } else if (tri == Tri.TAILLE) {
            ordre = Comparator.comparingLong((Entree e) -> e.taille).reversed().thenComparing(parNom);
        }
        entrees.sort(Comparator.comparing((Entree e) -> !e.dossier).thenComparing(ordre));
        return entrees;
    }

    /** « 0 o », « 12 Ko », « 3,4 Mo », « 1,25 Go ». */
    public static String taille(long octets) {
        if (octets < 1024) {
            return octets + " o";
        }
        if (octets < 1024 * 1024) {
            return octets / 1024 + " Ko";
        }
        if (octets < 1024L * 1024 * 1024) {
            return String.format(Locale.FRANCE, "%.1f Mo", octets / (1024.0 * 1024));
        }
        return String.format(Locale.FRANCE, "%.2f Go", octets / (1024.0 * 1024 * 1024));
    }

    /** L'extension en minuscules, sans le point ; vide s'il n'y en a pas. */
    public static String extension(String nom) {
        int point = nom.lastIndexOf('.');
        // Un point en tête marque un fichier caché, pas une extension.
        if (point <= 0 || point == nom.length() - 1) {
            return "";
        }
        return nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }
}
