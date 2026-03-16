package com.android.internal.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.storage.StorageVolume;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.app.AlertController;
import com.android.internal.os.storage.ExternalStorageFormatter;

public class ExternalMediaFormatActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private static final int POSITIVE_BUTTON = -1;
    private BroadcastReceiver mStorageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("ExternalMediaFormatActivity", "got action " + action);
            if (action == Intent.ACTION_MEDIA_REMOVED || action == Intent.ACTION_MEDIA_CHECKING || action == Intent.ACTION_MEDIA_MOUNTED || action == Intent.ACTION_MEDIA_SHARED) {
                ExternalMediaFormatActivity.this.finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ExternalMediaFormatActivity", "onCreate!");
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.extmedia_format_title);
        p.mMessage = getString(R.string.extmedia_format_message);
        p.mPositiveButtonText = getString(R.string.extmedia_format_button_format);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(17039360);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        registerReceiver(this.mStorageReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.mStorageReceiver);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            Intent intent = new Intent(ExternalStorageFormatter.FORMAT_ONLY);
            intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, getIntent().getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME));
            startService(intent);
        }
        finish();
    }
}
