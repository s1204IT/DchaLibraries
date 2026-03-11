package com.android.settings.dream;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;

public class StartNowPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final DreamBackend mBackend;

    public StartNowPreferenceController(Context context) {
        super(context);
        this.mBackend = DreamBackend.getInstance(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "dream_start_now_button_container";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        ((Button) ((LayoutPreference) preferenceScreen.findPreference(getPreferenceKey())).findViewById(R.id.dream_start_now_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.mBackend.startDreaming();
            }
        });
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        ((Button) ((LayoutPreference) preference).findViewById(R.id.dream_start_now_button)).setEnabled(this.mBackend.getWhenToDreamSetting() != 3);
    }
}
