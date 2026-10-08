package fr.manboyx.notes;

import android.content.Intent;
import android.os.Bundle;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Ecran;
import fr.manboyx.notes.logique.Carnet;

import java.io.File;

public class NotesActivity extends ActiviteSobre {
    private Carnet carnet;

    /** Les notes vivent dans le dossier privé de l'appli : aucune autre appli ne peut les lire. */
    public Carnet carnet() {
        if (carnet == null) {
            carnet = new Carnet(new File(getFilesDir(), "notes"));
        }
        return carnet;
    }

    @Override
    protected Ecran accueil() {
        return new EcranListe();
    }

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        // « Partager » un texte vers Notes depuis une autre appli : il devient une nouvelle note.
        Intent demande = getIntent();
        if (demande != null && Intent.ACTION_SEND.equals(demande.getAction())) {
            CharSequence texte = demande.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (texte != null && texte.length() > 0) {
                ouvrir(new EcranNote(0, texte.toString()));
            }
        }
    }
}
