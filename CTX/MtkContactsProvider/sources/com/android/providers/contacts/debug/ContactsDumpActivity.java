package com.android.providers.contacts.debug;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.android.providers.contacts.R;
import java.io.IOException;

public class ContactsDumpActivity extends Activity implements View.OnClickListener {
    private static String TAG = "ContactsDumpActivity";
    private Button mCancelButton;
    private Button mConfirmButton;
    private Button mDeleteButton;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(3);
        setContentView(R.layout.contact_dump_activity);
        getWindow().setFeatureDrawableResource(3, android.R.drawable.ic_dialog_alert);
        this.mConfirmButton = (Button) findViewById(R.id.confirm);
        this.mCancelButton = (Button) findViewById(R.id.cancel);
        this.mDeleteButton = (Button) findViewById(R.id.delete);
        updateDeleteButton();
    }

    private void updateDeleteButton() {
        this.mDeleteButton.setEnabled(DataExporter.dumpFileExists(this));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.confirm:
                this.mConfirmButton.setEnabled(false);
                this.mCancelButton.setEnabled(false);
                new DumpDbTask().execute(new Void[0]);
                break;
            case R.id.delete:
                cleanup();
                updateDeleteButton();
                break;
            case R.id.cancel:
                finish();
                break;
        }
    }

    private void cleanup() {
        DataExporter.removeDumpFiles(this);
    }

    private class DumpDbTask extends AsyncTask<Void, Void, Uri> {
        private DumpDbTask() {
        }

        @Override
        protected void onPreExecute() {
            ContactsDumpActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Uri doInBackground(Void... voidArr) {
            try {
                return DataExporter.exportData(ContactsDumpActivity.this.getApplicationContext());
            } catch (IOException e) {
                Log.e(ContactsDumpActivity.TAG, "Failed to export", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (uri != null) {
                ContactsDumpActivity.this.emailFile(uri);
            }
        }
    }

    private void emailFile(Uri uri) {
        Log.i(TAG, "Drafting email");
        Intent intent = new Intent("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.SUBJECT", getString(R.string.debug_dump_email_subject));
        intent.putExtra("android.intent.extra.TEXT", getString(R.string.debug_dump_email_body));
        intent.setType("application/zip");
        intent.putExtra("android.intent.extra.STREAM", uri);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.debug_dump_email_sender_picker)), 0);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        updateDeleteButton();
        this.mConfirmButton.setEnabled(true);
        this.mCancelButton.setEnabled(true);
    }
}
