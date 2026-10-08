package fr.manboyx.telephone;

import fr.manboyx.telephone.logique.Appel;
import fr.manboyx.commun.Correspondant;

import java.util.List;

/** Ce que l'appli lit sur le téléphone : le journal d'appels et les numéros du carnet. */
public interface Depot {
    /** Les derniers appels, du plus récent au plus ancien. */
    List<Appel> journal(int maximum);

    List<Correspondant> carnet();
}
