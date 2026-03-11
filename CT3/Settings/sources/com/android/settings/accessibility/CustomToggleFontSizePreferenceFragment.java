package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import com.android.settings.CustomPreviewSeekBarPreferenceFragment;
import com.android.settings.R;

public class CustomToggleFontSizePreferenceFragment extends CustomPreviewSeekBarPreferenceFragment {
    private float[] mValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mActivityLayoutResId = R.layout.font_size_activity;
        this.mPreviewSampleResIds = new int[]{R.layout.font_size_preview};
        Resources res = getContext().getResources();
        ContentResolver resolver = getContext().getContentResolver();
        this.mEntries = res.getStringArray(R.array.custom_entries_font_size);
        String[] strEntryValues = res.getStringArray(R.array.custom_entryvalues_font_size);
        float currentScale = Settings.System.getFloat(resolver, "font_scale", 1.0f);
        this.mInitialIndex = fontSizeValueToIndex(currentScale, strEntryValues);
        this.mValues = new float[strEntryValues.length];
        for (int i = 0; i < strEntryValues.length; i++) {
            this.mValues[i] = Float.parseFloat(strEntryValues[i]);
        }
    }

    @Override
    protected Configuration createConfig(Configuration origConfig, int index) {
        Configuration config = new Configuration(origConfig);
        config.fontScale = this.mValues[index];
        return config;
    }

    @Override
    protected void commit() {
        if (getContext() == null) {
            return;
        }
        ContentResolver resolver = getContext().getContentResolver();
        Settings.System.putFloat(resolver, "font_scale", this.mValues[this.mCurrentIndex]);
    }

    @Override
    protected int getMetricsCategory() {
        return 340;
    }

    public static int fontSizeValueToIndex(float val, String[] indices) {
        float lastVal = Float.parseFloat(indices[0]);
        for (int i = 1; i < indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < ((thisVal - lastVal) * 0.5f) + lastVal) {
                return i - 1;
            }
            lastVal = thisVal;
        }
        return indices.length - 1;
    }
}
