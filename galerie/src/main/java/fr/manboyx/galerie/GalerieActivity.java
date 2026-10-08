package fr.manboyx.galerie;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Ui;
import fr.manboyx.galerie.logique.Media;

import java.util.ArrayList;
import java.util.List;

public class GalerieActivity extends ActiviteSobre {
    private Depot depot;
    private Vignettes vignettes;

    /** Les tests le remplacent par des images en mémoire. */
    public Depot depot() {
        if (depot == null) {
            depot = new DepotAndroid(getContentResolver());
        }
        return depot;
    }

    Vignettes vignettes() {
        if (vignettes == null) {
            vignettes = new Vignettes(depot(), Ui.dp(this, 120));
        }
        return vignettes;
    }

    /** Les autorisations à demander, qui changent de nom selon la version d'Android. */
    static String[] demandees() {
        if (Build.VERSION.SDK_INT >= 34) {
            return new String[] {Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[] {Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        }
        return new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    /** Vrai si l'appli peut voir au moins une partie des photos (Android 14 permet d'en choisir quelques-unes). */
    public boolean acces() {
        if (Build.VERSION.SDK_INT >= 34 && Autorisations.accordees(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) {
            return true;
        }
        return Autorisations.accordees(this, demandees()[0]);
    }

    @Override
    protected Ecran accueil() {
        return new EcranGalerie();
    }

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        // Une autre appli demande d'afficher une image : on la montre seule, sans la liste.
        Intent demande = getIntent();
        Uri cible = demande == null ? null : demande.getData();
        if (cible != null && Intent.ACTION_VIEW.equals(demande.getAction())) {
            List<Media> seule = new ArrayList<>();
            String type = demande.getType();
            seule.add(new Media(-1, cible.toString(), type != null && type.startsWith("video/"), "", 0, "", 0, type));
            remplacer(new EcranVue(seule, 0));
        }
    }
}
