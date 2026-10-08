package fr.manboyx.telephone.logique;

/** Une ligne du journal d'appels. */
public final class Appel {
    public static final int ENTRANT = 1;
    public static final int SORTANT = 2;
    public static final int MANQUE = 3;

    public final String numero;
    /** Nom connu du téléphone pour ce numéro, ou chaîne vide. */
    public final String nom;
    public final int sens;
    public final long quand;
    public final long secondes;

    public Appel(String numero, String nom, int sens, long quand, long secondes) {
        this.numero = numero == null ? "" : numero;
        this.nom = nom == null ? "" : nom;
        this.sens = sens;
        this.quand = quand;
        this.secondes = secondes;
    }
}
