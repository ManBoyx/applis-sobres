package fr.manboyx.commun;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fait un travail lent (lire les contacts, les SMS…) hors de l'écran, puis
 * rend le résultat à l'écran. Le fil de travail s'éteint tout seul après
 * quelques secondes sans rien à faire.
 */
public final class Fond {
    private Fond() {}

    private static Executor executeur;

    /** Remplace le fil de travail ; les tests s'en servent pour tout faire sur place. */
    public static void utiliser(Executor autre) {
        executeur = autre;
    }

    private static synchronized Executor executeur() {
        if (executeur == null) {
            ThreadPoolExecutor fil = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
            fil.allowCoreThreadTimeOut(true);
            executeur = fil;
        }
        return executeur;
    }

    /** Lance `travail` en arrière-plan, puis `suite` sur l'écran avec son résultat. */
    public static <T> void lancer(Supplier<T> travail, Consumer<T> suite) {
        Handler ecran = new Handler(Looper.getMainLooper());
        executeur().execute(() -> {
            T resultat = travail.get();
            if (Looper.myLooper() == Looper.getMainLooper()) {
                suite.accept(resultat);
            } else {
                ecran.post(() -> suite.accept(resultat));
            }
        });
    }
}
