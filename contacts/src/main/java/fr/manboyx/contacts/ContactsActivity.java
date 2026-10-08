package fr.manboyx.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Ecran;
import fr.manboyx.contacts.logique.Detail;

import java.util.Collections;

public class ContactsActivity extends ActiviteSobre {
    private Depot depot;

    /** Le carnet d'adresses. Les tests le remplacent par un carnet en mémoire. */
    public Depot depot() {
        if (depot == null) {
            depot = new DepotAndroid(getContentResolver());
        }
        return depot;
    }

    @Override
    protected Ecran accueil() {
        return new EcranListe();
    }

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        // Une autre appli demande d'ajouter un contact (« Ajouter aux contacts » du journal d'appels, par exemple).
        Intent demande = getIntent();
        if (demande != null && Intent.ACTION_INSERT.equals(demande.getAction())) {
            String nom = demande.getStringExtra(ContactsContract.Intents.Insert.NAME);
            String numero = demande.getStringExtra(ContactsContract.Intents.Insert.PHONE);
            String adresse = demande.getStringExtra(ContactsContract.Intents.Insert.EMAIL);
            ouvrir(new EcranEdition(new Detail(0, nom, false,
                    numero == null ? Collections.emptyList() : Collections.singletonList(numero),
                    adresse == null ? Collections.emptyList() : Collections.singletonList(adresse))));
        }
    }
}
