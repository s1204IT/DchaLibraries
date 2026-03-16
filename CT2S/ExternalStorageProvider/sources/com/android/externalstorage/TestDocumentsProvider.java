package com.android.externalstorage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class TestDocumentsProvider extends DocumentsProvider {
    private String mAuthority;
    private WeakReference<CloudTask> mTask;
    private static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "summary", "document_id", "available_bytes"};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size"};

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        this.mAuthority = info.authority;
        super.attachInfo(context, info);
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        Log.d("TestDocuments", "Someone asked for our roots!");
        MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("root_id", "myRoot");
        row.add("flags", 5);
        row.add("title", "_Test title which is really long");
        row.add("summary", SystemClock.elapsedRealtime() + " summary which is also super long text");
        row.add("document_id", "myDoc");
        row.add("available_bytes", 1024);
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, documentId, 0);
        return result;
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        return super.createDocument(parentDocumentId, mimeType, displayName);
    }

    private static class CloudTask implements Runnable {
        private volatile boolean mFinished;
        private final Uri mNotifyUri;
        private final ContentResolver mResolver;

        public CloudTask(ContentResolver resolver, Uri notifyUri) {
            this.mResolver = resolver;
            this.mNotifyUri = notifyUri;
        }

        @Override
        public void run() {
            Log.d("TestDocuments", hashCode() + ": pretending to do some network!");
            SystemClock.sleep(2000L);
            Log.d("TestDocuments", hashCode() + ": network done!");
            this.mFinished = true;
            this.mResolver.notifyChange(this.mNotifyUri, (ContentObserver) null, false);
        }

        public boolean includeIfFinished(MatrixCursor result) {
            Log.d("TestDocuments", hashCode() + ": includeIfFinished() found " + this.mFinished);
            if (!this.mFinished) {
                return false;
            }
            TestDocumentsProvider.includeFile(result, "_networkfile1", 0);
            TestDocumentsProvider.includeFile(result, "_networkfile2", 0);
            TestDocumentsProvider.includeFile(result, "_networkfile3", 0);
            TestDocumentsProvider.includeFile(result, "_networkfile4", 0);
            TestDocumentsProvider.includeFile(result, "_networkfile5", 0);
            TestDocumentsProvider.includeFile(result, "_networkfile6", 0);
            return true;
        }
    }

    private static class CloudCursor extends MatrixCursor {
        public final Bundle extras;
        public Object keepAlive;

        public CloudCursor(String[] columnNames) {
            super(columnNames);
            this.extras = new Bundle();
        }

        @Override
        public Bundle getExtras() {
            return this.extras;
        }
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        ContentResolver resolver = getContext().getContentResolver();
        Uri notifyUri = DocumentsContract.buildDocumentUri("com.example.documents", parentDocumentId);
        CloudCursor result = new CloudCursor(resolveDocumentProjection(projection));
        result.setNotificationUri(resolver, notifyUri);
        includeFile(result, "myNull", 0);
        includeFile(result, "localfile1", 0);
        includeFile(result, "localfile2", 1);
        includeFile(result, "localfile3", 0);
        includeFile(result, "localfile4", 0);
        synchronized (this) {
            CloudTask task = this.mTask != null ? this.mTask.get() : null;
            if (task == null) {
                Log.d("TestDocuments", "No network task found; starting!");
                task = new CloudTask(resolver, notifyUri);
                this.mTask = new WeakReference<>(task);
                new Thread(task).start();
                new Thread() {
                    @Override
                    public void run() {
                        while (TestDocumentsProvider.this.mTask.get() != null) {
                            SystemClock.sleep(200L);
                            System.gc();
                            System.runFinalization();
                        }
                        Log.d("TestDocuments", "AHA! THE CLOUD TASK WAS GC'ED!");
                    }
                }.start();
            }
            if (task.includeIfFinished(result)) {
                result.extras.putString("info", "Everything Went Better Than Expected and this message is quite long and verbose and maybe even too long");
                result.extras.putString("error", "But then again, maybe our server ran into an error, which means we're going to have a bad time");
            } else {
                result.extras.putBoolean("loading", true);
            }
            result.keepAlive = task;
        }
        return result;
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection) throws FileNotFoundException {
        SystemClock.sleep(3000L);
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, "It was /worth/ the_wait for?the file:with the&incredibly long name", 0);
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String docId, String mode, CancellationSignal signal) throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String docId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(-16776961);
        canvas.drawColor(-65536);
        canvas.drawLine(0.0f, 0.0f, 32.0f, 32.0f, paint);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        try {
            final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createReliablePipe();
            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object... params) {
                    FileOutputStream fos = new FileOutputStream(fds[1].getFileDescriptor());
                    try {
                        Streams.copy(bis, fos);
                        IoUtils.closeQuietly(fds[1]);
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[0]);
            return new AssetFileDescriptor(fds[0], 0L, -1L);
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private static void includeFile(MatrixCursor result, String docId, int flags) {
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", docId);
        row.add("last_modified", Long.valueOf(System.currentTimeMillis()));
        row.add("flags", Integer.valueOf(flags));
        if ("myDoc".equals(docId)) {
            row.add("mime_type", "vnd.android.document/directory");
            row.add("flags", 8);
        } else if (!"myNull".equals(docId)) {
            row.add("mime_type", "application/octet-stream");
        }
    }
}
