package fr.manboyx.notes.logique;

import fr.manboyx.commun.Textes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Les notes, rangées chacune dans un petit fichier texte du dossier privé de l'appli. */
public final class Carnet {
    private static final int LONGUEUR_TITRE = 80;
    private static final int LONGUEUR_EXTRAIT = 120;

    private final File dossier;

    public Carnet(File dossier) {
        this.dossier = dossier;
    }

    private File fichier(long id) {
        return new File(dossier, id + ".txt");
    }

    private static String lireFichier(File fichier) {
        try {
            return new String(Files.readAllBytes(fichier.toPath()), StandardCharsets.UTF_8);
        } catch (IOException illisible) {
            return "";
        }
    }

    private static String couper(String texte, int longueur) {
        return texte.length() <= longueur ? texte : texte.substring(0, longueur - 1) + "…";
    }

    /** Les notes dont le texte contient `recherche`, la plus récemment modifiée en tête. Recherche vide : toutes. */
    public List<Note> lister(String recherche) {
        List<Note> notes = new ArrayList<>();
        File[] fichiers = dossier.listFiles();
        if (fichiers == null) {
            return notes;
        }
        for (File fichier : fichiers) {
            String nom = fichier.getName();
            // Seuls les fichiers « nombre.txt » sont des notes ; le reste (écriture interrompue…) est ignoré.
            if (!nom.matches("[0-9]+\\.txt")) {
                continue;
            }
            String texte = lireFichier(fichier);
            if (!Textes.contient(texte, recherche)) {
                continue;
            }
            String titre = "";
            StringBuilder suite = new StringBuilder();
            for (String ligne : texte.split("\\R")) {
                String propre = ligne.trim();
                if (propre.isEmpty()) {
                    continue;
                }
                if (titre.isEmpty()) {
                    titre = propre;
                } else if (suite.length() < LONGUEUR_EXTRAIT) {
                    suite.append(suite.length() == 0 ? "" : " ").append(propre);
                }
            }
            long id = Long.parseLong(nom.substring(0, nom.length() - 4));
            notes.add(new Note(id, couper(titre, LONGUEUR_TITRE), couper(suite.toString(), LONGUEUR_EXTRAIT), fichier.lastModified()));
        }
        notes.sort(Comparator.comparingLong((Note n) -> n.modifiee).thenComparingLong(n -> n.id).reversed());
        return notes;
    }

    /** Le texte entier de la note, ou une chaîne vide si elle n'existe pas. */
    public String lire(long id) {
        File fichier = fichier(id);
        return fichier.isFile() ? lireFichier(fichier) : "";
    }

    /**
     * Enregistre le texte. `id` à 0 : nouvelle note. Un texte vide n'est jamais
     * gardé : la note est supprimée, ou pas créée. Rend l'identifiant, ou 0 s'il
     * n'y a plus de note.
     */
    public long enregistrer(long id, String texte) throws IOException {
        if (texte == null || texte.trim().isEmpty()) {
            if (id != 0) {
                supprimer(id);
            }
            return 0;
        }
        if (!dossier.isDirectory() && !dossier.mkdirs()) {
            throw new IOException("Dossier des notes inaccessible");
        }
        long numero = id;
        if (numero == 0) {
            numero = System.currentTimeMillis();
            while (fichier(numero).exists()) {
                numero++;
            }
        } else if (texte.equals(lire(numero))) {
            // Rien n'a changé : on ne touche pas au fichier, sa date de modification reste vraie.
            return numero;
        }
        // Écriture à côté puis remplacement : une coupure ne laisse jamais une note à moitié écrite.
        File provisoire = new File(dossier, numero + ".txt.tmp");
        Files.write(provisoire.toPath(), texte.getBytes(StandardCharsets.UTF_8));
        Files.move(provisoire.toPath(), fichier(numero).toPath(), StandardCopyOption.REPLACE_EXISTING);
        return numero;
    }

    public void supprimer(long id) {
        fichier(id).delete();
    }
}
