package fr.manboyx.commun;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cherche dans le carnet ce qui correspond à ce que l'on tape, nom ou chiffres. */
public final class Recherche {
    private Recherche() {}

    /**
     * Les correspondants dont le nom contient la saisie, ou dont le numéro
     * contient les chiffres tapés. Favoris d'abord, puis par nom. Une saisie
     * vide rend tout le carnet. `maximum` à 0 : pas de limite.
     */
    public static List<Correspondant> filtrer(List<Correspondant> carnet, String saisie, int maximum) {
        String chiffres = Numeros.chiffres(saisie).replace("+", "");
        // « 0700 » doit trouver « +33 7 00… » : on compare aussi sans le zéro de tête.
        String sansZero = chiffres.startsWith("0") ? chiffres.substring(1) : chiffres;
        boolean parNumero = !chiffres.isEmpty() && Textes.cle(saisie).replaceAll("[0-9 .+-]", "").isEmpty();

        List<Correspondant> trouves = new ArrayList<>();
        for (Correspondant c : carnet) {
            boolean retenu;
            if (parNumero) {
                String numero = Numeros.chiffres(c.numero).replace("+", "");
                retenu = numero.contains(chiffres) || (sansZero.length() >= 3 && numero.contains(sansZero));
            } else {
                retenu = Textes.contient(c.nom, saisie);
            }
            if (retenu) {
                trouves.add(c);
            }
        }
        trouves.sort(Comparator.comparing((Correspondant c) -> !c.favori)
                .thenComparing(c -> Textes.cle(c.nom))
                .thenComparing(c -> c.numero));
        if (maximum > 0 && trouves.size() > maximum) {
            return new ArrayList<>(trouves.subList(0, maximum));
        }
        return trouves;
    }
}
