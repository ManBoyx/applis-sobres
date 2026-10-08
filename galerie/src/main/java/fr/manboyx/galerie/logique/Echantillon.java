package fr.manboyx.galerie.logique;

/** Choisit à quel point réduire une image en la lisant, pour ne pas charger plus de points que l'écran n'en montre. */
public final class Echantillon {
    private Echantillon() {}

    /**
     * Le diviseur (1, 2, 4, 8…) le plus grand qui laisse encore le plus grand
     * côté de l'image au moins égal à `cible`.
     */
    public static int pour(int largeur, int hauteur, int cible) {
        int diviseur = 1;
        int grandCote = Math.max(largeur, hauteur);
        if (cible <= 0) {
            return diviseur;
        }
        while (grandCote / (diviseur * 2) >= cible) {
            diviseur *= 2;
        }
        return diviseur;
    }
}
