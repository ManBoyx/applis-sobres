package fr.manboyx.commun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NumerosTest {
    @Test
    public void lesChiffresGardentLePlusDeTete() {
        assertEquals("+33612345678", Numeros.chiffres("+33 6 12-34.56 78"));
        assertEquals("0612345678", Numeros.chiffres("06 12 34 56 78"));
        assertEquals("", Numeros.chiffres(null));
    }

    @Test
    public void leMemeNumeroEcritAvecOuSansIndicatifEstReconnu() {
        assertTrue(Numeros.memes("06 12 34 56 78", "+33612345678"));
        assertTrue(Numeros.memes("0033612345678", "0612345678"));
        assertTrue(Numeros.memes("+33 6 12 34 56 78", "+33612345678"));
    }

    @Test
    public void deuxNumerosDifferentsNeSontPasConfondus() {
        assertFalse(Numeros.memes("0612345678", "0612345679"));
        assertFalse(Numeros.memes("0612345678", "0712345678"));
    }

    @Test
    public void lesNumerosCourtsDoiventEtreIdentiques() {
        assertTrue(Numeros.memes("3615", "36 15"));
        assertFalse(Numeros.memes("15", "115"));
        assertFalse(Numeros.memes("112", "0612345112"));
    }

    @Test
    public void unNumeroVideNeRessembleARien() {
        assertFalse(Numeros.memes("", ""));
        assertFalse(Numeros.memes(null, "0612345678"));
    }

    @Test
    public void laCleEstLaMemePourToutesLesEcritures() {
        assertEquals(Numeros.cle("06 12 34 56 78"), Numeros.cle("+33612345678"));
        assertEquals("3615", Numeros.cle("36 15"));
    }

    @Test
    public void unExpediteurEnLettresGardeSonNomCommeCle() {
        // Les SMS de services arrivent souvent d'un nom, pas d'un numéro.
        assertEquals("MaBanque", Numeros.cle("MaBanque"));
        assertTrue(Numeros.memes("MaBanque", "MaBanque"));
        assertFalse(Numeros.memes("MaBanque", "AutreBanque"));
    }
}
