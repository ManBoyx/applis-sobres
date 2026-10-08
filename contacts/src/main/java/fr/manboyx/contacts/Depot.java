package fr.manboyx.contacts;

import fr.manboyx.contacts.logique.Detail;
import fr.manboyx.contacts.logique.Fiche;

import java.util.List;

/** L'accès au carnet d'adresses du téléphone. */
public interface Depot {
    List<Fiche> lister();

    /** Le contact en entier, ou `null` s'il n'existe plus. */
    Detail lire(long id);

    /** Crée le contact (`id` à 0) ou le modifie. Rend son identifiant. */
    long enregistrer(Detail detail);

    void supprimer(long id);

    void marquerFavori(long id, boolean favori);
}
