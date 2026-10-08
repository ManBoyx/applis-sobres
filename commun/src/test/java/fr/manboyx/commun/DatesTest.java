package fr.manboyx.commun;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class DatesTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static long le(int annee, int mois, int jour, int heure, int minute) {
        return LocalDateTime.of(annee, mois, jour, heure, minute).atZone(PARIS).toInstant().toEpochMilli();
    }

    /** Mercredi 14 octobre 2026, 15 h 30. */
    private static final long MAINTENANT = le(2026, 10, 14, 15, 30);

    @Test
    public void aujourdHuiOnDonneLHeure() {
        assertEquals("09:05", Dates.relative(le(2026, 10, 14, 9, 5), MAINTENANT, PARIS));
    }

    @Test
    public void laVeilleOnDitHier() {
        assertEquals("Hier", Dates.relative(le(2026, 10, 13, 23, 59), MAINTENANT, PARIS));
    }

    @Test
    public void dansLaSemaineOnDonneLeJour() {
        assertEquals("lundi", Dates.relative(le(2026, 10, 12, 8, 0), MAINTENANT, PARIS));
        assertEquals("jeudi", Dates.relative(le(2026, 10, 8, 8, 0), MAINTENANT, PARIS));
    }

    @Test
    public void auDelaDUneSemaineOnDonneLaDate() {
        assertEquals("7 oct.", Dates.relative(le(2026, 10, 7, 8, 0), MAINTENANT, PARIS));
        assertEquals("3 mars", Dates.relative(le(2026, 3, 3, 8, 0), MAINTENANT, PARIS));
    }

    @Test
    public void uneAutreAnneeEstEcriteEnEntier() {
        assertEquals("25/12/2025", Dates.relative(le(2025, 12, 25, 8, 0), MAINTENANT, PARIS));
    }

    @Test
    public void leTitreDUneJourneeSeDitCommeOnParle() {
        assertEquals("Aujourd'hui", Dates.jour(le(2026, 10, 14, 0, 1), MAINTENANT, PARIS));
        assertEquals("Hier", Dates.jour(le(2026, 10, 13, 12, 0), MAINTENANT, PARIS));
        assertEquals("lundi 12 octobre", Dates.jour(le(2026, 10, 12, 12, 0), MAINTENANT, PARIS));
        assertEquals("25 décembre 2025", Dates.jour(le(2025, 12, 25, 12, 0), MAINTENANT, PARIS));
    }

    @Test
    public void lHeureEstSurVingtQuatreHeures() {
        assertEquals("15:30", Dates.heure(MAINTENANT, PARIS));
        assertEquals("00:07", Dates.heure(le(2026, 10, 14, 0, 7), PARIS));
    }

    @Test
    public void lesDureesSontLisibles() {
        assertEquals("0 s", Dates.duree(0));
        assertEquals("45 s", Dates.duree(45));
        assertEquals("3 min 05 s", Dates.duree(185));
        assertEquals("1 h 02 min", Dates.duree(3725));
    }
}
