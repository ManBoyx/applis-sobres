package fr.manboyx.messages.logique;

/** Compte combien de SMS il faudra pour envoyer un texte. */
public final class Sms {
    private Sms() {}

    public static final class Compte {
        /** Nombre de SMS facturés. */
        public final int messages;
        /** Caractères encore disponibles avant le SMS suivant. */
        public final int restants;

        Compte(int messages, int restants) {
            this.messages = messages;
            this.restants = restants;
        }
    }

    /** L'alphabet de base des SMS : chacun de ces caractères compte pour un. */
    private static final String SIMPLES = "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
            + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà";
    /** Ceux-ci passent aussi, mais comptent double. */
    private static final String DOUBLES = "^{}\\[~]|€\f";

    public static Compte compter(String texte) {
        if (texte == null || texte.isEmpty()) {
            return new Compte(0, 160);
        }
        int unites = 0;
        boolean simple = true;
        for (int i = 0; i < texte.length() && simple; i++) {
            char c = texte.charAt(i);
            if (SIMPLES.indexOf(c) >= 0) {
                unites++;
            } else if (DOUBLES.indexOf(c) >= 0) {
                unites += 2;
            } else {
                simple = false;
            }
        }
        // Un seul caractère hors alphabet et tout le message change de codage : 70 par SMS au lieu de 160.
        int seul = simple ? 160 : 70;
        int parPartie = simple ? 153 : 67;
        if (!simple) {
            unites = texte.length();
        }
        if (unites <= seul) {
            return new Compte(1, seul - unites);
        }
        int messages = (unites + parPartie - 1) / parPartie;
        return new Compte(messages, messages * parPartie - unites);
    }
}
