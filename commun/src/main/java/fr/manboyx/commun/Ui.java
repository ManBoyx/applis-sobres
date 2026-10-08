package fr.manboyx.commun;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Fabrique les éléments d'écran, tous au même aspect. Pas de fichier de mise en page à lire : moins de mémoire. */
public final class Ui {
    private Ui() {}

    public static final int PLEIN = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int JUSTE = ViewGroup.LayoutParams.WRAP_CONTENT;

    public static int dp(Context c, float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics()));
    }

    public static int couleur(Context c, int ressource) {
        return c.getColor(ressource);
    }

    public static LinearLayout.LayoutParams place(int largeur, int hauteur) {
        return new LinearLayout.LayoutParams(largeur, hauteur);
    }

    /** Prend toute la place restante dans le sens de la rangée ou de la colonne. */
    public static LinearLayout.LayoutParams reste(boolean horizontal) {
        return horizontal ? new LinearLayout.LayoutParams(0, JUSTE, 1) : new LinearLayout.LayoutParams(PLEIN, 0, 1);
    }

    public static LinearLayout colonne(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout rangee(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static TextView texte(Context c, CharSequence contenu, float taille, int couleur) {
        TextView t = new TextView(c);
        t.setText(contenu);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, taille);
        t.setTextColor(couleur(c, couleur));
        return t;
    }

    /** Texte courant. */
    public static TextView texte(Context c, CharSequence contenu) {
        return texte(c, contenu, 16, R.color.texte);
    }

    /** Texte d'accompagnement, plus petit et plus pâle. */
    public static TextView discret(Context c, CharSequence contenu) {
        return texte(c, contenu, 14, R.color.texte2);
    }

    /** Texte tenant sur une ligne, coupé par « … » s'il est trop long. */
    public static TextView uneLigne(TextView t) {
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    public static TextView gras(TextView t) {
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static Button bouton(Context c, CharSequence libelle, boolean principal) {
        Button b = new Button(c);
        b.setText(libelle);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setMinHeight(dp(c, 48));
        b.setMinimumHeight(dp(c, 48));
        b.setPadding(dp(c, 24), 0, dp(c, 24), 0);
        b.setStateListAnimator(null);
        b.setBackgroundResource(principal ? R.drawable.fond_bouton : R.drawable.fond_bouton_secondaire);
        b.setTextColor(couleur(c, principal ? R.color.sur_accent : R.color.texte));
        return b;
    }

    /** Petit bouton sans cadre (signe ou mot court), de la taille d'un doigt. */
    public static TextView touche(Context c, CharSequence signe, CharSequence description) {
        TextView t = texte(c, signe, 22, R.color.texte);
        t.setGravity(Gravity.CENTER);
        t.setMinWidth(dp(c, 48));
        t.setMinimumWidth(dp(c, 48));
        t.setMinHeight(dp(c, 48));
        t.setMinimumHeight(dp(c, 48));
        t.setPadding(dp(c, 8), 0, dp(c, 8), 0);
        t.setBackgroundResource(R.drawable.fond_appui);
        t.setClickable(true);
        t.setFocusable(true);
        t.setContentDescription(description);
        return t;
    }

    public static EditText champ(Context c, CharSequence indice) {
        EditText e = new EditText(c);
        e.setHint(indice);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        e.setTextColor(couleur(c, R.color.texte));
        e.setHintTextColor(couleur(c, R.color.texte2));
        e.setBackgroundResource(R.drawable.fond_champ);
        e.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        e.setMinHeight(dp(c, 48));
        e.setMinimumHeight(dp(c, 48));
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return e;
    }

    /** Trait fin de séparation. */
    public static View trait(Context c) {
        View v = new View(c);
        v.setBackgroundColor(couleur(c, R.color.trait));
        v.setLayoutParams(new LinearLayout.LayoutParams(PLEIN, Math.max(1, dp(c, 1))));
        return v;
    }

    /** Message centré pour un écran vide. */
    public static TextView vide(Context c, CharSequence message) {
        TextView t = discret(c, message);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 32), dp(c, 48), dp(c, 32), dp(c, 48));
        return t;
    }

    public static void marges(View v, Context c, int gauche, int haut, int droite, int bas) {
        v.setPadding(dp(c, gauche), dp(c, haut), dp(c, droite), dp(c, bas));
    }
}
