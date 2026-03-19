package android.print;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.FileUtils;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.print.PrintDocumentAdapter;
import android.util.Log;
import com.android.internal.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class PrintFileDocumentAdapter extends PrintDocumentAdapter {
    private static final String LOG_TAG = "PrintedFileDocAdapter";
    private final Context mContext;
    private final PrintDocumentInfo mDocumentInfo;
    private final File mFile;
    private WriteFileAsyncTask mWriteFileAsyncTask;

    public PrintFileDocumentAdapter(Context context, File file, PrintDocumentInfo printDocumentInfo) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null!");
        }
        if (printDocumentInfo == null) {
            throw new IllegalArgumentException("documentInfo cannot be null!");
        }
        this.mContext = context;
        this.mFile = file;
        this.mDocumentInfo = printDocumentInfo;
    }

    @Override
    public void onLayout(PrintAttributes printAttributes, PrintAttributes printAttributes2, CancellationSignal cancellationSignal, PrintDocumentAdapter.LayoutResultCallback layoutResultCallback, Bundle bundle) {
        layoutResultCallback.onLayoutFinished(this.mDocumentInfo, false);
    }

    @Override
    public void onWrite(PageRange[] pageRangeArr, ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, PrintDocumentAdapter.WriteResultCallback writeResultCallback) {
        this.mWriteFileAsyncTask = new WriteFileAsyncTask(parcelFileDescriptor, cancellationSignal, writeResultCallback);
        this.mWriteFileAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    private final class WriteFileAsyncTask extends AsyncTask<Void, Void, Void> {
        private final CancellationSignal mCancellationSignal;
        private final ParcelFileDescriptor mDestination;
        private final PrintDocumentAdapter.WriteResultCallback mResultCallback;

        public WriteFileAsyncTask(ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, PrintDocumentAdapter.WriteResultCallback writeResultCallback) {
            this.mDestination = parcelFileDescriptor;
            this.mResultCallback = writeResultCallback;
            this.mCancellationSignal = cancellationSignal;
            this.mCancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                @Override
                public void onCancel() {
                    WriteFileAsyncTask.this.cancel(true);
                }
            });
        }

        @Override
        protected Void doInBackground(Void... voidArr) throws Exception {
            Throwable th;
            Throwable th2;
            Throwable th3;
            Throwable th4;
            try {
                FileInputStream fileInputStream = new FileInputStream(PrintFileDocumentAdapter.this.mFile);
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(this.mDestination.getFileDescriptor());
                    try {
                        FileUtils.copy(fileInputStream, fileOutputStream, (FileUtils.ProgressListener) null, this.mCancellationSignal);
                        $closeResource(null, fileOutputStream);
                        $closeResource(null, fileInputStream);
                    } catch (Throwable th5) {
                        try {
                            throw th5;
                        } catch (Throwable th6) {
                            th3 = th5;
                            th4 = th6;
                            $closeResource(th3, fileOutputStream);
                            throw th4;
                        }
                    }
                } catch (Throwable th7) {
                    try {
                        throw th7;
                    } catch (Throwable th8) {
                        th = th7;
                        th2 = th8;
                        $closeResource(th, fileInputStream);
                        throw th2;
                    }
                }
            } catch (OperationCanceledException e) {
            } catch (IOException e2) {
                Log.e(PrintFileDocumentAdapter.LOG_TAG, "Error writing data!", e2);
                this.mResultCallback.onWriteFailed(PrintFileDocumentAdapter.this.mContext.getString(R.string.write_fail_reason_cannot_write));
            }
            return null;
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
        protected void onPostExecute(Void r4) {
            this.mResultCallback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        }

        @Override
        protected void onCancelled(Void r3) {
            this.mResultCallback.onWriteFailed(PrintFileDocumentAdapter.this.mContext.getString(R.string.write_fail_reason_cancelled));
        }
    }
}
