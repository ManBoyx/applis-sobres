package fr.manboyx.calculatrice;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Ecran;

public class CalculatriceActivity extends ActiviteSobre {
    @Override
    protected Ecran accueil() {
        return new EcranCalcul();
    }
}
