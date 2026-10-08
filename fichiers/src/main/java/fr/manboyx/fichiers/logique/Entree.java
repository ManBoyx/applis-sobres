package fr.manboyx.fichiers.logique;

import java.io.File;

/** Un fichier ou un dossier tel qu'il apparaît dans la liste. */
public final class Entree {
    public final File fichier;
    public final String nom;
    public final boolean dossier;
    /** Taille en octets ; 0 pour un dossier. */
    public final long taille;
    public final long modifie;

    public Entree(File fichier) {
        this.fichier = fichier;
        this.nom = fichier.getName();
        this.dossier = fichier.isDirectory();
        this.taille = dossier ? 0 : fichier.length();
        this.modifie = fichier.lastModified();
    }
}
