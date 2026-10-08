package fr.manboyx.commun;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** La barre du haut d'un écran : retour éventuel, titre, boutons d'action. */
public final class Barre {
    private Barre() {}

    private static final String TITRE = "titre";

    /** Change le titre d'une barre déjà affichée. */
    public static void renommer(LinearLayout barre, CharSequence titre) {
        ((TextView) barre.findViewWithTag(TITRE)).setText(titre);
    }

    /** Réduit le titre d'une barre qui porte beaucoup de boutons. */
    public static void taille(LinearLayout barre, float sp) {
        ((TextView) barre.findViewWithTag(TITRE)).setTextSize(sp);
    }

    /** Les actions se construisent avec `Ui.touche`. Sans retour, le titre est celui de l'appli. */
    public static LinearLayout creer(ActiviteSobre a, CharSequence titre, boolean retour, View... actions) {
        LinearLayout barre = Ui.rangee(a);
        barre.setMinimumHeight(Ui.dp(a, 56));
        barre.setPadding(Ui.dp(a, retour ? 4 : 16), 0, Ui.dp(a, 4), 0);
        if (retour) {
            TextView fleche = Ui.touche(a, "←", "Retour");
            fleche.setOnClickListener(v -> a.onBackPressed());
            barre.addView(fleche);
        }
        TextView texte = Ui.uneLigne(Ui.gras(Ui.texte(a, titre, retour ? 20 : 24, R.color.texte)));
        texte.setPadding(Ui.dp(a, retour ? 4 : 0), 0, Ui.dp(a, 8), 0);
        texte.setTag(TITRE);
        barre.addView(texte, Ui.reste(true));
        for (View action : actions) {
            barre.addView(action);
        }
        return barre;
    }
}
