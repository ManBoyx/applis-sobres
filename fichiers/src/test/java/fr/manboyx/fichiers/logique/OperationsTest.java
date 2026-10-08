package fr.manboyx.fichiers.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class OperationsTest {
    private File racine;
    private File a;
    private File b;

    @Before
    public void preparer() throws Exception {
        racine = OutilsDossier.temporaire();
        a = new File(racine, "A");
        b = new File(racine, "B");
        a.mkdir();
        b.mkdir();
    }

    @After
    public void nettoyer() {
        OutilsDossier.effacer(racine);
    }

    private interface Action {
        void faire() throws IOException;
    }

    private static String refuse(Action action) {
        try {
            action.faire();
            fail("l'opération aurait dû être refusée");
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    @Test
    public void unNomVideOuAvecUneBarreOuDejaPrisEstRefuseAvecLaRaison() throws Exception {
        OutilsDossier.ecrire(a, "pris.txt", "x");
        assertNull(Operations.erreurDeNom(a, "libre.txt"));
        assertNotNull(Operations.erreurDeNom(a, "  "));
        assertNotNull(Operations.erreurDeNom(a, "sous/dossier"));
        assertNotNull(Operations.erreurDeNom(a, ".."));
        assertTrue(Operations.erreurDeNom(a, "pris.txt").contains("existe déjà"));
    }

    @Test
    public void creerUnDossier() throws Exception {
        File cree = Operations.creerDossier(a, " Vacances ");
        assertTrue(cree.isDirectory());
        assertEquals("Vacances", cree.getName());
        assertTrue(refuse(() -> Operations.creerDossier(a, "Vacances")).contains("existe déjà"));
    }

    @Test
    public void renommerGardeLeContenuEtRefuseUnNomPris() throws Exception {
        File fichier = OutilsDossier.ecrire(a, "ancien.txt", "contenu");
        OutilsDossier.ecrire(a, "autre.txt", "x");
        File renomme = Operations.renommer(fichier, "nouveau.txt");
        assertEquals("contenu", OutilsDossier.lire(renomme));
        assertFalse(fichier.exists());
        assertTrue(refuse(() -> Operations.renommer(renomme, "autre.txt")).contains("existe déjà"));
        assertEquals("un refus ne touche à rien", "contenu", OutilsDossier.lire(renomme));
    }

    @Test
    public void copierUnFichierLaisseLOriginalEnPlace() throws Exception {
        File source = OutilsDossier.ecrire(a, "note.txt", "bonjour");
        File copie = Operations.copier(source, b);
        assertEquals(new File(b, "note.txt"), copie);
        assertEquals("bonjour", OutilsDossier.lire(copie));
        assertEquals("bonjour", OutilsDossier.lire(source));
    }

    @Test
    public void copierVersUnNomDejaPrisNEcraseRien() throws Exception {
        File source = OutilsDossier.ecrire(a, "note.txt", "nouveau");
        OutilsDossier.ecrire(b, "note.txt", "ancien");
        File copie = Operations.copier(source, b);
        assertEquals("note (2).txt", copie.getName());
        assertEquals("ancien", OutilsDossier.lire(new File(b, "note.txt")));
        assertEquals("note (3).txt", Operations.copier(source, b).getName());
    }

    @Test
    public void copierDansSonPropreDossierFaitUnDouble() throws Exception {
        File source = OutilsDossier.ecrire(a, "note.txt", "x");
        assertEquals("note (2).txt", Operations.copier(source, a).getName());
    }

    @Test
    public void copierUnDossierEmporteToutSonContenu() throws Exception {
        OutilsDossier.ecrire(a, "sous/profond/fichier.txt", "au fond");
        OutilsDossier.ecrire(a, "haut.txt", "en haut");
        File copie = Operations.copier(a, b);
        assertEquals("au fond", OutilsDossier.lire(new File(copie, "sous/profond/fichier.txt")));
        assertEquals("en haut", OutilsDossier.lire(new File(copie, "haut.txt")));
        assertTrue(new File(a, "haut.txt").exists());
    }

    @Test
    public void unDossierNePeutPasEtreCopieNiDeplaceDansLuiMeme() throws Exception {
        File sous = new File(a, "sous");
        sous.mkdir();
        assertTrue(refuse(() -> Operations.copier(a, sous)).contains("lui-même"));
        assertTrue(refuse(() -> Operations.deplacer(a, sous)).contains("lui-même"));
        assertTrue(refuse(() -> Operations.copier(a, a)).contains("lui-même"));
        assertEquals("rien n'a été créé", 0, sous.list().length);
    }

    @Test
    public void deplacerRetireLOriginal() throws Exception {
        File source = OutilsDossier.ecrire(a, "note.txt", "bonjour");
        File deplace = Operations.deplacer(source, b);
        assertEquals("bonjour", OutilsDossier.lire(deplace));
        assertFalse(source.exists());
    }

    @Test
    public void deplacerUnDossierEmporteSonContenu() throws Exception {
        OutilsDossier.ecrire(a, "sous/fichier.txt", "dedans");
        File deplace = Operations.deplacer(new File(a, "sous"), b);
        assertEquals("dedans", OutilsDossier.lire(new File(deplace, "fichier.txt")));
        assertFalse(new File(a, "sous").exists());
    }

    @Test
    public void deplacerVersUnNomDejaPrisNEcraseRien() throws Exception {
        File source = OutilsDossier.ecrire(a, "note.txt", "nouveau");
        OutilsDossier.ecrire(b, "note.txt", "ancien");
        File deplace = Operations.deplacer(source, b);
        assertEquals("note (2).txt", deplace.getName());
        assertEquals("ancien", OutilsDossier.lire(new File(b, "note.txt")));
    }

    @Test
    public void deplacerLaOuLeFichierEstDejaNeChangeRien() throws Exception {
        File source = OutilsDossier.ecrire(a, "note.txt", "x");
        assertEquals(source, Operations.deplacer(source, a));
        assertEquals(1, a.list().length);
    }

    @Test
    public void supprimerUnDossierRetireToutSonContenuEtRienDAutre() throws Exception {
        OutilsDossier.ecrire(a, "sous/fichier.txt", "x");
        OutilsDossier.ecrire(b, "a-garder.txt", "x");
        Operations.supprimer(a);
        assertFalse(a.exists());
        assertTrue(new File(b, "a-garder.txt").exists());
    }

    @Test
    public void compterDonneLeNombreDElementsContenus() throws Exception {
        OutilsDossier.ecrire(a, "sous/fichier.txt", "x");
        OutilsDossier.ecrire(a, "haut.txt", "x");
        assertEquals(3, Operations.compter(a));
        assertEquals(0, Operations.compter(b));
    }

    @Test
    public void lesNomsSansExtensionEtLesFichiersCachesSontNumerotesProprement() throws Exception {
        OutilsDossier.ecrire(b, "LISEZMOI", "x");
        OutilsDossier.ecrire(b, ".config", "x");
        assertEquals("LISEZMOI (2)", Operations.copier(OutilsDossier.ecrire(a, "LISEZMOI", "y"), b).getName());
        assertEquals(".config (2)", Operations.copier(OutilsDossier.ecrire(a, ".config", "y"), b).getName());
    }
}
