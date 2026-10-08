package fr.manboyx.notes.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CarnetTest {
    private File dossier;
    private Carnet carnet;

    @Before
    public void preparer() throws Exception {
        dossier = Files.createTempDirectory("notes-essai").toFile();
        carnet = new Carnet(new File(dossier, "notes"));
    }

    @After
    public void nettoyer() {
        File notes = new File(dossier, "notes");
        File[] fichiers = notes.listFiles();
        if (fichiers != null) {
            for (File f : fichiers) {
                f.delete();
            }
        }
        notes.delete();
        dossier.delete();
    }

    private List<String> titres(String recherche) {
        List<String> titres = new ArrayList<>();
        for (Note note : carnet.lister(recherche)) {
            titres.add(note.titre);
        }
        return titres;
    }

    @Test
    public void unCarnetNeufEstVide() {
        assertEquals(0, carnet.lister("").size());
    }

    @Test
    public void uneNoteEnregistreeSeRelitALIdentique() throws Exception {
        long id = carnet.enregistrer(0, "Courses\n- pain\n- œufs 🥚\n");
        assertNotEquals(0, id);
        assertEquals("Courses\n- pain\n- œufs 🥚\n", carnet.lire(id));
        assertEquals("elle survit à la fermeture de l'appli", "Courses\n- pain\n- œufs 🥚\n", new Carnet(new File(dossier, "notes")).lire(id));
    }

    @Test
    public void leTitreEstLaPremiereLigneNonVideEtLExtraitLaSuite() throws Exception {
        carnet.enregistrer(0, "\n  \nCourses  \n- pain\n\n- œufs");
        Note note = carnet.lister("").get(0);
        assertEquals("Courses", note.titre);
        assertEquals("- pain - œufs", note.extrait);
    }

    @Test
    public void modifierUneNoteNeEnCreePasUneDeuxieme() throws Exception {
        long id = carnet.enregistrer(0, "Première version");
        assertEquals(id, carnet.enregistrer(id, "Deuxième version"));
        assertEquals(Arrays.asList("Deuxième version"), titres(""));
    }

    @Test
    public void uneNoteVideNEstPasCreee() throws Exception {
        assertEquals(0, carnet.enregistrer(0, "   \n\n "));
        assertEquals(0, carnet.lister("").size());
    }

    @Test
    public void viderUneNoteLaSupprime() throws Exception {
        long id = carnet.enregistrer(0, "À effacer");
        assertEquals(0, carnet.enregistrer(id, ""));
        assertEquals(0, carnet.lister("").size());
    }

    @Test
    public void supprimerRetireLaNoteEtElleSeule() throws Exception {
        long a = carnet.enregistrer(0, "A");
        carnet.enregistrer(0, "B");
        carnet.supprimer(a);
        assertEquals(Arrays.asList("B"), titres(""));
        assertEquals("", carnet.lire(a));
    }

    @Test
    public void deuxNotesCreeesCoupSurCoupOntDesIdentifiantsDifferents() throws Exception {
        long a = carnet.enregistrer(0, "A");
        long b = carnet.enregistrer(0, "B");
        assertNotEquals(a, b);
        assertEquals(2, carnet.lister("").size());
    }

    @Test
    public void laDerniereNoteModifieeEstEnTete() throws Exception {
        long a = carnet.enregistrer(0, "A");
        long b = carnet.enregistrer(0, "B");
        // On fixe les dates : deux écritures dans la même milliseconde ne se départageraient pas.
        new File(new File(dossier, "notes"), a + ".txt").setLastModified(2_000_000);
        new File(new File(dossier, "notes"), b + ".txt").setLastModified(1_000_000);
        assertEquals(Arrays.asList("A", "B"), titres(""));
    }

    @Test
    public void laRechercheLitToutLeTexteSansAccentsNiMajuscules() throws Exception {
        carnet.enregistrer(0, "Courses\n- pain\n- Œufs frais");
        carnet.enregistrer(0, "Idées cadeaux\nun vélo");
        assertEquals(Arrays.asList("Idées cadeaux"), titres("VELO"));
        assertEquals(Arrays.asList("Courses"), titres("pain"));
        assertEquals(0, titres("introuvable").size());
        assertEquals(2, titres("  ").size());
    }

    @Test
    public void lesFichiersEtrangersDuDossierSontIgnores() throws Exception {
        carnet.enregistrer(0, "Vraie note");
        Files.write(new File(new File(dossier, "notes"), "autre.bin").toPath(), new byte[] {1, 2, 3});
        Files.write(new File(new File(dossier, "notes"), "12.txt.tmp").toPath(), "reste d'une écriture".getBytes());
        assertEquals(Arrays.asList("Vraie note"), titres(""));
    }

    @Test
    public void unTitreTresLongEstCoupe() throws Exception {
        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            long_.append("abcdefghij");
        }
        carnet.enregistrer(0, long_.toString());
        assertTrue(carnet.lister("").get(0).titre.length() <= 80);
    }
}
