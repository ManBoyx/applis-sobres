package fr.manboyx.calendrier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.CalendarContract;

import fr.manboyx.calendrier.logique.Evenement;
import fr.manboyx.calendrier.logique.Fiche;
import fr.manboyx.calendrier.logique.Repetition;
import fr.manboyx.essais.FauxFournisseur;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

/** Vérifie ce que l'appli demande aux agendas d'Android, sur un faux agenda qui note tout. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DepotAndroidTest {
    private FauxFournisseur base;
    private DepotAndroid depot;

    @Before
    public void preparer() {
        base = FauxFournisseur.installer(CalendarContract.AUTHORITY);
        depot = new DepotAndroid(RuntimeEnvironment.getApplication().getContentResolver());
    }

    private static Fiche fiche() {
        Fiche f = new Fiche();
        f.agenda = 5;
        f.titre = "Dentiste";
        f.debut = 1_000_000L;
        f.fin = 4_600_000L;
        return f;
    }

    @Test
    public void lesOccurrencesSontDemandeesPourLIntervalleEtLesSeulsAgendasVisibles() {
        Uri[] demandee = new Uri[1];
        String[] condition = new String[1];
        base.lecture = (uri, colonnes, cond, arguments) -> {
            demandee[0] = uri;
            condition[0] = cond;
            MatrixCursor c = new MatrixCursor(colonnes);
            c.addRow(new Object[] {7L, "Dentiste", 1000L, 2000L, 0, 0x00123456, "Cabinet"});
            c.addRow(new Object[] {8L, null, 3000L, 4000L, 1, 0, null});
            return c;
        };
        List<Evenement> evenements = depot.entre(111, 999);
        assertEquals("content://com.android.calendar/instances/when/111/999", demandee[0].toString());
        assertEquals("visible=1", condition[0]);
        assertEquals(2, evenements.size());
        assertEquals("la couleur est rendue opaque", 0xFF123456, evenements.get(0).couleur);
        assertEquals("Cabinet", evenements.get(0).lieu);
        assertEquals("Sans titre", evenements.get(1).titre);
        assertTrue(evenements.get(1).journee);
    }

    @Test
    public void unEvenementSimpleEstCreeAvecUneHeureDeFinEtSansRegle() {
        depot.enregistrer(fiche());
        assertEquals(1, base.journal.size());
        String ligne = base.journal.get(0);
        assertTrue(ligne, ligne.startsWith("insert content://com.android.calendar/events {"));
        assertTrue(ligne, ligne.contains("calendar_id=5"));
        assertTrue(ligne, ligne.contains("dtstart=1000000"));
        assertTrue(ligne, ligne.contains("dtend=4600000"));
        assertTrue(ligne, ligne.contains("rrule=null"));
        assertTrue(ligne, ligne.contains("duration=null"));
        assertTrue(ligne, ligne.contains("allDay=0"));
        assertTrue(ligne, ligne.contains("title=Dentiste"));
    }

    @Test
    public void unEvenementRepeteEstCreeAvecUneDureeAuLieuDUneFin() {
        Fiche f = fiche();
        f.repetition = Repetition.SEMAINE;
        depot.enregistrer(f);
        String ligne = base.journal.get(0);
        assertTrue(ligne, ligne.contains("rrule=FREQ=WEEKLY"));
        assertTrue(ligne, ligne.contains("duration=P3600S"));
        assertTrue(ligne, ligne.contains("dtend=null"));
    }

    @Test
    public void uneJourneeEntiereEstNoteeEnTempsUniversel() {
        Fiche f = fiche();
        f.journee = true;
        depot.enregistrer(f);
        String ligne = base.journal.get(0);
        assertTrue(ligne, ligne.contains("allDay=1"));
        assertTrue(ligne, ligne.contains("eventTimezone=UTC"));
    }

    @Test
    public void leRappelEstRattacheALEvenementCree() {
        Fiche f = fiche();
        f.rappel = 10;
        long id = depot.enregistrer(f);
        assertEquals("identifiant donné par le faux agenda", 100, id);
        assertEquals(2, base.journal.size());
        assertEquals("insert content://com.android.calendar/reminders {event_id=100, method=1, minutes=10}", base.journal.get(1));
    }

    @Test
    public void modifierMetAJourLEvenementSansChangerSonAgendaEtRemplaceSonRappel() {
        Fiche f = fiche();
        f.id = 7;
        f.rappel = 30;
        assertEquals(7, depot.enregistrer(f));
        assertEquals(3, base.journal.size());
        assertTrue(base.journal.get(0), base.journal.get(0).startsWith("update content://com.android.calendar/events/7 {"));
        assertFalse("l'agenda d'un événement ne se change pas", base.journal.get(0).contains("calendar_id"));
        assertEquals("delete content://com.android.calendar/reminders si event_id=? [7]", base.journal.get(1));
        assertEquals("insert content://com.android.calendar/reminders {event_id=7, method=1, minutes=30}", base.journal.get(2));
    }

    @Test
    public void supprimerViseLEvenementEtLuiSeul() {
        depot.supprimer(7);
        assertEquals("delete content://com.android.calendar/events/7", base.journal.get(0));
        assertEquals(1, base.journal.size());
    }

    @Test
    public void lAgendaLocalEstCreeHorsDeToutCompteEnLigne() {
        depot.creerAgendaLocal();
        String ligne = base.journal.get(0);
        assertTrue(ligne, ligne.startsWith("insert content://com.android.calendar/calendars?"));
        assertTrue(ligne, ligne.contains("caller_is_syncadapter=true"));
        assertTrue(ligne, ligne.contains("account_type=LOCAL"));
        assertTrue(ligne, ligne.contains("calendar_displayName=Sur ce téléphone"));
    }

    @Test
    public void unEvenementRepeteEstReluAvecSaDureeEtSaRegle() {
        base.lecture = (uri, colonnes, cond, arguments) -> {
            MatrixCursor c = new MatrixCursor(colonnes);
            if (uri.toString().contains("/events/7")) {
                c.addRow(new Object[] {5L, "Piscine", 1_000_000L, null, "PT1H30M", 0, null, null, "FREQ=WEEKLY;WKST=MO"});
            } else if (uri.toString().contains("/reminders")) {
                c.addRow(new Object[] {15});
            }
            return c;
        };
        Fiche f = depot.lire(7);
        assertEquals(7, f.id);
        assertEquals(5, f.agenda);
        assertEquals(1_000_000L + 5_400_000L, f.fin);
        assertEquals(Repetition.SEMAINE, f.repetition);
        assertEquals(15, f.rappel);
        assertEquals("", f.lieu);
    }

    @Test
    public void unEvenementDisparuDonneNull() {
        assertNull(depot.lire(404));
    }
}
