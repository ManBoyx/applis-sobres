package fr.manboyx.galerie;

import static org.robolectric.Shadows.shadowOf;

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
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(GalerieActivity.demandees());
        EcransTest.remplir();
        long maintenant = System.currentTimeMillis();
        for (int i = 6; i <= 20; i++) {
            EcransTest.photos.ajouter(i, i % 7 == 0, i % 3 == 0 ? "WhatsApp Images" : "Camera", maintenant - i * 3_600_000L);
        }
        EcransTest.ActiviteEssai a = Robolectric.buildActivity(EcransTest.ActiviteEssai.class).setup().get();
        View ecran = a.getWindow().getDecorView();
        Essai.attendre();
        Essai.capturer(ecran, "galerie-1-photos" + suffixe);
        Essai.appuyer(ecran, "Albums");
        Essai.capturer(ecran, "galerie-2-albums" + suffixe);
        Essai.appuyer(ecran, "Photos");
        Essai.appuyer(ecran, "Photo IMG_8.jpg");
        Essai.capturer(ecran, "galerie-3-photo" + suffixe);
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
