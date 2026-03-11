package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;

public class StorageWizardMigrateProgress extends StorageWizardBase {
    private final PackageManager.MoveCallback mCallback = new PackageManager.MoveCallback() {
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (StorageWizardMigrateProgress.this.mMoveId != moveId) {
                return;
            }
            Context context = StorageWizardMigrateProgress.this;
            if (PackageManager.isMoveStatusFinished(status)) {
                Log.d("StorageSettings", "Finished with status " + status);
                if (status == -100) {
                    if (StorageWizardMigrateProgress.this.mDisk != null) {
                        Intent finishIntent = new Intent("com.android.systemui.action.FINISH_WIZARD");
                        finishIntent.addFlags(1073741824);
                        StorageWizardMigrateProgress.this.sendBroadcast(finishIntent);
                        if (!StorageWizardMigrateProgress.this.isFinishing()) {
                            Intent intent = new Intent(context, (Class<?>) StorageWizardReady.class);
                            intent.putExtra("android.os.storage.extra.DISK_ID", StorageWizardMigrateProgress.this.mDisk.getId());
                            StorageWizardMigrateProgress.this.startActivity(intent);
                        }
                    }
                } else {
                    Toast.makeText(context, StorageWizardMigrateProgress.this.getString(R.string.insufficient_storage), 1).show();
                }
                StorageWizardMigrateProgress.this.finishAffinity();
                return;
            }
            StorageWizardMigrateProgress.this.setCurrentProgress(status);
        }
    };
    private int mMoveId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mVolume == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_progress);
        this.mMoveId = getIntent().getIntExtra("android.content.pm.extra.MOVE_ID", -1);
        String descrip = this.mStorage.getBestVolumeDescription(this.mVolume);
        setIllustrationType(1);
        setHeaderText(R.string.storage_wizard_migrate_progress_title, descrip);
        setBodyText(R.string.storage_wizard_migrate_details, descrip);
        getNextButton().setVisibility(8);
        getPackageManager().registerMoveCallback(this.mCallback, new Handler());
        this.mCallback.onStatusChanged(this.mMoveId, getPackageManager().getMoveStatus(this.mMoveId), -1L);
    }
}
