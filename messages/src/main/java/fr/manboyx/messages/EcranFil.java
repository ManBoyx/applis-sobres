package fr.manboyx.messages;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.commun.Affichage;
import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Dates;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;
import fr.manboyx.messages.logique.Conversations;
import fr.manboyx.messages.logique.Fil;
import fr.manboyx.messages.logique.Message;
import fr.manboyx.messages.logique.Sms;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Une conversation : les messages échangés et de quoi répondre. */
final class EcranFil extends Ecran {
    private static final int DEMANDE = 2;
    /** Les messages plus anciens restent dans le téléphone, mais ne sont pas chargés. */
    private static final int MESSAGES_AFFICHES = 500;

    private long fil;
    private final String adresse;
    private String titre;
    private final String brouillon;

    private List<Fil.Ligne> lignes = new ArrayList<>();
    private final Bulles adaptateur = new Bulles();
    private LinearLayout barre;
    private ListView liste;
    private EditText champ;
    private TextView compteur;
    private Button envoyer;
    private boolean enLecture;
    private boolean guette;
    private final ContentObserver guetteur = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean soiMeme) {
            charger();
        }
    };

    /** `fil` à 0 : conversation à retrouver par son numéro, ou pas encore commencée. */
    EcranFil(long fil, String adresse, String titre, String brouillon) {
        this.fil = fil;
        this.adresse = adresse;
        this.titre = titre == null || titre.isEmpty() ? Affichage.numero(adresse) : titre;
        this.brouillon = brouillon == null ? "" : brouillon;
    }

    private Depot depot() {
        return ((MessagesActivity) activite).depot();
    }

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        TextView appeler = Ui.touche(activite, "Appeler", "Appeler");
        appeler.setTextSize(15);
        appeler.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.accent));
        appeler.setOnClickListener(v -> {
            try {
                activite.startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", adresse, null)));
            } catch (ActivityNotFoundException aucune) {
                Dialogues.dire(activite, "Aucune appli du téléphone ne sait appeler.");
            }
        });
        barre = Barre.creer(activite, titre, true, appeler);
        colonne.addView(barre);

        liste = new ListView(activite);
        liste.setDivider(null);
        liste.setStackFromBottom(true);
        liste.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        liste.setAdapter(adaptateur);
        liste.setOnItemLongClickListener((parent, vue, rang, id) -> {
            Message message = lignes.get(rang).message;
            if (message == null) {
                return false;
            }
            activite.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Message", message.texte));
            Dialogues.dire(activite, "Message copié.");
            return true;
        });
        colonne.addView(liste, Ui.reste(false));
        colonne.addView(Ui.trait(activite));

        compteur = Ui.discret(activite, "");
        compteur.setGravity(Gravity.END);
        compteur.setPadding(Ui.dp(activite, 16), Ui.dp(activite, 4), Ui.dp(activite, 16), 0);
        compteur.setVisibility(View.GONE);
        colonne.addView(compteur, Ui.place(Ui.PLEIN, Ui.JUSTE));

        LinearLayout saisie = Ui.rangee(activite);
        saisie.setGravity(Gravity.BOTTOM);
        Ui.marges(saisie, activite, 12, 8, 12, 12);
        champ = Ui.champ(activite, "Message");
        champ.setSingleLine(false);
        champ.setMaxLines(4);
        champ.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        champ.setText(brouillon);
        champ.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int debut, int avant, int apres) {}

            @Override
            public void onTextChanged(CharSequence s, int debut, int avant, int nombre) {}

            @Override
            public void afterTextChanged(Editable s) {
                compter();
            }
        });
        saisie.addView(champ, Ui.reste(true));
        envoyer = Ui.bouton(activite, "Envoyer", true);
        envoyer.setOnClickListener(v -> envoyer());
        LinearLayout.LayoutParams aDroite = Ui.place(Ui.JUSTE, Ui.JUSTE);
        aDroite.leftMargin = Ui.dp(activite, 8);
        saisie.addView(envoyer, aDroite);
        colonne.addView(saisie);
        compter();
        return colonne;
    }

    /** Prévient quand le texte dépasse un SMS : chaque SMS supplémentaire peut être facturé. */
    private void compter() {
        Sms.Compte compte = Sms.compter(champ.getText().toString());
        boolean utile = compte.messages > 1 || (compte.messages == 1 && compte.restants <= 20);
        compteur.setVisibility(utile ? View.VISIBLE : View.GONE);
        compteur.setText(compte.messages > 1
                ? compte.messages + " SMS · " + compte.restants + " caractères avant le suivant"
                : compte.restants + " caractères avant un 2e SMS");
    }

    @Override
    public void afficher() {
        if (!Autorisations.accordees(activite, Manifest.permission.READ_SMS)) {
            return;
        }
        if (!guette) {
            activite.getContentResolver().registerContentObserver(EcranConversations.SMS, true, guetteur);
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

    private void charger() {
        if (enLecture) {
            return;
        }
        enLecture = true;
        Depot depot = depot();
        long connu = fil;
        Fond.lancer(() -> {
            long trouve = connu;
            if (trouve == 0) {
                Conversations.Resume resume = Conversations.trouver(depot.conversations(), adresse);
                trouve = resume == null ? 0 : resume.fil;
            }
            List<Message> messages = trouve == 0 ? new ArrayList<Message>() : depot.fil(trouve, MESSAGES_AFFICHES);
            String nom = depot.nom(adresse);
            return new Object[] {trouve, Fil.lignes(messages, System.currentTimeMillis(), ZoneId.systemDefault()), nom};
        }, lu -> {
            enLecture = false;
            fil = (Long) lu[0];
            @SuppressWarnings("unchecked")
            List<Fil.Ligne> lues = (List<Fil.Ligne>) lu[1];
            lignes = lues;
            adaptateur.notifyDataSetChanged();
            String nom = (String) lu[2];
            if (!nom.isEmpty() && !nom.equals(titre)) {
                titre = nom;
                Barre.renommer(barre, titre);
            }
        });
    }

    private void envoyer() {
        String texte = champ.getText().toString().trim();
        if (texte.isEmpty()) {
            return;
        }
        if (!Autorisations.accordees(activite, Manifest.permission.SEND_SMS)) {
            Autorisations.demander(activite, DEMANDE, Manifest.permission.SEND_SMS);
            return;
        }
        envoyer.setEnabled(false);
        Depot depot = depot();
        Fond.lancer(() -> {
            try {
                depot.envoyer(adresse, texte);
                return true;
            } catch (RuntimeException refus) {
                return false;
            }
        }, parti -> {
            envoyer.setEnabled(true);
            if (parti) {
                champ.setText("");
                charger();
            } else {
                Dialogues.dire(activite, "Le message n'a pas pu être envoyé.");
            }
        });
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        if (code == DEMANDE && accordees) {
            envoyer();
        } else if (code == DEMANDE) {
            Dialogues.dire(activite, "Sans l'autorisation d'envoyer des SMS, le message reste dans le champ.");
        }
    }

    private final class Bulles extends BaseAdapter {
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
            return rang;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int rang) {
            return lignes.get(rang).jour != null ? 0 : 1;
        }

        @Override
        public boolean isEnabled(int rang) {
            return lignes.get(rang).message != null;
        }

        @Override
        public View getView(int rang, View recyclee, ViewGroup parent) {
            Fil.Ligne ligne = lignes.get(rang);
            if (ligne.jour != null) {
                TextView jour = recyclee != null ? (TextView) recyclee : Ui.discret(activite, "");
                jour.setGravity(Gravity.CENTER);
                Ui.marges(jour, activite, 16, 14, 16, 6);
                jour.setText(ligne.jour);
                return jour;
            }
            LinearLayout rangee = (LinearLayout) recyclee;
            if (rangee == null) {
                rangee = Ui.colonne(activite);
                Ui.marges(rangee, activite, 12, 3, 12, 3);
                TextView bulle = Ui.texte(activite, "");
                Ui.marges(bulle, activite, 14, 9, 14, 9);
                // Une bulle n'occupe jamais toute la largeur : on voit d'un coup d'œil qui parle.
                bulle.setMaxWidth(activite.getResources().getDisplayMetrics().widthPixels * 78 / 100);
                rangee.addView(bulle, Ui.place(Ui.JUSTE, Ui.JUSTE));
                TextView heure = Ui.texte(activite, "", 12, fr.manboyx.commun.R.color.texte2);
                heure.setPadding(Ui.dp(activite, 6), Ui.dp(activite, 2), Ui.dp(activite, 6), 0);
                rangee.addView(heure, Ui.place(Ui.JUSTE, Ui.JUSTE));
            }
            Message message = ligne.message;
            rangee.setGravity(message.recu ? Gravity.START : Gravity.END);
            TextView bulle = (TextView) rangee.getChildAt(0);
            bulle.setText(message.texte);
            bulle.setBackgroundResource(message.recu ? R.drawable.fond_bulle_recue : R.drawable.fond_bulle_envoyee);
            bulle.setTextColor(Ui.couleur(activite, message.recu ? fr.manboyx.commun.R.color.texte : fr.manboyx.commun.R.color.sur_accent));
            bulle.setContentDescription((message.recu ? "Reçu : " : "Envoyé : ") + message.texte);
            ((TextView) rangee.getChildAt(1)).setText(Dates.heure(message.quand, ZoneId.systemDefault()));
            return rangee;
        }
    }
}
