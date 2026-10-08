package fr.manboyx.commun;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/** Demander une autorisation en expliquant pourquoi, et proposer les réglages si elle a été refusée. */
public final class Autorisations {
    private Autorisations() {}

    public static boolean accordees(Context c, String... autorisations) {
        for (String autorisation : autorisations) {
            if (c.checkSelfPermission(autorisation) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void demander(Activity a, int code, String... autorisations) {
        a.requestPermissions(autorisations, code);
    }

    /** Ouvre la fiche de l'appli dans les réglages du téléphone. */
    public static void ouvrirReglages(Activity a) {
        Intent reglages = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", a.getPackageName(), null));
        try {
            a.startActivity(reglages);
        } catch (RuntimeException sansReglages) {
            // Téléphone sans écran de réglages accessible : rien à faire de plus.
        }
    }

    /**
     * Ce que l'écran montre tant que l'autorisation manque : pourquoi elle est
     * utile, un bouton pour l'accorder, et l'accès aux réglages pour le cas où
     * Android ne repose plus la question.
     */
    public static View explication(Activity a, CharSequence pourquoi, int code, String... autorisations) {
        LinearLayout colonne = Ui.colonne(a);
        colonne.setGravity(Gravity.CENTER);
        Ui.marges(colonne, a, 32, 32, 32, 32);
        colonne.addView(Ui.vide(a, pourquoi));
        Button accorder = Ui.bouton(a, "Autoriser", true);
        accorder.setOnClickListener(v -> demander(a, code, autorisations));
        colonne.addView(accorder, Ui.place(Ui.JUSTE, Ui.JUSTE));
        Button reglages = Ui.bouton(a, "Ouvrir les réglages", false);
        reglages.setOnClickListener(v -> ouvrirReglages(a));
        LinearLayout.LayoutParams dessous = Ui.place(Ui.JUSTE, Ui.JUSTE);
        dessous.topMargin = Ui.dp(a, 12);
        colonne.addView(reglages, dessous);
        return colonne;
    }
}
