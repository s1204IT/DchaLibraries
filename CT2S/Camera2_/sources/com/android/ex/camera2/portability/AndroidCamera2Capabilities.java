package com.android.ex.camera2.portability;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.support.v4.widget.ViewDragHelper;
import android.util.Range;
import android.util.Rational;
import com.android.camera.AnimationManager;
import com.android.camera.ButtonManager;
import com.android.camera.data.MediaDetails;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.debug.Log;
import java.util.Arrays;

public class AndroidCamera2Capabilities extends CameraCapabilities {
    private static Log.Tag TAG = new Log.Tag("AndCam2Capabs");

    AndroidCamera2Capabilities(CameraCharacteristics p) {
        super(new CameraCapabilities.Stringifier());
        StreamConfigurationMap s = (StreamConfigurationMap) p.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Range<Integer>[] arr$ = (Range[]) p.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        for (Range<Integer> fpsRange : arr$) {
            this.mSupportedPreviewFpsRange.add(new int[]{((Integer) fpsRange.getLower()).intValue(), ((Integer) fpsRange.getUpper()).intValue()});
        }
        this.mSupportedPreviewSizes.addAll(Size.buildListFromAndroidSizes(Arrays.asList(s.getOutputSizes(SurfaceTexture.class))));
        int[] arr$2 = s.getOutputFormats();
        for (int format : arr$2) {
            this.mSupportedPreviewFormats.add(Integer.valueOf(format));
        }
        this.mSupportedVideoSizes.addAll(Size.buildListFromAndroidSizes(Arrays.asList(s.getOutputSizes(MediaRecorder.class))));
        this.mSupportedPhotoSizes.addAll(Size.buildListFromAndroidSizes(Arrays.asList(s.getOutputSizes(256))));
        this.mSupportedPhotoFormats.addAll(this.mSupportedPreviewFormats);
        buildSceneModes(p);
        buildFlashModes(p);
        buildFocusModes(p);
        buildWhiteBalances(p);
        buildColorEffects(p);
        buildAntibanding(p);
        buildSensorSensitivity(p);
        this.mMaxContrast = ((Integer) p.get(CameraCharacteristics.CONTROL_MAX_CONTRAST)).intValue();
        this.mMinContrast = ((Integer) p.get(CameraCharacteristics.CONTROL_MIN_CONTRAST)).intValue();
        this.mMaxSaturation = ((Integer) p.get(CameraCharacteristics.CONTROL_MAX_SATURATION)).intValue();
        this.mMinSaturation = ((Integer) p.get(CameraCharacteristics.CONTROL_MIN_SATURATION)).intValue();
        this.mMaxBrightness = ((Integer) p.get(CameraCharacteristics.CONTROL_MAX_BRIGHTNESS)).intValue();
        this.mMinBrightness = ((Integer) p.get(CameraCharacteristics.CONTROL_MIN_BRIGHTNESS)).intValue();
        this.mMaxSharpness = ((Integer) p.get(CameraCharacteristics.CONTROL_MAX_SHARPNESS)).intValue();
        this.mMinSharpness = ((Integer) p.get(CameraCharacteristics.CONTROL_MIN_SHARPNESS)).intValue();
        Range<Integer> ecRange = (Range) p.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        this.mMinExposureCompensation = ((Integer) ecRange.getLower()).intValue();
        this.mMaxExposureCompensation = ((Integer) ecRange.getUpper()).intValue();
        Rational ecStep = (Rational) p.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        this.mExposureCompensationStep = ecStep.getNumerator() / ecStep.getDenominator();
        this.mMaxNumOfFacesSupported = ((Integer) p.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT)).intValue();
        this.mMaxNumOfMeteringArea = ((Integer) p.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)).intValue();
        this.mMaxZoomRatio = ((Float) p.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)).floatValue();
        if (supports(CameraCapabilities.FocusMode.AUTO)) {
            this.mMaxNumOfFocusAreas = ((Integer) p.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)).intValue();
            if (this.mMaxNumOfFocusAreas > 0) {
                this.mSupportedFeatures.add(CameraCapabilities.Feature.FOCUS_AREA);
            }
        }
        if (this.mMaxNumOfMeteringArea > 0) {
            this.mSupportedFeatures.add(CameraCapabilities.Feature.METERING_AREA);
        }
        if (this.mMaxZoomRatio > 1.0f) {
            this.mSupportedFeatures.add(CameraCapabilities.Feature.ZOOM);
        }
    }

    private void buildSceneModes(CameraCharacteristics p) {
        int[] scenes = (int[]) p.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
        if (scenes != null) {
            for (int scene : scenes) {
                CameraCapabilities.SceneMode equiv = sceneModeFromInt(scene);
                if (equiv != null) {
                    this.mSupportedSceneModes.add(equiv);
                }
            }
        }
    }

    private void buildFlashModes(CameraCharacteristics p) {
        this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.OFF);
        if (((Boolean) p.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)).booleanValue()) {
            this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.AUTO);
            this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.ON);
            this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.TORCH);
            int[] arr$ = (int[]) p.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            for (int expose : arr$) {
                if (expose == 4) {
                    this.mSupportedFlashModes.add(CameraCapabilities.FlashMode.RED_EYE);
                }
            }
        }
    }

    private void buildFocusModes(CameraCharacteristics p) {
        int[] focuses = (int[]) p.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (focuses != null) {
            for (int focus : focuses) {
                CameraCapabilities.FocusMode equiv = focusModeFromInt(focus);
                if (equiv != null) {
                    this.mSupportedFocusModes.add(equiv);
                }
            }
        }
    }

    private void buildWhiteBalances(CameraCharacteristics p) {
        int[] bals = (int[]) p.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        if (bals != null) {
            for (int bal : bals) {
                CameraCapabilities.WhiteBalance equiv = whiteBalanceFromInt(bal);
                if (equiv != null) {
                    this.mSupportedWhiteBalances.add(equiv);
                }
            }
        }
    }

    private void buildColorEffects(CameraCharacteristics p) {
        int[] bals = (int[]) p.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
        if (bals != null) {
            for (int bal : bals) {
                CameraCapabilities.ColorEffect equiv = colorEffectFromInt(bal);
                if (equiv != null) {
                    this.mSupportedColorEffects.add(equiv);
                }
            }
        }
    }

    private void buildAntibanding(CameraCharacteristics p) {
        int[] bals = (int[]) p.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
        if (bals != null) {
            for (int bal : bals) {
                CameraCapabilities.AntiBanding equiv = bandingFilterFromInt(bal);
                if (equiv != null) {
                    this.mSupportedAntiBanding.add(equiv);
                }
            }
        }
    }

    private void buildIsoModes(CameraCharacteristics p) {
        int[] bals = (int[]) p.get(CameraCharacteristics.CONTROL_AVAILABLE_ISO_MODES);
        if (bals != null) {
            for (int bal : bals) {
                CameraCapabilities.IsoMode equiv = isoModeFromInt(bal);
                if (equiv != null) {
                    this.mSupportedIsoModes.add(equiv);
                }
            }
        }
    }

    private void buildSensorSensitivity(CameraCharacteristics p) {
        Range<Integer> isoRange = (Range) p.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        int minIso = ((Integer) isoRange.getLower()).intValue();
        int maxIso = ((Integer) isoRange.getUpper()).intValue();
        if (isoModeToInt(CameraCapabilities.IsoMode.ISOAUTO) <= maxIso && isoModeToInt(CameraCapabilities.IsoMode.ISOAUTO) >= minIso) {
            this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISOAUTO);
        }
        if (isoModeToInt(CameraCapabilities.IsoMode.ISO100) <= maxIso && isoModeToInt(CameraCapabilities.IsoMode.ISO100) >= minIso) {
            this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO100);
        }
        if (isoModeToInt(CameraCapabilities.IsoMode.ISO200) <= maxIso && isoModeToInt(CameraCapabilities.IsoMode.ISO200) >= minIso) {
            this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO200);
        }
        if (isoModeToInt(CameraCapabilities.IsoMode.ISO400) <= maxIso && isoModeToInt(CameraCapabilities.IsoMode.ISO400) >= minIso) {
            this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO400);
        }
        if (isoModeToInt(CameraCapabilities.IsoMode.ISO800) <= maxIso && isoModeToInt(CameraCapabilities.IsoMode.ISO800) >= minIso) {
            this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO800);
        }
        if (isoModeToInt(CameraCapabilities.IsoMode.ISO1600) <= maxIso && isoModeToInt(CameraCapabilities.IsoMode.ISO1600) >= minIso) {
            this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO1600);
        }
        if (isoModeToInt(CameraCapabilities.IsoMode.ISO3200) <= maxIso && isoModeToInt(CameraCapabilities.IsoMode.ISO3200) >= minIso) {
            this.mSupportedIsoModes.add(CameraCapabilities.IsoMode.ISO3200);
        }
    }

    public static CameraCapabilities.FocusMode focusModeFromInt(int fm) {
        switch (fm) {
            case 0:
                return CameraCapabilities.FocusMode.FIXED;
            case 1:
                return CameraCapabilities.FocusMode.AUTO;
            case 2:
                return CameraCapabilities.FocusMode.MACRO;
            case 3:
                return CameraCapabilities.FocusMode.CONTINUOUS_VIDEO;
            case 4:
                return CameraCapabilities.FocusMode.CONTINUOUS_PICTURE;
            case 5:
                return CameraCapabilities.FocusMode.EXTENDED_DOF;
            case 6:
                return CameraCapabilities.FocusMode.MANUAL;
            default:
                Log.w(TAG, "Unable to convert from API 2 focus mode: " + fm);
                return null;
        }
    }

    public static CameraCapabilities.SceneMode sceneModeFromInt(int sm) {
        switch (sm) {
            case 0:
                return CameraCapabilities.SceneMode.AUTO;
            case 1:
            case 6:
            default:
                if (sm == LegacyVendorTags.CONTROL_SCENE_MODE_HDR) {
                    return CameraCapabilities.SceneMode.HDR;
                }
                Log.w(TAG, "Unable to convert from API 2 scene mode: " + sm);
                return null;
            case 2:
                return CameraCapabilities.SceneMode.ACTION;
            case 3:
                return CameraCapabilities.SceneMode.PORTRAIT;
            case 4:
                return CameraCapabilities.SceneMode.LANDSCAPE;
            case 5:
                return CameraCapabilities.SceneMode.NIGHT;
            case 7:
                return CameraCapabilities.SceneMode.THEATRE;
            case 8:
                return CameraCapabilities.SceneMode.BEACH;
            case 9:
                return CameraCapabilities.SceneMode.SNOW;
            case 10:
                return CameraCapabilities.SceneMode.SUNSET;
            case 11:
                return CameraCapabilities.SceneMode.STEADYPHOTO;
            case 12:
                return CameraCapabilities.SceneMode.FIREWORKS;
            case ButtonManager.BUTTON_AUTO_FOCUS:
                return CameraCapabilities.SceneMode.SPORTS;
            case 14:
                return CameraCapabilities.SceneMode.PARTY;
            case ViewDragHelper.EDGE_ALL:
                return CameraCapabilities.SceneMode.CANDLELIGHT;
            case 16:
                return CameraCapabilities.SceneMode.BARCODE;
        }
    }

    public static CameraCapabilities.WhiteBalance whiteBalanceFromInt(int wb) {
        switch (wb) {
            case 1:
                return CameraCapabilities.WhiteBalance.AUTO;
            case 2:
                return CameraCapabilities.WhiteBalance.INCANDESCENT;
            case 3:
                return CameraCapabilities.WhiteBalance.FLUORESCENT;
            case 4:
                return CameraCapabilities.WhiteBalance.WARM_FLUORESCENT;
            case 5:
                return CameraCapabilities.WhiteBalance.DAYLIGHT;
            case 6:
                return CameraCapabilities.WhiteBalance.CLOUDY_DAYLIGHT;
            case 7:
                return CameraCapabilities.WhiteBalance.TWILIGHT;
            case 8:
                return CameraCapabilities.WhiteBalance.SHADE;
            default:
                Log.w(TAG, "Unable to convert from API 2 white balance: " + wb);
                return null;
        }
    }

    public static CameraCapabilities.ColorEffect colorEffectFromInt(int ce) {
        switch (ce) {
            case 0:
                return CameraCapabilities.ColorEffect.NONE;
            case 1:
                return CameraCapabilities.ColorEffect.MONO;
            case 2:
                return CameraCapabilities.ColorEffect.NEGATIVE;
            case 3:
                return CameraCapabilities.ColorEffect.SOLARIZE;
            case 4:
                return CameraCapabilities.ColorEffect.SEPIA;
            case 5:
                return CameraCapabilities.ColorEffect.POSTERIZE;
            case 6:
                return CameraCapabilities.ColorEffect.WHITE_BOARD;
            case 7:
                return CameraCapabilities.ColorEffect.BLACK_BOARD;
            case 8:
                return CameraCapabilities.ColorEffect.AQUA;
            default:
                Log.w(TAG, "Unable to convert from API 2 color effect: " + ce);
                return null;
        }
    }

    public static CameraCapabilities.AntiBanding bandingFilterFromInt(int bf) {
        switch (bf) {
            case 0:
                return CameraCapabilities.AntiBanding.ABOFF;
            case 1:
                return CameraCapabilities.AntiBanding.AB50HZ;
            case 2:
                return CameraCapabilities.AntiBanding.AB60HZ;
            case 3:
                return CameraCapabilities.AntiBanding.ABAUTO;
            default:
                Log.w(TAG, "Unable to convert from API 2 banding filter: " + bf);
                return null;
        }
    }

    public static CameraCapabilities.IsoMode isoModeFromInt(int bf) {
        switch (bf) {
            case 0:
                return CameraCapabilities.IsoMode.ISOAUTO;
            case 50:
                return CameraCapabilities.IsoMode.ISO50;
            case MediaDetails.INDEX_MAKE:
                return CameraCapabilities.IsoMode.ISO100;
            case MediaDetails.INDEX_PATH:
                return CameraCapabilities.IsoMode.ISO200;
            case AnimationManager.SHRINK_DURATION:
                return CameraCapabilities.IsoMode.ISO400;
            case 800:
                return CameraCapabilities.IsoMode.ISO800;
            case 1600:
                return CameraCapabilities.IsoMode.ISO1600;
            case 3200:
                return CameraCapabilities.IsoMode.ISO3200;
            default:
                Log.w(TAG, "Unable to convert from API 2 ISO mode: " + bf);
                return null;
        }
    }

    public static int isoModeToInt(CameraCapabilities.IsoMode bf) {
        switch (bf) {
            case ISOAUTO:
                return 0;
            case ISO50:
                return 50;
            case ISO100:
                return 100;
            case ISO200:
                return MediaDetails.INDEX_PATH;
            case ISO400:
                return AnimationManager.SHRINK_DURATION;
            case ISO800:
                return 800;
            case ISO1600:
                return 1600;
            case ISO3200:
                return 3200;
            default:
                Log.w(TAG, "Unable to convert API 2 ISO mode: " + bf);
                return -1;
        }
    }

    public static CameraCapabilities.BurstCapture burstCaptureModeFromInt(int bf) {
        switch (bf) {
            case 0:
                return CameraCapabilities.BurstCapture.OFF;
            case 1:
                return CameraCapabilities.BurstCapture.INFINITE;
            case 2:
                return CameraCapabilities.BurstCapture.FAST;
            default:
                Log.w(TAG, "Unable to convert from API 2 BurstCapture mode: " + bf);
                return null;
        }
    }
}
