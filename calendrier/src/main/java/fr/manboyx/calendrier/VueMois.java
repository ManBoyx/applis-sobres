package fr.manboyx.calendrier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import fr.manboyx.calendrier.logique.Evenement;
import fr.manboyx.calendrier.logique.Mois;
import fr.manboyx.commun.Ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** La grille du mois, dessinée d'un bloc : 42 cases sans créer 42 éléments d'écran. */
final class VueMois extends View {
    interface Ecoute {
        void jour(LocalDate jour);

        /** +1 : mois suivant ; −1 : mois précédent. */
        void glissement(int sens);
    }

    /** Au-delà, un « + » remplace les points. */
    private static final int POINTS = 3;

    private final Paint texte = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint forme = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GestureDetector gestes;
    private List<LocalDate> cases = new ArrayList<>();
    private YearMonth mois = YearMonth.now();
    private LocalDate aujourdhui = LocalDate.now();
    private LocalDate choisi = LocalDate.now();
    private Map<LocalDate, List<Evenement>> jours = new HashMap<>();

    VueMois(Context contexte, Ecoute ecoute) {
        super(contexte);
        texte.setTextAlign(Paint.Align.CENTER);
        texte.setTextSize(Ui.dp(contexte, 16));
        gestes = new GestureDetector(contexte, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                LocalDate jour = jourEn(e.getX(), e.getY());
                if (jour != null) {
                    ecoute.jour(jour);
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent depart, MotionEvent e, float vx, float vy) {
                if (Math.abs(vx) > Math.abs(vy) * 1.5f && Math.abs(vx) > 600) {
                    ecoute.glissement(vx < 0 ? 1 : -1);
                }
                return true;
            }
        });
    }

    void montrer(YearMonth mois, LocalDate aujourdhui, LocalDate choisi, Map<LocalDate, List<Evenement>> jours) {
        this.mois = mois;
        this.cases = Mois.grille(mois);
        this.aujourdhui = aujourdhui;
        this.choisi = choisi;
        this.jours = jours;
        setContentDescription("Mois de " + Mois.titre(mois));
        invalidate();
    }

    /** Le jour dessiné à cet endroit de la grille. */
    LocalDate jourEn(float x, float y) {
        if (cases.isEmpty() || getWidth() == 0 || getHeight() == 0) {
            return null;
        }
        int colonne = Math.max(0, Math.min(6, (int) (x * 7 / getWidth())));
        int ligne = Math.max(0, Math.min(5, (int) (y * 6 / getHeight())));
        return cases.get(ligne * 7 + colonne);
    }

    @Override
    protected void onMeasure(int largeur, int hauteur) {
        setMeasuredDimension(MeasureSpec.getSize(largeur), 6 * Ui.dp(getContext(), 50));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        gestes.onTouchEvent(e);
        return true;
    }

    @Override
    protected void onDraw(Canvas toile) {
        Context c = getContext();
        float largeur = getWidth() / 7f;
        float hauteur = getHeight() / 6f;
        float rayon = Ui.dp(c, 16);
        float point = Ui.dp(c, 2.5f);
        for (int i = 0; i < cases.size(); i++) {
            LocalDate jour = cases.get(i);
            float cx = (i % 7) * largeur + largeur / 2;
            float haut = (i / 7) * hauteur;
            float cy = haut + Ui.dp(c, 20);
            boolean duMois = YearMonth.from(jour).equals(mois);
            int couleurTexte = Ui.couleur(c, duMois ? fr.manboyx.commun.R.color.texte : fr.manboyx.commun.R.color.texte2);
            if (jour.equals(aujourdhui)) {
                forme.setStyle(Paint.Style.FILL);
                forme.setColor(Ui.couleur(c, fr.manboyx.commun.R.color.accent));
                toile.drawCircle(cx, cy, rayon, forme);
                couleurTexte = Ui.couleur(c, fr.manboyx.commun.R.color.sur_accent);
            }
            if (jour.equals(choisi)) {
                forme.setStyle(Paint.Style.STROKE);
                forme.setStrokeWidth(Ui.dp(c, 2));
                forme.setColor(Ui.couleur(c, fr.manboyx.commun.R.color.accent));
                toile.drawCircle(cx, cy, rayon + Ui.dp(c, 2), forme);
            }
            texte.setColor(couleurTexte);
            texte.setFakeBoldText(jour.equals(aujourdhui));
            toile.drawText(String.valueOf(jour.getDayOfMonth()), cx, cy - (texte.descent() + texte.ascent()) / 2, texte);

            List<Evenement> prevus = jours.get(jour);
            if (prevus == null) {
                continue;
            }
            int nombre = Math.min(POINTS, prevus.size());
            float ecart = point * 3.2f;
            float depart = cx - (nombre - 1) * ecart / 2;
            forme.setStyle(Paint.Style.FILL);
            for (int p = 0; p < nombre; p++) {
                forme.setColor(prevus.get(p).couleur);
                toile.drawCircle(depart + p * ecart, haut + Ui.dp(c, 42), point, forme);
            }
        }
    }
}
