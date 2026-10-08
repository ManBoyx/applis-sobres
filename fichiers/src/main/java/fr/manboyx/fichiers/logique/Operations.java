package fr.manboyx.fichiers.logique;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Copier, déplacer, renommer, supprimer, créer un dossier. Chaque refus est expliqué en français. */
public final class Operations {
    private Operations() {}

    /** Le nom proposé n'est pas utilisable : vide, avec « / », déjà pris… `null` si le nom convient. */
    public static String erreurDeNom(File dossier, String nom) {
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty()) {
            return "Le nom ne peut pas être vide.";
        }
        if (propre.indexOf('/') >= 0 || propre.indexOf('\0') >= 0 || propre.equals(".") || propre.equals("..")) {
            return "Ce nom n'est pas utilisable.";
        }
        if (new File(dossier, propre).exists()) {
            return "« " + propre + " » existe déjà ici.";
        }
        return null;
    }

    private static void verifierNom(File dossier, String nom) throws IOException {
        String erreur = erreurDeNom(dossier, nom);
        if (erreur != null) {
            throw new IOException(erreur);
        }
    }

    public static File creerDossier(File parent, String nom) throws IOException {
        verifierNom(parent, nom);
        File dossier = new File(parent, nom.trim());
        if (!dossier.mkdir()) {
            throw new IOException("Le dossier n'a pas pu être créé.");
        }
        return dossier;
    }

    public static File renommer(File fichier, String nouveauNom) throws IOException {
        File parent = fichier.getParentFile();
        verifierNom(parent, nouveauNom);
        File renomme = new File(parent, nouveauNom.trim());
        if (!fichier.renameTo(renomme)) {
            throw new IOException("« " + fichier.getName() + " » n'a pas pu être renommé.");
        }
        return renomme;
    }

    /** Un nom libre dans `dossier` : `nom` lui-même, sinon « nom (2) », « nom (3) »… */
    private static File libre(File dossier, String nom) {
        File candidat = new File(dossier, nom);
        // Le point d'un fichier caché (« .config ») n'ouvre pas une extension.
        int point = nom.lastIndexOf('.');
        String base = point > 0 ? nom.substring(0, point) : nom;
        String extension = point > 0 ? nom.substring(point) : "";
        for (int numero = 2; candidat.exists(); numero++) {
            candidat = new File(dossier, base + " (" + numero + ")" + extension);
        }
        return candidat;
    }

    private static void refuserDansLuiMeme(File source, File cible) throws IOException {
        if (!source.isDirectory()) {
            return;
        }
        String dedans = source.getCanonicalPath();
        String vers = cible.getCanonicalPath();
        if (vers.equals(dedans) || vers.startsWith(dedans + File.separator)) {
            throw new IOException("Un dossier ne peut pas être placé dans lui-même.");
        }
    }

    /**
     * Copie le fichier ou le dossier (avec tout son contenu) dans `cible`. Si
     * le nom y est déjà pris, la copie s'appelle « nom (2) », « nom (3) »…
     * Rend la copie.
     */
    public static File copier(File source, File cible) throws IOException {
        refuserDansLuiMeme(source, cible);
        File copie = libre(cible, source.getName());
        recopier(source, copie);
        return copie;
    }

    private static void recopier(File source, File copie) throws IOException {
        if (!source.isDirectory()) {
            Files.copy(source.toPath(), copie.toPath());
            return;
        }
        if (!copie.mkdir()) {
            throw new IOException("Le dossier « " + copie.getName() + " » n'a pas pu être créé.");
        }
        File[] contenu = source.listFiles();
        if (contenu != null) {
            for (File enfant : contenu) {
                recopier(enfant, new File(copie, enfant.getName()));
            }
        }
    }

    /** Déplace dans `cible`. Sans effet si le fichier y est déjà. */
    public static File deplacer(File source, File cible) throws IOException {
        File parent = source.getParentFile();
        if (parent != null && parent.getCanonicalPath().equals(cible.getCanonicalPath())) {
            return source;
        }
        refuserDansLuiMeme(source, cible);
        File arrivee = libre(cible, source.getName());
        if (source.renameTo(arrivee)) {
            return arrivee;
        }
        // D'une mémoire à une autre (carte SD…), il faut copier puis effacer l'original.
        recopier(source, arrivee);
        supprimer(source);
        return arrivee;
    }

    /** Supprime le fichier, ou le dossier avec tout son contenu. */
    public static void supprimer(File fichier) throws IOException {
        // Un lien vers un dossier est effacé comme un fichier : on ne vide pas ce vers quoi il pointe.
        if (fichier.isDirectory() && !Files.isSymbolicLink(fichier.toPath())) {
            File[] contenu = fichier.listFiles();
            if (contenu != null) {
                for (File enfant : contenu) {
                    supprimer(enfant);
                }
            }
        }
        if (!fichier.delete() && fichier.exists()) {
            throw new IOException("« " + fichier.getName() + " » n'a pas pu être supprimé.");
        }
    }

    /** Nombre de fichiers et de sous-dossiers contenus, tous niveaux confondus. */
    public static int compter(File dossier) {
        int nombre = 0;
        File[] contenu = dossier.listFiles();
        if (contenu != null) {
            for (File enfant : contenu) {
                nombre++;
                if (enfant.isDirectory() && !Files.isSymbolicLink(enfant.toPath())) {
                    nombre += compter(enfant);
                }
            }
        }
        return nombre;
    }
}
