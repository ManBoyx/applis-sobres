package fr.manboyx.appareil;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Ecran;

public class AppareilActivity extends ActiviteSobre {
    private Chambre chambre;
    private Rangement rangement;

    /** Les tests la remplacent par une fausse chambre. */
    public Chambre chambre() {
        if (chambre == null) {
            chambre = new ChambreCamera2(this);
        }
        return chambre;
    }

    public Rangement rangement() {
        if (rangement == null) {
            rangement = new RangementAndroid(getContentResolver());
        }
        return rangement;
    }

    /** Sur Android 9 et avant, enregistrer une photo demande aussi le droit d'écrire dans la mémoire. */
    static String[] demandees() {
        if (Build.VERSION.SDK_INT < 29) {
            return new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
        return new String[] {Manifest.permission.CAMERA};
    }

    @Override
    protected Ecran accueil() {
        return new EcranPrise();
    }

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        // L'écran ne s'éteint pas pendant qu'on cadre.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
