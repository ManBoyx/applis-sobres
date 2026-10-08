package fr.manboyx.messages.logique;

/** Un SMS. */
public final class Message {
    public final long id;
    /** Numéro de la conversation tel que le téléphone le range. */
    public final long fil;
    /** Le numéro (ou le nom d'expéditeur) de l'autre personne. */
    public final String adresse;
    public final String texte;
    public final long quand;
    /** Vrai pour un message reçu, faux pour un message envoyé. */
    public final boolean recu;
    public final boolean lu;

    public Message(long id, long fil, String adresse, String texte, long quand, boolean recu, boolean lu) {
        this.id = id;
        this.fil = fil;
        this.adresse = adresse == null ? "" : adresse;
        this.texte = texte == null ? "" : texte;
        this.quand = quand;
        this.recu = recu;
        this.lu = lu;
    }
}
