package fr.manboyx.telephone;

import android.content.Intent;
import android.net.Uri;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Ecran;

public class TelephoneActivity extends ActiviteSobre {
    private Depot depot;

    /** Les tests le remplacent par des données en mémoire. */
    public Depot depot() {
        if (depot == null) {
            depot = new DepotAndroid(getContentResolver());
        }
        return depot;
    }

    @Override
    protected Ecran accueil() {
        // Ouverte par une autre appli sur un numéro (« tel:… ») : il est déjà composé.
        Intent demande = getIntent();
        Uri cible = demande == null ? null : demande.getData();
        String numero = cible != null && "tel".equals(cible.getScheme()) ? cible.getSchemeSpecificPart() : "";
        return new EcranPrincipal(numero);
    }
}
