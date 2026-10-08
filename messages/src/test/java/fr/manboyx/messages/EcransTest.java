package fr.manboyx.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import fr.manboyx.commun.Correspondant;
import fr.manboyx.commun.Fond;
import fr.manboyx.essais.Essai;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Pilote l'appli comme le ferait quelqu'un, sur des SMS en mémoire. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcransTest {
    static BoiteMemoire boite;

    public static class ActiviteEssai extends MessagesActivity {
        @Override
        public Depot depot() {
            return boite;
        }
    }

    private Application appli;

    static void remplir() {
        long maintenant = System.currentTimeMillis();
        long jour = 24 * 3600_000L;
        boite = new BoiteMemoire();
        boite.carnet.add(new Correspondant("Zoé Martin", "06 12 34 56 78", false));
        boite.carnet.add(new Correspondant("Alice Durand", "07 00 00 00 01", true));
        boite.recu(1, "+33612345678", "Tu viens samedi ?", maintenant - 2 * jour, true);
        boite.envoye(1, "+33612345678", "Oui, vers midi.", maintenant - 2 * jour + 60_000);
        boite.recu(1, "+33612345678", "Super, à samedi !", maintenant - 3_600_000, false);
        boite.recu(2, "MaBanque", "Votre code de validation est 482913.", maintenant - 5 * jour, true);
        boite.envoye(3, "0144556677", "Merci pour le rendez-vous.", maintenant - 9 * jour);
    }

    @Before
    public void preparer() {
        Fond.utiliser(Runnable::run);
        appli = RuntimeEnvironment.getApplication();
        shadowOf(appli).grantPermissions(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS);
        remplir();
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
    public void lesConversationsSontListeesDeLaPlusRecenteALaPlusAncienneAvecLesNoms() {
        List<String> textes = Essai.textes(ecran(ouvrir()));
        int zoe = textes.indexOf("Zoé Martin");
        int banque = textes.indexOf("MaBanque");
        int inconnu = textes.indexOf("01 44 55 66 77");
        assertTrue(textes.toString(), zoe >= 0 && zoe < banque && banque < inconnu);
        assertTrue(textes.contains("Super, à samedi !"));
        assertTrue(textes.contains("Vous : Merci pour le rendez-vous."));
    }

    @Test
    public void leNombreDeMessagesNonLusEstIndique() {
        View ecran = ecran(ouvrir());
        assertNotNull(Essai.trouver(ecran, "1 non lus"));
        assertEquals("une seule conversation a du non lu", 1, Collections.frequency(Essai.textes(ecran), "1"));
    }

    @Test
    public void sansAutorisationLAppliExpliquePourquoiEtNAfficheAucunMessage() {
        shadowOf(appli).denyPermissions(Manifest.permission.READ_SMS);
        View ecran = ecran(ouvrir());
        assertTrue(Essai.contient(ecran, "a besoin de les lire"));
        assertFalse(Essai.contient(ecran, "samedi"));
    }

    @Test
    public void ouvrirUneConversationMontreSesMessagesDansLOrdreAvecLesJours() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Zoé Martin");
        List<String> textes = Essai.textes(ecran(a));
        int premier = textes.indexOf("Tu viens samedi ?");
        int reponse = textes.indexOf("Oui, vers midi.");
        int dernier = textes.indexOf("Super, à samedi !");
        assertTrue(textes.toString(), premier >= 0 && premier < reponse && reponse < dernier);
        assertTrue(textes.contains("Aujourd'hui"));
        assertFalse("les autres conversations ne s'y mêlent pas", Essai.contient(ecran(a), "code de validation"));
        assertNotNull(Essai.trouver(ecran(a), "Envoyé : Oui, vers midi."));
        assertNotNull(Essai.trouver(ecran(a), "Reçu : Tu viens samedi ?"));
    }

    @Test
    public void envoyerFaitPartirLeTexteVersLeBonNumeroEtVideLeChamp() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Zoé Martin");
        Essai.saisir(ecran(a), "Message", "  J'apporte le dessert.  ");
        Essai.appuyer(ecran(a), "Envoyer");
        assertEquals(Arrays.asList("+33612345678|J'apporte le dessert."), boite.envois);
        assertEquals("", Essai.champ(ecran(a), "Message").getText().toString());
        assertNotNull("le message envoyé apparaît dans la conversation", Essai.trouver(ecran(a), "Envoyé : J'apporte le dessert."));
    }

    @Test
    public void unMessageVideNePartPas() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Zoé Martin");
        Essai.saisir(ecran(a), "Message", "   ");
        Essai.appuyer(ecran(a), "Envoyer");
        assertTrue(boite.envois.isEmpty());
    }

    @Test
    public void sansAutorisationDEnvoyerRienNePartEtLeTexteEstGarde() {
        shadowOf(appli).denyPermissions(Manifest.permission.SEND_SMS);
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Zoé Martin");
        Essai.saisir(ecran(a), "Message", "Bonjour");
        Essai.appuyer(ecran(a), "Envoyer");
        assertTrue(boite.envois.isEmpty());
        assertEquals(Manifest.permission.SEND_SMS, shadowOf(a).getLastRequestedPermission().requestedPermissions[0]);
        assertEquals("Bonjour", Essai.champ(ecran(a), "Message").getText().toString());
    }

    @Test
    public void siLeTelephoneRefuseLEnvoiLeTexteEstGardeEtOnLeDit() {
        boite.envoiRefuse = true;
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Zoé Martin");
        Essai.saisir(ecran(a), "Message", "Bonjour");
        Essai.appuyer(ecran(a), "Envoyer");
        assertEquals("Bonjour", Essai.champ(ecran(a), "Message").getText().toString());
        assertEquals("Le message n'a pas pu être envoyé.", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void unLongTexteAnnonceLeNombreDeSms() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Zoé Martin");
        assertFalse(Essai.contient(ecran(a), "SMS ·"));
        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            long_.append('a');
        }
        Essai.saisir(ecran(a), "Message", long_.toString());
        assertTrue(Essai.contient(ecran(a), "2 SMS · 106 caractères"));
    }

    @Test
    public void nouveauMessageVersUnContactOuvreSaConversation() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouveau message");
        Essai.saisir(ecran(a), "Nom ou numéro", "zoe");
        assertFalse(Essai.visible(ecran(a), "Alice Durand"));
        Essai.appuyer(ecran(a), "Zoé Martin");
        assertNotNull("la conversation existante est retrouvée", Essai.trouver(ecran(a), "Reçu : Tu viens samedi ?"));
        a.onBackPressed();
        Essai.attendre();
        assertTrue("le retour revient à la liste, pas à l'écran de choix", Essai.visible(ecran(a), "MaBanque"));
    }

    @Test
    public void nouveauMessageVersUnNumeroInconnuCommenceUneConversation() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouveau message");
        assertFalse("pas de bouton tant que rien ne ressemble à un numéro", Essai.visible(ecran(a), "Écrire à ce numéro"));
        Essai.saisir(ecran(a), "Nom ou numéro", "06 99 88 77 66");
        Essai.appuyer(ecran(a), "Écrire à ce numéro");
        Essai.saisir(ecran(a), "Message", "Bonjour");
        Essai.appuyer(ecran(a), "Envoyer");
        assertEquals(Arrays.asList("06 99 88 77 66|Bonjour"), boite.envois);
        assertNotNull(Essai.trouver(ecran(a), "Envoyé : Bonjour"));
    }

    @Test
    public void uneAutreAppliPeutOuvrirUneConversationAvecUnTextePrepare() {
        Intent demande = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", "0612345678", null)).putExtra("sms_body", "À tout de suite");
        ActiviteEssai a = Robolectric.buildActivity(ActiviteEssai.class, demande).setup().get();
        Essai.attendre();
        assertTrue(Essai.contient(ecran(a), "Zoé Martin"));
        assertEquals("À tout de suite", Essai.champ(ecran(a), "Message").getText().toString());
        assertTrue("rien ne part sans appui sur Envoyer", boite.envois.isEmpty());
        assertNotNull(Essai.trouver(ecran(a), "Reçu : Super, à samedi !"));
    }

    @Test
    public void appelerOuvreLeTelephoneSurLeNumeroDeLaConversation() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Zoé Martin");
        Essai.appuyer(ecran(a), "Appeler");
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals(Intent.ACTION_DIAL, lance.getAction());
        assertEquals("+33612345678", lance.getData().getSchemeSpecificPart());
    }
}
