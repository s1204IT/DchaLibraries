package com.android.settings.enterprise;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class ActionDisabledByAdminDialog extends Activity implements DialogInterface.OnDismissListener {
    private ActionDisabledByAdminDialogHelper mDialogHelper;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        RestrictedLockUtils.EnforcedAdmin adminDetailsFromIntent = getAdminDetailsFromIntent(getIntent());
        String restrictionFromIntent = getRestrictionFromIntent(getIntent());
        this.mDialogHelper = new ActionDisabledByAdminDialogHelper(this);
        this.mDialogHelper.prepareDialogBuilder(restrictionFromIntent, adminDetailsFromIntent).setOnDismissListener(this).show();
    }

    @Override // android.app.Activity
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        RestrictedLockUtils.EnforcedAdmin adminDetailsFromIntent = getAdminDetailsFromIntent(intent);
        this.mDialogHelper.updateDialog(getRestrictionFromIntent(intent), adminDetailsFromIntent);
    }

    RestrictedLockUtils.EnforcedAdmin getAdminDetailsFromIntent(Intent intent) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdmin = new RestrictedLockUtils.EnforcedAdmin(null, UserHandle.myUserId());
        if (intent == null) {
            return enforcedAdmin;
        }
        enforcedAdmin.component = (ComponentName) intent.getParcelableExtra("android.app.extra.DEVICE_ADMIN");
        enforcedAdmin.userId = intent.getIntExtra("android.intent.extra.USER_ID", UserHandle.myUserId());
        return enforcedAdmin;
    }

    String getRestrictionFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra("android.app.extra.RESTRICTION");
    }

    @Override // android.content.DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }
}
