package com.android.providers.downloads;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Formatter;
import android.util.Log;
import java.util.LinkedList;
import java.util.Queue;

public class SizeLimitActivity extends Activity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
    private Intent mCurrentIntent;
    private Uri mCurrentUri;
    private Dialog mDialog;
    private Queue<Intent> mDownloadsToShow = new LinkedList();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            this.mDownloadsToShow.add(intent);
            setIntent(null);
            showNextDialog();
        }
        if (this.mDialog != null && !this.mDialog.isShowing()) {
            this.mDialog.show();
        }
    }

    private void showNextDialog() {
        if (this.mDialog == null) {
            if (this.mDownloadsToShow.isEmpty()) {
                finish();
                return;
            }
            this.mCurrentIntent = this.mDownloadsToShow.poll();
            this.mCurrentUri = this.mCurrentIntent.getData();
            Cursor cursor = getContentResolver().query(this.mCurrentUri, null, null, null, null);
            try {
                if (!cursor.moveToFirst()) {
                    Log.e("DownloadManager", "Empty cursor for URI " + this.mCurrentUri);
                    dialogClosed();
                } else {
                    showDialog(cursor);
                    cursor.close();
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void showDialog(Cursor cursor) {
        int size = cursor.getInt(cursor.getColumnIndexOrThrow("total_bytes"));
        String sizeString = Formatter.formatFileSize(this, size);
        String queueText = getString(R.string.button_queue_for_wifi);
        boolean isWifiRequired = this.mCurrentIntent.getExtras().getBoolean("isWifiRequired");
        AlertDialog.Builder builder = new AlertDialog.Builder(this, 2);
        if (isWifiRequired) {
            builder.setTitle(R.string.wifi_required_title).setMessage(getString(R.string.wifi_required_body, new Object[]{sizeString, queueText})).setPositiveButton(R.string.button_queue_for_wifi, this).setNegativeButton(R.string.button_cancel_download, this);
        } else {
            builder.setTitle(R.string.wifi_recommended_title).setMessage(getString(R.string.wifi_recommended_body, new Object[]{sizeString, queueText})).setPositiveButton(R.string.button_start_now, this).setNegativeButton(R.string.button_queue_for_wifi, this);
        }
        this.mDialog = builder.setOnCancelListener(this).show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dialogClosed();
    }

    private void dialogClosed() {
        this.mDialog = null;
        this.mCurrentUri = null;
        showNextDialog();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        boolean isRequired = this.mCurrentIntent.getExtras().getBoolean("isWifiRequired");
        if (isRequired && which == -2) {
            getContentResolver().delete(this.mCurrentUri, null, null);
        } else if (!isRequired && which == -1) {
            ContentValues values = new ContentValues();
            values.put("bypass_recommended_size_limit", (Boolean) true);
            getContentResolver().update(this.mCurrentUri, values, null, null);
        }
        dialogClosed();
    }
}
