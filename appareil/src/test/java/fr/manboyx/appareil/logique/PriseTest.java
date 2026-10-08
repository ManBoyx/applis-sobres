package fr.manboyx.appareil.logique;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PriseTest {
    @Test
    public void linclinaisonEstRameneeAuQuartDeTourLePlusProche() {
        assertEquals(0, Orientation.arrondir(10, 0));
        assertEquals(0, Orientation.arrondir(350, 90));
        assertEquals(90, Orientation.arrondir(60, 0));
        assertEquals(180, Orientation.arrondir(170, 0));
        assertEquals(270, Orientation.arrondir(300, 0));
        assertEquals(90, Orientation.arrondir(45, 0));
    }

    @Test
    public void telephoneAPlatOnGardeLaDerniereTenueConnue() {
        assertEquals(90, Orientation.arrondir(Orientation.INCONNUE, 90));
    }

    @Test
    public void laPhotoDeLaCameraArriereSuitLaTenueDuTelephone() {
        // Capteur monté à 90°, cas de presque tous les téléphones.
        assertEquals(90, Orientation.photo(90, 0, false));
        assertEquals(180, Orientation.photo(90, 90, false));
        assertEquals(270, Orientation.photo(90, 180, false));
        assertEquals(0, Orientation.photo(90, 270, false));
    }

    @Test
    public void laCameraFrontaleTourneDansLAutreSens() {
        // Capteur frontal monté à 270° le plus souvent.
        assertEquals(270, Orientation.photo(270, 0, true));
        assertEquals(180, Orientation.photo(270, 90, true));
        assertEquals(90, Orientation.photo(270, 180, true));
        assertEquals(0, Orientation.photo(270, 270, true));
    }

    private static final List<Taille> TAILLES = Arrays.asList(
            new Taille(4000, 2250), new Taille(4000, 3000), new Taille(3264, 2448), new Taille(1920, 1080),
            new Taille(1440, 1080), new Taille(1280, 960), new Taille(640, 480), new Taille(320, 240));

    @Test
    public void laPhotoPrendLaPlusGrandeTailleQuatreTiers() {
        assertEquals("4000×3000", Taille.photo(TAILLES).toString());
    }

    @Test
    public void sansTailleQuatreTiersOnPrendLaPlusGrande() {
        assertEquals("4000×2250", Taille.photo(Arrays.asList(new Taille(1920, 1080), new Taille(4000, 2250))).toString());
        assertNull(Taille.photo(Collections.emptyList()));
    }

    @Test
    public void lAperçuAuMemeFormatQueLaPhotoSansDepasserLEcran() {
        Taille photo = new Taille(4000, 3000);
        assertEquals("1440×1080", Taille.apercu(TAILLES, photo, 1920).toString());
        assertEquals("1280×960", Taille.apercu(TAILLES, photo, 1300).toString());
    }

    @Test
    public void siAucunAperçuNEstAssezPetitOnPrendLePlusPetitDuMemeFormat() {
        assertEquals("320×240", Taille.apercu(TAILLES, new Taille(4000, 3000), 100).toString());
    }

    @Test
    public void sansAperçuDuMemeFormatOnPrendLePlusProcheEnTaille() {
        List<Taille> seizeNeuviemes = Arrays.asList(new Taille(1920, 1080), new Taille(1280, 720));
        assertEquals("1920×1080", Taille.apercu(seizeNeuviemes, new Taille(4000, 3000), 1920).toString());
        assertNull(Taille.apercu(Collections.emptyList(), new Taille(4000, 3000), 1920));
    }

    @Test
    public void leBoutonDuFlashFaitLeTourDesTroisReglages() {
        assertEquals(Flash.OUI, Flash.AUTO.suivant());
        assertEquals(Flash.NON, Flash.OUI.suivant());
        assertEquals(Flash.AUTO, Flash.NON.suivant());
    }

    @Test
    public void leNomDeLaPhotoPorteLaDateEtLHeure() {
        assertEquals("IMG_20261008_170509.jpg", Prise.nom(LocalDateTime.of(2026, 10, 8, 17, 5, 9)));
    }

    @Test
    public void sansZoomToutLeCapteurSert() {
        assertArrayEquals(new int[] {0, 0, 4000, 3000}, Prise.zone(4000, 3000, 1f, 4f));
    }

    @Test
    public void leZoomPrendUneZoneCentreeDeMemeForme() {
        assertArrayEquals(new int[] {1000, 750, 3000, 2250}, Prise.zone(4000, 3000, 2f, 4f));
    }

    @Test
    public void leZoomResteEntreUnEtLeMaximumDeLAppareil() {
        assertEquals(1f, Prise.borner(0.3f, 4f), 0.001f);
        assertEquals(4f, Prise.borner(9f, 4f), 0.001f);
        assertEquals("un appareil sans zoom reste à 1", 1f, Prise.borner(3f, 0f), 0.001f);
        assertArrayEquals(new int[] {1500, 1125, 2500, 1875}, Prise.zone(4000, 3000, 10f, 4f));
    }
}
