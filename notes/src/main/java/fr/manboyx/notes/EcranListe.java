package fr.manboyx.notes;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Dates;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Ui;
import fr.manboyx.notes.logique.Note;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** La liste des notes, la plus récente en tête. */
final class EcranListe extends Ecran {
    private List<Note> notes = new ArrayList<>();
    private final Lignes adaptateur = new Lignes();
    private EditText recherche;
    private TextView vide;

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        TextView nouvelle = Ui.touche(activite, "+", "Nouvelle note");
        nouvelle.setOnClickListener(v -> activite.ouvrir(new EcranNote(0, "")));
        colonne.addView(Barre.creer(activite, "Notes", false, nouvelle));

        recherche = Ui.champ(activite, "Rechercher");
        recherche.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int debut, int avant, int apres) {}

            @Override
            public void onTextChanged(CharSequence s, int debut, int avant, int nombre) {}

            @Override
            public void afterTextChanged(Editable s) {
                lire();
            }
        });
        LinearLayout.LayoutParams place = Ui.place(Ui.PLEIN, Ui.JUSTE);
        place.setMargins(Ui.dp(activite, 16), 0, Ui.dp(activite, 16), Ui.dp(activite, 8));
        colonne.addView(recherche, place);

        FrameLayout cadre = new FrameLayout(activite);
        ListView liste = new ListView(activite);
        liste.setDivider(null);
        liste.setAdapter(adaptateur);
        liste.setOnItemClickListener((parent, vue, rang, id) -> activite.ouvrir(new EcranNote(notes.get(rang).id, "")));
        cadre.addView(liste, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        vide = Ui.vide(activite, "");
        cadre.addView(vide, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
        colonne.addView(cadre, Ui.reste(false));
        return colonne;
    }

    @Override
    public void afficher() {
        lire();
    }

    @Override
    public boolean retour() {
        if (recherche.length() > 0) {
            recherche.setText("");
            return true;
        }
        return false;
    }

    private void lire() {
        String cherche = recherche.getText().toString();
        notes = ((NotesActivity) activite).carnet().lister(cherche);
        adaptateur.notifyDataSetChanged();
        vide.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        vide.setText(cherche.trim().isEmpty() ? "Aucune note. Appuyez sur + pour en écrire une." : "Aucune note ne correspond.");
    }

    private final class Lignes extends BaseAdapter {
        @Override
        public int getCount() {
            return notes.size();
        }

        @Override
        public Object getItem(int rang) {
            return notes.get(rang);
        }

        @Override
        public long getItemId(int rang) {
            return notes.get(rang).id;
        }

        @Override
        public View getView(int rang, View recyclee, ViewGroup parent) {
            LinearLayout rangee = (LinearLayout) recyclee;
            if (rangee == null) {
                rangee = Ui.rangee(activite);
                rangee.setMinimumHeight(Ui.dp(activite, 68));
                Ui.marges(rangee, activite, 16, 10, 16, 10);
                LinearLayout textes = Ui.colonne(activite);
                textes.addView(Ui.uneLigne(Ui.gras(Ui.texte(activite, ""))));
                textes.addView(Ui.uneLigne(Ui.discret(activite, "")));
                rangee.addView(textes, Ui.reste(true));
                TextView date = Ui.discret(activite, "");
                date.setPadding(Ui.dp(activite, 12), 0, 0, 0);
                rangee.addView(date, Ui.place(Ui.JUSTE, Ui.JUSTE));
            }
            Note note = notes.get(rang);
            LinearLayout textes = (LinearLayout) rangee.getChildAt(0);
            ((TextView) textes.getChildAt(0)).setText(note.titre);
            TextView extrait = (TextView) textes.getChildAt(1);
            extrait.setText(note.extrait);
            extrait.setVisibility(note.extrait.isEmpty() ? View.GONE : View.VISIBLE);
            ((TextView) rangee.getChildAt(1)).setText(Dates.relative(note.modifiee, System.currentTimeMillis(), ZoneId.systemDefault()));
            return rangee;
        }
    }
}
