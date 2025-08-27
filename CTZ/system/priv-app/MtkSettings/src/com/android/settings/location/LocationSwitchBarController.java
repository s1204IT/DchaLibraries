package com.android.settings.location;

import android.content.Context;
import android.os.UserHandle;
import android.widget.Switch;
import com.android.settings.location.LocationEnabler;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/* loaded from: classes.dex */
public class LocationSwitchBarController implements LocationEnabler.LocationModeChangeListener, SwitchBar.OnSwitchChangeListener, LifecycleObserver, OnStart, OnStop {
    private final LocationEnabler mLocationEnabler;
    private final Switch mSwitch;
    private final SwitchBar mSwitchBar;
    private boolean mValidListener;

    public LocationSwitchBarController(Context context, SwitchBar switchBar, Lifecycle lifecycle) {
        this.mSwitchBar = switchBar;
        this.mSwitch = this.mSwitchBar.getSwitch();
        this.mLocationEnabler = new LocationEnabler(context, this, lifecycle);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        if (!this.mValidListener) {
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mValidListener = true;
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        if (this.mValidListener) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mValidListener = false;
        }
    }

    @Override // com.android.settings.location.LocationEnabler.LocationModeChangeListener
    public void onLocationModeChanged(int i, boolean z) {
        boolean zIsEnabled = this.mLocationEnabler.isEnabled(i);
        int iMyUserId = UserHandle.myUserId();
        RestrictedLockUtils.EnforcedAdmin shareLocationEnforcedAdmin = this.mLocationEnabler.getShareLocationEnforcedAdmin(iMyUserId);
        if (!this.mLocationEnabler.hasShareLocationRestriction(iMyUserId) && shareLocationEnforcedAdmin != null) {
            this.mSwitchBar.setDisabledByAdmin(shareLocationEnforcedAdmin);
        } else {
            this.mSwitchBar.setEnabled(!z);
        }
        if (zIsEnabled != this.mSwitch.isChecked()) {
            if (this.mValidListener) {
                this.mSwitchBar.removeOnSwitchChangeListener(this);
            }
            this.mSwitch.setChecked(zIsEnabled);
            if (this.mValidListener) {
                this.mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
    }

    @Override // com.android.settings.widget.SwitchBar.OnSwitchChangeListener
    public void onSwitchChanged(Switch r1, boolean z) {
        this.mLocationEnabler.setLocationEnabled(z);
    }
}
