package fr.manboyx.essais;

import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Outils des tests d'écran : lire ce qui est affiché, appuyer sur un texte, prendre une image. */
public final class Essai {
    private Essai() {}

    /** Laisse l'écran finir ce qu'il a en attente. */
    public static void attendre() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** Tous les textes visibles sous `racine`, dans l'ordre d'affichage. */
    public static List<String> textes(View racine) {
        List<String> trouves = new ArrayList<>();
        parcourir(racine, trouves, null);
        return trouves;
    }

    /** Vrai si un texte visible est exactement `texte`. */
    public static boolean visible(View racine, String texte) {
        return trouver(racine, texte) != null;
    }

    /** Vrai si un texte visible contient `morceau`. */
    public static boolean contient(View racine, String morceau) {
        for (String texte : textes(racine)) {
            if (texte.contains(morceau)) {
                return true;
            }
        }
        return false;
    }

    /** La vue visible dont le texte (ou la description) est exactement `texte`, ou `null`. */
    public static View trouver(View racine, String texte) {
        List<View> vues = new ArrayList<>();
        parcourir(racine, null, vues);
        for (View vue : vues) {
            boolean parTexte = vue instanceof TextView && texte.contentEquals(((TextView) vue).getText());
            boolean parDescription = vue.getContentDescription() != null && texte.contentEquals(vue.getContentDescription());
            if (parTexte || parDescription) {
                return vue;
            }
        }
        return null;
    }

    /** Comme `trouver`, mais voit aussi ce qui est caché : pour vérifier qu'un élément est bien masqué. */
    public static View trouverMemeCache(View racine, String description) {
        if (racine.getContentDescription() != null && description.contentEquals(racine.getContentDescription())) {
            return racine;
        }
        if (racine instanceof ViewGroup) {
            ViewGroup groupe = (ViewGroup) racine;
            for (int i = 0; i < groupe.getChildCount(); i++) {
                View trouvee = trouverMemeCache(groupe.getChildAt(i), description);
                if (trouvee != null) {
                    return trouvee;
                }
            }
        }
        return null;
    }

    /** Appuie sur le texte (ou sur la ligne qui le contient) et laisse l'écran réagir. */
    public static void appuyer(View racine, String texte) {
        View vue = trouver(racine, texte);
        if (vue == null) {
            throw new AssertionError("« " + texte + " » n'est pas à l'écran. Visible : " + textes(racine));
        }
        // Dans une liste, c'est la ligne entière qui réagit, pas le texte.
        View cible = vue;
        while (cible != null && !cible.hasOnClickListeners() && !(cible.getParent() instanceof android.widget.AdapterView)) {
            cible = cible.getParent() instanceof View ? (View) cible.getParent() : null;
        }
        if (cible == null) {
            throw new AssertionError("« " + texte + " » ne réagit pas à l'appui");
        }
        if (cible.getParent() instanceof android.widget.AdapterView && !cible.hasOnClickListeners()) {
            android.widget.AdapterView<?> liste = (android.widget.AdapterView<?>) cible.getParent();
            int rang = liste.getPositionForView(cible);
            liste.performItemClick(cible, rang, liste.getItemIdAtPosition(rang));
        } else {
            cible.performClick();
        }
        attendre();
    }

    /** Le champ de saisie visible dont l'indice est `indice`, ou une erreur claire. */
    public static android.widget.EditText champ(View racine, String indice) {
        List<View> vues = new ArrayList<>();
        parcourir(racine, null, vues);
        for (View vue : vues) {
            if (vue instanceof android.widget.EditText && indice.contentEquals(String.valueOf(((android.widget.EditText) vue).getHint()))) {
                return (android.widget.EditText) vue;
            }
        }
        throw new AssertionError("Pas de champ « " + indice + " » à l'écran");
    }

    /** Écrit dans le champ d'indice `indice`. */
    public static void saisir(View racine, String indice, String texte) {
        champ(racine, indice).setText(texte);
        attendre();
    }

    /** La boîte de dialogue affichée en dernier. */
    public static android.app.AlertDialog dialogue() {
        android.app.AlertDialog dialogue = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        if (dialogue == null) {
            throw new AssertionError("Aucune boîte de dialogue n'est ouverte");
        }
        return dialogue;
    }

    /** Appuie sur le bouton de validation de la boîte de dialogue ouverte. */
    public static void valider() {
        dialogue().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
        attendre();
    }

    /** Choisit une ligne dans le menu ouvert. */
    public static void choisir(String ligne) {
        org.robolectric.shadows.ShadowAlertDialog menu = shadowOf(dialogue());
        CharSequence[] lignes = menu.getItems();
        for (int i = 0; lignes != null && i < lignes.length; i++) {
            if (ligne.contentEquals(lignes[i])) {
                menu.clickOnItem(i);
                attendre();
                return;
            }
        }
        throw new AssertionError("« " + ligne + " » n'est pas dans le menu : " + java.util.Arrays.toString(lignes));
    }

    /** Remplace le texte du champ de la boîte de dialogue ouverte. */
    public static void ecrireDansDialogue(String texte) {
        List<View> vues = new ArrayList<>();
        parcourir(dialogue().getWindow().getDecorView(), null, vues);
        for (View vue : vues) {
            if (vue instanceof android.widget.EditText) {
                ((android.widget.EditText) vue).setText(texte);
                return;
            }
        }
        throw new AssertionError("La boîte de dialogue n'a pas de champ de saisie");
    }

    /** Écrit une image de la vue dans le dossier « captures.dir », si cette propriété est donnée. */
    public static void capturer(View vue, String nom) throws IOException {
        String dossier = System.getProperty("captures.dir", "");
        if (dossier.isEmpty()) {
            return;
        }
        attendre();
        Bitmap image = Bitmap.createBitmap(vue.getWidth(), vue.getHeight(), Bitmap.Config.ARGB_8888);
        vue.draw(new Canvas(image));
        new File(dossier).mkdirs();
        try (FileOutputStream sortie = new FileOutputStream(new File(dossier, nom + ".png"))) {
            image.compress(Bitmap.CompressFormat.PNG, 100, sortie);
        }
    }

    private static void parcourir(View vue, List<String> textes, List<View> vues) {
        if (vue.getVisibility() != View.VISIBLE) {
            return;
        }
        if (vues != null) {
            vues.add(vue);
        }
        if (textes != null && vue instanceof TextView) {
            CharSequence texte = ((TextView) vue).getText();
            if (texte != null && texte.length() > 0) {
                textes.add(texte.toString());
            }
        }
        if (vue instanceof ViewGroup) {
            ViewGroup groupe = (ViewGroup) vue;
            for (int i = 0; i < groupe.getChildCount(); i++) {
                parcourir(groupe.getChildAt(i), textes, vues);
            }
        }
    }
}
