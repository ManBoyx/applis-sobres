package fr.manboyx.calculatrice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import fr.manboyx.essais.Essai;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcranTest {
    private final CalculatriceActivity a = Robolectric.buildActivity(CalculatriceActivity.class).setup().get();
    private final View ecran = a.getWindow().getDecorView();

    private void taper(String touches) {
        for (String touche : touches.split(" ")) {
            Essai.appuyer(ecran, "Touche " + (touche.equals("()") ? "( )" : touche));
        }
    }

    private String lire(String description) {
        return ((TextView) Essai.trouver(ecran, description)).getText().toString();
    }

    @Test
    public void lExpressionSAfficheAvecSonResultatEnAvance() {
        taper("1 2 + 3 × 4");
        assertEquals("12 + 3 × 4", lire("Expression"));
        assertEquals("= 24", lire("Aperçu du résultat"));
    }

    @Test
    public void egalAfficheLeResultatEtGardeLeCalculDansLHistorique() {
        taper("2 + 3 =");
        assertEquals("5", lire("Expression"));
        assertEquals("", lire("Aperçu du résultat"));
        assertTrue(Essai.visible(ecran, "2 + 3 = 5"));
    }

    @Test
    public void uneDivisionParZeroEstAnnonceeSansEffacerCeQuiEstTape() {
        taper("5 ÷ 0 =");
        assertEquals("Division par zéro", lire("Aperçu du résultat"));
        assertEquals("5 ÷ 0", lire("Expression"));
        assertFalse(Essai.contient(ecran, "5 ÷ 0 ="));
    }

    @Test
    public void appuyerSurUnAncienCalculReprendSonResultat() {
        taper("6 × 7 = C");
        assertEquals("0", lire("Expression"));
        Essai.appuyer(ecran, "6 × 7 = 42");
        assertEquals("42", lire("Expression"));
        taper("+ 8");
        assertEquals("= 50", lire("Aperçu du résultat"));
    }

    @Test
    public void unAppuiLongSurEffacerVideTout() {
        taper("1 2 3");
        Essai.trouver(ecran, "Touche ⌫").performLongClick();
        assertEquals("0", lire("Expression"));
    }
}
