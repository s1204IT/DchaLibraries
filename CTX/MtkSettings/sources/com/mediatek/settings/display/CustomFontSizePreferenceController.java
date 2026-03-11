package com.mediatek.settings.display;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.accessibility.CustomToggleFontSizePreferenceFragment;
import com.mediatek.settings.ext.IDisplaySettingsExt;

public class CustomFontSizePreferenceController extends BasePreferenceController {
    private static final String KEY_CUSTOM_FONT_SIZE = "custom_font_size";
    private IDisplaySettingsExt customFontSizePref;
    private Context mContext;

    public CustomFontSizePreferenceController(Context context, String str) {
        super(context, str);
        this.mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        this.customFontSizePref = UtilsExt.getDisplaySettingsExt(this.mContext);
        if (this.customFontSizePref.isCustomPrefPresent()) {
            return 0;
        }
        return 2;
    }

    @Override
    public CharSequence getSummary() {
        float f = Settings.System.getFloat(this.mContext.getContentResolver(), "font_scale", 1.0f);
        Resources resources = this.mContext.getResources();
        return resources.getStringArray(R.array.custom_entries_font_size)[CustomToggleFontSizePreferenceFragment.fontSizeValueToIndex(f, resources.getStringArray(R.array.custom_entryvalues_font_size))];
    }
}
