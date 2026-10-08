package fr.manboyx.appareil.logique;

/** Dans quel sens enregistrer la photo, selon la façon dont le téléphone est tenu. */
public final class Orientation {
    private Orientation() {}

    /** Valeur du capteur d'inclinaison quand le téléphone est à plat : on ne sait pas. */
    public static final int INCONNUE = -1;

    /**
     * Ramène l'inclinaison mesurée (0 à 359 degrés) au quart de tour le plus
     * proche. Téléphone à plat : on garde `precedente`.
     */
    public static int arrondir(int degres, int precedente) {
        if (degres == INCONNUE) {
            return precedente;
        }
        return (degres + 45) / 90 * 90 % 360;
    }

    /**
     * La rotation à inscrire dans la photo pour qu'elle s'affiche droite.
     * `capteur` : montage du capteur dans le téléphone (90 le plus souvent) ;
     * `tenue` : 0 téléphone debout, 90 couché sur la droite, etc.
     */
    public static int photo(int capteur, int tenue, boolean frontale) {
        // La caméra frontale regarde dans l'autre sens : sa rotation s'inverse.
        int rotation = frontale ? -tenue : tenue;
        return ((capteur + rotation) % 360 + 360) % 360;
    }
}
