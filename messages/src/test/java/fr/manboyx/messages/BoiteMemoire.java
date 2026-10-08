package fr.manboyx.messages;

import fr.manboyx.commun.Correspondant;
import fr.manboyx.commun.Numeros;
import fr.manboyx.messages.logique.Conversations;
import fr.manboyx.messages.logique.Message;

import java.util.ArrayList;
import java.util.List;

/** Des SMS et un carnet en mémoire, pour piloter les écrans sans téléphone. */
final class BoiteMemoire implements Depot {
    final List<Message> messages = new ArrayList<>();
    final List<Correspondant> carnet = new ArrayList<>();
    /** Ce qui a été envoyé : « numéro|texte ». */
    final List<String> envois = new ArrayList<>();
    boolean envoiRefuse;
    private long prochain = 1;

    void recu(long fil, String adresse, String texte, long quand, boolean lu) {
        messages.add(new Message(prochain++, fil, adresse, texte, quand, true, lu));
    }

    void envoye(long fil, String adresse, String texte, long quand) {
        messages.add(new Message(prochain++, fil, adresse, texte, quand, false, true));
    }

    @Override
    public List<Conversations.Resume> conversations() {
        Conversations.Cumul cumul = new Conversations.Cumul();
        for (Message m : messages) {
            cumul.ajouter(m);
        }
        return cumul.resumes();
    }

    @Override
    public List<Message> fil(long fil, int maximum) {
        List<Message> sortie = new ArrayList<>();
        for (Message m : messages) {
            if (m.fil == fil) {
                sortie.add(m);
            }
        }
        sortie.sort((a, b) -> Long.compare(a.quand, b.quand));
        return sortie;
    }

    @Override
    public void envoyer(String adresse, String texte) {
        if (envoiRefuse) {
            throw new IllegalStateException("pas de réseau");
        }
        envois.add(adresse + "|" + texte);
        // Comme sur un vrai téléphone : le message envoyé rejoint sa conversation, créée au besoin.
        Conversations.Resume connue = Conversations.trouver(conversations(), adresse);
        long fil = connue != null ? connue.fil : 1000 + prochain;
        envoye(fil, adresse, texte, System.currentTimeMillis());
    }

    @Override
    public String nom(String adresse) {
        for (Correspondant c : carnet) {
            if (Numeros.memes(c.numero, adresse)) {
                return c.nom;
            }
        }
        return "";
    }

    @Override
    public List<Correspondant> carnet() {
        return carnet;
    }
}
