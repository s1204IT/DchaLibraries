package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.View;
import com.android.internal.app.PlatLogoActivity;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class FirmwareVersionDialogController implements View.OnClickListener {
    static final int FIRMWARE_VERSION_LABEL_ID = 2131362140;
    static final int FIRMWARE_VERSION_VALUE_ID = 2131362141;
    private final Context mContext;
    private final FirmwareVersionDialogFragment mDialog;
    private RestrictedLockUtils.EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;
    private final long[] mHits = new long[3];
    private final UserManager mUserManager;

    public FirmwareVersionDialogController(FirmwareVersionDialogFragment firmwareVersionDialogFragment) {
        this.mDialog = firmwareVersionDialogFragment;
        this.mContext = firmwareVersionDialogFragment.getContext();
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        arrayCopy();
        this.mHits[this.mHits.length - 1] = SystemClock.uptimeMillis();
        if (this.mHits[0] >= SystemClock.uptimeMillis() - 500) {
            if (this.mUserManager.hasUserRestriction("no_fun")) {
                if (this.mFunDisallowedAdmin != null && !this.mFunDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mFunDisallowedAdmin);
                }
                Log.d("firmwareDialogCtrl", "Sorry, no fun for you!");
                return;
            }
            Intent className = new Intent("android.intent.action.MAIN").setClassName("android", PlatLogoActivity.class.getName());
            try {
                this.mContext.startActivity(className);
            } catch (Exception e) {
                Log.e("firmwareDialogCtrl", "Unable to start activity " + className.toString());
            }
        }
    }

    public void initialize() {
        initializeAdminPermissions();
        registerClickListeners();
        this.mDialog.setText(R.id.firmware_version_value, Build.VERSION.RELEASE);
    }

    private void registerClickListeners() {
        this.mDialog.registerClickListener(R.id.firmware_version_label, this);
        this.mDialog.registerClickListener(R.id.firmware_version_value, this);
    }

    void arrayCopy() {
        System.arraycopy(this.mHits, 1, this.mHits, 0, this.mHits.length - 1);
    }

    void initializeAdminPermissions() {
        this.mFunDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_fun", UserHandle.myUserId());
        this.mFunDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_fun", UserHandle.myUserId());
    }
}
