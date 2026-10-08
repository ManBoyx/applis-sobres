package fr.manboyx.galerie.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CadrageTest {
    private static final float E = 0.01f;

    /** Une photo en paysage de 2000 × 1000 dans un écran en portrait de 1000 × 2000. */
    private static Cadrage paysage() {
        return new Cadrage(2000, 1000, 1000, 2000);
    }

    @Test
    public void auReposLImageEstEntiereEtCentree() {
        Cadrage c = paysage();
        assertEquals(1f, c.zoom(), E);
        assertEquals(0.5f, c.echelle(), E);
        assertEquals(0f, c.gauche(), E);
        assertEquals("bandes égales en haut et en bas", 750f, c.haut(), E);
        assertTrue(c.auRepos());
    }

    @Test
    public void unePetiteImageEstAgrandiePourRemplirLEcran() {
        Cadrage c = new Cadrage(100, 100, 1000, 2000);
        assertEquals(10f, c.echelle(), E);
        assertEquals(500f, c.haut(), E);
    }

    @Test
    public void zoomerGardeSousLesDoigtsLePointVise() {
        Cadrage c = paysage();
        // Le point d'écran (250, 1000) montre le point d'image (500, 500).
        c.zoomer(2f, 250, 1000);
        assertEquals(2f, c.zoom(), E);
        assertEquals(1f, c.echelle(), E);
        assertEquals(250f, c.gauche() + 500 * c.echelle(), E);
        assertEquals(1000f, c.haut() + 500 * c.echelle(), E);
        assertFalse(c.auRepos());
    }

    @Test
    public void leZoomResteEntreLImageEntiereEtLeMaximum() {
        Cadrage c = paysage();
        c.zoomer(0.2f, 500, 1000);
        assertEquals(1f, c.zoom(), E);
        c.zoomer(100f, 500, 1000);
        assertEquals(Cadrage.MAXIMUM, c.zoom(), E);
    }

    @Test
    public void onNePeutPasFaireSortirLImageDeLEcran() {
        Cadrage c = paysage();
        c.zoomer(4f, 500, 1000);
        // À ce grossissement l'image fait 4000 × 2000 : plus large que l'écran, exactement aussi haute.
        c.deplacer(10_000, 10_000);
        assertEquals("le bord gauche de l'image s'arrête au bord gauche de l'écran", 0f, c.gauche(), E);
        assertEquals(0f, c.haut(), E);
        c.deplacer(-10_000, -10_000);
        assertEquals("le bord droit de l'image s'arrête au bord droit de l'écran", 1000f, c.gauche() + 2000 * c.echelle(), E);
        assertEquals(2000f, c.haut() + 1000 * c.echelle(), E);
    }

    @Test
    public void dansLeSensOuLImageEstPlusPetiteQueLEcranElleResteCentree() {
        Cadrage c = paysage();
        c.zoomer(2f, 500, 1000);
        // 2000 × 1000 à l'écran : toute la hauteur de l'image tient, elle ne doit pas se promener de haut en bas.
        c.deplacer(0, 700);
        assertEquals(500f, c.haut(), E);
    }

    @Test
    public void auReposUnDeplacementNeBougeRien() {
        Cadrage c = paysage();
        c.deplacer(300, 300);
        assertEquals(0f, c.gauche(), E);
        assertEquals(750f, c.haut(), E);
        assertTrue(c.auRepos());
    }

    @Test
    public void leDoubleAppuiGrossitPuisRevientALImageEntiere() {
        Cadrage c = paysage();
        c.basculer(500, 1000);
        assertEquals(Cadrage.DOUBLE_APPUI, c.zoom(), E);
        c.basculer(500, 1000);
        assertEquals(1f, c.zoom(), E);
        assertEquals(750f, c.haut(), E);
        assertTrue(c.auRepos());
    }

    @Test
    public void desDimensionsNullesNeFontPasPlanter() {
        Cadrage c = new Cadrage(0, 0, 1000, 2000);
        c.zoomer(2f, 10, 10);
        c.deplacer(5, 5);
        assertTrue(c.echelle() > 0);
    }
}
