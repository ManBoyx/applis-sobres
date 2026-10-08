package fr.manboyx.calendrier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.view.View;
import android.widget.CheckBox;

import fr.manboyx.calendrier.logique.Fiche;
import fr.manboyx.calendrier.logique.Mois;
import fr.manboyx.calendrier.logique.Repetition;
import fr.manboyx.commun.Fond;
import fr.manboyx.essais.Essai;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Pilote l'appli comme le ferait quelqu'un, sur un agenda en mémoire. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "fr-rFR-w360dp-h740dp-xhdpi")
public class EcransTest {
    static AgendaMemoire agenda;

    public static class ActiviteEssai extends CalendrierActivity {
        @Override
        public Depot depot() {
            return agenda;
        }
    }

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private Application appli;
    private final LocalDate aujourdhui = LocalDate.now();

    static long a(LocalDate jour, int heure, int minute) {
        return jour.atTime(heure, minute).atZone(ZONE).toInstant().toEpochMilli();
    }

    static void remplir(LocalDate aujourdhui) {
        agenda = new AgendaMemoire();
        agenda.agendas.add(new Depot.Agenda(5, "Jours fériés", 0xFF808080, false));
        agenda.agendas.add(new Depot.Agenda(1, "Personnel", 0xFF1D5FA8, true));
        agenda.agendas.add(new Depot.Agenda(2, "Travail", 0xFFC04A3A, true));
        agenda.ajouter("Dentiste", a(aujourdhui, 15, 0), a(aujourdhui, 16, 0), false).lieu = "Cabinet du Dr Martin";
        agenda.ajouter("Café avec Zoé", a(aujourdhui, 9, 0), a(aujourdhui, 9, 30), false);
        agenda.ajouter("Anniversaire d'Alice", aujourdhui.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                aujourdhui.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), true);
    }

    @Before
    public void preparer() {
        Fond.utiliser(Runnable::run);
        appli = RuntimeEnvironment.getApplication();
        shadowOf(appli).grantPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR, Manifest.permission.POST_NOTIFICATIONS);
        remplir(aujourdhui);
    }

    private ActiviteEssai ouvrir() {
        ActiviteEssai a = Robolectric.buildActivity(ActiviteEssai.class).setup().get();
        Essai.attendre();
        return a;
    }

    private static View ecran(ActiviteEssai a) {
        return a.getWindow().getDecorView();
    }

    /** Le titre tel qu'affiché : le nom du mois avec sa majuscule. */
    private static String titre(YearMonth mois) {
        String nom = Mois.titre(mois);
        return nom.substring(0, 1).toUpperCase(java.util.Locale.FRENCH) + nom.substring(1);
    }

    private static EcranMois mois(ActiviteEssai a) {
        return (EcranMois) a.ecran();
    }

    private static EcranEvenement formulaire(ActiviteEssai a) {
        return (EcranEvenement) a.ecran();
    }

    @Test
    public void aLOuvertureLeMoisCourantEtLesEvenementsDuJourSontAffiches() {
        ActiviteEssai a = ouvrir();
        assertTrue(Essai.visible(ecran(a), titre(YearMonth.now())));
        assertTrue(Essai.visible(ecran(a), "Aujourd'hui"));
        java.util.List<String> textes = Essai.textes(ecran(a));
        int anniversaire = textes.indexOf("Anniversaire d'Alice");
        int cafe = textes.indexOf("Café avec Zoé");
        int dentiste = textes.indexOf("Dentiste");
        assertTrue(textes.toString(), anniversaire >= 0 && anniversaire < cafe && cafe < dentiste);
        assertTrue(textes.contains("Toute la journée"));
        assertTrue(textes.contains("15:00 – 16:00 · Cabinet du Dr Martin"));
    }

    @Test
    public void sansAutorisationLAppliExpliquePourquoiEtNAfficheAucunRendezVous() {
        shadowOf(appli).denyPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
        ActiviteEssai a = ouvrir();
        assertTrue(Essai.contient(ecran(a), "accéder aux agendas"));
        assertFalse(Essai.visible(ecran(a), "Dentiste"));
    }

    @Test
    public void lesFlechesChangentDeMoisEtLeBoutonRevientAAujourdHui() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Mois suivant");
        assertTrue(Essai.visible(ecran(a), titre(YearMonth.now().plusMonths(1))));
        assertFalse(Essai.visible(ecran(a), "Dentiste"));
        Essai.appuyer(ecran(a), "Mois précédent");
        Essai.appuyer(ecran(a), "Mois précédent");
        assertTrue(Essai.visible(ecran(a), titre(YearMonth.now().minusMonths(1))));
        Essai.appuyer(ecran(a), "Revenir à aujourd'hui");
        assertTrue(Essai.visible(ecran(a), titre(YearMonth.now())));
        assertTrue(Essai.visible(ecran(a), "Dentiste"));
    }

    @Test
    public void choisirUnAutreJourMontreSesEvenementsOuDitQuIlNYARien() {
        ActiviteEssai a = ouvrir();
        LocalDate autre = aujourdhui.getDayOfMonth() < 15 ? aujourdhui.plusDays(3) : aujourdhui.minusDays(3);
        mois(a).choisir(autre);
        Essai.attendre();
        assertFalse(Essai.visible(ecran(a), "Dentiste"));
        assertTrue(Essai.visible(ecran(a), "Rien de prévu ce jour-là."));
        mois(a).choisir(aujourdhui);
        Essai.attendre();
        assertTrue(Essai.visible(ecran(a), "Dentiste"));
    }

    @Test
    public void unNouvelEvenementEstEnregistreDansLePremierAgendaModifiableEtApparaitDansLaJournee() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvel événement");
        assertTrue(Essai.visible(ecran(a), "Nouvel événement"));
        Essai.saisir(ecran(a), "Titre", "Réunion parents");
        Essai.saisir(ecran(a), "Lieu", "École");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertTrue("retour au mois, événement visible", Essai.visible(ecran(a), "Réunion parents"));
        Fiche creee = agenda.fiches.get(agenda.fiches.size() - 1);
        assertEquals("l'agenda en lecture seule est ignoré", 1, creee.agenda);
        assertEquals("École", creee.lieu);
        assertEquals(3_600_000L, creee.fin - creee.debut);
        assertEquals(aujourdhui, Instant.ofEpochMilli(creee.debut).atZone(ZONE).toLocalDate());
    }

    @Test
    public void sansAucunAgendaModifiableUnAgendaLocalEstCree() {
        agenda.agendas.removeIf(ag -> ag.modifiable);
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvel événement");
        assertEquals(1, agenda.agendasCrees);
        Essai.saisir(ecran(a), "Titre", "Premier rendez-vous");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertEquals(99, agenda.fiches.get(agenda.fiches.size() - 1).agenda);
    }

    @Test
    public void unEvenementSansTitreEstEnregistreSansTitre() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvel événement");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertEquals("Sans titre", agenda.fiches.get(agenda.fiches.size() - 1).titre);
    }

    @Test
    public void touteLaJourneeMasqueLesHeuresEtNoteLeJourEnTempsUniversel() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvel événement");
        assertTrue(Essai.trouver(ecran(a), "Heure de début") != null);
        ((CheckBox) Essai.trouver(ecran(a), "Toute la journée")).setChecked(true);
        Essai.attendre();
        assertNull(Essai.trouver(ecran(a), "Heure de début"));
        assertNull(Essai.trouver(ecran(a), "Heure de fin"));
        Essai.saisir(ecran(a), "Titre", "Férié");
        Essai.appuyer(ecran(a), "Enregistrer");
        Fiche creee = agenda.fiches.get(agenda.fiches.size() - 1);
        assertTrue(creee.journee);
        assertEquals(aujourdhui.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), creee.debut);
        assertEquals(aujourdhui.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), creee.fin);
        assertTrue(Essai.visible(ecran(a), "Férié"));
    }

    @Test
    public void uneRepetitionHebdomadaireReparaitLaSemaineSuivante() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvel événement");
        Essai.saisir(ecran(a), "Titre", "Piscine");
        Essai.appuyer(ecran(a), "Répétition");
        Essai.choisir("Chaque semaine");
        assertTrue(Essai.visible(ecran(a), "Répétition : Chaque semaine"));
        Essai.appuyer(ecran(a), "Enregistrer");
        assertEquals(Repetition.SEMAINE, agenda.fiches.get(agenda.fiches.size() - 1).repetition);
        mois(a).choisir(aujourdhui.plusDays(7));
        Essai.attendre();
        assertTrue(Essai.visible(ecran(a), "Piscine"));
        assertFalse("les événements non répétés n'y sont pas", Essai.visible(ecran(a), "Dentiste"));
    }

    @Test
    public void unRappelEstEnregistreAvecLEvenement() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvel événement");
        Essai.appuyer(ecran(a), "Rappel");
        Essai.choisir("10 minutes avant");
        assertTrue(Essai.visible(ecran(a), "Rappel : 10 minutes avant"));
        Essai.appuyer(ecran(a), "Enregistrer");
        assertEquals(10, agenda.fiches.get(agenda.fiches.size() - 1).rappel);
    }

    @Test
    public void sansDroitDeNotifierLAppliLeDemandePuisEnregistreQuandMeme() {
        shadowOf(appli).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
        ActiviteEssai a = ouvrir();
        int avant = agenda.fiches.size();
        Essai.appuyer(ecran(a), "Nouvel événement");
        Essai.appuyer(ecran(a), "Rappel");
        Essai.choisir("À l'heure");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertEquals(Manifest.permission.POST_NOTIFICATIONS, shadowOf(a).getLastRequestedPermission().requestedPermissions[0]);
        assertEquals("rien n'est écrit avant la réponse", avant, agenda.fiches.size());
        a.onRequestPermissionsResult(2, new String[] {Manifest.permission.POST_NOTIFICATIONS},
                new int[] {android.content.pm.PackageManager.PERMISSION_DENIED});
        Essai.attendre();
        assertEquals(avant + 1, agenda.fiches.size());
    }

    @Test
    public void appuyerSurUnEvenementLOuvrePourLeModifierSansEnCreerUnAutre() {
        ActiviteEssai a = ouvrir();
        int avant = agenda.fiches.size();
        Essai.appuyer(ecran(a), "Dentiste");
        assertTrue(Essai.visible(ecran(a), "Modifier"));
        assertEquals("Dentiste", Essai.champ(ecran(a), "Titre").getText().toString());
        assertEquals("Cabinet du Dr Martin", Essai.champ(ecran(a), "Lieu").getText().toString());
        assertNull("on ne change pas d'agenda après coup", Essai.trouver(ecran(a), "Agenda"));
        Essai.saisir(ecran(a), "Titre", "Dentiste (contrôle)");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertEquals(avant, agenda.fiches.size());
        assertTrue(Essai.visible(ecran(a), "Dentiste (contrôle)"));
    }

    @Test
    public void supprimerDemandeConfirmationPuisRetireLEvenement() {
        ActiviteEssai a = ouvrir();
        int avant = agenda.fiches.size();
        Essai.appuyer(ecran(a), "Dentiste");
        Essai.appuyer(ecran(a), "Supprimer");
        assertEquals(avant, agenda.fiches.size());
        Essai.valider();
        assertEquals(avant - 1, agenda.fiches.size());
        assertFalse(Essai.visible(ecran(a), "Dentiste"));
        assertTrue(Essai.visible(ecran(a), "Café avec Zoé"));
    }

    @Test
    public void deplacerLeDebutGardeLaMemeDuree() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Dentiste");
        formulaire(a).debut(LocalDateTime.of(aujourdhui.plusDays(2), java.time.LocalTime.of(10, 30)));
        Essai.appuyer(ecran(a), "Enregistrer");
        Fiche dentiste = agenda.fiches.get(0);
        assertEquals(a(aujourdhui.plusDays(2), 10, 30), dentiste.debut);
        assertEquals(a(aujourdhui.plusDays(2), 11, 30), dentiste.fin);
    }

    @Test
    public void uneFinPlaceeAvantLeDebutEstRepousseeApresLui() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Dentiste");
        formulaire(a).fin(LocalDateTime.of(aujourdhui, java.time.LocalTime.of(8, 0)));
        Essai.appuyer(ecran(a), "Enregistrer");
        Fiche dentiste = agenda.fiches.get(0);
        assertEquals(a(aujourdhui, 15, 0), dentiste.debut);
        assertEquals(a(aujourdhui, 16, 0), dentiste.fin);
    }

    @Test
    public void quandIlYAPlusieursAgendasOnPeutChoisirCeluiDUnNouvelEvenement() {
        ActiviteEssai a = ouvrir();
        Essai.appuyer(ecran(a), "Nouvel événement");
        assertTrue(Essai.visible(ecran(a), "Agenda : Personnel"));
        Essai.appuyer(ecran(a), "Agenda");
        Essai.choisir("Travail");
        Essai.appuyer(ecran(a), "Enregistrer");
        assertEquals(2, agenda.fiches.get(agenda.fiches.size() - 1).agenda);
    }
}
