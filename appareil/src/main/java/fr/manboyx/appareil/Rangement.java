package fr.manboyx.appareil;

import android.net.Uri;

import java.io.IOException;

/** Là où vont les photos prises. */
public interface Rangement {
    /** Enregistre la photo parmi celles du téléphone et rend son adresse. */
    Uri enregistrer(byte[] jpeg, String nom) throws IOException;
}
