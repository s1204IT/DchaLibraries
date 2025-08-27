package com.android.settings.display;

import android.content.Context;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class NightDisplayPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public NightDisplayPreferenceController(Context context) {
        super(context);
    }

    public static boolean isSuggestionComplete(Context context) {
        return (context.getResources().getBoolean(R.bool.config_night_light_suggestion_enabled) && new ColorDisplayController(context).getAutoMode() == 0) ? false : true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return ColorDisplayController.isAvailable(this.mContext);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "night_display";
    }
}
