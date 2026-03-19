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

    private static String[] resolveRootProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_DOCUMENT_PROJECTION;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo providerInfo) {
        this.mAuthority = providerInfo.authority;
        super.attachInfo(context, providerInfo);
    }

    @Override
    public Cursor queryRoots(String[] strArr) throws FileNotFoundException {
        Log.d("TestDocuments", "Someone asked for our roots!");
        MatrixCursor matrixCursor = new MatrixCursor(resolveRootProjection(strArr));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("root_id", "myRoot");
        rowBuilderNewRow.add("flags", 5);
        rowBuilderNewRow.add("title", "_Test title which is really long");
        rowBuilderNewRow.add("summary", SystemClock.elapsedRealtime() + " summary which is also super long text");
        rowBuilderNewRow.add("document_id", "myDoc");
        rowBuilderNewRow.add("available_bytes", 1024);
        return matrixCursor;
    }

    @Override
    public Cursor queryDocument(String str, String[] strArr) throws FileNotFoundException {
        MatrixCursor matrixCursor = new MatrixCursor(resolveDocumentProjection(strArr));
        includeFile(matrixCursor, str, 0);
        return matrixCursor;
    }

    @Override
    public String createDocument(String str, String str2, String str3) throws FileNotFoundException {
        return super.createDocument(str, str2, str3);
    }

    private static class CloudTask implements Runnable {
        private volatile boolean mFinished;
        private final Uri mNotifyUri;
        private final ContentResolver mResolver;

        public CloudTask(ContentResolver contentResolver, Uri uri) {
            this.mResolver = contentResolver;
            this.mNotifyUri = uri;
        }

        @Override
        public void run() {
            Log.d("TestDocuments", hashCode() + ": pretending to do some network!");
            SystemClock.sleep(2000L);
            Log.d("TestDocuments", hashCode() + ": network done!");
            this.mFinished = true;
            this.mResolver.notifyChange(this.mNotifyUri, (ContentObserver) null, false);
        }

        public boolean includeIfFinished(MatrixCursor matrixCursor) {
            Log.d("TestDocuments", hashCode() + ": includeIfFinished() found " + this.mFinished);
            if (!this.mFinished) {
                return false;
            }
            TestDocumentsProvider.includeFile(matrixCursor, "_networkfile1", 0);
            TestDocumentsProvider.includeFile(matrixCursor, "_networkfile2", 0);
            TestDocumentsProvider.includeFile(matrixCursor, "_networkfile3", 0);
            TestDocumentsProvider.includeFile(matrixCursor, "_networkfile4", 0);
            TestDocumentsProvider.includeFile(matrixCursor, "_networkfile5", 0);
            TestDocumentsProvider.includeFile(matrixCursor, "_networkfile6", 0);
            return true;
        }
    }

    private static class CloudCursor extends MatrixCursor {
        public final Bundle extras;
        public Object keepAlive;

        public CloudCursor(String[] strArr) {
            super(strArr);
            this.extras = new Bundle();
        }

        @Override
        public Bundle getExtras() {
            return this.extras;
        }
    }

    @Override
    public Cursor queryChildDocuments(String str, String[] strArr, String str2) throws FileNotFoundException {
        ContentResolver contentResolver = getContext().getContentResolver();
        Uri uriBuildDocumentUri = DocumentsContract.buildDocumentUri("com.example.documents", str);
        CloudCursor cloudCursor = new CloudCursor(resolveDocumentProjection(strArr));
        cloudCursor.setNotificationUri(contentResolver, uriBuildDocumentUri);
        includeFile(cloudCursor, "myNull", 0);
        includeFile(cloudCursor, "localfile1", 0);
        includeFile(cloudCursor, "localfile2", 1);
        includeFile(cloudCursor, "localfile3", 0);
        includeFile(cloudCursor, "localfile4", 0);
        synchronized (this) {
            CloudTask cloudTask = this.mTask != null ? this.mTask.get() : null;
            if (cloudTask == null) {
                Log.d("TestDocuments", "No network task found; starting!");
                cloudTask = new CloudTask(contentResolver, uriBuildDocumentUri);
                this.mTask = new WeakReference<>(cloudTask);
                new Thread(cloudTask).start();
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
            if (cloudTask.includeIfFinished(cloudCursor)) {
                cloudCursor.extras.putString("info", "Everything Went Better Than Expected and this message is quite long and verbose and maybe even too long");
                cloudCursor.extras.putString("error", "But then again, maybe our server ran into an error, which means we're going to have a bad time");
            } else {
                cloudCursor.extras.putBoolean("loading", true);
            }
            cloudCursor.keepAlive = cloudTask;
        }
        return cloudCursor;
    }

    @Override
    public Cursor queryRecentDocuments(String str, String[] strArr) throws FileNotFoundException {
        SystemClock.sleep(3000L);
        MatrixCursor matrixCursor = new MatrixCursor(resolveDocumentProjection(strArr));
        includeFile(matrixCursor, "It was /worth/ the_wait for?the file:with the&incredibly long name", 0);
        return matrixCursor;
    }

    @Override
    public ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String str, Point point, CancellationSignal cancellationSignal) throws FileNotFoundException {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint();
        paint.setColor(-16776961);
        canvas.drawColor(-65536);
        canvas.drawLine(0.0f, 0.0f, 32.0f, 32.0f, paint);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmapCreateBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        try {
            final ParcelFileDescriptor[] parcelFileDescriptorArrCreateReliablePipe = ParcelFileDescriptor.createReliablePipe();
            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object... objArr) {
                    try {
                        Streams.copy(byteArrayInputStream, new FileOutputStream(parcelFileDescriptorArrCreateReliablePipe[1].getFileDescriptor()));
                        IoUtils.closeQuietly(parcelFileDescriptorArrCreateReliablePipe[1]);
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[0]);
            return new AssetFileDescriptor(parcelFileDescriptorArrCreateReliablePipe[0], 0L, -1L);
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private static void includeFile(MatrixCursor matrixCursor, String str, int i) {
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", str);
        rowBuilderNewRow.add("_display_name", str);
        rowBuilderNewRow.add("last_modified", Long.valueOf(System.currentTimeMillis()));
        rowBuilderNewRow.add("flags", Integer.valueOf(i));
        if ("myDoc".equals(str)) {
            rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
            rowBuilderNewRow.add("flags", 8);
        } else if (!"myNull".equals(str)) {
            rowBuilderNewRow.add("mime_type", "application/octet-stream");
        }
    }
}
