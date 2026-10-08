package fr.manboyx.calendrier.logique;

import fr.manboyx.commun.Dates;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Le calendrier d'un mois : ses cases, et les événements de chaque jour. */
public final class Mois {
    private Mois() {}

    /** Nombre de cases : six semaines, pour que la grille ait toujours la même hauteur. */
    public static final int CASES = 42;

    private static final String[] NOMS = {"janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août",
            "septembre", "octobre", "novembre", "décembre"};

    /** Les 42 jours affichés pour ce mois : des semaines entières commençant le lundi, le 1er dans la première. */
    public static List<LocalDate> grille(YearMonth mois) {
        LocalDate premier = mois.atDay(1);
        LocalDate lundi = premier.minusDays(premier.getDayOfWeek().getValue() - 1);
        List<LocalDate> cases = new ArrayList<>(CASES);
        for (int i = 0; i < CASES; i++) {
            cases.add(lundi.plusDays(i));
        }
        return cases;
    }

    /** « octobre 2026 » */
    public static String titre(YearMonth mois) {
        return NOMS[mois.getMonthValue() - 1] + " " + mois.getYear();
    }

    /**
     * Les événements rangés par jour. Un événement de plusieurs jours figure à
     * chacun d'eux. Dans une journée : ceux qui durent toute la journée
     * d'abord, puis par heure de début.
     */
    public static Map<LocalDate, List<Evenement>> parJour(List<Evenement> evenements, ZoneId zone) {
        Map<LocalDate, List<Evenement>> jours = new TreeMap<>();
        for (Evenement e : evenements) {
            // Une journée entière est notée en temps universel : la lire dans le fuseau du téléphone la ferait glisser d'un jour.
            ZoneId lecture = e.journee ? ZoneOffset.UTC : zone;
            LocalDate premier = Instant.ofEpochMilli(e.debut).atZone(lecture).toLocalDate();
            // La fin est exclue : un événement qui s'arrête à minuit n'occupe pas le jour suivant.
            long dernierInstant = e.fin > e.debut ? e.fin - 1 : e.fin;
            LocalDate dernier = Instant.ofEpochMilli(dernierInstant).atZone(lecture).toLocalDate();
            for (LocalDate jour = premier; !jour.isAfter(dernier); jour = jour.plusDays(1)) {
                List<Evenement> liste = jours.get(jour);
                if (liste == null) {
                    liste = new ArrayList<>();
                    jours.put(jour, liste);
                }
                liste.add(e);
            }
        }
        for (List<Evenement> liste : jours.values()) {
            liste.sort(Comparator.comparing((Evenement e) -> !e.journee)
                    .thenComparingLong(e -> e.debut)
                    .thenComparing(e -> e.titre));
        }
        return jours;
    }

    /** « Toute la journée », « 14:00 – 15:30 », ou « 14:00 » si l'événement n'a pas de durée. */
    public static String horaire(Evenement evenement, ZoneId zone) {
        if (evenement.journee) {
            return "Toute la journée";
        }
        String debut = Dates.heure(evenement.debut, zone);
        return evenement.fin == evenement.debut ? debut : debut + " – " + Dates.heure(evenement.fin, zone);
    }
}
