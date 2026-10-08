package fr.manboyx.appareil.logique;

/** Les trois réglages du flash, que le bouton fait défiler. */
public enum Flash {
    AUTO("Flash auto"),
    OUI("Flash activé"),
    NON("Flash coupé");

    public final String libelle;

    Flash(String libelle) {
        this.libelle = libelle;
    }

    public Flash suivant() {
        return values()[(ordinal() + 1) % values().length];
    }
}
