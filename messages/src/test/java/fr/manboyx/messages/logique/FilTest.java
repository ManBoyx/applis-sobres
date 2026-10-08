package fr.manboyx.messages.logique;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FilTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static long le(int jour, int heure) {
        return LocalDateTime.of(2026, 10, jour, heure, 0).atZone(PARIS).toInstant().toEpochMilli();
    }

    private static Message message(String texte, long quand) {
        return new Message(quand, 1, "0612345678", texte, quand, true, true);
    }

    private static List<String> lire(List<Message> messages) {
        List<String> sortie = new ArrayList<>();
        for (Fil.Ligne ligne : Fil.lignes(messages, le(14, 18), PARIS)) {
            sortie.add(ligne.jour != null ? "[" + ligne.jour + "]" : ligne.message.texte);
        }
        return sortie;
    }

    @Test
    public void unTitreEstInsereAChaqueChangementDeJour() {
        assertEquals(
                Arrays.asList("[lundi 12 octobre]", "a", "[Hier]", "b", "c", "[Aujourd'hui]", "d"),
                lire(Arrays.asList(message("a", le(12, 9)), message("b", le(13, 8)), message("c", le(13, 23)), message("d", le(14, 7)))));
    }

    @Test
    public void uneConversationVideNeDonneRien() {
        assertEquals(Collections.emptyList(), lire(Collections.emptyList()));
    }
}
