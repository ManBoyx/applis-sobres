package fr.manboyx.telephone;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Dates;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Numeros;
import fr.manboyx.commun.Onglets;
import fr.manboyx.commun.Ui;
import fr.manboyx.commun.Affichage;
import fr.manboyx.telephone.logique.Appel;
import fr.manboyx.commun.Correspondant;
import fr.manboyx.telephone.logique.Journal;
import fr.manboyx.commun.Recherche;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** L'écran de l'appli : clavier, journal d'appels et contacts, un onglet chacun. */
final class EcranPrincipal extends Ecran {
    private static final int CLAVIER = 0;
    private static final int JOURNAL = 1;
    private static final int CONTACTS = 2;
    private static final int DEMANDE_APPEL = 1;
    private static final int DEMANDE_JOURNAL = 2;
    private static final int DEMANDE_CONTACTS = 3;
    /** Assez pour remonter plusieurs semaines sans tout charger. */
    private static final int APPELS_AFFICHES = 300;

    private String saisie;
    private String enAttente = "";
    private List<Correspondant> carnet = new ArrayList<>();
    private List<Journal.Groupe> groupes = new ArrayList<>();
    private List<Correspondant> trouves = new ArrayList<>();

    private Onglets onglets;
    private FrameLayout contenu;
    private View clavier;
    private TextView numero;
    private LinearLayout suggestions;
    private View journal;
    private ListView listeJournal;
    private TextView journalVide;
    private View journalExplication;
    private View contacts;
    private EditText recherche;
    private ListView listeContacts;
    private TextView contactsVide;
    private View contactsExplication;
    private final Appels adaptateurJournal = new Appels();
    private final Numeros2 adaptateurContacts = new Numeros2();

    EcranPrincipal(String numeroDeDepart) {
        saisie = Numeros.chiffres(numeroDeDepart);
    }

    private Depot depot() {
        return ((TelephoneActivity) activite).depot();
    }

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        contenu = new FrameLayout(activite);
        colonne.addView(contenu, Ui.reste(false));
        colonne.addView(Ui.trait(activite));
        onglets = new Onglets(activite, new String[] {"Clavier", "Journal", "Contacts"}, this::montrer);
        colonne.addView(onglets, Ui.place(Ui.PLEIN, Ui.JUSTE));
        clavier = clavier();
        journal = journal();
        contacts = contacts();
        for (View panneau : new View[] {clavier, journal, contacts}) {
            contenu.addView(panneau, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        }
        onglets.choisir(CLAVIER);
        return colonne;
    }

    private void montrer(int onglet) {
        clavier.setVisibility(onglet == CLAVIER ? View.VISIBLE : View.GONE);
        journal.setVisibility(onglet == JOURNAL ? View.VISIBLE : View.GONE);
        contacts.setVisibility(onglet == CONTACTS ? View.VISIBLE : View.GONE);
        rafraichir();
    }

    @Override
    public void afficher() {
        rafraichir();
    }

    /** Relit ce que montre l'onglet courant ; les autres attendent d'être ouverts. */
    private void rafraichir() {
        boolean voitContacts = Autorisations.accordees(activite, Manifest.permission.READ_CONTACTS);
        if (voitContacts && onglets.courant() != JOURNAL) {
            Fond.lancer(() -> depot().carnet(), lu -> {
                carnet = lu;
                suggerer();
                filtrer();
            });
        }
        if (onglets.courant() == CLAVIER) {
            ecrireNumero();
        } else if (onglets.courant() == JOURNAL) {
            boolean autorise = Autorisations.accordees(activite, Manifest.permission.READ_CALL_LOG);
            journalExplication.setVisibility(autorise ? View.GONE : View.VISIBLE);
            listeJournal.setVisibility(autorise ? View.VISIBLE : View.GONE);
            journalVide.setVisibility(View.GONE);
            if (autorise) {
                Fond.lancer(() -> Journal.regrouper(depot().journal(APPELS_AFFICHES)), lus -> {
                    groupes = lus;
                    adaptateurJournal.notifyDataSetChanged();
                    journalVide.setVisibility(lus.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        } else {
            contactsExplication.setVisibility(voitContacts ? View.GONE : View.VISIBLE);
            recherche.setVisibility(voitContacts ? View.VISIBLE : View.GONE);
            listeContacts.setVisibility(voitContacts ? View.VISIBLE : View.GONE);
            if (!voitContacts) {
                contactsVide.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        if (code == DEMANDE_APPEL) {
            String cible = enAttente;
            enAttente = "";
            if (accordees) {
                appeler(cible);
            } else {
                // Sans l'autorisation, on confie le numéro au téléphone, qui demandera confirmation.
                lancer(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", cible, null)));
            }
            return;
        }
        rafraichir();
    }

    @Override
    public boolean retour() {
        if (onglets.courant() == CONTACTS && recherche.length() > 0) {
            recherche.setText("");
            return true;
        }
        return false;
    }

    // ----- Appeler -----

    private void appeler(String cible) {
        String propre = cible == null ? "" : cible.trim();
        if (propre.isEmpty()) {
            return;
        }
        if (!Autorisations.accordees(activite, Manifest.permission.CALL_PHONE)) {
            enAttente = propre;
            Autorisations.demander(activite, DEMANDE_APPEL, Manifest.permission.CALL_PHONE);
            return;
        }
        lancer(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", propre, null)));
    }

    private void lancer(Intent intention) {
        try {
            activite.startActivity(intention);
        } catch (ActivityNotFoundException | SecurityException impossible) {
            Dialogues.dire(activite, "Le téléphone n'a pas pu lancer cette action.");
        }
    }

    // ----- Onglet Clavier -----

    private View clavier() {
        LinearLayout colonne = Ui.colonne(activite);
        colonne.setGravity(Gravity.BOTTOM);
        Ui.marges(colonne, activite, 16, 8, 16, 16);

        suggestions = Ui.colonne(activite);
        colonne.addView(suggestions);

        LinearLayout ligne = Ui.rangee(activite);
        ligne.setMinimumHeight(Ui.dp(activite, 72));
        numero = Ui.uneLigne(Ui.texte(activite, "", 30, fr.manboyx.commun.R.color.texte));
        numero.setGravity(Gravity.CENTER);
        numero.setContentDescription("Numéro composé");
        ligne.addView(numero, Ui.reste(true));
        TextView effacer = Ui.touche(activite, "⌫", "Effacer");
        effacer.setOnClickListener(v -> {
            if (!saisie.isEmpty()) {
                saisie = saisie.substring(0, saisie.length() - 1);
                ecrireNumero();
            }
        });
        effacer.setOnLongClickListener(v -> {
            saisie = "";
            ecrireNumero();
            return true;
        });
        ligne.addView(effacer);
        colonne.addView(ligne);

        String[][] touches = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"*", "0", "#"}};
        for (String[] rang : touches) {
            LinearLayout rangee = Ui.rangee(activite);
            for (String signe : rang) {
                TextView touche = Ui.touche(activite, signe, "Touche " + signe);
                touche.setTextSize(28);
                touche.setOnClickListener(v -> taper(signe));
                if (signe.equals("0")) {
                    // Appui long sur 0 : le « + » des numéros internationaux.
                    touche.setOnLongClickListener(v -> {
                        taper("+");
                        return true;
                    });
                }
                rangee.addView(touche, new LinearLayout.LayoutParams(0, Ui.dp(activite, 64), 1));
            }
            colonne.addView(rangee);
        }

        Button appeler = Ui.bouton(activite, "Appeler", true);
        appeler.setOnClickListener(v -> appeler(saisie));
        LinearLayout.LayoutParams place = Ui.place(Ui.PLEIN, Ui.dp(activite, 56));
        place.topMargin = Ui.dp(activite, 12);
        colonne.addView(appeler, place);
        return colonne;
    }

    private void taper(String signe) {
        if (saisie.length() < 40) {
            saisie += signe;
            ecrireNumero();
        }
    }

    private void ecrireNumero() {
        numero.setText(Affichage.numero(saisie));
        suggerer();
    }

    /** Sous le numéro en cours de saisie, les contacts qui y ressemblent. */
    private void suggerer() {
        suggestions.removeAllViews();
        if (saisie.length() < 3) {
            return;
        }
        for (Correspondant c : Recherche.filtrer(carnet, saisie, 3)) {
            LinearLayout ligne = Ui.rangee(activite);
            ligne.setMinimumHeight(Ui.dp(activite, 48));
            ligne.setBackgroundResource(fr.manboyx.commun.R.drawable.fond_appui);
            ligne.addView(Ui.uneLigne(Ui.texte(activite, c.nom)), Ui.reste(true));
            ligne.addView(Ui.discret(activite, Affichage.numero(c.numero)));
            ligne.setOnClickListener(v -> {
                saisie = Numeros.chiffres(c.numero);
                ecrireNumero();
            });
            suggestions.addView(ligne);
        }
    }

    // ----- Onglet Journal -----

    private View journal() {
        FrameLayout cadre = new FrameLayout(activite);
        listeJournal = new ListView(activite);
        listeJournal.setDivider(null);
        listeJournal.setAdapter(adaptateurJournal);
        listeJournal.setOnItemClickListener((parent, vue, rang, id) -> appeler(groupes.get(rang).dernier.numero));
        listeJournal.setOnItemLongClickListener((parent, vue, rang, id) -> {
            menu(groupes.get(rang).dernier);
            return true;
        });
        cadre.addView(listeJournal, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        journalVide = Ui.vide(activite, "Aucun appel dans le journal.");
        journalVide.setVisibility(View.GONE);
        cadre.addView(journalVide, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
        journalExplication = Autorisations.explication(activite,
                "Pour afficher vos derniers appels, l'appli a besoin de lire le journal d'appels du téléphone.",
                DEMANDE_JOURNAL, Manifest.permission.READ_CALL_LOG);
        cadre.addView(journalExplication, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        return cadre;
    }

    private static String nomOuNumero(Appel appel) {
        if (!appel.nom.isEmpty()) {
            return appel.nom;
        }
        return appel.numero.isEmpty() ? "Numéro masqué" : Affichage.numero(appel.numero);
    }

    private void menu(Appel appel) {
        if (appel.numero.isEmpty()) {
            return;
        }
        String[] choix = {"Appeler", "Envoyer un message", "Ajouter aux contacts", "Copier le numéro"};
        Dialogues.choisir(activite, nomOuNumero(appel), choix, rang -> {
            if (rang == 0) {
                appeler(appel.numero);
            } else if (rang == 1) {
                lancer(new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", appel.numero, null)));
            } else if (rang == 2) {
                lancer(new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                        .putExtra(ContactsContract.Intents.Insert.PHONE, appel.numero));
            } else {
                ClipboardManager presse = activite.getSystemService(ClipboardManager.class);
                presse.setPrimaryClip(ClipData.newPlainText("Numéro", appel.numero));
                Dialogues.dire(activite, "Numéro copié.");
            }
        });
    }

    private final class Appels extends BaseAdapter {
        @Override
        public int getCount() {
            return groupes.size();
        }

        @Override
        public Object getItem(int rang) {
            return groupes.get(rang);
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
                rangee.setMinimumHeight(Ui.dp(activite, 64));
                Ui.marges(rangee, activite, 16, 8, 16, 8);
                TextView sens = Ui.texte(activite, "", 20, fr.manboyx.commun.R.color.texte2);
                sens.setGravity(Gravity.CENTER);
                rangee.addView(sens, Ui.place(Ui.dp(activite, 32), Ui.JUSTE));
                LinearLayout textes = Ui.colonne(activite);
                textes.setPadding(Ui.dp(activite, 10), 0, Ui.dp(activite, 8), 0);
                textes.addView(Ui.uneLigne(Ui.texte(activite, "")));
                textes.addView(Ui.uneLigne(Ui.discret(activite, "")));
                rangee.addView(textes, Ui.reste(true));
                rangee.addView(Ui.discret(activite, ""));
            }
            Journal.Groupe groupe = groupes.get(rang);
            Appel appel = groupe.dernier;
            boolean manque = appel.sens == Appel.MANQUE;
            TextView sens = (TextView) rangee.getChildAt(0);
            sens.setText(manque ? "✕" : appel.sens == Appel.SORTANT ? "↗" : "↙");
            sens.setTextColor(Ui.couleur(activite, manque ? fr.manboyx.commun.R.color.danger : fr.manboyx.commun.R.color.texte2));
            LinearLayout textes = (LinearLayout) rangee.getChildAt(1);
            TextView titre = (TextView) textes.getChildAt(0);
            titre.setText(nomOuNumero(appel) + (groupe.nombre > 1 ? " (" + groupe.nombre + ")" : ""));
            titre.setTextColor(Ui.couleur(activite, manque ? fr.manboyx.commun.R.color.danger : fr.manboyx.commun.R.color.texte));
            String detail = manque ? "Appel manqué"
                    : (appel.sens == Appel.SORTANT ? "Sortant" : "Entrant") + " · " + Dates.duree(appel.secondes);
            ((TextView) textes.getChildAt(1)).setText(detail);
            ((TextView) rangee.getChildAt(2)).setText(Dates.relative(appel.quand, System.currentTimeMillis(), ZoneId.systemDefault()));
            return rangee;
        }
    }

    // ----- Onglet Contacts -----

    private View contacts() {
        LinearLayout colonne = Ui.colonne(activite);
        recherche = Ui.champ(activite, "Rechercher un nom ou un numéro");
        recherche.addTextChangedListener(new TextWatcher() {
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
        place.setMargins(Ui.dp(activite, 16), Ui.dp(activite, 12), Ui.dp(activite, 16), Ui.dp(activite, 8));
        colonne.addView(recherche, place);

        FrameLayout cadre = new FrameLayout(activite);
        listeContacts = new ListView(activite);
        listeContacts.setDivider(null);
        listeContacts.setAdapter(adaptateurContacts);
        listeContacts.setOnItemClickListener((parent, vue, rang, id) -> appeler(trouves.get(rang).numero));
        cadre.addView(listeContacts, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        contactsVide = Ui.vide(activite, "");
        contactsVide.setVisibility(View.GONE);
        cadre.addView(contactsVide, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
        contactsExplication = Autorisations.explication(activite,
                "Pour appeler un contact par son nom, l'appli a besoin de lire vos contacts.",
                DEMANDE_CONTACTS, Manifest.permission.READ_CONTACTS);
        cadre.addView(contactsExplication, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        colonne.addView(cadre, Ui.reste(false));
        return colonne;
    }

    private void filtrer() {
        String cherche = recherche.getText().toString();
        trouves = Recherche.filtrer(carnet, cherche, 0);
        adaptateurContacts.notifyDataSetChanged();
        boolean visible = listeContacts.getVisibility() == View.VISIBLE;
        contactsVide.setVisibility(visible && trouves.isEmpty() ? View.VISIBLE : View.GONE);
        contactsVide.setText(cherche.trim().isEmpty() ? "Aucun contact avec un numéro." : "Aucun contact ne correspond.");
    }

    private final class Numeros2 extends BaseAdapter {
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
            LinearLayout rangee = (LinearLayout) recyclee;
            if (rangee == null) {
                rangee = Ui.rangee(activite);
                rangee.setMinimumHeight(Ui.dp(activite, 60));
                Ui.marges(rangee, activite, 16, 8, 16, 8);
                LinearLayout textes = Ui.colonne(activite);
                textes.addView(Ui.uneLigne(Ui.texte(activite, "")));
                textes.addView(Ui.uneLigne(Ui.discret(activite, "")));
                rangee.addView(textes, Ui.reste(true));
                rangee.addView(Ui.texte(activite, "★", 16, fr.manboyx.commun.R.color.accent));
            }
            Correspondant c = trouves.get(rang);
            LinearLayout textes = (LinearLayout) rangee.getChildAt(0);
            ((TextView) textes.getChildAt(0)).setText(c.nom.isEmpty() ? Affichage.numero(c.numero) : c.nom);
            ((TextView) textes.getChildAt(1)).setText(Affichage.numero(c.numero));
            rangee.getChildAt(1).setVisibility(c.favori ? View.VISIBLE : View.GONE);
            return rangee;
        }
    }
}
