package fr.manboyx.contacts.logique;

import fr.manboyx.commun.Numeros;

import java.util.ArrayList;
import java.util.List;

/** Un contact en entier, tel qu'on le lit ou qu'on l'enregistre. `id` vaut 0 pour un nouveau contact. */
public final class Detail {
    public final long id;
    public final String nom;
    public final boolean favori;
    public final List<String> telephones;
    public final List<String> emails;

    public Detail(long id, String nom, boolean favori, List<String> telephones, List<String> emails) {
        this.id = id;
        this.nom = nom == null ? "" : nom;
        this.favori = favori;
        this.telephones = new ArrayList<>(telephones);
        this.emails = new ArrayList<>(emails);
    }

    /** Le même contact, débarrassé des lignes vides et des doublons, prêt à être enregistré. */
    public Detail nettoye() {
        List<String> numeros = new ArrayList<>();
        for (String brut : telephones) {
            String numero = brut == null ? "" : brut.trim();
            boolean connu = false;
            for (String garde : numeros) {
                connu |= Numeros.memes(garde, numero);
            }
            if (!numero.isEmpty() && !connu) {
                numeros.add(numero);
            }
        }
        List<String> adresses = new ArrayList<>();
        for (String brut : emails) {
            String adresse = brut == null ? "" : brut.trim();
            boolean connue = false;
            for (String gardee : adresses) {
                connue |= gardee.equalsIgnoreCase(adresse);
            }
            if (!adresse.isEmpty() && !connue) {
                adresses.add(adresse);
            }
        }
        String propre = nom.trim();
        if (propre.isEmpty() && !numeros.isEmpty()) {
            propre = numeros.get(0);
        }
        return new Detail(id, propre, favori, numeros, adresses);
    }

    /** Vrai s'il y a de quoi enregistrer : un nom ou au moins un numéro. */
    public boolean enregistrable() {
        return !nettoye().nom.isEmpty();
    }
}
