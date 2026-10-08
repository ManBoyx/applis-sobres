package fr.manboyx.contacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.database.MatrixCursor;
import android.provider.ContactsContract;

import fr.manboyx.contacts.logique.Detail;
import fr.manboyx.contacts.logique.Fiche;
import fr.manboyx.essais.FauxFournisseur;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Vérifie ce que l'appli demande au carnet d'adresses d'Android, sur un faux carnet qui note tout. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DepotAndroidTest {
    private FauxFournisseur base;
    private DepotAndroid depot;

    @Before
    public void preparer() {
        base = FauxFournisseur.installer(ContactsContract.AUTHORITY);
        depot = new DepotAndroid(RuntimeEnvironment.getApplication().getContentResolver());
    }

    private String journal() {
        return String.join("\n", base.journal);
    }

    @Test
    public void laListeReprendNomEtFavoriDeChaqueContact() {
        base.lecture = (uri, colonnes, condition, arguments) -> {
            MatrixCursor c = new MatrixCursor(colonnes);
            c.addRow(new Object[] {7L, "Alice", 1});
            c.addRow(new Object[] {8L, null, 0});
            return c;
        };
        List<Fiche> fiches = depot.lister();
        assertEquals(2, fiches.size());
        assertEquals(7, fiches.get(0).id);
        assertEquals("Alice", fiches.get(0).nom);
        assertTrue(fiches.get(0).favori);
        assertEquals("un contact sans nom ne fait pas planter la liste", "", fiches.get(1).nom);
    }

    @Test
    public void laFicheSepareNumerosEtAdresses() {
        base.lecture = (uri, colonnes, condition, arguments) -> {
            MatrixCursor c = new MatrixCursor(colonnes);
            if (uri.toString().contains("/contacts/7")) {
                c.addRow(new Object[] {"Alice", 0});
            } else if (uri.toString().endsWith("/data")) {
                c.addRow(new Object[] {ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, "0612345678"});
                c.addRow(new Object[] {ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, "a@b.fr"});
                c.addRow(new Object[] {ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, "Alice"});
                c.addRow(new Object[] {ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, "+33612345678"});
            }
            return c;
        };
        Detail d = depot.lire(7);
        assertEquals("Alice", d.nom);
        assertEquals("le même numéro venu de deux comptes n'apparaît qu'une fois", Collections.singletonList("0612345678"), d.telephones);
        assertEquals(Collections.singletonList("a@b.fr"), d.emails);
    }

    @Test
    public void unContactDisparuDonneNull() {
        assertNull(depot.lire(99));
    }

    @Test
    public void creerEcritLeNomLesNumerosEtLesAdressesSurLeTelephoneSeul() {
        depot.enregistrer(new Detail(0, "Alice", false, Arrays.asList("0612345678", "0123456789"), Arrays.asList("a@b.fr")));
        assertEquals(5, base.journal.size());
        assertTrue(base.journal.get(0), base.journal.get(0).startsWith("insert content://com.android.contacts/raw_contacts {account_name=null, account_type=null}"));
        String texte = journal();
        assertTrue(texte, texte.contains("data1=Alice"));
        assertTrue(texte, texte.contains("data1=0612345678"));
        assertTrue(texte, texte.contains("data1=0123456789"));
        assertTrue(texte, texte.contains("data1=a@b.fr"));
        // Chaque donnée est rattachée au contact qui vient d'être créé (identifiant 100 dans le faux carnet).
        for (String ligne : base.journal.subList(1, 5)) {
            assertTrue(ligne, ligne.contains("raw_contact_id=100"));
        }
    }

    @Test
    public void supprimerViseLeContactEtLuiSeul() {
        depot.supprimer(42);
        assertEquals(Collections.singletonList("delete content://com.android.contacts/contacts/42"), base.journal);
    }

    @Test
    public void marquerFavoriNeChangeQueLEtoile() {
        depot.marquerFavori(42, true);
        depot.marquerFavori(42, false);
        assertEquals(Arrays.asList(
                "update content://com.android.contacts/contacts/42 {starred=1}",
                "update content://com.android.contacts/contacts/42 {starred=0}"), base.journal);
    }

    @Test
    public void modifierRemplaceNumerosEtAdressesDeToutesLesFichesDuContact() {
        base.lecture = (uri, colonnes, condition, arguments) -> {
            MatrixCursor c = new MatrixCursor(colonnes);
            if (uri.toString().endsWith("/raw_contacts")) {
                c.addRow(new Object[] {11L});
                c.addRow(new Object[] {12L});
            } else if (uri.toString().endsWith("/data")) {
                c.addRow(new Object[] {500L});
            }
            return c;
        };
        depot.enregistrer(new Detail(7, "Alice B.", false, Collections.singletonList("0700000000"), Collections.emptyList()));
        assertEquals(3, base.journal.size());
        assertTrue(base.journal.get(0), base.journal.get(0).startsWith("update content://com.android.contacts/data {data1=Alice B.} si raw_contact_id IN (11,12)"));
        assertTrue(base.journal.get(1), base.journal.get(1).startsWith("delete content://com.android.contacts/data si raw_contact_id IN (11,12) AND mimetype IN (?,?)"));
        assertTrue(base.journal.get(2), base.journal.get(2).contains("data1=0700000000") && base.journal.get(2).contains("raw_contact_id=11"));
        assertFalse("le contact lui-même n'est jamais supprimé par une modification", journal().contains("delete content://com.android.contacts/contacts"));
    }
}
