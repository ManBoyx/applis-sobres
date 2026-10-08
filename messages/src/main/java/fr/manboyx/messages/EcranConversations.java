package fr.manboyx.messages;

import android.Manifest;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.commun.Affichage;
import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Dates;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Textes;
import fr.manboyx.commun.Ui;
import fr.manboyx.messages.logique.Conversations;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** La liste des conversations. */
final class EcranConversations extends Ecran {
    private static final int DEMANDE = 1;
    static final Uri SMS = Uri.parse("content://sms");

    /** Une conversation et le nom à afficher pour elle. */
    private static final class Vue {
        final Conversations.Resume resume;
        final String titre;

        Vue(Conversations.Resume resume, String titre) {
            this.resume = resume;
            this.titre = titre;
        }
    }

    private List<Vue> vues = new ArrayList<>();
    private final Lignes adaptateur = new Lignes();
    private ListView liste;
    private TextView vide;
    private View explication;
    private boolean enLecture;
    private final ContentObserver guetteur = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean soiMeme) {
            charger();
        }
    };
    private boolean guette;

    private Depot depot() {
        return ((MessagesActivity) activite).depot();
    }

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        TextView nouveau = Ui.touche(activite, "+", "Nouveau message");
        nouveau.setOnClickListener(v -> activite.ouvrir(new EcranNouveau()));
        colonne.addView(Barre.creer(activite, "Messages", false, nouveau));

        FrameLayout cadre = new FrameLayout(activite);
        liste = new ListView(activite);
        liste.setDivider(null);
        liste.setAdapter(adaptateur);
        liste.setOnItemClickListener((parent, vue, rang, id) -> {
            Vue choisie = vues.get(rang);
            activite.ouvrir(new EcranFil(choisie.resume.fil, choisie.resume.adresse, choisie.titre, ""));
        });
        cadre.addView(liste, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        vide = Ui.vide(activite, "Aucun message. Appuyez sur + pour en écrire un.");
        vide.setVisibility(View.GONE);
        cadre.addView(vide, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
        explication = Autorisations.explication(activite,
                "Pour afficher vos SMS, l'appli a besoin de les lire. Ils restent sur le téléphone.",
                DEMANDE, Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS);
        cadre.addView(explication, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        colonne.addView(cadre, Ui.reste(false));
        return colonne;
    }

    @Override
    public void afficher() {
        boolean autorise = Autorisations.accordees(activite, Manifest.permission.READ_SMS);
        explication.setVisibility(autorise ? View.GONE : View.VISIBLE);
        liste.setVisibility(autorise ? View.VISIBLE : View.GONE);
        if (!autorise) {
            vide.setVisibility(View.GONE);
            return;
        }
        // Tant que l'écran est visible, un SMS qui arrive met la liste à jour.
        if (!guette) {
            activite.getContentResolver().registerContentObserver(SMS, true, guetteur);
            guette = true;
        }
        charger();
    }

    @Override
    public void masquer() {
        if (guette) {
            activite.getContentResolver().unregisterContentObserver(guetteur);
            guette = false;
        }
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        afficher();
    }

    private void charger() {
        if (enLecture) {
            return;
        }
        enLecture = true;
        Depot depot = depot();
        Fond.lancer(() -> {
            List<Vue> lues = new ArrayList<>();
            for (Conversations.Resume resume : depot.conversations()) {
                String nom = depot.nom(resume.adresse);
                lues.add(new Vue(resume, nom.isEmpty() ? Affichage.numero(resume.adresse) : nom));
            }
            return lues;
        }, lues -> {
            enLecture = false;
            vues = lues;
            adaptateur.notifyDataSetChanged();
            vide.setVisibility(lues.isEmpty() && liste.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        });
    }

    private final class Lignes extends BaseAdapter {
        @Override
        public int getCount() {
            return vues.size();
        }

        @Override
        public Object getItem(int rang) {
            return vues.get(rang);
        }

        @Override
        public long getItemId(int rang) {
            return rang;
        }

        @Override
        public View getView(int rang, View recyclee, ViewGroup parent) {
            LinearLayout rangee = (LinearLayout) recyclee;
            if (rangee == null) {
                rangee = Ui.rangee(activite);
                rangee.setMinimumHeight(Ui.dp(activite, 72));
                Ui.marges(rangee, activite, 16, 8, 16, 8);
                TextView pastille = Ui.gras(Ui.texte(activite, "", 16, fr.manboyx.commun.R.color.texte));
                pastille.setGravity(Gravity.CENTER);
                pastille.setBackgroundResource(fr.manboyx.commun.R.drawable.fond_pastille);
                rangee.addView(pastille, Ui.place(Ui.dp(activite, 40), Ui.dp(activite, 40)));
                LinearLayout textes = Ui.colonne(activite);
                textes.setPadding(Ui.dp(activite, 14), 0, Ui.dp(activite, 8), 0);
                textes.addView(Ui.uneLigne(Ui.texte(activite, "")));
                textes.addView(Ui.uneLigne(Ui.discret(activite, "")));
                rangee.addView(textes, Ui.reste(true));
                LinearLayout droite = Ui.colonne(activite);
                droite.setGravity(Gravity.END);
                // Largeur explicite : sinon la date prend celle de la pastille et se retrouve coupée.
                droite.addView(Ui.discret(activite, ""), Ui.place(Ui.JUSTE, Ui.JUSTE));
                TextView nonLus = Ui.gras(Ui.texte(activite, "", 12, fr.manboyx.commun.R.color.sur_accent));
                nonLus.setGravity(Gravity.CENTER);
                nonLus.setBackgroundResource(R.drawable.fond_compteur);
                nonLus.setMinWidth(Ui.dp(activite, 22));
                nonLus.setMinimumWidth(Ui.dp(activite, 22));
                nonLus.setPadding(Ui.dp(activite, 6), Ui.dp(activite, 2), Ui.dp(activite, 6), Ui.dp(activite, 2));
                LinearLayout.LayoutParams dessous = Ui.place(Ui.JUSTE, Ui.JUSTE);
                dessous.topMargin = Ui.dp(activite, 4);
                droite.addView(nonLus, dessous);
                rangee.addView(droite);
            }
            Vue vue = vues.get(rang);
            Conversations.Resume r = vue.resume;
            ((TextView) rangee.getChildAt(0)).setText(Textes.initiale(vue.titre));
            LinearLayout textes = (LinearLayout) rangee.getChildAt(1);
            TextView titre = (TextView) textes.getChildAt(0);
            titre.setText(vue.titre);
            titre.setTypeface(r.nonLus > 0 ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
            ((TextView) textes.getChildAt(1)).setText(r.envoye ? "Vous : " + r.extrait : r.extrait);
            LinearLayout droite = (LinearLayout) rangee.getChildAt(2);
            ((TextView) droite.getChildAt(0)).setText(Dates.relative(r.quand, System.currentTimeMillis(), ZoneId.systemDefault()));
            TextView nonLus = (TextView) droite.getChildAt(1);
            nonLus.setText(String.valueOf(r.nonLus));
            nonLus.setContentDescription(r.nonLus + " non lus");
            nonLus.setVisibility(r.nonLus > 0 ? View.VISIBLE : View.GONE);
            return rangee;
        }
    }
}
