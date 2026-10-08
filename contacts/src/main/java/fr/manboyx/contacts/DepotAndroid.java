package fr.manboyx.contacts;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

import fr.manboyx.contacts.logique.Detail;
import fr.manboyx.contacts.logique.Fiche;

import java.util.ArrayList;
import java.util.List;

/** Le carnet d'adresses d'Android : les contacts créés ici sont de vrais contacts du téléphone. */
public final class DepotAndroid implements Depot {
    private final ContentResolver base;

    public DepotAndroid(ContentResolver base) {
        this.base = base;
    }

    @Override
    public List<Fiche> lister() {
        List<Fiche> fiches = new ArrayList<>();
        String[] colonnes = {Contacts._ID, Contacts.DISPLAY_NAME_PRIMARY, Contacts.STARRED};
        try (Cursor c = base.query(Contacts.CONTENT_URI, colonnes, null, null, null)) {
            while (c != null && c.moveToNext()) {
                fiches.add(new Fiche(c.getLong(0), c.getString(1), c.getInt(2) != 0));
            }
        }
        return fiches;
    }

    @Override
    public Detail lire(long id) {
        String nom;
        boolean favori;
        String[] colonnes = {Contacts.DISPLAY_NAME_PRIMARY, Contacts.STARRED};
        try (Cursor c = base.query(ContentUris.withAppendedId(Contacts.CONTENT_URI, id), colonnes, null, null, null)) {
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            nom = c.getString(0);
            favori = c.getInt(1) != 0;
        }
        List<String> telephones = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        String[] donnees = {Data.MIMETYPE, Data.DATA1};
        try (Cursor c = base.query(Data.CONTENT_URI, donnees, Data.CONTACT_ID + "=?", new String[] {String.valueOf(id)}, Data._ID)) {
            while (c != null && c.moveToNext()) {
                String valeur = c.getString(1);
                if (valeur == null || valeur.isEmpty()) {
                    continue;
                }
                if (Phone.CONTENT_ITEM_TYPE.equals(c.getString(0))) {
                    telephones.add(valeur);
                } else if (Email.CONTENT_ITEM_TYPE.equals(c.getString(0))) {
                    emails.add(valeur);
                }
            }
        }
        // Un même numéro peut venir de deux comptes réunis en un seul contact.
        return new Detail(id, nom, favori, telephones, emails).nettoye();
    }

    @Override
    public long enregistrer(Detail detail) {
        try {
            return detail.id == 0 ? creer(detail) : modifier(detail);
        } catch (android.os.RemoteException | android.content.OperationApplicationException e) {
            throw new IllegalStateException("Le contact n'a pas pu être enregistré", e);
        }
    }

    private long creer(Detail d) throws android.os.RemoteException, android.content.OperationApplicationException {
        ArrayList<ContentProviderOperation> etapes = new ArrayList<>();
        // Sans compte : le contact reste sur le téléphone, il ne part chez personne.
        etapes.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .build());
        etapes.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, d.nom)
                .build());
        for (String numero : d.telephones) {
            etapes.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, numero)
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .build());
        }
        for (String adresse : d.emails) {
            etapes.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.ADDRESS, adresse)
                    .withValue(Email.TYPE, Email.TYPE_HOME)
                    .build());
        }
        ContentProviderResult[] resultats = base.applyBatch(ContactsContract.AUTHORITY, etapes);
        long brut = ContentUris.parseId(resultats[0].uri);
        long id = 0;
        try (Cursor c = base.query(ContentUris.withAppendedId(RawContacts.CONTENT_URI, brut),
                new String[] {RawContacts.CONTACT_ID}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
            }
        }
        if (d.favori && id != 0) {
            marquerFavori(id, true);
        }
        return id;
    }

    private long modifier(Detail d) throws android.os.RemoteException, android.content.OperationApplicationException {
        // Un contact affiché peut réunir plusieurs fiches (une par compte) : on les traite toutes.
        List<Long> bruts = new ArrayList<>();
        try (Cursor c = base.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID},
                RawContacts.CONTACT_ID + "=?", new String[] {String.valueOf(d.id)}, RawContacts._ID)) {
            while (c != null && c.moveToNext()) {
                bruts.add(c.getLong(0));
            }
        }
        if (bruts.isEmpty()) {
            return d.id;
        }
        StringBuilder liste = new StringBuilder();
        for (long brut : bruts) {
            liste.append(liste.length() == 0 ? "" : ",").append(brut);
        }
        String desFiches = Data.RAW_CONTACT_ID + " IN (" + liste + ")";
        long premiere = bruts.get(0);

        boolean aUnNom = false;
        try (Cursor c = base.query(Data.CONTENT_URI, new String[] {Data._ID},
                desFiches + " AND " + Data.MIMETYPE + "=?", new String[] {StructuredName.CONTENT_ITEM_TYPE}, null)) {
            aUnNom = c != null && c.getCount() > 0;
        }

        ArrayList<ContentProviderOperation> etapes = new ArrayList<>();
        if (aUnNom) {
            etapes.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                    .withSelection(desFiches + " AND " + Data.MIMETYPE + "=?", new String[] {StructuredName.CONTENT_ITEM_TYPE})
                    .withValue(StructuredName.DISPLAY_NAME, d.nom)
                    .build());
        } else {
            etapes.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, premiere)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, d.nom)
                    .build());
        }
        // Numéros et adresses : on remplace l'ancienne liste par la nouvelle.
        etapes.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                .withSelection(desFiches + " AND " + Data.MIMETYPE + " IN (?,?)",
                        new String[] {Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE})
                .build());
        for (String numero : d.telephones) {
            etapes.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, premiere)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, numero)
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .build());
        }
        for (String adresse : d.emails) {
            etapes.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, premiere)
                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.ADDRESS, adresse)
                    .withValue(Email.TYPE, Email.TYPE_HOME)
                    .build());
        }
        base.applyBatch(ContactsContract.AUTHORITY, etapes);
        return d.id;
    }

    @Override
    public void supprimer(long id) {
        base.delete(ContentUris.withAppendedId(Contacts.CONTENT_URI, id), null, null);
    }

    @Override
    public void marquerFavori(long id, boolean favori) {
        ContentValues valeurs = new ContentValues();
        valeurs.put(Contacts.STARRED, favori ? 1 : 0);
        Uri contact = ContentUris.withAppendedId(Contacts.CONTENT_URI, id);
        base.update(contact, valeurs, null, null);
    }
}
