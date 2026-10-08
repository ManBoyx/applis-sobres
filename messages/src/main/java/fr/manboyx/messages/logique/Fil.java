package fr.manboyx.messages.logique;

import fr.manboyx.commun.Dates;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Prépare l'affichage d'une conversation : les messages, avec un titre à chaque changement de jour. */
public final class Fil {
    private Fil() {}

    public static final class Ligne {
        /** Non nul pour un titre de journée (« Aujourd'hui », « lundi 12 octobre »…). */
        public final String jour;
        /** Non nul pour un message. */
        public final Message message;

        Ligne(String jour, Message message) {
            this.jour = jour;
            this.message = message;
        }
    }

    /** `messages` va du plus ancien au plus récent. */
    public static List<Ligne> lignes(List<Message> messages, long maintenant, ZoneId zone) {
        List<Ligne> lignes = new ArrayList<>();
        LocalDate courant = null;
        for (Message message : messages) {
            LocalDate jour = Instant.ofEpochMilli(message.quand).atZone(zone).toLocalDate();
            if (!jour.equals(courant)) {
                courant = jour;
                lignes.add(new Ligne(Dates.jour(message.quand, maintenant, zone), null));
            }
            lignes.add(new Ligne(null, message));
        }
        return lignes;
    }
}
