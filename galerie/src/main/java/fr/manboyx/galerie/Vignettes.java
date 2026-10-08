package fr.manboyx.galerie;

import android.graphics.Bitmap;
import android.util.LruCache;
import android.widget.ImageView;

import fr.manboyx.commun.Fond;
import fr.manboyx.galerie.logique.Media;

/**
 * Charge les petites images de la grille hors de l'écran et n'en garde qu'un
 * nombre limité en mémoire : faire défiler des milliers de photos ne fait pas
 * grossir l'appli.
 */
final class Vignettes {
    /** Mémoire maximale consacrée aux vignettes. */
    private static final int OCTETS = 6 * 1024 * 1024;

    private final Depot depot;
    private final int cote;
    private final LruCache<Long, Bitmap> gardees = new LruCache<Long, Bitmap>(OCTETS) {
        @Override
        protected int sizeOf(Long id, Bitmap image) {
            return image.getAllocationByteCount();
        }
    };

    Vignettes(Depot depot, int cote) {
        this.depot = depot;
        this.cote = cote;
    }

    /** Met la vignette du média dans la case, tout de suite si elle est connue, sinon dès qu'elle est lue. */
    void poser(ImageView vue, Media media) {
        Long id = media.id;
        vue.setTag(id);
        Bitmap connue = gardees.get(id);
        if (connue != null) {
            vue.setImageBitmap(connue);
            return;
        }
        vue.setImageDrawable(null);
        // Pendant un défilement rapide, la case a souvent changé de photo avant son tour : on ne lit alors rien.
        Fond.lancer(() -> id.equals(vue.getTag()) ? depot.vignette(media, cote) : null, image -> {
            if (image == null) {
                return;
            }
            gardees.put(id, image);
            if (id.equals(vue.getTag())) {
                vue.setImageBitmap(image);
            }
        });
    }
}
