package fr.manboyx.messages;

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
                Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS);
        EcransTest.remplir();
        EcransTest.ActiviteEssai a = Robolectric.buildActivity(EcransTest.ActiviteEssai.class).setup().get();
        View ecran = a.getWindow().getDecorView();
        Essai.attendre();
        Essai.capturer(ecran, "messages-1-conversations" + suffixe);
        Essai.appuyer(ecran, "Zoé Martin");
        Essai.saisir(ecran, "Message", "J'apporte le dessert.");
        Essai.capturer(ecran, "messages-2-conversation" + suffixe);
        a.onBackPressed();
        Essai.attendre();
        Essai.appuyer(ecran, "Nouveau message");
        Essai.saisir(ecran, "Nom ou numéro", "06 99");
        Essai.capturer(ecran, "messages-3-nouveau" + suffixe);
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
