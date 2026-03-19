package android.print;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PrintDocumentAdapter;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import libcore.io.IoUtils;

public class PrintFileDocumentAdapter extends PrintDocumentAdapter {
    private static final String LOG_TAG = "PrintedFileDocAdapter";
    private final Context mContext;
    private final PrintDocumentInfo mDocumentInfo;
    private final File mFile;
    private WriteFileAsyncTask mWriteFileAsyncTask;

    public PrintFileDocumentAdapter(Context context, File file, PrintDocumentInfo documentInfo) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null!");
        }
        if (documentInfo == null) {
            throw new IllegalArgumentException("documentInfo cannot be null!");
        }
        this.mContext = context;
        this.mFile = file;
        this.mDocumentInfo = documentInfo;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, PrintDocumentAdapter.LayoutResultCallback callback, Bundle metadata) {
        callback.onLayoutFinished(this.mDocumentInfo, false);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, PrintDocumentAdapter.WriteResultCallback callback) {
        this.mWriteFileAsyncTask = new WriteFileAsyncTask(destination, cancellationSignal, callback);
        this.mWriteFileAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    private final class WriteFileAsyncTask extends AsyncTask<Void, Void, Void> {
        private final CancellationSignal mCancellationSignal;
        private final ParcelFileDescriptor mDestination;
        private final PrintDocumentAdapter.WriteResultCallback mResultCallback;

        public WriteFileAsyncTask(ParcelFileDescriptor destination, CancellationSignal cancellationSignal, PrintDocumentAdapter.WriteResultCallback callback) {
            this.mDestination = destination;
            this.mResultCallback = callback;
            this.mCancellationSignal = cancellationSignal;
            this.mCancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                @Override
                public void onCancel() {
                    WriteFileAsyncTask.this.cancel(true);
                }
            });
        }

        @Override
        protected Void doInBackground(Void... params) throws Throwable {
            int readByteCount;
            FileInputStream fileInputStream = null;
            FileOutputStream fileOutputStream = new FileOutputStream(this.mDestination.getFileDescriptor());
            byte[] buffer = new byte[8192];
            try {
                try {
                    FileInputStream fileInputStream2 = new FileInputStream(PrintFileDocumentAdapter.this.mFile);
                    while (!isCancelled() && (readByteCount = fileInputStream2.read(buffer)) >= 0) {
                        try {
                            fileOutputStream.write(buffer, 0, readByteCount);
                        } catch (IOException e) {
                            ioe = e;
                            fileInputStream = fileInputStream2;
                            Log.e(PrintFileDocumentAdapter.LOG_TAG, "Error writing data!", ioe);
                            this.mResultCallback.onWriteFailed(PrintFileDocumentAdapter.this.mContext.getString(17040770));
                            IoUtils.closeQuietly(fileInputStream);
                            IoUtils.closeQuietly(fileOutputStream);
                            return null;
                        } catch (Throwable th) {
                            th = th;
                            fileInputStream = fileInputStream2;
                            IoUtils.closeQuietly(fileInputStream);
                            IoUtils.closeQuietly(fileOutputStream);
                            throw th;
                        }
                    }
                    IoUtils.closeQuietly(fileInputStream2);
                    IoUtils.closeQuietly(fileOutputStream);
                    return null;
                } catch (IOException e2) {
                    ioe = e2;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            this.mResultCallback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        }

        @Override
        protected void onCancelled(Void result) {
            this.mResultCallback.onWriteFailed(PrintFileDocumentAdapter.this.mContext.getString(17040769));
        }
    }
}
