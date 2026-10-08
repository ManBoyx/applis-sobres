package fr.manboyx.galerie;

import android.view.View;
import android.widget.LinearLayout;

import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;
import fr.manboyx.galerie.logique.Albums;
import fr.manboyx.galerie.logique.Media;

import java.util.ArrayList;
import java.util.List;

/** Les photos et vidéos d'un album. */
final class EcranAlbum extends Ecran {
    private final String album;
    private Grille grille;

    EcranAlbum(String album) {
        this.album = album;
    }

    @Override
    protected View creer() {
        GalerieActivity appli = (GalerieActivity) activite;
        LinearLayout colonne = Ui.colonne(activite);
        colonne.addView(Barre.creer(activite, album, true));
        grille = new Grille(activite, appli.vignettes(), rang -> activite.ouvrir(new EcranVue(new ArrayList<>(grille.medias()), rang)));
        colonne.addView(grille.vue, Ui.reste(false));
        return colonne;
    }

    @Override
    public void afficher() {
        Depot depot = ((GalerieActivity) activite).depot();
        Fond.lancer(() -> Albums.contenu(depot.medias(), album), (List<Media> lus) -> {
            if (lus.isEmpty()) {
                // La dernière photo de l'album vient d'être supprimée.
                activite.fermer();
                return;
            }
            grille.montrer(lus);
        });
    }
}
