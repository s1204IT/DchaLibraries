package com.mediatek.settings.accessibility;

import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import com.android.settings.R;
import com.mediatek.settings.CustomPreviewSeekBarPreferenceFragment;

/* loaded from: classes.dex */
public class CustomToggleFontSizePreferenceFragment extends CustomPreviewSeekBarPreferenceFragment {
    private float[] mValues;

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) throws Resources.NotFoundException {
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

    @Override // com.mediatek.settings.CustomPreviewSeekBarPreferenceFragment
    protected Configuration createConfig(Configuration configuration, int i) {
        Configuration configuration2 = new Configuration(configuration);
        configuration2.fontScale = this.mValues[i];
        return configuration2;
    }

    @Override // com.mediatek.settings.CustomPreviewSeekBarPreferenceFragment
    protected void commit() {
        if (getContext() == null) {
            return;
        }
        Settings.System.putFloat(getContext().getContentResolver(), "font_scale", this.mValues[this.mCurrentIndex]);
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_font_size;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 340;
    }

    public static int fontSizeValueToIndex(float f, String[] strArr) throws NumberFormatException {
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
