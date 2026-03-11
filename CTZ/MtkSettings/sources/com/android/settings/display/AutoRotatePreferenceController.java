package com.android.settings.display;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.internal.view.RotationPolicy;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AutoRotatePreferenceController extends TogglePreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private Preference mPreference;
    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;

    public AutoRotatePreferenceController(Context context, String str) {
        super(context, str);
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public void updateState(Preference preference) {
        this.mPreference = preference;
        super.updateState(preference);
    }

    @Override
    public void onResume() {
        if (this.mRotationPolicyListener == null) {
            this.mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                public void onChange() {
                    if (AutoRotatePreferenceController.this.mPreference != null) {
                        AutoRotatePreferenceController.this.updateState(AutoRotatePreferenceController.this.mPreference);
                    }
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
    }

    @Override
    public void onPause() {
        if (this.mRotationPolicyListener != null) {
            RotationPolicy.unregisterRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return RotationPolicy.isRotationLockToggleVisible(this.mContext) ? 0 : 2;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "auto_rotate");
    }

    @Override
    public boolean isChecked() {
        return !RotationPolicy.isRotationLocked(this.mContext);
    }

    @Override
    public boolean setChecked(boolean z) {
        boolean z2 = !z;
        this.mMetricsFeatureProvider.action(this.mContext, 203, z2);
        RotationPolicy.setRotationLock(this.mContext, z2);
        return true;
    }
}
