package fr.manboyx.appareil;

import android.content.Context;
import android.widget.FrameLayout;

/** Donne à l'aperçu la même forme que l'image du capteur, pour qu'il ne soit ni étiré ni écrasé. */
final class CadreApercu extends FrameLayout {
    /** Hauteur divisée par largeur, téléphone tenu debout : 4/3 pour un capteur 4:3. */
    private float forme = 4f / 3f;

    CadreApercu(Context contexte) {
        super(contexte);
    }

    void forme(int largeurCapteur, int hauteurCapteur) {
        if (largeurCapteur > 0 && hauteurCapteur > 0) {
            // Le capteur est monté couché : son grand côté est la hauteur à l'écran.
            forme = (float) Math.max(largeurCapteur, hauteurCapteur) / Math.min(largeurCapteur, hauteurCapteur);
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int largeurDemandee, int hauteurDemandee) {
        int largeur = MeasureSpec.getSize(largeurDemandee);
        int hauteur = Math.round(largeur * forme);
        int place = MeasureSpec.getSize(hauteurDemandee);
        if (MeasureSpec.getMode(hauteurDemandee) != MeasureSpec.UNSPECIFIED && hauteur > place) {
            hauteur = place;
            largeur = Math.round(hauteur / forme);
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(largeur, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(hauteur, MeasureSpec.EXACTLY));
    }
}
