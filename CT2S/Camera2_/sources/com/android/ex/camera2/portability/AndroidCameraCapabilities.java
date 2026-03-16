package com.android.ex.camera2.portability;

import android.hardware.Camera;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.debug.Log;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class AndroidCameraCapabilities extends CameraCapabilities {
    private static final String RECORDING_HINT = "recording-hint";
    private static Log.Tag TAG = new Log.Tag("AndCamCapabs");
    private static final String TRUE = "true";
    public static final float ZOOM_MULTIPLIER = 100.0f;
    private FpsComparator mFpsComparator;
    private SizeComparator mSizeComparator;

    AndroidCameraCapabilities(Camera.Parameters p) {
        super(new CameraCapabilities.Stringifier());
        this.mFpsComparator = new FpsComparator();
        this.mSizeComparator = new SizeComparator();
        this.mMaxExposureCompensation = p.getMaxExposureCompensation();
        this.mMinExposureCompensation = p.getMinExposureCompensation();
        this.mExposureCompensationStep = p.getExposureCompensationStep();
        this.mMaxNumOfFacesSupported = p.getMaxNumDetectedFaces();
        this.mMaxNumOfMeteringArea = p.getMaxNumMeteringAreas();
        this.mPreferredPreviewSizeForVideo = new Size(p.getPreferredPreviewSizeForVideo());
        this.mSupportedPreviewFormats.addAll(p.getSupportedPreviewFormats());
        this.mSupportedPhotoFormats.addAll(p.getSupportedPictureFormats());
        this.mHorizontalViewAngle = p.getHorizontalViewAngle();
        this.mVerticalViewAngle = p.getVerticalViewAngle();
        buildPreviewFpsRange(p);
        buildPreviewSizes(p);
        buildVideoSizes(p);
        buildPictureSizes(p);
        buildSceneModes(p);
        buildFlashModes(p);
        buildFocusModes(p);
        buildWhiteBalances(p);
        buildColorEffects(p);
        buildAntibanding(p);
        buildBurstCaptureModes(p);
        buildIsoModes(p);
        this.mMaxContrast = p.getMaxContrast();
        this.mMinContrast = p.getMinContrast();
        this.mMaxSaturation = p.getMaxSaturation();
        this.mMinSaturation = p.getMinSaturation();
        this.mMaxBrightness = p.getMaxBrightness();
        this.mMinBrightness = p.getMinBrightness();
        this.mMaxSharpness = p.getMaxSharpness();
        this.mMinSharpness = p.getMinSharpness();
        if (p.isZoomSupported()) {
            this.mMaxZoomRatio = p.getZoomRatios().get(p.getMaxZoom()).intValue() / 100.0f;
            this.mSupportedFeatures.add(CameraCapabilities.Feature.ZOOM);
        }
        if (p.isVideoSnapshotSupported()) {
            this.mSupportedFeatures.add(CameraCapabilities.Feature.VIDEO_SNAPSHOT);
        }
        if (p.isAutoExposureLockSupported()) {
            this.mSupportedFeatures.add(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK);
        }
        if (p.isAutoWhiteBalanceLockSupported()) {
            this.mSupportedFeatures.add(CameraCapabilities.Feature.AUTO_WHITE_BALANCE_LOCK);
        }
        if (p.isVideoStabilizationSupported()) {
            this.mSupportedFeatures.add(CameraCapabilities.Feature.VIDEO_STABILIZATION);
        }
        if (p.isVideoTNRSupported()) {
            this.mSupportedFeatures.add(CameraCapabilities.Feature.VIDEO_TNR);
        }
        if (supports(CameraCapabilities.FocusMode.AUTO)) {
            this.mMaxNumOfFocusAreas = p.getMaxNumFocusAreas();
            if (this.mMaxNumOfFocusAreas > 0) {
                this.mSupportedFeatures.add(CameraCapabilities.Feature.FOCUS_AREA);
            }
        }
        if (this.mMaxNumOfMeteringArea > 0) {
            this.mSupportedFeatures.add(CameraCapabilities.Feature.METERING_AREA);
        }
    }

    AndroidCameraCapabilities(AndroidCameraCapabilities src) {
        super(src);
        this.mFpsComparator = new FpsComparator();
        this.mSizeComparator = new SizeComparator();
    }

    private void buildPreviewFpsRange(Camera.Parameters p) {
        List<int[]> supportedPreviewFpsRange = p.getSupportedPreviewFpsRange();
        if (supportedPreviewFpsRange != null) {
            this.mSupportedPreviewFpsRange.addAll(supportedPreviewFpsRange);
        }
        Collections.sort(this.mSupportedPreviewFpsRange, this.mFpsComparator);
    }

    private void buildPreviewSizes(Camera.Parameters p) {
        List<Camera.Size> supportedPreviewSizes = p.getSupportedPreviewSizes();
        if (supportedPreviewSizes != null) {
            for (Camera.Size s : supportedPreviewSizes) {
                this.mSupportedPreviewSizes.add(new Size(s.width, s.height));
            }
        }
        Collections.sort(this.mSupportedPreviewSizes, this.mSizeComparator);
    }

    private void buildVideoSizes(Camera.Parameters p) {
        List<Camera.Size> supportedVideoSizes = p.getSupportedVideoSizes();
        if (supportedVideoSizes != null) {
            for (Camera.Size s : supportedVideoSizes) {
                this.mSupportedVideoSizes.add(new Size(s.width, s.height));
            }
        }
        Collections.sort(this.mSupportedVideoSizes, this.mSizeComparator);
    }

    private void buildPictureSizes(Camera.Parameters p) {
        List<Camera.Size> supportedPictureSizes = p.getSupportedPictureSizes();
        if (supportedPictureSizes != null) {
            for (Camera.Size s : supportedPictureSizes) {
                this.mSupportedPhotoSizes.add(new Size(s.width, s.height));
            }
        }
        Collections.sort(this.mSupportedPhotoSizes, this.mSizeComparator);
    }

    private void buildSceneModes(Camera.Parameters p) {
        List<String> supportedSceneModes;
        if (TRUE.equals(p.get(RECORDING_HINT))) {
            supportedSceneModes = p.getSupportedVideoSceneModes();
        } else {
            supportedSceneModes = p.getSupportedSceneModes();
        }
        if (supportedSceneModes != null) {
            for (String scene : supportedSceneModes) {
                if ("auto".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.AUTO);
                } else if ("action".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.ACTION);
                } else if ("barcode".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.BARCODE);
                } else if ("beach".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.BEACH);
                } else if ("candlelight".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.CANDLELIGHT);
                } else if ("fireworks".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.FIREWORKS);
                } else if ("hdr".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.HDR);
                } else if ("landscape".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.LANDSCAPE);
                } else if ("night".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.NIGHT);
                } else if ("night-portrait".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.NIGHT_PORTRAIT);
                } else if ("party".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.PARTY);
                } else if ("portrait".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.PORTRAIT);
                } else if ("snow".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.SNOW);
                } else if ("sports".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.SPORTS);
                } else if ("steadyphoto".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.STEADYPHOTO);
                } else if ("sunset".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.SUNSET);
                } else if ("theatre".equals(scene)) {
                    this.mSupportedSceneModes.add(CameraCapabilities.SceneMode.THEATRE);
                }
            }
        }
    }

    private void buildFlashModes(Camera.Parameters p) {
        List<String> supportedFlashModes = p.getSupportedFlashModes();
        if (supportedFlashModes == null) {
            this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.NO_FLASH);
            return;
        }
        for (String flash : supportedFlashModes) {
            if ("auto".equals(flash)) {
                this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.AUTO);
            } else if ("off".equals(flash)) {
                this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.OFF);
            } else if ("on".equals(flash)) {
                this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.ON);
            } else if ("red-eye".equals(flash)) {
                this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.RED_EYE);
            } else if ("torch".equals(flash)) {
                this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.TORCH);
            }
        }
    }

    private void buildFocusModes(Camera.Parameters p) {
        List<String> supportedFocusModes = p.getSupportedFocusModes();
        if (supportedFocusModes != null) {
            for (String focus : supportedFocusModes) {
                if ("auto".equals(focus)) {
                    this.mSupportedFocusModes.add(CameraCapabilities.FocusMode.AUTO);
                } else if ("continuous-picture".equals(focus)) {
                    this.mSupportedFocusModes.add(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE);
                } else if ("continuous-video".equals(focus)) {
                    this.mSupportedFocusModes.add(CameraCapabilities.FocusMode.CONTINUOUS_VIDEO);
                } else if ("edof".equals(focus)) {
                    this.mSupportedFocusModes.add(CameraCapabilities.FocusMode.EXTENDED_DOF);
                } else if ("fixed".equals(focus)) {
                    this.mSupportedFocusModes.add(CameraCapabilities.FocusMode.FIXED);
                } else if ("infinity".equals(focus)) {
                    this.mSupportedFocusModes.add(CameraCapabilities.FocusMode.INFINITY);
                } else if ("macro".equals(focus)) {
                    this.mSupportedFocusModes.add(CameraCapabilities.FocusMode.MACRO);
                }
            }
        }
    }

    private void buildWhiteBalances(Camera.Parameters p) {
        List<String> supportedWhiteBalances = p.getSupportedWhiteBalance();
        if (supportedWhiteBalances != null) {
            for (String wb : supportedWhiteBalances) {
                if ("auto".equals(wb)) {
                    this.mSupportedWhiteBalances.add(CameraCapabilities.WhiteBalance.AUTO);
                } else if ("cloudy-daylight".equals(wb)) {
                    this.mSupportedWhiteBalances.add(CameraCapabilities.WhiteBalance.CLOUDY_DAYLIGHT);
                } else if ("daylight".equals(wb)) {
                    this.mSupportedWhiteBalances.add(CameraCapabilities.WhiteBalance.DAYLIGHT);
                } else if ("fluorescent".equals(wb)) {
                    this.mSupportedWhiteBalances.add(CameraCapabilities.WhiteBalance.FLUORESCENT);
                } else if ("incandescent".equals(wb)) {
                    this.mSupportedWhiteBalances.add(CameraCapabilities.WhiteBalance.INCANDESCENT);
                } else if ("shade".equals(wb)) {
                    this.mSupportedWhiteBalances.add(CameraCapabilities.WhiteBalance.SHADE);
                } else if ("twilight".equals(wb)) {
                    this.mSupportedWhiteBalances.add(CameraCapabilities.WhiteBalance.TWILIGHT);
                } else if ("warm-fluorescent".equals(wb)) {
                    this.mSupportedWhiteBalances.add(CameraCapabilities.WhiteBalance.WARM_FLUORESCENT);
                }
            }
        }
    }

    private void buildColorEffects(Camera.Parameters p) {
        List<String> supportedColorEffects = p.getSupportedColorEffects();
        if (supportedColorEffects != null) {
            for (String ce : supportedColorEffects) {
                if ("none".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.NONE);
                } else if ("mono".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.MONO);
                } else if ("negative".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.NEGATIVE);
                } else if ("solarize".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.SOLARIZE);
                } else if ("sepia".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.SEPIA);
                } else if ("posterize".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.POSTERIZE);
                } else if ("whiteboard".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.WHITE_BOARD);
                } else if ("blackboard".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.BLACK_BOARD);
                } else if ("aqua".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.AQUA);
                } else if ("oldmovie".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.OLD_MOVIE);
                } else if ("toonshading".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.TOON_SHADING);
                } else if ("pencilsketch".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.PENCIL_SKETCH);
                } else if ("glow".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.GLOW);
                } else if ("twist".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.TWIST);
                } else if ("vivid".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.VIVID);
                } else if ("frame".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.FRAME);
                } else if ("sunshine".equals(ce)) {
                    this.mSupportedColorEffects.add(CameraCapabilities.ColorEffect.SUNSHINE);
                }
            }
        }
    }

    private void buildAntibanding(Camera.Parameters p) {
        List<String> supportedAntibanding = p.getSupportedAntibanding();
        if (supportedAntibanding != null) {
            for (String ab : supportedAntibanding) {
                if ("50hz".equals(ab)) {
                    this.mSupportedAntiBanding.add(CameraCapabilities.AntiBanding.AB50HZ);
                } else if ("60hz".equals(ab)) {
                    this.mSupportedAntiBanding.add(CameraCapabilities.AntiBanding.AB60HZ);
                } else if ("auto".equals(ab)) {
                    this.mSupportedAntiBanding.add(CameraCapabilities.AntiBanding.ABAUTO);
                } else if ("off".equals(ab)) {
                    this.mSupportedAntiBanding.add(CameraCapabilities.AntiBanding.ABOFF);
                }
            }
        }
    }

    private void buildBurstCaptureModes(Camera.Parameters p) {
        List<String> supportedBurstCaptureModes = p.getSupportedBurstCaptureModes();
        if (supportedBurstCaptureModes != null) {
            for (String bc : supportedBurstCaptureModes) {
                if ("fast-burst".equals(bc)) {
                    this.mSupportedBurstCaptureModes.add(CameraCapabilities.BurstCapture.FAST);
                } else if ("infinite-burst".equals(bc)) {
                    this.mSupportedBurstCaptureModes.add(CameraCapabilities.BurstCapture.INFINITE);
                } else if ("off-burst".equals(bc)) {
                    this.mSupportedBurstCaptureModes.add(CameraCapabilities.BurstCapture.OFF);
                }
            }
        }
    }

    private void buildIsoModes(Camera.Parameters p) {
        List<String> supportedIsoModes = p.getSupportedIsoModes();
        if (supportedIsoModes != null) {
            for (String im : supportedIsoModes) {
                if ("auto".equals(im)) {
                    this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISOAUTO);
                } else if ("50".equals(im)) {
                    this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO50);
                } else if ("100".equals(im)) {
                    this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO100);
                } else if ("200".equals(im)) {
                    this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO200);
                } else if ("400".equals(im)) {
                    this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO400);
                } else if ("800".equals(im)) {
                    this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO800);
                } else if ("1600".equals(im)) {
                    this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO1600);
                } else if ("3200".equals(im)) {
                    this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO3200);
                }
            }
        }
    }

    private static class FpsComparator implements Comparator<int[]> {
        private FpsComparator() {
        }

        @Override
        public int compare(int[] fps1, int[] fps2) {
            return fps1[0] == fps2[0] ? fps1[1] - fps2[1] : fps1[0] - fps2[0];
        }
    }

    private static class SizeComparator implements Comparator<Size> {
        private SizeComparator() {
        }

        @Override
        public int compare(Size size1, Size size2) {
            return size1.width() == size2.width() ? size1.height() - size2.height() : size1.width() - size2.width();
        }
    }
}
