package fr.manboyx.fichiers.logique;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Fabrique et efface les dossiers d'essai. */
final class OutilsDossier {
    private OutilsDossier() {}

    static File temporaire() throws IOException {
        return Files.createTempDirectory("fichiers-essai").toFile();
    }

    static File ecrire(File dossier, String nom, String contenu) throws IOException {
        File fichier = new File(dossier, nom);
        fichier.getParentFile().mkdirs();
        Files.write(fichier.toPath(), contenu.getBytes(StandardCharsets.UTF_8));
        return fichier;
    }

    static String lire(File fichier) throws IOException {
        return new String(Files.readAllBytes(fichier.toPath()), StandardCharsets.UTF_8);
    }

    static void effacer(File fichier) {
        File[] contenu = fichier.listFiles();
        if (contenu != null) {
            for (File f : contenu) {
                effacer(f);
            }
        }
        fichier.delete();
    }
}
