package fr.manboyx.calendrier;

import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.view.View;

import fr.manboyx.commun.Fond;
import fr.manboyx.essais.Essai;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.time.LocalDate;

/** Écrit des images des écrans. Ne fait rien sans « captures.dir ». */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CapturesTest {
    private void ecrans(String suffixe) throws Exception {
        Fond.utiliser(Runnable::run);
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
                Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR, Manifest.permission.POST_NOTIFICATIONS);
        LocalDate aujourdhui = LocalDate.now();
        EcransTest.remplir(aujourdhui);
        EcransTest.agenda.ajouter("Piscine", EcransTest.a(aujourdhui.plusDays(2), 18, 0), EcransTest.a(aujourdhui.plusDays(2), 19, 0), false);
        EcransTest.agenda.ajouter("Marché", EcransTest.a(aujourdhui.minusDays(4), 10, 0), EcransTest.a(aujourdhui.minusDays(4), 11, 0), false);
        EcransTest.ActiviteEssai a = Robolectric.buildActivity(EcransTest.ActiviteEssai.class).setup().get();
        View ecran = a.getWindow().getDecorView();
        Essai.attendre();
        Essai.capturer(ecran, "calendrier-1-mois" + suffixe);
        Essai.appuyer(ecran, "Dentiste");
        Essai.capturer(ecran, "calendrier-2-evenement" + suffixe);
    }

    @Test
    @Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
    public void clair() throws Exception {
        ecrans("-clair");
    }

    @Test
    @Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-night-xhdpi")
    public void sombre() throws Exception {
        ecrans("-sombre");
    }
}
