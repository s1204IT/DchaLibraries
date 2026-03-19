package com.android.contacts.vcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import com.android.contacts.R;
import com.android.contacts.activities.RequestImportVCardPermissionsActivity;
import com.android.contacts.vcard.VCardService;
import com.mediatek.contacts.util.Log;
import java.util.List;

public class ExportVCardActivity extends Activity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener, ServiceConnection {
    private static final BidiFormatter mBidiFormatter = BidiFormatter.getInstance();
    protected boolean mConnected;
    private String mErrorReason;
    private volatile boolean mProcessOngoing = true;
    protected VCardService mService;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.d("VCardExport", "[onCreate]");
        super.onCreate(bundle);
        if (RequestImportVCardPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }
        if (!hasExportIntentHandler()) {
            Log.e("VCardExport", "Couldn't find export intent handler");
            showErrorDialog();
        } else {
            connectVCardService();
        }
    }

    private void connectVCardService() {
        String string = getIntent().getExtras().getString("CALLING_ACTIVITY");
        Log.d("VCardExport", "[connectVCardService]callingActivity = " + string);
        Intent intent = new Intent(this, (Class<?>) VCardService.class);
        intent.putExtra("CALLING_ACTIVITY", string);
        if (startService(intent) == null) {
            Log.e("VCardExport", "Failed to start vCard service");
            showErrorDialog();
        } else if (!bindService(intent, this, 1)) {
            Log.e("VCardExport", "Failed to connect to vCard service.");
            showErrorDialog();
        }
    }

    private boolean hasExportIntentHandler() {
        List<ResolveInfo> listQueryIntentActivities = getPackageManager().queryIntentActivities(getCreateDocIntent(), 65536);
        return listQueryIntentActivities != null && listQueryIntentActivities.size() > 0;
    }

    private Intent getCreateDocIntent() {
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("text/x-vcard");
        intent.putExtra("android.intent.extra.TITLE", mBidiFormatter.unicodeWrap(getString(R.string.exporting_vcard_filename), TextDirectionHeuristics.LTR));
        return intent;
    }

    private void showErrorDialog() {
        this.mErrorReason = getString(R.string.fail_reason_unknown);
        showDialog(R.id.dialog_fail_to_export_with_reason);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 100) {
            if (i2 == -1 && this.mService != null && intent != null && intent.getData() != null) {
                this.mService.handleExportRequest(new ExportRequest(intent.getData()), new NotificationImportExportListener(this));
            }
            finish();
        }
    }

    @Override
    public synchronized void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.mConnected = true;
        this.mService = ((VCardService.MyBinder) iBinder).getService();
        startActivityForResult(getCreateDocIntent(), 100);
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName componentName) {
        this.mService = null;
        this.mConnected = false;
        if (this.mProcessOngoing) {
            Log.w("VCardExport", "Disconnected from service during the process ongoing.");
            showErrorDialog();
        }
    }

    @Override
    protected Dialog onCreateDialog(int i, Bundle bundle) {
        if (i == R.id.dialog_fail_to_export_with_reason) {
            Log.w("VCardExport", "[onCreateDialog]");
            this.mProcessOngoing = false;
            AlertDialog.Builder title = new AlertDialog.Builder(this).setTitle(R.string.exporting_contact_failed_title);
            Object[] objArr = new Object[1];
            objArr[0] = this.mErrorReason != null ? this.mErrorReason : getString(R.string.fail_reason_unknown);
            return title.setMessage(getString(R.string.exporting_contact_failed_message, objArr)).setPositiveButton(android.R.string.ok, this).setOnCancelListener(this).create();
        }
        return super.onCreateDialog(i, bundle);
    }

    @Override
    protected void onPrepareDialog(int i, Dialog dialog, Bundle bundle) {
        if (i == R.id.dialog_fail_to_export_with_reason) {
            ((AlertDialog) dialog).setMessage(this.mErrorReason);
        } else {
            super.onPrepareDialog(i, dialog, bundle);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        this.mProcessOngoing = false;
        finish();
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        this.mProcessOngoing = false;
        super.unbindService(serviceConnection);
    }

    @Override
    protected void onDestroy() {
        if (this.mConnected) {
            unbindService(this);
            this.mConnected = false;
        }
        super.onDestroy();
    }

    static String getOpenableUriDisplayName(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        Cursor cursorQuery = context.getContentResolver().query(uri, null, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getString(cursorQuery.getColumnIndex("_display_name"));
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return null;
    }
}
