package fr.manboyx.appareil;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Range les photos dans le dossier habituel (DCIM/Camera) : la Galerie et les autres applis les voient aussitôt. */
final class RangementAndroid implements Rangement {
    private final ContentResolver base;

    RangementAndroid(ContentResolver base) {
        this.base = base;
    }

    @Override
    public Uri enregistrer(byte[] jpeg, String nom) throws IOException {
        ContentValues fiche = new ContentValues();
        fiche.put(MediaStore.MediaColumns.DISPLAY_NAME, nom);
        fiche.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        fiche.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        if (Build.VERSION.SDK_INT < 29) {
            return ancienneMethode(jpeg, nom, fiche);
        }
        fiche.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera");
        // « En cours » : la photo n'apparaît dans les autres applis qu'une fois entièrement écrite.
        fiche.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri adresse = base.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), fiche);
        if (adresse == null) {
            throw new IOException("Le téléphone a refusé d'enregistrer la photo");
        }
        try (OutputStream sortie = base.openOutputStream(adresse)) {
            if (sortie == null) {
                throw new IOException("Écriture impossible");
            }
            sortie.write(jpeg);
        } catch (IOException | RuntimeException echec) {
            base.delete(adresse, null, null);
            throw new IOException(echec);
        }
        ContentValues terminee = new ContentValues();
        terminee.put(MediaStore.MediaColumns.IS_PENDING, 0);
        base.update(adresse, terminee, null, null);
        return adresse;
    }

    /** Android 9 et avant : on écrit le fichier soi-même, puis on le signale au téléphone. */
    @SuppressWarnings("deprecation")
    private Uri ancienneMethode(byte[] jpeg, String nom, ContentValues fiche) throws IOException {
        File dossier = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (!dossier.isDirectory() && !dossier.mkdirs()) {
            throw new IOException("Dossier des photos inaccessible");
        }
        File fichier = new File(dossier, nom);
        try (FileOutputStream sortie = new FileOutputStream(fichier)) {
            sortie.write(jpeg);
        }
        fiche.put(MediaStore.MediaColumns.DATA, fichier.getAbsolutePath());
        Uri adresse = base.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fiche);
        if (adresse == null) {
            throw new IOException("Le téléphone a refusé d'enregistrer la photo");
        }
        return adresse;
    }
}
