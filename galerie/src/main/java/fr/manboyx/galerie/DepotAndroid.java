package fr.manboyx.galerie;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import fr.manboyx.galerie.logique.Echantillon;
import fr.manboyx.galerie.logique.Media;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DepotAndroid implements Depot {
    private static final String[] COLONNES = {
            MediaStore.MediaColumns._ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE};

    private final ContentResolver base;

    public DepotAndroid(ContentResolver base) {
        this.base = base;
    }

    @Override
    public List<Media> medias() {
        List<Media> medias = new ArrayList<>();
        lire(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, medias);
        lire(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, medias);
        return medias;
    }

    private void lire(Uri collection, boolean video, List<Media> medias) {
        try (Cursor c = base.query(collection, COLONNES, null, null, null)) {
            while (c != null && c.moveToNext()) {
                long id = c.getLong(0);
                // La date de prise de vue manque souvent (captures d'écran, images reçues) : à défaut, la date du fichier.
                long quand = c.getLong(2) > 0 ? c.getLong(2) : c.getLong(3) * 1000;
                medias.add(new Media(id, ContentUris.withAppendedId(collection, id).toString(), video,
                        c.getString(1), quand, c.getString(4), c.getLong(5), c.getString(6)));
            }
        } catch (RuntimeException collectionAbsente) {
            // Téléphone sans mémoire partagée montée : pas de photos à lister.
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public Bitmap vignette(Media media, int cote) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                return base.loadThumbnail(Uri.parse(media.adresse), new Size(cote, cote), null);
            }
            return media.video
                    ? MediaStore.Video.Thumbnails.getThumbnail(base, media.id, MediaStore.Video.Thumbnails.MINI_KIND, null)
                    : MediaStore.Images.Thumbnails.getThumbnail(base, media.id, MediaStore.Images.Thumbnails.MINI_KIND, null);
        } catch (IOException | RuntimeException illisible) {
            return null;
        }
    }

    @Override
    public Bitmap image(Media media, int cote) {
        if (media.video) {
            return vignette(media, cote);
        }
        Uri adresse = Uri.parse(media.adresse);
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                // Lue directement à taille réduite, et déjà tournée dans le bon sens.
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(base, adresse), (decodeur, infos, source) ->
                        decodeur.setTargetSampleSize(Echantillon.pour(infos.getSize().getWidth(), infos.getSize().getHeight(), cote)));
            }
            return ancienneLecture(adresse, cote);
        } catch (IOException | RuntimeException illisible) {
            return null;
        }
    }

    /** Android 8 : lecture en deux temps (dimensions, puis image réduite), et rotation à la main. */
    private Bitmap ancienneLecture(Uri adresse, int cote) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream flux = base.openInputStream(adresse)) {
            BitmapFactory.decodeStream(flux, null, options);
        }
        options.inSampleSize = Echantillon.pour(options.outWidth, options.outHeight, cote);
        options.inJustDecodeBounds = false;
        Bitmap image;
        try (InputStream flux = base.openInputStream(adresse)) {
            image = BitmapFactory.decodeStream(flux, null, options);
        }
        if (image == null) {
            return null;
        }
        int degres = 0;
        try (InputStream flux = base.openInputStream(adresse)) {
            if (flux != null) {
                int sens = new ExifInterface(flux).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                degres = sens == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                        : sens == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                        : sens == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            }
        }
        if (degres == 0) {
            return image;
        }
        Matrix rotation = new Matrix();
        rotation.postRotate(degres);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), rotation, true);
    }

    @Override
    public boolean confirmeLuiMeme() {
        return Build.VERSION.SDK_INT >= 30;
    }

    @Override
    public boolean supprimer(Activity activite, Media media, int code) {
        Uri adresse = Uri.parse(media.adresse);
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                PendingIntent demande = MediaStore.createDeleteRequest(base, Collections.singletonList(adresse));
                activite.startIntentSenderForResult(demande.getIntentSender(), code, null, 0, 0, 0);
                return false;
            }
            try {
                return base.delete(adresse, null, null) > 0;
            } catch (SecurityException refus) {
                // Android 10 : une photo prise par une autre appli demande l'accord de l'utilisateur.
                if (Build.VERSION.SDK_INT == 29 && refus instanceof RecoverableSecurityException) {
                    IntentSender accord = ((RecoverableSecurityException) refus).getUserAction().getActionIntent().getIntentSender();
                    activite.startIntentSenderForResult(accord, code, null, 0, 0, 0);
                    return false;
                }
                throw refus;
            }
        } catch (IntentSender.SendIntentException echec) {
            throw new IllegalStateException(echec);
        }
    }
}
