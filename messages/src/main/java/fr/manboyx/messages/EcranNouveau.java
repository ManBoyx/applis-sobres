package fr.manboyx.messages;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.commun.Affichage;
import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Correspondant;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Numeros;
import fr.manboyx.commun.Recherche;
import fr.manboyx.commun.Ui;

import java.util.ArrayList;
import java.util.List;

/** Choisir à qui écrire : un contact, ou un numéro tapé. */
final class EcranNouveau extends Ecran {
    private List<Correspondant> carnet = new ArrayList<>();
    private List<Correspondant> trouves = new ArrayList<>();
    private final Lignes adaptateur = new Lignes();
    private EditText champ;
    private Button ecrire;

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        colonne.addView(Barre.creer(activite, "Nouveau message", true));
        champ = Ui.champ(activite, "Nom ou numéro");
        champ.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int debut, int avant, int apres) {}

            @Override
            public void onTextChanged(CharSequence s, int debut, int avant, int nombre) {}

            @Override
            public void afterTextChanged(Editable s) {
                filtrer();
            }
        });
        LinearLayout.LayoutParams place = Ui.place(Ui.PLEIN, Ui.JUSTE);
        place.setMargins(Ui.dp(activite, 16), 0, Ui.dp(activite, 16), Ui.dp(activite, 8));
        colonne.addView(champ, place);

        ecrire = Ui.bouton(activite, "Écrire à ce numéro", true);
        ecrire.setOnClickListener(v -> ouvrir(champ.getText().toString().trim(), ""));
        LinearLayout.LayoutParams dessous = Ui.place(Ui.PLEIN, Ui.JUSTE);
        dessous.setMargins(Ui.dp(activite, 16), 0, Ui.dp(activite, 16), Ui.dp(activite, 8));
        colonne.addView(ecrire, dessous);

        ListView liste = new ListView(activite);
        liste.setDivider(null);
        liste.setAdapter(adaptateur);
        liste.setOnItemClickListener((parent, vue, rang, id) -> ouvrir(trouves.get(rang).numero, trouves.get(rang).nom));
        colonne.addView(liste, Ui.reste(false));
        filtrer();
        return colonne;
    }

    @Override
    public void afficher() {
        Depot depot = ((MessagesActivity) activite).depot();
        Fond.lancer(depot::carnet, lu -> {
            carnet = lu;
            filtrer();
        });
    }

    private void filtrer() {
        String saisie = champ.getText().toString();
        trouves = Recherche.filtrer(carnet, saisie, 0);
        adaptateur.notifyDataSetChanged();
        // Le bouton n'apparaît que si ce qui est tapé ressemble à un numéro.
        boolean numero = Numeros.chiffres(saisie).replace("+", "").length() >= 3
                && saisie.replaceAll("[0-9 .+-]", "").isEmpty();
        ecrire.setVisibility(numero ? View.VISIBLE : View.GONE);
    }

    private void ouvrir(String numero, String nom) {
        activite.remplacer(new EcranFil(0, numero, nom, ""));
    }

    private final class Lignes extends BaseAdapter {
        @Override
        public int getCount() {
            return trouves.size();
        }

        @Override
        public Object getItem(int rang) {
            return trouves.get(rang);
        }

        @Override
        public long getItemId(int rang) {
            return rang;
        }

        @Override
        public View getView(int rang, View recyclee, ViewGroup parent) {
            LinearLayout colonne = (LinearLayout) recyclee;
            if (colonne == null) {
                colonne = Ui.colonne(activite);
                colonne.setMinimumHeight(Ui.dp(activite, 60));
                colonne.setGravity(android.view.Gravity.CENTER_VERTICAL);
                Ui.marges(colonne, activite, 16, 8, 16, 8);
                colonne.addView(Ui.uneLigne(Ui.texte(activite, "")));
                colonne.addView(Ui.uneLigne(Ui.discret(activite, "")));
            }
            Correspondant c = trouves.get(rang);
            ((TextView) colonne.getChildAt(0)).setText(c.nom.isEmpty() ? Affichage.numero(c.numero) : c.nom);
            ((TextView) colonne.getChildAt(1)).setText(Affichage.numero(c.numero));
            return colonne;
        }
    }
}
