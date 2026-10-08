package fr.manboyx.calculatrice.logique;

import java.math.BigDecimal;

/** Ce que l'on tape sur la calculatrice : tient l'expression propre touche après touche. */
public final class Saisie {
    public static final String EFFACER = "⌫";
    public static final String VIDER = "C";
    public static final String EGAL = "=";
    public static final String PARENTHESES = "( )";

    private static final String OPERATEURS = "+−×÷";

    /** L'expression sans aucun espace : chiffres, virgules, opérateurs, parenthèses, « % ». */
    private final StringBuilder brut = new StringBuilder();
    private boolean resultat;

    private char dernier() {
        return brut.length() == 0 ? '\0' : brut.charAt(brut.length() - 1);
    }

    private static boolean chiffre(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean operateur(char c) {
        return c != '\0' && OPERATEURS.indexOf(c) >= 0;
    }

    /** Vrai si le signe à cette place est un moins « de signe » (−5) et non une soustraction. */
    private boolean unaire(int place) {
        if (brut.charAt(place) != '−') {
            return false;
        }
        return place == 0 || brut.charAt(place - 1) == '(' || operateur(brut.charAt(place - 1));
    }

    /** L'expression en cours, telle qu'affichée. */
    public String expression() {
        return lisible(brut);
    }

    private String lisible(CharSequence source) {
        StringBuilder sortie = new StringBuilder();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (chiffre(c) || c == ',') {
                int fin = i;
                while (fin < source.length() && chiffre(source.charAt(fin))) {
                    fin++;
                }
                sortie.append(Calcul.grouper(source.subSequence(i, fin).toString()));
                while (fin < source.length() && (chiffre(source.charAt(fin)) || source.charAt(fin) == ',')) {
                    sortie.append(source.charAt(fin++));
                }
                i = fin;
                continue;
            }
            boolean moinsDeSigne = c == '−' && (i == 0 || source.charAt(i - 1) == '(' || operateur(source.charAt(i - 1)));
            if (operateur(c) && !moinsDeSigne) {
                sortie.append(' ').append(c).append(' ');
            } else if (c == '%') {
                sortie.append(" %");
            } else {
                sortie.append(c);
            }
            i++;
        }
        return sortie.toString();
    }

    /** Vrai juste après « = » : l'expression affichée est un résultat. */
    public boolean resultat() {
        return resultat;
    }

    private int ouvertes() {
        int ouvertes = 0;
        for (int i = 0; i < brut.length(); i++) {
            ouvertes += brut.charAt(i) == '(' ? 1 : brut.charAt(i) == ')' ? -1 : 0;
        }
        return ouvertes;
    }

    /**
     * Prend en compte une touche. Rend la ligne à ajouter à l'historique
     * (« 2 + 2 = 4 ») quand « = » vient de donner un résultat, sinon `null`.
     */
    public String taper(String touche) throws Calcul.Erreur {
        if (touche.equals(EGAL)) {
            return egal();
        }
        boolean suite = resultat;
        resultat = false;
        char d = dernier();
        if (touche.equals(VIDER)) {
            brut.setLength(0);
        } else if (touche.equals(EFFACER)) {
            if (brut.length() > 0) {
                brut.setLength(brut.length() - 1);
            }
        } else if (touche.equals(PARENTHESES)) {
            if (d == ',') {
                brut.setLength(brut.length() - 1);
                d = dernier();
            }
            if (d == '\0' || d == '(' || operateur(d)) {
                brut.append('(');
            } else if (ouvertes() > 0) {
                brut.append(')');
            } else {
                brut.append("×(");
            }
        } else if (touche.length() == 1 && chiffre(touche.charAt(0))) {
            if (suite) {
                // Après un résultat, un chiffre commence un nouveau calcul.
                brut.setLength(0);
            } else if (d == ')' || d == '%') {
                brut.append('×');
            }
            brut.append(touche);
        } else if (touche.equals(",")) {
            if (suite) {
                brut.setLength(0);
                d = '\0';
            }
            if (d == ')' || d == '%') {
                brut.append('×');
                d = '×';
            }
            int debut = brut.length();
            while (debut > 0 && (chiffre(brut.charAt(debut - 1)) || brut.charAt(debut - 1) == ',')) {
                debut--;
            }
            String nombre = brut.substring(debut);
            if (nombre.isEmpty()) {
                brut.append("0,");
            } else if (nombre.indexOf(',') < 0) {
                brut.append(',');
            }
        } else if (touche.equals("%")) {
            if (chiffre(d) || d == ')') {
                brut.append('%');
            }
        } else if (touche.length() == 1 && operateur(touche.charAt(0))) {
            char signe = touche.charAt(0);
            if (d == ',') {
                brut.setLength(brut.length() - 1);
                d = dernier();
            }
            if (d == '\0' || d == '(') {
                // En tête, seul le moins a un sens : « −5 ».
                if (signe == '−') {
                    brut.append(signe);
                }
            } else if (operateur(d)) {
                if (!unaire(brut.length() - 1)) {
                    brut.setCharAt(brut.length() - 1, signe);
                }
            } else {
                brut.append(signe);
            }
        }
        return null;
    }

    /** L'expression prête à calculer : sans opérateur ni parenthèse ouvrante en suspens, parenthèses refermées. */
    private static String complete(CharSequence source, boolean tolerante) {
        StringBuilder texte = new StringBuilder(source);
        if (tolerante) {
            while (texte.length() > 0) {
                char fin = texte.charAt(texte.length() - 1);
                if (operateur(fin) || fin == '(' || fin == ',') {
                    texte.setLength(texte.length() - 1);
                } else {
                    break;
                }
            }
        }
        int ouvertes = 0;
        for (int i = 0; i < texte.length(); i++) {
            ouvertes += texte.charAt(i) == '(' ? 1 : texte.charAt(i) == ')' ? -1 : 0;
        }
        for (; ouvertes > 0; ouvertes--) {
            texte.append(')');
        }
        return texte.toString();
    }

    /** Vrai s'il n'y a qu'un nombre : rien à calculer. */
    private static boolean nombreSeul(String texte) {
        return texte.matches("−?[0-9,]*");
    }

    private String egal() throws Calcul.Erreur {
        String aCalculer = complete(brut, false);
        if (resultat || nombreSeul(aCalculer)) {
            return null;
        }
        BigDecimal valeur = Calcul.evaluer(aCalculer);
        String ecrit = Calcul.ecrire(valeur);
        String ligne = lisible(aCalculer) + " = " + ecrit;
        reprendre(ecrit);
        resultat = true;
        return ligne;
    }

    /** Le résultat de ce qui est déjà tapé, pour l'afficher en avance ; vide s'il n'y a rien d'utile à montrer. */
    public String apercu() {
        String aCalculer = complete(brut, true);
        if (resultat || nombreSeul(aCalculer)) {
            return "";
        }
        try {
            return Calcul.ecrire(Calcul.evaluer(aCalculer));
        } catch (Calcul.Erreur incomplet) {
            return "";
        }
    }

    /** Repart d'un nombre (un résultat de l'historique). */
    public void reprendre(String nombre) {
        brut.setLength(0);
        String compact = nombre.replace(" ", "");
        if (compact.indexOf('E') >= 0) {
            // Un nombre écrit en puissance de dix est repris en entier, pour pouvoir continuer à calculer.
            try {
                compact = new BigDecimal(compact.replace('−', '-').replace(',', '.')).toPlainString().replace('-', '−').replace('.', ',');
            } catch (NumberFormatException illisible) {
                compact = "";
            }
        }
        brut.append(compact);
        resultat = false;
    }
}
