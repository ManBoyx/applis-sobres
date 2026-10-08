package fr.manboyx.galerie.logique;

/**
 * La position et le grossissement d'une image dans l'écran. Au repos, l'image
 * est entière et centrée ; on peut la grossir jusqu'à `MAXIMUM` et la déplacer
 * sans jamais faire apparaître de vide là où il reste de l'image à montrer.
 */
public final class Cadrage {
    public static final float MAXIMUM = 6f;
    public static final float DOUBLE_APPUI = 2.5f;

    private final float largeurImage;
    private final float hauteurImage;
    private final float largeurVue;
    private final float hauteurVue;
    /** Échelle à laquelle l'image tient entière dans l'écran. */
    private final float base;
    private float zoom = 1f;
    private float gauche;
    private float haut;

    public Cadrage(int largeurImage, int hauteurImage, int largeurVue, int hauteurVue) {
        this.largeurImage = Math.max(1, largeurImage);
        this.hauteurImage = Math.max(1, hauteurImage);
        this.largeurVue = Math.max(1, largeurVue);
        this.hauteurVue = Math.max(1, hauteurVue);
        base = Math.min(this.largeurVue / this.largeurImage, this.hauteurVue / this.hauteurImage);
        contraindre();
    }

    /** Grossissement par rapport à l'image entière : 1 au repos. */
    public float zoom() {
        return zoom;
    }

    /** Facteur à appliquer aux points de l'image pour l'afficher. */
    public float echelle() {
        return base * zoom;
    }

    /** Position à l'écran du coin haut gauche de l'image. */
    public float gauche() {
        return gauche;
    }

    public float haut() {
        return haut;
    }

    /** Grossit ou réduit autour du point (x, y) de l'écran, qui reste sous les doigts. */
    public void zoomer(float facteur, float x, float y) {
        float nouveau = Math.max(1f, Math.min(MAXIMUM, zoom * facteur));
        float rapport = nouveau / zoom;
        gauche = x - (x - gauche) * rapport;
        haut = y - (y - haut) * rapport;
        zoom = nouveau;
        contraindre();
    }

    public void deplacer(float dx, float dy) {
        gauche += dx;
        haut += dy;
        contraindre();
    }

    /** Double appui : grossit sur le point touché, ou revient à l'image entière. */
    public void basculer(float x, float y) {
        if (auRepos()) {
            zoomer(DOUBLE_APPUI, x, y);
        } else {
            zoom = 1f;
            contraindre();
        }
    }

    /** Vrai si l'image est au repos : un glissement change alors de photo au lieu de la déplacer. */
    public boolean auRepos() {
        return zoom <= 1.01f;
    }

    /** Dans chaque sens : centrée si elle est plus petite que l'écran, sinon bord à bord au plus. */
    private void contraindre() {
        gauche = borner(gauche, largeurImage * echelle(), largeurVue);
        haut = borner(haut, hauteurImage * echelle(), hauteurVue);
    }

    private static float borner(float position, float taille, float place) {
        if (taille <= place) {
            return (place - taille) / 2f;
        }
        return Math.max(place - taille, Math.min(0f, position));
    }
}
