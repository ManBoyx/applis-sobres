package fr.manboyx.galerie.logique;

/** Une photo ou une vidéo du téléphone. */
public final class Media {
    public final long id;
    /** L'adresse « content://… » à laquelle le téléphone donne accès au fichier. */
    public final String adresse;
    public final boolean video;
    /** Le dossier où elle se trouve (« Camera », « Screenshots », « WhatsApp Images »…). */
    public final String album;
    public final long quand;
    public final String nom;
    public final long taille;
    /** Type du fichier (« image/jpeg »…). */
    public final String type;

    public Media(long id, String adresse, boolean video, String album, long quand, String nom, long taille, String type) {
        this.id = id;
        this.adresse = adresse;
        this.video = video;
        this.album = album == null || album.trim().isEmpty() ? "Sans nom" : album;
        this.quand = quand;
        this.nom = nom == null ? "" : nom;
        this.taille = taille;
        this.type = type == null || type.isEmpty() ? (video ? "video/*" : "image/*") : type;
    }
}
