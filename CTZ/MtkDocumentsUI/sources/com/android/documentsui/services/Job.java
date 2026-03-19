package com.android.documentsui.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.Metrics;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.files.FilesActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class Job implements Runnable {
    static final boolean $assertionsDisabled = false;
    final Context appContext;
    final String id;
    final Listener listener;
    private final Features mFeatures;
    final UrisSupplier mResourceUris;
    final int operationType;
    final Context service;
    final DocumentStack stack;
    int failureCount = 0;
    final ArrayList<DocumentInfo> failedDocs = new ArrayList<>();
    final ArrayList<Uri> failedUris = new ArrayList<>();
    final CancellationSignal mSignal = new CancellationSignal();
    private final Map<String, ContentProviderClient> mClients = new HashMap();
    private volatile int mState = 0;
    final Notification.Builder mProgressBuilder = createProgressBuilder();

    interface Listener {
        void onFinished(Job job);

        void onStart(Job job);
    }

    abstract Notification.Builder createProgressBuilder();

    abstract void finish();

    abstract Notification getFailureNotification();

    abstract Notification getProgressNotification();

    abstract Notification getSetupNotification();

    abstract Notification getWarningNotification();

    abstract void start();

    Job(Context context, Listener listener, String str, int i, DocumentStack documentStack, UrisSupplier urisSupplier, Features features) {
        this.service = context;
        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.operationType = i;
        this.id = str;
        this.stack = documentStack;
        this.mResourceUris = urisSupplier;
        this.mFeatures = features;
    }

    @Override
    public final void run() {
        if (isCanceled()) {
            return;
        }
        this.mState = 1;
        this.listener.onStart(this);
        int i = 3;
        try {
            try {
                if (setUp() && !isCanceled()) {
                    this.mState = 2;
                    start();
                }
            } catch (RuntimeException e) {
                Log.e("Job", "Operation failed due to an unhandled runtime exception.", e);
                Metrics.logFileOperationErrors(this.service, this.operationType, this.failedDocs, this.failedUris);
                if (this.mState != 1 && this.mState != 2) {
                }
            }
        } finally {
            if (this.mState != 1 && this.mState != 2) {
                i = this.mState;
            }
            this.mState = i;
            finish();
            this.listener.onFinished(this);
            this.mResourceUris.dispose();
        }
    }

    boolean setUp() {
        return true;
    }

    Uri getDataUriForIntent(String str) {
        return Uri.parse(String.format("data,%s-%s", str, this.id));
    }

    ContentProviderClient getClient(Uri uri) throws RemoteException {
        ContentProviderClient contentProviderClient = this.mClients.get(uri.getAuthority());
        if (contentProviderClient == null) {
            ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(getContentResolver(), uri.getAuthority());
            this.mClients.put(uri.getAuthority(), contentProviderClientAcquireUnstableProviderOrThrow);
            return contentProviderClientAcquireUnstableProviderOrThrow;
        }
        return contentProviderClient;
    }

    ContentProviderClient getClient(DocumentInfo documentInfo) throws RemoteException {
        return getClient(documentInfo.derivedUri);
    }

    final void cleanup() {
        Iterator<ContentProviderClient> it = this.mClients.values().iterator();
        while (it.hasNext()) {
            ContentProviderClient.releaseQuietly(it.next());
        }
    }

    final int getState() {
        return this.mState;
    }

    final void cancel() {
        this.mState = 4;
        this.mSignal.cancel();
        Metrics.logFileOperationCancelled(this.service, this.operationType);
    }

    final boolean isCanceled() {
        return this.mState == 4;
    }

    final boolean isFinished() {
        return this.mState == 4 || this.mState == 3;
    }

    final ContentResolver getContentResolver() {
        return this.service.getContentResolver();
    }

    void onFileFailed(DocumentInfo documentInfo) {
        this.failureCount++;
        this.failedDocs.add(documentInfo);
    }

    void onResolveFailed(Uri uri) {
        this.failureCount++;
        this.failedUris.add(uri);
    }

    final boolean hasFailures() {
        return this.failureCount > 0;
    }

    boolean hasWarnings() {
        return false;
    }

    final void deleteDocument(DocumentInfo documentInfo, DocumentInfo documentInfo2) throws ResourceException {
        if (documentInfo2 != null) {
            try {
                if (documentInfo.isRemoveSupported()) {
                    DocumentsContract.removeDocument(getClient(documentInfo), documentInfo.derivedUri, documentInfo2.derivedUri);
                    return;
                }
            } catch (RemoteException | RuntimeException e) {
                throw new ResourceException("Failed to delete file %s due to an exception.", documentInfo.derivedUri, e);
            }
        }
        if (documentInfo.isDeleteSupported()) {
            DocumentsContract.deleteDocument(getClient(documentInfo), documentInfo.derivedUri);
            return;
        }
        throw new ResourceException("Unable to delete source document. File is not deletable or removable: %s.", documentInfo.derivedUri);
    }

    Notification getSetupNotification(String str) {
        this.mProgressBuilder.setProgress(0, 0, true).setContentText(str);
        return this.mProgressBuilder.build();
    }

    Notification getFailureNotification(int i, int i2) {
        Intent intentBuildNavigateIntent = buildNavigateIntent("failure");
        intentBuildNavigateIntent.putExtra("com.android.documentsui.DIALOG_TYPE", 1);
        intentBuildNavigateIntent.putExtra("com.android.documentsui.OPERATION_TYPE", this.operationType);
        intentBuildNavigateIntent.putParcelableArrayListExtra("com.android.documentsui.FAILED_DOCS", this.failedDocs);
        intentBuildNavigateIntent.putParcelableArrayListExtra("com.android.documentsui.FAILED_URIS", this.failedUris);
        return createNotificationBuilder().setContentTitle(this.service.getResources().getQuantityString(i, this.failureCount, Integer.valueOf(this.failureCount))).setContentText(this.service.getString(R.string.notification_touch_for_details)).setContentIntent(PendingIntent.getActivity(this.appContext, 0, intentBuildNavigateIntent, 1207959552)).setCategory("err").setSmallIcon(i2).setAutoCancel(true).build();
    }

    final Notification.Builder createProgressBuilder(String str, int i, String str2, int i2) {
        Notification.Builder ongoing = createNotificationBuilder().setContentTitle(str).setContentIntent(PendingIntent.getActivity(this.appContext, 0, buildNavigateIntent("progress"), 0)).setCategory("progress").setSmallIcon(i).setOngoing(true);
        ongoing.addAction(i2, str2, PendingIntent.getService(this.service, 0, createCancelIntent(), 1342177280));
        return ongoing;
    }

    Notification.Builder createNotificationBuilder() {
        if (this.mFeatures.isNotificationChannelEnabled()) {
            return new Notification.Builder(this.service, "channel_id");
        }
        return new Notification.Builder(this.service);
    }

    Intent buildNavigateIntent(String str) {
        Intent intent = new Intent(this.service, (Class<?>) FilesActivity.class);
        intent.addFlags(268435456);
        intent.setData(getDataUriForIntent(str));
        intent.putExtra("com.android.documentsui.STACK", this.stack);
        return intent;
    }

    Intent createCancelIntent() {
        Intent intent = new Intent(this.service, (Class<?>) FileOperationService.class);
        intent.setData(getDataUriForIntent("cancel"));
        intent.putExtra("com.android.documentsui.CANCEL", true);
        intent.putExtra("com.android.documentsui.JOB_ID", this.id);
        return intent;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Job");
        sb.append("{");
        sb.append("id=" + this.id);
        sb.append("}");
        return sb.toString();
    }
}
