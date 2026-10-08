package fr.manboyx.galerie;

import android.app.Activity;
import android.graphics.Bitmap;

import fr.manboyx.galerie.logique.Media;

import java.util.List;

/** L'accès aux photos et vidéos du téléphone. */
public interface Depot {
    List<Media> medias();

    /** Une petite image carrée d'environ `cote` points, ou `null` si elle ne peut pas être lue. */
    Bitmap vignette(Media media, int cote);

    /** L'image réduite à la taille de l'écran (`cote` = son plus grand côté), ou `null`. */
    Bitmap image(Media media, int cote);

    /** Vrai si Android demande lui-même confirmation avant de supprimer. */
    boolean confirmeLuiMeme();

    /**
     * Supprime la photo. Rend vrai si c'est fait ; faux si Android a ouvert sa
     * propre demande de confirmation, dont la réponse arrivera à l'écran avec ce `code`.
     */
    boolean supprimer(Activity activite, Media media, int code);
}
