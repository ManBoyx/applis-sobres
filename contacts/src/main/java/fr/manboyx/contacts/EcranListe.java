package fr.manboyx.contacts;

import android.Manifest;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Textes;
import fr.manboyx.commun.Ui;
import fr.manboyx.contacts.logique.Classement;
import fr.manboyx.contacts.logique.Fiche;

import java.util.ArrayList;
import java.util.List;

/** La liste des contacts, avec sa recherche. */
final class EcranListe extends Ecran {
    private static final int DEMANDE = 1;

    private final List<Fiche> fiches = new ArrayList<>();
    private List<Classement.Ligne> lignes = new ArrayList<>();
    private final Lignes adaptateur = new Lignes();
    private EditText recherche;
    private FrameLayout contenu;
    private ListView liste;
    private TextView vide;
    private View explication;

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        TextView nouveau = Ui.touche(activite, "+", "Nouveau contact");
        nouveau.setOnClickListener(v -> activite.ouvrir(new EcranEdition(null)));
        colonne.addView(Barre.creer(activite, "Contacts", false, nouveau));

        recherche = Ui.champ(activite, "Rechercher");
        recherche.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int debut, int avant, int apres) {}

            @Override
            public void onTextChanged(CharSequence s, int debut, int avant, int nombre) {}

            @Override
            public void afterTextChanged(Editable s) {
                ranger();
            }
        });
        LinearLayout.LayoutParams place = Ui.place(Ui.PLEIN, Ui.JUSTE);
        place.setMargins(Ui.dp(activite, 16), 0, Ui.dp(activite, 16), Ui.dp(activite, 8));
        colonne.addView(recherche, place);

        contenu = new FrameLayout(activite);
        liste = new ListView(activite);
        liste.setDivider(null);
        liste.setAdapter(adaptateur);
        liste.setOnItemClickListener((parent, vue, rang, id) -> {
            Fiche fiche = lignes.get(rang).fiche;
            if (fiche != null) {
                activite.ouvrir(new EcranFiche(fiche.id));
            }
        });
        contenu.addView(liste, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        vide = Ui.vide(activite, "");
        contenu.addView(vide, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
        explication = Autorisations.explication(activite,
                "Pour afficher vos contacts, l'appli a besoin d'y accéder. Ils restent sur le téléphone.",
                DEMANDE, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS);
        contenu.addView(explication, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        colonne.addView(contenu, Ui.reste(false));
        return colonne;
    }

    @Override
    public void afficher() {
        boolean autorise = Autorisations.accordees(activite, Manifest.permission.READ_CONTACTS);
        explication.setVisibility(autorise ? View.GONE : View.VISIBLE);
        recherche.setVisibility(autorise ? View.VISIBLE : View.GONE);
        liste.setVisibility(autorise ? View.VISIBLE : View.GONE);
        vide.setVisibility(View.GONE);
        if (!autorise) {
            return;
        }
        Depot depot = ((ContactsActivity) activite).depot();
        Fond.lancer(depot::lister, lus -> {
            fiches.clear();
            fiches.addAll(lus);
            ranger();
        });
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        afficher();
    }

    @Override
    public boolean retour() {
        // Le bouton retour efface d'abord la recherche.
        if (recherche.length() > 0) {
            recherche.setText("");
            return true;
        }
        return false;
    }

    private void ranger() {
        String cherche = recherche.getText().toString();
        lignes = Classement.lignes(fiches, cherche);
        adaptateur.notifyDataSetChanged();
        boolean rien = lignes.isEmpty() && liste.getVisibility() == View.VISIBLE;
        vide.setVisibility(rien ? View.VISIBLE : View.GONE);
        vide.setText(Textes.cle(cherche).isEmpty()
                ? "Aucun contact. Appuyez sur + pour en ajouter un."
                : "Aucun contact ne correspond.");
    }

    private final class Lignes extends BaseAdapter {
        @Override
        public int getCount() {
            return lignes.size();
        }

        @Override
        public Object getItem(int rang) {
            return lignes.get(rang);
        }

        @Override
        public long getItemId(int rang) {
            Fiche fiche = lignes.get(rang).fiche;
            return fiche == null ? -rang - 1 : fiche.id;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int rang) {
            return lignes.get(rang).titre != null ? 0 : 1;
        }

        @Override
        public boolean isEnabled(int rang) {
            return lignes.get(rang).fiche != null;
        }

        @Override
        public View getView(int rang, View recyclee, ViewGroup parent) {
            Classement.Ligne ligne = lignes.get(rang);
            if (ligne.titre != null) {
                TextView titre = recyclee != null ? (TextView) recyclee : Ui.gras(Ui.texte(activite, "", 14, fr.manboyx.commun.R.color.accent));
                Ui.marges(titre, activite, 16, 14, 16, 4);
                titre.setText(ligne.titre);
                return titre;
            }
            LinearLayout rangee = (LinearLayout) recyclee;
            if (rangee == null) {
                rangee = Ui.rangee(activite);
                rangee.setMinimumHeight(Ui.dp(activite, 56));
                Ui.marges(rangee, activite, 16, 6, 16, 6);
                TextView pastille = Ui.gras(Ui.texte(activite, "", 16, fr.manboyx.commun.R.color.texte));
                pastille.setGravity(Gravity.CENTER);
                pastille.setBackgroundResource(fr.manboyx.commun.R.drawable.fond_pastille);
                rangee.addView(pastille, Ui.place(Ui.dp(activite, 40), Ui.dp(activite, 40)));
                TextView nom = Ui.uneLigne(Ui.texte(activite, ""));
                nom.setPadding(Ui.dp(activite, 14), 0, Ui.dp(activite, 8), 0);
                rangee.addView(nom, Ui.reste(true));
                rangee.addView(Ui.texte(activite, "★", 16, fr.manboyx.commun.R.color.accent));
            }
            ((TextView) rangee.getChildAt(0)).setText(Textes.initiale(ligne.fiche.nom));
            ((TextView) rangee.getChildAt(1)).setText(ligne.fiche.nom);
            rangee.getChildAt(2).setVisibility(ligne.fiche.favori ? View.VISIBLE : View.GONE);
            return rangee;
        }
    }
}
