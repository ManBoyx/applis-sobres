package fr.manboyx.contacts;

import android.Manifest;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;
import fr.manboyx.contacts.logique.Detail;

import java.util.ArrayList;
import java.util.List;

/** Créer ou modifier un contact. */
final class EcranEdition extends Ecran {
    private static final int DEMANDE = 2;

    /** Le contact de départ ; `null` ou `id` à 0 pour un nouveau. */
    private final Detail depart;
    private EditText nom;
    private LinearLayout telephones;
    private LinearLayout emails;
    private TextView erreur;

    EcranEdition(Detail depart) {
        this.depart = depart;
    }

    @Override
    protected View creer() {
        boolean nouveau = depart == null || depart.id == 0;
        LinearLayout colonne = Ui.colonne(activite);
        colonne.addView(Barre.creer(activite, nouveau ? "Nouveau contact" : "Modifier", true));

        LinearLayout corps = Ui.colonne(activite);
        Ui.marges(corps, activite, 16, 8, 16, 24);
        nom = Ui.champ(activite, "Nom");
        nom.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nom.setText(depart == null ? "" : depart.nom);
        corps.addView(nom);

        corps.addView(titre("Téléphone"));
        telephones = Ui.colonne(activite);
        corps.addView(telephones);
        if (depart != null) {
            for (String numero : depart.telephones) {
                ajouter(telephones, "Numéro", InputType.TYPE_CLASS_PHONE, numero);
            }
        }
        if (telephones.getChildCount() == 0) {
            ajouter(telephones, "Numéro", InputType.TYPE_CLASS_PHONE, "");
        }
        corps.addView(plus("Ajouter un numéro", () -> ajouter(telephones, "Numéro", InputType.TYPE_CLASS_PHONE, "")));

        corps.addView(titre("E-mail"));
        emails = Ui.colonne(activite);
        corps.addView(emails);
        int adresse = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
        if (depart != null) {
            for (String email : depart.emails) {
                ajouter(emails, "Adresse", adresse, email);
            }
        }
        if (emails.getChildCount() == 0) {
            ajouter(emails, "Adresse", adresse, "");
        }
        corps.addView(plus("Ajouter une adresse", () -> ajouter(emails, "Adresse", adresse, "")));

        erreur = Ui.texte(activite, "", 14, fr.manboyx.commun.R.color.danger);
        erreur.setPadding(0, Ui.dp(activite, 16), 0, 0);
        erreur.setVisibility(View.GONE);
        corps.addView(erreur);

        Button enregistrer = Ui.bouton(activite, "Enregistrer", true);
        enregistrer.setOnClickListener(v -> enregistrer());
        LinearLayout.LayoutParams place = Ui.place(Ui.PLEIN, Ui.JUSTE);
        place.topMargin = Ui.dp(activite, 24);
        corps.addView(enregistrer, place);

        ScrollView defilement = new ScrollView(activite);
        defilement.addView(corps);
        colonne.addView(defilement, Ui.reste(false));
        return colonne;
    }

    private TextView titre(String texte) {
        TextView titre = Ui.gras(Ui.texte(activite, texte, 14, fr.manboyx.commun.R.color.accent));
        titre.setPadding(0, Ui.dp(activite, 20), 0, Ui.dp(activite, 6));
        return titre;
    }

    private void ajouter(LinearLayout groupe, String indice, int saisie, String valeur) {
        EditText champ = Ui.champ(activite, indice);
        champ.setInputType(saisie);
        champ.setText(valeur);
        LinearLayout.LayoutParams place = Ui.place(Ui.PLEIN, Ui.JUSTE);
        place.bottomMargin = Ui.dp(activite, 8);
        groupe.addView(champ, place);
    }

    private Button plus(String libelle, Runnable action) {
        Button bouton = Ui.bouton(activite, libelle, false);
        bouton.setOnClickListener(v -> action.run());
        bouton.setLayoutParams(Ui.place(Ui.JUSTE, Ui.JUSTE));
        return bouton;
    }

    private static List<String> valeurs(LinearLayout groupe) {
        List<String> valeurs = new ArrayList<>();
        for (int i = 0; i < groupe.getChildCount(); i++) {
            valeurs.add(((EditText) groupe.getChildAt(i)).getText().toString());
        }
        return valeurs;
    }

    private void enregistrer() {
        Detail saisi = new Detail(depart == null ? 0 : depart.id, nom.getText().toString(),
                depart != null && depart.favori, valeurs(telephones), valeurs(emails));
        if (!saisi.enregistrable()) {
            erreur.setText("Indiquez un nom ou un numéro.");
            erreur.setVisibility(View.VISIBLE);
            return;
        }
        if (!Autorisations.accordees(activite, Manifest.permission.WRITE_CONTACTS)) {
            Autorisations.demander(activite, DEMANDE, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS);
            return;
        }
        erreur.setVisibility(View.GONE);
        Depot depot = ((ContactsActivity) activite).depot();
        Fond.lancer(() -> {
            try {
                depot.enregistrer(saisi.nettoye());
                return true;
            } catch (RuntimeException echec) {
                return false;
            }
        }, reussi -> {
            if (reussi) {
                activite.fermer();
            } else {
                erreur.setText("Le contact n'a pas pu être enregistré.");
                erreur.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        if (accordees) {
            enregistrer();
        } else {
            erreur.setText("Sans l'autorisation d'accéder aux contacts, rien ne peut être enregistré.");
            erreur.setVisibility(View.VISIBLE);
        }
    }
}
