package fr.manboyx.commun;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.IntConsumer;

/** La barre d'onglets du bas d'un écran. */
public final class Onglets extends LinearLayout {
    private final IntConsumer choix;
    private int courant = -1;

    public Onglets(Context c, String[] noms, IntConsumer choix) {
        super(c);
        this.choix = choix;
        setOrientation(HORIZONTAL);
        setBackgroundColor(Ui.couleur(c, R.color.surface));
        for (int i = 0; i < noms.length; i++) {
            int rang = i;
            TextView onglet = Ui.texte(c, noms[i], 15, R.color.texte2);
            onglet.setGravity(Gravity.CENTER);
            onglet.setMinHeight(Ui.dp(c, 56));
            onglet.setMinimumHeight(Ui.dp(c, 56));
            onglet.setBackgroundResource(R.drawable.fond_appui);
            onglet.setClickable(true);
            onglet.setFocusable(true);
            onglet.setOnClickListener(v -> choisir(rang));
            addView(onglet, new LayoutParams(0, Ui.JUSTE, 1));
        }
    }

    public int courant() {
        return courant;
    }

    /** Met l'onglet en avant et prévient l'écran, même s'il était déjà choisi. */
    public void choisir(int rang) {
        courant = rang;
        for (int i = 0; i < getChildCount(); i++) {
            TextView onglet = (TextView) getChildAt(i);
            boolean actif = i == rang;
            onglet.setTextColor(Ui.couleur(getContext(), actif ? R.color.accent : R.color.texte2));
            onglet.setTypeface(actif ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            onglet.setSelected(actif);
        }
        choix.accept(rang);
    }
}
