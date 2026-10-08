package fr.manboyx.essais;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Tient la place d'une base du téléphone (contacts, SMS, journal d'appels…) :
 * répond aux lectures ce que le test lui a dit, et note tout ce qu'on lui demande d'écrire.
 */
public class FauxFournisseur extends ContentProvider {
    /** Ce que répond une lecture. */
    public interface Lecture {
        Cursor repondre(Uri uri, String[] colonnes, String condition, String[] arguments);
    }

    /** Tout ce qui a été demandé, dans l'ordre : « insert content://… {colonne=valeur, …} ». */
    public final List<String> journal = new ArrayList<>();
    public Lecture lecture = (uri, colonnes, condition, arguments) -> new MatrixCursor(colonnes == null ? new String[0] : colonnes);
    private long prochainId = 100;

    /** Met ce faux fournisseur à la place de la base désignée par `autorite`. */
    public static FauxFournisseur installer(String autorite) {
        return Robolectric.buildContentProvider(FauxFournisseur.class).create(autorite).get();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] colonnes, String condition, String[] arguments, String tri) {
        return lecture.repondre(uri, colonnes, condition, arguments);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues valeurs) {
        journal.add("insert " + uri + " " + lisible(valeurs));
        return ContentUris.withAppendedId(uri, prochainId++);
    }

    @Override
    public int update(Uri uri, ContentValues valeurs, String condition, String[] arguments) {
        journal.add("update " + uri + " " + lisible(valeurs) + suite(condition, arguments));
        return 1;
    }

    @Override
    public int delete(Uri uri, String condition, String[] arguments) {
        journal.add("delete " + uri + suite(condition, arguments));
        return 1;
    }

    private static String suite(String condition, String[] arguments) {
        if (condition == null) {
            return "";
        }
        return " si " + condition + (arguments == null ? "" : " " + java.util.Arrays.toString(arguments));
    }

    /** Les valeurs triées par nom de colonne, pour que les tests ne dépendent pas d'un ordre. */
    private static String lisible(ContentValues valeurs) {
        TreeMap<String, Object> triees = new TreeMap<>();
        if (valeurs != null) {
            for (String cle : valeurs.keySet()) {
                triees.put(cle, valeurs.get(cle));
            }
        }
        return triees.toString();
    }
}
