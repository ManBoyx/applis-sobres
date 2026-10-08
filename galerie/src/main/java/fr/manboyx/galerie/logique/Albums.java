package fr.manboyx.galerie.logique;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Range les photos et vidéos par album. */
public final class Albums {
    private Albums() {}

    public static final class Album {
        public final String nom;
        public final int nombre;
        /** L'élément le plus récent, qui sert d'image à l'album. */
        public final Media couverture;

        Album(String nom, int nombre, Media couverture) {
            this.nom = nom;
            this.nombre = nombre;
            this.couverture = couverture;
        }
    }

    /** Du plus récent au plus ancien, quel que soit l'ordre reçu. */
    public static List<Media> trier(List<Media> medias) {
        List<Media> tries = new ArrayList<>(medias);
        tries.sort(Comparator.comparingLong((Media m) -> m.quand).thenComparingLong(m -> m.id).reversed());
        return tries;
    }

    /** Un album par dossier, celui qui a reçu une photo le plus récemment en tête. */
    public static List<Album> regrouper(List<Media> medias) {
        // Parcourus du plus récent au plus ancien : le premier rencontré d'un album en est la couverture.
        Map<String, int[]> nombres = new LinkedHashMap<>();
        Map<String, Media> couvertures = new LinkedHashMap<>();
        for (Media media : trier(medias)) {
            if (!couvertures.containsKey(media.album)) {
                couvertures.put(media.album, media);
                nombres.put(media.album, new int[1]);
            }
            nombres.get(media.album)[0]++;
        }
        List<Album> albums = new ArrayList<>();
        for (Map.Entry<String, Media> entree : couvertures.entrySet()) {
            albums.add(new Album(entree.getKey(), nombres.get(entree.getKey())[0], entree.getValue()));
        }
        return albums;
    }

    /** Le contenu d'un album, du plus récent au plus ancien. */
    public static List<Media> contenu(List<Media> medias, String album) {
        List<Media> contenu = new ArrayList<>();
        for (Media media : trier(medias)) {
            if (media.album.equals(album)) {
                contenu.add(media);
            }
        }
        return contenu;
    }
}
