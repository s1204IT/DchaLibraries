package com.android.contacts.common.vcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.R;
import com.android.contacts.common.vcard.VCardService;
import java.io.File;

public class ExportVCardActivity extends Activity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener, ServiceConnection {
    private static final BidiFormatter mBidiFormatter = BidiFormatter.getInstance();
    private boolean mConnected;
    private String mErrorReason;
    private VCardService mService;
    private String mTargetFileName;
    private volatile boolean mProcessOngoing = true;
    private final Messenger mIncomingMessenger = new Messenger(new IncomingHandler());

    private class IncomingHandler extends Handler {
        private IncomingHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 != 0) {
                Log.i("VCardExport", "Message returned from vCard server contains error code.");
                if (msg.obj != null) {
                    ExportVCardActivity.this.mErrorReason = (String) msg.obj;
                }
                ExportVCardActivity.this.showDialog(msg.arg1);
            }
            switch (msg.what) {
                case 5:
                    if (msg.obj != null) {
                        ExportVCardActivity.this.mTargetFileName = (String) msg.obj;
                        if (TextUtils.isEmpty(ExportVCardActivity.this.mTargetFileName)) {
                            Log.w("VCardExport", "Destination file name coming from vCard service is empty.");
                            ExportVCardActivity.this.mErrorReason = ExportVCardActivity.this.getString(R.string.fail_reason_unknown);
                            ExportVCardActivity.this.showDialog(R.id.dialog_fail_to_export_with_reason);
                        } else {
                            ExportVCardActivity.this.showDialog(R.id.dialog_export_confirmation);
                        }
                    } else {
                        Log.w("VCardExport", "Message returned from vCard server doesn't contain valid path");
                        ExportVCardActivity.this.mErrorReason = ExportVCardActivity.this.getString(R.string.fail_reason_unknown);
                        ExportVCardActivity.this.showDialog(R.id.dialog_fail_to_export_with_reason);
                    }
                    break;
                default:
                    Log.w("VCardExport", "Unknown message type: " + msg.what);
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private class ExportConfirmationListener implements DialogInterface.OnClickListener {
        private final Uri mDestinationUri;

        public ExportConfirmationListener(ExportVCardActivity exportVCardActivity, String path) {
            this(Uri.parse("file://" + path));
        }

        public ExportConfirmationListener(Uri uri) {
            this.mDestinationUri = uri;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                ExportRequest request = new ExportRequest(this.mDestinationUri);
                ExportVCardActivity.this.mService.handleExportRequest(request, new NotificationImportExportListener(ExportVCardActivity.this));
            }
            ExportVCardActivity.this.unbindAndFinish();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!Environment.getExternalStorageState().equals("mounted")) {
            Log.w("VCardExport", "External storage is in state " + Environment.getExternalStorageState() + ". Cancelling export");
            showDialog(R.id.dialog_sdcard_not_found);
            return;
        }
        File targetDirectory = Environment.getExternalStorageDirectory();
        if ((!targetDirectory.exists() || !targetDirectory.isDirectory() || !targetDirectory.canRead()) && !targetDirectory.mkdirs()) {
            showDialog(R.id.dialog_sdcard_not_found);
            return;
        }
        String callingActivity = getIntent().getExtras().getString("CALLING_ACTIVITY");
        Intent intent = new Intent(this, (Class<?>) VCardService.class);
        intent.putExtra("CALLING_ACTIVITY", callingActivity);
        if (startService(intent) == null) {
            Log.e("VCardExport", "Failed to start vCard service");
            this.mErrorReason = getString(R.string.fail_reason_unknown);
            showDialog(R.id.dialog_fail_to_export_with_reason);
        } else if (!bindService(intent, this, 1)) {
            Log.e("VCardExport", "Failed to connect to vCard service.");
            this.mErrorReason = getString(R.string.fail_reason_unknown);
            showDialog(R.id.dialog_fail_to_export_with_reason);
        }
    }

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder binder) {
        this.mConnected = true;
        this.mService = ((VCardService.MyBinder) binder).getService();
        this.mService.handleRequestAvailableExportDestination(this.mIncomingMessenger);
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        this.mService = null;
        this.mConnected = false;
        if (this.mProcessOngoing) {
            Log.w("VCardExport", "Disconnected from service during the process ongoing.");
            this.mErrorReason = getString(R.string.fail_reason_unknown);
            showDialog(R.id.dialog_fail_to_export_with_reason);
        }
    }

    private String getTargetFileForDisplay() {
        if (this.mTargetFileName == null) {
            return null;
        }
        return mBidiFormatter.unicodeWrap(this.mTargetFileName, TextDirectionHeuristics.LTR);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        switch (id) {
            case R.string.fail_reason_too_many_vcard:
                this.mProcessOngoing = false;
                return new AlertDialog.Builder(this).setTitle(R.string.exporting_contact_failed_title).setMessage(getString(R.string.exporting_contact_failed_message, new Object[]{getString(R.string.fail_reason_too_many_vcard)})).setPositiveButton(android.R.string.ok, this).create();
            case R.id.dialog_sdcard_not_found:
                this.mProcessOngoing = false;
                return new AlertDialog.Builder(this).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.no_sdcard_message).setPositiveButton(android.R.string.ok, this).create();
            case R.id.dialog_export_confirmation:
                return new AlertDialog.Builder(this).setTitle(R.string.confirm_export_title).setMessage(getString(R.string.confirm_export_message, new Object[]{getTargetFileForDisplay()})).setPositiveButton(android.R.string.ok, new ExportConfirmationListener(this, this.mTargetFileName)).setNegativeButton(android.R.string.cancel, this).setOnCancelListener(this).create();
            case R.id.dialog_fail_to_export_with_reason:
                this.mProcessOngoing = false;
                AlertDialog.Builder title = new AlertDialog.Builder(this).setTitle(R.string.exporting_contact_failed_title);
                Object[] objArr = new Object[1];
                objArr[0] = this.mErrorReason != null ? this.mErrorReason : getString(R.string.fail_reason_unknown);
                return title.setMessage(getString(R.string.exporting_contact_failed_message, objArr)).setPositiveButton(android.R.string.ok, this).setOnCancelListener(this).create();
            default:
                return super.onCreateDialog(id, bundle);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        if (id == R.id.dialog_fail_to_export_with_reason) {
            ((AlertDialog) dialog).setMessage(this.mErrorReason);
        } else if (id == R.id.dialog_export_confirmation) {
            ((AlertDialog) dialog).setMessage(getString(R.string.confirm_export_message, new Object[]{getTargetFileForDisplay()}));
        } else {
            super.onPrepareDialog(id, dialog, args);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing()) {
            unbindAndFinish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        unbindAndFinish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        this.mProcessOngoing = false;
        unbindAndFinish();
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        this.mProcessOngoing = false;
        super.unbindService(conn);
    }

    private synchronized void unbindAndFinish() {
        if (this.mConnected) {
            unbindService(this);
            this.mConnected = false;
        }
        finish();
    }
}
