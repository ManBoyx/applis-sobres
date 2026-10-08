package fr.manboyx.messages;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;

import fr.manboyx.commun.Correspondant;
import fr.manboyx.messages.logique.Conversations;
import fr.manboyx.messages.logique.Message;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class DepotAndroid implements Depot {
    private static final String[] COLONNES = {Sms._ID, Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY, Sms.DATE, Sms.TYPE, Sms.READ};

    private final Context contexte;

    public DepotAndroid(Context contexte) {
        this.contexte = contexte.getApplicationContext();
    }

    /** `null` pour un brouillon : il n'a pas à apparaître dans les conversations. */
    private static Message lire(Cursor c) {
        int type = c.getInt(5);
        if (type == Sms.MESSAGE_TYPE_DRAFT) {
            return null;
        }
        return new Message(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getLong(4),
                type == Sms.MESSAGE_TYPE_INBOX, c.getInt(6) != 0);
    }

    @Override
    public List<Conversations.Resume> conversations() {
        // Les messages passent un par un dans le résumé : rien n'est gardé en mémoire.
        Conversations.Cumul cumul = new Conversations.Cumul();
        try (Cursor c = contexte.getContentResolver().query(Sms.CONTENT_URI, COLONNES, null, null, null)) {
            while (c != null && c.moveToNext()) {
                Message message = lire(c);
                if (message != null) {
                    cumul.ajouter(message);
                }
            }
        }
        return cumul.resumes();
    }

    @Override
    public List<Message> fil(long fil, int maximum) {
        ArrayDeque<Message> derniers = new ArrayDeque<>();
        try (Cursor c = contexte.getContentResolver().query(Sms.CONTENT_URI, COLONNES,
                Sms.THREAD_ID + "=?", new String[] {String.valueOf(fil)}, Sms.DATE + " ASC")) {
            while (c != null && c.moveToNext()) {
                Message message = lire(c);
                if (message == null) {
                    continue;
                }
                derniers.addLast(message);
                if (derniers.size() > maximum) {
                    derniers.removeFirst();
                }
            }
        }
        return new ArrayList<>(derniers);
    }

    @Override
    public void envoyer(String adresse, String texte) {
        SmsManager sms = Build.VERSION.SDK_INT >= 31 ? contexte.getSystemService(SmsManager.class) : ancienGestionnaire();
        if (sms == null) {
            throw new IllegalStateException("Ce téléphone ne sait pas envoyer de SMS");
        }
        // Un texte long part en plusieurs SMS que le téléphone du destinataire recolle.
        // Le téléphone range lui-même le message envoyé dans la conversation.
        ArrayList<String> parties = sms.divideMessage(texte);
        sms.sendMultipartTextMessage(adresse, null, parties, null, null);
    }

    @SuppressWarnings("deprecation")
    private static SmsManager ancienGestionnaire() {
        return SmsManager.getDefault();
    }

    @Override
    public String nom(String adresse) {
        if (adresse == null || adresse.isEmpty()
                || contexte.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        Uri recherche = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(adresse));
        try (Cursor c = contexte.getContentResolver().query(recherche, new String[] {PhoneLookup.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null) {
                return c.getString(0);
            }
        } catch (RuntimeException adresseInhabituelle) {
            // Expéditeur en toutes lettres ou numéro que le carnet ne sait pas chercher.
        }
        return "";
    }

    @Override
    public List<Correspondant> carnet() {
        List<Correspondant> carnet = new ArrayList<>();
        if (contexte.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return carnet;
        }
        String[] colonnes = {Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.STARRED};
        try (Cursor c = contexte.getContentResolver().query(Phone.CONTENT_URI, colonnes, null, null, null)) {
            while (c != null && c.moveToNext()) {
                if (c.getString(1) != null && !c.getString(1).isEmpty()) {
                    carnet.add(new Correspondant(c.getString(0), c.getString(1), c.getInt(2) != 0));
                }
            }
        }
        return carnet;
    }
}
