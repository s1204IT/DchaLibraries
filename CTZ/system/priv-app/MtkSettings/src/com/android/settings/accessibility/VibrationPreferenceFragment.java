package com.android.settings.accessibility;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.accessibility.VibrationPreferenceFragment;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/* loaded from: classes.dex */
public abstract class VibrationPreferenceFragment extends RadioButtonPickerFragment {
    static final String KEY_INTENSITY_HIGH = "intensity_high";
    static final String KEY_INTENSITY_LOW = "intensity_low";
    static final String KEY_INTENSITY_MEDIUM = "intensity_medium";
    static final String KEY_INTENSITY_OFF = "intensity_off";
    static final String KEY_INTENSITY_ON = "intensity_on";
    private final Map<String, VibrationIntensityCandidateInfo> mCandidates = new ArrayMap();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    protected abstract int getDefaultVibrationIntensity();

    protected abstract String getVibrationIntensitySetting();

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mSettingsObserver.register();
        if (this.mCandidates.isEmpty()) {
            loadCandidates(context);
        }
    }

    private void loadCandidates(Context context) {
        if (context.getResources().getBoolean(R.bool.config_vibration_supports_multiple_intensities)) {
            this.mCandidates.put(KEY_INTENSITY_OFF, new VibrationIntensityCandidateInfo(KEY_INTENSITY_OFF, R.string.accessibility_vibration_intensity_off, 0));
            this.mCandidates.put(KEY_INTENSITY_LOW, new VibrationIntensityCandidateInfo(KEY_INTENSITY_LOW, R.string.accessibility_vibration_intensity_low, 1));
            this.mCandidates.put(KEY_INTENSITY_MEDIUM, new VibrationIntensityCandidateInfo(KEY_INTENSITY_MEDIUM, R.string.accessibility_vibration_intensity_medium, 2));
            this.mCandidates.put(KEY_INTENSITY_HIGH, new VibrationIntensityCandidateInfo(KEY_INTENSITY_HIGH, R.string.accessibility_vibration_intensity_high, 3));
            return;
        }
        this.mCandidates.put(KEY_INTENSITY_OFF, new VibrationIntensityCandidateInfo(KEY_INTENSITY_OFF, R.string.switch_off_text, 0));
        this.mCandidates.put(KEY_INTENSITY_ON, new VibrationIntensityCandidateInfo(KEY_INTENSITY_ON, R.string.switch_on_text, getDefaultVibrationIntensity()));
    }

    @Override // android.app.Fragment
    public void onDetach() {
        super.onDetach();
        this.mSettingsObserver.unregister();
    }

    protected void onVibrationIntensitySelected(int i) {
    }

    protected void playVibrationPreview() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Vibrator.class);
        VibrationEffect vibrationEffect = VibrationEffect.get(0);
        AudioAttributes.Builder builder = new AudioAttributes.Builder();
        builder.setUsage(getPreviewVibrationAudioAttributesUsage());
        vibrator.vibrate(vibrationEffect, builder.build());
    }

    protected int getPreviewVibrationAudioAttributesUsage() {
        return 0;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected List<? extends CandidateInfo> getCandidates() {
        ArrayList arrayList = new ArrayList(this.mCandidates.values());
        arrayList.sort(Comparator.comparing(new Function() { // from class: com.android.settings.accessibility.-$$Lambda$_Oh9z60fg9jQX72D1CuzQSHZqtM
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return Integer.valueOf(((VibrationPreferenceFragment.VibrationIntensityCandidateInfo) obj).getIntensity());
            }
        }).reversed());
        return arrayList;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getDefaultKey() {
        int i = Settings.System.getInt(getContext().getContentResolver(), getVibrationIntensitySetting(), getDefaultVibrationIntensity());
        for (VibrationIntensityCandidateInfo vibrationIntensityCandidateInfo : this.mCandidates.values()) {
            boolean z = false;
            boolean z2 = vibrationIntensityCandidateInfo.getIntensity() == i;
            if (vibrationIntensityCandidateInfo.getKey().equals(KEY_INTENSITY_ON) && i != 0) {
                z = true;
            }
            if (z2 || z) {
                return vibrationIntensityCandidateInfo.getKey();
            }
        }
        return null;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected boolean setDefaultKey(String str) {
        VibrationIntensityCandidateInfo vibrationIntensityCandidateInfo = this.mCandidates.get(str);
        if (vibrationIntensityCandidateInfo == null) {
            Log.e("VibrationPreferenceFragment", "Tried to set unknown intensity (key=" + str + ")!");
            return false;
        }
        Settings.System.putInt(getContext().getContentResolver(), getVibrationIntensitySetting(), vibrationIntensityCandidateInfo.getIntensity());
        onVibrationIntensitySelected(vibrationIntensityCandidateInfo.getIntensity());
        return true;
    }

    class VibrationIntensityCandidateInfo extends CandidateInfo {
        private int mIntensity;
        private String mKey;
        private int mLabelId;

        public VibrationIntensityCandidateInfo(String str, int i, int i2) {
            super(true);
            this.mKey = str;
            this.mLabelId = i;
            this.mIntensity = i2;
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public CharSequence loadLabel() {
            return VibrationPreferenceFragment.this.getContext().getString(this.mLabelId);
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public Drawable loadIcon() {
            return null;
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public String getKey() {
            return this.mKey;
        }

        public int getIntensity() {
            return this.mIntensity;
        }
    }

    private class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(new Handler());
        }

        public void register() {
            VibrationPreferenceFragment.this.getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor(VibrationPreferenceFragment.this.getVibrationIntensitySetting()), false, this);
        }

        public void unregister() {
            VibrationPreferenceFragment.this.getContext().getContentResolver().unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            VibrationPreferenceFragment.this.updateCandidates();
            VibrationPreferenceFragment.this.playVibrationPreview();
        }
    }
}
