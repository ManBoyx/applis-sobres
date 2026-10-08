package fr.manboyx.galerie;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import fr.manboyx.galerie.logique.Cadrage;

/** Affiche une image que l'on peut grossir en écartant deux doigts, déplacer, et agrandir d'un double appui. */
final class VueZoom extends View {
    interface Ecoute {
        /** Un appui simple : montrer ou cacher les boutons. */
        void appui();

        /** Un glissement de côté, image au repos : +1 pour la suivante, −1 pour la précédente. */
        void glissement(int sens);
    }

    private final Paint pinceau = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Matrix matrice = new Matrix();
    private final ScaleGestureDetector ecartement;
    private final GestureDetector gestes;
    private Bitmap image;
    private Cadrage cadrage;

    VueZoom(Context contexte, Ecoute ecoute) {
        super(contexte);
        ecartement = new ScaleGestureDetector(contexte, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                if (cadrage != null) {
                    cadrage.zoomer(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                    invalidate();
                }
                return true;
            }
        });
        gestes = new GestureDetector(contexte, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent depart, MotionEvent e, float dx, float dy) {
                if (cadrage != null && !cadrage.auRepos()) {
                    cadrage.deplacer(-dx, -dy);
                    invalidate();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (cadrage != null) {
                    cadrage.basculer(e.getX(), e.getY());
                    invalidate();
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                ecoute.appui();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent depart, MotionEvent e, float vx, float vy) {
                boolean deCote = Math.abs(vx) > Math.abs(vy) * 1.5f && Math.abs(vx) > 600;
                if (deCote && (cadrage == null || cadrage.auRepos())) {
                    ecoute.glissement(vx < 0 ? 1 : -1);
                }
                return true;
            }
        });
    }

    /** `null` : écran vide en attendant l'image. */
    void montrer(Bitmap nouvelle) {
        image = nouvelle;
        cadrage = null;
        invalidate();
    }

    boolean aUneImage() {
        return image != null;
    }

    @Override
    protected void onSizeChanged(int largeur, int hauteur, int avantL, int avantH) {
        cadrage = null;
    }

    @Override
    protected void onDraw(Canvas toile) {
        if (image == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        if (cadrage == null) {
            cadrage = new Cadrage(image.getWidth(), image.getHeight(), getWidth(), getHeight());
        }
        matrice.setScale(cadrage.echelle(), cadrage.echelle());
        matrice.postTranslate(cadrage.gauche(), cadrage.haut());
        toile.drawBitmap(image, matrice, pinceau);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        ecartement.onTouchEvent(e);
        gestes.onTouchEvent(e);
        return true;
    }
}
