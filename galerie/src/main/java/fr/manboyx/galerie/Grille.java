package fr.manboyx.galerie;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;

import fr.manboyx.commun.ActiviteSobre;
import fr.manboyx.commun.Ui;
import fr.manboyx.galerie.logique.Media;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** La grille de vignettes, commune à « toutes les photos » et au contenu d'un album. */
final class Grille extends BaseAdapter {
    private final ActiviteSobre activite;
    private final Vignettes vignettes;
    private List<Media> medias = new ArrayList<>();
    final GridView vue;

    Grille(ActiviteSobre activite, Vignettes vignettes, IntConsumer ouvrir) {
        this.activite = activite;
        this.vignettes = vignettes;
        vue = new GridView(activite);
        vue.setNumColumns(GridView.AUTO_FIT);
        vue.setColumnWidth(Ui.dp(activite, 110));
        vue.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        vue.setHorizontalSpacing(Ui.dp(activite, 2));
        vue.setVerticalSpacing(Ui.dp(activite, 2));
        vue.setAdapter(this);
        vue.setOnItemClickListener((parent, case_, rang, id) -> ouvrir.accept(rang));
    }

    void montrer(List<Media> nouveaux) {
        medias = nouveaux;
        notifyDataSetChanged();
    }

    List<Media> medias() {
        return medias;
    }

    @Override
    public int getCount() {
        return medias.size();
    }

    @Override
    public Object getItem(int rang) {
        return medias.get(rang);
    }

    @Override
    public long getItemId(int rang) {
        return medias.get(rang).id;
    }

    @Override
    public View getView(int rang, View recyclee, ViewGroup parent) {
        FrameLayout case_ = (FrameLayout) recyclee;
        if (case_ == null) {
            case_ = new FrameLayout(activite);
            case_.addView(new Carre(activite), new FrameLayout.LayoutParams(Ui.PLEIN, Ui.JUSTE));
            TextView video = Ui.texte(activite, "▶", 14, fr.manboyx.commun.R.color.sur_accent);
            video.setShadowLayer(4, 0, 0, 0xFF000000);
            video.setTextColor(0xFFFFFFFF);
            FrameLayout.LayoutParams coin = new FrameLayout.LayoutParams(Ui.JUSTE, Ui.JUSTE, Gravity.BOTTOM | Gravity.START);
            coin.setMargins(Ui.dp(activite, 6), 0, 0, Ui.dp(activite, 4));
            case_.addView(video, coin);
        }
        Media media = medias.get(rang);
        Carre image = (Carre) case_.getChildAt(0);
        image.setContentDescription((media.video ? "Vidéo " : "Photo ") + media.nom);
        vignettes.poser(image, media);
        case_.getChildAt(1).setVisibility(media.video ? View.VISIBLE : View.GONE);
        return case_;
    }
}
