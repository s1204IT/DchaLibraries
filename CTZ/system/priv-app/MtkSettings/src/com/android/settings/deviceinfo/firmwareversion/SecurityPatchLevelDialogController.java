package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.wrapper.PackageManagerWrapper;

/* loaded from: classes.dex */
public class SecurityPatchLevelDialogController implements View.OnClickListener {
    private static final Uri INTENT_URI_DATA = Uri.parse("https://source.android.com/security/bulletin/");
    static final int SECURITY_PATCH_LABEL_ID = 2131362516;
    static final int SECURITY_PATCH_VALUE_ID = 2131362517;
    private final Context mContext;
    private final String mCurrentPatch = DeviceInfoUtils.getSecurityPatch();
    private final FirmwareVersionDialogFragment mDialog;
    private final PackageManagerWrapper mPackageManager;

    public SecurityPatchLevelDialogController(FirmwareVersionDialogFragment firmwareVersionDialogFragment) {
        this.mDialog = firmwareVersionDialogFragment;
        this.mContext = firmwareVersionDialogFragment.getContext();
        this.mPackageManager = new PackageManagerWrapper(this.mContext.getPackageManager());
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(INTENT_URI_DATA);
        if (this.mPackageManager.queryIntentActivities(intent, 0).isEmpty()) {
            Log.w("SecurityPatchCtrl", "Stop click action on 2131362517: queryIntentActivities() returns empty");
        } else {
            this.mContext.startActivity(intent);
        }
    }

    public void initialize() {
        if (TextUtils.isEmpty(this.mCurrentPatch)) {
            this.mDialog.removeSettingFromScreen(R.id.security_patch_level_label);
            this.mDialog.removeSettingFromScreen(R.id.security_patch_level_value);
        } else {
            registerListeners();
            this.mDialog.setText(R.id.security_patch_level_value, this.mCurrentPatch);
        }
    }

    private void registerListeners() {
        this.mDialog.registerClickListener(R.id.security_patch_level_label, this);
        this.mDialog.registerClickListener(R.id.security_patch_level_value, this);
    }
}
