package com.android.settings.password;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.Utils;
import com.android.settings.password.SetNewPasswordController;

/* loaded from: classes.dex */
public class SetNewPasswordActivity extends Activity implements SetNewPasswordController.Ui {
    private String mNewPasswordAction;
    private SetNewPasswordController mSetNewPasswordController;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mNewPasswordAction = getIntent().getAction();
        if (!"android.app.action.SET_NEW_PASSWORD".equals(this.mNewPasswordAction) && !"android.app.action.SET_NEW_PARENT_PROFILE_PASSWORD".equals(this.mNewPasswordAction)) {
            Log.e("SetNewPasswordActivity", "Unexpected action to launch this activity");
            finish();
        } else {
            this.mSetNewPasswordController = SetNewPasswordController.create(this, this, getIntent(), getActivityToken());
            this.mSetNewPasswordController.dispatchSetNewPasswordIntent();
        }
    }

    @Override // com.android.settings.password.SetNewPasswordController.Ui
    public void launchChooseLock(Bundle bundle) {
        Intent intent = Utils.isDeviceProvisioned(this) ^ true ? new Intent(this, (Class<?>) SetupChooseLockGeneric.class) : new Intent(this, (Class<?>) ChooseLockGeneric.class);
        intent.setAction(this.mNewPasswordAction);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }
}
