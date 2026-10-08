package fr.manboyx.galerie;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import fr.manboyx.galerie.logique.Media;

import java.util.ArrayList;
import java.util.List;

/** Des photos fabriquées en mémoire, pour piloter les écrans sans téléphone. */
final class PhotosMemoire implements Depot {
    final List<Media> medias = new ArrayList<>();
    /** Vrai : la suppression se fait tout de suite. Faux : comme sur Android 11+, elle attend l'accord d'Android. */
    boolean suppressionDirecte = true;
    Media aSupprimer;
    int imagesLues;

    void ajouter(long id, boolean video, String album, long quand) {
        medias.add(new Media(id, "content://media/external/" + (video ? "video" : "images") + "/media/" + id, video, album, quand,
                (video ? "VID_" : "IMG_") + id + (video ? ".mp4" : ".jpg"), 2_500_000, video ? "video/mp4" : "image/jpeg"));
    }

    /** Un dégradé dont les couleurs dépendent du numéro : chaque photo est reconnaissable. */
    private static Bitmap dessiner(long id, int largeur, int hauteur) {
        Bitmap image = Bitmap.createBitmap(largeur, hauteur, Bitmap.Config.ARGB_8888);
        Paint pinceau = new Paint();
        int[] teintes = {0xFF1D5FA8, 0xFFE0A030, 0xFF8A3A8A, 0xFFC04A3A, 0xFF2A8A8A, 0xFF707070};
        pinceau.setShader(new LinearGradient(0, 0, largeur, hauteur,
                teintes[(int) Math.floorMod(id, (long) teintes.length)], teintes[(int) Math.floorMod(id + 2, (long) teintes.length)], Shader.TileMode.CLAMP));
        new Canvas(image).drawRect(0, 0, largeur, hauteur, pinceau);
        return image;
    }

    @Override
    public List<Media> medias() {
        return new ArrayList<>(medias);
    }

    @Override
    public Bitmap vignette(Media media, int cote) {
        return dessiner(media.id, 96, 96);
    }

    @Override
    public Bitmap image(Media media, int cote) {
        imagesLues++;
        return media.id == 404 ? null : dessiner(media.id, 400, 300);
    }

    @Override
    public boolean confirmeLuiMeme() {
        return !suppressionDirecte;
    }

    @Override
    public boolean supprimer(Activity activite, Media media, int code) {
        if (!suppressionDirecte) {
            aSupprimer = media;
            return false;
        }
        medias.removeIf(m -> m.id == media.id);
        return true;
    }
}
