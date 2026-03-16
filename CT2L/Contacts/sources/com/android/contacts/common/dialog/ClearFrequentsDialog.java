package com.android.contacts.common.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import com.android.contacts.R;

public class ClearFrequentsDialog extends DialogFragment {
    public static void show(FragmentManager fragmentManager) {
        ClearFrequentsDialog dialog = new ClearFrequentsDialog();
        dialog.show(fragmentManager, "clearFrequents");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ContentResolver resolver = getActivity().getContentResolver();
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final IndeterminateProgressDialog progressDialog = IndeterminateProgressDialog.show(ClearFrequentsDialog.this.getFragmentManager(), ClearFrequentsDialog.this.getString(R.string.clearFrequentsProgress_title), null, 500L);
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        resolver.delete(ContactsContract.DataUsageFeedback.DELETE_USAGE_URI, null, null);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        progressDialog.dismiss();
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            }
        };
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.clearFrequentsConfirmation_title).setMessage(R.string.clearFrequentsConfirmation).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(android.R.string.ok, okListener).setCancelable(true).create();
    }
}
