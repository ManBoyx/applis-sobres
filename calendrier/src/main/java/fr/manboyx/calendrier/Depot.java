package fr.manboyx.calendrier;

import fr.manboyx.calendrier.logique.Evenement;
import fr.manboyx.calendrier.logique.Fiche;

import java.util.List;

/** L'accès aux agendas du téléphone. */
public interface Depot {
    final class Agenda {
        public final long id;
        public final String nom;
        public final int couleur;
        /** Faux pour un agenda en lecture seule (jours fériés, anniversaires…). */
        public final boolean modifiable;

        public Agenda(long id, String nom, int couleur, boolean modifiable) {
            this.id = id;
            this.nom = nom == null || nom.isEmpty() ? "Agenda" : nom;
            this.couleur = couleur;
            this.modifiable = modifiable;
        }
    }

    List<Agenda> agendas();

    /** Toutes les occurrences qui touchent l'intervalle, répétitions comprises. */
    List<Evenement> entre(long debut, long fin);

    /** L'événement complet, pour le modifier ; `null` s'il n'existe plus. */
    Fiche lire(long id);

    /** Crée (`id` à 0) ou modifie. Rend l'identifiant. */
    long enregistrer(Fiche fiche);

    /** Supprime l'événement, avec toutes ses occurrences s'il se répète. */
    void supprimer(long id);

    /** Crée un agenda qui reste sur le téléphone, pour qui n'en a aucun. Rend son identifiant. */
    long creerAgendaLocal();
}
