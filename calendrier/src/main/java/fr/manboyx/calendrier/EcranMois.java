package fr.manboyx.calendrier;

import android.Manifest;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.calendrier.logique.Evenement;
import fr.manboyx.calendrier.logique.Fiche;
import fr.manboyx.calendrier.logique.Mois;
import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Dates;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Le mois, et sous lui les événements du jour choisi. */
final class EcranMois extends Ecran {
    private static final int DEMANDE = 1;
    static final String[] AUTORISATIONS = {Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};

    private YearMonth mois = YearMonth.now();
    private LocalDate choisi = LocalDate.now();
    private Map<LocalDate, List<Evenement>> jours = new HashMap<>();
    private List<Evenement> duJour = new ArrayList<>();
    private final Lignes adaptateur = new Lignes();
    private LinearLayout barre;
    private VueMois grille;
    private TextView titreJour;
    private TextView vide;
    private View contenu;
    private View explication;

    private Depot depot() {
        return ((CalendrierActivity) activite).depot();
    }

    @Override
    protected View creer() {
        FrameLayout racine = new FrameLayout(activite);
        LinearLayout colonne = Ui.colonne(activite);
        TextView precedent = Ui.touche(activite, "‹", "Mois précédent");
        precedent.setOnClickListener(v -> changer(-1));
        TextView suivant = Ui.touche(activite, "›", "Mois suivant");
        suivant.setOnClickListener(v -> changer(1));
        TextView aujourdhui = Ui.touche(activite, "Auj.", "Revenir à aujourd'hui");
        aujourdhui.setTextSize(15);
        aujourdhui.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.accent));
        aujourdhui.setOnClickListener(v -> {
            choisi = LocalDate.now();
            mois = YearMonth.from(choisi);
            afficher();
        });
        TextView nouveau = Ui.touche(activite, "+", "Nouvel événement");
        nouveau.setOnClickListener(v -> nouveau());
        barre = Barre.creer(activite, titre(), false, aujourdhui, precedent, suivant, nouveau);
        // Quatre boutons à droite : le nom du mois doit tenir dans ce qui reste, même « septembre ».
        Barre.taille(barre, 18);
        colonne.addView(barre);

        LinearLayout semaine = Ui.rangee(activite);
        for (String lettre : new String[] {"L", "M", "M", "J", "V", "S", "D"}) {
            TextView jour = Ui.discret(activite, lettre);
            jour.setGravity(Gravity.CENTER);
            // Ces lettres répétées ne sont qu'un repère visuel : elles ne doivent pas être prises pour des boutons.
            jour.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            semaine.addView(jour, new LinearLayout.LayoutParams(0, Ui.dp(activite, 28), 1));
        }
        colonne.addView(semaine);

        grille = new VueMois(activite, new VueMois.Ecoute() {
            @Override
            public void jour(LocalDate jour) {
                choisir(jour);
            }

            @Override
            public void glissement(int sens) {
                changer(sens);
            }
        });
        colonne.addView(grille, Ui.place(Ui.PLEIN, Ui.JUSTE));
        colonne.addView(Ui.trait(activite));

        titreJour = Ui.gras(Ui.texte(activite, "", 15, fr.manboyx.commun.R.color.accent));
        Ui.marges(titreJour, activite, 16, 12, 16, 4);
        colonne.addView(titreJour);
        FrameLayout cadre = new FrameLayout(activite);
        ListView liste = new ListView(activite);
        liste.setDivider(null);
        liste.setAdapter(adaptateur);
        liste.setOnItemClickListener((parent, vue, rang, id) -> ouvrir(duJour.get(rang)));
        cadre.addView(liste, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        vide = Ui.vide(activite, "Rien de prévu ce jour-là.");
        cadre.addView(vide, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
        colonne.addView(cadre, Ui.reste(false));
        contenu = colonne;
        racine.addView(contenu, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));

        explication = Autorisations.explication(activite,
                "Pour afficher et créer vos rendez-vous, l'appli a besoin d'accéder aux agendas du téléphone.",
                DEMANDE, AUTORISATIONS);
        explication.setBackgroundColor(Ui.couleur(activite, fr.manboyx.commun.R.color.fond));
        racine.addView(explication, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        return racine;
    }

    LocalDate choisi() {
        return choisi;
    }

    /** « Octobre 2026 » */
    private String titre() {
        String nom = Mois.titre(mois);
        return nom.substring(0, 1).toUpperCase(java.util.Locale.FRENCH) + nom.substring(1);
    }

    /** Passe au mois suivant (+1) ou précédent (−1) ; le jour choisi suit, au même quantième si possible. */
    void changer(int sens) {
        mois = mois.plusMonths(sens);
        choisi = mois.atDay(Math.min(choisi.getDayOfMonth(), mois.lengthOfMonth()));
        afficher();
    }

    void choisir(LocalDate jour) {
        choisi = jour;
        if (!YearMonth.from(jour).equals(mois)) {
            // Un jour grisé du mois voisin : on y va.
            mois = YearMonth.from(jour);
            afficher();
            return;
        }
        dessiner();
    }

    @Override
    public void afficher() {
        boolean autorise = Autorisations.accordees(activite, AUTORISATIONS);
        explication.setVisibility(autorise ? View.GONE : View.VISIBLE);
        contenu.setVisibility(autorise ? View.VISIBLE : View.INVISIBLE);
        Barre.renommer(barre, titre());
        if (!autorise) {
            return;
        }
        dessiner();
        ZoneId zone = ZoneId.systemDefault();
        List<LocalDate> cases = Mois.grille(mois);
        // Un jour de marge de chaque côté : les journées entières sont notées en temps universel.
        long debut = cases.get(0).minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long fin = cases.get(cases.size() - 1).plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli();
        YearMonth demande = mois;
        Depot depot = depot();
        Fond.lancer(() -> Mois.parJour(depot.entre(debut, fin), zone), lus -> {
            if (demande.equals(mois)) {
                jours = lus;
                dessiner();
            }
        });
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        afficher();
    }

    private void dessiner() {
        grille.montrer(mois, LocalDate.now(), choisi, jours);
        ZoneId zone = ZoneId.systemDefault();
        long midi = choisi.atTime(12, 0).atZone(zone).toInstant().toEpochMilli();
        String nom = Dates.jour(midi, System.currentTimeMillis(), zone);
        titreJour.setText(nom.substring(0, 1).toUpperCase(java.util.Locale.FRENCH) + nom.substring(1));
        List<Evenement> prevus = jours.get(choisi);
        duJour = prevus == null ? new ArrayList<>() : prevus;
        adaptateur.notifyDataSetChanged();
        vide.setVisibility(duJour.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void ouvrir(Evenement evenement) {
        Depot depot = depot();
        Fond.lancer(() -> new Object[] {depot.lire(evenement.id), depot.agendas()}, lu -> {
            if (lu[0] == null) {
                afficher();
                return;
            }
            @SuppressWarnings("unchecked")
            List<Depot.Agenda> agendas = (List<Depot.Agenda>) lu[1];
            activite.ouvrir(new EcranEvenement((Fiche) lu[0], agendas));
        });
    }

    private void nouveau() {
        Depot depot = depot();
        LocalDate jour = choisi;
        Fond.lancer(() -> {
            try {
                List<Depot.Agenda> agendas = depot.agendas();
                boolean unModifiable = false;
                for (Depot.Agenda agenda : agendas) {
                    unModifiable |= agenda.modifiable;
                }
                if (!unModifiable) {
                    // Téléphone sans compte d'agenda : on en crée un qui reste sur le téléphone.
                    depot.creerAgendaLocal();
                    agendas = depot.agendas();
                }
                return agendas;
            } catch (RuntimeException echec) {
                return null;
            }
        }, agendas -> {
            Fiche fiche = Fiche.nouvelle(jour, System.currentTimeMillis(), ZoneId.systemDefault());
            if (agendas != null) {
                for (Depot.Agenda agenda : agendas) {
                    if (agenda.modifiable && fiche.agenda == 0) {
                        fiche.agenda = agenda.id;
                    }
                }
            }
            if (fiche.agenda == 0) {
                Dialogues.dire(activite, "Aucun agenda du téléphone n'accepte de nouvel événement.");
                return;
            }
            activite.ouvrir(new EcranEvenement(fiche, agendas));
        });
    }

    private final class Lignes extends BaseAdapter {
        @Override
        public int getCount() {
            return duJour.size();
        }

        @Override
        public Object getItem(int rang) {
            return duJour.get(rang);
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
                rangee.addView(new View(activite), Ui.place(Ui.dp(activite, 4), Ui.dp(activite, 40)));
                LinearLayout textes = Ui.colonne(activite);
                textes.setPadding(Ui.dp(activite, 14), 0, 0, 0);
                textes.addView(Ui.uneLigne(Ui.texte(activite, "")));
                textes.addView(Ui.uneLigne(Ui.discret(activite, "")));
                rangee.addView(textes, Ui.reste(true));
            }
            Evenement e = duJour.get(rang);
            rangee.getChildAt(0).setBackgroundColor(e.couleur);
            LinearLayout textes = (LinearLayout) rangee.getChildAt(1);
            ((TextView) textes.getChildAt(0)).setText(e.titre);
            String horaire = Mois.horaire(e, ZoneId.systemDefault());
            ((TextView) textes.getChildAt(1)).setText(e.lieu.isEmpty() ? horaire : horaire + " · " + e.lieu);
            return rangee;
        }
    }
}
