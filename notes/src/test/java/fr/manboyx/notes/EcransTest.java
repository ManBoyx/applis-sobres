package fr.manboyx.notes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;

import fr.manboyx.essais.Essai;
import fr.manboyx.notes.logique.Note;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.List;

/** Pilote l'appli comme le ferait quelqu'un. Les notes sont de vrais fichiers, dans le dossier du simulateur. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcransTest {
    private static final String CHAMP = "Écrivez ici…";

    private ActivityController<NotesActivity> controleur;

    private NotesActivity ouvrir() {
        controleur = Robolectric.buildActivity(NotesActivity.class).setup();
        Essai.attendre();
        return controleur.get();
    }

    private static View ecran(NotesActivity a) {
        return a.getWindow().getDecorView();
    }

    private static void retour(NotesActivity a) {
        a.onBackPressed();
        Essai.attendre();
    }

    /** Écrit une note par l'appli elle-même et revient à la liste. */
    private static void ecrire(NotesActivity a, String texte) {
        Essai.appuyer(ecran(a), "Nouvelle note");
        Essai.saisir(ecran(a), CHAMP, texte);
        retour(a);
    }

    @Test
    public void sansNoteLAppliDitCommentCommencer() {
        assertTrue(Essai.visible(ecran(ouvrir()), "Aucune note. Appuyez sur + pour en écrire une."));
    }

    @Test
    public void uneNoteEcriteEstEnregistreeEnRevenantEtApparaitDansLaListe() {
        NotesActivity a = ouvrir();
        ecrire(a, "Courses\n- pain\n- œufs");
        assertTrue(Essai.visible(ecran(a), "Courses"));
        assertTrue(Essai.visible(ecran(a), "- pain - œufs"));
        List<Note> notes = a.carnet().lister("");
        assertEquals(1, notes.size());
        assertEquals("Courses\n- pain\n- œufs", a.carnet().lire(notes.get(0).id));
    }

    @Test
    public void rouvrirUneNoteMontreSonTexteEtLaModifierNeLaDoublePas() {
        NotesActivity a = ouvrir();
        ecrire(a, "Première version");
        Essai.appuyer(ecran(a), "Première version");
        assertEquals("Première version", Essai.champ(ecran(a), CHAMP).getText().toString());
        Essai.saisir(ecran(a), CHAMP, "Deuxième version");
        retour(a);
        assertTrue(Essai.visible(ecran(a), "Deuxième version"));
        assertFalse(Essai.visible(ecran(a), "Première version"));
        assertEquals(1, a.carnet().lister("").size());
    }

    @Test
    public void uneNoteLaisseeVideNEstPasGardee() {
        NotesActivity a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvelle note");
        retour(a);
        assertEquals(0, a.carnet().lister("").size());
        ecrire(a, "À vider");
        Essai.appuyer(ecran(a), "À vider");
        Essai.saisir(ecran(a), CHAMP, "  ");
        retour(a);
        assertEquals(0, a.carnet().lister("").size());
    }

    @Test
    public void laNoteEstEnregistreeDesQueLAppliPasseALArrierePlan() {
        NotesActivity a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvelle note");
        Essai.saisir(ecran(a), CHAMP, "Écrit juste avant un appel");
        controleur.pause();
        assertEquals(1, a.carnet().lister("").size());
        // Revenir puis repartir ne crée pas une deuxième note.
        controleur.resume();
        Essai.saisir(ecran(a), CHAMP, "Écrit juste avant un appel, puis complété");
        controleur.pause();
        assertEquals(1, a.carnet().lister("").size());
        assertEquals("Écrit juste avant un appel, puis complété", a.carnet().lister("").get(0).titre);
    }

    @Test
    public void supprimerDemandeConfirmationPuisRetireLaNote() {
        NotesActivity a = ouvrir();
        ecrire(a, "À supprimer");
        ecrire(a, "À garder");
        Essai.appuyer(ecran(a), "À supprimer");
        Essai.appuyer(ecran(a), "Supprimer");
        assertEquals("rien n'est supprimé avant la confirmation", 2, a.carnet().lister("").size());
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Essai.attendre();
        assertFalse(Essai.visible(ecran(a), "À supprimer"));
        assertTrue(Essai.visible(ecran(a), "À garder"));
        assertEquals(1, a.carnet().lister("").size());
    }

    @Test
    public void laRechercheLitLeTexteEntierDesNotes() {
        NotesActivity a = ouvrir();
        ecrire(a, "Courses\n- pain");
        ecrire(a, "Idées cadeaux\nun vélo");
        Essai.saisir(ecran(a), "Rechercher", "velo");
        assertTrue(Essai.visible(ecran(a), "Idées cadeaux"));
        assertFalse(Essai.visible(ecran(a), "Courses"));
        Essai.saisir(ecran(a), "Rechercher", "zzz");
        assertTrue(Essai.visible(ecran(a), "Aucune note ne correspond."));
    }

    @Test
    public void partagerEnvoieLeTexteDeLaNote() {
        NotesActivity a = ouvrir();
        ecrire(a, "À partager");
        Essai.appuyer(ecran(a), "À partager");
        Essai.appuyer(ecran(a), "Partager");
        Intent choix = shadowOf(a).getNextStartedActivity();
        Intent envoi = choix.getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals(Intent.ACTION_SEND, envoi.getAction());
        assertEquals("À partager", envoi.getStringExtra(Intent.EXTRA_TEXT));
    }

    @Test
    public void uneNoteVideNeSePartagePas() {
        NotesActivity a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvelle note");
        Essai.appuyer(ecran(a), "Partager");
        assertNull(shadowOf(a).getNextStartedActivity());
    }

    @Test
    public void unTextePartageDepuisUneAutreAppliDevientUneNote() {
        Intent demande = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Adresse du restaurant");
        NotesActivity a = Robolectric.buildActivity(NotesActivity.class, demande).setup().get();
        Essai.attendre();
        assertEquals("Adresse du restaurant", Essai.champ(ecran(a), CHAMP).getText().toString());
        retour(a);
        assertTrue(Essai.visible(ecran(a), "Adresse du restaurant"));
    }
}
