package fr.manboyx.messages.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class ConversationsTest {
    private static Message recu(long fil, String adresse, String texte, long quand, boolean lu) {
        return new Message(quand, fil, adresse, texte, quand, true, lu);
    }

    private static Message envoye(long fil, String adresse, String texte, long quand) {
        return new Message(quand, fil, adresse, texte, quand, false, true);
    }

    private static List<Conversations.Resume> resumer(Message... messages) {
        Conversations.Cumul cumul = new Conversations.Cumul();
        for (Message m : messages) {
            cumul.ajouter(m);
        }
        return cumul.resumes();
    }

    @Test
    public void chaqueConversationDonneUneLigneAvecSonDernierMessage() {
        List<Conversations.Resume> resumes = resumer(
                recu(1, "0612345678", "Bonjour", 100, true),
                envoye(1, "0612345678", "Salut !", 300),
                recu(2, "MaBanque", "Votre code : 1234", 200, true));
        assertEquals(2, resumes.size());
        assertEquals("la plus récente en tête", 1, resumes.get(0).fil);
        assertEquals("Salut !", resumes.get(0).extrait);
        assertTrue(resumes.get(0).envoye);
        assertEquals(300, resumes.get(0).quand);
        assertEquals("MaBanque", resumes.get(1).adresse);
        assertFalse(resumes.get(1).envoye);
    }

    @Test
    public void lOrdreDArriveeDesMessagesNeChangeRien() {
        List<Conversations.Resume> resumes = resumer(
                envoye(1, "0612345678", "Le plus récent", 300),
                recu(1, "0612345678", "Le plus ancien", 100, true));
        assertEquals("Le plus récent", resumes.get(0).extrait);
    }

    @Test
    public void lesMessagesRecusNonLusSontComptes() {
        List<Conversations.Resume> resumes = resumer(
                recu(1, "0612345678", "un", 100, false),
                recu(1, "0612345678", "deux", 200, false),
                recu(1, "0612345678", "trois", 300, true),
                envoye(1, "0612345678", "réponse", 400));
        assertEquals(2, resumes.get(0).nonLus);
    }

    @Test
    public void lExtraitTientSurUneLigne() {
        List<Conversations.Resume> resumes = resumer(recu(1, "0612345678", "ligne 1\nligne 2\r\n  ligne 3", 100, true));
        assertEquals("ligne 1 ligne 2 ligne 3", resumes.get(0).extrait);
    }

    @Test
    public void unTresLongMessageEstCoupeDansLExtrait() {
        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            long_.append("abcdefghij");
        }
        assertTrue(resumer(recu(1, "0612345678", long_.toString(), 100, true)).get(0).extrait.length() <= 120);
    }

    @Test
    public void sansNumeroDeConversationOnRegroupeParNumero() {
        List<Conversations.Resume> resumes = resumer(
                recu(0, "06 12 34 56 78", "un", 100, true),
                envoye(0, "+33612345678", "deux", 200),
                recu(0, "0700000000", "autre", 150, true));
        assertEquals(2, resumes.size());
        assertEquals("deux", resumes.get(0).extrait);
    }

    @Test
    public void onRetrouveUneConversationParSonNumeroQuelleQueSoitLEcriture() {
        List<Conversations.Resume> resumes = resumer(
                recu(7, "+33612345678", "Bonjour", 100, true),
                recu(8, "0700000000", "autre", 150, true));
        assertEquals(7, Conversations.trouver(resumes, "06 12 34 56 78").fil);
        assertNull(Conversations.trouver(resumes, "0699999999"));
        assertNull(Conversations.trouver(resumes, ""));
    }
}
