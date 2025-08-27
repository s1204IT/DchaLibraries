package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/* loaded from: classes.dex */
public class VisiblePatternProfilePreferenceController extends TogglePreferenceController implements LifecycleObserver, OnResume {
    private static final String KEY_VISIBLE_PATTERN_PROFILE = "visiblepattern_profile";
    private static final String TAG = "VisPtnProfPrefCtrl";
    private final LockPatternUtils mLockPatternUtils;
    private Preference mPreference;
    private final int mProfileChallengeUserId;
    private final UserManager mUm;
    private final int mUserId;

    public VisiblePatternProfilePreferenceController(Context context) {
        this(context, null);
    }

    public VisiblePatternProfilePreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY_VISIBLE_PATTERN_PROFILE);
        this.mUserId = UserHandle.myUserId();
        this.mUm = (UserManager) context.getSystemService("user");
        this.mLockPatternUtils = FeatureFactory.getFactory(context).getSecurityFeatureProvider().getLockPatternUtils(context);
        this.mProfileChallengeUserId = Utils.getManagedProfileId(this.mUm, this.mUserId);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        FutureTask futureTask = new FutureTask(new Callable() { // from class: com.android.settings.security.-$$Lambda$VisiblePatternProfilePreferenceController$rwDeZ_aTyFGsJcFkBXrMF4RE1tM
            @Override // java.util.concurrent.Callable
            public final Object call() {
                return VisiblePatternProfilePreferenceController.lambda$getAvailabilityStatus$0(this.f$0);
            }
        });
        try {
            futureTask.run();
            return ((Integer) futureTask.get()).intValue();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting lock pattern state.");
            return 3;
        }
    }

    public static /* synthetic */ Integer lambda$getAvailabilityStatus$0(VisiblePatternProfilePreferenceController visiblePatternProfilePreferenceController) throws Exception {
        boolean zIsSecure = visiblePatternProfilePreferenceController.mLockPatternUtils.isSecure(visiblePatternProfilePreferenceController.mProfileChallengeUserId);
        boolean z = visiblePatternProfilePreferenceController.mLockPatternUtils.getKeyguardStoredPasswordQuality(visiblePatternProfilePreferenceController.mProfileChallengeUserId) == 65536;
        if (zIsSecure && z) {
            return 0;
        }
        return 3;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return this.mLockPatternUtils.isVisiblePatternEnabled(this.mProfileChallengeUserId);
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        if (Utils.startQuietModeDialogIfNecessary(this.mContext, this.mUm, this.mProfileChallengeUserId)) {
            return false;
        }
        this.mLockPatternUtils.setVisiblePatternEnabled(z, this.mProfileChallengeUserId);
        return true;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        this.mPreference.setVisible(isAvailable());
    }
}
