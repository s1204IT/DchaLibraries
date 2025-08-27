package com.android.settings.dream;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;
import java.util.Optional;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class CurrentDreamPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final DreamBackend mBackend;

    public CurrentDreamPreferenceController(Context context) {
        super(context);
        this.mBackend = DreamBackend.getInstance(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mBackend.getDreamInfos().size() > 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "current_screensaver";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
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
                gearPreference.setOnGearClickListener(new GearPreference.OnGearClickListener() { // from class: com.android.settings.dream.-$$Lambda$CurrentDreamPreferenceController$faOOwvjkeM0i38i1bxACLza6vQ4
                    @Override // com.android.settings.widget.GearPreference.OnGearClickListener
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
        return this.mBackend.getDreamInfos().stream().filter(new Predicate() { // from class: com.android.settings.dream.-$$Lambda$CurrentDreamPreferenceController$JJd0D4Ql1FstWgOpYrMCLEB2pnU
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((DreamBackend.DreamInfo) obj).isActive;
            }
        }).findFirst();
    }
}
