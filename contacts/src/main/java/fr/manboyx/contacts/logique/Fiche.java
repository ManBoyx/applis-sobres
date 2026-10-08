package fr.manboyx.contacts.logique;

/** Un contact tel qu'il apparaît dans la liste : juste de quoi l'afficher. */
public final class Fiche {
    public final long id;
    public final String nom;
    public final boolean favori;

    public Fiche(long id, String nom, boolean favori) {
        this.id = id;
        this.nom = nom == null ? "" : nom;
        this.favori = favori;
    }
}
