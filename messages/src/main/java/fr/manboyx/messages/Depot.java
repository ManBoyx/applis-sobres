package fr.manboyx.messages;

import fr.manboyx.commun.Correspondant;
import fr.manboyx.messages.logique.Conversations;
import fr.manboyx.messages.logique.Message;

import java.util.List;

/** Ce que l'appli lit et fait sur le téléphone : les SMS, leur envoi, le carnet. */
public interface Depot {
    /** Une ligne par conversation, la plus récente en tête. */
    List<Conversations.Resume> conversations();

    /** Les derniers messages d'une conversation, du plus ancien au plus récent. */
    List<Message> fil(long fil, int maximum);

    /** Envoie un SMS. Lève une exception si le téléphone refuse. */
    void envoyer(String adresse, String texte);

    /** Le nom du contact qui a ce numéro, ou une chaîne vide. */
    String nom(String adresse);

    List<Correspondant> carnet();
}
