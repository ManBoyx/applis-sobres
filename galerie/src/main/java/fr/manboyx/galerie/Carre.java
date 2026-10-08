package fr.manboyx.galerie;

import android.content.Context;
import android.widget.ImageView;

/** Une image toujours aussi haute que large, pour les cases de la grille. */
final class Carre extends ImageView {
    Carre(Context contexte) {
        super(contexte);
        setScaleType(ScaleType.CENTER_CROP);
        setBackgroundColor(contexte.getColor(fr.manboyx.commun.R.color.trait));
    }

    @Override
    protected void onMeasure(int largeur, int hauteur) {
        super.onMeasure(largeur, largeur);
    }
}
