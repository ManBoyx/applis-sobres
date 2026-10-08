package fr.manboyx.fichiers;

import android.view.View;

import fr.manboyx.commun.Fond;
import fr.manboyx.essais.Essai;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Écrit des images des écrans. Ne fait rien sans « captures.dir ». */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CapturesTest {
    private void ecrans(String suffixe) throws Exception {
        Fond.utiliser(Runnable::run);
        EcransTest.remplir();
        try {
            EcransTest.ActiviteEssai a = Robolectric.buildActivity(EcransTest.ActiviteEssai.class).setup().get();
            View ecran = a.getWindow().getDecorView();
            Essai.attendre();
            Essai.capturer(ecran, "fichiers-1-racine" + suffixe);
            a.presse = new FichiersActivity.Presse(new java.io.File(EcransTest.racine, "notes.txt"), false);
            Essai.appuyer(ecran, "Documents");
            Essai.capturer(ecran, "fichiers-2-coller" + suffixe);
        } finally {
            EcransTest.effacer(EcransTest.racine);
        }
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
