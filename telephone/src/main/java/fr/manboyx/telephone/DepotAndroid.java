package fr.manboyx.telephone;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import fr.manboyx.telephone.logique.Appel;
import fr.manboyx.commun.Correspondant;

import java.util.ArrayList;
import java.util.List;

public final class DepotAndroid implements Depot {
    private final ContentResolver base;

    public DepotAndroid(ContentResolver base) {
        this.base = base;
    }

    @Override
    public List<Appel> journal(int maximum) {
        List<Appel> appels = new ArrayList<>();
        Uri journal = Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter(Calls.LIMIT_PARAM_KEY, String.valueOf(maximum))
                .build();
        String[] colonnes = {Calls.NUMBER, Calls.CACHED_NAME, Calls.TYPE, Calls.DATE, Calls.DURATION};
        try (Cursor c = base.query(journal, colonnes, null, null, Calls.DATE + " DESC")) {
            while (c != null && c.moveToNext()) {
                appels.add(new Appel(c.getString(0), c.getString(1), sens(c.getInt(2)), c.getLong(3), c.getLong(4)));
            }
        }
        return appels;
    }

    /** Android distingue aussi rejeté, bloqué, messagerie : pour l'utilisateur, ce sont des appels non pris. */
    static int sens(int type) {
        switch (type) {
            case Calls.OUTGOING_TYPE:
                return Appel.SORTANT;
            case Calls.INCOMING_TYPE:
            case Calls.ANSWERED_EXTERNALLY_TYPE:
                return Appel.ENTRANT;
            default:
                return Appel.MANQUE;
        }
    }

    @Override
    public List<Correspondant> carnet() {
        List<Correspondant> carnet = new ArrayList<>();
        String[] colonnes = {Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.STARRED};
        try (Cursor c = base.query(Phone.CONTENT_URI, colonnes, null, null, null)) {
            while (c != null && c.moveToNext()) {
                if (c.getString(1) != null && !c.getString(1).isEmpty()) {
                    carnet.add(new Correspondant(c.getString(0), c.getString(1), c.getInt(2) != 0));
                }
            }
        }
        return carnet;
    }
}
