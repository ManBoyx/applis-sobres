package fr.manboyx.commun;

import android.content.Intent;
import android.view.View;

/**
 * Un écran d'une appli. L'activité n'en montre qu'un à la fois et garde la pile
 * de ceux d'où l'on vient ; le bouton retour y revient.
 */
public abstract class Ecran {
    protected ActiviteSobre activite;
    private View vue;

    /** Construit la vue. Appelé une seule fois. */
    protected abstract View creer();

    /** L'écran devient visible : c'est ici qu'on lit les données et qu'on s'abonne. */
    public void afficher() {}

    /** L'écran n'est plus visible : c'est ici qu'on arrête tout ce qui tourne. */
    public void masquer() {}

    /** Bouton retour. Rendre vrai si l'écran s'en est chargé lui-même. */
    public boolean retour() {
        return false;
    }

    /** Réponse à une demande d'autorisation lancée depuis cet écran. */
    public void autorisations(int code, boolean accordees) {}

    /** Réponse d'un autre écran du téléphone lancé depuis celui-ci. */
    public void resultat(int code, int resultat, Intent donnees) {}

    final View vue(ActiviteSobre activite) {
        this.activite = activite;
        if (vue == null) {
            vue = creer();
        }
        return vue;
    }
}
