package fr.manboyx.appareil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import fr.manboyx.appareil.logique.Flash;
import fr.manboyx.commun.Fond;
import fr.manboyx.essais.Essai;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Pilote l'écran de prise de vue avec une fausse chambre, qui note ce qu'on lui demande. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcranTest {
    static final class FausseChambre implements Chambre {
        final List<String> appels = new ArrayList<>();
        Ecoute ecoute;
        boolean deuxCotes = true;
        boolean ouverte;

        @Override
        public void ecouter(Ecoute ecoute) {
            this.ecoute = ecoute;
        }

        @Override
        public boolean deuxCotes() {
            return deuxCotes;
        }

        @Override
        public void ouvrir(SurfaceTexture surface, boolean frontale) {
            ouverte = true;
            appels.add(frontale ? "ouvrir de face" : "ouvrir de dos");
        }

        @Override
        public void fermer() {
            if (ouverte) {
                appels.add("fermer");
            }
            ouverte = false;
        }

        @Override
        public void flash(Flash mode) {
            appels.add("flash " + mode);
        }

        @Override
        public void zoom(float facteur) {
            appels.add("zoom");
        }

        @Override
        public void prendre(int tenue) {
            appels.add("prendre");
        }

        /** L'appareil annonce que l'aperçu tourne. */
        void prete(boolean frontale, boolean flash) {
            ecoute.pret(1440, 1080, frontale, flash, 4f);
        }
    }

    static FausseChambre chambre;
    static List<String> rangees;
    static boolean rangementEnPanne;

    public static class ActiviteEssai extends AppareilActivity {
        @Override
        public Chambre chambre() {
            return chambre;
        }

        @Override
        public Rangement rangement() {
            return (jpeg, nom) -> {
                if (rangementEnPanne) {
                    throw new IOException("mémoire pleine");
                }
                rangees.add(nom + " (" + jpeg.length + " octets)");
                return Uri.parse("content://media/external/images/media/" + rangees.size());
            };
        }
    }

    private Application appli;
    private ActivityController<ActiviteEssai> controleur;

    @Before
    public void preparer() {
        Fond.utiliser(Runnable::run);
        appli = RuntimeEnvironment.getApplication();
        shadowOf(appli).grantPermissions(AppareilActivity.demandees());
        chambre = new FausseChambre();
        rangees = new ArrayList<>();
        rangementEnPanne = false;
    }

    /** Ouvre l'appli et fait comme si l'endroit où dessiner l'aperçu venait d'être prêt. */
    private ActiviteEssai ouvrir(Intent demande) {
        controleur = demande == null ? Robolectric.buildActivity(ActiviteEssai.class) : Robolectric.buildActivity(ActiviteEssai.class, demande);
        ActiviteEssai a = controleur.setup().get();
        ((EcranPrise) a.ecran()).surfaceDisponible(new SurfaceTexture(0));
        Essai.attendre();
        return a;
    }

    private static View ecran(ActiviteEssai a) {
        return a.getWindow().getDecorView();
    }

    private static byte[] jpeg() {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        android.graphics.Bitmap.createBitmap(64, 48, android.graphics.Bitmap.Config.ARGB_8888)
                .compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, sortie);
        return sortie.toByteArray();
    }

    @Test
    public void sansAutorisationLAppareilPhotoNEstJamaisOuvert() {
        shadowOf(appli).denyPermissions(AppareilActivity.demandees());
        ActiviteEssai a = ouvrir(null);
        assertTrue(Essai.contient(ecran(a), "a besoin de l'appareil photo"));
        assertTrue(chambre.appels.toString(), chambre.appels.isEmpty());
    }

    @Test
    public void aLOuvertureLaCameraArriereDemarre() {
        ouvrir(null);
        assertEquals("ouvrir de dos", chambre.appels.get(0));
    }

    @Test
    public void leDeclencheurNeFaitRienTantQueLAppareilNEstPasPret() {
        ActiviteEssai a = ouvrir(null);
        Essai.appuyer(ecran(a), "Prendre la photo");
        assertFalse(chambre.appels.contains("prendre"));
        chambre.prete(false, true);
        Essai.appuyer(ecran(a), "Prendre la photo");
        assertTrue(chambre.appels.contains("prendre"));
    }

    @Test
    public void laPhotoPriseEstRangeeEtSaVignetteApparait() {
        ActiviteEssai a = ouvrir(null);
        chambre.prete(false, true);
        assertEquals(View.INVISIBLE, Essai.trouverMemeCache(ecran(a), "Dernière photo").getVisibility());
        Essai.appuyer(ecran(a), "Prendre la photo");
        byte[] photo = jpeg();
        chambre.ecoute.photo(photo, 90);
        Essai.attendre();
        assertEquals(1, rangees.size());
        assertTrue(rangees.get(0), rangees.get(0).matches("IMG_\\d{8}_\\d{6}\\.jpg \\(" + photo.length + " octets\\)"));
        assertEquals(View.VISIBLE, Essai.trouverMemeCache(ecran(a), "Dernière photo").getVisibility());
    }

    @Test
    public void unDeuxiemeAppuiPendantLEnregistrementNePrendPasDeDeuxiemePhoto() {
        ActiviteEssai a = ouvrir(null);
        chambre.prete(false, true);
        Essai.appuyer(ecran(a), "Prendre la photo");
        Essai.appuyer(ecran(a), "Prendre la photo");
        assertEquals(1, java.util.Collections.frequency(chambre.appels, "prendre"));
        chambre.ecoute.photo(jpeg(), 0);
        Essai.attendre();
        Essai.appuyer(ecran(a), "Prendre la photo");
        assertEquals(2, java.util.Collections.frequency(chambre.appels, "prendre"));
    }

    @Test
    public void siLaPhotoNePeutPasEtreRangeeOnLeDitEtOnPeutReessayer() {
        rangementEnPanne = true;
        ActiviteEssai a = ouvrir(null);
        chambre.prete(false, true);
        Essai.appuyer(ecran(a), "Prendre la photo");
        chambre.ecoute.photo(jpeg(), 0);
        Essai.attendre();
        assertEquals("La photo n'a pas pu être enregistrée.", ShadowToast.getTextOfLatestToast());
        rangementEnPanne = false;
        Essai.appuyer(ecran(a), "Prendre la photo");
        assertEquals(2, java.util.Collections.frequency(chambre.appels, "prendre"));
    }

    @Test
    public void leBoutonDuFlashFaitDefilerLesReglages() {
        ActiviteEssai a = ouvrir(null);
        chambre.prete(false, true);
        assertEquals("Flash auto", ((TextView) Essai.trouver(ecran(a), "Flash")).getText().toString());
        Essai.appuyer(ecran(a), "Flash");
        assertEquals("Flash activé", ((TextView) Essai.trouver(ecran(a), "Flash")).getText().toString());
        assertEquals("flash OUI", chambre.appels.get(chambre.appels.size() - 1));
        Essai.appuyer(ecran(a), "Flash");
        Essai.appuyer(ecran(a), "Flash");
        assertEquals("flash AUTO", chambre.appels.get(chambre.appels.size() - 1));
    }

    @Test
    public void sansFlashLeBoutonDisparait() {
        ActiviteEssai a = ouvrir(null);
        chambre.prete(true, false);
        assertNull(Essai.trouver(ecran(a), "Flash"));
    }

    @Test
    public void retournerPasseALaCameraDeFace() {
        ActiviteEssai a = ouvrir(null);
        chambre.prete(false, true);
        Essai.appuyer(ecran(a), "Changer de caméra");
        assertEquals("ouvrir de face", chambre.appels.get(chambre.appels.size() - 1));
        assertTrue("l'ancienne est libérée avant", chambre.appels.contains("ouvrir de dos"));
    }

    @Test
    public void avecUneSeuleCameraLeBoutonRetournerNEstPasPropose() {
        chambre.deuxCotes = false;
        ActiviteEssai a = ouvrir(null);
        assertNull(Essai.trouver(ecran(a), "Changer de caméra"));
    }

    @Test
    public void lAppareilPhotoEstLibereDesQueLAppliQuitteLEcran() {
        ouvrir(null);
        chambre.prete(false, true);
        controleur.pause();
        assertEquals("fermer", chambre.appels.get(chambre.appels.size() - 1));
        assertFalse(chambre.ouverte);
        controleur.resume();
        assertTrue("il est repris au retour", chambre.ouverte);
    }

    @Test
    public void unePanneDeLAppareilEstAfficheeEtBloqueLeDeclencheur() {
        ActiviteEssai a = ouvrir(null);
        chambre.ecoute.erreur("L'appareil photo n'a pas pu s'ouvrir.");
        assertTrue(Essai.visible(ecran(a), "L'appareil photo n'a pas pu s'ouvrir."));
        Essai.appuyer(ecran(a), "Prendre la photo");
        assertFalse(chambre.appels.contains("prendre"));
    }

    @Test
    public void appuyerSurLaVignetteOuvreLaDernierePhoto() {
        ActiviteEssai a = ouvrir(null);
        chambre.prete(false, true);
        Essai.appuyer(ecran(a), "Prendre la photo");
        chambre.ecoute.photo(jpeg(), 0);
        Essai.attendre();
        Essai.appuyer(ecran(a), "Dernière photo");
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals(Intent.ACTION_VIEW, lance.getAction());
        assertEquals("content://media/external/images/media/1", lance.getDataString());
    }

    @Test
    public void pourUneAutreAppliLaPhotoEstEcriteLaOuElleLeDemandeEtPasDansLaGalerie() {
        Uri destination = Uri.parse("content://autre.appli/photo/7");
        ByteArrayOutputStream recu = new ByteArrayOutputStream();
        shadowOf(appli.getContentResolver()).registerOutputStream(destination, recu);
        Intent demande = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, destination);
        ActiviteEssai a = ouvrir(demande);
        chambre.prete(false, true);
        Essai.appuyer(ecran(a), "Prendre la photo");
        byte[] photo = jpeg();
        chambre.ecoute.photo(photo, 0);
        Essai.attendre();
        assertEquals(photo.length, recu.size());
        assertTrue("rien n'est ajouté aux photos du téléphone", rangees.isEmpty());
        assertEquals(Activity.RESULT_OK, shadowOf(a).getResultCode());
        assertTrue(a.isFinishing());
    }

    /** Lance une demande de photo venue d'une autre appli, prend la photo, et rend ce qui a été écrit à `destination`. */
    private int demander(ActiviteEssai[] ouverte, Uri destination) {
        ByteArrayOutputStream recu = new ByteArrayOutputStream();
        shadowOf(appli.getContentResolver()).registerOutputStream(destination, recu);
        ActiviteEssai a = ouvrir(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, destination));
        ouverte[0] = a;
        chambre.prete(false, true);
        Essai.appuyer(ecran(a), "Prendre la photo");
        chambre.ecoute.photo(jpeg(), 0);
        Essai.attendre();
        return recu.size();
    }

    @Test
    public void uneDestinationQuiNEstPasPreteeParUneAppliEstRefusee() {
        // Un chemin de fichier donné tel quel : rien ne prouve que l'appli qui demande y a droit.
        ActiviteEssai[] a = new ActiviteEssai[1];
        assertEquals(0, demander(a, Uri.parse("file:///storage/emulated/0/DCIM/Camera/photo-de-famille.jpg")));
        assertEquals("La photo n'a pas pu être transmise.", ShadowToast.getTextOfLatestToast());
        assertFalse(a[0].isFinishing());
        assertEquals(Activity.RESULT_CANCELED, shadowOf(a[0]).getResultCode());
    }

    @Test
    public void uneDestinationQuiContientDejaQuelqueChoseNEstJamaisEcrasee() {
        fr.manboyx.essais.FauxFournisseur existant = fr.manboyx.essais.FauxFournisseur.installer("autre.appli.fichiers");
        existant.lecture = (uri, colonnes, condition, arguments) -> {
            android.database.MatrixCursor c = new android.database.MatrixCursor(colonnes);
            c.addRow(new Object[] {2_500_000L});
            return c;
        };
        ActiviteEssai[] a = new ActiviteEssai[1];
        assertEquals(0, demander(a, Uri.parse("content://autre.appli.fichiers/photo-existante")));
        assertEquals("La photo n'a pas pu être transmise.", ShadowToast.getTextOfLatestToast());
        assertFalse(a[0].isFinishing());
    }

    @Test
    public void uneDestinationVideEstAcceptee() {
        fr.manboyx.essais.FauxFournisseur neuf = fr.manboyx.essais.FauxFournisseur.installer("autre.appli.neuf");
        neuf.lecture = (uri, colonnes, condition, arguments) -> {
            android.database.MatrixCursor c = new android.database.MatrixCursor(colonnes);
            c.addRow(new Object[] {0L});
            return c;
        };
        ActiviteEssai[] a = new ActiviteEssai[1];
        assertTrue(demander(a, Uri.parse("content://autre.appli.neuf/photo-a-remplir")) > 0);
        assertEquals(Activity.RESULT_OK, shadowOf(a[0]).getResultCode());
    }
}
