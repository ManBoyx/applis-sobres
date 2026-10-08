package fr.manboyx.telephone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;

import fr.manboyx.essais.FauxFournisseur;
import fr.manboyx.telephone.logique.Appel;
import fr.manboyx.commun.Correspondant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DepotAndroidTest {
    @Test
    public void leJournalEstLuDansLaLimiteDemandeeEtChaqueTypeEstTraduit() {
        FauxFournisseur base = FauxFournisseur.installer(CallLog.AUTHORITY);
        Uri[] demandee = new Uri[1];
        base.lecture = (uri, colonnes, condition, arguments) -> {
            demandee[0] = uri;
            MatrixCursor c = new MatrixCursor(colonnes);
            c.addRow(new Object[] {"0612345678", "Zoé", CallLog.Calls.INCOMING_TYPE, 3000L, 60L});
            c.addRow(new Object[] {"0612345678", null, CallLog.Calls.OUTGOING_TYPE, 2000L, 5L});
            c.addRow(new Object[] {null, null, CallLog.Calls.MISSED_TYPE, 1000L, 0L});
            c.addRow(new Object[] {"0700000000", null, CallLog.Calls.REJECTED_TYPE, 900L, 0L});
            c.addRow(new Object[] {"0700000000", null, CallLog.Calls.BLOCKED_TYPE, 800L, 0L});
            return c;
        };
        List<Appel> appels = new DepotAndroid(RuntimeEnvironment.getApplication().getContentResolver()).journal(50);
        assertEquals("50", demandee[0].getQueryParameter("limit"));
        assertEquals(5, appels.size());
        assertEquals(Appel.ENTRANT, appels.get(0).sens);
        assertEquals("Zoé", appels.get(0).nom);
        assertEquals(60, appels.get(0).secondes);
        assertEquals(Appel.SORTANT, appels.get(1).sens);
        assertEquals("", appels.get(1).nom);
        assertEquals(Appel.MANQUE, appels.get(2).sens);
        assertEquals("", appels.get(2).numero);
        assertEquals(Appel.MANQUE, appels.get(3).sens);
        assertEquals(Appel.MANQUE, appels.get(4).sens);
    }

    @Test
    public void leCarnetNeGardeQueLesLignesAvecUnNumero() {
        FauxFournisseur base = FauxFournisseur.installer(ContactsContract.AUTHORITY);
        base.lecture = (uri, colonnes, condition, arguments) -> {
            MatrixCursor c = new MatrixCursor(colonnes);
            c.addRow(new Object[] {"Alice", "0612345678", 1});
            c.addRow(new Object[] {"Sans numéro", null, 0});
            return c;
        };
        List<Correspondant> carnet = new DepotAndroid(RuntimeEnvironment.getApplication().getContentResolver()).carnet();
        assertEquals(1, carnet.size());
        assertEquals("Alice", carnet.get(0).nom);
        assertTrue(carnet.get(0).favori);
    }
}
