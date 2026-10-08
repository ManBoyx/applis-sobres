package fr.manboyx.calendrier;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;

import fr.manboyx.calendrier.logique.Evenement;
import fr.manboyx.calendrier.logique.Fiche;
import fr.manboyx.calendrier.logique.Repetition;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class DepotAndroid implements Depot {
    private static final String COMPTE_LOCAL = "Calendrier";

    private final ContentResolver base;

    public DepotAndroid(ContentResolver base) {
        this.base = base;
    }

    @Override
    public List<Agenda> agendas() {
        List<Agenda> agendas = new ArrayList<>();
        String[] colonnes = {Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.CALENDAR_COLOR, Calendars.CALENDAR_ACCESS_LEVEL};
        try (Cursor c = base.query(Calendars.CONTENT_URI, colonnes, null, null, Calendars._ID)) {
            while (c != null && c.moveToNext()) {
                agendas.add(new Agenda(c.getLong(0), c.getString(1), c.getInt(2), c.getInt(3) >= Calendars.CAL_ACCESS_CONTRIBUTOR));
            }
        }
        return agendas;
    }

    @Override
    public List<Evenement> entre(long debut, long fin) {
        List<Evenement> evenements = new ArrayList<>();
        Uri.Builder intervalle = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(intervalle, debut);
        ContentUris.appendId(intervalle, fin);
        String[] colonnes = {Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN, Instances.END, Instances.ALL_DAY,
                Instances.DISPLAY_COLOR, Instances.EVENT_LOCATION};
        try (Cursor c = base.query(intervalle.build(), colonnes, Instances.VISIBLE + "=1", null, Instances.BEGIN)) {
            while (c != null && c.moveToNext()) {
                evenements.add(new Evenement(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3), c.getInt(4) != 0,
                        c.getInt(5) | 0xFF000000, c.getString(6)));
            }
        }
        return evenements;
    }

    @Override
    public Fiche lire(long id) {
        Fiche fiche = new Fiche();
        String[] colonnes = {Events.CALENDAR_ID, Events.TITLE, Events.DTSTART, Events.DTEND, Events.DURATION, Events.ALL_DAY,
                Events.EVENT_LOCATION, Events.DESCRIPTION, Events.RRULE};
        try (Cursor c = base.query(ContentUris.withAppendedId(Events.CONTENT_URI, id), colonnes, null, null, null)) {
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            fiche.id = id;
            fiche.agenda = c.getLong(0);
            fiche.titre = c.getString(1) == null ? "" : c.getString(1);
            fiche.debut = c.getLong(2);
            // Un événement répété n'a pas d'heure de fin : il a une durée.
            fiche.fin = c.isNull(3) ? fiche.debut + Fiche.millis(c.getString(4)) : c.getLong(3);
            fiche.journee = c.getInt(5) != 0;
            fiche.lieu = c.getString(6) == null ? "" : c.getString(6);
            fiche.notes = c.getString(7) == null ? "" : c.getString(7);
            fiche.regleDOrigine = c.getString(8) == null ? "" : c.getString(8);
            fiche.repetition = Repetition.depuis(fiche.regleDOrigine);
        }
        try (Cursor c = base.query(Reminders.CONTENT_URI, new String[] {Reminders.MINUTES},
                Reminders.EVENT_ID + "=?", new String[] {String.valueOf(id)}, Reminders.MINUTES)) {
            if (c != null && c.moveToFirst()) {
                fiche.rappel = Math.max(0, c.getInt(0));
            }
        }
        return fiche;
    }

    @Override
    public long enregistrer(Fiche f) {
        ContentValues valeurs = new ContentValues();
        valeurs.put(Events.TITLE, f.titre);
        valeurs.put(Events.EVENT_LOCATION, f.lieu);
        valeurs.put(Events.DESCRIPTION, f.notes);
        valeurs.put(Events.DTSTART, f.debut);
        valeurs.put(Events.ALL_DAY, f.journee ? 1 : 0);
        valeurs.put(Events.EVENT_TIMEZONE, f.fuseau(ZoneId.systemDefault()));
        String regle = f.regle();
        if (regle.isEmpty()) {
            valeurs.put(Events.DTEND, f.fin);
            valeurs.putNull(Events.RRULE);
            valeurs.putNull(Events.DURATION);
        } else {
            // L'agenda d'Android l'exige : un événement répété porte une durée, pas une heure de fin.
            valeurs.put(Events.RRULE, regle);
            valeurs.put(Events.DURATION, f.duree());
            valeurs.putNull(Events.DTEND);
        }
        long id = f.id;
        if (id == 0) {
            valeurs.put(Events.CALENDAR_ID, f.agenda);
            Uri cree = base.insert(Events.CONTENT_URI, valeurs);
            if (cree == null) {
                throw new IllegalStateException("L'agenda a refusé l'événement");
            }
            id = ContentUris.parseId(cree);
        } else {
            base.update(ContentUris.withAppendedId(Events.CONTENT_URI, id), valeurs, null, null);
            base.delete(Reminders.CONTENT_URI, Reminders.EVENT_ID + "=?", new String[] {String.valueOf(id)});
        }
        if (f.rappel >= 0) {
            ContentValues rappel = new ContentValues();
            rappel.put(Reminders.EVENT_ID, id);
            rappel.put(Reminders.MINUTES, f.rappel);
            rappel.put(Reminders.METHOD, Reminders.METHOD_ALERT);
            base.insert(Reminders.CONTENT_URI, rappel);
        }
        return id;
    }

    @Override
    public void supprimer(long id) {
        base.delete(ContentUris.withAppendedId(Events.CONTENT_URI, id), null, null);
    }

    @Override
    public long creerAgendaLocal() {
        // Un agenda « local » n'appartient à aucun compte : il n'est envoyé nulle part.
        Uri commeUnCompte = Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(Calendars.ACCOUNT_NAME, COMPTE_LOCAL)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();
        ContentValues valeurs = new ContentValues();
        valeurs.put(Calendars.ACCOUNT_NAME, COMPTE_LOCAL);
        valeurs.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        valeurs.put(Calendars.NAME, COMPTE_LOCAL);
        valeurs.put(Calendars.CALENDAR_DISPLAY_NAME, "Sur ce téléphone");
        valeurs.put(Calendars.CALENDAR_COLOR, 0xFF1D5FA8);
        valeurs.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
        valeurs.put(Calendars.OWNER_ACCOUNT, COMPTE_LOCAL);
        valeurs.put(Calendars.VISIBLE, 1);
        valeurs.put(Calendars.SYNC_EVENTS, 1);
        Uri cree = base.insert(commeUnCompte, valeurs);
        if (cree == null) {
            throw new IllegalStateException("L'agenda n'a pas pu être créé");
        }
        return ContentUris.parseId(cree);
    }
}
