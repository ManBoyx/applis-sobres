package fr.manboyx.calendrier;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Ecran;

public class CalendrierActivity extends ActiviteSobre {
    private Depot depot;

    /** Les tests le remplacent par un agenda en mémoire. */
    public Depot depot() {
        if (depot == null) {
            depot = new DepotAndroid(getContentResolver());
        }
        return depot;
    }

    @Override
    protected Ecran accueil() {
        return new EcranMois();
    }
}
