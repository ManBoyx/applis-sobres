package fr.manboyx.fichiers;

import android.Manifest;
import android.os.Build;
import android.os.Environment;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Ecran;
import fr.manboyx.fichiers.logique.Dossier;

import java.io.File;

public class FichiersActivity extends ActiviteSobre {
    /** Ce qui attend d'être collé : un fichier ou un dossier, à copier ou à déplacer. */
    static final class Presse {
        final File fichier;
        final boolean deplacer;

        Presse(File fichier, boolean deplacer) {
            this.fichier = fichier;
            this.deplacer = deplacer;
        }
    }

    Presse presse;
    Dossier.Tri tri = Dossier.Tri.NOM;
    boolean caches;

    /** Le dossier de départ : la mémoire du téléphone. */
    public File racine() {
        return Environment.getExternalStorageDirectory();
    }

    /** Vrai si l'appli a le droit de parcourir la mémoire du téléphone. */
    public boolean acces() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return Autorisations.accordees(this, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    protected Ecran accueil() {
        return new EcranDossier(racine());
    }
}
