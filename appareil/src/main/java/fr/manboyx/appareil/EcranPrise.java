package fr.manboyx.appareil;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.manboyx.appareil.logique.Flash;
import fr.manboyx.appareil.logique.Orientation;
import fr.manboyx.appareil.logique.Prise;
import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;

import java.io.OutputStream;
import java.time.LocalDateTime;

/** L'écran de prise de vue. L'appareil photo n'est tenu que tant que cet écran est visible. */
final class EcranPrise extends Ecran implements Chambre.Ecoute {
    private static final int DEMANDE = 1;
    private static final int BLANC = 0xFFFFFFFF;

    private Chambre chambre;
    private SurfaceTexture surface;
    private boolean frontale;
    private boolean pret;
    private boolean enCours;
    private Flash flash = Flash.AUTO;
    private float zoom = 1f;
    private float zoomMaximum = 1f;
    private int tenue;
    private Uri derniere;
    private OrientationEventListener inclinaison;

    private View prise;
    private View explication;
    private CadreApercu cadre;
    private TextView boutonFlash;
    private TextView retourner;
    private TextView message;
    private View declencheur;
    private ImageView vignette;

    private AppareilActivity appli() {
        return (AppareilActivity) activite;
    }

    /** Vrai si une autre appli attend la photo en retour (« prendre une photo » depuis un formulaire, par exemple). */
    private boolean pourUneAutreAppli() {
        Intent demande = activite.getIntent();
        return demande != null && MediaStore.ACTION_IMAGE_CAPTURE.equals(demande.getAction());
    }

    @Override
    protected View creer() {
        chambre = appli().chambre();
        chambre.ecouter(this);
        FrameLayout racine = new FrameLayout(activite);

        LinearLayout colonne = Ui.colonne(activite);
        colonne.setBackgroundColor(0xFF000000);
        LinearLayout haut = Ui.rangee(activite);
        haut.setMinimumHeight(Ui.dp(activite, 56));
        haut.setPadding(Ui.dp(activite, 8), 0, Ui.dp(activite, 8), 0);
        boutonFlash = blanc(Ui.touche(activite, flash.libelle, "Flash"));
        boutonFlash.setTextSize(15);
        boutonFlash.setOnClickListener(v -> {
            flash = flash.suivant();
            boutonFlash.setText(flash.libelle);
            chambre.flash(flash);
        });
        haut.addView(boutonFlash);
        colonne.addView(haut);

        FrameLayout milieu = new FrameLayout(activite);
        cadre = new CadreApercu(activite);
        TextureView apercu = new TextureView(activite);
        apercu.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture disponible, int largeur, int hauteur) {
                surfaceDisponible(disponible);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture s, int largeur, int hauteur) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture detruite) {
                surface = null;
                fermer();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture s) {}
        });
        cadre.addView(apercu, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        // Écarter deux doigts sur l'image : zoom.
        ScaleGestureDetector pincement = new ScaleGestureDetector(activite, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                zoom = Prise.borner(zoom * d.getScaleFactor(), zoomMaximum);
                chambre.zoom(zoom);
                return true;
            }
        });
        cadre.setOnTouchListener((v, geste) -> {
            pincement.onTouchEvent(geste);
            return true;
        });
        milieu.addView(cadre, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE, Gravity.CENTER));
        message = blanc(Ui.texte(activite, "", 16, fr.manboyx.commun.R.color.texte));
        message.setGravity(Gravity.CENTER);
        Ui.marges(message, activite, 32, 0, 32, 0);
        milieu.addView(message, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE, Gravity.CENTER));
        colonne.addView(milieu, Ui.reste(false));

        LinearLayout bas = Ui.rangee(activite);
        bas.setMinimumHeight(Ui.dp(activite, 120));
        FrameLayout gauche = new FrameLayout(activite);
        vignette = new ImageView(activite);
        vignette.setScaleType(ImageView.ScaleType.CENTER_CROP);
        vignette.setContentDescription("Dernière photo");
        vignette.setVisibility(View.INVISIBLE);
        vignette.setOnClickListener(v -> voirDerniere());
        gauche.addView(vignette, new FrameLayout.LayoutParams(Ui.dp(activite, 52), Ui.dp(activite, 52), Gravity.CENTER));
        bas.addView(gauche, new LinearLayout.LayoutParams(0, Ui.JUSTE, 1));
        FrameLayout centre = new FrameLayout(activite);
        declencheur = new View(activite);
        declencheur.setBackgroundResource(R.drawable.declencheur);
        declencheur.setContentDescription("Prendre la photo");
        declencheur.setOnClickListener(v -> prendre());
        centre.addView(declencheur, new FrameLayout.LayoutParams(Ui.dp(activite, 76), Ui.dp(activite, 76), Gravity.CENTER));
        bas.addView(centre, new LinearLayout.LayoutParams(0, Ui.JUSTE, 1));
        FrameLayout droite = new FrameLayout(activite);
        retourner = blanc(Ui.touche(activite, "Retourner", "Changer de caméra"));
        retourner.setTextSize(15);
        retourner.setOnClickListener(v -> {
            frontale = !frontale;
            ouvrir();
        });
        droite.addView(retourner, new FrameLayout.LayoutParams(Ui.JUSTE, Ui.JUSTE, Gravity.CENTER));
        bas.addView(droite, new LinearLayout.LayoutParams(0, Ui.JUSTE, 1));
        colonne.addView(bas);
        prise = colonne;
        racine.addView(prise, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));

        explication = Autorisations.explication(activite,
                "Pour prendre des photos, l'appli a besoin de l'appareil photo. Les photos restent sur le téléphone.",
                DEMANDE, AppareilActivity.demandees());
        explication.setBackgroundColor(Ui.couleur(activite, fr.manboyx.commun.R.color.fond));
        racine.addView(explication, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));

        inclinaison = new OrientationEventListener(activite) {
            @Override
            public void onOrientationChanged(int degres) {
                tenue = Orientation.arrondir(degres, tenue);
            }
        };
        return racine;
    }

    private static TextView blanc(TextView texte) {
        texte.setTextColor(BLANC);
        return texte;
    }

    /** L'endroit où dessiner l'aperçu est prêt : on peut ouvrir l'appareil photo. */
    void surfaceDisponible(SurfaceTexture disponible) {
        surface = disponible;
        ouvrir();
    }

    @Override
    public void afficher() {
        boolean autorise = Autorisations.accordees(activite, AppareilActivity.demandees());
        explication.setVisibility(autorise ? View.GONE : View.VISIBLE);
        prise.setVisibility(autorise ? View.VISIBLE : View.INVISIBLE);
        if (!autorise) {
            return;
        }
        retourner.setVisibility(chambre.deuxCotes() ? View.VISIBLE : View.INVISIBLE);
        if (inclinaison.canDetectOrientation()) {
            inclinaison.enable();
        }
        ouvrir();
    }

    @Override
    public void masquer() {
        inclinaison.disable();
        fermer();
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        afficher();
    }

    private void ouvrir() {
        if (surface == null || !Autorisations.accordees(activite, AppareilActivity.demandees())) {
            return;
        }
        pret = false;
        enCours = false;
        zoom = 1f;
        message.setText("");
        declencheur.setAlpha(0.4f);
        chambre.ouvrir(surface, frontale);
    }

    private void fermer() {
        pret = false;
        chambre.fermer();
    }

    // ----- Réponses de l'appareil photo -----

    @Override
    public void pret(int largeurApercu, int hauteurApercu, boolean deFace, boolean flashPossible, float maximum) {
        pret = true;
        frontale = deFace;
        zoomMaximum = maximum;
        cadre.forme(largeurApercu, hauteurApercu);
        boutonFlash.setVisibility(flashPossible ? View.VISIBLE : View.INVISIBLE);
        declencheur.setAlpha(1f);
        chambre.flash(flash);
    }

    @Override
    public void erreur(String texte) {
        pret = false;
        enCours = false;
        declencheur.setAlpha(0.4f);
        message.setText(texte);
    }

    private void prendre() {
        if (!pret || enCours) {
            return;
        }
        enCours = true;
        declencheur.setAlpha(0.4f);
        chambre.prendre(tenue);
    }

    @Override
    public void photo(byte[] jpeg, int rotation) {
        if (pourUneAutreAppli()) {
            rendre(jpeg, rotation);
            return;
        }
        Rangement rangement = appli().rangement();
        Fond.lancer(() -> {
            try {
                Uri adresse = rangement.enregistrer(jpeg, Prise.nom(LocalDateTime.now()));
                return new Object[] {adresse, miniature(jpeg, rotation, 8)};
            } catch (Exception echec) {
                return null;
            }
        }, resultat -> {
            enCours = false;
            declencheur.setAlpha(pret ? 1f : 0.4f);
            if (resultat == null) {
                Dialogues.dire(activite, "La photo n'a pas pu être enregistrée.");
                return;
            }
            derniere = (Uri) resultat[0];
            vignette.setImageBitmap((Bitmap) resultat[1]);
            vignette.setVisibility(View.VISIBLE);
        });
    }

    /** Une petite version de la photo, remise droite. */
    private static Bitmap miniature(byte[] jpeg, int rotation, int reduction) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = reduction;
        Bitmap petite = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);
        if (petite == null || rotation == 0) {
            return petite;
        }
        Matrix tour = new Matrix();
        tour.postRotate(rotation);
        return Bitmap.createBitmap(petite, 0, 0, petite.getWidth(), petite.getHeight(), tour, true);
    }

    private void voirDerniere() {
        if (derniere == null) {
            return;
        }
        Intent voir = new Intent(Intent.ACTION_VIEW).setDataAndType(derniere, "image/jpeg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activite.startActivity(voir);
        } catch (ActivityNotFoundException aucune) {
            Dialogues.dire(activite, "Aucune appli du téléphone ne sait afficher cette photo.");
        }
    }

    /**
     * L'adresse où écrire vient d'une autre appli : on ne s'y fie pas. On
     * n'accepte qu'un fichier prêté dans les règles (« content:// »), et jamais
     * un fichier qui contient déjà quelque chose : une appli malveillante ne
     * peut donc pas se servir de l'appareil photo pour écraser un fichier.
     */
    private boolean destinationSure(Uri destination) {
        if (!"content".equals(destination.getScheme())) {
            return false;
        }
        String[] taille = {android.provider.OpenableColumns.SIZE};
        try (android.database.Cursor c = activite.getContentResolver().query(destination, taille, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0) && c.getLong(0) > 0) {
                return false;
            }
        } catch (RuntimeException illisible) {
            // On ne peut pas savoir : l'écriture elle-même dira si elle est permise.
        }
        return true;
    }

    /** Rend la photo à l'appli qui l'a demandée, sans la ranger parmi les photos du téléphone. */
    private void rendre(byte[] jpeg, int rotation) {
        Uri destination = activite.getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        Fond.lancer(() -> {
            Intent reponse = new Intent();
            if (destination == null) {
                // Sans destination, l'usage est de rendre une petite image directement.
                reponse.putExtra("data", miniature(jpeg, rotation, 16));
                return reponse;
            }
            if (!destinationSure(destination)) {
                return null;
            }
            try (OutputStream sortie = activite.getContentResolver().openOutputStream(destination)) {
                if (sortie == null) {
                    return null;
                }
                sortie.write(jpeg);
                return reponse;
            } catch (Exception echec) {
                return null;
            }
        }, reponse -> {
            if (reponse == null) {
                enCours = false;
                declencheur.setAlpha(pret ? 1f : 0.4f);
                Dialogues.dire(activite, "La photo n'a pas pu être transmise.");
                return;
            }
            activite.setResult(Activity.RESULT_OK, reponse);
            activite.finish();
        });
    }
}
