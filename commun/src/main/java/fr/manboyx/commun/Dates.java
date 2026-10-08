package fr.manboyx.commun;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Dates et durées écrites comme on les dit en français. */
public final class Dates {
    private Dates() {}

    private static final String[] JOURS = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
    private static final String[] MOIS = {"janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août",
            "septembre", "octobre", "novembre", "décembre"};
    private static final String[] MOIS_COURTS = {"janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août",
            "sept.", "oct.", "nov.", "déc."};
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final DateTimeFormatter ENTIERE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    private static ZonedDateTime moment(long instant, ZoneId zone) {
        return Instant.ofEpochMilli(instant).atZone(zone);
    }

    /** Pour une liste : l'heure si c'est aujourd'hui, « Hier », le jour de la semaine, puis la date. */
    public static String relative(long instant, long maintenant, ZoneId zone) {
        ZonedDateTime moment = moment(instant, zone);
        LocalDate jour = moment.toLocalDate();
        LocalDate aujourdhui = moment(maintenant, zone).toLocalDate();
        long ecart = ChronoUnit.DAYS.between(jour, aujourdhui);
        if (ecart == 0) {
            return HEURE.format(moment);
        }
        if (ecart == 1) {
            return "Hier";
        }
        if (ecart > 1 && ecart < 7) {
            return JOURS[jour.getDayOfWeek().getValue() - 1];
        }
        if (jour.getYear() == aujourdhui.getYear()) {
            return jour.getDayOfMonth() + " " + MOIS_COURTS[jour.getMonthValue() - 1];
        }
        return ENTIERE.format(moment);
    }

    /** Pour un titre de journée : « Aujourd'hui », « Hier », « lundi 12 mars », « 12 mars 2024 ». */
    public static String jour(long instant, long maintenant, ZoneId zone) {
        LocalDate jour = moment(instant, zone).toLocalDate();
        LocalDate aujourdhui = moment(maintenant, zone).toLocalDate();
        long ecart = ChronoUnit.DAYS.between(jour, aujourdhui);
        if (ecart == 0) {
            return "Aujourd'hui";
        }
        if (ecart == 1) {
            return "Hier";
        }
        String date = jour.getDayOfMonth() + " " + MOIS[jour.getMonthValue() - 1];
        if (jour.getYear() == aujourdhui.getYear()) {
            return JOURS[jour.getDayOfWeek().getValue() - 1] + " " + date;
        }
        return date + " " + jour.getYear();
    }

    /** « 14:05 » */
    public static String heure(long instant, ZoneId zone) {
        return HEURE.format(moment(instant, zone));
    }

    /** « 45 s », « 3 min 05 s », « 1 h 02 min » */
    public static String duree(long secondes) {
        long heures = secondes / 3600;
        long minutes = secondes % 3600 / 60;
        long reste = secondes % 60;
        if (heures > 0) {
            return String.format(Locale.ROOT, "%d h %02d min", heures, minutes);
        }
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%d min %02d s", minutes, reste);
        }
        return reste + " s";
    }
}
