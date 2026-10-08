package fr.manboyx.notes;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Ui;

import java.io.IOException;

/**
 * Une note. Pas de bouton « Enregistrer » : le texte est écrit sur le téléphone
 * dès que l'écran est quitté, que ce soit par le retour ou parce que l'appli
 * passe à l'arrière-plan.
 */
final class EcranNote extends Ecran {
    private long id;
    private final String depart;
    private EditText champ;
    private boolean lue;
    private boolean supprimee;

    /** `id` à 0 : nouvelle note, qui commence par le texte `depart`. */
    EcranNote(long id, String depart) {
        this.id = id;
        this.depart = depart;
    }

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        TextView partager = action("Partager");
        partager.setOnClickListener(v -> partager());
        TextView supprimer = action("Supprimer");
        supprimer.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.danger));
        supprimer.setOnClickListener(v -> Dialogues.confirmer(activite, "Supprimer cette note ?",
                "Elle ne pourra pas être récupérée.", "Supprimer", () -> {
                    supprimee = true;
                    if (id != 0) {
                        ((NotesActivity) activite).carnet().supprimer(id);
                    }
                    activite.fermer();
                }));
        colonne.addView(Barre.creer(activite, "Note", true, partager, supprimer));

        champ = new EditText(activite);
        champ.setHint("Écrivez ici…");
        champ.setTextSize(17);
        champ.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.texte));
        champ.setHintTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.texte2));
        champ.setBackground(null);
        champ.setGravity(Gravity.TOP | Gravity.START);
        champ.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        Ui.marges(champ, activite, 20, 8, 20, 20);
        colonne.addView(champ, Ui.reste(false));
        return colonne;
    }

    private TextView action(String libelle) {
        TextView action = Ui.touche(activite, libelle, libelle);
        action.setTextSize(15);
        action.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.accent));
        return action;
    }

    @Override
    public void afficher() {
        if (!lue) {
            lue = true;
            champ.setText(id == 0 ? depart : ((NotesActivity) activite).carnet().lire(id));
            champ.setSelection(champ.getText().length());
        }
    }

    @Override
    public void masquer() {
        if (supprimee || !lue) {
            return;
        }
        try {
            id = ((NotesActivity) activite).carnet().enregistrer(id, champ.getText().toString());
        } catch (IOException echec) {
            Dialogues.dire(activite, "La note n'a pas pu être enregistrée.");
        }
    }

    private void partager() {
        String texte = champ.getText().toString();
        if (texte.trim().isEmpty()) {
            return;
        }
        Intent envoi = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texte);
        try {
            activite.startActivity(Intent.createChooser(envoi, "Partager la note"));
        } catch (ActivityNotFoundException aucune) {
            Dialogues.dire(activite, "Aucune appli du téléphone ne peut recevoir ce texte.");
        }
    }
}
