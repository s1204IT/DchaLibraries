package com.android.settings.deviceinfo;

import android.content.Intent;
import android.content.pm.IPackageMoveObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IVoldTaskListener;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class StorageWizardFormatProgress extends StorageWizardBase {
    private boolean mFormatPrivate;
    private PartitionTask mTask;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (this.mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_progress);
        setKeepScreenOn(true);
        this.mFormatPrivate = getIntent().getBooleanExtra("format_private", false);
        setHeaderText(R.string.storage_wizard_format_progress_title, getDiskShortDescription());
        setBodyText(R.string.storage_wizard_format_progress_body, getDiskDescription());
        this.mTask = (PartitionTask) getLastNonConfigurationInstance();
        if (this.mTask == null) {
            this.mTask = new PartitionTask();
            this.mTask.setActivity(this);
            this.mTask.execute(new Void[0]);
            return;
        }
        this.mTask.setActivity(this);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.mTask;
    }

    public static class PartitionTask extends AsyncTask<Void, Integer, Exception> {
        public StorageWizardFormatProgress mActivity;
        private volatile long mPrivateBench;
        private volatile int mProgress = 20;

        @Override
        protected Exception doInBackground(Void... voidArr) {
            StorageWizardFormatProgress storageWizardFormatProgress = this.mActivity;
            StorageManager storageManager = this.mActivity.mStorage;
            try {
                if (storageWizardFormatProgress.mFormatPrivate) {
                    storageManager.partitionPrivate(storageWizardFormatProgress.mDisk.getId());
                    publishProgress(40);
                    VolumeInfo volumeInfoFindFirstVolume = storageWizardFormatProgress.findFirstVolume(1, 25);
                    final CompletableFuture completableFuture = new CompletableFuture();
                    storageManager.benchmark(volumeInfoFindFirstVolume.getId(), new IVoldTaskListener.Stub() {
                        public void onStatus(int i, PersistableBundle persistableBundle) {
                            PartitionTask.this.publishProgress(Integer.valueOf(40 + ((i * 40) / 100)));
                        }

                        public void onFinished(int i, PersistableBundle persistableBundle) {
                            completableFuture.complete(persistableBundle);
                        }
                    });
                    this.mPrivateBench = ((PersistableBundle) completableFuture.get(60L, TimeUnit.SECONDS)).getLong("run", Long.MAX_VALUE);
                    if (storageWizardFormatProgress.mDisk.isDefaultPrimary() && Objects.equals(storageManager.getPrimaryStorageUuid(), "primary_physical")) {
                        Log.d("StorageSettings", "Just formatted primary physical; silently moving storage to new emulated volume");
                        storageManager.setPrimaryStorageUuid(volumeInfoFindFirstVolume.getFsUuid(), new SilentObserver());
                    }
                } else {
                    storageManager.partitionPublic(storageWizardFormatProgress.mDisk.getId());
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... numArr) {
            this.mProgress = numArr[0].intValue();
            this.mActivity.setCurrentProgress(this.mProgress);
        }

        public void setActivity(StorageWizardFormatProgress storageWizardFormatProgress) {
            this.mActivity = storageWizardFormatProgress;
            this.mActivity.setCurrentProgress(this.mProgress);
        }

        @Override
        protected void onPostExecute(Exception exc) {
            StorageWizardFormatProgress storageWizardFormatProgress = this.mActivity;
            if (storageWizardFormatProgress.isDestroyed()) {
                return;
            }
            if (exc == null) {
                if (storageWizardFormatProgress.mFormatPrivate) {
                    Log.d("StorageSettings", "New volume took " + this.mPrivateBench + "ms to run benchmark");
                    if (this.mPrivateBench > 2000 || SystemProperties.getBoolean("sys.debug.storage_slow", false)) {
                        this.mActivity.onFormatFinishedSlow();
                        return;
                    } else {
                        this.mActivity.onFormatFinished();
                        return;
                    }
                }
                this.mActivity.onFormatFinished();
                return;
            }
            Log.e("StorageSettings", "Failed to partition", exc);
            Toast.makeText(storageWizardFormatProgress, exc.getMessage(), 1).show();
            storageWizardFormatProgress.finishAffinity();
        }
    }

    public void onFormatFinished() {
        Intent intent = new Intent(this, (Class<?>) StorageWizardFormatSlow.class);
        intent.putExtra("format_slow", false);
        startActivity(intent);
        finishAffinity();
    }

    public void onFormatFinishedSlow() {
        Intent intent = new Intent(this, (Class<?>) StorageWizardFormatSlow.class);
        intent.putExtra("format_slow", true);
        startActivity(intent);
        finishAffinity();
    }

    private static class SilentObserver extends IPackageMoveObserver.Stub {
        private SilentObserver() {
        }

        public void onCreated(int i, Bundle bundle) {
        }

        public void onStatusChanged(int i, int i2, long j) {
        }
    }
}
