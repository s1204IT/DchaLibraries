package com.android.settings.display;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import com.android.settings.core.BasePreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDisplaySettingsExt;

public class FontSizePreferenceController extends BasePreferenceController {
    private IDisplaySettingsExt customFontSizePref;
    private Context mContext;

    public FontSizePreferenceController(Context context, String str) {
        super(context, str);
        this.mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        this.customFontSizePref = UtilsExt.getDisplaySettingsExt(this.mContext);
        if (!this.customFontSizePref.isCustomPrefPresent()) {
            return 0;
        }
        return 2;
    }

    @Override
    public CharSequence getSummary() {
        float f = Settings.System.getFloat(this.mContext.getContentResolver(), "font_scale", 1.0f);
        Resources resources = this.mContext.getResources();
        return resources.getStringArray(R.array.entries_font_size)[ToggleFontSizePreferenceFragment.fontSizeValueToIndex(f, resources.getStringArray(R.array.entryvalues_font_size))];
    }
}
