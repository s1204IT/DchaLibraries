package com.android.settings.deviceinfo;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.android.internal.util.Preconditions;
import com.android.settings.R;

public class StorageWizardMoveConfirm extends StorageWizardBase {
    private ApplicationInfo mApp;
    private String mPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mVolume == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);
        try {
            this.mPackageName = getIntent().getStringExtra("android.intent.extra.PACKAGE_NAME");
            this.mApp = getPackageManager().getApplicationInfo(this.mPackageName, 0);
            Preconditions.checkState(getPackageManager().getPackageCandidateVolumes(this.mApp).contains(this.mVolume));
            String appName = getPackageManager().getApplicationLabel(this.mApp).toString();
            String volumeName = this.mStorage.getBestVolumeDescription(this.mVolume);
            setIllustrationType(1);
            setHeaderText(R.string.storage_wizard_move_confirm_title, appName);
            setBodyText(R.string.storage_wizard_move_confirm_body, appName, volumeName);
            getNextButton().setText(R.string.move_app);
        } catch (PackageManager.NameNotFoundException e) {
            finish();
        }
    }

    @Override
    public void onNavigateNext() {
        String appName = getPackageManager().getApplicationLabel(this.mApp).toString();
        int moveId = getPackageManager().movePackage(this.mPackageName, this.mVolume);
        Intent intent = new Intent(this, (Class<?>) StorageWizardMoveProgress.class);
        intent.putExtra("android.content.pm.extra.MOVE_ID", moveId);
        intent.putExtra("android.intent.extra.TITLE", appName);
        intent.putExtra("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
        startActivity(intent);
        finishAffinity();
    }
}
