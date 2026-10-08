package fr.manboyx.fichiers.logique;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DossierTest {
    private File racine;

    @Before
    public void preparer() throws Exception {
        racine = OutilsDossier.temporaire();
        new File(racine, "Photos").mkdir();
        new File(racine, "documents").mkdir();
        OutilsDossier.ecrire(racine, "zèbre.txt", "12345").setLastModified(3_000_000);
        OutilsDossier.ecrire(racine, "Alpha.pdf", "1234567890").setLastModified(1_000_000);
        OutilsDossier.ecrire(racine, "émile.jpg", "1").setLastModified(2_000_000);
        OutilsDossier.ecrire(racine, ".cache", "x");
    }

    @After
    public void nettoyer() {
        OutilsDossier.effacer(racine);
    }

    private List<String> noms(Dossier.Tri tri, boolean caches) {
        List<String> noms = new ArrayList<>();
        for (Entree e : Dossier.lister(racine, tri, caches)) {
            noms.add(e.nom);
        }
        return noms;
    }

    @Test
    public void lesDossiersPassentAvantLesFichiersEtLeTriIgnoreAccentsEtMajuscules() {
        assertEquals(Arrays.asList("documents", "Photos", "Alpha.pdf", "émile.jpg", "zèbre.txt"), noms(Dossier.Tri.NOM, false));
    }

    @Test
    public void leTriParDateMetLePlusRecentEnTete() {
        assertEquals(Arrays.asList("zèbre.txt", "émile.jpg", "Alpha.pdf"), noms(Dossier.Tri.DATE, false).subList(2, 5));
    }

    @Test
    public void leTriParTailleMetLePlusGrosEnTete() {
        assertEquals(Arrays.asList("Alpha.pdf", "zèbre.txt", "émile.jpg"), noms(Dossier.Tri.TAILLE, false).subList(2, 5));
    }

    @Test
    public void lesFichiersCachesNApparaissentQueSurDemande() {
        assertEquals(5, noms(Dossier.Tri.NOM, false).size());
        assertEquals(".cache", noms(Dossier.Tri.NOM, true).get(2));
    }

    @Test
    public void unDossierInexistantOuUnFichierDonneUneListeVide() {
        assertEquals(0, Dossier.lister(new File(racine, "absent"), Dossier.Tri.NOM, false).size());
        assertEquals(0, Dossier.lister(new File(racine, "Alpha.pdf"), Dossier.Tri.NOM, false).size());
    }

    @Test
    public void lesTaillesSontLisibles() {
        assertEquals("0 o", Dossier.taille(0));
        assertEquals("999 o", Dossier.taille(999));
        assertEquals("1 Ko", Dossier.taille(1024));
        assertEquals("12 Ko", Dossier.taille(12_800));
        assertEquals("3,4 Mo", Dossier.taille(3_565_158));
        assertEquals("1,25 Go", Dossier.taille(1_342_177_280L));
    }

    @Test
    public void lExtensionEstLueEnMinusculesSansLePoint() {
        assertEquals("jpg", Dossier.extension("Photo.JPG"));
        assertEquals("gz", Dossier.extension("archive.tar.gz"));
        assertEquals("", Dossier.extension("sans-extension"));
        assertEquals("un fichier caché n'a pas d'extension", "", Dossier.extension(".cache"));
        assertEquals("", Dossier.extension("point-final."));
    }
}
