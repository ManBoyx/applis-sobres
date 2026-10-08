package fr.manboyx.commun;

/** Numéros de téléphone : les écrire de la même façon et reconnaître deux écritures du même numéro. */
public final class Numeros {
    private Numeros() {}

    /** Nombre de chiffres de fin comparés : assez pour ignorer « 0 », « +33 » ou « 0033 » en tête. */
    private static final int FIN = 9;
    /** En dessous, c'est un numéro court (15, 3615…) : pas de comparaison approchée. */
    private static final int COURT = 7;

    /** Le numéro réduit à ses chiffres, avec son « + » de tête s'il en a un. */
    public static String chiffres(String numero) {
        if (numero == null) {
            return "";
        }
        StringBuilder sortie = new StringBuilder(numero.length());
        for (int i = 0; i < numero.length(); i++) {
            char c = numero.charAt(i);
            if (c >= '0' && c <= '9' || c == '+' && sortie.length() == 0) {
                sortie.append(c);
            }
        }
        return sortie.toString();
    }

    /**
     * Vrai si les deux écritures désignent le même numéro : « 06 12 34 56 78 »
     * et « +33612345678 », par exemple. Les numéros courts doivent être identiques.
     */
    public static boolean memes(String a, String b) {
        String cleA = cle(a);
        return !cleA.isEmpty() && cleA.equals(cle(b));
    }

    /** Une clé identique pour toutes les écritures d'un même numéro : sert à regrouper. */
    public static String cle(String numero) {
        if (numero == null) {
            return "";
        }
        String chiffres = chiffres(numero).replace("+", "");
        if (chiffres.isEmpty() || contientDesLettres(numero)) {
            // Expéditeur en toutes lettres (« MaBanque ») : son nom est sa clé.
            return numero.trim();
        }
        if (chiffres.length() < COURT) {
            return chiffres;
        }
        return chiffres.substring(Math.max(0, chiffres.length() - FIN));
    }

    private static boolean contientDesLettres(String texte) {
        for (int i = 0; i < texte.length(); i++) {
            if (Character.isLetter(texte.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
