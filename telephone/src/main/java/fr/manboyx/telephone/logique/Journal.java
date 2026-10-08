package fr.manboyx.telephone.logique;

import fr.manboyx.commun.Numeros;

import java.util.ArrayList;
import java.util.List;

/** Met en forme le journal d'appels. */
public final class Journal {
    private Journal() {}

    /** Plusieurs appels de suite du même numéro et dans le même sens, réunis en une ligne. */
    public static final class Groupe {
        /** Le plus récent du groupe. */
        public final Appel dernier;
        public final int nombre;

        Groupe(Appel dernier, int nombre) {
            this.dernier = dernier;
            this.nombre = nombre;
        }
    }

    /** `appels` va du plus récent au plus ancien ; l'ordre est gardé. */
    public static List<Groupe> regrouper(List<Appel> appels) {
        List<Groupe> groupes = new ArrayList<>();
        Appel tete = null;
        int nombre = 0;
        for (Appel appel : appels) {
            boolean suite = tete != null
                    && tete.sens == appel.sens
                    && Numeros.memes(tete.numero, appel.numero);
            if (suite) {
                nombre++;
                continue;
            }
            if (tete != null) {
                groupes.add(new Groupe(tete, nombre));
            }
            tete = appel;
            nombre = 1;
        }
        if (tete != null) {
            groupes.add(new Groupe(tete, nombre));
        }
        return groupes;
    }
}
