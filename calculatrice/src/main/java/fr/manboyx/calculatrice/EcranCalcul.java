package fr.manboyx.calculatrice;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.calculatrice.logique.Calcul;
import fr.manboyx.calculatrice.logique.Saisie;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Ui;

import java.util.ArrayList;
import java.util.List;

/** L'écran de la calculatrice : derniers calculs en haut, expression, touches. */
final class EcranCalcul extends Ecran {
    /** Au-delà, les plus anciens calculs sont oubliés. */
    private static final int HISTORIQUE = 50;
    private static final String[][] TOUCHES = {
            {Saisie.VIDER, Saisie.PARENTHESES, "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "−"},
            {"1", "2", "3", "+"},
            {"0", ",", Saisie.EFFACER, Saisie.EGAL}};

    private final Saisie saisie = new Saisie();
    private final List<String> historique = new ArrayList<>();
    private final Lignes adaptateur = new Lignes();
    private TextView expression;
    private TextView apercu;
    private ListView liste;

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);

        liste = new ListView(activite);
        liste.setDivider(null);
        liste.setStackFromBottom(true);
        liste.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        liste.setAdapter(adaptateur);
        // Appuyer sur un ancien calcul reprend son résultat.
        liste.setOnItemClickListener((parent, vue, rang, id) -> {
            String ligne = historique.get(rang);
            saisie.reprendre(ligne.substring(ligne.lastIndexOf(" = ") + 3));
            ecrire("");
        });
        colonne.addView(liste, Ui.reste(false));

        expression = Ui.texte(activite, "", 36, fr.manboyx.commun.R.color.texte);
        expression.setGravity(Gravity.END);
        expression.setSingleLine(true);
        // Une longue expression garde sa fin visible : c'est là que l'on tape.
        expression.setEllipsize(TextUtils.TruncateAt.START);
        expression.setContentDescription("Expression");
        Ui.marges(expression, activite, 20, 8, 20, 0);
        colonne.addView(expression, Ui.place(Ui.PLEIN, Ui.JUSTE));

        apercu = Ui.texte(activite, "", 20, fr.manboyx.commun.R.color.texte2);
        apercu.setGravity(Gravity.END);
        apercu.setSingleLine(true);
        apercu.setMinHeight(Ui.dp(activite, 32));
        apercu.setContentDescription("Aperçu du résultat");
        Ui.marges(apercu, activite, 20, 0, 20, 8);
        colonne.addView(apercu, Ui.place(Ui.PLEIN, Ui.JUSTE));
        colonne.addView(Ui.trait(activite));

        LinearLayout clavier = Ui.colonne(activite);
        Ui.marges(clavier, activite, 8, 8, 8, 8);
        for (String[] rang : TOUCHES) {
            LinearLayout rangee = Ui.rangee(activite);
            for (String signe : rang) {
                TextView touche = Ui.touche(activite, signe, "Touche " + signe);
                touche.setTextSize(26);
                boolean chiffre = signe.length() == 1 && (Character.isDigit(signe.charAt(0)) || signe.equals(","));
                if (signe.equals(Saisie.EGAL)) {
                    touche.setBackgroundResource(fr.manboyx.commun.R.drawable.fond_bouton);
                    touche.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.sur_accent));
                } else if (!chiffre) {
                    touche.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.accent));
                }
                touche.setOnClickListener(v -> taper(signe));
                if (signe.equals(Saisie.EFFACER)) {
                    touche.setOnLongClickListener(v -> {
                        taper(Saisie.VIDER);
                        return true;
                    });
                }
                LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(0, Ui.dp(activite, 62), 1);
                int marge = Ui.dp(activite, 3);
                place.setMargins(marge, marge, marge, marge);
                rangee.addView(touche, place);
            }
            clavier.addView(rangee);
        }
        colonne.addView(clavier);
        ecrire("");
        return colonne;
    }

    private void taper(String touche) {
        try {
            String ligne = saisie.taper(touche);
            if (ligne != null) {
                historique.add(ligne);
                if (historique.size() > HISTORIQUE) {
                    historique.remove(0);
                }
                adaptateur.notifyDataSetChanged();
            }
            ecrire("");
        } catch (Calcul.Erreur erreur) {
            ecrire(erreur.getMessage());
        }
    }

    /** Met l'écran à jour. `erreur` remplace l'aperçu quand le calcul demandé est impossible. */
    private void ecrire(String erreur) {
        String texte = saisie.expression();
        expression.setText(texte.isEmpty() ? "0" : texte);
        expression.setTextColor(Ui.couleur(activite, texte.isEmpty() ? fr.manboyx.commun.R.color.texte2 : fr.manboyx.commun.R.color.texte));
        boolean enErreur = !erreur.isEmpty();
        String avance = saisie.apercu();
        apercu.setText(enErreur ? erreur : avance.isEmpty() ? "" : "= " + avance);
        apercu.setTextColor(Ui.couleur(activite, enErreur ? fr.manboyx.commun.R.color.danger : fr.manboyx.commun.R.color.texte2));
    }

    private final class Lignes extends BaseAdapter {
        @Override
        public int getCount() {
            return historique.size();
        }

        @Override
        public Object getItem(int rang) {
            return historique.get(rang);
        }

        @Override
        public long getItemId(int rang) {
            return rang;
        }

        @Override
        public View getView(int rang, View recyclee, ViewGroup parent) {
            TextView ligne = (TextView) recyclee;
            if (ligne == null) {
                ligne = Ui.texte(activite, "", 16, fr.manboyx.commun.R.color.texte2);
                ligne.setGravity(Gravity.END);
                ligne.setMinHeight(Ui.dp(activite, 40));
                Ui.marges(ligne, activite, 20, 8, 20, 8);
            }
            ligne.setText(historique.get(rang));
            return ligne;
        }
    }
}
