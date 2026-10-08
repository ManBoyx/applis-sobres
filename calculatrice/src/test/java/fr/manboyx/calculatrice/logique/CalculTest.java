package fr.manboyx.calculatrice.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.math.BigDecimal;

public class CalculTest {
    private static String calcule(String expression) throws Calcul.Erreur {
        return Calcul.ecrire(Calcul.evaluer(expression));
    }

    private static String erreur(String expression) {
        try {
            Calcul.evaluer(expression);
            fail("« " + expression + " » aurait dû être refusée");
            return null;
        } catch (Calcul.Erreur e) {
            return e.getMessage();
        }
    }

    @Test
    public void lesQuatreOperations() throws Exception {
        assertEquals("5", calcule("2 + 3"));
        assertEquals("−1", calcule("2 − 3"));
        assertEquals("6", calcule("2 × 3"));
        assertEquals("2,5", calcule("5 ÷ 2"));
    }

    @Test
    public void laMultiplicationPasseAvantLAddition() throws Exception {
        assertEquals("14", calcule("2 + 3 × 4"));
        assertEquals("1", calcule("10 − 6 ÷ 2 × 3"));
    }

    @Test
    public void lesParenthesesChangentLOrdre() throws Exception {
        assertEquals("20", calcule("(2 + 3) × 4"));
        assertEquals("21", calcule("3 × (2 + (1 + 4))"));
    }

    @Test
    public void lesDecimauxSontExacts() throws Exception {
        assertEquals("0,3", calcule("0,1 + 0,2"));
        assertEquals("1,21", calcule("1,1 × 1,1"));
    }

    @Test
    public void unNombreNegatifPeutCommencerUneExpressionOuUneParenthese() throws Exception {
        assertEquals("−3", calcule("−5 + 2"));
        assertEquals("−10", calcule("2 × (−5)"));
        assertEquals("7", calcule("2 − −5"));
    }

    @Test
    public void lePourcentageSeulVautUnCentieme() throws Exception {
        assertEquals("0,5", calcule("50 %"));
        assertEquals("30", calcule("200 × 15 %"));
    }

    @Test
    public void ajouterOuRetirerUnPourcentagePorteSurLeNombreQuiPrecede() throws Exception {
        assertEquals("220", calcule("200 + 10 %"));
        assertEquals("170", calcule("200 − 15 %"));
    }

    @Test
    public void uneDivisionQuiNeTombePasJusteEstArrondie() throws Exception {
        assertEquals("0,333333333333", calcule("1 ÷ 3"));
        assertEquals("0,666666666667", calcule("2 ÷ 3"));
    }

    @Test
    public void lesGrandsNombresSontSeparesParMilliers() throws Exception {
        assertEquals("1 234 567", calcule("1234567"));
        assertEquals("1 234,5", calcule("1234,5"));
        assertEquals("−1 000", calcule("−1000"));
    }

    @Test
    public void lesZerosInutilesDisparaissent() throws Exception {
        assertEquals("2", calcule("1,5 + 0,5"));
        assertEquals("100", calcule("10 × 10"));
        assertEquals("0", calcule("5 − 5"));
    }

    @Test
    public void unTresGrandNombreEstEcritEnPuissanceDeDix() {
        assertEquals("1,5E20", Calcul.ecrire(new BigDecimal("150000000000000000000")));
    }

    @Test
    public void diviserParZeroEstUneErreurExpliquee() {
        assertEquals("Division par zéro", erreur("5 ÷ 0"));
        assertEquals("Division par zéro", erreur("5 ÷ (2 − 2)"));
    }

    @Test
    public void uneExpressionIncompleteEstRefusee() {
        assertEquals("Expression incomplète", erreur("2 +"));
        assertEquals("Expression incomplète", erreur("(2 + 3"));
        assertEquals("Expression incomplète", erreur(""));
        assertEquals("Expression incomplète", erreur("2 × × 3"));
        assertEquals("Expression incomplète", erreur("2 3"));
        assertEquals("Expression incomplète", erreur("1,2,3"));
    }

    @Test
    public void lesEspacesNeComptentPas() throws Exception {
        assertEquals("1 000 000", calcule("1 000 × 1 000"));
        assertEquals("5", calcule("2+3"));
    }
}
