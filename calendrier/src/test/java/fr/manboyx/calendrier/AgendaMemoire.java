package fr.manboyx.calendrier;

import fr.manboyx.calendrier.logique.Evenement;
import fr.manboyx.calendrier.logique.Fiche;
import fr.manboyx.calendrier.logique.Repetition;

import java.util.ArrayList;
import java.util.List;

/** Un agenda en mémoire, pour piloter les écrans sans téléphone. */
final class AgendaMemoire implements Depot {
    final List<Agenda> agendas = new ArrayList<>();
    final List<Fiche> fiches = new ArrayList<>();
    int agendasCrees;
    private long prochain = 1;

    Fiche ajouter(String titre, long debut, long fin, boolean journee) {
        Fiche f = new Fiche();
        f.agenda = 1;
        f.titre = titre;
        f.debut = debut;
        f.fin = fin;
        f.journee = journee;
        enregistrer(f);
        return f;
    }

    @Override
    public List<Agenda> agendas() {
        return new ArrayList<>(agendas);
    }

    @Override
    public List<Evenement> entre(long debut, long fin) {
        List<Evenement> sortie = new ArrayList<>();
        long semaine = 7 * 24 * 3600_000L;
        for (Fiche f : fiches) {
            // Les répétitions hebdomadaires sont dépliées comme le fait le téléphone ; les autres ne servent pas aux tests.
            long pas = f.repetition == Repetition.SEMAINE ? semaine : Long.MAX_VALUE / 4;
            for (long decalage = 0; f.debut + decalage < fin && decalage < 60 * semaine; decalage += pas) {
                if (f.fin + decalage > debut) {
                    sortie.add(new Evenement(f.id, f.titre, f.debut + decalage, f.fin + decalage, f.journee, 0xFF1D5FA8, f.lieu));
                }
            }
        }
        return sortie;
    }

    @Override
    public Fiche lire(long id) {
        for (Fiche f : fiches) {
            if (f.id == id) {
                return f;
            }
        }
        return null;
    }

    @Override
    public long enregistrer(Fiche fiche) {
        if (fiche.id == 0) {
            fiche.id = prochain++;
            fiches.add(fiche);
        }
        return fiche.id;
    }

    @Override
    public void supprimer(long id) {
        fiches.removeIf(f -> f.id == id);
    }

    @Override
    public long creerAgendaLocal() {
        agendasCrees++;
        agendas.add(new Agenda(99, "Sur ce téléphone", 0xFF1D5FA8, true));
        return 99;
    }
}
