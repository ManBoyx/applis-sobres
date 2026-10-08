package fr.manboyx.appareil;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import fr.manboyx.appareil.logique.Flash;
import fr.manboyx.appareil.logique.Orientation;
import fr.manboyx.appareil.logique.Prise;
import fr.manboyx.appareil.logique.Taille;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** L'appareil photo d'Android. Tout ce qui est lent se fait sur un fil à part, arrêté à la fermeture. */
final class ChambreCamera2 implements Chambre {
    private static final String ECHEC = "L'appareil photo n'a pas pu s'ouvrir.";
    /** Largeur maximale de l'aperçu : au-delà, l'écran n'y gagne rien et la batterie y perd. */
    private static final int LARGEUR_APERCU = 1920;

    private final CameraManager gestionnaire;
    private final Handler ecran = new Handler(Looper.getMainLooper());
    private Ecoute ecoute;

    private HandlerThread fil;
    private Handler travail;
    private CameraDevice appareil;
    private CameraCaptureSession seance;
    private ImageReader lecteur;
    private Surface surfaceApercu;
    private CaptureRequest.Builder apercu;

    private boolean frontale;
    private boolean flashPossible;
    private boolean miseAuPointContinue;
    private float zoomMaximum = 1f;
    private int capteur = 90;
    private Rect capteurUtile;
    private Flash flash = Flash.AUTO;
    private float zoom = 1f;
    private int rotationEnCours;
    /** Change à chaque ouverture : les réponses d'une ouverture précédente sont ignorées. */
    private int generation;

    ChambreCamera2(Context contexte) {
        gestionnaire = contexte.getSystemService(CameraManager.class);
    }

    @Override
    public void ecouter(Ecoute ecoute) {
        this.ecoute = ecoute;
    }

    private String trouver(boolean frontale) throws CameraAccessException {
        int cote = frontale ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;
        String[] toutes = gestionnaire.getCameraIdList();
        for (String id : toutes) {
            Integer face = gestionnaire.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
            if (face != null && face == cote) {
                return id;
            }
        }
        return toutes.length > 0 ? toutes[0] : null;
    }

    @Override
    public boolean deuxCotes() {
        try {
            boolean avant = false;
            boolean arriere = false;
            for (String id : gestionnaire.getCameraIdList()) {
                Integer face = gestionnaire.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                avant |= face != null && face == CameraCharacteristics.LENS_FACING_FRONT;
                arriere |= face != null && face == CameraCharacteristics.LENS_FACING_BACK;
            }
            return avant && arriere;
        } catch (CameraAccessException | RuntimeException inaccessible) {
            return false;
        }
    }

    private static List<Taille> tailles(Size[] proposees) {
        List<Taille> tailles = new ArrayList<>();
        if (proposees != null) {
            for (Size s : proposees) {
                tailles.add(new Taille(s.getWidth(), s.getHeight()));
            }
        }
        return tailles;
    }

    @Override
    public void ouvrir(SurfaceTexture surface, boolean frontale) {
        fermer();
        int ouverture = ++generation;
        try {
            String id = trouver(frontale);
            if (id == null) {
                signaler(ouverture, "Ce téléphone n'a pas d'appareil photo.");
                return;
            }
            CameraCharacteristics c = gestionnaire.getCameraCharacteristics(id);
            Integer face = c.get(CameraCharacteristics.LENS_FACING);
            this.frontale = face != null && face == CameraCharacteristics.LENS_FACING_FRONT;
            StreamConfigurationMap carte = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Taille photo = carte == null ? null : Taille.photo(tailles(carte.getOutputSizes(ImageFormat.JPEG)));
            Taille vue = photo == null ? null : Taille.apercu(tailles(carte.getOutputSizes(SurfaceTexture.class)), photo, LARGEUR_APERCU);
            if (photo == null || vue == null) {
                signaler(ouverture, ECHEC);
                return;
            }
            Integer montage = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
            capteur = montage == null ? 90 : montage;
            flashPossible = Boolean.TRUE.equals(c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));
            Float maximum = c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            zoomMaximum = maximum == null ? 1f : maximum;
            capteurUtile = c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int[] modes = c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            miseAuPointContinue = false;
            for (int i = 0; modes != null && i < modes.length; i++) {
                miseAuPointContinue |= modes[i] == CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
            }
            zoom = 1f;

            fil = new HandlerThread("appareil-photo");
            fil.start();
            travail = new Handler(fil.getLooper());

            surface.setDefaultBufferSize(vue.largeur, vue.hauteur);
            surfaceApercu = new Surface(surface);
            lecteur = ImageReader.newInstance(photo.largeur, photo.hauteur, ImageFormat.JPEG, 2);
            lecteur.setOnImageAvailableListener(source -> {
                try (Image image = source.acquireNextImage()) {
                    if (image == null) {
                        return;
                    }
                    ByteBuffer tampon = image.getPlanes()[0].getBuffer();
                    byte[] jpeg = new byte[tampon.remaining()];
                    tampon.get(jpeg);
                    int rotation = rotationEnCours;
                    ecran.post(() -> {
                        if (ouverture == generation && ecoute != null) {
                            ecoute.photo(jpeg, rotation);
                        }
                    });
                } catch (RuntimeException illisible) {
                    signaler(ouverture, "La photo n'a pas pu être prise.");
                }
            }, travail);

            gestionnaire.openCamera(id, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice ouvert) {
                    if (ouverture != generation) {
                        ouvert.close();
                        return;
                    }
                    appareil = ouvert;
                    demarrer(ouverture, vue);
                }

                @Override
                public void onDisconnected(CameraDevice perdu) {
                    perdu.close();
                    if (appareil == perdu) {
                        appareil = null;
                    }
                }

                @Override
                public void onError(CameraDevice enPanne, int erreur) {
                    enPanne.close();
                    if (appareil == enPanne) {
                        appareil = null;
                    }
                    signaler(ouverture, ECHEC);
                }
            }, travail);
        } catch (CameraAccessException | SecurityException | IllegalArgumentException | IllegalStateException echec) {
            signaler(ouverture, ECHEC);
        }
    }

    @SuppressWarnings("deprecation")
    private void demarrer(int ouverture, Taille vue) {
        try {
            appareil.createCaptureSession(Arrays.asList(surfaceApercu, lecteur.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession prete) {
                    if (ouverture != generation || appareil == null) {
                        return;
                    }
                    seance = prete;
                    try {
                        apercu = appareil.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        apercu.addTarget(surfaceApercu);
                        regler(apercu);
                        prete.setRepeatingRequest(apercu.build(), null, travail);
                        boolean deFace = frontale;
                        boolean avecFlash = flashPossible;
                        float maximum = zoomMaximum;
                        ecran.post(() -> {
                            if (ouverture == generation && ecoute != null) {
                                ecoute.pret(vue.largeur, vue.hauteur, deFace, avecFlash, maximum);
                            }
                        });
                    } catch (CameraAccessException | IllegalStateException | IllegalArgumentException echec) {
                        signaler(ouverture, ECHEC);
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession ratee) {
                    signaler(ouverture, ECHEC);
                }
            }, travail);
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException echec) {
            signaler(ouverture, ECHEC);
        }
    }

    /** Applique mise au point, flash et zoom à une demande (aperçu ou photo). */
    private void regler(CaptureRequest.Builder demande) {
        demande.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        if (miseAuPointContinue) {
            demande.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        }
        int exposition = CameraMetadata.CONTROL_AE_MODE_ON;
        if (flashPossible && flash == Flash.AUTO) {
            exposition = CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH;
        } else if (flashPossible && flash == Flash.OUI) {
            exposition = CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
        }
        demande.set(CaptureRequest.CONTROL_AE_MODE, exposition);
        if (capteurUtile != null && zoomMaximum > 1f) {
            int[] z = Prise.zone(capteurUtile.width(), capteurUtile.height(), zoom, zoomMaximum);
            demande.set(CaptureRequest.SCALER_CROP_REGION, new Rect(z[0], z[1], z[2], z[3]));
        }
    }

    private void reprendreApercu() {
        if (seance == null || apercu == null) {
            return;
        }
        try {
            regler(apercu);
            seance.setRepeatingRequest(apercu.build(), null, travail);
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException fermee) {
            // La séance vient de se fermer : le prochain réglage attendra la suivante.
        }
    }

    @Override
    public void flash(Flash mode) {
        flash = mode;
        reprendreApercu();
    }

    @Override
    public void zoom(float facteur) {
        zoom = Prise.borner(facteur, zoomMaximum);
        reprendreApercu();
    }

    @Override
    public void prendre(int tenue) {
        int ouverture = generation;
        if (appareil == null || seance == null) {
            signaler(ouverture, "L'appareil photo n'est pas prêt.");
            return;
        }
        try {
            CaptureRequest.Builder prise = appareil.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            prise.addTarget(lecteur.getSurface());
            regler(prise);
            rotationEnCours = Orientation.photo(capteur, tenue, frontale);
            prise.set(CaptureRequest.JPEG_ORIENTATION, rotationEnCours);
            prise.set(CaptureRequest.JPEG_QUALITY, (byte) 92);
            seance.capture(prise.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureFailed(CameraCaptureSession s, CaptureRequest demande, CaptureFailure panne) {
                    signaler(ouverture, "La photo n'a pas pu être prise.");
                }
            }, travail);
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException echec) {
            signaler(ouverture, "La photo n'a pas pu être prise.");
        }
    }

    @Override
    public void fermer() {
        generation++;
        try {
            if (seance != null) {
                seance.close();
            }
            if (appareil != null) {
                appareil.close();
            }
        } catch (RuntimeException dejaFerme) {
            // Rien à libérer de plus.
        }
        seance = null;
        appareil = null;
        apercu = null;
        if (lecteur != null) {
            lecteur.close();
            lecteur = null;
        }
        if (surfaceApercu != null) {
            surfaceApercu.release();
            surfaceApercu = null;
        }
        if (fil != null) {
            fil.quitSafely();
            fil = null;
            travail = null;
        }
    }

    private void signaler(int ouverture, String message) {
        ecran.post(() -> {
            if (ouverture == generation && ecoute != null) {
                ecoute.erreur(message);
            }
        });
    }
}
