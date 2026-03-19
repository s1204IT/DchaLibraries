package com.android.packageinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InstallStaging extends Activity {
    private static final String LOG_TAG = InstallStaging.class.getSimpleName();
    private File mStagedFile;
    private StagingAsyncTask mStagingTask;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.install_staging);
        if (bundle != null) {
            this.mStagedFile = new File(bundle.getString("STAGED_FILE"));
            if (!this.mStagedFile.exists()) {
                this.mStagedFile = null;
            }
        }
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                InstallStaging.lambda$onCreate$0(this.f$0, view);
            }
        });
    }

    public static void lambda$onCreate$0(InstallStaging installStaging, View view) {
        if (installStaging.mStagingTask != null) {
            installStaging.mStagingTask.cancel(true);
        }
        installStaging.setResult(0);
        installStaging.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mStagingTask == null) {
            if (this.mStagedFile == null) {
                try {
                    this.mStagedFile = TemporaryFileManager.getStagedFile(this);
                } catch (IOException e) {
                    showError();
                    return;
                }
            }
            this.mStagingTask = new StagingAsyncTask();
            this.mStagingTask.execute(getIntent().getData());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("STAGED_FILE", this.mStagedFile.getPath());
    }

    @Override
    protected void onDestroy() {
        if (this.mStagingTask != null) {
            this.mStagingTask.cancel(true);
        }
        super.onDestroy();
    }

    private void showError() {
        new ErrorDialog().showAllowingStateLoss(getFragmentManager(), "error");
        Intent intent = new Intent();
        intent.putExtra("android.intent.extra.INSTALL_RESULT", -2);
        setResult(1, intent);
    }

    public static class ErrorDialog extends DialogFragment {
        private Activity mActivity;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            this.mActivity = (Activity) context;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mActivity).setMessage(R.string.Parse_error_dlg_text).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.mActivity.finish();
                }
            }).create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            return alertDialogCreate;
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            super.onCancel(dialogInterface);
            this.mActivity.finish();
        }
    }

    private final class StagingAsyncTask extends AsyncTask<Uri, Void, Boolean> {
        private StagingAsyncTask() {
        }

        @Override
        protected Boolean doInBackground(Uri... uriArr) {
            Throwable th;
            Throwable th2;
            if (uriArr == null || uriArr.length <= 0) {
                return false;
            }
            try {
                InputStream inputStreamOpenInputStream = InstallStaging.this.getContentResolver().openInputStream(uriArr[0]);
                try {
                    if (inputStreamOpenInputStream == null) {
                        return false;
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(InstallStaging.this.mStagedFile);
                    try {
                        byte[] bArr = new byte[1048576];
                        while (true) {
                            int i = inputStreamOpenInputStream.read(bArr);
                            if (i < 0) {
                                $closeResource(null, fileOutputStream);
                                if (inputStreamOpenInputStream != null) {
                                    $closeResource(null, inputStreamOpenInputStream);
                                }
                                return true;
                            }
                            if (isCancelled()) {
                                break;
                            }
                            fileOutputStream.write(bArr, 0, i);
                        }
                    } catch (Throwable th3) {
                        try {
                            throw th3;
                        } catch (Throwable th4) {
                            th = th3;
                            th2 = th4;
                            $closeResource(th, fileOutputStream);
                            throw th2;
                        }
                    }
                } finally {
                    if (inputStreamOpenInputStream != null) {
                        $closeResource(null, inputStreamOpenInputStream);
                    }
                }
            } catch (IOException | IllegalStateException | SecurityException e) {
                Log.w(InstallStaging.LOG_TAG, "Error staging apk from content URI", e);
                return false;
            }
        }

        private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
            if (th == null) {
                autoCloseable.close();
                return;
            }
            try {
                autoCloseable.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (!bool.booleanValue()) {
                InstallStaging.this.showError();
                return;
            }
            Intent intent = new Intent(InstallStaging.this.getIntent());
            intent.setClass(InstallStaging.this, DeleteStagedFileOnResult.class);
            intent.setData(Uri.fromFile(InstallStaging.this.mStagedFile));
            if (intent.getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
                intent.addFlags(33554432);
            }
            intent.addFlags(65536);
            InstallStaging.this.startActivity(intent);
            InstallStaging.this.finish();
        }
    }
}
