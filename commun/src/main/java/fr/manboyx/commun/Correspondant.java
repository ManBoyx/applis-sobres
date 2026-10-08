package fr.manboyx.commun;

/** Un numéro du carnet d'adresses, avec le nom de son contact. */
public final class Correspondant {
    public final String nom;
    public final String numero;
    public final boolean favori;

    public Correspondant(String nom, String numero, boolean favori) {
        this.nom = nom == null ? "" : nom;
        this.numero = numero == null ? "" : numero;
        this.favori = favori;
    }
}
