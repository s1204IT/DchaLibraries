package com.android.settings.language;

import android.content.Context;
import android.speech.tts.TtsEngines;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class TtsPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final TtsEngines mTtsEngines;

    public TtsPreferenceController(Context context, TtsEngines ttsEngines) {
        super(context);
        this.mTtsEngines = ttsEngines;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !this.mTtsEngines.getEngines().isEmpty() && this.mContext.getResources().getBoolean(R.bool.config_show_tts_settings_summary);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "tts_settings_summary";
    }
}
