package fr.manboyx.commun;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.function.Consumer;

/** Les petites boîtes de dialogue : confirmer, saisir un texte, choisir dans une liste, prévenir. */
public final class Dialogues {
    private Dialogues() {}

    public static AlertDialog confirmer(Activity a, CharSequence titre, CharSequence message, CharSequence oui, Runnable action) {
        return new AlertDialog.Builder(a)
                .setTitle(titre)
                .setMessage(message)
                .setPositiveButton(oui, (d, w) -> action.run())
                .setNegativeButton("Annuler", null)
                .show();
    }

    public static AlertDialog saisir(Activity a, CharSequence titre, CharSequence depart, CharSequence valider, Consumer<String> action) {
        EditText champ = Ui.champ(a, "");
        champ.setText(depart);
        champ.setSelection(champ.getText().length());
        FrameLayout cadre = new FrameLayout(a);
        Ui.marges(cadre, a, 20, 12, 20, 0);
        cadre.addView(champ);
        return new AlertDialog.Builder(a)
                .setTitle(titre)
                .setView(cadre)
                .setPositiveButton(valider, (d, w) -> action.accept(champ.getText().toString().trim()))
                .setNegativeButton("Annuler", null)
                .show();
    }

    public static AlertDialog choisir(Activity a, CharSequence titre, CharSequence[] choix, Consumer<Integer> action) {
        return new AlertDialog.Builder(a)
                .setTitle(titre)
                .setItems(choix, (d, rang) -> action.accept(rang))
                .show();
    }

    public static void dire(Activity a, CharSequence message) {
        Toast.makeText(a, message, Toast.LENGTH_LONG).show();
    }
}
