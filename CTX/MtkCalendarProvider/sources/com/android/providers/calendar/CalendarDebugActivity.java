package com.android.providers.calendar;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CalendarDebugActivity extends Activity implements View.OnClickListener {
    private static String TAG = "CalendarDebugActivity";
    private Button mCancelButton;
    private Button mConfirmButton;
    private Button mDeleteButton;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(3);
        setContentView(R.layout.dialog_activity);
        getWindow().setFeatureDrawableResource(3, android.R.drawable.ic_dialog_alert);
        this.mConfirmButton = (Button) findViewById(R.id.confirm);
        this.mCancelButton = (Button) findViewById(R.id.cancel);
        this.mDeleteButton = (Button) findViewById(R.id.delete);
        updateDeleteButton();
    }

    private void updateDeleteButton() {
        this.mDeleteButton.setEnabled(new File(Environment.getExternalStorageDirectory(), "calendar.db.zip").exists());
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
        Log.i(TAG, "Deleting calendar.db.zip");
        new File(Environment.getExternalStorageDirectory(), "calendar.db.zip").delete();
    }

    private class DumpDbTask extends AsyncTask<Void, Void, File> {
        private DumpDbTask() {
        }

        @Override
        protected void onPreExecute() {
            CalendarDebugActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected File doInBackground(Void... voidArr) throws Throwable {
            FileInputStream fileInputStream;
            ZipOutputStream zipOutputStream;
            ZipOutputStream zipOutputStream2;
            File file;
            File databasePath;
            ZipOutputStream zipOutputStream3 = null;
            try {
                try {
                    file = new File(Environment.getExternalStorageDirectory(), "calendar.db.zip");
                    file.delete();
                    Log.i(CalendarDebugActivity.TAG, "Outfile=" + file.getAbsolutePath());
                    databasePath = CalendarDebugActivity.this.getDatabasePath("calendar.db");
                    fileInputStream = new FileInputStream(databasePath);
                } catch (Throwable th) {
                    th = th;
                    zipOutputStream3 = zipOutputStream2;
                }
                try {
                    zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
                    try {
                        zipOutputStream.putNextEntry(new ZipEntry(databasePath.getName()));
                        byte[] bArr = new byte[4096];
                        int i = 0;
                        while (true) {
                            int i2 = fileInputStream.read(bArr);
                            if (i2 <= 0) {
                                break;
                            }
                            zipOutputStream.write(bArr, 0, i2);
                            i += i2;
                        }
                        zipOutputStream.closeEntry();
                        Log.i(CalendarDebugActivity.TAG, "bytes read " + i);
                        zipOutputStream.flush();
                        zipOutputStream.close();
                        MediaScannerConnection.scanFile(CalendarDebugActivity.this, new String[]{file.toString()}, new String[]{"application/zip"}, null);
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            Log.i(CalendarDebugActivity.TAG, "Error " + e.toString());
                        }
                        return file;
                    } catch (IOException e2) {
                        e = e2;
                        Log.i(CalendarDebugActivity.TAG, "Error " + e.toString());
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e3) {
                                Log.i(CalendarDebugActivity.TAG, "Error " + e3.toString());
                                return null;
                            }
                        }
                        if (zipOutputStream != null) {
                            zipOutputStream.close();
                        }
                        return null;
                    }
                } catch (IOException e4) {
                    e = e4;
                    zipOutputStream = null;
                } catch (Throwable th2) {
                    th = th2;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e5) {
                            Log.i(CalendarDebugActivity.TAG, "Error " + e5.toString());
                            throw th;
                        }
                    }
                    if (zipOutputStream3 != null) {
                        zipOutputStream3.close();
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                fileInputStream = null;
                zipOutputStream = null;
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = null;
            }
        }

        @Override
        protected void onPostExecute(File file) {
            if (file != null) {
                CalendarDebugActivity.this.emailFile(file);
            }
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        updateDeleteButton();
        this.mConfirmButton.setEnabled(true);
        this.mCancelButton.setEnabled(true);
    }

    private void emailFile(File file) {
        Log.i(TAG, "Drafting email to send " + file.getAbsolutePath());
        Intent intent = new Intent("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.SUBJECT", getString(R.string.debug_tool_email_subject));
        intent.putExtra("android.intent.extra.TEXT", getString(R.string.debug_tool_email_body));
        intent.setType("application/zip");
        intent.putExtra("android.intent.extra.STREAM", Uri.fromFile(file));
        startActivityForResult(Intent.createChooser(intent, getString(R.string.debug_tool_email_sender_picker)), 0);
    }
}
