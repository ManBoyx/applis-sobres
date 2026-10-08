package fr.manboyx.messages.logique;

import fr.manboyx.commun.Numeros;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Résume les SMS en une ligne par conversation. */
public final class Conversations {
    private Conversations() {}

    public static final class Resume {
        public final long fil;
        public final String adresse;
        /** Le dernier message, sur une ligne. */
        public final String extrait;
        public final long quand;
        public final int nonLus;
        /** Vrai si le dernier message a été envoyé par l'utilisateur. */
        public final boolean envoye;

        Resume(long fil, String adresse, String extrait, long quand, int nonLus, boolean envoye) {
            this.fil = fil;
            this.adresse = adresse;
            this.extrait = extrait;
            this.quand = quand;
            this.nonLus = nonLus;
            this.envoye = envoye;
        }
    }

    /**
     * Reçoit les messages un par un, dans n'importe quel ordre, sans les
     * garder : même avec des milliers de SMS, seul un résumé par conversation
     * reste en mémoire.
     */
    public static final class Cumul {
        private static final int LONGUEUR_EXTRAIT = 120;

        private static final class Etat {
            Message dernier;
            int nonLus;
        }

        private final Map<String, Etat> etats = new HashMap<>();

        public void ajouter(Message message) {
            // Le téléphone numérote les conversations ; à défaut, on regroupe par numéro.
            String cle = message.fil != 0 ? "f" + message.fil : "n" + Numeros.cle(message.adresse);
            Etat etat = etats.get(cle);
            if (etat == null) {
                etat = new Etat();
                etats.put(cle, etat);
            }
            if (etat.dernier == null || message.quand > etat.dernier.quand) {
                etat.dernier = message;
            }
            if (message.recu && !message.lu) {
                etat.nonLus++;
            }
        }

        /** Une ligne par conversation, la plus récente en tête. */
        public List<Resume> resumes() {
            List<Resume> resumes = new ArrayList<>();
            for (Etat etat : etats.values()) {
                Message m = etat.dernier;
                String extrait = m.texte.trim().replaceAll("\\s+", " ");
                if (extrait.length() > LONGUEUR_EXTRAIT) {
                    extrait = extrait.substring(0, LONGUEUR_EXTRAIT - 1) + "…";
                }
                resumes.add(new Resume(m.fil, m.adresse, extrait, m.quand, etat.nonLus, !m.recu));
            }
            resumes.sort(Comparator.comparingLong((Resume r) -> r.quand).reversed());
            return resumes;
        }
    }

    /** La conversation tenue avec ce numéro, quelle que soit la façon de l'écrire ; `null` s'il n'y en a pas. */
    public static Resume trouver(List<Resume> resumes, String adresse) {
        for (Resume resume : resumes) {
            if (Numeros.memes(resume.adresse, adresse)) {
                return resume;
            }
        }
        return null;
    }
}
