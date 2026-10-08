package fr.manboyx.messages;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Ecran;

public class MessagesActivity extends ActiviteSobre {
    private Depot depot;

    /** Les tests le remplacent par des données en mémoire. */
    public Depot depot() {
        if (depot == null) {
            depot = new DepotAndroid(this);
        }
        return depot;
    }

    @Override
    protected Ecran accueil() {
        return new EcranConversations();
    }

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        // « Envoyer un message » depuis une autre appli : on ouvre directement la conversation.
        Intent demande = getIntent();
        Uri cible = demande == null ? null : demande.getData();
        if (cible != null && ("sms".equals(cible.getScheme()) || "smsto".equals(cible.getScheme()))) {
            String adresse = cible.getSchemeSpecificPart();
            int question = adresse.indexOf('?');
            if (question >= 0) {
                adresse = adresse.substring(0, question);
            }
            String texte = demande.getStringExtra("sms_body");
            if (!adresse.trim().isEmpty()) {
                ouvrir(new EcranFil(0, adresse.trim(), "", texte == null ? "" : texte));
            }
        }
    }
}
