package fr.manboyx.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.database.MatrixCursor;
import android.provider.Telephony;

import fr.manboyx.essais.FauxFournisseur;
import fr.manboyx.messages.logique.Conversations;
import fr.manboyx.messages.logique.Message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DepotAndroidTest {
    private FauxFournisseur base;
    private DepotAndroid depot;
    private String[] derniereCondition;

    @Before
    public void preparer() {
        base = FauxFournisseur.installer("sms");
        depot = new DepotAndroid(RuntimeEnvironment.getApplication());
        base.lecture = (uri, colonnes, condition, arguments) -> {
            derniereCondition = new String[] {condition, arguments == null ? null : arguments[0]};
            MatrixCursor c = new MatrixCursor(colonnes);
            c.addRow(new Object[] {1L, 7L, "0612345678", "Bonjour", 100L, Telephony.Sms.MESSAGE_TYPE_INBOX, 0});
            c.addRow(new Object[] {2L, 7L, "0612345678", "Salut", 200L, Telephony.Sms.MESSAGE_TYPE_SENT, 1});
            c.addRow(new Object[] {3L, 7L, "0612345678", "pas fini", 300L, Telephony.Sms.MESSAGE_TYPE_DRAFT, 1});
            c.addRow(new Object[] {4L, 7L, null, null, 250L, Telephony.Sms.MESSAGE_TYPE_OUTBOX, 1});
            return c;
        };
    }

    @Test
    public void lesConversationsIgnorentLesBrouillonsEtComptentLesNonLus() {
        List<Conversations.Resume> resumes = depot.conversations();
        assertEquals(1, resumes.size());
        assertEquals(7, resumes.get(0).fil);
        assertEquals(1, resumes.get(0).nonLus);
        assertEquals("un message en cours d'envoi, même vide, ne fait pas planter", 250, resumes.get(0).quand);
    }

    @Test
    public void leFilNeDemandeQueLaConversationVoulueEtDistingueRecuEtEnvoye() {
        List<Message> messages = depot.fil(7, 50);
        assertEquals("thread_id=?", derniereCondition[0]);
        assertEquals("7", derniereCondition[1]);
        assertEquals(3, messages.size());
        assertTrue(messages.get(0).recu);
        assertFalse(messages.get(0).lu);
        assertFalse(messages.get(1).recu);
    }

    @Test
    public void leFilNeGardeQueLesDerniersMessagesDemandes() {
        List<Message> messages = depot.fil(7, 2);
        assertEquals(2, messages.size());
        assertEquals("Salut", messages.get(0).texte);
    }
}
