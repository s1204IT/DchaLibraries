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
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CalendarDebugActivity extends Activity implements View.OnClickListener {
    private static String TAG = "CalendarDebugActivity";
    private Button mCancelButton;
    private Button mConfirmButton;
    private Button mDeleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(3);
        setContentView(R.layout.dialog_activity);
        getWindow().setFeatureDrawableResource(3, android.R.drawable.ic_dialog_alert);
        this.mConfirmButton = (Button) findViewById(R.id.confirm);
        this.mCancelButton = (Button) findViewById(R.id.cancel);
        this.mDeleteButton = (Button) findViewById(R.id.delete);
        updateDeleteButton();
    }

    private void updateDeleteButton() {
        boolean fileExist = new File(Environment.getExternalStorageDirectory(), "calendar.db.zip").exists();
        this.mDeleteButton.setEnabled(fileExist);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
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
        File outFile = new File(Environment.getExternalStorageDirectory(), "calendar.db.zip");
        outFile.delete();
    }

    private class DumpDbTask extends AsyncTask<Void, Void, File> {
        private DumpDbTask() {
        }

        @Override
        protected void onPreExecute() {
            CalendarDebugActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected File doInBackground(Void... params) throws Throwable {
            File outFile;
            File inFile;
            InputStream is;
            ZipOutputStream os;
            byte[] buf;
            int totalLen;
            InputStream is2 = null;
            ZipOutputStream os2 = null;
            try {
                try {
                    File path = Environment.getExternalStorageDirectory();
                    outFile = new File(path, "calendar.db.zip");
                    outFile.delete();
                    Log.i(CalendarDebugActivity.TAG, "Outfile=" + outFile.getAbsolutePath());
                    inFile = CalendarDebugActivity.this.getDatabasePath("calendar.db");
                    is = new FileInputStream(inFile);
                    try {
                        os = new ZipOutputStream(new FileOutputStream(outFile));
                    } catch (IOException e) {
                        e = e;
                        is2 = is;
                    } catch (Throwable th) {
                        th = th;
                        is2 = is;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e2) {
                e = e2;
            }
            try {
                os.putNextEntry(new ZipEntry(inFile.getName()));
                buf = new byte[4096];
                totalLen = 0;
            } catch (IOException e3) {
                e = e3;
                os2 = os;
                is2 = is;
                Log.i(CalendarDebugActivity.TAG, "Error " + e.toString());
                if (is2 != null) {
                    try {
                        is2.close();
                    } catch (IOException e4) {
                        Log.i(CalendarDebugActivity.TAG, "Error " + e4.toString());
                        outFile = null;
                        return outFile;
                    }
                }
                if (os2 != null) {
                    os2.close();
                }
                outFile = null;
            } catch (Throwable th3) {
                th = th3;
                os2 = os;
                is2 = is;
                if (is2 != null) {
                    try {
                        is2.close();
                    } catch (IOException e5) {
                        Log.i(CalendarDebugActivity.TAG, "Error " + e5.toString());
                        throw th;
                    }
                }
                if (os2 != null) {
                    os2.close();
                }
                throw th;
            }
            while (true) {
                int len = is.read(buf);
                if (len <= 0) {
                    break;
                }
                os.write(buf, 0, len);
                totalLen += len;
                return outFile;
            }
            os.closeEntry();
            Log.i(CalendarDebugActivity.TAG, "bytes read " + totalLen);
            os.flush();
            os.close();
            os2 = null;
            MediaScannerConnection.scanFile(CalendarDebugActivity.this, new String[]{outFile.toString()}, new String[]{"application/zip"}, null);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e6) {
                    Log.i(CalendarDebugActivity.TAG, "Error " + e6.toString());
                }
            }
            if (0 != 0) {
                os2.close();
            }
            is2 = is;
            return outFile;
        }

        @Override
        protected void onPostExecute(File outFile) {
            if (outFile != null) {
                CalendarDebugActivity.this.emailFile(outFile);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
