package com.android.contacts.vcard;

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
import com.android.contacts.R;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.vcard.VCardService;
import com.mediatek.contacts.util.Log;

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
        public void onClick(DialogInterface dialogInterface, int i) {
            CancelActivity.this.bindService(new Intent(CancelActivity.this, (Class<?>) VCardService.class), CancelActivity.this, 1);
        }
    }

    private class CancelListener implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private CancelListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            CancelActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            CancelActivity.this.finish();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Uri data = getIntent().getData();
        this.mJobId = Integer.parseInt(data.getQueryParameter("job_id"));
        this.mDisplayName = data.getQueryParameter("display_name");
        this.mType = Integer.parseInt(data.getQueryParameter(BaseAccountType.Attr.TYPE));
        showDialog(R.id.dialog_cancel_confirmation);
    }

    @Override
    protected Dialog onCreateDialog(int i, Bundle bundle) {
        String string;
        if (i == R.id.dialog_cancel_confirmation) {
            if (this.mType == 1) {
                string = getString(R.string.cancel_import_confirmation_message, new Object[]{this.mDisplayName});
            } else {
                string = getString(R.string.cancel_export_confirmation_message, new Object[]{this.mDisplayName});
            }
            return new AlertDialog.Builder(this).setMessage(string).setPositiveButton(R.string.yes_button, new RequestCancelListener()).setOnCancelListener(this.mCancelListener).setNegativeButton(R.string.no_button, this.mCancelListener).create();
        }
        if (i == R.id.dialog_cancel_failed) {
            return new AlertDialog.Builder(this).setTitle(R.string.cancel_vcard_import_or_export_failed).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(getString(R.string.fail_reason_unknown)).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener).create();
        }
        Log.w("VCardCancel", "Unknown dialog id: " + i);
        return super.onCreateDialog(i, bundle);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        try {
            ((VCardService.MyBinder) iBinder).getService().handleCancelRequest(new CancelRequest(this.mJobId, this.mDisplayName), null);
            unbindService(this);
            finish();
        } catch (Throwable th) {
            unbindService(this);
            throw th;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }
}
