package fr.manboyx.appareil.logique;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Petits calculs autour d'une prise de vue : nom du fichier, zone de zoom. */
public final class Prise {
    private Prise() {}

    /** « IMG_20261008_171500.jpg » */
    public static String nom(LocalDateTime quand) {
        return "IMG_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT).format(quand) + ".jpg";
    }

    /**
     * La partie du capteur à utiliser pour ce zoom : {gauche, haut, droite, bas},
     * centrée. Le zoom est ramené entre 1 et `maximum`.
     */
    public static int[] zone(int largeurCapteur, int hauteurCapteur, float zoom, float maximum) {
        float reel = borner(zoom, maximum);
        int largeur = Math.round(largeurCapteur / reel);
        int hauteur = Math.round(hauteurCapteur / reel);
        int gauche = (largeurCapteur - largeur) / 2;
        int haut = (hauteurCapteur - hauteur) / 2;
        return new int[] {gauche, haut, gauche + largeur, haut + hauteur};
    }

    public static float borner(float zoom, float maximum) {
        return Math.max(1f, Math.min(Math.max(1f, maximum), zoom));
    }
}
