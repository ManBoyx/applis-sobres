package fr.manboyx.calendrier.logique;

/** Un événement tel qu'il tombe un jour donné (une occurrence, pour un événement répété). */
public final class Evenement {
    public final long id;
    public final String titre;
    /** Début et fin en millisecondes. Pour un événement « toute la journée » : minuit UTC du premier jour, minuit UTC du lendemain du dernier. */
    public final long debut;
    public final long fin;
    public final boolean journee;
    public final int couleur;
    public final String lieu;

    public Evenement(long id, String titre, long debut, long fin, boolean journee, int couleur, String lieu) {
        this.id = id;
        this.titre = titre == null || titre.trim().isEmpty() ? "Sans titre" : titre;
        this.debut = debut;
        this.fin = Math.max(debut, fin);
        this.journee = journee;
        this.couleur = couleur;
        this.lieu = lieu == null ? "" : lieu;
    }
}
