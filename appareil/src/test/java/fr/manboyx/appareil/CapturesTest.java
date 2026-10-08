package fr.manboyx.appareil;

import static org.robolectric.Shadows.shadowOf;

import android.graphics.SurfaceTexture;
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

/** Écrit une image de l'écran (l'aperçu de la caméra y reste noir). Ne fait rien sans « captures.dir ». */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CapturesTest {
    @Test
    @Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
    public void ecran() throws Exception {
        Fond.utiliser(Runnable::run);
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(AppareilActivity.demandees());
        EcranTest.chambre = new EcranTest.FausseChambre();
        EcranTest.rangees = new java.util.ArrayList<>();
        EcranTest.ActiviteEssai a = Robolectric.buildActivity(EcranTest.ActiviteEssai.class).setup().get();
        ((EcranPrise) a.ecran()).surfaceDisponible(new SurfaceTexture(0));
        EcranTest.chambre.prete(false, true);
        View ecran = a.getWindow().getDecorView();
        Essai.capturer(ecran, "appareil-1");
    }
}
