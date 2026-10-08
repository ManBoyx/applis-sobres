package fr.manboyx.galerie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.GridView;

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

/** Pilote l'appli comme le ferait quelqu'un, sur des photos fabriquées en mémoire. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcransTest {
    static PhotosMemoire photos;

    public static class ActiviteEssai extends GalerieActivity {
        @Override
        public Depot depot() {
            return photos;
        }
    }

    private Application appli;

    static void remplir() {
        long jour = 24 * 3600_000L;
        long maintenant = System.currentTimeMillis();
        photos = new PhotosMemoire();
        photos.ajouter(1, false, "Camera", maintenant - 5 * jour);
        photos.ajouter(2, false, "Camera", maintenant - 4 * jour);
        photos.ajouter(3, true, "Camera", maintenant - 3 * jour);
        photos.ajouter(4, false, "Screenshots", maintenant - 2 * jour);
        photos.ajouter(5, false, "Camera", maintenant - jour);
    }

    @Before
    public void preparer() {
        Fond.utiliser(Runnable::run);
        appli = RuntimeEnvironment.getApplication();
        shadowOf(appli).grantPermissions(GalerieActivity.demandees());
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

    private static EcranVue vue(ActiviteEssai a) {
        return (EcranVue) a.ecran();
    }

    @Test
    public void laGrilleMontreToutesLesPhotosLaPlusRecenteEnPremier() {
        ActiviteEssai a = ouvrir();
        assertNotNull(Essai.trouver(ecran(a), "Photo IMG_5.jpg"));
        assertNotNull(Essai.trouver(ecran(a), "Vidéo VID_3.mp4"));
        assertNotNull(Essai.trouver(ecran(a), "Photo IMG_1.jpg"));
        GridView grille = (GridView) Essai.trouver(ecran(a), "Photo IMG_5.jpg").getParent().getParent();
        assertEquals(5, grille.getAdapter().getCount());
        assertEquals(5, grille.getAdapter().getItemId(0));
    }

    @Test
    public void sansAutorisationLAppliExpliquePourquoiEtNeMontreAucunePhoto() {
        shadowOf(appli).denyPermissions(GalerieActivity.demandees());
        ActiviteEssai a = ouvrir();
        assertTrue(Essai.contient(ecran(a), "a besoin d'y accéder"));
        assertNull(Essai.trouver(ecran(a), "Photo IMG_5.jpg"));
    }

    @Test
    public void sansPhotoLAppliLeDit() {
        photos.medias.clear();
        assertTrue(Essai.visible(ecran(ouvrir()), "Aucune photo ni vidéo sur ce téléphone."));
    }

    @Test
    public void lOngletAlbumsListeLesDossiersAvecLeurNombre() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Albums");
        assertTrue(Essai.visible(ecran(a), "Camera"));
        assertTrue(Essai.visible(ecran(a), "4 éléments"));
        assertTrue(Essai.visible(ecran(a), "Screenshots"));
        assertTrue(Essai.visible(ecran(a), "1 élément"));
        assertNull("la grille de toutes les photos est cachée", Essai.trouver(ecran(a), "Photo IMG_5.jpg"));
    }

    @Test
    public void ouvrirUnAlbumNeMontreQueSonContenu() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Albums");
        Essai.appuyer(ecran(a), "Screenshots");
        assertNotNull(Essai.trouver(ecran(a), "Photo IMG_4.jpg"));
        assertNull(Essai.trouver(ecran(a), "Photo IMG_5.jpg"));
        a.onBackPressed();
        Essai.attendre();
        assertTrue(Essai.visible(ecran(a), "Camera"));
    }

    @Test
    public void appuyerSurUnePhotoLOuvreEnGrandAvecSaPositionEtSesInfos() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Photo IMG_4.jpg");
        assertEquals(4, vue(a).courant().id);
        assertTrue(Essai.visible(ecran(a), "2 / 5"));
        assertTrue(Essai.contient(ecran(a), "IMG_4.jpg"));
        assertTrue(Essai.contient(ecran(a), "2,4 Mo"));
        assertFalse(Essai.visible(ecran(a), "Lire la vidéo"));
    }

    @Test
    public void glisserPasseALaSuivanteSansDepasserLesBouts() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Photo IMG_5.jpg");
        vue(a).aller(-1);
        assertEquals("déjà la première", 5, vue(a).courant().id);
        vue(a).aller(1);
        Essai.attendre();
        assertEquals(4, vue(a).courant().id);
        for (int i = 0; i < 10; i++) {
            vue(a).aller(1);
        }
        assertEquals("on s'arrête à la dernière", 1, vue(a).courant().id);
        assertTrue(Essai.visible(ecran(a), "5 / 5"));
    }

    @Test
    public void uneVideoProposeDeLaLireDansLeLecteurDuTelephone() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Vidéo VID_3.mp4");
        Essai.appuyer(ecran(a), "Lire la vidéo");
        Intent lance = shadowOf(a).getNextStartedActivity();
        assertEquals(Intent.ACTION_VIEW, lance.getAction());
        assertEquals("video/mp4", lance.getType());
        assertEquals("content://media/external/video/media/3", lance.getDataString());
    }

    @Test
    public void partagerEnvoieLaPhotoAffichee() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Photo IMG_4.jpg");
        Essai.appuyer(ecran(a), "Partager");
        Intent envoi = shadowOf(a).getNextStartedActivity().getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals("image/jpeg", envoi.getType());
        Uri jointe = envoi.getParcelableExtra(Intent.EXTRA_STREAM);
        assertEquals("content://media/external/images/media/4", jointe.toString());
        assertTrue((envoi.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }

    @Test
    public void supprimerDemandeConfirmationPuisMontreLaPhotoSuivante() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Photo IMG_4.jpg");
        Essai.appuyer(ecran(a), "Supprimer");
        assertEquals("rien n'est supprimé avant la confirmation", 5, photos.medias.size());
        Essai.valider();
        assertEquals(4, photos.medias.size());
        assertTrue(photos.medias.stream().noneMatch(m -> m.id == 4));
        assertEquals(3, vue(a).courant().id);
        assertTrue(Essai.visible(ecran(a), "2 / 4"));
    }

    @Test
    public void quandAndroidDemandeLuiMemeLaPhotoNePartQuApresSonAccord() {
        photos.suppressionDirecte = false;
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Photo IMG_4.jpg");
        Essai.appuyer(ecran(a), "Supprimer");
        assertEquals(4, photos.aSupprimer.id);
        // L'utilisateur refuse dans la boîte d'Android : la photo reste affichée.
        vue(a).resultat(2, Activity.RESULT_CANCELED, null);
        assertEquals(4, vue(a).courant().id);
        assertTrue(Essai.visible(ecran(a), "2 / 5"));
        // Il recommence et accepte : elle quitte l'écran.
        Essai.appuyer(ecran(a), "Supprimer");
        vue(a).resultat(2, Activity.RESULT_OK, null);
        Essai.attendre();
        assertEquals(3, vue(a).courant().id);
    }

    @Test
    public void supprimerLaDernierePhotoRefermeLaVue() {
        photos.medias.removeIf(m -> m.id != 4);
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Albums");
        Essai.appuyer(ecran(a), "Screenshots");
        Essai.appuyer(ecran(a), "Photo IMG_4.jpg");
        Essai.appuyer(ecran(a), "Supprimer");
        Essai.valider();
        assertFalse(a.ecran() instanceof EcranVue);
        assertTrue("l'album vide se referme aussi", a.ecran() instanceof EcranGalerie);
    }

    @Test
    public void uneImageIllisibleLeDitSansPlanter() {
        photos.ajouter(404, false, "Camera", System.currentTimeMillis());
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Photo IMG_404.jpg");
        assertEquals("Cette image ne peut pas être affichée.", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void quitterLaVueNeGardeAucuneGrandeImageEnMemoire() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Photo IMG_4.jpg");
        View zoom = ((android.view.ViewGroup) ((android.widget.FrameLayout) ecran(a).findViewById(android.R.id.content)).getChildAt(0)).getChildAt(0);
        VueZoom image = (VueZoom) ((android.view.ViewGroup) zoom).getChildAt(0);
        assertTrue(image.aUneImage());
        a.onBackPressed();
        Essai.attendre();
        assertFalse(image.aUneImage());
    }

    @Test
    public void uneImageEnvoyeeParUneAutreAppliSOuvreSeuleSansBoutonSupprimer() {
        Intent demande = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://autre.appli/image/9"), "image/png");
        ActiviteEssai a = Robolectric.buildActivity(ActiviteEssai.class, demande).setup().get();
        Essai.attendre();
        assertEquals("content://autre.appli/image/9", vue(a).courant().adresse);
        assertFalse(Essai.visible(ecran(a), "Supprimer"));
        assertTrue(Essai.visible(ecran(a), "Partager"));
        a.onBackPressed();
        assertTrue("le retour rend la main à l'autre appli", a.isFinishing());
    }
}
