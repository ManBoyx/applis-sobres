package fr.manboyx.telephone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import fr.manboyx.commun.Fond;
import fr.manboyx.essais.Essai;
import fr.manboyx.telephone.logique.Appel;
import fr.manboyx.commun.Correspondant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Pilote l'appli comme le ferait quelqu'un, sur un journal et un carnet en mémoire. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcransTest {
    static List<Appel> journal;
    static List<Correspondant> carnet;

    public static class ActiviteEssai extends TelephoneActivity {
        @Override
        public Depot depot() {
            return new Depot() {
                @Override
                public List<Appel> journal(int maximum) {
                    return journal;
                }

                @Override
                public List<Correspondant> carnet() {
                    return carnet;
                }
            };
        }
    }

    private Application appli;

    static void remplir() {
        long maintenant = System.currentTimeMillis();
        journal = new ArrayList<>();
        journal.add(new Appel("0612345678", "Zoé Martin", Appel.MANQUE, maintenant - 60_000, 0));
        journal.add(new Appel("0612345678", "Zoé Martin", Appel.MANQUE, maintenant - 120_000, 0));
        journal.add(new Appel("0144556677", "", Appel.SORTANT, maintenant - 3_600_000, 185));
        journal.add(new Appel("", "", Appel.ENTRANT, maintenant - 7_200_000, 45));
        carnet = new ArrayList<>();
        carnet.add(new Correspondant("Zoé Martin", "06 12 34 56 78", false));
        carnet.add(new Correspondant("Alice Durand", "07 00 00 00 01", true));
        carnet.add(new Correspondant("Bruno Petit", "01 23 45 67 89", false));
    }

    @Before
    public void preparer() {
        Fond.utiliser(Runnable::run);
        appli = RuntimeEnvironment.getApplication();
        shadowOf(appli).grantPermissions(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS);
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

    private static String compose(ActiviteEssai a) {
        return ((TextView) Essai.trouver(ecran(a), "Numéro composé")).getText().toString();
    }

    private static void taper(ActiviteEssai a, String chiffres) {
        for (char c : chiffres.toCharArray()) {
            Essai.appuyer(ecran(a), "Touche " + c);
        }
    }

    @Test
    public void lesTouchesComposentLeNumeroEtLaFlecheEfface() {
        ActiviteEssai a = ouvrir();
        taper(a, "0612");
        assertEquals("0612", compose(a));
        Essai.appuyer(ecran(a), "Effacer");
        assertEquals("061", compose(a));
    }

    @Test
    public void unNumeroCompletEstEcritParPaires() {
        ActiviteEssai a = ouvrir();
        taper(a, "0612345678");
        assertEquals("06 12 34 56 78", compose(a));
    }

    @Test
    public void unAppuiLongSurZeroDonneLePlusEtSurEffacerVideTout() {
        ActiviteEssai a = ouvrir();
        Essai.trouver(ecran(a), "Touche 0").performLongClick();
        taper(a, "33");
        assertEquals("+33", compose(a));
        Essai.trouver(ecran(a), "Effacer").performLongClick();
        assertEquals("", compose(a));
    }

    @Test
    public void appelerLanceLAppelVersLeNumeroCompose() {
        ActiviteEssai a = ouvrir();
        taper(a, "0612345678");
        Essai.appuyer(ecran(a), "Appeler");
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals(Intent.ACTION_CALL, lance.getAction());
        assertEquals(Uri.fromParts("tel", "0612345678", null), lance.getData());
    }

    @Test
    public void lesCodesAvecDieseSontTransmisEntiers() {
        ActiviteEssai a = ouvrir();
        taper(a, "*123#");
        Essai.appuyer(ecran(a), "Appeler");
        assertEquals("*123#", shadowOf(a).getNextStartedActivity().getData().getSchemeSpecificPart());
    }

    @Test
    public void sansNumeroAppelerNeFaitRien() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Appeler");
        assertNull(shadowOf(a).getNextStartedActivity());
    }

    @Test
    public void sansAutorisationDAppelerLAppliLaDemandeAvantTout() {
        shadowOf(appli).denyPermissions(Manifest.permission.CALL_PHONE);
        ActiviteEssai a = ouvrir();
        taper(a, "0612345678");
        Essai.appuyer(ecran(a), "Appeler");
        assertEquals(Manifest.permission.CALL_PHONE, shadowOf(a).getLastRequestedPermission().requestedPermissions[0]);
        // Le seul écran lancé est la question d'Android, pas un appel.
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals("android.content.pm.action.REQUEST_PERMISSIONS", lance.getAction());
        assertNull(shadowOf(a).getNextStartedActivity());
    }

    @Test
    public void siLAutorisationEstRefuseeLeNumeroEstConfieAuTelephone() {
        shadowOf(appli).denyPermissions(Manifest.permission.CALL_PHONE);
        ActiviteEssai a = ouvrir();
        taper(a, "0612345678");
        Essai.appuyer(ecran(a), "Appeler");
        a.onRequestPermissionsResult(1, new String[] {Manifest.permission.CALL_PHONE}, new int[] {android.content.pm.PackageManager.PERMISSION_DENIED});
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals(Intent.ACTION_DIAL, lance.getAction());
        assertEquals("0612345678", lance.getData().getSchemeSpecificPart());
    }

    @Test
    public void enTapantLesContactsRessemblantsSontProposes() {
        ActiviteEssai a = ouvrir();
        taper(a, "0612");
        assertTrue(Essai.visible(ecran(a), "Zoé Martin"));
        assertFalse(Essai.visible(ecran(a), "Alice Durand"));
        Essai.appuyer(ecran(a), "Zoé Martin");
        assertEquals("06 12 34 56 78", compose(a));
    }

    @Test
    public void leJournalReunitLesAppelsRepetesEtNommeLesCorrespondants() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Journal");
        assertTrue(Essai.visible(ecran(a), "Zoé Martin (2)"));
        assertTrue(Essai.visible(ecran(a), "Appel manqué"));
        assertTrue(Essai.visible(ecran(a), "01 44 55 66 77"));
        assertTrue(Essai.visible(ecran(a), "Sortant · 3 min 05 s"));
        assertTrue(Essai.visible(ecran(a), "Numéro masqué"));
    }

    @Test
    public void appuyerSurUneLigneDuJournalRappelle() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Journal");
        Essai.appuyer(ecran(a), "01 44 55 66 77");
        assertEquals("0144556677", shadowOf(a).getNextStartedActivity().getData().getSchemeSpecificPart());
    }

    @Test
    public void unNumeroMasqueNePeutPasEtreRappele() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Journal");
        Essai.appuyer(ecran(a), "Numéro masqué");
        assertNull(shadowOf(a).getNextStartedActivity());
    }

    @Test
    public void sansAutorisationLeJournalExpliquePourquoi() {
        shadowOf(appli).denyPermissions(Manifest.permission.READ_CALL_LOG);
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Journal");
        assertTrue(Essai.contient(ecran(a), "lire le journal d'appels"));
        assertFalse(Essai.contient(ecran(a), "Zoé Martin"));
    }

    @Test
    public void lOngletContactsListeLesFavorisDAbordEtAppelleDUnAppui() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Contacts");
        List<String> textes = Essai.textes(ecran(a));
        assertTrue(textes.indexOf("Alice Durand") < textes.indexOf("Bruno Petit"));
        assertTrue(textes.indexOf("Bruno Petit") < textes.indexOf("Zoé Martin"));
        Essai.appuyer(ecran(a), "Bruno Petit");
        assertEquals("01 23 45 67 89", shadowOf(a).getNextStartedActivity().getData().getSchemeSpecificPart());
    }

    @Test
    public void laRechercheDeContactsFiltreParNom() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Contacts");
        Essai.saisir(ecran(a), "Rechercher un nom ou un numéro", "zoe");
        assertTrue(Essai.visible(ecran(a), "Zoé Martin"));
        assertFalse(Essai.visible(ecran(a), "Bruno Petit"));
    }

    @Test
    public void sansAutorisationLesContactsNeSontNiListesNiProposes() {
        shadowOf(appli).denyPermissions(Manifest.permission.READ_CONTACTS);
        ActiviteEssai a = ouvrir();
        taper(a, "0612");
        assertFalse(Essai.visible(ecran(a), "Zoé Martin"));
        Essai.appuyer(ecran(a), "Contacts");
        assertTrue(Essai.contient(ecran(a), "lire vos contacts"));
    }

    @Test
    public void unNumeroEnvoyeParUneAutreAppliArriveDejaCompose() {
        Intent demande = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", "06 12 34 56 78", null));
        ActiviteEssai a = Robolectric.buildActivity(ActiviteEssai.class, demande).setup().get();
        Essai.attendre();
        assertEquals("06 12 34 56 78", compose(a));
        assertNull("composer n'est pas appeler", shadowOf(a).getNextStartedActivity());
    }
}
