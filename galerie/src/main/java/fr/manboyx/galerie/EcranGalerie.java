package fr.manboyx.galerie;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Onglets;
import fr.manboyx.commun.Ui;
import fr.manboyx.galerie.logique.Albums;
import fr.manboyx.galerie.logique.Media;

import java.util.ArrayList;
import java.util.List;

/** L'écran d'accueil : toutes les photos, ou la liste des albums. */
final class EcranGalerie extends Ecran {
    private static final int DEMANDE = 1;
    private static final int PHOTOS = 0;

    private List<Media> medias = new ArrayList<>();
    private List<Albums.Album> albums = new ArrayList<>();
    private final Lignes adaptateurAlbums = new Lignes();
    private Grille grille;
    private ListView listeAlbums;
    private Onglets onglets;
    private TextView vide;
    private View explication;

    private GalerieActivity appli() {
        return (GalerieActivity) activite;
    }

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        FrameLayout cadre = new FrameLayout(activite);
        grille = new Grille(activite, appli().vignettes(), rang -> activite.ouvrir(new EcranVue(new ArrayList<>(grille.medias()), rang)));
        cadre.addView(grille.vue, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        listeAlbums = new ListView(activite);
        listeAlbums.setDivider(null);
        listeAlbums.setAdapter(adaptateurAlbums);
        listeAlbums.setOnItemClickListener((parent, vue, rang, id) -> activite.ouvrir(new EcranAlbum(albums.get(rang).nom)));
        cadre.addView(listeAlbums, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        vide = Ui.vide(activite, "Aucune photo ni vidéo sur ce téléphone.");
        cadre.addView(vide, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
        explication = Autorisations.explication(activite,
                "Pour afficher vos photos et vidéos, l'appli a besoin d'y accéder. Elles restent sur le téléphone.",
                DEMANDE, GalerieActivity.demandees());
        cadre.addView(explication, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        colonne.addView(cadre, Ui.reste(false));
        colonne.addView(Ui.trait(activite));
        onglets = new Onglets(activite, new String[] {"Photos", "Albums"}, rang -> montrer());
        colonne.addView(onglets, Ui.place(Ui.PLEIN, Ui.JUSTE));
        onglets.choisir(PHOTOS);
        return colonne;
    }

    private void montrer() {
        boolean autorise = appli().acces();
        boolean photos = onglets.courant() == PHOTOS;
        explication.setVisibility(autorise ? View.GONE : View.VISIBLE);
        grille.vue.setVisibility(autorise && photos ? View.VISIBLE : View.GONE);
        listeAlbums.setVisibility(autorise && !photos ? View.VISIBLE : View.GONE);
        vide.setVisibility(autorise && medias.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void afficher() {
        montrer();
        if (!appli().acces()) {
            return;
        }
        Depot depot = appli().depot();
        Fond.lancer(() -> Albums.trier(depot.medias()), lus -> {
            medias = lus;
            albums = Albums.regrouper(lus);
            grille.montrer(lus);
            adaptateurAlbums.notifyDataSetChanged();
            montrer();
        });
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        afficher();
    }

    private final class Lignes extends BaseAdapter {
        @Override
        public int getCount() {
            return albums.size();
        }

        @Override
        public Object getItem(int rang) {
            return albums.get(rang);
        }

        @Override
        public long getItemId(int rang) {
            return rang;
        }

        @Override
        public View getView(int rang, View recyclee, ViewGroup parent) {
            LinearLayout rangee = (LinearLayout) recyclee;
            if (rangee == null) {
                rangee = Ui.rangee(activite);
                Ui.marges(rangee, activite, 16, 8, 16, 8);
                rangee.addView(new Carre(activite), Ui.place(Ui.dp(activite, 64), Ui.dp(activite, 64)));
                LinearLayout textes = Ui.colonne(activite);
                textes.setPadding(Ui.dp(activite, 16), 0, 0, 0);
                textes.addView(Ui.uneLigne(Ui.texte(activite, "")));
                textes.addView(Ui.discret(activite, ""));
                rangee.addView(textes, Ui.reste(true));
            }
            Albums.Album album = albums.get(rang);
            appli().vignettes().poser((Carre) rangee.getChildAt(0), album.couverture);
            LinearLayout textes = (LinearLayout) rangee.getChildAt(1);
            ((TextView) textes.getChildAt(0)).setText(album.nom);
            ((TextView) textes.getChildAt(1)).setText(album.nombre == 1 ? "1 élément" : album.nombre + " éléments");
            return rangee;
        }
    }
}
