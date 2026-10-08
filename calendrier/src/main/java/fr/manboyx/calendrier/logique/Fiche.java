package fr.manboyx.calendrier.logique;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** Un événement en cours de création ou de modification, tel que le formulaire le tient. */
public final class Fiche {
    /** 0 pour un nouvel événement. */
    public long id;
    public long agenda;
    public String titre = "";
    public String lieu = "";
    public String notes = "";
    public long debut;
    public long fin;
    public boolean journee;
    public Repetition repetition = Repetition.AUCUNE;
    /** La règle d'origine, gardée telle quelle quand `repetition` vaut AUTRE. */
    public String regleDOrigine = "";
    /** Minutes avant le début ; −1 : pas de rappel. */
    public int rappel = -1;

    private static final long HEURE = 3_600_000L;
    private static final long JOUR = 24 * HEURE;

    /** Minutes proposées pour le rappel, et leur libellé. */
    public static final int[] RAPPELS = {-1, 0, 10, 30, 60, 1440};

    public static String libelleRappel(int minutes) {
        if (minutes < 0) {
            return "Aucun rappel";
        }
        if (minutes == 0) {
            return "À l'heure";
        }
        if (minutes % 1440 == 0) {
            int jours = minutes / 1440;
            return jours + (jours == 1 ? " jour avant" : " jours avant");
        }
        if (minutes % 60 == 0) {
            int heures = minutes / 60;
            return heures + (heures == 1 ? " heure avant" : " heures avant");
        }
        return minutes + " minutes avant";
    }

    /** Une fiche neuve pour ce jour : une heure, à partir de la prochaine heure pleine si c'est aujourd'hui, sinon 9 h. */
    public static Fiche nouvelle(java.time.LocalDate jour, long maintenant, ZoneId zone) {
        Fiche fiche = new Fiche();
        java.time.ZonedDateTime instant = Instant.ofEpochMilli(maintenant).atZone(zone);
        java.time.ZonedDateTime debut = jour.equals(instant.toLocalDate())
                ? instant.truncatedTo(ChronoUnit.HOURS).plusHours(1)
                : jour.atTime(9, 0).atZone(zone);
        fiche.debut = debut.toInstant().toEpochMilli();
        fiche.fin = fiche.debut + HEURE;
        return fiche;
    }

    /** Remet la fiche d'aplomb avant enregistrement : titre net, fin jamais avant le début. */
    public void corriger() {
        titre = titre == null || titre.trim().isEmpty() ? "Sans titre" : titre.trim();
        lieu = lieu == null ? "" : lieu.trim();
        notes = notes == null ? "" : notes.trim();
        if (fin < debut || (journee && fin == debut)) {
            fin = debut + (journee ? JOUR : HEURE);
        }
    }

    /** Passe en « toute la journée » ou en revient, en gardant le même jour. */
    public void journee(boolean oui, ZoneId zone) {
        if (oui == journee) {
            return;
        }
        if (oui) {
            java.time.LocalDate premier = Instant.ofEpochMilli(debut).atZone(zone).toLocalDate();
            java.time.LocalDate dernier = Instant.ofEpochMilli(fin > debut ? fin - 1 : fin).atZone(zone).toLocalDate();
            // Les agendas notent une journée entière de minuit UTC à minuit UTC du lendemain du dernier jour.
            debut = premier.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            fin = dernier.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        } else {
            java.time.LocalDate jour = Instant.ofEpochMilli(debut).atZone(ZoneOffset.UTC).toLocalDate();
            debut = jour.atTime(9, 0).atZone(zone).toInstant().toEpochMilli();
            fin = debut + HEURE;
        }
        journee = oui;
    }

    /** La règle de répétition à écrire dans l'agenda ; vide s'il n'y en a pas. */
    public String regle() {
        return repetition == Repetition.AUTRE ? regleDOrigine : repetition.regle;
    }

    /** La durée au format des agendas (« P3600S », « P1D »), exigée pour un événement répété. */
    public String duree() {
        if (journee) {
            return "P" + Math.max(1, (fin - debut) / JOUR) + "D";
        }
        return "P" + Math.max(0, (fin - debut) / 1000) + "S";
    }

    /** Lit une durée écrite par un agenda (« P3600S », « PT1H30M », « P2D », « P1W ») ; 0 si elle est illisible. */
    public static long millis(String duree) {
        if (duree == null || !duree.startsWith("P")) {
            return 0;
        }
        long total = 0;
        long nombre = 0;
        boolean chiffres = false;
        boolean heures = false;
        for (int i = 1; i < duree.length(); i++) {
            char c = duree.charAt(i);
            if (c >= '0' && c <= '9') {
                nombre = nombre * 10 + (c - '0');
                chiffres = true;
                continue;
            }
            if (c == 'T') {
                // Après « T » viennent les heures, minutes et secondes.
                heures = true;
                continue;
            }
            long unite;
            switch (c) {
                case 'W':
                    unite = 7 * JOUR;
                    break;
                case 'D':
                    unite = JOUR;
                    break;
                case 'H':
                    unite = HEURE;
                    break;
                case 'M':
                    unite = heures ? 60_000L : 0;
                    break;
                case 'S':
                    unite = 1000L;
                    break;
                default:
                    unite = 0;
            }
            if (!chiffres || unite == 0) {
                return 0;
            }
            total += nombre * unite;
            nombre = 0;
            chiffres = false;
        }
        return total;
    }

    /** Le fuseau à enregistrer : UTC pour une journée entière, celui du téléphone sinon. */
    public String fuseau(ZoneId zone) {
        return journee ? "UTC" : zone.getId();
    }
}
