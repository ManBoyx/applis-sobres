package fr.manboyx.calendrier;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import fr.manboyx.calendrier.logique.Fiche;
import fr.manboyx.calendrier.logique.Repetition;
import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Dates;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Créer ou modifier un événement. */
final class EcranEvenement extends Ecran {
    private static final int DEMANDE_NOTIFICATIONS = 2;
    private static final String[] JOURS = {"lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."};
    private static final String[] MOIS = {"janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."};
    private static final Repetition[] REPETITIONS = {Repetition.AUCUNE, Repetition.JOUR, Repetition.SEMAINE, Repetition.MOIS, Repetition.AN};

    private final Fiche fiche;
    private final List<Depot.Agenda> agendas = new ArrayList<>();
    private EditText titre;
    private EditText lieu;
    private EditText notes;
    private CheckBox journee;
    private Button dateDebut;
    private Button heureDebut;
    private Button dateFin;
    private Button heureFin;
    private Button repetition;
    private Button rappel;
    private Button agenda;
    private TextView erreur;

    EcranEvenement(Fiche fiche, List<Depot.Agenda> agendas) {
        this.fiche = fiche;
        if (agendas != null) {
            for (Depot.Agenda a : agendas) {
                if (a.modifiable) {
                    this.agendas.add(a);
                }
            }
        }
    }

    private Depot depot() {
        return ((CalendrierActivity) activite).depot();
    }

    @Override
    protected View creer() {
        LinearLayout colonne = Ui.colonne(activite);
        colonne.addView(Barre.creer(activite, fiche.id == 0 ? "Nouvel événement" : "Modifier", true));
        LinearLayout corps = Ui.colonne(activite);
        Ui.marges(corps, activite, 16, 8, 16, 24);

        titre = Ui.champ(activite, "Titre");
        titre.setText(fiche.titre);
        corps.addView(titre);

        journee = new CheckBox(activite);
        journee.setText("Toute la journée");
        journee.setTextSize(16);
        journee.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.texte));
        journee.setChecked(fiche.journee);
        journee.setMinHeight(Ui.dp(activite, 48));
        journee.setOnCheckedChangeListener((case_, coche) -> {
            fiche.journee(coche, ZoneId.systemDefault());
            ecrire();
        });
        corps.addView(journee);

        dateDebut = Ui.bouton(activite, "", false);
        dateDebut.setContentDescription("Date de début");
        dateDebut.setOnClickListener(v -> choisirDate(true));
        heureDebut = Ui.bouton(activite, "", false);
        heureDebut.setContentDescription("Heure de début");
        heureDebut.setOnClickListener(v -> choisirHeure(true));
        corps.addView(ligne("Début", dateDebut, heureDebut));
        dateFin = Ui.bouton(activite, "", false);
        dateFin.setContentDescription("Date de fin");
        dateFin.setOnClickListener(v -> choisirDate(false));
        heureFin = Ui.bouton(activite, "", false);
        heureFin.setContentDescription("Heure de fin");
        heureFin.setOnClickListener(v -> choisirHeure(false));
        corps.addView(ligne("Fin", dateFin, heureFin));

        repetition = Ui.bouton(activite, "", false);
        repetition.setContentDescription("Répétition");
        repetition.setOnClickListener(v -> {
            String[] choix = new String[REPETITIONS.length];
            for (int i = 0; i < choix.length; i++) {
                choix[i] = REPETITIONS[i].libelle;
            }
            Dialogues.choisir(activite, "Répétition", choix, rang -> {
                fiche.repetition = REPETITIONS[rang];
                ecrire();
            });
        });
        corps.addView(reglage(repetition));
        rappel = Ui.bouton(activite, "", false);
        rappel.setContentDescription("Rappel");
        rappel.setOnClickListener(v -> {
            String[] choix = new String[Fiche.RAPPELS.length];
            for (int i = 0; i < choix.length; i++) {
                choix[i] = Fiche.libelleRappel(Fiche.RAPPELS[i]);
            }
            Dialogues.choisir(activite, "Rappel", choix, rang -> {
                fiche.rappel = Fiche.RAPPELS[rang];
                ecrire();
            });
        });
        corps.addView(reglage(rappel));
        agenda = Ui.bouton(activite, "", false);
        agenda.setContentDescription("Agenda");
        agenda.setOnClickListener(v -> {
            String[] choix = new String[agendas.size()];
            for (int i = 0; i < choix.length; i++) {
                choix[i] = agendas.get(i).nom;
            }
            Dialogues.choisir(activite, "Agenda", choix, rang -> {
                fiche.agenda = agendas.get(rang).id;
                ecrire();
            });
        });
        corps.addView(reglage(agenda));
        // L'agenda d'un événement existant ne se change pas ; et s'il n'y en a qu'un, il n'y a rien à choisir.
        agenda.setVisibility(fiche.id == 0 && agendas.size() > 1 ? View.VISIBLE : View.GONE);

        lieu = Ui.champ(activite, "Lieu");
        lieu.setText(fiche.lieu);
        corps.addView(lieu, espace(12));
        notes = Ui.champ(activite, "Notes");
        notes.setSingleLine(false);
        notes.setMinLines(2);
        notes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notes.setText(fiche.notes);
        corps.addView(notes, espace(8));

        erreur = Ui.texte(activite, "", 14, fr.manboyx.commun.R.color.danger);
        erreur.setVisibility(View.GONE);
        corps.addView(erreur, espace(12));
        Button enregistrer = Ui.bouton(activite, "Enregistrer", true);
        enregistrer.setOnClickListener(v -> enregistrer());
        corps.addView(enregistrer, espace(20));
        if (fiche.id != 0) {
            Button supprimer = Ui.bouton(activite, "Supprimer", false);
            supprimer.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.danger));
            supprimer.setOnClickListener(v -> Dialogues.confirmer(activite, "Supprimer cet événement ?",
                    fiche.repetition == Repetition.AUCUNE ? "Il sera retiré de l'agenda." : "Toutes ses répétitions seront retirées de l'agenda.",
                    "Supprimer", () -> Fond.lancer(() -> {
                        depot().supprimer(fiche.id);
                        return true;
                    }, fait -> activite.fermer())));
            corps.addView(supprimer, espace(12));
        }
        ScrollView defilement = new ScrollView(activite);
        defilement.addView(corps);
        colonne.addView(defilement, Ui.reste(false));
        ecrire();
        return colonne;
    }

    private LinearLayout.LayoutParams espace(int haut) {
        LinearLayout.LayoutParams place = Ui.place(Ui.PLEIN, Ui.JUSTE);
        place.topMargin = Ui.dp(activite, haut);
        return place;
    }

    private View ligne(String nom, Button date, Button heure) {
        LinearLayout rangee = Ui.rangee(activite);
        rangee.setPadding(0, Ui.dp(activite, 8), 0, 0);
        rangee.addView(Ui.texte(activite, nom), Ui.place(Ui.dp(activite, 56), Ui.JUSTE));
        rangee.addView(date, Ui.reste(true));
        LinearLayout.LayoutParams aDroite = Ui.place(Ui.JUSTE, Ui.JUSTE);
        aDroite.leftMargin = Ui.dp(activite, 8);
        rangee.addView(heure, aDroite);
        return rangee;
    }

    private View reglage(Button bouton) {
        bouton.setLayoutParams(espace(8));
        bouton.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
        return bouton;
    }

    /** Le moment tel que l'utilisateur le voit : une journée entière est notée en temps universel. */
    private LocalDateTime lire(long instant) {
        return Instant.ofEpochMilli(instant).atZone(fiche.journee ? ZoneOffset.UTC : ZoneId.systemDefault()).toLocalDateTime();
    }

    private long noter(LocalDateTime moment) {
        return moment.atZone(fiche.journee ? ZoneOffset.UTC : ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** Le dernier jour affiché : pour une journée entière, la fin notée est le lendemain. */
    private LocalDate dernierJour() {
        return lire(fiche.journee ? fiche.fin - 1 : fiche.fin).toLocalDate();
    }

    private static String ecrireDate(LocalDate jour) {
        return JOURS[jour.getDayOfWeek().getValue() - 1] + " " + jour.getDayOfMonth() + " " + MOIS[jour.getMonthValue() - 1] + " " + jour.getYear();
    }

    private void ecrire() {
        ZoneId zone = fiche.journee ? ZoneOffset.UTC : ZoneId.systemDefault();
        dateDebut.setText(ecrireDate(lire(fiche.debut).toLocalDate()));
        dateFin.setText(ecrireDate(dernierJour()));
        heureDebut.setText(Dates.heure(fiche.debut, zone));
        heureFin.setText(Dates.heure(fiche.fin, zone));
        heureDebut.setVisibility(fiche.journee ? View.GONE : View.VISIBLE);
        heureFin.setVisibility(fiche.journee ? View.GONE : View.VISIBLE);
        repetition.setText("Répétition : " + fiche.repetition.libelle);
        rappel.setText("Rappel : " + Fiche.libelleRappel(fiche.rappel));
        for (Depot.Agenda a : agendas) {
            if (a.id == fiche.agenda) {
                agenda.setText("Agenda : " + a.nom);
            }
        }
    }

    /** Change le début ; la fin suit pour garder la même durée. */
    void debut(LocalDateTime moment) {
        long duree = fiche.fin - fiche.debut;
        fiche.debut = noter(moment);
        fiche.fin = fiche.debut + duree;
        ecrire();
    }

    /** Change la fin. Une fin placée avant le début est repoussée après lui. */
    void fin(LocalDateTime moment) {
        fiche.fin = noter(fiche.journee ? moment.toLocalDate().plusDays(1).atStartOfDay() : moment);
        if (fiche.fin < fiche.debut || (fiche.journee && fiche.fin == fiche.debut)) {
            // Une heure après le début, ou le jour même pour une journée entière.
            fiche.fin = fiche.debut + (fiche.journee ? 24 * 3_600_000L : 3_600_000L);
        }
        ecrire();
    }

    private void choisirDate(boolean duDebut) {
        LocalDate jour = duDebut ? lire(fiche.debut).toLocalDate() : dernierJour();
        new DatePickerDialog(activite, (vue, annee, moisZero, quantieme) -> {
            LocalDate choisi = LocalDate.of(annee, moisZero + 1, quantieme);
            if (duDebut) {
                debut(LocalDateTime.of(choisi, lire(fiche.debut).toLocalTime()));
            } else {
                fin(LocalDateTime.of(choisi, lire(fiche.fin).toLocalTime()));
            }
        }, jour.getYear(), jour.getMonthValue() - 1, jour.getDayOfMonth()).show();
    }

    private void choisirHeure(boolean duDebut) {
        LocalTime heure = lire(duDebut ? fiche.debut : fiche.fin).toLocalTime();
        new TimePickerDialog(activite, (vue, heures, minutes) -> {
            LocalTime choisie = LocalTime.of(heures, minutes);
            if (duDebut) {
                debut(LocalDateTime.of(lire(fiche.debut).toLocalDate(), choisie));
            } else {
                fin(LocalDateTime.of(lire(fiche.fin).toLocalDate(), choisie));
            }
        }, heure.getHour(), heure.getMinute(), true).show();
    }

    private void enregistrer() {
        fiche.titre = titre.getText().toString();
        fiche.lieu = lieu.getText().toString();
        fiche.notes = notes.getText().toString();
        fiche.corriger();
        // Un rappel s'affiche par une notification : sur Android 13 et suivants, il faut l'avoir permis.
        if (fiche.rappel >= 0 && Build.VERSION.SDK_INT >= 33
                && !Autorisations.accordees(activite, Manifest.permission.POST_NOTIFICATIONS)) {
            Autorisations.demander(activite, DEMANDE_NOTIFICATIONS, Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        ecrireDansLAgenda();
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        if (code == DEMANDE_NOTIFICATIONS) {
            if (!accordees) {
                Dialogues.dire(activite, "Sans notifications, le rappel ne s'affichera pas.");
            }
            ecrireDansLAgenda();
        }
    }

    private void ecrireDansLAgenda() {
        Depot depot = depot();
        Fond.lancer(() -> {
            try {
                depot.enregistrer(fiche);
                return true;
            } catch (RuntimeException echec) {
                return false;
            }
        }, reussi -> {
            if (reussi) {
                activite.fermer();
            } else {
                erreur.setText("L'événement n'a pas pu être enregistré.");
                erreur.setVisibility(View.VISIBLE);
            }
        });
    }
}
