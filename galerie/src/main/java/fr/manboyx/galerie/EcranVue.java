package fr.manboyx.galerie;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.manboyx.commun.Dates;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;
import fr.manboyx.galerie.logique.Media;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/** Une photo en plein écran. Une seule grande image est en mémoire à la fois. */
final class EcranVue extends Ecran {
    private static final int DEMANDE_SUPPRESSION = 2;
    private static final int BLANC = 0xFFFFFFFF;

    private final List<Media> medias;
    private int rang;
    private Media enAttente;
    private VueZoom vue;
    private LinearLayout barre;
    private TextView position;
    private TextView infos;
    private TextView supprimer;
    private Button lire;

    EcranVue(List<Media> medias, int rang) {
        this.medias = medias;
        this.rang = rang;
    }

    private Depot depot() {
        return ((GalerieActivity) activite).depot();
    }

    Media courant() {
        return medias.get(rang);
    }

    @Override
    protected View creer() {
        FrameLayout cadre = new FrameLayout(activite);
        cadre.setBackgroundColor(0xFF000000);
        vue = new VueZoom(activite, new VueZoom.Ecoute() {
            @Override
            public void appui() {
                boolean visibles = barre.getVisibility() == View.VISIBLE;
                barre.setVisibility(visibles ? View.GONE : View.VISIBLE);
                infos.setVisibility(visibles ? View.GONE : View.VISIBLE);
            }

            @Override
            public void glissement(int sens) {
                aller(sens);
            }
        });
        cadre.addView(vue, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));

        // Texte blanc sur fond sombre quel que soit le thème : on est toujours sur une photo.
        barre = Ui.rangee(activite);
        barre.setBackgroundColor(0x99000000);
        barre.setPadding(Ui.dp(activite, 4), 0, Ui.dp(activite, 4), 0);
        barre.addView(action("←", "Retour", v -> activite.onBackPressed()));
        position = Ui.texte(activite, "", 16, fr.manboyx.commun.R.color.texte);
        position.setTextColor(BLANC);
        position.setPadding(Ui.dp(activite, 8), 0, 0, 0);
        barre.addView(position, Ui.reste(true));
        barre.addView(action("Partager", "Partager", v -> partager()));
        supprimer = action("Supprimer", "Supprimer", v -> supprimer());
        barre.addView(supprimer);
        cadre.addView(barre, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.dp(activite, 56), Gravity.TOP));

        lire = Ui.bouton(activite, "Lire la vidéo", true);
        lire.setOnClickListener(v -> ouvrirVideo());
        cadre.addView(lire, new FrameLayout.LayoutParams(Ui.JUSTE, Ui.JUSTE, Gravity.CENTER));

        infos = Ui.texte(activite, "", 14, fr.manboyx.commun.R.color.texte);
        infos.setTextColor(BLANC);
        infos.setBackgroundColor(0x99000000);
        Ui.marges(infos, activite, 16, 10, 16, 10);
        cadre.addView(infos, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE, Gravity.BOTTOM));
        return cadre;
    }

    private TextView action(String libelle, String description, View.OnClickListener action) {
        TextView touche = Ui.touche(activite, libelle, description);
        touche.setTextColor(BLANC);
        if (libelle.length() > 1) {
            touche.setTextSize(15);
        }
        touche.setOnClickListener(action);
        return touche;
    }

    @Override
    public void afficher() {
        charger();
    }

    @Override
    public void masquer() {
        // Rien ne reste en mémoire quand l'écran n'est plus visible.
        vue.montrer(null);
    }

    /** Passe à la photo suivante (+1) ou précédente (−1), sans dépasser les bouts de la liste. */
    void aller(int sens) {
        int cible = Math.max(0, Math.min(medias.size() - 1, rang + sens));
        if (cible != rang) {
            rang = cible;
            charger();
        }
    }

    private void charger() {
        Media media = courant();
        position.setText(medias.size() > 1 ? (rang + 1) + " / " + medias.size() : "");
        lire.setVisibility(media.video ? View.VISIBLE : View.GONE);
        // Une image ouverte depuis une autre appli n'est pas à nous : on ne propose pas de la supprimer.
        supprimer.setVisibility(media.id >= 0 ? View.VISIBLE : View.GONE);
        StringBuilder texte = new StringBuilder(media.nom);
        if (media.quand > 0) {
            ZoneId zone = ZoneId.systemDefault();
            texte.append(texte.length() > 0 ? "\n" : "")
                    .append(Dates.jour(media.quand, System.currentTimeMillis(), zone)).append(" à ").append(Dates.heure(media.quand, zone));
        }
        if (media.taille > 0) {
            texte.append(" · ").append(taille(media.taille));
        }
        infos.setText(texte);
        infos.setVisibility(texte.length() > 0 && barre.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        vue.montrer(null);
        Depot depot = depot();
        int cote = Math.max(activite.getResources().getDisplayMetrics().widthPixels, activite.getResources().getDisplayMetrics().heightPixels);
        Fond.lancer(() -> depot.image(media, cote), image -> {
            // Entre-temps on a pu passer à une autre photo.
            if (media == courant()) {
                vue.montrer(image);
                if (image == null) {
                    Dialogues.dire(activite, "Cette image ne peut pas être affichée.");
                }
            }
        });
    }

    private static String taille(long octets) {
        if (octets < 1024 * 1024) {
            return Math.max(1, octets / 1024) + " Ko";
        }
        return String.format(Locale.FRANCE, "%.1f Mo", octets / (1024.0 * 1024));
    }

    private void partager() {
        Media media = courant();
        Intent envoi = new Intent(Intent.ACTION_SEND)
                .setType(media.type)
                .putExtra(Intent.EXTRA_STREAM, Uri.parse(media.adresse))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activite.startActivity(Intent.createChooser(envoi, "Partager"));
        } catch (ActivityNotFoundException aucune) {
            Dialogues.dire(activite, "Aucune appli du téléphone ne peut recevoir cette image.");
        }
    }

    private void ouvrirVideo() {
        Media media = courant();
        Intent lecture = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(Uri.parse(media.adresse), media.type)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activite.startActivity(lecture);
        } catch (ActivityNotFoundException aucune) {
            Dialogues.dire(activite, "Aucune appli du téléphone ne sait lire cette vidéo.");
        }
    }

    private void supprimer() {
        if (depot().confirmeLuiMeme()) {
            lancerSuppression();
        } else {
            Dialogues.confirmer(activite, "Supprimer ?", "Le fichier sera retiré du téléphone. Il n'y a pas de corbeille.",
                    "Supprimer", this::lancerSuppression);
        }
    }

    private void lancerSuppression() {
        Media media = courant();
        try {
            if (depot().supprimer(activite, media, DEMANDE_SUPPRESSION)) {
                retirer(media);
            } else {
                enAttente = media;
            }
        } catch (RuntimeException refus) {
            Dialogues.dire(activite, "Ce fichier n'a pas pu être supprimé.");
        }
    }

    @Override
    public void resultat(int code, int resultat, Intent donnees) {
        if (code == DEMANDE_SUPPRESSION && enAttente != null) {
            Media media = enAttente;
            enAttente = null;
            if (resultat == Activity.RESULT_OK) {
                retirer(media);
            }
        }
    }

    private void retirer(Media media) {
        medias.remove(media);
        if (medias.isEmpty()) {
            activite.fermer();
            return;
        }
        rang = Math.min(rang, medias.size() - 1);
        charger();
    }
}
