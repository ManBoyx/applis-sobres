package fr.manboyx.calendrier.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class FicheTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static long le(int jour, int heure, int minute) {
        return LocalDateTime.of(2026, 10, jour, heure, minute).atZone(PARIS).toInstant().toEpochMilli();
    }

    private static LocalDateTime lire(long instant) {
        return Instant.ofEpochMilli(instant).atZone(PARIS).toLocalDateTime();
    }

    @Test
    public void unNouvelEvenementAujourdHuiCommenceALaProchaineHeurePleineEtDureUneHeure() {
        Fiche f = Fiche.nouvelle(LocalDate.of(2026, 10, 14), le(14, 15, 20), PARIS);
        assertEquals(LocalDateTime.of(2026, 10, 14, 16, 0), lire(f.debut));
        assertEquals(LocalDateTime.of(2026, 10, 14, 17, 0), lire(f.fin));
        assertFalse(f.journee);
        assertEquals(0, f.id);
        assertEquals(-1, f.rappel);
    }

    @Test
    public void unNouvelEvenementUnAutreJourCommenceANeufHeures() {
        Fiche f = Fiche.nouvelle(LocalDate.of(2026, 10, 20), le(14, 15, 20), PARIS);
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 0), lire(f.debut));
        assertEquals(LocalDateTime.of(2026, 10, 20, 10, 0), lire(f.fin));
    }

    @Test
    public void corrigerNettoieLeTitreEtEmpecheUneFinAvantLeDebut() {
        Fiche f = new Fiche();
        f.titre = "  Dentiste  ";
        f.debut = le(14, 15, 0);
        f.fin = le(14, 14, 0);
        f.corriger();
        assertEquals("Dentiste", f.titre);
        assertEquals("une heure après le début", le(14, 16, 0), f.fin);
    }

    @Test
    public void unTitreVideDevientSansTitre() {
        Fiche f = new Fiche();
        f.titre = "   ";
        f.debut = le(14, 15, 0);
        f.fin = le(14, 16, 0);
        f.corriger();
        assertEquals("Sans titre", f.titre);
    }

    @Test
    public void passerEnJourneeEntiereGardeLeJourEtSEcritEnMinuitUtc() {
        Fiche f = new Fiche();
        f.debut = le(14, 23, 30);
        f.fin = le(15, 0, 30);
        f.journee(true, PARIS);
        assertTrue(f.journee);
        assertEquals(LocalDate.of(2026, 10, 14).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), f.debut);
        assertEquals("jusqu'au lendemain du dernier jour", LocalDate.of(2026, 10, 16).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), f.fin);
        assertEquals("UTC", f.fuseau(PARIS));
    }

    @Test
    public void revenirAUnEvenementAHeureFixeLeReposeLeMemeJourANeufHeures() {
        Fiche f = new Fiche();
        f.debut = le(14, 10, 0);
        f.fin = le(14, 11, 0);
        f.journee(true, PARIS);
        f.journee(false, PARIS);
        assertFalse(f.journee);
        assertEquals(LocalDateTime.of(2026, 10, 14, 9, 0), lire(f.debut));
        assertEquals(LocalDateTime.of(2026, 10, 14, 10, 0), lire(f.fin));
        assertEquals("Europe/Paris", f.fuseau(PARIS));
    }

    @Test
    public void laDureeEstEcriteAuFormatDesAgendas() {
        Fiche f = new Fiche();
        f.debut = le(14, 10, 0);
        f.fin = le(14, 11, 30);
        assertEquals("P5400S", f.duree());
        f.journee(true, PARIS);
        assertEquals("P1D", f.duree());
        f.fin += 2 * 24 * 3600_000L;
        assertEquals("P3D", f.duree());
    }

    @Test
    public void lesDureesEcritesParLesAgendasSontComprises() {
        assertEquals(3_600_000L, Fiche.millis("P3600S"));
        assertEquals(5_400_000L, Fiche.millis("PT1H30M"));
        assertEquals(90_000L, Fiche.millis("PT1M30S"));
        assertEquals(2 * 86_400_000L, Fiche.millis("P2D"));
        assertEquals(7 * 86_400_000L, Fiche.millis("P1W"));
        assertEquals(86_400_000L + 3_600_000L, Fiche.millis("P1DT1H"));
        assertEquals(0, Fiche.millis(null));
        assertEquals(0, Fiche.millis("n'importe quoi"));
    }

    @Test
    public void laRepetitionDonneSaRegle() {
        Fiche f = new Fiche();
        assertEquals("", f.regle());
        f.repetition = Repetition.SEMAINE;
        assertEquals("FREQ=WEEKLY", f.regle());
    }

    @Test
    public void uneRegleVenueDUneAutreAppliEstGardeeTelleQuelle() {
        Fiche f = new Fiche();
        f.regleDOrigine = "FREQ=MONTHLY;BYDAY=2TU";
        f.repetition = Repetition.depuis(f.regleDOrigine);
        assertEquals(Repetition.AUTRE, f.repetition);
        assertEquals("FREQ=MONTHLY;BYDAY=2TU", f.regle());
    }

    @Test
    public void lesReglesSimplesSontReconnues() {
        assertEquals(Repetition.AUCUNE, Repetition.depuis(null));
        assertEquals(Repetition.AUCUNE, Repetition.depuis(""));
        assertEquals(Repetition.JOUR, Repetition.depuis("FREQ=DAILY"));
        assertEquals(Repetition.SEMAINE, Repetition.depuis("FREQ=WEEKLY;WKST=MO"));
        assertEquals(Repetition.AN, Repetition.depuis("FREQ=YEARLY"));
        assertEquals("avec une fin ou un intervalle, ce n'est plus une règle simple", Repetition.AUTRE, Repetition.depuis("FREQ=WEEKLY;INTERVAL=2"));
        assertEquals(Repetition.AUTRE, Repetition.depuis("FREQ=DAILY;COUNT=5"));
    }

    @Test
    public void lesRappelsOntUnLibelleLisible() {
        assertEquals("Aucun rappel", Fiche.libelleRappel(-1));
        assertEquals("À l'heure", Fiche.libelleRappel(0));
        assertEquals("10 minutes avant", Fiche.libelleRappel(10));
        assertEquals("1 heure avant", Fiche.libelleRappel(60));
        assertEquals("1 jour avant", Fiche.libelleRappel(1440));
        assertEquals("un rappel posé par une autre appli garde un libellé juste", "45 minutes avant", Fiche.libelleRappel(45));
        assertEquals("2 heures avant", Fiche.libelleRappel(120));
    }
}
