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
    private static short[] mEQBandLevelRange = EQUALIZER_BAND_LEVEL_RANGE_DEFAULT;
    private static short mEQNumBands = 5;
    private static int[] mEQCenterFreq = EQUALIZER_CENTER_FREQ_DEFAULT;
    private static short mEQNumPresets = 0;
    private static short[][] mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) short.class, 0, 5);
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

    public static void initEffectsPreferences(Context context, String str, int i) throws Throwable {
        Virtualizer virtualizer;
        int audioSessionId;
        Equalizer equalizer;
        RuntimeException e;
        UnsupportedOperationException e2;
        IllegalStateException e3;
        IllegalArgumentException e4;
        SharedPreferences sharedPreferences = context.getSharedPreferences(str, 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        ControlMode controlMode = getControlMode(i);
        try {
            boolean z = sharedPreferences.getBoolean(Key.global_enabled.toString(), false);
            editorEdit.putBoolean(Key.global_enabled.toString(), z);
            Log.v("MusicFXControlPanelEffect", "isGlobalEnabled = " + z);
            boolean z2 = sharedPreferences.getBoolean(Key.virt_enabled.toString(), true);
            Virtualizer virtualizer2 = new Virtualizer(0, i);
            int i2 = sharedPreferences.getInt(Key.virt_strength.toString(), virtualizer2.getRoundedStrength());
            virtualizer2.release();
            editorEdit.putBoolean(Key.virt_enabled.toString(), z2);
            editorEdit.putInt(Key.virt_strength.toString(), i2);
            MediaPlayer mediaPlayer = new MediaPlayer();
            try {
                virtualizer = new Virtualizer(0, mediaPlayer.getAudioSessionId());
                try {
                    editorEdit.putBoolean(Key.virt_strength_supported.toString(), virtualizer.getStrengthSupported());
                    Equalizer equalizer2 = "Releasing dummy Virtualizer effect";
                    Log.d("MusicFXControlPanelEffect", "Releasing dummy Virtualizer effect");
                    virtualizer.release();
                    mediaPlayer.release();
                    boolean z3 = sharedPreferences.getBoolean(Key.bb_enabled.toString(), true);
                    int i3 = sharedPreferences.getInt(Key.bb_strength.toString(), 667);
                    editorEdit.putBoolean(Key.bb_enabled.toString(), z3);
                    editorEdit.putInt(Key.bb_strength.toString(), i3);
                    synchronized (mEQInitLock) {
                        MediaPlayer mediaPlayer2 = mIsEQInitialized;
                        if (mediaPlayer2 == 0) {
                            try {
                                mediaPlayer2 = new MediaPlayer();
                                audioSessionId = mediaPlayer2.getAudioSessionId();
                            } catch (Throwable th) {
                                th = th;
                            }
                            try {
                                Log.d("MusicFXControlPanelEffect", "Creating dummy EQ effect on session " + audioSessionId);
                                equalizer = new Equalizer(0, audioSessionId);
                                try {
                                    mEQBandLevelRange = equalizer.getBandLevelRange();
                                    mEQNumBands = equalizer.getNumberOfBands();
                                    mEQCenterFreq = new int[mEQNumBands];
                                    for (short s = 0; s < mEQNumBands; s = (short) (s + 1)) {
                                        mEQCenterFreq[s] = equalizer.getCenterFreq(s);
                                    }
                                    mEQNumPresets = equalizer.getNumberOfPresets();
                                    mEQPresetNames = new String[mEQNumPresets];
                                    mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) short.class, mEQNumPresets, mEQNumBands);
                                    for (short s2 = 0; s2 < mEQNumPresets; s2 = (short) (s2 + 1)) {
                                        mEQPresetNames[s2] = equalizer.getPresetName(s2);
                                        equalizer.usePreset(s2);
                                        for (short s3 = 0; s3 < mEQNumBands; s3 = (short) (s3 + 1)) {
                                            mEQPresetOpenSLESBandLevel[s2][s3] = equalizer.getBandLevel(s3);
                                        }
                                    }
                                    mIsEQInitialized = true;
                                    Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                    equalizer.release();
                                    mediaPlayer2.release();
                                    if (!mIsEQInitialized) {
                                        Log.e("MusicFXControlPanelEffect", "Error retrieving default EQ values, setting all presets to flat response");
                                        mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) short.class, mEQNumPresets, mEQNumBands);
                                        for (short s4 = 0; s4 < mEQNumPresets; s4 = (short) (s4 + 1)) {
                                            mEQPresetNames[s4] = sharedPreferences.getString(Key.eq_preset_name.toString() + ((int) s4), "Preset" + ((int) s4));
                                            mEQPresetOpenSLESBandLevel[s4] = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                                        }
                                    }
                                } catch (IllegalArgumentException e5) {
                                    e4 = e5;
                                    Log.e("MusicFXControlPanelEffect", "Equalizer: " + e4);
                                    if (equalizer != null) {
                                        Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                        equalizer.release();
                                    }
                                    mediaPlayer2.release();
                                    if (!mIsEQInitialized) {
                                        Log.e("MusicFXControlPanelEffect", "Error retrieving default EQ values, setting all presets to flat response");
                                        mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) short.class, mEQNumPresets, mEQNumBands);
                                        for (short s5 = 0; s5 < mEQNumPresets; s5 = (short) (s5 + 1)) {
                                            mEQPresetNames[s5] = sharedPreferences.getString(Key.eq_preset_name.toString() + ((int) s5), "Preset" + ((int) s5));
                                            mEQPresetOpenSLESBandLevel[s5] = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                                        }
                                    }
                                } catch (IllegalStateException e6) {
                                    e3 = e6;
                                    Log.e("MusicFXControlPanelEffect", "Equalizer: " + e3);
                                    if (equalizer != null) {
                                        Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                        equalizer.release();
                                    }
                                    mediaPlayer2.release();
                                    if (!mIsEQInitialized) {
                                        Log.e("MusicFXControlPanelEffect", "Error retrieving default EQ values, setting all presets to flat response");
                                        mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) short.class, mEQNumPresets, mEQNumBands);
                                        for (short s6 = 0; s6 < mEQNumPresets; s6 = (short) (s6 + 1)) {
                                            mEQPresetNames[s6] = sharedPreferences.getString(Key.eq_preset_name.toString() + ((int) s6), "Preset" + ((int) s6));
                                            mEQPresetOpenSLESBandLevel[s6] = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                                        }
                                    }
                                } catch (UnsupportedOperationException e7) {
                                    e2 = e7;
                                    Log.e("MusicFXControlPanelEffect", "Equalizer: " + e2);
                                    if (equalizer != null) {
                                        Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                        equalizer.release();
                                    }
                                    mediaPlayer2.release();
                                    if (!mIsEQInitialized) {
                                        Log.e("MusicFXControlPanelEffect", "Error retrieving default EQ values, setting all presets to flat response");
                                        mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) short.class, mEQNumPresets, mEQNumBands);
                                        for (short s7 = 0; s7 < mEQNumPresets; s7 = (short) (s7 + 1)) {
                                            mEQPresetNames[s7] = sharedPreferences.getString(Key.eq_preset_name.toString() + ((int) s7), "Preset" + ((int) s7));
                                            mEQPresetOpenSLESBandLevel[s7] = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                                        }
                                    }
                                } catch (RuntimeException e8) {
                                    e = e8;
                                    Log.e("MusicFXControlPanelEffect", "Equalizer: " + e);
                                    if (equalizer != null) {
                                        Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                        equalizer.release();
                                    }
                                    mediaPlayer2.release();
                                    if (!mIsEQInitialized) {
                                        Log.e("MusicFXControlPanelEffect", "Error retrieving default EQ values, setting all presets to flat response");
                                        mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) short.class, mEQNumPresets, mEQNumBands);
                                        for (short s8 = 0; s8 < mEQNumPresets; s8 = (short) (s8 + 1)) {
                                            mEQPresetNames[s8] = sharedPreferences.getString(Key.eq_preset_name.toString() + ((int) s8), "Preset" + ((int) s8));
                                            mEQPresetOpenSLESBandLevel[s8] = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                                        }
                                    }
                                }
                            } catch (IllegalArgumentException e9) {
                                equalizer = null;
                                e4 = e9;
                            } catch (IllegalStateException e10) {
                                equalizer = null;
                                e3 = e10;
                            } catch (UnsupportedOperationException e11) {
                                equalizer = null;
                                e2 = e11;
                            } catch (RuntimeException e12) {
                                equalizer = null;
                                e = e12;
                            } catch (Throwable th2) {
                                th = th2;
                                equalizer2 = 0;
                                if (equalizer2 != 0) {
                                    Log.d("MusicFXControlPanelEffect", "Releasing dummy EQ effect");
                                    equalizer2.release();
                                }
                                mediaPlayer2.release();
                                if (!mIsEQInitialized) {
                                    Log.e("MusicFXControlPanelEffect", "Error retrieving default EQ values, setting all presets to flat response");
                                    mEQPresetOpenSLESBandLevel = (short[][]) Array.newInstance((Class<?>) short.class, mEQNumPresets, mEQNumBands);
                                    for (short s9 = 0; s9 < mEQNumPresets; s9 = (short) (s9 + 1)) {
                                        mEQPresetNames[s9] = sharedPreferences.getString(Key.eq_preset_name.toString() + ((int) s9), "Preset" + ((int) s9));
                                        mEQPresetOpenSLESBandLevel[s9] = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                                    }
                                }
                                throw th;
                            }
                        }
                        editorEdit.putInt(Key.eq_level_range.toString() + 0, mEQBandLevelRange[0]);
                        editorEdit.putInt(Key.eq_level_range.toString() + 1, mEQBandLevelRange[1]);
                        editorEdit.putInt(Key.eq_num_bands.toString(), mEQNumBands);
                        editorEdit.putInt(Key.eq_num_presets.toString(), mEQNumPresets);
                        short[] sArrCopyOf = Arrays.copyOf(EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, (int) mEQNumBands);
                        short[] sArrCopyOf2 = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                        short s10 = (short) sharedPreferences.getInt(Key.eq_current_preset.toString(), mEQNumPresets);
                        editorEdit.putInt(Key.eq_current_preset.toString(), s10);
                        short[] sArr = new short[mEQNumBands];
                        for (short s11 = 0; s11 < mEQNumBands; s11 = (short) (s11 + 1)) {
                            if (controlMode == ControlMode.CONTROL_PREFERENCES) {
                                if (s10 < mEQNumPresets) {
                                    sArr[s11] = mEQPresetOpenSLESBandLevel[s10][s11];
                                } else if (s10 == mEQNumPresets) {
                                    sArr[s11] = sArrCopyOf[s11];
                                } else {
                                    sArr[s11] = (short) sharedPreferences.getInt(Key.eq_preset_user_band_level.toString() + ((int) s11), sArrCopyOf2[s11]);
                                }
                                editorEdit.putInt(Key.eq_band_level.toString() + ((int) s11), sArr[s11]);
                            }
                            editorEdit.putInt(Key.eq_center_freq.toString() + ((int) s11), mEQCenterFreq[s11]);
                            editorEdit.putInt(Key.eq_preset_ci_extreme_band_level.toString() + ((int) s11), sArrCopyOf[s11]);
                            editorEdit.putInt(Key.eq_preset_user_band_level_default.toString() + ((int) s11), sArrCopyOf2[s11]);
                        }
                        for (short s12 = 0; s12 < mEQNumPresets; s12 = (short) (s12 + 1)) {
                            editorEdit.putString(Key.eq_preset_name.toString() + ((int) s12), mEQPresetNames[s12]);
                            for (short s13 = (short) 0; s13 < mEQNumBands; s13 = (short) (s13 + 1)) {
                                editorEdit.putInt(Key.eq_preset_opensl_es_band_level.toString() + ((int) s12) + "_" + ((int) s13), mEQPresetOpenSLESBandLevel[s12][s13]);
                            }
                        }
                    }
                    editorEdit.putBoolean(Key.eq_enabled.toString(), sharedPreferences.getBoolean(Key.eq_enabled.toString(), true));
                    boolean z4 = sharedPreferences.getBoolean(Key.pr_enabled.toString(), false);
                    short s14 = (short) sharedPreferences.getInt(Key.pr_current_preset.toString(), 0);
                    editorEdit.putBoolean(Key.pr_enabled.toString(), z4);
                    editorEdit.putInt(Key.pr_current_preset.toString(), s14);
                    editorEdit.commit();
                } catch (Throwable th3) {
                    th = th3;
                    if (virtualizer != null) {
                        Log.d("MusicFXControlPanelEffect", "Releasing dummy Virtualizer effect");
                        virtualizer.release();
                    }
                    mediaPlayer.release();
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                virtualizer = null;
            }
        } catch (RuntimeException e13) {
            Log.e("MusicFXControlPanelEffect", "initEffectsPreferences: processingEnabled: " + e13);
        }
    }

    public static ControlMode getControlMode(int i) {
        if (i == -4) {
            return ControlMode.CONTROL_PREFERENCES;
        }
        return ControlMode.CONTROL_EFFECTS;
    }

    public static void setParameterBoolean(Context context, String str, int i, Key key, boolean z) {
        SharedPreferences sharedPreferences;
        ControlMode controlMode;
        boolean enabled;
        short s = 0;
        try {
            sharedPreferences = context.getSharedPreferences(str, 0);
            controlMode = getControlMode(i);
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "setParameterBoolean: " + key + "; " + z + "; " + e);
            return;
        }
        if (key == Key.global_enabled) {
            if (z) {
                if (controlMode == ControlMode.CONTROL_EFFECTS) {
                    Virtualizer virtualizerEffect = getVirtualizerEffect(i);
                    if (virtualizerEffect != null) {
                        virtualizerEffect.setEnabled(sharedPreferences.getBoolean(Key.virt_enabled.toString(), true));
                        setParameterInt(context, str, i, Key.virt_strength, sharedPreferences.getInt(Key.virt_strength.toString(), virtualizerEffect.getRoundedStrength()));
                    }
                    BassBoost bassBoostEffect = getBassBoostEffect(i);
                    if (bassBoostEffect != null) {
                        bassBoostEffect.setEnabled(sharedPreferences.getBoolean(Key.bb_enabled.toString(), true));
                        setParameterInt(context, str, i, Key.bb_strength, sharedPreferences.getInt(Key.bb_strength.toString(), 667));
                    }
                    Equalizer equalizerEffect = getEqualizerEffect(i);
                    if (equalizerEffect != null) {
                        equalizerEffect.setEnabled(sharedPreferences.getBoolean(Key.eq_enabled.toString(), true));
                        int[] parameterIntArray = getParameterIntArray(context, str, i, Key.eq_band_level);
                        int length = parameterIntArray.length;
                        while (s < length) {
                            setParameterInt(context, str, i, Key.eq_band_level, parameterIntArray[s], s);
                            s = (short) (s + 1);
                        }
                    }
                }
                Log.v("MusicFXControlPanelEffect", "processingEnabled=true");
                s = (short) 1;
            } else {
                if (controlMode == ControlMode.CONTROL_EFFECTS) {
                    Virtualizer virtualizerEffectNoCreate = getVirtualizerEffectNoCreate(i);
                    if (virtualizerEffectNoCreate != null) {
                        mVirtualizerInstances.remove(Integer.valueOf(i), virtualizerEffectNoCreate);
                        virtualizerEffectNoCreate.setEnabled(false);
                        virtualizerEffectNoCreate.release();
                    }
                    BassBoost bassBoostEffectNoCreate = getBassBoostEffectNoCreate(i);
                    if (bassBoostEffectNoCreate != null) {
                        mBassBoostInstances.remove(Integer.valueOf(i), bassBoostEffectNoCreate);
                        bassBoostEffectNoCreate.setEnabled(false);
                        bassBoostEffectNoCreate.release();
                    }
                    Equalizer equalizerEffectNoCreate = getEqualizerEffectNoCreate(i);
                    if (equalizerEffectNoCreate != null) {
                        mEQInstances.remove(Integer.valueOf(i), equalizerEffectNoCreate);
                        equalizerEffectNoCreate.setEnabled(false);
                        equalizerEffectNoCreate.release();
                    }
                }
                Log.v("MusicFXControlPanelEffect", "processingEnabled=false");
            }
            enabled = s;
        } else if (controlMode != ControlMode.CONTROL_EFFECTS || !sharedPreferences.getBoolean(Key.global_enabled.toString(), false)) {
            enabled = z;
        } else {
            switch (key) {
                case global_enabled:
                    enabled = z;
                    break;
                case virt_enabled:
                    Virtualizer virtualizerEffect2 = getVirtualizerEffect(i);
                    if (virtualizerEffect2 != null) {
                        virtualizerEffect2.setEnabled(z);
                        enabled = virtualizerEffect2.getEnabled();
                        break;
                    }
                    break;
                case bb_enabled:
                    BassBoost bassBoostEffect2 = getBassBoostEffect(i);
                    if (bassBoostEffect2 != null) {
                        bassBoostEffect2.setEnabled(z);
                        enabled = bassBoostEffect2.getEnabled();
                        break;
                    }
                    break;
                case eq_enabled:
                    Equalizer equalizerEffect2 = getEqualizerEffect(i);
                    if (equalizerEffect2 != null) {
                        equalizerEffect2.setEnabled(z);
                        enabled = equalizerEffect2.getEnabled();
                        break;
                    }
                    break;
                case pr_enabled:
                    enabled = z;
                    break;
                default:
                    Log.e("MusicFXControlPanelEffect", "Unknown/unsupported key " + key);
                    break;
            }
            return;
        }
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.putBoolean(key.toString(), enabled);
        editorEdit.commit();
    }

    public static Boolean getParameterBoolean(Context context, String str, int i, Key key) {
        boolean z;
        try {
            z = context.getSharedPreferences(str, 0).getBoolean(key.toString(), false);
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "getParameterBoolean: " + key + "; false; " + e);
            z = false;
        }
        return Boolean.valueOf(z);
    }

    public static void setParameterInt(Context context, String str, int i, Key key, int i2, int i3) {
        short s;
        int roundedStrength;
        short s2;
        String string = key.toString();
        short s3 = 0;
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(str, 0);
            SharedPreferences.Editor editorEdit = sharedPreferences.edit();
            if (getControlMode(i) == ControlMode.CONTROL_EFFECTS) {
                switch (key) {
                    case virt_strength:
                        Virtualizer virtualizerEffect = getVirtualizerEffect(i);
                        if (virtualizerEffect != null) {
                            virtualizerEffect.setStrength((short) i2);
                            roundedStrength = virtualizerEffect.getRoundedStrength();
                        }
                        editorEdit.putInt(string, roundedStrength);
                        editorEdit.apply();
                    case bb_strength:
                        BassBoost bassBoostEffect = getBassBoostEffect(i);
                        if (bassBoostEffect != null) {
                            bassBoostEffect.setStrength((short) i2);
                            roundedStrength = bassBoostEffect.getRoundedStrength();
                        }
                        editorEdit.putInt(string, roundedStrength);
                        editorEdit.apply();
                    case eq_band_level:
                        if (i3 == -1) {
                            throw new IllegalArgumentException("Dummy arg passed.");
                        }
                        short s4 = (short) i3;
                        string = string + ((int) s4);
                        Equalizer equalizerEffect = getEqualizerEffect(i);
                        if (equalizerEffect != null) {
                            equalizerEffect.setBandLevel(s4, (short) i2);
                            short bandLevel = equalizerEffect.getBandLevel(s4);
                            editorEdit.putInt(Key.eq_preset_user_band_level.toString() + ((int) s4), bandLevel);
                            roundedStrength = bandLevel;
                        }
                        editorEdit.putInt(string, roundedStrength);
                        editorEdit.apply();
                    case eq_current_preset:
                        Equalizer equalizerEffect2 = getEqualizerEffect(i);
                        if (equalizerEffect2 != null) {
                            short s5 = (short) i2;
                            int i4 = sharedPreferences.getInt(Key.eq_num_bands.toString(), 5);
                            int i5 = sharedPreferences.getInt(Key.eq_num_presets.toString(), 0);
                            if (s5 < i5) {
                                equalizerEffect2.usePreset(s5);
                                roundedStrength = equalizerEffect2.getCurrentPreset();
                            } else {
                                short[] sArrCopyOf = Arrays.copyOf(EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, i4);
                                short[] sArrCopyOf2 = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, i4);
                                for (short s6 = 0; s6 < i4; s6 = (short) (s6 + 1)) {
                                    if (s5 == i5) {
                                        s2 = (short) sharedPreferences.getInt(Key.eq_preset_ci_extreme_band_level.toString() + ((int) s6), sArrCopyOf[s6]);
                                    } else {
                                        s2 = (short) sharedPreferences.getInt(Key.eq_preset_user_band_level.toString() + ((int) s6), sArrCopyOf2[s6]);
                                    }
                                    equalizerEffect2.setBandLevel(s6, s2);
                                }
                                roundedStrength = i2;
                            }
                            while (s3 < i4) {
                                editorEdit.putInt(Key.eq_band_level.toString() + ((int) s3), equalizerEffect2.getBandLevel(s3));
                                s3 = (short) (s3 + 1);
                            }
                        }
                        editorEdit.putInt(string, roundedStrength);
                        editorEdit.apply();
                    case eq_preset_user_band_level:
                    case eq_preset_user_band_level_default:
                    case eq_preset_ci_extreme_band_level:
                        if (i3 == -1) {
                            throw new IllegalArgumentException("Dummy arg passed.");
                        }
                        string = string + ((int) ((short) i3));
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
                        break;
                    case bb_strength:
                        break;
                    case eq_band_level:
                        if (i3 == -1) {
                            throw new IllegalArgumentException("Dummy arg passed.");
                        }
                        short s7 = (short) i3;
                        string = string + ((int) s7);
                        editorEdit.putInt(Key.eq_preset_user_band_level.toString() + ((int) s7), i2);
                        break;
                        break;
                    case eq_current_preset:
                        short s8 = (short) i2;
                        int i6 = sharedPreferences.getInt(Key.eq_num_bands.toString(), 5);
                        int i7 = sharedPreferences.getInt(Key.eq_num_presets.toString(), 0);
                        short[][] sArr = (short[][]) Arrays.copyOf(mEQPresetOpenSLESBandLevel, i7);
                        short[] sArrCopyOf3 = Arrays.copyOf(EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, i6);
                        short[] sArrCopyOf4 = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, i6);
                        while (s3 < i6) {
                            if (s8 < i7) {
                                s = (short) sharedPreferences.getInt(Key.eq_preset_opensl_es_band_level.toString() + ((int) s8) + "_" + ((int) s3), sArr[s8][s3]);
                            } else if (s8 == i7) {
                                s = (short) sharedPreferences.getInt(Key.eq_preset_ci_extreme_band_level.toString() + ((int) s3), sArrCopyOf3[s3]);
                            } else {
                                s = (short) sharedPreferences.getInt(Key.eq_preset_user_band_level.toString() + ((int) s3), sArrCopyOf4[s3]);
                            }
                            editorEdit.putInt(Key.eq_band_level.toString() + ((int) s3), s);
                            s3 = (short) (s3 + 1);
                        }
                        break;
                    case eq_preset_user_band_level:
                    case eq_preset_user_band_level_default:
                    case eq_preset_ci_extreme_band_level:
                        if (i3 == -1) {
                            throw new IllegalArgumentException("Dummy arg passed.");
                        }
                        string = string + ((int) ((short) i3));
                        break;
                        break;
                    case pr_current_preset:
                        break;
                    case virt_type:
                        break;
                    default:
                        Log.e("MusicFXControlPanelEffect", "setParameterInt: Unknown/unsupported key " + key);
                        return;
                }
            }
            roundedStrength = i2;
            editorEdit.putInt(string, roundedStrength);
            editorEdit.apply();
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "setParameterInt: " + key + "; " + i2 + "; " + i3 + "; " + e);
        }
    }

    public static void setParameterInt(Context context, String str, int i, Key key, int i2) {
        setParameterInt(context, str, i, key, i2, -1);
    }

    public static int getParameterInt(Context context, String str, int i, String str2) {
        try {
            return context.getSharedPreferences(str, 0).getInt(str2, 0);
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "getParameterInt: " + str2 + "; " + e);
            return 0;
        }
    }

    public static int getParameterInt(Context context, String str, int i, Key key) {
        return getParameterInt(context, str, i, key.toString());
    }

    public static int[] getParameterIntArray(Context context, String str, int i, Key key) {
        int[] iArr;
        SharedPreferences sharedPreferences = context.getSharedPreferences(str, 0);
        int[] iArr2 = null;
        try {
            switch (key) {
                case eq_band_level:
                case eq_preset_user_band_level:
                case eq_preset_user_band_level_default:
                case eq_preset_ci_extreme_band_level:
                case eq_center_freq:
                    iArr = new int[sharedPreferences.getInt(Key.eq_num_bands.toString(), 0)];
                    break;
                case eq_current_preset:
                case pr_current_preset:
                case virt_type:
                default:
                    Log.e("MusicFXControlPanelEffect", "getParameterIntArray: Unknown/unsupported key " + key);
                    return null;
                case eq_level_range:
                    iArr = new int[2];
                    break;
            }
            iArr2 = iArr;
            for (int i2 = 0; i2 < iArr2.length; i2++) {
                iArr2[i2] = sharedPreferences.getInt(key.toString() + i2, 0);
            }
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "getParameterIntArray: " + key + "; " + e);
        }
        return iArr2;
    }

    public static String getParameterString(Context context, String str, int i, String str2) {
        try {
            return context.getSharedPreferences(str, 0).getString(str2, "");
        } catch (RuntimeException e) {
            Log.e("MusicFXControlPanelEffect", "getParameterString: " + str2 + "; " + e);
            return "";
        }
    }

    public static String getParameterString(Context context, String str, int i, Key key, int i2) {
        return getParameterString(context, str, i, key.toString() + i2);
    }

    public static void openSession(Context context, String str, int i) {
        boolean z;
        short currentPreset;
        short s;
        short[] sArr;
        short s2;
        int[] iArr;
        short s3;
        String[] strArr;
        short roundedStrength;
        Log.v("MusicFXControlPanelEffect", "openSession(" + context + ", " + str + ", " + i + ")");
        SharedPreferences sharedPreferences = context.getSharedPreferences(str, 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        boolean z2 = sharedPreferences.getBoolean(Key.global_enabled.toString(), false);
        editorEdit.putBoolean(Key.global_enabled.toString(), z2);
        if (!z2) {
            return;
        }
        try {
            Integer numPutIfAbsent = mPackageSessions.putIfAbsent(str, Integer.valueOf(i));
            if (numPutIfAbsent != null) {
                if (numPutIfAbsent.intValue() != i) {
                    closeSession(context, str, numPutIfAbsent.intValue());
                    z = false;
                } else {
                    z = true;
                }
            } else {
                z = false;
            }
            Virtualizer virtualizerEffect = getVirtualizerEffect(i);
            try {
                boolean z3 = sharedPreferences.getBoolean(Key.virt_enabled.toString(), true);
                if (!z) {
                    roundedStrength = virtualizerEffect.getRoundedStrength();
                } else {
                    roundedStrength = 0;
                }
                virtualizerEffect.setProperties(new Virtualizer.Settings("Virtualizer;strength=" + sharedPreferences.getInt(Key.virt_strength.toString(), roundedStrength)));
                if (z2) {
                    virtualizerEffect.setEnabled(z3);
                } else {
                    virtualizerEffect.setEnabled(false);
                }
                Virtualizer.Settings properties = virtualizerEffect.getProperties();
                Log.v("MusicFXControlPanelEffect", "Parameters: " + properties.toString() + ";enabled=" + z3);
                editorEdit.putBoolean(Key.virt_enabled.toString(), z3);
                editorEdit.putInt(Key.virt_strength.toString(), properties.strength);
            } catch (RuntimeException e) {
                Log.e("MusicFXControlPanelEffect", "openSession: Virtualizer error: " + e);
            }
            if (z) {
                editorEdit.apply();
                return;
            }
            BassBoost bassBoostEffect = getBassBoostEffect(i);
            try {
                boolean z4 = sharedPreferences.getBoolean(Key.bb_enabled.toString(), true);
                bassBoostEffect.setProperties(new BassBoost.Settings("BassBoost;strength=" + sharedPreferences.getInt(Key.bb_strength.toString(), 667)));
                if (z2) {
                    bassBoostEffect.setEnabled(z4);
                } else {
                    bassBoostEffect.setEnabled(false);
                }
                BassBoost.Settings properties2 = bassBoostEffect.getProperties();
                Log.v("MusicFXControlPanelEffect", "Parameters: " + properties2.toString() + ";enabled=" + z4);
                editorEdit.putBoolean(Key.bb_enabled.toString(), z4);
                editorEdit.putInt(Key.bb_strength.toString(), properties2.strength);
            } catch (RuntimeException e2) {
                Log.e("MusicFXControlPanelEffect", "openSession: BassBoost error: " + e2);
            }
            Equalizer equalizerEffect = getEqualizerEffect(i);
            try {
                synchronized (mEQInitLock) {
                    mEQBandLevelRange = equalizerEffect.getBandLevelRange();
                    mEQNumBands = equalizerEffect.getNumberOfBands();
                    mEQCenterFreq = new int[mEQNumBands];
                    mEQNumPresets = equalizerEffect.getNumberOfPresets();
                    mEQPresetNames = new String[mEQNumPresets];
                    for (short s4 = 0; s4 < mEQNumPresets; s4 = (short) (s4 + 1)) {
                        mEQPresetNames[s4] = equalizerEffect.getPresetName(s4);
                        editorEdit.putString(Key.eq_preset_name.toString() + ((int) s4), mEQPresetNames[s4]);
                    }
                    editorEdit.putInt(Key.eq_level_range.toString() + 0, mEQBandLevelRange[0]);
                    editorEdit.putInt(Key.eq_level_range.toString() + 1, mEQBandLevelRange[1]);
                    editorEdit.putInt(Key.eq_num_bands.toString(), mEQNumBands);
                    editorEdit.putInt(Key.eq_num_presets.toString(), mEQNumPresets);
                    short[] sArrCopyOf = Arrays.copyOf(EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, (int) mEQNumBands);
                    short[] sArrCopyOf2 = Arrays.copyOf(EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, (int) mEQNumBands);
                    currentPreset = (short) sharedPreferences.getInt(Key.eq_current_preset.toString(), mEQNumPresets);
                    if (currentPreset < mEQNumPresets) {
                        equalizerEffect.usePreset(currentPreset);
                        currentPreset = equalizerEffect.getCurrentPreset();
                    } else {
                        for (short s5 = 0; s5 < mEQNumBands; s5 = (short) (s5 + 1)) {
                            if (currentPreset == mEQNumPresets) {
                                s = sArrCopyOf[s5];
                            } else {
                                s = (short) sharedPreferences.getInt(Key.eq_preset_user_band_level.toString() + ((int) s5), sArrCopyOf2[s5]);
                            }
                            equalizerEffect.setBandLevel(s5, s);
                        }
                    }
                    editorEdit.putInt(Key.eq_current_preset.toString(), currentPreset);
                    sArr = new short[mEQNumBands];
                    for (short s6 = 0; s6 < mEQNumBands; s6 = (short) (s6 + 1)) {
                        mEQCenterFreq[s6] = equalizerEffect.getCenterFreq(s6);
                        sArr[s6] = equalizerEffect.getBandLevel(s6);
                        editorEdit.putInt(Key.eq_band_level.toString() + ((int) s6), sArr[s6]);
                        editorEdit.putInt(Key.eq_center_freq.toString() + ((int) s6), mEQCenterFreq[s6]);
                        editorEdit.putInt(Key.eq_preset_ci_extreme_band_level.toString() + ((int) s6), sArrCopyOf[s6]);
                        editorEdit.putInt(Key.eq_preset_user_band_level_default.toString() + ((int) s6), sArrCopyOf2[s6]);
                    }
                    s2 = mEQNumBands;
                    iArr = mEQCenterFreq;
                    s3 = mEQNumPresets;
                    strArr = mEQPresetNames;
                }
                boolean z5 = sharedPreferences.getBoolean(Key.eq_enabled.toString(), true);
                editorEdit.putBoolean(Key.eq_enabled.toString(), z5);
                if (z2) {
                    equalizerEffect.setEnabled(z5);
                } else {
                    equalizerEffect.setEnabled(false);
                }
                Log.v("MusicFXControlPanelEffect", "Parameters: Equalizer");
                Log.v("MusicFXControlPanelEffect", "bands=" + ((int) s2));
                String str2 = "levels=";
                for (short s7 = (short) 0; s7 < s2; s7 = (short) (s7 + 1)) {
                    str2 = str2 + ((int) sArr[s7]) + "; ";
                }
                Log.v("MusicFXControlPanelEffect", str2);
                String str3 = "center=";
                for (short s8 = 0; s8 < s2; s8 = (short) (s8 + 1)) {
                    str3 = str3 + iArr[s8] + "; ";
                }
                Log.v("MusicFXControlPanelEffect", str3);
                String str4 = "presets=";
                for (short s9 = 0; s9 < s3; s9 = (short) (s9 + 1)) {
                    str4 = str4 + strArr[s9] + "; ";
                }
                Log.v("MusicFXControlPanelEffect", str4);
                Log.v("MusicFXControlPanelEffect", "current=" + ((int) currentPreset));
            } catch (RuntimeException e3) {
                Log.e("MusicFXControlPanelEffect", "openSession: Equalizer error: " + e3);
            }
            editorEdit.commit();
        } catch (NullPointerException e4) {
            Log.e("MusicFXControlPanelEffect", "openSession: " + e4);
            editorEdit.commit();
        }
    }

    public static void closeSession(Context context, String str, int i) {
        Log.v("MusicFXControlPanelEffect", "closeSession(" + context + ", " + str + ", " + i + ")");
        PresetReverb presetReverbRemove = mPresetReverbInstances.remove(Integer.valueOf(i));
        if (presetReverbRemove != null) {
            presetReverbRemove.release();
        }
        Equalizer equalizerRemove = mEQInstances.remove(Integer.valueOf(i));
        if (equalizerRemove != null) {
            equalizerRemove.release();
        }
        BassBoost bassBoostRemove = mBassBoostInstances.remove(Integer.valueOf(i));
        if (bassBoostRemove != null) {
            bassBoostRemove.release();
        }
        Virtualizer virtualizerRemove = mVirtualizerInstances.remove(Integer.valueOf(i));
        if (virtualizerRemove != null) {
            virtualizerRemove.release();
        }
        mPackageSessions.remove(str);
    }

    private static Virtualizer getVirtualizerEffectNoCreate(int i) {
        return mVirtualizerInstances.get(Integer.valueOf(i));
    }

    private static Virtualizer getVirtualizerEffect(int i) {
        Virtualizer virtualizerEffectNoCreate = getVirtualizerEffectNoCreate(i);
        if (virtualizerEffectNoCreate == null) {
            try {
                Virtualizer virtualizer = new Virtualizer(0, i);
                Virtualizer virtualizerPutIfAbsent = mVirtualizerInstances.putIfAbsent(Integer.valueOf(i), virtualizer);
                return virtualizerPutIfAbsent == null ? virtualizer : virtualizerPutIfAbsent;
            } catch (IllegalArgumentException e) {
                Log.e("MusicFXControlPanelEffect", "Virtualizer: " + e);
                return virtualizerEffectNoCreate;
            } catch (UnsupportedOperationException e2) {
                Log.e("MusicFXControlPanelEffect", "Virtualizer: " + e2);
                return virtualizerEffectNoCreate;
            } catch (RuntimeException e3) {
                Log.e("MusicFXControlPanelEffect", "Virtualizer: " + e3);
                return virtualizerEffectNoCreate;
            }
        }
        return virtualizerEffectNoCreate;
    }

    private static BassBoost getBassBoostEffectNoCreate(int i) {
        return mBassBoostInstances.get(Integer.valueOf(i));
    }

    private static BassBoost getBassBoostEffect(int i) {
        BassBoost bassBoostEffectNoCreate = getBassBoostEffectNoCreate(i);
        if (bassBoostEffectNoCreate == null) {
            try {
                BassBoost bassBoost = new BassBoost(0, i);
                BassBoost bassBoostPutIfAbsent = mBassBoostInstances.putIfAbsent(Integer.valueOf(i), bassBoost);
                return bassBoostPutIfAbsent == null ? bassBoost : bassBoostPutIfAbsent;
            } catch (IllegalArgumentException e) {
                Log.e("MusicFXControlPanelEffect", "BassBoost: " + e);
                return bassBoostEffectNoCreate;
            } catch (UnsupportedOperationException e2) {
                Log.e("MusicFXControlPanelEffect", "BassBoost: " + e2);
                return bassBoostEffectNoCreate;
            } catch (RuntimeException e3) {
                Log.e("MusicFXControlPanelEffect", "BassBoost: " + e3);
                return bassBoostEffectNoCreate;
            }
        }
        return bassBoostEffectNoCreate;
    }

    private static Equalizer getEqualizerEffectNoCreate(int i) {
        return mEQInstances.get(Integer.valueOf(i));
    }

    private static Equalizer getEqualizerEffect(int i) {
        Equalizer equalizerEffectNoCreate = getEqualizerEffectNoCreate(i);
        if (equalizerEffectNoCreate == null) {
            try {
                Equalizer equalizer = new Equalizer(0, i);
                Equalizer equalizerPutIfAbsent = mEQInstances.putIfAbsent(Integer.valueOf(i), equalizer);
                return equalizerPutIfAbsent == null ? equalizer : equalizerPutIfAbsent;
            } catch (IllegalArgumentException e) {
                Log.e("MusicFXControlPanelEffect", "Equalizer: " + e);
                return equalizerEffectNoCreate;
            } catch (UnsupportedOperationException e2) {
                Log.e("MusicFXControlPanelEffect", "Equalizer: " + e2);
                return equalizerEffectNoCreate;
            } catch (RuntimeException e3) {
                Log.e("MusicFXControlPanelEffect", "Equalizer: " + e3);
                return equalizerEffectNoCreate;
            }
        }
        return equalizerEffectNoCreate;
    }
}
