package fr.manboyx.galerie.logique;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AlbumsTest {
    private static Media media(long id, String album, long quand) {
        return new Media(id, "content://media/" + id, false, album, quand, "IMG_" + id + ".jpg", 1000, "image/jpeg");
    }

    private static final List<Media> TOUT = Arrays.asList(
            media(1, "Camera", 100),
            media(2, "Screenshots", 500),
            media(3, "Camera", 300),
            media(4, null, 50),
            media(5, "Camera", 200));

    private static List<Long> ids(List<Media> medias) {
        List<Long> ids = new ArrayList<>();
        for (Media m : medias) {
            ids.add(m.id);
        }
        return ids;
    }

    @Test
    public void lesPhotosSontRangeesDeLaPlusRecenteALaPlusAncienne() {
        assertEquals(Arrays.asList(2L, 3L, 5L, 1L, 4L), ids(Albums.trier(TOUT)));
    }

    @Test
    public void unAlbumParDossierAvecSonNombreEtSaPhotoLaPlusRecente() {
        List<Albums.Album> albums = Albums.regrouper(TOUT);
        assertEquals(3, albums.size());
        assertEquals("l'album qui a la photo la plus récente vient en premier", "Screenshots", albums.get(0).nom);
        assertEquals("Camera", albums.get(1).nom);
        assertEquals(3, albums.get(1).nombre);
        assertEquals(3, albums.get(1).couverture.id);
        assertEquals("Sans nom", albums.get(2).nom);
    }

    @Test
    public void leContenuDUnAlbumNeMelePasLesAutres() {
        assertEquals(Arrays.asList(3L, 5L, 1L), ids(Albums.contenu(TOUT, "Camera")));
        assertEquals(Collections.emptyList(), ids(Albums.contenu(TOUT, "Inconnu")));
    }

    @Test
    public void sansPhotoIlNYARien() {
        assertEquals(0, Albums.regrouper(Collections.emptyList()).size());
        assertEquals(0, Albums.trier(Collections.emptyList()).size());
    }
}
