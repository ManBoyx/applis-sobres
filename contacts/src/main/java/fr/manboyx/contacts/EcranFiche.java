package fr.manboyx.contacts;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;
import fr.manboyx.contacts.logique.Detail;

/** La fiche d'un contact : ses numéros, ses adresses, et de quoi l'appeler ou lui écrire. */
final class EcranFiche extends Ecran {
    private final long id;
    private FrameLayout cadre;
    private Detail detail;

    EcranFiche(long id) {
        this.id = id;
    }

    @Override
    protected View creer() {
        cadre = new FrameLayout(activite);
        return cadre;
    }

    private Depot depot() {
        return ((ContactsActivity) activite).depot();
    }

    @Override
    public void afficher() {
        Fond.lancer(() -> depot().lire(id), lu -> {
            if (lu == null) {
                // Le contact a été supprimé entre-temps.
                activite.fermer();
                return;
            }
            detail = lu;
            remplir();
        });
    }

    private void remplir() {
        LinearLayout colonne = Ui.colonne(activite);
        TextView etoile = Ui.touche(activite, detail.favori ? "★" : "☆",
                detail.favori ? "Retirer des favoris" : "Ajouter aux favoris");
        etoile.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.accent));
        etoile.setOnClickListener(v -> {
            boolean favori = !detail.favori;
            Fond.lancer(() -> {
                depot().marquerFavori(id, favori);
                return depot().lire(id);
            }, lu -> {
                if (lu != null) {
                    detail = lu;
                    remplir();
                }
            });
        });
        colonne.addView(Barre.creer(activite, detail.nom, true, etoile));

        LinearLayout corps = Ui.colonne(activite);
        Ui.marges(corps, activite, 16, 8, 16, 24);
        if (!detail.telephones.isEmpty()) {
            corps.addView(titre("Téléphone"));
            for (String numero : detail.telephones) {
                corps.addView(ligne(numero,
                        "Appeler", new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", numero, null)),
                        "Message", new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", numero, null))));
            }
        }
        if (!detail.emails.isEmpty()) {
            corps.addView(titre("E-mail"));
            for (String adresse : detail.emails) {
                corps.addView(ligne(adresse,
                        "Écrire", new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", adresse, null)), null, null));
            }
        }
        if (detail.telephones.isEmpty() && detail.emails.isEmpty()) {
            corps.addView(Ui.vide(activite, "Ni numéro ni adresse pour ce contact."));
        }

        LinearLayout actions = Ui.rangee(activite);
        actions.setPadding(0, Ui.dp(activite, 24), 0, 0);
        Button modifier = Ui.bouton(activite, "Modifier", false);
        modifier.setOnClickListener(v -> activite.ouvrir(new EcranEdition(detail)));
        actions.addView(modifier, Ui.place(Ui.JUSTE, Ui.JUSTE));
        Button supprimer = Ui.bouton(activite, "Supprimer", false);
        supprimer.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.danger));
        supprimer.setOnClickListener(v -> Dialogues.confirmer(activite, "Supprimer ce contact ?",
                detail.nom + " sera retiré du téléphone.", "Supprimer",
                () -> Fond.lancer(() -> {
                    depot().supprimer(id);
                    return true;
                }, fait -> activite.fermer())));
        LinearLayout.LayoutParams aDroite = Ui.place(Ui.JUSTE, Ui.JUSTE);
        aDroite.leftMargin = Ui.dp(activite, 12);
        actions.addView(supprimer, aDroite);
        corps.addView(actions);

        ScrollView defilement = new ScrollView(activite);
        defilement.addView(corps);
        colonne.addView(defilement, Ui.reste(false));
        cadre.removeAllViews();
        cadre.addView(colonne, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
    }

    private TextView titre(String texte) {
        TextView titre = Ui.gras(Ui.texte(activite, texte, 14, fr.manboyx.commun.R.color.accent));
        titre.setPadding(0, Ui.dp(activite, 16), 0, Ui.dp(activite, 4));
        return titre;
    }

    /** Une valeur et, à droite, un ou deux boutons qui ouvrent l'appli du téléphone prévue pour cela. */
    private View ligne(String valeur, String action1, Intent intention1, String action2, Intent intention2) {
        LinearLayout colonne = Ui.colonne(activite);
        colonne.setPadding(0, Ui.dp(activite, 6), 0, Ui.dp(activite, 6));
        TextView texte = Ui.texte(activite, valeur, 18, fr.manboyx.commun.R.color.texte);
        texte.setTextIsSelectable(true);
        colonne.addView(texte);
        LinearLayout boutons = Ui.rangee(activite);
        boutons.setPadding(0, Ui.dp(activite, 8), 0, 0);
        boutons.addView(bouton(action1, intention1, true), Ui.place(Ui.JUSTE, Ui.JUSTE));
        if (action2 != null) {
            LinearLayout.LayoutParams aDroite = Ui.place(Ui.JUSTE, Ui.JUSTE);
            aDroite.leftMargin = Ui.dp(activite, 12);
            boutons.addView(bouton(action2, intention2, false), aDroite);
        }
        colonne.addView(boutons);
        return colonne;
    }

    private Button bouton(String libelle, Intent intention, boolean principal) {
        Button bouton = Ui.bouton(activite, libelle, principal);
        bouton.setOnClickListener(v -> {
            try {
                activite.startActivity(intention);
            } catch (ActivityNotFoundException aucune) {
                Dialogues.dire(activite, "Aucune appli du téléphone ne sait faire cela.");
            }
        });
        return bouton;
    }
}
