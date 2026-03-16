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
import android.os.IBinder;
import android.util.Log;
import com.android.contacts.R;
import com.android.contacts.common.vcard.VCardService;

public class CancelActivity extends Activity implements ServiceConnection {
    private final String LOG_TAG = "VCardCancel";
    private final CancelListener mCancelListener = new CancelListener();
    private String mDisplayName;
    private int mJobId;
    private int mType;

    private class RequestCancelListener implements DialogInterface.OnClickListener {
        private RequestCancelListener() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            CancelActivity.this.bindService(new Intent(CancelActivity.this, (Class<?>) VCardService.class), CancelActivity.this, 1);
        }
    }

    private class CancelListener implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private CancelListener() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            CancelActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            CancelActivity.this.finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        this.mJobId = Integer.parseInt(uri.getQueryParameter("job_id"));
        this.mDisplayName = uri.getQueryParameter("display_name");
        this.mType = Integer.parseInt(uri.getQueryParameter("type"));
        showDialog(R.id.dialog_cancel_confirmation);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        String message;
        switch (id) {
            case R.id.dialog_cancel_confirmation:
                if (this.mType == 1) {
                    message = getString(R.string.cancel_import_confirmation_message, new Object[]{this.mDisplayName});
                } else {
                    message = getString(R.string.cancel_export_confirmation_message, new Object[]{this.mDisplayName});
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(message).setPositiveButton(android.R.string.ok, new RequestCancelListener()).setOnCancelListener(this.mCancelListener).setNegativeButton(android.R.string.cancel, this.mCancelListener);
                return builder.create();
            case R.id.dialog_cancel_failed:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this).setTitle(R.string.cancel_vcard_import_or_export_failed).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(getString(R.string.fail_reason_unknown)).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener);
                return builder2.create();
            default:
                Log.w("VCardCancel", "Unknown dialog id: " + id);
                return super.onCreateDialog(id, bundle);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        VCardService service = ((VCardService.MyBinder) binder).getService();
        try {
            CancelRequest request = new CancelRequest(this.mJobId, this.mDisplayName);
            service.handleCancelRequest(request, null);
            unbindService(this);
            finish();
        } catch (Throwable th) {
            unbindService(this);
            throw th;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
}
