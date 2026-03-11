package com.mediatek.settings.accessibility;

import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import com.android.settings.R;
import com.mediatek.settings.CustomPreviewSeekBarPreferenceFragment;

public class CustomToggleFontSizePreferenceFragment extends CustomPreviewSeekBarPreferenceFragment {
    private float[] mValues;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mActivityLayoutResId = R.layout.font_size_activity;
        this.mPreviewSampleResIds = new int[]{R.layout.font_size_preview};
        Resources resources = getContext().getResources();
        ContentResolver contentResolver = getContext().getContentResolver();
        this.mEntries = resources.getStringArray(R.array.custom_entries_font_size);
        String[] stringArray = resources.getStringArray(R.array.custom_entryvalues_font_size);
        this.mInitialIndex = fontSizeValueToIndex(Settings.System.getFloat(contentResolver, "font_scale", 1.0f), stringArray);
        this.mValues = new float[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            this.mValues[i] = Float.parseFloat(stringArray[i]);
        }
        getActivity().setTitle(R.string.title_font_size);
    }

    @Override
    protected Configuration createConfig(Configuration configuration, int i) {
        Configuration configuration2 = new Configuration(configuration);
        configuration2.fontScale = this.mValues[i];
        return configuration2;
    }

    @Override
    protected void commit() {
        if (getContext() == null) {
            return;
        }
        Settings.System.putFloat(getContext().getContentResolver(), "font_scale", this.mValues[this.mCurrentIndex]);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_font_size;
    }

    @Override
    public int getMetricsCategory() {
        return 340;
    }

    public static int fontSizeValueToIndex(float f, String[] strArr) {
        float f2 = Float.parseFloat(strArr[0]);
        int i = 1;
        while (i < strArr.length) {
            float f3 = Float.parseFloat(strArr[i]);
            if (f >= f2 + ((f3 - f2) * 0.5f)) {
                i++;
                f2 = f3;
            } else {
                return i - 1;
            }
        }
        return strArr.length - 1;
    }
}
