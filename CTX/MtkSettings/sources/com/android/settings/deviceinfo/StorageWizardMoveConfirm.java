package com.android.settings.deviceinfo;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.password.ChooseLockSettingsHelper;

public class StorageWizardMoveConfirm extends StorageWizardBase {
    private ApplicationInfo mApp;
    private String mPackageName;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (this.mVolume == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);
        try {
            this.mPackageName = getIntent().getStringExtra("android.intent.extra.PACKAGE_NAME");
            this.mApp = getPackageManager().getApplicationInfo(this.mPackageName, 0);
            Preconditions.checkState(getPackageManager().getPackageCandidateVolumes(this.mApp).contains(this.mVolume));
            String string = getPackageManager().getApplicationLabel(this.mApp).toString();
            String bestVolumeDescription = this.mStorage.getBestVolumeDescription(this.mVolume);
            setIcon(R.drawable.ic_swap_horiz);
            setHeaderText(R.string.storage_wizard_move_confirm_title, string);
            setBodyText(R.string.storage_wizard_move_confirm_body, string, bestVolumeDescription);
            setNextButtonText(R.string.move_app, new CharSequence[0]);
        } catch (PackageManager.NameNotFoundException e) {
            finish();
        }
    }

    @Override
    public void onNavigateNext(View view) {
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            for (UserInfo userInfo : ((UserManager) getSystemService(UserManager.class)).getUsers()) {
                if (userInfo.isInitialized() && !StorageManager.isUserKeyUnlocked(userInfo.id)) {
                    Log.d("StorageSettings", "User " + userInfo.id + " is currently locked; requesting unlock");
                    new ChooseLockSettingsHelper(this).launchConfirmationActivityForAnyUser(100, null, null, TextUtils.expandTemplate(getText(R.string.storage_wizard_move_unlock), userInfo.name), userInfo.id);
                    return;
                }
            }
        }
        String string = getPackageManager().getApplicationLabel(this.mApp).toString();
        int iMovePackage = getPackageManager().movePackage(this.mPackageName, this.mVolume);
        Intent intent = new Intent(this, (Class<?>) StorageWizardMoveProgress.class);
        intent.putExtra("android.content.pm.extra.MOVE_ID", iMovePackage);
        intent.putExtra("android.intent.extra.TITLE", string);
        intent.putExtra("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
        startActivity(intent);
        finishAffinity();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 100) {
            if (i2 == -1) {
                onNavigateNext(null);
                return;
            } else {
                Log.w("StorageSettings", "Failed to confirm credentials");
                return;
            }
        }
        super.onActivityResult(i, i2, intent);
    }
}
