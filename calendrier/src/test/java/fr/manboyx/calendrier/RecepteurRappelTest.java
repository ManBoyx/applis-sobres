package fr.manboyx.calendrier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.MatrixCursor;
import android.provider.CalendarContract;

import fr.manboyx.essais.FauxFournisseur;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RecepteurRappelTest {
    private Application appli;
    private FauxFournisseur base;

    @Before
    public void preparer() {
        appli = RuntimeEnvironment.getApplication();
        base = FauxFournisseur.installer(CalendarContract.AUTHORITY);
        base.lecture = (uri, colonnes, condition, arguments) -> {
            MatrixCursor c = new MatrixCursor(colonnes);
            c.addRow(new Object[] {1L, 42L, "Dentiste", System.currentTimeMillis() + 600_000, System.currentTimeMillis() + 4_200_000, 0, "Cabinet"});
            return c;
        };
    }

    private List<Notification> affichees() {
        return shadowOf(appli.getSystemService(NotificationManager.class)).getAllNotifications();
    }

    @Test
    public void unRappelEchuEstAfficheUneFoisPuisMarqueCommeMontre() {
        shadowOf(appli).grantPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
        new RecepteurRappel().onReceive(appli, new Intent("android.intent.action.EVENT_REMINDER"));
        assertEquals(1, affichees().size());
        assertEquals("Dentiste", shadowOf(affichees().get(0)).getContentTitle().toString());
        assertTrue(shadowOf(affichees().get(0)).getContentText().toString().endsWith("· Cabinet"));
        assertEquals(1, base.journal.size());
        assertTrue(base.journal.get(0), base.journal.get(0).startsWith("update content://com.android.calendar/calendar_alerts {state=1} si state=? AND alarmTime<=?"));
    }

    @Test
    public void sansAccesAuxAgendasRienNEstLuNiAffiche() {
        shadowOf(appli).denyPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
        new RecepteurRappel().onReceive(appli, new Intent("android.intent.action.EVENT_REMINDER"));
        assertEquals(0, affichees().size());
        assertEquals(0, base.journal.size());
    }
}
