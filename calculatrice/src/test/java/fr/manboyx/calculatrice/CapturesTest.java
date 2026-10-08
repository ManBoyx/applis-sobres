package fr.manboyx.calculatrice;

import android.view.View;

import fr.manboyx.essais.Essai;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Écrit des images de l'écran. Ne fait rien sans « captures.dir ». */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CapturesTest {
    private void ecran(String suffixe) throws Exception {
        CalculatriceActivity a = Robolectric.buildActivity(CalculatriceActivity.class).setup().get();
        View ecran = a.getWindow().getDecorView();
        for (String touche : "2 0 0 + 1 0 % = C 6 × 7 = C 1 2 , 5 × ( ) 3 + 4".split(" (?![)])")) {
            Essai.appuyer(ecran, "Touche " + touche);
        }
        Essai.capturer(ecran, "calculatrice-1" + suffixe);
    }

    @Test
    @Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
    public void clair() throws Exception {
        ecran("-clair");
    }

    @Test
    @Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-night-xhdpi")
    public void sombre() throws Exception {
        ecran("-sombre");
    }
}
