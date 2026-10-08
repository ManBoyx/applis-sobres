package fr.manboyx.fichiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.MimeTypeMap;

import fr.manboyx.commun.Fond;
import fr.manboyx.essais.Essai;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** Pilote l'appli comme le ferait quelqu'un, sur un vrai dossier temporaire. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcransTest {
    static File racine;
    static boolean acces;

    public static class ActiviteEssai extends FichiersActivity {
        @Override
        public File racine() {
            return racine;
        }

        @Override
        public boolean acces() {
            return acces;
        }
    }

    static File ecrire(String chemin, String contenu) throws Exception {
        File fichier = new File(racine, chemin);
        fichier.getParentFile().mkdirs();
        Files.write(fichier.toPath(), contenu.getBytes(StandardCharsets.UTF_8));
        return fichier;
    }

    static void effacer(File fichier) {
        File[] contenu = fichier.listFiles();
        if (contenu != null) {
            for (File f : contenu) {
                effacer(f);
            }
        }
        fichier.delete();
    }

    static void remplir() throws Exception {
        racine = Files.createTempDirectory("fichiers-ecrans").toFile();
        acces = true;
        ecrire("Documents/facture.pdf", "0123456789");
        ecrire("Documents/Impôts/2025.txt", "x");
        ecrire("Photos/plage.jpg", "image");
        ecrire("notes.txt", "bonjour");
        ecrire("gros.bin", "0123456789012345678901234567890123456789");
        ecrire(".cache", "x");
    }

    @Before
    public void preparer() throws Exception {
        Fond.utiliser(Runnable::run);
        remplir();
    }

    @After
    public void nettoyer() {
        effacer(racine);
    }

    private ActiviteEssai ouvrir() {
        ActiviteEssai a = Robolectric.buildActivity(ActiviteEssai.class).setup().get();
        Essai.attendre();
        return a;
    }

    private static View ecran(ActiviteEssai a) {
        return a.getWindow().getDecorView();
    }

    private static void appuiLong(ActiviteEssai a, String nom) {
        View ligne = Essai.trouver(ecran(a), nom);
        while (!(ligne.getParent() instanceof android.widget.ListView)) {
            ligne = (View) ligne.getParent();
        }
        android.widget.ListView liste = (android.widget.ListView) ligne.getParent();
        int rang = liste.getPositionForView(ligne);
        liste.getOnItemLongClickListener().onItemLongClick(liste, ligne, rang, rang);
        Essai.attendre();
    }

    @Test
    public void laRacineMontreLesDossiersPuisLesFichiersSansLesFichiersCaches() {
        List<String> textes = Essai.textes(ecran(ouvrir()));
        assertTrue(textes.toString(), textes.indexOf("Documents") < textes.indexOf("Photos"));
        assertTrue(textes.indexOf("Photos") < textes.indexOf("gros.bin"));
        assertTrue(textes.indexOf("gros.bin") < textes.indexOf("notes.txt"));
        assertFalse(textes.contains(".cache"));
        assertTrue(Essai.contient(ecran(ouvrir()), "7 o · "));
    }

    @Test
    public void sansAccesLAppliExpliquePourquoiEtNeListeRien() {
        acces = false;
        ActiviteEssai a = ouvrir();
        assertTrue(Essai.contient(ecran(a), "accès à tous les fichiers"));
        assertFalse(Essai.visible(ecran(a), "Documents"));
        Essai.appuyer(ecran(a), "Autoriser l'accès");
        assertEquals("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", shadowOf(a).getNextStartedActivity().getAction());
    }

    @Test
    public void appuyerSurUnDossierYEntreEtLeRetourEnSort() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Documents");
        assertTrue(Essai.visible(ecran(a), "facture.pdf"));
        assertTrue(Essai.visible(ecran(a), "Impôts"));
        assertFalse(Essai.visible(ecran(a), "notes.txt"));
        assertTrue(Essai.contient(ecran(a), "Mémoire du téléphone › Documents"));
        a.onBackPressed();
        Essai.attendre();
        assertTrue(Essai.visible(ecran(a), "notes.txt"));
        a.onBackPressed();
        assertTrue(a.isFinishing());
    }

    @Test
    public void unDossierVideLeDit() throws Exception {
        new File(racine, "Vide").mkdir();
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Vide");
        assertTrue(Essai.visible(ecran(a), "Ce dossier est vide."));
    }

    @Test
    public void creerUnDossierLAjouteALaListe() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouveau dossier");
        Essai.ecrireDansDialogue("Vacances");
        Essai.valider();
        assertTrue(new File(racine, "Vacances").isDirectory());
        assertTrue(Essai.visible(ecran(a), "Vacances"));
    }

    @Test
    public void unNomDejaPrisEstRefuseAvecUnMessage() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouveau dossier");
        Essai.ecrireDansDialogue("Photos");
        Essai.valider();
        assertEquals("« Photos » existe déjà ici.", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void renommerChangeLeNomEnGardantLeContenu() throws Exception {
        ActiviteEssai a = ouvrir();
        appuiLong(a, "notes.txt");
        Essai.choisir("Renommer");
        Essai.ecrireDansDialogue("journal.txt");
        Essai.valider();
        assertFalse(new File(racine, "notes.txt").exists());
        assertEquals("bonjour", new String(Files.readAllBytes(new File(racine, "journal.txt").toPath()), StandardCharsets.UTF_8));
        assertTrue(Essai.visible(ecran(a), "journal.txt"));
    }

    @Test
    public void copierPuisCollerDansUnAutreDossierLaisseLOriginal() {
        ActiviteEssai a = ouvrir();
        appuiLong(a, "notes.txt");
        Essai.choisir("Copier");
        assertTrue(Essai.visible(ecran(a), "Copier « notes.txt » ici"));
        Essai.appuyer(ecran(a), "Photos");
        Essai.appuyer(ecran(a), "Coller");
        assertTrue(new File(racine, "Photos/notes.txt").isFile());
        assertTrue(new File(racine, "notes.txt").isFile());
        assertTrue(Essai.visible(ecran(a), "notes.txt"));
        assertFalse("le bandeau disparaît une fois collé", Essai.visible(ecran(a), "Coller"));
    }

    @Test
    public void deplacerPuisCollerRetireLOriginal() {
        ActiviteEssai a = ouvrir();
        appuiLong(a, "Documents");
        Essai.choisir("Déplacer");
        Essai.appuyer(ecran(a), "Photos");
        Essai.appuyer(ecran(a), "Coller");
        assertFalse(new File(racine, "Documents").exists());
        assertTrue(new File(racine, "Photos/Documents/Impôts/2025.txt").isFile());
    }

    @Test
    public void onPeutRenoncerAColler() {
        ActiviteEssai a = ouvrir();
        appuiLong(a, "notes.txt");
        Essai.choisir("Copier");
        Essai.appuyer(ecran(a), "Ne pas coller");
        assertFalse(Essai.visible(ecran(a), "Coller"));
        assertEquals(5, racine.list().length);
    }

    @Test
    public void collerUnDossierDansLuiMemeEstRefuse() {
        ActiviteEssai a = ouvrir();
        appuiLong(a, "Documents");
        Essai.choisir("Copier");
        Essai.appuyer(ecran(a), "Documents");
        Essai.appuyer(ecran(a), "Coller");
        assertEquals("Un dossier ne peut pas être placé dans lui-même.", ShadowToast.getTextOfLatestToast());
        assertEquals(2, new File(racine, "Documents").list().length);
    }

    @Test
    public void supprimerUnDossierAnnonceCeQuIlContientEtAttendLaConfirmation() {
        ActiviteEssai a = ouvrir();
        appuiLong(a, "Documents");
        Essai.choisir("Supprimer");
        assertTrue(shadowOf(Essai.dialogue()).getMessage().toString().contains("3 éléments"));
        assertTrue("rien n'est supprimé avant la confirmation", new File(racine, "Documents").exists());
        Essai.valider();
        assertFalse(new File(racine, "Documents").exists());
        assertFalse(Essai.visible(ecran(a), "Documents"));
        assertTrue(new File(racine, "Photos/plage.jpg").exists());
    }

    @Test
    public void ouvrirUnFichierLeConfieEnLectureSeuleAUneAutreAppli() {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypeMapping("txt", "text/plain");
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "notes.txt");
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals(Intent.ACTION_VIEW, lance.getAction());
        assertEquals("text/plain", lance.getType());
        assertEquals("content", lance.getData().getScheme());
        assertEquals(new File(racine, "notes.txt").getAbsolutePath(), lance.getData().getPath());
        assertTrue((lance.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertEquals("aucun droit d'écriture n'est prêté", 0, lance.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    @Test
    public void partagerEnvoieLeFichier() {
        ActiviteEssai a = ouvrir();
        appuiLong(a, "notes.txt");
        Essai.choisir("Partager");
        Intent envoi = shadowOf(a).getNextStartedActivity().getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals(Intent.ACTION_SEND, envoi.getAction());
        Uri joint = envoi.getParcelableExtra(Intent.EXTRA_STREAM);
        assertEquals(new File(racine, "notes.txt").getAbsolutePath(), joint.getPath());
    }

    @Test
    public void leTriParTailleMetLePlusGrosFichierEnTete() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Options d'affichage");
        Essai.choisir("Trier par taille");
        List<String> textes = Essai.textes(ecran(a));
        assertTrue(textes.indexOf("gros.bin") < textes.indexOf("notes.txt"));
        Essai.appuyer(ecran(a), "Options d'affichage");
        Essai.choisir("Trier par nom");
    }

    @Test
    public void lesFichiersCachesApparaissentSurDemande() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Options d'affichage");
        Essai.choisir("Afficher les fichiers cachés");
        assertTrue(Essai.visible(ecran(a), ".cache"));
    }

    @Test
    public void lePretDeFichierRefuseToutCeQuiSortDeLaMemoireDuTelephone() throws Exception {
        File dehors = Files.createTempFile("secret", ".txt").toFile();
        try {
            assertTrue(FournisseurFichiers.permis(racine, new File(racine, "notes.txt")));
            assertFalse(FournisseurFichiers.permis(racine, dehors));
            assertFalse(FournisseurFichiers.permis(racine, new File(racine, "../" + dehors.getName())));
            assertFalse("un dossier ne se prête pas", FournisseurFichiers.permis(racine, new File(racine, "Photos")));
            assertFalse(FournisseurFichiers.permis(racine, new File(racine, "absent.txt")));
            File lien = new File(racine, "lien.txt");
            Files.createSymbolicLink(lien.toPath(), dehors.toPath());
            assertFalse("un lien vers l'extérieur ne se prête pas", FournisseurFichiers.permis(racine, lien));
        } finally {
            dehors.delete();
        }
    }
}
