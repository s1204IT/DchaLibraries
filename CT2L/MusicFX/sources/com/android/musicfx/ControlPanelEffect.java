package com.android.musicfx;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.util.Log;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class ControlPanelEffect {
    private static String[] mEQPresetNames;
    private static final ConcurrentHashMap<Integer, Virtualizer> mVirtualizerInstances = new ConcurrentHashMap<>(16, 0.75f, 2);
    private static final ConcurrentHashMap<Integer, BassBoost> mBassBoostInstances = new ConcurrentHashMap<>(16, 0.75f, 2);
    private static final ConcurrentHashMap<Integer, Equalizer> mEQInstances = new ConcurrentHashMap<>(16, 0.75f, 2);
    private static final ConcurrentHashMap<Integer, PresetReverb> mPresetReverbInstances = new ConcurrentHashMap<>(16, 0.75f, 2);
    private static final ConcurrentHashMap<String, Integer> mPackageSessions = new ConcurrentHashMap<>(16, 0.75f, 2);
    private static final short[] EQUALIZER_BAND_LEVEL_RANGE_DEFAULT = {-1500, 1500};
    private static final int[] EQUALIZER_CENTER_FREQ_DEFAULT = {60000, 230000, 910000, 3600000, 14000000};
    private static final short[] EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL = {0, 800, 400, 100, 1000};
    private static final short[] EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT = {0, 0, 0, 0, 0};
    private static final short[][] EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT = (short[][]) Array.newInstance((Class<?>) Short.TYPE, 0, 5);
    private static short[] mEQBandLevelRange = EQUALIZER_BAND_LEVEL_RANGE_DEFAULT;
    private static short mEQNumBands = 5;
    private static int[] mEQCenterFreq = EQUALIZER_CENTER_FREQ_DEFAULT;
    private static short mEQNumPresets = 0;
    private static short[][] mEQPresetOpenSLESBandLevel = EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT;
    private static boolean mIsEQInitialized = false;
    private static final Object mEQInitLock = new Object();

    enum ControlMode {
        CONTROL_EFFECTS,
        CONTROL_PREFERENCES
    }

    enum Key {
        global_enabled,
        virt_enabled,
        virt_strength_supported,
        virt_strength,
        virt_type,
        bb_enabled,
        bb_strength,
        te_enabled,
        te_strength,
        avl_enabled,
        lm_enabled,
        lm_strength,
        eq_enabled,
        eq_num_bands,
        eq_level_range,
        eq_center_freq,
        eq_band_level,
        eq_num_presets,
        eq_preset_name,
        eq_preset_user_band_level,
        eq_preset_user_band_level_default,
        eq_preset_opensl_es_band_level,
        eq_preset_ci_extreme_band_level,
        eq_current_preset,
        pr_enabled,
        pr_current_preset
    }

    public static void initEffectsPreferences(Context context, String packageName, int audioSession) throws Throwable {
        Virtualizer virtualizerEffect;
        Throwable th;
        Equalizer equalizerEffect;
        SharedPreferences prefs = context.getSharedPreferences(packageName, 0);
        SharedPreferences.Editor editor = prefs.edit();
        ControlMode controlMode = getControlMode(audioSession);
        try {
            boolean isGlobalEnabled = prefs.getBoolean(Key.global_enabled.toString(), false);
            editor.putBoolean(Key.global_enabled.toString(), isGlobalEnabled);
            Log.v("MusicFXControlPanelEffect", "isGlobalEnabled = " + isGlobalEnabled);
            boolean isVIEnabled = prefs.getBoolean(Key.virt_enabled.toString(), true);
            Virtualizer virt = new Virtualizer(0, audioSession);
            int vIStrength = prefs.getInt(Key.virt_strength.toString(), virt.getRoundedStrength());
            virt.release();
            editor.putBoolean(Key.virt_enabled.toString(), isVIEnabled);
            editor.putInt(Key.virt_strength.toString(), vIStrength);
            MediaPlayer mediaPlayer = new MediaPlayer();
            Virtualizer virtualizerEffect2 = null;
            try {
                virtualizerEffect = new Virtualizer(0, mediaPlayer.getAudioSessionId());
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                editor.putBoolean(Key.virt_strength_supported.toString(), virtualizerEffect.getStrengthSupported());
                if (virtualizerEffect != null) {
                    Log.d("MusicFXControlPanelEffect", "Releasing dummy Virtualizer effect");
                    virtualizerEffect.release();
                }
                mediaPlayer.release();
                boolean isBBEnabled = prefs.getBoolean(Key.bb_enabled.toString(), true);
                int bBStrength = prefs.getInt(Key.bb_strength.toString(), 667);
                editor.putBoolean(Key.bb_enabled.toString(), isBBEnabled);
                editor.putInt(Key.bb_strength.toString(), bBStrength);
                synchronized (mEQInitLock) {
                    if (!mIsEQInitialized) {
                        MediaPlayer mediaPlayer2 = new MediaPlayer();
                        int session = mediaPlayer2.getAudioSessionId();
                        Equalizer equalizerEffect2 = null;
                        try {
                            try {
                                Log.d("MusicFXControlPanelEffect", "Creating dummy EQ effect on session " + session);
                                equalizerEffect = new Equalizer(0, session);
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        } catch (IllegalArgumentException e) {
                            e = e;
                        } catch (IllegalStateException e2) {
                            e = e2;
                        } catch (UnsupportedOperationException e3) {
                            e = e3;
                        } catch (RuntimeException e4) {
                            e = e4;
                        }
                        try {
                            mEQBandLevelRange = equalizerEffect.getBandLevelRange();
                            mEQNumBands = equalizerEffect.getNumberOfBands();
                            mEQCenterFreq = new int[mEQNumBands];
                            for (short band = 0; band < mEQNumBands; band = (short) (band + 1)) {
                                mEQCenterFreq[band] = equalizerEffect.getCenterFreq(band);
                            }
                            mEQNumPresets = equalizerEffect.getNumberOfPresets();
                            mEQPresetNames = new String[mEQNumPresets];
                            mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) Short.TYPE, mEQNumPresets, mEQNumBands);
                            for (short preset = 0; preset < mEQNumPresets; preset = (short) (preset + 1)) {
                                mEQPresetNames[preset] = equalizerEffect.getPresetName(preset);
                                equalizerEffect.usePreset(preset);
                                for (short band2 = 0; band2 < mEQNumBands; band2 = (short) (band2 + 1)) {
                                    mEQPresetOpenSLESBandLevel[preset][band2] = equalizerEffect.getBandLevel(band2);
                                }
                            }
                            mIsEQInitialized = true;
                            if (equalizerEffect != null) {
                                Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                equalizerEffect.release();
                            }
                            mediaPlayer2.release();
                            if (!mIsEQInitialized) {
                                mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) Short.TYPE, mEQNumPresets, mEQNumBands);
                                for (short preset2 = 0; preset2 < mEQNumPresets; preset2 = (short) (preset2 + 1)) {
                                    mEQPresetNames[preset2] = prefs.getString(Key.eq_preset_name.toString() + ((int) preset2), "Preset" + ((int) preset2));
                                    if (preset2 < EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT.length) {
                                        mEQPresetOpenSLESBandLevel[preset2] = Arrays.copyOf(EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT[preset2], (int) mEQNumBands);
                                    }
                                }
                            }
                        } catch (IllegalArgumentException e5) {
                            e = e5;
                            equalizerEffect2 = equalizerEffect;
                            Log.e("MusicFXControlPanelEffect", "Equalizer: " + e);
                            if (equalizerEffect2 != null) {
                                Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                equalizerEffect2.release();
                            }
                            mediaPlayer2.release();
                            if (!mIsEQInitialized) {
                                mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) Short.TYPE, mEQNumPresets, mEQNumBands);
                                for (short preset3 = 0; preset3 < mEQNumPresets; preset3 = (short) (preset3 + 1)) {
                                    mEQPresetNames[preset3] = prefs.getString(Key.eq_preset_name.toString() + ((int) preset3), "Preset" + ((int) preset3));
                                    if (preset3 < EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT.length) {
                                        mEQPresetOpenSLESBandLevel[preset3] = Arrays.copyOf(EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT[preset3], (int) mEQNumBands);
                                    }
                                }
                            }
                        } catch (IllegalStateException e6) {
                            e = e6;
                            equalizerEffect2 = equalizerEffect;
                            Log.e("MusicFXControlPanelEffect", "Equalizer: " + e);
                            if (equalizerEffect2 != null) {
                                Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                equalizerEffect2.release();
                            }
                            mediaPlayer2.release();
                            if (!mIsEQInitialized) {
                                mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) Short.TYPE, mEQNumPresets, mEQNumBands);
                                for (short preset4 = 0; preset4 < mEQNumPresets; preset4 = (short) (preset4 + 1)) {
                                    mEQPresetNames[preset4] = prefs.getString(Key.eq_preset_name.toString() + ((int) preset4), "Preset" + ((int) preset4));
                                    if (preset4 < EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT.length) {
                                        mEQPresetOpenSLESBandLevel[preset4] = Arrays.copyOf(EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT[preset4], (int) mEQNumBands);
                                    }
                                }
                            }
                        } catch (UnsupportedOperationException e7) {
                            e = e7;
                            equalizerEffect2 = equalizerEffect;
                            Log.e("MusicFXControlPanelEffect", "Equalizer: " + e);
                            if (equalizerEffect2 != null) {
                                Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                equalizerEffect2.release();
                            }
                            mediaPlayer2.release();
                            if (!mIsEQInitialized) {
                                mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) Short.TYPE, mEQNumPresets, mEQNumBands);
                                for (short preset5 = 0; preset5 < mEQNumPresets; preset5 = (short) (preset5 + 1)) {
                                    mEQPresetNames[preset5] = prefs.getString(Key.eq_preset_name.toString() + ((int) preset5), "Preset" + ((int) preset5));
                                    if (preset5 < EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT.length) {
                                        mEQPresetOpenSLESBandLevel[preset5] = Arrays.copyOf(EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT[preset5], (int) mEQNumBands);
                                    }
                                }
                            }
                        } catch (RuntimeException e8) {
                            e = e8;
                            equalizerEffect2 = equalizerEffect;
                            Log.e("MusicFXControlPanelEffect", "Equalizer: " + e);
                            if (equalizerEffect2 != null) {
                                Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                equalizerEffect2.release();
                            }
                            mediaPlayer2.release();
                            if (!mIsEQInitialized) {
                                mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) Short.TYPE, mEQNumPresets, mEQNumBands);
                                for (short preset6 = 0; preset6 < mEQNumPresets; preset6 = (short) (preset6 + 1)) {
                                    mEQPresetNames[preset6] = prefs.getString(Key.eq_preset_name.toString() + ((int) preset6), "Preset" + ((int) preset6));
                                    if (preset6 < EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT.length) {
                                        mEQPresetOpenSLESBandLevel[preset6] = Arrays.copyOf(EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT[preset6], (int) mEQNumBands);
                                    }
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            equalizerEffect2 = equalizerEffect;
                            if (equalizerEffect2 != null) {
                                Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                equalizerEffect2.release();
                            }
                            mediaPlayer2.release();
                            if (mIsEQInitialized) {
                                throw th;
                            }
                            mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) Short.TYPE, mEQNumPresets, mEQNumBands);
                            for (short preset7 = 0; preset7 < mEQNumPresets; preset7 = (short) (preset7 + 1)) {
                                mEQPresetNames[preset7] = prefs.getString(Key.eq_preset_name.toString() + ((int) preset7), "Preset" + ((int) preset7));
                                if (preset7 < EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT.length) {
                                    mEQPresetOpenSLESBandLevel[preset7] = Arrays.copyOf(EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT[preset7], (int) mEQNumBands);
                                }
                            }
                            throw th;
                        }
                    }
                    editor.putInt(Key.eq_level_range.toString() + 0, mEQBandLevelRange[0]);
                    editor.putInt(Key.eq_level_range.toString() + 1, mEQBandLevelRange[1]);
                    editor.putInt(Key.eq_num_bands.toString(), mEQNumBands);
                    editor.putInt(Key.eq_num_presets.toString(), mEQNumPresets);
                    short[] eQPresetCIExtremeBandLevel = Arrays.copyOf(EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, (int) mEQNumBands);
                    short[] eQPresetUserBandLevelDefault = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                    short eQPreset = (short) prefs.getInt(Key.eq_current_preset.toString(), mEQNumPresets);
                    editor.putInt(Key.eq_current_preset.toString(), eQPreset);
                    short[] bandLevel = new short[mEQNumBands];
                    for (short band3 = 0; band3 < mEQNumBands; band3 = (short) (band3 + 1)) {
                        if (controlMode == ControlMode.CONTROL_PREFERENCES) {
                            if (eQPreset < mEQNumPresets) {
                                bandLevel[band3] = mEQPresetOpenSLESBandLevel[eQPreset][band3];
                            } else if (eQPreset == mEQNumPresets) {
                                bandLevel[band3] = eQPresetCIExtremeBandLevel[band3];
                            } else {
                                bandLevel[band3] = (short) prefs.getInt(Key.eq_preset_user_band_level.toString() + ((int) band3), eQPresetUserBandLevelDefault[band3]);
                            }
                            editor.putInt(Key.eq_band_level.toString() + ((int) band3), bandLevel[band3]);
                        }
                        editor.putInt(Key.eq_center_freq.toString() + ((int) band3), mEQCenterFreq[band3]);
                        editor.putInt(Key.eq_preset_ci_extreme_band_level.toString() + ((int) band3), eQPresetCIExtremeBandLevel[band3]);
                        editor.putInt(Key.eq_preset_user_band_level_default.toString() + ((int) band3), eQPresetUserBandLevelDefault[band3]);
                    }
                    for (short preset8 = 0; preset8 < mEQNumPresets; preset8 = (short) (preset8 + 1)) {
                        editor.putString(Key.eq_preset_name.toString() + ((int) preset8), mEQPresetNames[preset8]);
                        for (short band4 = 0; band4 < mEQNumBands; band4 = (short) (band4 + 1)) {
                            editor.putInt(Key.eq_preset_opensl_es_band_level.toString() + ((int) preset8) + "_" + ((int) band4), mEQPresetOpenSLESBandLevel[preset8][band4]);
                        }
                    }
                }
                boolean isEQEnabled = prefs.getBoolean(Key.eq_enabled.toString(), true);
                editor.putBoolean(Key.eq_enabled.toString(), isEQEnabled);
                boolean isEnabledPR = prefs.getBoolean(Key.pr_enabled.toString(), false);
                short presetPR = (short) prefs.getInt(Key.pr_current_preset.toString(), 0);
                editor.putBoolean(Key.pr_enabled.toString(), isEnabledPR);
                editor.putInt(Key.pr_current_preset.toString(), presetPR);
                editor.commit();
            } catch (Throwable th5) {
                th = th5;
                virtualizerEffect2 = virtualizerEffect;
                if (virtualizerEffect2 != null) {
                    Log.d("MusicFXControlPanelEffect", "Releasing dummy Virtualizer effect");
                    virtualizerEffect2.release();
                }
                mediaPlayer.release();
                throw th;
            }
        } catch (RuntimeException e9) {
            Log.e("MusicFXControlPanelEffect", "initEffectsPreferences: processingEnabled: " + e9);
        }
    }

    public static ControlMode getControlMode(int audioSession) {
        return audioSession == -4 ? ControlMode.CONTROL_PREFERENCES : ControlMode.CONTROL_EFFECTS;
    }

    public static void setParameterBoolean(Context context, String packageName, int audioSession, Key key, boolean value) {
        boolean processingEnabled;
        try {
            SharedPreferences prefs = context.getSharedPreferences(packageName, 0);
            ControlMode controlMode = getControlMode(audioSession);
            boolean enabled = value;
            if (key == Key.global_enabled) {
                if (value) {
                    if (controlMode == ControlMode.CONTROL_EFFECTS) {
                        Virtualizer virtualizerEffect = getVirtualizerEffect(audioSession);
                        if (virtualizerEffect != null) {
                            virtualizerEffect.setEnabled(prefs.getBoolean(Key.virt_enabled.toString(), true));
                            int defaultstrength = virtualizerEffect.getRoundedStrength();
                            int vIStrength = prefs.getInt(Key.virt_strength.toString(), defaultstrength);
                            setParameterInt(context, packageName, audioSession, Key.virt_strength, vIStrength);
                        }
                        BassBoost bassBoostEffect = getBassBoostEffect(audioSession);
                        if (bassBoostEffect != null) {
                            bassBoostEffect.setEnabled(prefs.getBoolean(Key.bb_enabled.toString(), true));
                            int bBStrength = prefs.getInt(Key.bb_strength.toString(), 667);
                            setParameterInt(context, packageName, audioSession, Key.bb_strength, bBStrength);
                        }
                        Equalizer equalizerEffect = getEqualizerEffect(audioSession);
                        if (equalizerEffect != null) {
                            equalizerEffect.setEnabled(prefs.getBoolean(Key.eq_enabled.toString(), true));
                            int[] bandLevels = getParameterIntArray(context, packageName, audioSession, Key.eq_band_level);
                            int len = bandLevels.length;
                            for (short band = 0; band < len; band = (short) (band + 1)) {
                                int level = bandLevels[band];
                                setParameterInt(context, packageName, audioSession, Key.eq_band_level, level, band);
                            }
                        }
                    }
                    processingEnabled = true;
                    Log.v("MusicFXControlPanelEffect", "processingEnabled=true");
                } else {
                    if (controlMode == ControlMode.CONTROL_EFFECTS) {
                        Virtualizer virtualizerEffect2 = getVirtualizerEffectNoCreate(audioSession);
                        if (virtualizerEffect2 != null) {
                            mVirtualizerInstances.remove(Integer.valueOf(audioSession), virtualizerEffect2);
                            virtualizerEffect2.setEnabled(false);
                            virtualizerEffect2.release();
                        }
                        BassBoost bassBoostEffect2 = getBassBoostEffectNoCreate(audioSession);
                        if (bassBoostEffect2 != null) {
                            mBassBoostInstances.remove(Integer.valueOf(audioSession), bassBoostEffect2);
                            bassBoostEffect2.setEnabled(false);
                            bassBoostEffect2.release();
                        }
                        Equalizer equalizerEffect2 = getEqualizerEffectNoCreate(audioSession);
                        if (equalizerEffect2 != null) {
                            mEQInstances.remove(Integer.valueOf(audioSession), equalizerEffect2);
                            equalizerEffect2.setEnabled(false);
                            equalizerEffect2.release();
                        }
                    }
                    processingEnabled = false;
                    Log.v("MusicFXControlPanelEffect", "processingEnabled=false");
                }
                enabled = processingEnabled;
            } else if (controlMode == ControlMode.CONTROL_EFFECTS) {
                boolean isGlobalEnabled = prefs.getBoolean(Key.global_enabled.toString(), false);
                if (isGlobalEnabled) {
                    switch (key) {
                        case global_enabled:
                        case pr_enabled:
                            break;
                        case virt_enabled:
                            Virtualizer virtualizerEffect3 = getVirtualizerEffect(audioSession);
                            if (virtualizerEffect3 != null) {
                                virtualizerEffect3.setEnabled(value);
                                enabled = virtualizerEffect3.getEnabled();
                            }
                            break;
                        case bb_enabled:
                            BassBoost bassBoostEffect3 = getBassBoostEffect(audioSession);
                            if (bassBoostEffect3 != null) {
                                bassBoostEffect3.setEnabled(value);
                                enabled = bassBoostEffect3.getEnabled();
                            }
                            break;
                        case eq_enabled:
                            Equalizer equalizerEffect3 = getEqualizerEffect(audioSession);
                            if (equalizerEffect3 != null) {
                                equalizerEffect3.setEnabled(value);
                                enabled = equalizerEffect3.getEnabled();
                            }
                            break;
                        default:
                            Log.e("MusicFXControlPanelEffect", "Unknown/unsupported key " + key);
                            return;
                    }
                }
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(key.toString(), enabled);
            editor.commit();
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "setParameterBoolean: " + key + "; " + value + "; " + e);
        }
    }

    public static Boolean getParameterBoolean(Context context, String packageName, int audioSession, Key key) {
        SharedPreferences prefs = context.getSharedPreferences(packageName, 0);
        boolean value = false;
        try {
            value = prefs.getBoolean(key.toString(), false);
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "getParameterBoolean: " + key + "; false; " + e);
        }
        return Boolean.valueOf(value);
    }

    public static void setParameterInt(Context context, String packageName, int audioSession, Key key, int arg0, int arg1) {
        short bandLevel;
        short bandLevel2;
        String strKey = key.toString();
        int value = arg0;
        try {
            SharedPreferences prefs = context.getSharedPreferences(packageName, 0);
            SharedPreferences.Editor editor = prefs.edit();
            ControlMode controlMode = getControlMode(audioSession);
            if (controlMode == ControlMode.CONTROL_EFFECTS) {
                switch (key) {
                    case virt_strength:
                        Virtualizer virtualizerEffect = getVirtualizerEffect(audioSession);
                        if (virtualizerEffect != null) {
                            virtualizerEffect.setStrength((short) value);
                            value = virtualizerEffect.getRoundedStrength();
                        }
                        break;
                    case bb_strength:
                        BassBoost bassBoostEffect = getBassBoostEffect(audioSession);
                        if (bassBoostEffect != null) {
                            bassBoostEffect.setStrength((short) value);
                            value = bassBoostEffect.getRoundedStrength();
                        }
                        break;
                    case eq_band_level:
                        if (arg1 == -1) {
                            throw new IllegalArgumentException("Dummy arg passed.");
                        }
                        short band = (short) arg1;
                        strKey = strKey + ((int) band);
                        Equalizer equalizerEffect = getEqualizerEffect(audioSession);
                        if (equalizerEffect != null) {
                            equalizerEffect.setBandLevel(band, (short) value);
                            value = equalizerEffect.getBandLevel(band);
                            editor.putInt(Key.eq_preset_user_band_level.toString() + ((int) band), value);
                        }
                        break;
                        break;
                    case eq_current_preset:
                        Equalizer equalizerEffect2 = getEqualizerEffect(audioSession);
                        if (equalizerEffect2 != null) {
                            short preset = (short) value;
                            int numBands = prefs.getInt(Key.eq_num_bands.toString(), 5);
                            int numPresets = prefs.getInt(Key.eq_num_presets.toString(), 0);
                            if (preset < numPresets) {
                                equalizerEffect2.usePreset(preset);
                                value = equalizerEffect2.getCurrentPreset();
                            } else {
                                short[] eQPresetCIExtremeBandLevelDefault = Arrays.copyOf(EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, numBands);
                                short[] eQPresetUserBandLevelDefault = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, numBands);
                                for (short band2 = 0; band2 < numBands; band2 = (short) (band2 + 1)) {
                                    if (preset == numPresets) {
                                        bandLevel2 = (short) prefs.getInt(Key.eq_preset_ci_extreme_band_level.toString() + ((int) band2), eQPresetCIExtremeBandLevelDefault[band2]);
                                    } else {
                                        bandLevel2 = (short) prefs.getInt(Key.eq_preset_user_band_level.toString() + ((int) band2), eQPresetUserBandLevelDefault[band2]);
                                    }
                                    equalizerEffect2.setBandLevel(band2, bandLevel2);
                                }
                            }
                            for (short band3 = 0; band3 < numBands; band3 = (short) (band3 + 1)) {
                                short level = equalizerEffect2.getBandLevel(band3);
                                editor.putInt(Key.eq_band_level.toString() + ((int) band3), level);
                            }
                        }
                        break;
                    case eq_preset_user_band_level:
                    case eq_preset_user_band_level_default:
                    case eq_preset_ci_extreme_band_level:
                        if (arg1 == -1) {
                            throw new IllegalArgumentException("Dummy arg passed.");
                        }
                        short band4 = (short) arg1;
                        strKey = strKey + ((int) band4);
                        break;
                        break;
                    case pr_current_preset:
                        break;
                    default:
                        Log.e("MusicFXControlPanelEffect", "setParameterInt: Unknown/unsupported key " + key);
                        return;
                }
            } else {
                switch (key) {
                    case virt_strength:
                    case bb_strength:
                    case pr_current_preset:
                    case virt_type:
                        break;
                    case eq_band_level:
                        if (arg1 == -1) {
                            throw new IllegalArgumentException("Dummy arg passed.");
                        }
                        short band5 = (short) arg1;
                        strKey = strKey + ((int) band5);
                        editor.putInt(Key.eq_preset_user_band_level.toString() + ((int) band5), value);
                        break;
                        break;
                    case eq_current_preset:
                        short preset2 = (short) value;
                        int numBands2 = prefs.getInt(Key.eq_num_bands.toString(), 5);
                        int numPresets2 = prefs.getInt(Key.eq_num_presets.toString(), 0);
                        short[][] eQPresetOpenSLESBandLevelDefault = (short[][]) Arrays.copyOf(EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT, numBands2);
                        short[] eQPresetCIExtremeBandLevelDefault2 = Arrays.copyOf(EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, numBands2);
                        short[] eQPresetUserBandLevelDefault2 = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, numBands2);
                        for (short band6 = 0; band6 < numBands2; band6 = (short) (band6 + 1)) {
                            if (preset2 < numPresets2) {
                                bandLevel = (short) prefs.getInt(Key.eq_preset_opensl_es_band_level.toString() + ((int) preset2) + "_" + ((int) band6), eQPresetOpenSLESBandLevelDefault[preset2][band6]);
                            } else if (preset2 == numPresets2) {
                                bandLevel = (short) prefs.getInt(Key.eq_preset_ci_extreme_band_level.toString() + ((int) band6), eQPresetCIExtremeBandLevelDefault2[band6]);
                            } else {
                                bandLevel = (short) prefs.getInt(Key.eq_preset_user_band_level.toString() + ((int) band6), eQPresetUserBandLevelDefault2[band6]);
                            }
                            editor.putInt(Key.eq_band_level.toString() + ((int) band6), bandLevel);
                        }
                        break;
                    case eq_preset_user_band_level:
                    case eq_preset_user_band_level_default:
                    case eq_preset_ci_extreme_band_level:
                        if (arg1 == -1) {
                            throw new IllegalArgumentException("Dummy arg passed.");
                        }
                        short band7 = (short) arg1;
                        strKey = strKey + ((int) band7);
                        break;
                        break;
                    default:
                        Log.e("MusicFXControlPanelEffect", "setParameterInt: Unknown/unsupported key " + key);
                        return;
                }
            }
            editor.putInt(strKey, value);
            editor.apply();
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "setParameterInt: " + key + "; " + arg0 + "; " + arg1 + "; " + e);
        }
    }

    public static void setParameterInt(Context context, String packageName, int audioSession, Key key, int arg) {
        setParameterInt(context, packageName, audioSession, key, arg, -1);
    }

    public static int getParameterInt(Context context, String packageName, int audioSession, String key) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(packageName, 0);
            int value = prefs.getInt(key, 0);
            return value;
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "getParameterInt: " + key + "; " + e);
            return 0;
        }
    }

    public static int getParameterInt(Context context, String packageName, int audioSession, Key key) {
        return getParameterInt(context, packageName, audioSession, key.toString());
    }

    public static int[] getParameterIntArray(Context context, String packageName, int audioSession, Key key) {
        SharedPreferences prefs = context.getSharedPreferences(packageName, 0);
        int[] intArray = null;
        try {
            switch (key) {
                case eq_band_level:
                case eq_preset_user_band_level:
                case eq_preset_user_band_level_default:
                case eq_preset_ci_extreme_band_level:
                case eq_center_freq:
                    int numBands = prefs.getInt(Key.eq_num_bands.toString(), 0);
                    intArray = new int[numBands];
                    break;
                case eq_current_preset:
                case pr_current_preset:
                case virt_type:
                default:
                    Log.e("MusicFXControlPanelEffect", "getParameterIntArray: Unknown/unsupported key " + key);
                    return null;
                case eq_level_range:
                    intArray = new int[2];
                    break;
            }
            for (int i = 0; i < intArray.length; i++) {
                intArray[i] = prefs.getInt(key.toString() + i, 0);
            }
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "getParameterIntArray: " + key + "; " + e);
        }
        return intArray;
    }

    public static String getParameterString(Context context, String packageName, int audioSession, String key) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(packageName, 0);
            String value = prefs.getString(key, "");
            return value;
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "getParameterString: " + key + "; " + e);
            return "";
        }
    }

    public static String getParameterString(Context context, String packageName, int audioSession, Key key, int arg) {
        return getParameterString(context, packageName, audioSession, key.toString() + arg);
    }

    public static void openSession(Context context, String packageName, int audioSession) {
        short eQPreset;
        short level;
        short[] bandLevel;
        short eQNumBands;
        int[] eQCenterFreq;
        short eQNumPresets;
        String[] eQPresetNames;
        Log.v("MusicFXControlPanelEffect", "openSession(" + context + ", " + packageName + ", " + audioSession + ")");
        SharedPreferences prefs = context.getSharedPreferences(packageName, 0);
        SharedPreferences.Editor editor = prefs.edit();
        boolean isGlobalEnabled = prefs.getBoolean(Key.global_enabled.toString(), false);
        editor.putBoolean(Key.global_enabled.toString(), isGlobalEnabled);
        if (isGlobalEnabled) {
            boolean isExistingAudioSession = false;
            try {
                Integer currentAudioSession = mPackageSessions.putIfAbsent(packageName, Integer.valueOf(audioSession));
                if (currentAudioSession != null) {
                    if (currentAudioSession.intValue() == audioSession) {
                        isExistingAudioSession = true;
                    } else {
                        closeSession(context, packageName, currentAudioSession.intValue());
                    }
                }
                Virtualizer virtualizerEffect = getVirtualizerEffect(audioSession);
                try {
                    boolean isEnabled = prefs.getBoolean(Key.virt_enabled.toString(), true);
                    int defaultstrength = isExistingAudioSession ? 1000 : virtualizerEffect.getRoundedStrength();
                    int strength = prefs.getInt(Key.virt_strength.toString(), defaultstrength);
                    virtualizerEffect.setProperties(new Virtualizer.Settings("Virtualizer;strength=" + strength));
                    if (isGlobalEnabled) {
                        virtualizerEffect.setEnabled(isEnabled);
                    } else {
                        virtualizerEffect.setEnabled(false);
                    }
                    Virtualizer.Settings settings = virtualizerEffect.getProperties();
                    Log.v("MusicFXControlPanelEffect", "Parameters: " + settings.toString() + ";enabled=" + isEnabled);
                    editor.putBoolean(Key.virt_enabled.toString(), isEnabled);
                    editor.putInt(Key.virt_strength.toString(), settings.strength);
                } catch (RuntimeException e) {
                    Log.e("MusicFXControlPanelEffect", "openSession: Virtualizer error: " + e);
                }
                if (isExistingAudioSession) {
                    editor.apply();
                    return;
                }
                BassBoost bassBoostEffect = getBassBoostEffect(audioSession);
                try {
                    boolean isEnabled2 = prefs.getBoolean(Key.bb_enabled.toString(), true);
                    int strength2 = prefs.getInt(Key.bb_strength.toString(), 667);
                    bassBoostEffect.setProperties(new BassBoost.Settings("BassBoost;strength=" + strength2));
                    if (isGlobalEnabled) {
                        bassBoostEffect.setEnabled(isEnabled2);
                    } else {
                        bassBoostEffect.setEnabled(false);
                    }
                    BassBoost.Settings settings2 = bassBoostEffect.getProperties();
                    Log.v("MusicFXControlPanelEffect", "Parameters: " + settings2.toString() + ";enabled=" + isEnabled2);
                    editor.putBoolean(Key.bb_enabled.toString(), isEnabled2);
                    editor.putInt(Key.bb_strength.toString(), settings2.strength);
                } catch (RuntimeException e2) {
                    Log.e("MusicFXControlPanelEffect", "openSession: BassBoost error: " + e2);
                }
                Equalizer equalizerEffect = getEqualizerEffect(audioSession);
                try {
                    synchronized (mEQInitLock) {
                        mEQBandLevelRange = equalizerEffect.getBandLevelRange();
                        mEQNumBands = equalizerEffect.getNumberOfBands();
                        mEQCenterFreq = new int[mEQNumBands];
                        mEQNumPresets = equalizerEffect.getNumberOfPresets();
                        mEQPresetNames = new String[mEQNumPresets];
                        for (short preset = 0; preset < mEQNumPresets; preset = (short) (preset + 1)) {
                            mEQPresetNames[preset] = equalizerEffect.getPresetName(preset);
                            editor.putString(Key.eq_preset_name.toString() + ((int) preset), mEQPresetNames[preset]);
                        }
                        editor.putInt(Key.eq_level_range.toString() + 0, mEQBandLevelRange[0]);
                        editor.putInt(Key.eq_level_range.toString() + 1, mEQBandLevelRange[1]);
                        editor.putInt(Key.eq_num_bands.toString(), mEQNumBands);
                        editor.putInt(Key.eq_num_presets.toString(), mEQNumPresets);
                        short[] eQPresetCIExtremeBandLevel = Arrays.copyOf(EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, (int) mEQNumBands);
                        short[] eQPresetUserBandLevelDefault = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                        eQPreset = (short) prefs.getInt(Key.eq_current_preset.toString(), mEQNumPresets);
                        if (eQPreset < mEQNumPresets) {
                            equalizerEffect.usePreset(eQPreset);
                            eQPreset = equalizerEffect.getCurrentPreset();
                        } else {
                            for (short band = 0; band < mEQNumBands; band = (short) (band + 1)) {
                                if (eQPreset == mEQNumPresets) {
                                    level = eQPresetCIExtremeBandLevel[band];
                                } else {
                                    level = (short) prefs.getInt(Key.eq_preset_user_band_level.toString() + ((int) band), eQPresetUserBandLevelDefault[band]);
                                }
                                equalizerEffect.setBandLevel(band, level);
                            }
                        }
                        editor.putInt(Key.eq_current_preset.toString(), eQPreset);
                        bandLevel = new short[mEQNumBands];
                        for (short band2 = 0; band2 < mEQNumBands; band2 = (short) (band2 + 1)) {
                            mEQCenterFreq[band2] = equalizerEffect.getCenterFreq(band2);
                            bandLevel[band2] = equalizerEffect.getBandLevel(band2);
                            editor.putInt(Key.eq_band_level.toString() + ((int) band2), bandLevel[band2]);
                            editor.putInt(Key.eq_center_freq.toString() + ((int) band2), mEQCenterFreq[band2]);
                            editor.putInt(Key.eq_preset_ci_extreme_band_level.toString() + ((int) band2), eQPresetCIExtremeBandLevel[band2]);
                            editor.putInt(Key.eq_preset_user_band_level_default.toString() + ((int) band2), eQPresetUserBandLevelDefault[band2]);
                        }
                        eQNumBands = mEQNumBands;
                        eQCenterFreq = mEQCenterFreq;
                        eQNumPresets = mEQNumPresets;
                        eQPresetNames = mEQPresetNames;
                    }
                    boolean isEnabled3 = prefs.getBoolean(Key.eq_enabled.toString(), true);
                    editor.putBoolean(Key.eq_enabled.toString(), isEnabled3);
                    if (isGlobalEnabled) {
                        equalizerEffect.setEnabled(isEnabled3);
                    } else {
                        equalizerEffect.setEnabled(false);
                    }
                    Log.v("MusicFXControlPanelEffect", "Parameters: Equalizer");
                    Log.v("MusicFXControlPanelEffect", "bands=" + ((int) eQNumBands));
                    String str = "levels=";
                    for (short band3 = 0; band3 < eQNumBands; band3 = (short) (band3 + 1)) {
                        str = str + ((int) bandLevel[band3]) + "; ";
                    }
                    Log.v("MusicFXControlPanelEffect", str);
                    String str2 = "center=";
                    for (short band4 = 0; band4 < eQNumBands; band4 = (short) (band4 + 1)) {
                        str2 = str2 + eQCenterFreq[band4] + "; ";
                    }
                    Log.v("MusicFXControlPanelEffect", str2);
                    String str3 = "presets=";
                    for (short preset2 = 0; preset2 < eQNumPresets; preset2 = (short) (preset2 + 1)) {
                        str3 = str3 + eQPresetNames[preset2] + "; ";
                    }
                    Log.v("MusicFXControlPanelEffect", str3);
                    Log.v("MusicFXControlPanelEffect", "current=" + ((int) eQPreset));
                } catch (RuntimeException e3) {
                    Log.e("MusicFXControlPanelEffect", "openSession: Equalizer error: " + e3);
                }
                editor.commit();
            } catch (NullPointerException e4) {
                Log.e("MusicFXControlPanelEffect", "openSession: " + e4);
                editor.commit();
            }
        }
    }

    public static void closeSession(Context context, String packageName, int audioSession) {
        Log.v("MusicFXControlPanelEffect", "closeSession(" + context + ", " + packageName + ", " + audioSession + ")");
        PresetReverb presetReverb = mPresetReverbInstances.remove(Integer.valueOf(audioSession));
        if (presetReverb != null) {
            presetReverb.release();
        }
        Equalizer equalizer = mEQInstances.remove(Integer.valueOf(audioSession));
        if (equalizer != null) {
            equalizer.release();
        }
        BassBoost bassBoost = mBassBoostInstances.remove(Integer.valueOf(audioSession));
        if (bassBoost != null) {
            bassBoost.release();
        }
        Virtualizer virtualizer = mVirtualizerInstances.remove(Integer.valueOf(audioSession));
        if (virtualizer != null) {
            virtualizer.release();
        }
        mPackageSessions.remove(packageName);
    }

    private static Virtualizer getVirtualizerEffectNoCreate(int audioSession) {
        return mVirtualizerInstances.get(Integer.valueOf(audioSession));
    }

    private static Virtualizer getVirtualizerEffect(int audioSession) {
        Virtualizer virtualizerEffect = getVirtualizerEffectNoCreate(audioSession);
        if (virtualizerEffect == null) {
            try {
                Virtualizer newVirtualizerEffect = new Virtualizer(0, audioSession);
                Virtualizer virtualizerEffect2 = mVirtualizerInstances.putIfAbsent(Integer.valueOf(audioSession), newVirtualizerEffect);
                return virtualizerEffect2 == null ? newVirtualizerEffect : virtualizerEffect2;
            } catch (IllegalArgumentException e) {
                Log.e("MusicFXControlPanelEffect", "Virtualizer: " + e);
                return virtualizerEffect;
            } catch (UnsupportedOperationException e2) {
                Log.e("MusicFXControlPanelEffect", "Virtualizer: " + e2);
                return virtualizerEffect;
            } catch (RuntimeException e3) {
                Log.e("MusicFXControlPanelEffect", "Virtualizer: " + e3);
                return virtualizerEffect;
            }
        }
        return virtualizerEffect;
    }

    private static BassBoost getBassBoostEffectNoCreate(int audioSession) {
        return mBassBoostInstances.get(Integer.valueOf(audioSession));
    }

    private static BassBoost getBassBoostEffect(int audioSession) {
        BassBoost bassBoostEffect = getBassBoostEffectNoCreate(audioSession);
        if (bassBoostEffect == null) {
            try {
                BassBoost newBassBoostEffect = new BassBoost(0, audioSession);
                BassBoost bassBoostEffect2 = mBassBoostInstances.putIfAbsent(Integer.valueOf(audioSession), newBassBoostEffect);
                return bassBoostEffect2 == null ? newBassBoostEffect : bassBoostEffect2;
            } catch (IllegalArgumentException e) {
                Log.e("MusicFXControlPanelEffect", "BassBoost: " + e);
                return bassBoostEffect;
            } catch (UnsupportedOperationException e2) {
                Log.e("MusicFXControlPanelEffect", "BassBoost: " + e2);
                return bassBoostEffect;
            } catch (RuntimeException e3) {
                Log.e("MusicFXControlPanelEffect", "BassBoost: " + e3);
                return bassBoostEffect;
            }
        }
        return bassBoostEffect;
    }

    private static Equalizer getEqualizerEffectNoCreate(int audioSession) {
        return mEQInstances.get(Integer.valueOf(audioSession));
    }

    private static Equalizer getEqualizerEffect(int audioSession) {
        Equalizer equalizerEffect = getEqualizerEffectNoCreate(audioSession);
        if (equalizerEffect == null) {
            try {
                Equalizer newEqualizerEffect = new Equalizer(0, audioSession);
                Equalizer equalizerEffect2 = mEQInstances.putIfAbsent(Integer.valueOf(audioSession), newEqualizerEffect);
                return equalizerEffect2 == null ? newEqualizerEffect : equalizerEffect2;
            } catch (IllegalArgumentException e) {
                Log.e("MusicFXControlPanelEffect", "Equalizer: " + e);
                return equalizerEffect;
            } catch (UnsupportedOperationException e2) {
                Log.e("MusicFXControlPanelEffect", "Equalizer: " + e2);
                return equalizerEffect;
            } catch (RuntimeException e3) {
                Log.e("MusicFXControlPanelEffect", "Equalizer: " + e3);
                return equalizerEffect;
            }
        }
        return equalizerEffect;
    }
}
