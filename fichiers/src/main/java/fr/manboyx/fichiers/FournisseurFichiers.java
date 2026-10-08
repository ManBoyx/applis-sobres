package fr.manboyx.fichiers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import fr.manboyx.fichiers.logique.Dossier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Prête un fichier, en lecture seule, à l'appli choisie pour l'ouvrir ou le
 * recevoir. Rien n'est accessible sans que l'utilisateur ait lui-même demandé
 * « Ouvrir » ou « Partager » sur ce fichier.
 */
public final class FournisseurFichiers extends ContentProvider {
    static final String AUTORITE = "fr.manboyx.fichiers.partage";

    static Uri adresse(File fichier) {
        return new Uri.Builder().scheme("content").authority(AUTORITE).path(fichier.getAbsolutePath()).build();
    }

    static String type(String nom) {
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Dossier.extension(nom));
        return type == null ? "*/*" : type;
    }

    /** Vrai si `fichier` est un vrai fichier situé sous `racine`, liens et « .. » résolus. */
    static boolean permis(File racine, File fichier) {
        try {
            String base = racine.getCanonicalPath();
            String chemin = fichier.getCanonicalPath();
            return fichier.isFile() && chemin.startsWith(base + File.separator);
        } catch (IOException illisible) {
            return false;
        }
    }

    private static File fichier(Uri adresse) throws FileNotFoundException {
        String chemin = adresse.getPath();
        File fichier = chemin == null ? null : new File(chemin);
        if (fichier == null || !permis(Environment.getExternalStorageDirectory(), fichier)) {
            throw new FileNotFoundException("Fichier indisponible");
        }
        return fichier;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri adresse, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Lecture seule");
        }
        return ParcelFileDescriptor.open(fichier(adresse), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri adresse, String[] colonnes, String condition, String[] arguments, String tri) {
        String[] demandees = colonnes != null ? colonnes : new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor reponse = new MatrixCursor(demandees);
        try {
            File fichier = fichier(adresse);
            Object[] ligne = new Object[demandees.length];
            for (int i = 0; i < demandees.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(demandees[i])) {
                    ligne[i] = fichier.getName();
                } else if (OpenableColumns.SIZE.equals(demandees[i])) {
                    ligne[i] = fichier.length();
                }
            }
            reponse.addRow(ligne);
        } catch (FileNotFoundException absent) {
            // Réponse vide.
        }
        return reponse;
    }

    @Override
    public String getType(Uri adresse) {
        String chemin = adresse.getPath();
        return chemin == null ? null : type(new File(chemin).getName());
    }

    @Override
    public Uri insert(Uri adresse, ContentValues valeurs) {
        throw new UnsupportedOperationException("Lecture seule");
    }

    @Override
    public int update(Uri adresse, ContentValues valeurs, String condition, String[] arguments) {
        throw new UnsupportedOperationException("Lecture seule");
    }

    @Override
    public int delete(Uri adresse, String condition, String[] arguments) {
        throw new UnsupportedOperationException("Lecture seule");
    }
}
