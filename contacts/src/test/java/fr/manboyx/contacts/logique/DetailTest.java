package fr.manboyx.contacts.logique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DetailTest {
    private static Detail detail(String nom, String[] telephones, String[] emails) {
        return new Detail(0, nom, false, Arrays.asList(telephones), Arrays.asList(emails));
    }

    @Test
    public void lesLignesVidesEtLesEspacesAutourSontRetires() {
        Detail d = detail("  Alice  ", new String[] {" 06 12 34 56 78 ", "", "  "}, new String[] {"", " a@b.fr "}).nettoye();
        assertEquals("Alice", d.nom);
        assertEquals(Collections.singletonList("06 12 34 56 78"), d.telephones);
        assertEquals(Collections.singletonList("a@b.fr"), d.emails);
    }

    @Test
    public void leMemeNumeroEcritDeuxFoisNEstGardeQuUneFois() {
        Detail d = detail("Alice", new String[] {"06 12 34 56 78", "+33612345678", "0700000000"}, new String[] {}).nettoye();
        assertEquals(Arrays.asList("06 12 34 56 78", "0700000000"), d.telephones);
    }

    @Test
    public void laMemeAdresseEcriteDeuxFoisNEstGardeeQuUneFois() {
        Detail d = detail("Alice", new String[] {}, new String[] {"a@b.fr", "A@B.FR"}).nettoye();
        assertEquals(Collections.singletonList("a@b.fr"), d.emails);
    }

    @Test
    public void sansNomLePremierNumeroSertDeNom() {
        assertEquals("0612345678", detail(" ", new String[] {"0612345678"}, new String[] {}).nettoye().nom);
    }

    @Test
    public void ilFautUnNomOuUnNumeroPourEnregistrer() {
        assertTrue(detail("Alice", new String[] {}, new String[] {}).enregistrable());
        assertTrue(detail("", new String[] {"0612345678"}, new String[] {}).enregistrable());
        assertFalse(detail(" ", new String[] {" "}, new String[] {"a@b.fr"}).enregistrable());
    }

    @Test
    public void leNettoyageGardeLIdentifiantEtLeFavori() {
        Detail d = new Detail(42, "Alice", true, Collections.emptyList(), Collections.emptyList()).nettoye();
        assertEquals(42, d.id);
        assertTrue(d.favori);
    }
}
