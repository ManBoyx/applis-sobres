package fr.manboyx.calendrier;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract.CalendarAlerts;

import fr.manboyx.calendrier.logique.Evenement;
import fr.manboyx.calendrier.logique.Mois;

import java.time.ZoneId;

/**
 * Réveillé par le téléphone à l'heure d'un rappel : affiche la notification,
 * puis se rendort. Aucun service ne tourne en attendant.
 */
public final class RecepteurRappel extends BroadcastReceiver {
    static final String CANAL = "rappels";

    @Override
    public void onReceive(Context contexte, Intent annonce) {
        if (contexte.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager notifications = contexte.getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(CANAL, "Rappels d'événements", NotificationManager.IMPORTANCE_HIGH));
        PendingIntent ouvrir = PendingIntent.getActivity(contexte, 0,
                new Intent(contexte, CalendrierActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String[] colonnes = {CalendarAlerts._ID, CalendarAlerts.EVENT_ID, CalendarAlerts.TITLE, CalendarAlerts.BEGIN,
                CalendarAlerts.END, CalendarAlerts.ALL_DAY, CalendarAlerts.EVENT_LOCATION};
        // Les rappels arrivés à échéance et pas encore montrés.
        String echus = CalendarAlerts.STATE + "=? AND " + CalendarAlerts.ALARM_TIME + "<=?";
        String[] arguments = {String.valueOf(CalendarAlerts.STATE_SCHEDULED), String.valueOf(System.currentTimeMillis())};
        try (Cursor c = contexte.getContentResolver().query(CalendarAlerts.CONTENT_URI, colonnes, echus, arguments, null)) {
            while (c != null && c.moveToNext()) {
                Evenement e = new Evenement(c.getLong(1), c.getString(2), c.getLong(3), c.getLong(4), c.getInt(5) != 0, 0, c.getString(6));
                String horaire = Mois.horaire(e, ZoneId.systemDefault());
                Notification rappel = new Notification.Builder(contexte, CANAL)
                        .setSmallIcon(R.drawable.ic_notif)
                        .setContentTitle(e.titre)
                        .setContentText(e.lieu.isEmpty() ? horaire : horaire + " · " + e.lieu)
                        .setWhen(e.debut)
                        .setShowWhen(true)
                        .setCategory(Notification.CATEGORY_EVENT)
                        .setContentIntent(ouvrir)
                        .setAutoCancel(true)
                        .build();
                notifications.notify((int) e.id, rappel);
            }
        } catch (RuntimeException agendaIndisponible) {
            return;
        }
        if (contexte.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            // Marqués comme montrés, pour ne pas les réafficher au prochain réveil.
            ContentValues montre = new ContentValues();
            montre.put(CalendarAlerts.STATE, CalendarAlerts.STATE_FIRED);
            contexte.getContentResolver().update(CalendarAlerts.CONTENT_URI, montre, echus, arguments);
        }
    }
}
