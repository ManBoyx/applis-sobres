package fr.manboyx.commun;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * L'unique activité de chaque appli : elle montre un écran à la fois et tient
 * la pile des écrans ouverts. Rien ne tourne quand elle n'est pas à l'écran.
 */
public abstract class ActiviteSobre extends Activity {
    private final List<Ecran> pile = new ArrayList<>();
    private FrameLayout cadre;
    private boolean visible;

    /** Le premier écran de l'appli. */
    protected abstract Ecran accueil();

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        cadre = new FrameLayout(this);
        // Android dessine désormais sous les barres du haut et du bas : on s'en écarte.
        cadre.setOnApplyWindowInsetsListener((vue, marges) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = marges.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                vue.setPadding(i.left, i.top, i.right, i.bottom);
            } else {
                vue.setPadding(marges.getSystemWindowInsetLeft(), marges.getSystemWindowInsetTop(),
                        marges.getSystemWindowInsetRight(), marges.getSystemWindowInsetBottom());
            }
            return marges;
        });
        setContentView(cadre);
        ouvrir(accueil());
    }

    /** L'écran montré en ce moment. */
    public Ecran ecran() {
        return pile.isEmpty() ? null : pile.get(pile.size() - 1);
    }

    /** Montre un nouvel écran par-dessus l'actuel. */
    public void ouvrir(Ecran ecran) {
        Ecran ancien = ecran();
        if (ancien != null && visible) {
            ancien.masquer();
        }
        pile.add(ecran);
        montrer();
    }

    /** Met un écran à la place de l'actuel : le bouton retour ne reviendra pas sur celui qu'on quitte. */
    public void remplacer(Ecran ecran) {
        Ecran ancien = ecran();
        if (ancien != null && visible) {
            ancien.masquer();
        }
        if (!pile.isEmpty()) {
            pile.remove(pile.size() - 1);
        }
        pile.add(ecran);
        montrer();
    }

    /** Referme l'écran actuel et revient au précédent ; quitte l'appli s'il n'y en a plus. */
    public void fermer() {
        Ecran actuel = ecran();
        if (actuel != null && visible) {
            actuel.masquer();
        }
        if (pile.size() <= 1) {
            pile.clear();
            finish();
            return;
        }
        pile.remove(pile.size() - 1);
        montrer();
    }

    private void montrer() {
        Ecran actuel = ecran();
        View vue = actuel.vue(this);
        cadre.removeAllViews();
        cadre.addView(vue, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        if (visible) {
            actuel.afficher();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;
        if (ecran() != null) {
            ecran().afficher();
        }
    }

    @Override
    protected void onPause() {
        visible = false;
        if (ecran() != null) {
            ecran().masquer();
        }
        super.onPause();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        Ecran actuel = ecran();
        if (actuel != null && actuel.retour()) {
            return;
        }
        fermer();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] autorisations, int[] reponses) {
        super.onRequestPermissionsResult(code, autorisations, reponses);
        boolean accordees = reponses.length > 0;
        for (int reponse : reponses) {
            accordees &= reponse == PackageManager.PERMISSION_GRANTED;
        }
        if (ecran() != null) {
            ecran().autorisations(code, accordees);
        }
    }

    @Override
    protected void onActivityResult(int code, int resultat, Intent donnees) {
        super.onActivityResult(code, resultat, donnees);
        if (ecran() != null) {
            ecran().resultat(code, resultat, donnees);
        }
    }
}
