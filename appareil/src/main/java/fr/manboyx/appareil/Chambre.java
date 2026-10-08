package fr.manboyx.appareil;

import android.graphics.SurfaceTexture;

import fr.manboyx.appareil.logique.Flash;

/** L'appareil photo du téléphone, réduit à ce dont l'écran a besoin. */
public interface Chambre {
    interface Ecoute {
        /** L'aperçu tourne. `zoomMaximum` vaut 1 si l'appareil ne sait pas zoomer. */
        void pret(int largeurApercu, int hauteurApercu, boolean frontale, boolean flashPossible, float zoomMaximum);

        /** La photo demandée, en JPEG, et la rotation à lui appliquer pour l'afficher droite. */
        void photo(byte[] jpeg, int rotation);

        void erreur(String message);
    }

    void ecouter(Ecoute ecoute);

    /** Vrai si le téléphone a une caméra de chaque côté. */
    boolean deuxCotes();

    /** Démarre l'aperçu sur `surface`. La réponse arrive par `Ecoute.pret` ou `Ecoute.erreur`. */
    void ouvrir(SurfaceTexture surface, boolean frontale);

    /** Libère l'appareil photo : aucune autre appli ne peut s'en servir tant que ce n'est pas fait. */
    void fermer();

    void flash(Flash mode);

    void zoom(float facteur);

    /** Prend une photo, le téléphone étant tenu à `tenue` degrés (0 debout, 90, 180 ou 270). */
    void prendre(int tenue);
}
