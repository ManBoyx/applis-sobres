package fr.manboyx.commun;


/** Écrit un numéro de façon lisible. */
public final class Affichage {
    private Affichage() {}

    /** « 0612345678 » devient « 06 12 34 56 78 », « +33612345678 » devient « +33 6 12 34 56 78 ». Le reste est laissé tel quel. */
    public static String numero(String numero) {
        if (numero == null) {
            return "";
        }
        String chiffres = Numeros.chiffres(numero);
        // Autre chose que des chiffres et de la ponctuation (« *123# ») : on n'y touche pas.
        if (!numero.replaceAll("[0-9 .+-]", "").isEmpty()) {
            return numero;
        }
        if (chiffres.length() == 10 && chiffres.startsWith("0")) {
            return paires(chiffres, 0);
        }
        if (chiffres.length() == 12 && chiffres.startsWith("+33")) {
            return "+33 " + chiffres.charAt(3) + " " + paires(chiffres, 4);
        }
        return chiffres;
    }

    private static String paires(String chiffres, int debut) {
        StringBuilder sortie = new StringBuilder();
        for (int i = debut; i < chiffres.length(); i += 2) {
            if (sortie.length() > 0) {
                sortie.append(' ');
            }
            sortie.append(chiffres, i, Math.min(i + 2, chiffres.length()));
        }
        return sortie.toString();
    }
}
