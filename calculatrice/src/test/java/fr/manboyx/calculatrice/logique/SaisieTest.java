package fr.manboyx.calculatrice.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class SaisieTest {
    /** Tape les touches une à une (séparées par des espaces) et rend la dernière ligne d'historique produite. */
    private static String taper(Saisie saisie, String touches) throws Calcul.Erreur {
        String ligne = null;
        for (String touche : touches.split(" ")) {
            String produite = saisie.taper(touche.equals("()") ? Saisie.PARENTHESES : touche);
            if (produite != null) {
                ligne = produite;
            }
        }
        return ligne;
    }

    private static Saisie apres(String touches) throws Calcul.Erreur {
        Saisie saisie = new Saisie();
        taper(saisie, touches);
        return saisie;
    }

    @Test
    public void lesChiffresEtLesOperateursFormentLExpression() throws Exception {
        assertEquals("12 + 3", apres("1 2 + 3").expression());
    }

    @Test
    public void unDeuxiemeOperateurRemplaceLePremier() throws Exception {
        assertEquals("5 × 2", apres("5 + × 2").expression());
    }

    @Test
    public void onNePeutPasCommencerParUnOperateurSaufLeMoins() throws Exception {
        assertEquals("", apres("×").expression());
        assertEquals("−5", apres("− 5").expression());
    }

    @Test
    public void uneSeuleVirguleParNombre() throws Exception {
        assertEquals("1,5", apres("1 , 5 ,").expression());
        assertEquals("1,5 + 2,5", apres("1 , 5 + 2 , 5").expression());
    }

    @Test
    public void uneVirguleSeuleCommenceParZero() throws Exception {
        assertEquals("0,5", apres(", 5").expression());
        assertEquals("2 + 0,5", apres("2 + , 5").expression());
    }

    @Test
    public void effacerRetireLeDernierSigneEtViderToutLeReste() throws Exception {
        assertEquals("12", apres("1 2 + ⌫").expression());
        assertEquals("1", apres("1 2 ⌫").expression());
        assertEquals("", apres("1 2 + 3 C").expression());
        assertEquals("", apres("⌫").expression());
    }

    @Test
    public void laToucheParenthesesOuvrePuisFerme() throws Exception {
        assertEquals("(2 + 3)", apres("() 2 + 3 ()").expression());
        assertEquals("2 × (", apres("2 × ()").expression());
        assertEquals("2 × (", apres("2 ()").expression());
    }

    @Test
    public void egalDonneLeResultatEtLaLigneDHistorique() throws Exception {
        Saisie saisie = new Saisie();
        assertEquals("2 + 3 × 4 = 14", taper(saisie, "2 + 3 × 4 ="));
        assertEquals("14", saisie.expression());
        assertTrue(saisie.resultat());
    }

    @Test
    public void egalFermeLesParenthesesOubliees() throws Exception {
        Saisie saisie = new Saisie();
        assertEquals("(2 + 3) × 4 = 20", taper(saisie, "() 2 + 3 () × 4 ="));
        saisie = new Saisie();
        assertEquals("2 × (3 + 4) = 14", taper(saisie, "2 × () 3 + 4 ="));
    }

    @Test
    public void apresUnResultatUnChiffreRecommenceEtUnOperateurContinue() throws Exception {
        assertEquals("7", apres("2 + 3 = 7").expression());
        assertEquals("5 × 2", apres("2 + 3 = × 2").expression());
        assertFalse(apres("2 + 3 = 7").resultat());
    }

    @Test
    public void uneErreurLaisseLExpressionTelleQuelle() throws Exception {
        Saisie saisie = apres("5 ÷ 0");
        try {
            saisie.taper(Saisie.EGAL);
            fail("la division par zéro aurait dû être signalée");
        } catch (Calcul.Erreur e) {
            assertEquals("Division par zéro", e.getMessage());
        }
        assertEquals("5 ÷ 0", saisie.expression());
        assertFalse(saisie.resultat());
    }

    @Test
    public void egalSurUnNombreSeulOuSurRienNeFaitRien() throws Exception {
        Saisie saisie = new Saisie();
        assertNull(taper(saisie, "="));
        assertNull(taper(saisie, "4 2 ="));
        assertEquals("42", saisie.expression());
    }

    @Test
    public void lApercuMontreLeResultatEnAvance() throws Exception {
        assertEquals("14", apres("2 + 3 × 4").apercu());
        assertEquals("un calcul inachevé montre ce qui est déjà calculable", "5", apres("2 + 3 ×").apercu());
        assertEquals("rien à montrer pour un nombre seul", "", apres("4 2").apercu());
        assertEquals("", apres("5 ÷ 0").apercu());
        assertEquals("", new Saisie().apercu());
    }

    @Test
    public void lePourcentageSePoseApresUnNombre() throws Exception {
        assertEquals("200 + 10 %", apres("2 0 0 + 1 0 %").expression());
        assertEquals("", apres("%").expression());
        assertEquals("220", apres("2 0 0 + 1 0 %").apercu());
    }

    @Test
    public void onPeutRepartirDUnResultatDeLHistorique() throws Exception {
        Saisie saisie = apres("1 +");
        saisie.reprendre("1 234,5");
        assertEquals("1 234,5", saisie.expression());
        taper(saisie, "× 2 =");
        assertEquals("2 469", saisie.expression());
    }

    @Test
    public void lesGrandsNombresTapesNeSontPasCoupes() throws Exception {
        assertEquals("1 000 000", Calcul.ecrire(Calcul.evaluer(apres("1 0 0 0 × 1 0 0 0").expression())));
    }
}
