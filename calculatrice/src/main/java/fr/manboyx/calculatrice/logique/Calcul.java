package fr.manboyx.calculatrice.logique;

import java.math.BigDecimal;
import java.math.MathContext;

/** Calcule une expression écrite comme sur l'écran : « 12,5 × (3 + 4) − 10 % ». */
public final class Calcul {
    private Calcul() {}

    /** L'expression ne peut pas être calculée ; le message dit pourquoi, en français. */
    public static final class Erreur extends Exception {
        public Erreur(String message) {
            super(message);
        }
    }

    private static final String INCOMPLETE = "Expression incomplète";
    /** Précision des calculs, bien au-delà de ce qui est affiché. */
    private static final MathContext CALCUL = new MathContext(30);
    /** Nombre de chiffres affichés. */
    private static final MathContext AFFICHAGE = new MathContext(12);
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    public static BigDecimal evaluer(String expression) throws Erreur {
        Lecteur lecteur = new Lecteur(expression == null ? "" : expression);
        BigDecimal valeur = lecteur.somme();
        if (!lecteur.fini()) {
            throw new Erreur(INCOMPLETE);
        }
        return valeur;
    }

    /** Lit l'expression de gauche à droite en respectant les priorités. */
    private static final class Lecteur {
        private final String texte;
        private int position;
        /** Vrai si le dernier terme lu était un simple pourcentage (« 10 % »). */
        private boolean pourcentSeul;

        Lecteur(String texte) {
            this.texte = texte;
        }

        private char suivant() {
            while (position < texte.length() && Character.isWhitespace(texte.charAt(position))) {
                position++;
            }
            return position < texte.length() ? texte.charAt(position) : '\0';
        }

        boolean fini() {
            return suivant() == '\0';
        }

        /** Additions et soustractions. */
        BigDecimal somme() throws Erreur {
            BigDecimal total = produit();
            while (true) {
                char signe = suivant();
                boolean plus = signe == '+';
                boolean moins = signe == '−' || signe == '-';
                if (!plus && !moins) {
                    return total;
                }
                position++;
                BigDecimal terme = produit();
                // « 200 + 10 % » : le pourcentage porte sur ce qui précède.
                if (pourcentSeul) {
                    terme = total.multiply(terme, CALCUL);
                }
                total = plus ? total.add(terme, CALCUL) : total.subtract(terme, CALCUL);
            }
        }

        /** Multiplications et divisions. */
        private BigDecimal produit() throws Erreur {
            BigDecimal total = facteur();
            boolean seul = pourcentSeul;
            while (true) {
                char signe = suivant();
                boolean fois = signe == '×' || signe == '*';
                boolean divise = signe == '÷' || signe == '/';
                if (!fois && !divise) {
                    pourcentSeul = seul;
                    return total;
                }
                position++;
                seul = false;
                BigDecimal facteur = facteur();
                if (fois) {
                    total = total.multiply(facteur, CALCUL);
                } else if (facteur.signum() == 0) {
                    throw new Erreur("Division par zéro");
                } else {
                    total = total.divide(facteur, CALCUL);
                }
            }
        }

        /** Un nombre ou une parenthèse, éventuellement précédé d'un moins et suivi de « % ». */
        private BigDecimal facteur() throws Erreur {
            pourcentSeul = false;
            char debut = suivant();
            if (debut == '−' || debut == '-') {
                position++;
                BigDecimal oppose = facteur().negate();
                return oppose;
            }
            BigDecimal valeur;
            if (debut == '(') {
                position++;
                valeur = somme();
                if (suivant() != ')') {
                    throw new Erreur(INCOMPLETE);
                }
                position++;
                pourcentSeul = false;
            } else {
                valeur = nombre();
            }
            if (suivant() == '%') {
                position++;
                pourcentSeul = true;
                return valeur.divide(CENT, CALCUL);
            }
            return valeur;
        }

        private static boolean chiffre(char c) {
            return c >= '0' && c <= '9';
        }

        private BigDecimal nombre() throws Erreur {
            suivant();
            StringBuilder chiffres = new StringBuilder();
            boolean virgule = false;
            while (position < texte.length()) {
                char c = texte.charAt(position);
                if (chiffre(c)) {
                    chiffres.append(c);
                } else if ((c == ',' || c == '.') && !virgule) {
                    virgule = true;
                    chiffres.append('.');
                } else if (c == ' ' && !virgule && chiffres.length() > 0 && groupeDeMille(position + 1)) {
                    // « 1 000 » : l'espace sépare les milliers, il ne sépare pas deux nombres.
                } else {
                    break;
                }
                position++;
            }
            if (chiffres.length() == 0 || chiffres.toString().equals(".")) {
                throw new Erreur(INCOMPLETE);
            }
            return new BigDecimal(chiffres.toString());
        }

        /** Vrai si, à partir de `debut`, on trouve exactement trois chiffres. */
        private boolean groupeDeMille(int debut) {
            for (int i = debut; i < debut + 3; i++) {
                if (i >= texte.length() || !chiffre(texte.charAt(i))) {
                    return false;
                }
            }
            return debut + 3 >= texte.length() || !chiffre(texte.charAt(debut + 3));
        }
    }

    /** Écrit un nombre à la française : virgule, espaces entre les milliers, pas de zéros inutiles. */
    public static String ecrire(BigDecimal nombre) {
        BigDecimal arrondi = nombre.round(AFFICHAGE).stripTrailingZeros();
        if (arrondi.signum() == 0) {
            return "0";
        }
        String signe = arrondi.signum() < 0 ? "−" : "";
        BigDecimal absolu = arrondi.abs();
        int exposant = absolu.precision() - absolu.scale() - 1;
        if (exposant >= 15 || exposant < -9) {
            // Trop grand ou trop petit pour être lisible en entier.
            String chiffres = absolu.unscaledValue().toString();
            String mantisse = chiffres.length() == 1 ? chiffres : chiffres.charAt(0) + "," + chiffres.substring(1);
            return signe + mantisse + "E" + exposant;
        }
        String ecrit = absolu.toPlainString();
        int point = ecrit.indexOf('.');
        String entier = point < 0 ? ecrit : ecrit.substring(0, point);
        String decimales = point < 0 ? "" : "," + ecrit.substring(point + 1);
        return signe + grouper(entier) + decimales;
    }

    /** « 1234567 » devient « 1 234 567 ». */
    static String grouper(String entier) {
        StringBuilder sortie = new StringBuilder();
        for (int i = 0; i < entier.length(); i++) {
            if (i > 0 && (entier.length() - i) % 3 == 0) {
                sortie.append(' ');
            }
            sortie.append(entier.charAt(i));
        }
        return sortie.toString();
    }
}
