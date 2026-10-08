package fr.manboyx.contacts;

import fr.manboyx.contacts.logique.Detail;
import fr.manboyx.contacts.logique.Fiche;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Un carnet d'adresses en mémoire, pour piloter les écrans sans téléphone. */
final class CarnetMemoire implements Depot {
    final Map<Long, Detail> contacts = new LinkedHashMap<>();
    private long prochain = 1;

    long ajouter(String nom, boolean favori, String[] telephones, String[] emails) {
        return enregistrer(new Detail(0, nom, favori, Arrays.asList(telephones), Arrays.asList(emails)));
    }

    @Override
    public List<Fiche> lister() {
        List<Fiche> fiches = new ArrayList<>();
        for (Detail d : contacts.values()) {
            fiches.add(new Fiche(d.id, d.nom, d.favori));
        }
        return fiches;
    }

    @Override
    public Detail lire(long id) {
        return contacts.get(id);
    }

    @Override
    public long enregistrer(Detail d) {
        long id = d.id == 0 ? prochain++ : d.id;
        contacts.put(id, new Detail(id, d.nom, d.favori, d.telephones, d.emails));
        return id;
    }

    @Override
    public void supprimer(long id) {
        contacts.remove(id);
    }

    @Override
    public void marquerFavori(long id, boolean favori) {
        Detail d = contacts.get(id);
        contacts.put(id, new Detail(id, d.nom, favori, d.telephones, d.emails));
    }
}
