package fr.manboyx.appareil.logique;

import java.util.List;

/** Une taille d'image proposée par l'appareil photo, et le choix de la meilleure. */
public final class Taille {
    public final int largeur;
    public final int hauteur;

    public Taille(int largeur, int hauteur) {
        this.largeur = largeur;
        this.hauteur = hauteur;
    }

    /** La plus grande taille au format 4:3 ; à défaut, la plus grande tout court. `null` si la liste est vide. */
    public static Taille photo(List<Taille> tailles) {
        Taille quatreTiers = plusGrande(tailles, new Taille(4, 3), Integer.MAX_VALUE);
        return quatreTiers != null ? quatreTiers : plusGrande(tailles, null, Integer.MAX_VALUE);
    }

    /**
     * Pour l'aperçu à l'écran : la plus grande taille de même format que
     * `photo` qui ne dépasse pas `largeurMaximum` points de large. Inutile
     * d'afficher plus de points que l'écran n'en a.
     */
    public static Taille apercu(List<Taille> tailles, Taille photo, int largeurMaximum) {
        // Par ordre de préférence : même format et assez petit, même format, assez petit, n'importe lequel.
        Taille choix = plusGrande(tailles, photo, largeurMaximum);
        if (choix == null) {
            choix = plusPetite(tailles, photo);
        }
        if (choix == null) {
            choix = plusGrande(tailles, null, largeurMaximum);
        }
        return choix != null ? choix : plusPetite(tailles, null);
    }

    private boolean memeFormat(Taille autre) {
        // À 1 % près : les capteurs annoncent des tailles comme 4000 × 2992.
        long ecart = Math.abs((long) largeur * autre.hauteur - (long) hauteur * autre.largeur);
        return ecart * 100 <= (long) largeur * autre.hauteur;
    }

    private long surface() {
        return (long) largeur * hauteur;
    }

    private static Taille plusGrande(List<Taille> tailles, Taille format, int largeurMaximum) {
        Taille choix = null;
        for (Taille t : tailles) {
            boolean convient = t.largeur <= largeurMaximum && (format == null || t.memeFormat(format));
            if (convient && (choix == null || t.surface() > choix.surface())) {
                choix = t;
            }
        }
        return choix;
    }

    private static Taille plusPetite(List<Taille> tailles, Taille format) {
        Taille choix = null;
        for (Taille t : tailles) {
            boolean convient = format == null || t.memeFormat(format);
            if (convient && (choix == null || t.surface() < choix.surface())) {
                choix = t;
            }
        }
        return choix;
    }

    @Override
    public String toString() {
        return largeur + "×" + hauteur;
    }
}
