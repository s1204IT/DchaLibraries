package com.android.systemui.tuner;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService;
import java.util.Set;

public class StatusBarSwitch extends SwitchPreference implements TunerService.Tunable {
    private Set<String> mBlacklist;

    public StatusBarSwitch(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
    }

    @Override
    public void onDetached() {
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        super.onDetached();
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        if (!"icon_blacklist".equals(str)) {
            return;
        }
        this.mBlacklist = StatusBarIconController.getIconBlacklist(str2);
        setChecked(!this.mBlacklist.contains(getKey()));
    }

    @Override
    protected boolean persistBoolean(boolean z) {
        if (!z) {
            if (!this.mBlacklist.contains(getKey())) {
                MetricsLogger.action(getContext(), 234, getKey());
                this.mBlacklist.add(getKey());
                setList(this.mBlacklist);
                return true;
            }
            return true;
        }
        if (this.mBlacklist.remove(getKey())) {
            MetricsLogger.action(getContext(), 233, getKey());
            setList(this.mBlacklist);
            return true;
        }
        return true;
    }

    private void setList(Set<String> set) {
        Settings.Secure.putStringForUser(getContext().getContentResolver(), "icon_blacklist", TextUtils.join(",", set), ActivityManager.getCurrentUser());
    }
}
