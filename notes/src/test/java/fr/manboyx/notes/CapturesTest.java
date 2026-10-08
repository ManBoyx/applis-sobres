package fr.manboyx.notes;

import android.view.View;

import fr.manboyx.essais.Essai;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Écrit des images des écrans. Ne fait rien sans « captures.dir ». */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CapturesTest {
    private void ecrans(String suffixe) throws Exception {
        NotesActivity a = Robolectric.buildActivity(NotesActivity.class).setup().get();
        View ecran = a.getWindow().getDecorView();
        for (String texte : new String[] {"Idées cadeaux\nun vélo pour Zoé\nun livre de cuisine", "Code du portail : 4821", "Courses\n- pain\n- œufs\n- café"}) {
            Essai.appuyer(ecran, "Nouvelle note");
            Essai.saisir(ecran, "Écrivez ici…", texte);
            a.onBackPressed();
            Essai.attendre();
        }
        Essai.capturer(ecran, "notes-1-liste" + suffixe);
        Essai.appuyer(ecran, "Courses");
        Essai.capturer(ecran, "notes-2-note" + suffixe);
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
