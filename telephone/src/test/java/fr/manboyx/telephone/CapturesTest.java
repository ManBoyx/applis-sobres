package fr.manboyx.telephone;

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

/** Écrit des images des écrans. Ne fait rien sans « captures.dir ». */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CapturesTest {
    private void ecrans(String suffixe) throws Exception {
        Fond.utiliser(Runnable::run);
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
                Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS);
        EcransTest.remplir();
        EcransTest.ActiviteEssai a = Robolectric.buildActivity(EcransTest.ActiviteEssai.class).setup().get();
        View ecran = a.getWindow().getDecorView();
        Essai.attendre();
        for (char c : "0612".toCharArray()) {
            Essai.appuyer(ecran, "Touche " + c);
        }
        Essai.capturer(ecran, "telephone-1-clavier" + suffixe);
        Essai.appuyer(ecran, "Journal");
        Essai.capturer(ecran, "telephone-2-journal" + suffixe);
        Essai.appuyer(ecran, "Contacts");
        Essai.capturer(ecran, "telephone-3-contacts" + suffixe);
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
