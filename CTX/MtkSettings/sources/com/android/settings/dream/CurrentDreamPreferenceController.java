package com.android.settings.dream;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;
import java.util.Optional;
import java.util.function.Predicate;

public class CurrentDreamPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final DreamBackend mBackend;

    public CurrentDreamPreferenceController(Context context) {
        super(context);
        this.mBackend = DreamBackend.getInstance(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mBackend.getDreamInfos().size() > 0;
    }

    @Override
    public String getPreferenceKey() {
        return "current_screensaver";
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(this.mBackend.getActiveDreamName());
        setGearClickListenerForPreference(preference);
    }

    private void setGearClickListenerForPreference(Preference preference) {
        if (preference instanceof GearPreference) {
            GearPreference gearPreference = (GearPreference) preference;
            Optional<DreamBackend.DreamInfo> activeDreamInfo = getActiveDreamInfo();
            if (!activeDreamInfo.isPresent() || activeDreamInfo.get().settingsComponentName == null) {
                gearPreference.setOnGearClickListener(null);
            } else {
                gearPreference.setOnGearClickListener(new GearPreference.OnGearClickListener() {
                    @Override
                    public final void onGearClick(GearPreference gearPreference2) {
                        this.f$0.launchScreenSaverSettings();
                    }
                });
            }
        }
    }

    private void launchScreenSaverSettings() {
        Optional<DreamBackend.DreamInfo> activeDreamInfo = getActiveDreamInfo();
        if (activeDreamInfo.isPresent()) {
            this.mBackend.launchSettings(activeDreamInfo.get());
        }
    }

    private Optional<DreamBackend.DreamInfo> getActiveDreamInfo() {
        return this.mBackend.getDreamInfos().stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((DreamBackend.DreamInfo) obj).isActive;
            }
        }).findFirst();
    }
}
