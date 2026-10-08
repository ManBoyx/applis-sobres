package fr.manboyx.contacts;

import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.view.View;

import fr.manboyx.commun.Fond;
import fr.manboyx.essais.Essai;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Écrit des images des écrans, pour contrôler la mise en page sans téléphone. Ne fait rien sans « captures.dir ». */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CapturesTest {
    private void ecrans(String suffixe) throws Exception {
        Fond.utiliser(Runnable::run);
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS);
        EcransTest.carnet = new CarnetMemoire();
        EcransTest.carnet.ajouter("Alice Martin", true, new String[] {"06 12 34 56 78", "01 23 45 67 89"}, new String[] {"alice@exemple.fr"});
        EcransTest.carnet.ajouter("Bruno Petit", false, new String[] {"07 00 00 00 01"}, new String[] {});
        EcransTest.carnet.ajouter("Chloé Bernard", true, new String[] {"06 99 88 77 66"}, new String[] {});
        EcransTest.carnet.ajouter("Élodie Durand", false, new String[] {}, new String[] {"elodie@exemple.fr"});
        EcransTest.carnet.ajouter("Médecin", false, new String[] {"01 44 55 66 77"}, new String[] {});
        EcransTest.carnet.ajouter("3615 Service", false, new String[] {"3615"}, new String[] {});

        EcransTest.ActiviteEssai a = Robolectric.buildActivity(EcransTest.ActiviteEssai.class).setup().get();
        View ecran = a.getWindow().getDecorView();
        Essai.attendre();
        Essai.capturer(ecran, "contacts-1-liste" + suffixe);
        Essai.appuyer(ecran, "Bruno Petit");
        Essai.capturer(ecran, "contacts-2-fiche" + suffixe);
        Essai.appuyer(ecran, "Modifier");
        Essai.capturer(ecran, "contacts-3-modifier" + suffixe);
    }

    @Test
    @Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
    public void clair() throws Exception {
        ecrans("-clair");
    }

    @Test
    @Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-night-xhdpi")
    public void sombre() throws Exception {
        ecrans("-sombre");
    }
}
