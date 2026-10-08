package fr.manboyx.calendrier.logique;

/** Les répétitions simples que l'appli sait créer. */
public enum Repetition {
    AUCUNE("Une seule fois", ""),
    JOUR("Chaque jour", "FREQ=DAILY"),
    SEMAINE("Chaque semaine", "FREQ=WEEKLY"),
    MOIS("Chaque mois", "FREQ=MONTHLY"),
    AN("Chaque année", "FREQ=YEARLY"),
    /** Une règle créée par une autre appli (« le 2e mardi du mois »…) : affichée, jamais modifiée. */
    AUTRE("Répétition particulière", "");

    public final String libelle;
    /** La règle au format des agendas (RRULE). */
    public final String regle;

    Repetition(String libelle, String regle) {
        this.libelle = libelle;
        this.regle = regle;
    }

    /** Retrouve la répétition d'une règle lue dans l'agenda. */
    public static Repetition depuis(String regle) {
        if (regle == null || regle.trim().isEmpty()) {
            return AUCUNE;
        }
        // « WKST » ne fait que dire quel jour commence la semaine : il ne change pas la répétition.
        StringBuilder utile = new StringBuilder();
        for (String partie : regle.trim().toUpperCase(java.util.Locale.ROOT).split(";")) {
            if (!partie.isEmpty() && !partie.startsWith("WKST=")) {
                utile.append(utile.length() == 0 ? "" : ";").append(partie);
            }
        }
        for (Repetition simple : new Repetition[] {JOUR, SEMAINE, MOIS, AN}) {
            if (simple.regle.contentEquals(utile)) {
                return simple;
            }
        }
        return AUTRE;
    }
}
