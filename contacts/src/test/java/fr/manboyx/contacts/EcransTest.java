package fr.manboyx.contacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.provider.ContactsContract;
import android.view.View;

import fr.manboyx.commun.Fond;
import fr.manboyx.contacts.logique.Detail;
import fr.manboyx.essais.Essai;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.Arrays;
import java.util.List;

/** Pilote les écrans comme le ferait quelqu'un, sur un carnet d'adresses en mémoire. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcransTest {
    static CarnetMemoire carnet;

    /** L'appli, branchée sur le carnet en mémoire. */
    public static class ActiviteEssai extends ContactsActivity {
        @Override
        public Depot depot() {
            return carnet;
        }
    }

    private Application appli;
    private long alice;

    @Before
    public void preparer() {
        Fond.utiliser(Runnable::run);
        appli = RuntimeEnvironment.getApplication();
        shadowOf(appli).grantPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS);
        carnet = new CarnetMemoire();
        alice = carnet.ajouter("Alice Martin", true, new String[] {"06 12 34 56 78", "01 23 45 67 89"}, new String[] {"alice@exemple.fr"});
        carnet.ajouter("Bruno Petit", false, new String[] {"07 00 00 00 01"}, new String[] {});
        carnet.ajouter("Élodie Durand", false, new String[] {}, new String[] {"elodie@exemple.fr"});
    }

    private ActiviteEssai ouvrir() {
        ActiviteEssai a = Robolectric.buildActivity(ActiviteEssai.class).setup().get();
        Essai.attendre();
        return a;
    }

    private static View ecran(ActiviteEssai a) {
        return a.getWindow().getDecorView();
    }

    @Test
    public void laListeMontreLesFavorisPuisLesContactsParLettre() {
        List<String> textes = Essai.textes(ecran(ouvrir()));
        int favoris = textes.indexOf("Favoris");
        int lettreA = textes.indexOf("A");
        int lettreB = textes.indexOf("B");
        assertTrue(favoris >= 0 && favoris < lettreA && lettreA < lettreB);
        assertTrue(textes.contains("Bruno Petit"));
        assertTrue(textes.contains("Élodie Durand"));
        assertEquals("Alice est dans les favoris et sous sa lettre", 2, java.util.Collections.frequency(textes, "Alice Martin"));
    }

    @Test
    public void sansAutorisationLAppliExpliquePourquoiEtNAfficheAucunContact() {
        shadowOf(appli).denyPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS);
        View ecran = ecran(ouvrir());
        assertTrue(Essai.contient(ecran, "a besoin d'y accéder"));
        assertTrue(Essai.visible(ecran, "Autoriser"));
        assertFalse(Essai.visible(ecran, "Alice Martin"));
    }

    @Test
    public void laRechercheNeGardeQueLesContactsTrouves() {
        View ecran = ecran(ouvrir());
        Essai.saisir(ecran, "Rechercher", "elo");
        assertTrue(Essai.visible(ecran, "Élodie Durand"));
        assertFalse(Essai.visible(ecran, "Alice Martin"));
        assertFalse(Essai.visible(ecran, "Favoris"));
    }

    @Test
    public void uneRechercheSansResultatLeDit() {
        View ecran = ecran(ouvrir());
        Essai.saisir(ecran, "Rechercher", "zzz");
        assertTrue(Essai.visible(ecran, "Aucun contact ne correspond."));
    }

    @Test
    public void leBoutonRetourEffaceDAbordLaRecherche() {
        ActiviteEssai a = ouvrir();
        Essai.saisir(ecran(a), "Rechercher", "elo");
        a.onBackPressed();
        Essai.attendre();
        assertFalse(a.isFinishing());
        assertTrue(Essai.visible(ecran(a), "Alice Martin"));
    }

    @Test
    public void appuyerSurUnContactOuvreSaFicheAvecNumerosEtAdresses() {
        View ecran = ecran(ouvrir());
        Essai.appuyer(ecran, "Bruno Petit");
        assertTrue(Essai.visible(ecran, "07 00 00 00 01"));
        assertTrue(Essai.visible(ecran, "Appeler"));
        assertFalse("la liste n'est plus affichée", Essai.visible(ecran, "Élodie Durand"));
    }

    @Test
    public void appelerOuvreLeTelephoneSurLeNumero() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Bruno Petit");
        Essai.appuyer(ecran(a), "Appeler");
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals(Intent.ACTION_DIAL, lance.getAction());
        assertEquals("tel:07%2000%2000%2000%2001", lance.getDataString());
    }

    @Test
    public void messageOuvreLesSmsSurLeNumero() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Bruno Petit");
        Essai.appuyer(ecran(a), "Message");
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals(Intent.ACTION_SENDTO, lance.getAction());
        assertEquals("smsto", lance.getData().getScheme());
    }

    @Test
    public void lEtoileAjouteAuxFavorisPuisLesRetire() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Bruno Petit");
        Essai.appuyer(ecran(a), "Ajouter aux favoris");
        assertTrue(carnet.lister().stream().anyMatch(f -> f.nom.equals("Bruno Petit") && f.favori));
        assertNotNull(Essai.trouver(ecran(a), "Retirer des favoris"));
        Essai.appuyer(ecran(a), "Retirer des favoris");
        assertTrue(carnet.lister().stream().noneMatch(f -> f.nom.equals("Bruno Petit") && f.favori));
    }

    @Test
    public void unNouveauContactEnregistreApparaitDansLaListe() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouveau contact");
        Essai.saisir(ecran(a), "Nom", "Chloé Bernard");
        Essai.saisir(ecran(a), "Numéro", "06 99 88 77 66");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertTrue(Essai.visible(ecran(a), "Chloé Bernard"));
        Detail cree = carnet.contacts.values().stream().filter(d -> d.nom.equals("Chloé Bernard")).findFirst().get();
        assertEquals(Arrays.asList("06 99 88 77 66"), cree.telephones);
        assertTrue(cree.emails.isEmpty());
    }

    @Test
    public void unContactSansNomNiNumeroNEstPasEnregistre() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouveau contact");
        Essai.saisir(ecran(a), "Adresse", "seule@exemple.fr");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertTrue(Essai.visible(ecran(a), "Indiquez un nom ou un numéro."));
        assertEquals(3, carnet.contacts.size());
    }

    @Test
    public void onPeutAjouterUnDeuxiemeNumero() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouveau contact");
        Essai.saisir(ecran(a), "Nom", "Deux Numéros");
        Essai.saisir(ecran(a), "Numéro", "0611111111");
        Essai.appuyer(ecran(a), "Ajouter un numéro");
        // Le premier champ vide trouvé est maintenant le second numéro.
        for (View vue : new View[] {ecran(a)}) {
            java.util.List<android.widget.EditText> champs = new java.util.ArrayList<>();
            collecter(vue, champs);
            champs.stream().filter(c -> "Numéro".contentEquals(c.getHint()) && c.length() == 0).findFirst().get().setText("0622222222");
        }
        Essai.appuyer(ecran(a), "Enregistrer");
        Detail cree = carnet.contacts.values().stream().filter(d -> d.nom.equals("Deux Numéros")).findFirst().get();
        assertEquals(Arrays.asList("0611111111", "0622222222"), cree.telephones);
    }

    private static void collecter(View vue, java.util.List<android.widget.EditText> champs) {
        if (vue instanceof android.widget.EditText) {
            champs.add((android.widget.EditText) vue);
        } else if (vue instanceof android.view.ViewGroup) {
            android.view.ViewGroup groupe = (android.view.ViewGroup) vue;
            for (int i = 0; i < groupe.getChildCount(); i++) {
                collecter(groupe.getChildAt(i), champs);
            }
        }
    }

    @Test
    public void modifierUnContactChangeSonNomEnGardantSesNumeros() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Bruno Petit");
        Essai.appuyer(ecran(a), "Modifier");
        assertEquals("Bruno Petit", Essai.champ(ecran(a), "Nom").getText().toString());
        Essai.saisir(ecran(a), "Nom", "Bruno Grand");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertTrue("on revient sur la fiche, à jour", Essai.contient(ecran(a), "Bruno Grand"));
        assertTrue(Essai.visible(ecran(a), "07 00 00 00 01"));
        assertEquals(3, carnet.contacts.size());
    }

    @Test
    public void supprimerDemandeConfirmationPuisRetireLeContact() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Bruno Petit");
        Essai.appuyer(ecran(a), "Supprimer");
        assertEquals("rien n'est supprimé avant la confirmation", 3, carnet.contacts.size());
        AlertDialog dialogue = ShadowAlertDialog.getLatestAlertDialog();
        dialogue.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Essai.attendre();
        assertEquals(2, carnet.contacts.size());
        assertFalse(Essai.visible(ecran(a), "Bruno Petit"));
        assertTrue("retour à la liste", Essai.visible(ecran(a), "Élodie Durand"));
    }

    @Test
    public void leRetourDepuisUneFicheRevientALaListe() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Bruno Petit");
        a.onBackPressed();
        Essai.attendre();
        assertFalse(a.isFinishing());
        assertTrue(Essai.visible(ecran(a), "Élodie Durand"));
    }

    @Test
    public void leRetourDepuisLaListeQuitteLAppli() {
        ActiviteEssai a = ouvrir();
        a.onBackPressed();
        assertTrue(a.isFinishing());
    }

    @Test
    public void uneFicheSuppriméeEntreTempsSeReferme() {
        ActiviteEssai a = ouvrir();
        carnet.supprimer(alice);
        a.ouvrir(new EcranFiche(alice));
        Essai.attendre();
        assertTrue("retour à la liste", Essai.visible(ecran(a), "Bruno Petit"));
    }

    @Test
    public void uneAutreAppliPeutDemanderDAjouterUnNumero() {
        Intent demande = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                .putExtra(ContactsContract.Intents.Insert.PHONE, "0655443322");
        ActiviteEssai a = Robolectric.buildActivity(ActiviteEssai.class, demande).setup().get();
        Essai.attendre();
        assertTrue(Essai.visible(ecran(a), "Nouveau contact"));
        assertEquals("0655443322", Essai.champ(ecran(a), "Numéro").getText().toString());
        assertNull("rien n'est enregistré sans appui sur Enregistrer",
                carnet.contacts.values().stream().filter(d -> d.telephones.contains("0655443322")).findFirst().orElse(null));
    }
}
