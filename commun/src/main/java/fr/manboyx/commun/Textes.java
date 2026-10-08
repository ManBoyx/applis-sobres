package fr.manboyx.commun;

import java.text.Normalizer;
import java.util.Locale;

/** Comparaison et recherche de textes sans tenir compte des accents ni de la casse. */
public final class Textes {
    private Textes() {}

    /** Le texte en minuscules, sans accents ni espaces autour : sert à trier et à chercher. */
    public static String cle(String texte) {
        if (texte == null) {
            return "";
        }
        String decompose = Normalizer.normalize(texte.trim(), Normalizer.Form.NFD);
        StringBuilder sortie = new StringBuilder(decompose.length());
        for (int i = 0; i < decompose.length(); i++) {
            char c = decompose.charAt(i);
            // Les accents, une fois détachés de leur lettre, sont des « marques » à écarter.
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                sortie.append(c);
            }
        }
        return sortie.toString().toLowerCase(Locale.ROOT);
    }

    /** Vrai si `recherche` se trouve dans `texte`, accents et casse ignorés. Une recherche vide trouve tout. */
    public static boolean contient(String texte, String recherche) {
        String cherche = cle(recherche);
        return cherche.isEmpty() || cle(texte).contains(cherche);
    }

    /** La lettre sous laquelle ranger ce nom dans une liste alphabétique, ou « # ». */
    public static String initiale(String nom) {
        String cle = cle(nom);
        if (cle.isEmpty() || !Character.isLetter(cle.charAt(0))) {
            return "#";
        }
        return cle.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
