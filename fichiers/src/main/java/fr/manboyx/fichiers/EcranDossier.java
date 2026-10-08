package fr.manboyx.fichiers;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import fr.manboyx.commun.Autorisations;
import fr.manboyx.commun.Barre;
import fr.manboyx.commun.Dates;
import fr.manboyx.commun.Dialogues;
import fr.manboyx.commun.Ecran;
import fr.manboyx.commun.Fond;
import fr.manboyx.commun.Ui;
import fr.manboyx.fichiers.logique.Dossier;
import fr.manboyx.fichiers.logique.Entree;
import fr.manboyx.fichiers.logique.Operations;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Le contenu d'un dossier. */
final class EcranDossier extends Ecran {
    private static final int DEMANDE = 1;

    /** Une opération qui peut échouer, avec le message à montrer dans ce cas. */
    private interface Action {
        void faire() throws IOException;
    }

    private final File dossier;
    private List<Entree> entrees = new ArrayList<>();
    private final Lignes adaptateur = new Lignes();
    private ListView liste;
    private TextView vide;
    private View explication;
    private LinearLayout bandeau;
    private TextView texteBandeau;

    EcranDossier(File dossier) {
        this.dossier = dossier;
    }

    private FichiersActivity appli() {
        return (FichiersActivity) activite;
    }

    @Override
    protected View creer() {
        boolean racine = dossier.equals(appli().racine());
        LinearLayout colonne = Ui.colonne(activite);
        TextView nouveau = Ui.touche(activite, "+", "Nouveau dossier");
        nouveau.setOnClickListener(v -> Dialogues.saisir(activite, "Nouveau dossier", "", "Créer",
                nom -> faire(() -> Operations.creerDossier(dossier, nom))));
        TextView options = Ui.touche(activite, "Tri", "Options d'affichage");
        options.setTextSize(15);
        options.setTextColor(Ui.couleur(activite, fr.manboyx.commun.R.color.accent));
        options.setOnClickListener(v -> options());
        colonne.addView(Barre.creer(activite, racine ? "Fichiers" : dossier.getName(), !racine, nouveau, options));

        if (!racine) {
            String chemin = dossier.getAbsolutePath().substring(appli().racine().getAbsolutePath().length());
            TextView ou = Ui.uneLigne(Ui.discret(activite, "Mémoire du téléphone" + chemin.replace("/", " › ")));
            ou.setEllipsize(android.text.TextUtils.TruncateAt.START);
            Ui.marges(ou, activite, 16, 0, 16, 8);
            colonne.addView(ou);
        }

        FrameLayout cadre = new FrameLayout(activite);
        liste = new ListView(activite);
        liste.setDivider(null);
        liste.setAdapter(adaptateur);
        liste.setOnItemClickListener((parent, vue, rang, id) -> ouvrir(entrees.get(rang)));
        liste.setOnItemLongClickListener((parent, vue, rang, id) -> {
            menu(entrees.get(rang));
            return true;
        });
        cadre.addView(liste, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        vide = Ui.vide(activite, "Ce dossier est vide.");
        vide.setVisibility(View.GONE);
        cadre.addView(vide, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
        explication = explication();
        cadre.addView(explication, new FrameLayout.LayoutParams(Ui.PLEIN, Ui.PLEIN));
        colonne.addView(cadre, Ui.reste(false));

        // Ce qui a été « copié » ou « coupé » attend ici d'être collé dans le dossier affiché.
        bandeau = Ui.rangee(activite);
        bandeau.setBackgroundColor(Ui.couleur(activite, fr.manboyx.commun.R.color.surface));
        Ui.marges(bandeau, activite, 16, 8, 8, 8);
        texteBandeau = Ui.uneLigne(Ui.texte(activite, ""));
        bandeau.addView(texteBandeau, Ui.reste(true));
        Button coller = Ui.bouton(activite, "Coller", true);
        coller.setOnClickListener(v -> coller());
        bandeau.addView(coller, Ui.place(Ui.JUSTE, Ui.JUSTE));
        TextView annuler = Ui.touche(activite, "✕", "Ne pas coller");
        annuler.setOnClickListener(v -> {
            appli().presse = null;
            montrerBandeau();
        });
        bandeau.addView(annuler);
        colonne.addView(bandeau);
        return colonne;
    }

    /** Parcourir toute la mémoire demande, depuis Android 11, un réglage que seul l'utilisateur peut activer. */
    private View explication() {
        LinearLayout colonne = Ui.colonne(activite);
        colonne.setGravity(Gravity.CENTER);
        Ui.marges(colonne, activite, 32, 32, 32, 32);
        colonne.addView(Ui.vide(activite,
                "Pour parcourir vos dossiers, l'appli a besoin de l'accès à tous les fichiers du téléphone. Elle n'envoie rien nulle part."));
        Button autoriser = Ui.bouton(activite, "Autoriser l'accès", true);
        autoriser.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < 30) {
                Autorisations.demander(activite, DEMANDE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
            Uri appli = Uri.fromParts("package", activite.getPackageName(), null);
            try {
                activite.startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, appli));
            } catch (ActivityNotFoundException sansEcranDedie) {
                try {
                    activite.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                } catch (ActivityNotFoundException aucun) {
                    Autorisations.ouvrirReglages(activite);
                }
            }
        });
        colonne.addView(autoriser, Ui.place(Ui.JUSTE, Ui.JUSTE));
        return colonne;
    }

    @Override
    public void afficher() {
        boolean autorise = appli().acces();
        explication.setVisibility(autorise ? View.GONE : View.VISIBLE);
        liste.setVisibility(autorise ? View.VISIBLE : View.GONE);
        montrerBandeau();
        if (!autorise) {
            vide.setVisibility(View.GONE);
            return;
        }
        if (!dossier.isDirectory()) {
            // Le dossier a été supprimé ou déplacé pendant qu'on regardait ailleurs.
            activite.fermer();
            return;
        }
        Dossier.Tri tri = appli().tri;
        boolean caches = appli().caches;
        Fond.lancer(() -> Dossier.lister(dossier, tri, caches), lues -> {
            entrees = lues;
            adaptateur.notifyDataSetChanged();
            vide.setVisibility(lues.isEmpty() && liste.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void autorisations(int code, boolean accordees) {
        afficher();
    }

    private void montrerBandeau() {
        FichiersActivity.Presse presse = appli().presse;
        bandeau.setVisibility(presse != null && appli().acces() ? View.VISIBLE : View.GONE);
        if (presse != null) {
            texteBandeau.setText((presse.deplacer ? "Déplacer « " : "Copier « ") + presse.fichier.getName() + " » ici");
        }
    }

    /** Fait l'opération hors de l'écran, dit ce qui n'a pas marché, puis relit le dossier. */
    private void faire(Action action) {
        Fond.lancer(() -> {
            try {
                action.faire();
                return "";
            } catch (IOException | RuntimeException echec) {
                return echec.getMessage() == null ? "L'opération a échoué." : echec.getMessage();
            }
        }, erreur -> {
            if (!erreur.isEmpty()) {
                Dialogues.dire(activite, erreur);
            }
            afficher();
        });
    }

    private void coller() {
        FichiersActivity.Presse presse = appli().presse;
        appli().presse = null;
        if (presse == null) {
            return;
        }
        faire(() -> {
            if (presse.deplacer) {
                Operations.deplacer(presse.fichier, dossier);
            } else {
                Operations.copier(presse.fichier, dossier);
            }
        });
    }

    private void options() {
        String[] choix = {"Trier par nom", "Trier par date", "Trier par taille",
                appli().caches ? "Masquer les fichiers cachés" : "Afficher les fichiers cachés"};
        Dialogues.choisir(activite, "Affichage", choix, rang -> {
            if (rang == 3) {
                appli().caches = !appli().caches;
            } else {
                appli().tri = Dossier.Tri.values()[rang];
            }
            afficher();
        });
    }

    private void ouvrir(Entree entree) {
        if (entree.dossier) {
            activite.ouvrir(new EcranDossier(entree.fichier));
            return;
        }
        Intent voir = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(FournisseurFichiers.adresse(entree.fichier), FournisseurFichiers.type(entree.nom))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activite.startActivity(voir);
        } catch (ActivityNotFoundException aucune) {
            Dialogues.dire(activite, "Aucune appli du téléphone ne sait ouvrir ce fichier.");
        }
    }

    private void partager(Entree entree) {
        Intent envoi = new Intent(Intent.ACTION_SEND)
                .setType(FournisseurFichiers.type(entree.nom))
                .putExtra(Intent.EXTRA_STREAM, FournisseurFichiers.adresse(entree.fichier))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activite.startActivity(Intent.createChooser(envoi, "Partager le fichier"));
        } catch (ActivityNotFoundException aucune) {
            Dialogues.dire(activite, "Aucune appli du téléphone ne peut recevoir ce fichier.");
        }
    }

    private void menu(Entree entree) {
        List<String> choix = new ArrayList<>();
        choix.add("Ouvrir");
        if (!entree.dossier) {
            choix.add("Partager");
        }
        choix.add("Renommer");
        choix.add("Copier");
        choix.add("Déplacer");
        choix.add("Supprimer");
        choix.add("Détails");
        Dialogues.choisir(activite, entree.nom, choix.toArray(new String[0]), rang -> {
            switch (choix.get(rang)) {
                case "Ouvrir":
                    ouvrir(entree);
                    break;
                case "Partager":
                    partager(entree);
                    break;
                case "Renommer":
                    Dialogues.saisir(activite, "Renommer", entree.nom, "Renommer",
                            nom -> faire(() -> Operations.renommer(entree.fichier, nom)));
                    break;
                case "Copier":
                case "Déplacer":
                    appli().presse = new FichiersActivity.Presse(entree.fichier, choix.get(rang).equals("Déplacer"));
                    montrerBandeau();
                    break;
                case "Supprimer":
                    supprimer(entree);
                    break;
                default:
                    details(entree);
            }
        });
    }

    private void supprimer(Entree entree) {
        String message;
        if (entree.dossier) {
            int contenu = Operations.compter(entree.fichier);
            message = contenu == 0 ? "Le dossier est vide."
                    : "Le dossier et les " + contenu + " éléments qu'il contient seront supprimés.";
        } else {
            message = "Le fichier sera supprimé.";
        }
        Dialogues.confirmer(activite, "Supprimer « " + entree.nom + " » ?", message + " Il n'y a pas de corbeille.",
                "Supprimer", () -> faire(() -> Operations.supprimer(entree.fichier)));
    }

    private void details(Entree entree) {
        String quand = Dates.jour(entree.modifie, System.currentTimeMillis(), ZoneId.systemDefault())
                + " à " + Dates.heure(entree.modifie, ZoneId.systemDefault());
        String texte = (entree.dossier
                ? "Dossier, " + Operations.compter(entree.fichier) + " éléments"
                : "Fichier, " + Dossier.taille(entree.taille))
                + "\nModifié : " + quand
                + "\n" + entree.fichier.getAbsolutePath();
        new android.app.AlertDialog.Builder(activite).setTitle(entree.nom).setMessage(texte).setPositiveButton("Fermer", null).show();
    }

    private final class Lignes extends BaseAdapter {
        @Override
        public int getCount() {
            return entrees.size();
        }

        @Override
        public Object getItem(int rang) {
            return entrees.get(rang);
        }

        @Override
        public long getItemId(int rang) {
            return rang;
        }

        @Override
        public View getView(int rang, View recyclee, ViewGroup parent) {
            LinearLayout rangee = (LinearLayout) recyclee;
            if (rangee == null) {
                rangee = Ui.rangee(activite);
                rangee.setMinimumHeight(Ui.dp(activite, 60));
                Ui.marges(rangee, activite, 16, 8, 16, 8);
                TextView pastille = Ui.gras(Ui.texte(activite, "", 11, fr.manboyx.commun.R.color.texte));
                pastille.setGravity(Gravity.CENTER);
                rangee.addView(pastille, Ui.place(Ui.dp(activite, 40), Ui.dp(activite, 40)));
                LinearLayout textes = Ui.colonne(activite);
                textes.setPadding(Ui.dp(activite, 14), 0, 0, 0);
                textes.addView(Ui.uneLigne(Ui.texte(activite, "")));
                textes.addView(Ui.uneLigne(Ui.discret(activite, "")));
                rangee.addView(textes, Ui.reste(true));
            }
            Entree entree = entrees.get(rang);
            TextView pastille = (TextView) rangee.getChildAt(0);
            // Un dossier : pastille pleine. Un fichier : son extension dans un cadre.
            String extension = Dossier.extension(entree.nom).toUpperCase(Locale.ROOT);
            pastille.setText(entree.dossier ? "" : extension.length() > 4 ? extension.substring(0, 4) : extension);
            pastille.setBackgroundResource(entree.dossier ? R.drawable.fond_dossier : R.drawable.fond_fichier);
            LinearLayout textes = (LinearLayout) rangee.getChildAt(1);
            ((TextView) textes.getChildAt(0)).setText(entree.nom);
            String date = Dates.relative(entree.modifie, System.currentTimeMillis(), ZoneId.systemDefault());
            ((TextView) textes.getChildAt(1)).setText(entree.dossier ? "Dossier · " + date : Dossier.taille(entree.taille) + " · " + date);
            return rangee;
        }
    }
}
