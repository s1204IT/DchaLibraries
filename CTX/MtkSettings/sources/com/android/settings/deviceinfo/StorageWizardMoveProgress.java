package com.android.settings.deviceinfo;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;

public class StorageWizardMoveProgress extends StorageWizardBase {
    private final PackageManager.MoveCallback mCallback = new PackageManager.MoveCallback() {
        public void onStatusChanged(int i, int i2, long j) {
            if (StorageWizardMoveProgress.this.mMoveId != i) {
                return;
            }
            if (PackageManager.isMoveStatusFinished(i2)) {
                Log.d("StorageSettings", "Finished with status " + i2);
                if (i2 != -100) {
                    Toast.makeText(StorageWizardMoveProgress.this, StorageWizardMoveProgress.this.moveStatusToMessage(i2), 1).show();
                }
                StorageWizardMoveProgress.this.finishAffinity();
                return;
            }
            StorageWizardMoveProgress.this.setCurrentProgress(i2);
        }
    };
    private int mMoveId;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (this.mVolume == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_progress);
        this.mMoveId = getIntent().getIntExtra("android.content.pm.extra.MOVE_ID", -1);
        String stringExtra = getIntent().getStringExtra("android.intent.extra.TITLE");
        String bestVolumeDescription = this.mStorage.getBestVolumeDescription(this.mVolume);
        setIcon(R.drawable.ic_swap_horiz);
        setHeaderText(R.string.storage_wizard_move_progress_title, stringExtra);
        setBodyText(R.string.storage_wizard_move_progress_body, bestVolumeDescription, stringExtra);
        getPackageManager().registerMoveCallback(this.mCallback, new Handler());
        this.mCallback.onStatusChanged(this.mMoveId, getPackageManager().getMoveStatus(this.mMoveId), -1L);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getPackageManager().unregisterMoveCallback(this.mCallback);
    }

    private CharSequence moveStatusToMessage(int i) {
        if (i == -8) {
            return getString(R.string.move_error_device_admin);
        }
        switch (i) {
        }
        return getString(R.string.insufficient_storage);
    }
}
