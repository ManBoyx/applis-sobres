package fr.manboyx.messages.logique;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SmsTest {
    private static String repete(String motif, int fois) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < fois; i++) {
            s.append(motif);
        }
        return s.toString();
    }

    private static void verifier(int messages, int restants, String texte) {
        Sms.Compte compte = Sms.compter(texte);
        assertEquals("messages", messages, compte.messages);
        assertEquals("restants", restants, compte.restants);
    }

    @Test
    public void unTexteVideNeCouteRien() {
        verifier(0, 160, "");
        verifier(0, 160, null);
    }

    @Test
    public void jusquACentSoixanteCaracteresSimplesUnSeulSms() {
        verifier(1, 155, "Salut");
        verifier(1, 0, repete("a", 160));
    }

    @Test
    public void auDelaChaqueSmsNePorteQueCentCinquanteTrois() {
        verifier(2, 145, repete("a", 161));
        verifier(2, 0, repete("a", 306));
        verifier(3, 152, repete("a", 307));
    }

    @Test
    public void lesAccentsCourantsDuFrancaisRestentDansLAlphabetSimple() {
        verifier(1, 160 - 10, "été à où è");
    }

    @Test
    public void certainsSignesComptentDouble() {
        verifier(1, 160 - 2, "€");
        verifier(1, 160 - 4, "[]");
    }

    @Test
    public void unSeulCaractereHorsAlphabetFaitTomberLaLimiteASoixanteDix() {
        // « ê », « ç » minuscule ou un émoji obligent à changer de codage pour tout le message.
        verifier(1, 70 - 4, "fête");
        verifier(1, 0, repete("ê", 70));
        verifier(2, 134 - 71, repete("ê", 71));
    }

    @Test
    public void unEmojiCompteDeuxUnites() {
        verifier(1, 70 - 2, "😀");
    }
}
