package fr.manboyx.calendrier.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MoisTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static long le(int jour, int heure, int minute) {
        return LocalDateTime.of(2026, 10, jour, heure, minute).atZone(PARIS).toInstant().toEpochMilli();
    }

    /** Minuit UTC : c'est ainsi que les agendas notent les journées entières. */
    private static long jourEntier(int jour) {
        return LocalDate.of(2026, 10, jour).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private static Evenement evenement(String titre, long debut, long fin, boolean journee) {
        return new Evenement(1, titre, debut, fin, journee, 0, "");
    }

    @Test
    public void laGrilleASixSemainesEntieresCommencantUnLundi() {
        List<LocalDate> cases = Mois.grille(YearMonth.of(2026, 10));
        assertEquals(42, cases.size());
        assertEquals(DayOfWeek.MONDAY, cases.get(0).getDayOfWeek());
        // Le 1er octobre 2026 est un jeudi : la grille commence le lundi 28 septembre.
        assertEquals(LocalDate.of(2026, 9, 28), cases.get(0));
        assertEquals(LocalDate.of(2026, 10, 1), cases.get(3));
        assertEquals(LocalDate.of(2026, 11, 8), cases.get(41));
    }

    @Test
    public void unMoisQuiCommenceUnLundiCommenceEnPremiereCase() {
        assertEquals(LocalDate.of(2026, 6, 1), Mois.grille(YearMonth.of(2026, 6)).get(0));
    }

    @Test
    public void unMoisQuiCommenceUnDimancheOccupeLaDerniereCaseDeLaPremiereSemaine() {
        List<LocalDate> cases = Mois.grille(YearMonth.of(2026, 11));
        assertEquals(LocalDate.of(2026, 11, 1), cases.get(6));
        assertTrue(cases.contains(LocalDate.of(2026, 11, 30)));
    }

    @Test
    public void leTitreDuMoisEstEnFrancais() {
        assertEquals("octobre 2026", Mois.titre(YearMonth.of(2026, 10)));
        assertEquals("février 2027", Mois.titre(YearMonth.of(2027, 2)));
    }

    @Test
    public void lesEvenementsSontRangesParJourJourneesEntieresDAbordPuisParHeure() {
        Map<LocalDate, List<Evenement>> jours = Mois.parJour(Arrays.asList(
                evenement("Dentiste", le(14, 15, 0), le(14, 16, 0), false),
                evenement("Café", le(14, 9, 0), le(14, 9, 30), false),
                evenement("Anniversaire", jourEntier(14), jourEntier(15), true),
                evenement("Autre jour", le(15, 9, 0), le(15, 10, 0), false)), PARIS);
        List<String> titres = new ArrayList<>();
        for (Evenement e : jours.get(LocalDate.of(2026, 10, 14))) {
            titres.add(e.titre);
        }
        assertEquals(Arrays.asList("Anniversaire", "Café", "Dentiste"), titres);
        assertEquals(1, jours.get(LocalDate.of(2026, 10, 15)).size());
        assertNull(jours.get(LocalDate.of(2026, 10, 16)));
    }

    @Test
    public void uneJourneeEntiereNeDebordePasSurLaVeilleNiLeLendemain() {
        // À Paris (UTC+2 en octobre), minuit UTC tombe à 2 h du matin : l'événement ne doit pas glisser.
        Map<LocalDate, List<Evenement>> jours = Mois.parJour(
                Arrays.asList(evenement("Férié", jourEntier(14), jourEntier(15), true)), PARIS);
        assertEquals(1, jours.size());
        assertTrue(jours.containsKey(LocalDate.of(2026, 10, 14)));
    }

    @Test
    public void unEvenementDePlusieursJoursFigureAChacunDEux() {
        Map<LocalDate, List<Evenement>> jours = Mois.parJour(Arrays.asList(
                evenement("Vacances", jourEntier(20), jourEntier(23), true),
                evenement("Nuit de garde", le(10, 22, 0), le(11, 6, 0), false)), PARIS);
        assertTrue(jours.containsKey(LocalDate.of(2026, 10, 20)));
        assertTrue(jours.containsKey(LocalDate.of(2026, 10, 22)));
        assertFalse("le lendemain du dernier jour n'en fait pas partie", jours.containsKey(LocalDate.of(2026, 10, 23)));
        assertTrue(jours.containsKey(LocalDate.of(2026, 10, 10)));
        assertTrue(jours.containsKey(LocalDate.of(2026, 10, 11)));
    }

    @Test
    public void unEvenementQuiFinitAMinuitNEstPasComptéLeLendemain() {
        Map<LocalDate, List<Evenement>> jours = Mois.parJour(
                Arrays.asList(evenement("Soirée", le(10, 20, 0), le(11, 0, 0), false)), PARIS);
        assertFalse(jours.containsKey(LocalDate.of(2026, 10, 11)));
    }

    @Test
    public void lHoraireEstEcritSelonLeTypeDEvenement() {
        assertEquals("Toute la journée", Mois.horaire(evenement("x", jourEntier(14), jourEntier(15), true), PARIS));
        assertEquals("14:00 – 15:30", Mois.horaire(evenement("x", le(14, 14, 0), le(14, 15, 30), false), PARIS));
        assertEquals("14:00", Mois.horaire(evenement("x", le(14, 14, 0), le(14, 14, 0), false), PARIS));
    }
}
