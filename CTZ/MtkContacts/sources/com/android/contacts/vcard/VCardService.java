package com.android.contacts.vcard;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.SparseArray;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class VCardService extends Service {
    private static final SparseArray<ProcessorBase> mRunningJobMap = new SparseArray<>();
    private MyBinder mBinder;
    private String mCallingActivity;
    public boolean mCaching = false;
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private int mCurrentJobId = 1;
    private final List<CustomMediaScannerConnectionClient> mRemainingScannerConnections = new ArrayList();
    private final Set<String> mReservedDestination = new HashSet();

    private class CustomMediaScannerConnectionClient implements MediaScannerConnection.MediaScannerConnectionClient {
        final MediaScannerConnection mConnection;
        final String mPath;

        public CustomMediaScannerConnectionClient(String str) {
            this.mConnection = new MediaScannerConnection(VCardService.this, this);
            this.mPath = str;
        }

        public void start() {
            this.mConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            this.mConnection.scanFile(this.mPath, null);
        }

        @Override
        public void onScanCompleted(String str, Uri uri) {
            this.mConnection.disconnect();
            VCardService.this.removeConnectionClient(this);
        }
    }

    public class MyBinder extends Binder {
        public MyBinder() {
        }

        public VCardService getService() {
            return VCardService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBinder = new MyBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.i("VCardService", "[onStartCommand]intent = " + intent);
        if (intent != null && intent.getExtras() != null) {
            this.mCallingActivity = intent.getExtras().getString("CALLING_ACTIVITY");
            Log.i("VCardService", "[onStartCommand]mCallingActivity = " + this.mCallingActivity);
            return 2;
        }
        Log.i("VCardService", "[onStartCommand]mCallingActivity = null");
        this.mCallingActivity = null;
        return 2;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override
    public void onDestroy() {
        cancelAllRequestsAndShutdown();
        clearCache();
        stopForeground(false);
        super.onDestroy();
    }

    public synchronized void handleImportRequest(List<ImportRequest> list, VCardImportExportListener vCardImportExportListener) {
        Notification notificationOnImportProcessed;
        int size = list.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            }
            ImportRequest importRequest = list.get(i);
            if (!tryExecute(new ImportProcessor(this, vCardImportExportListener, importRequest, this.mCurrentJobId))) {
                break;
            }
            if (vCardImportExportListener != null && (notificationOnImportProcessed = vCardImportExportListener.onImportProcessed(importRequest, this.mCurrentJobId, i)) != null) {
                startForeground(this.mCurrentJobId, notificationOnImportProcessed);
            }
            this.mCurrentJobId++;
            i++;
        }
    }

    public synchronized void handleExportRequest(ExportRequest exportRequest, VCardImportExportListener vCardImportExportListener) {
        Notification notificationOnExportProcessed;
        if (tryExecute(new ExportProcessor(this, exportRequest, this.mCurrentJobId, this.mCallingActivity))) {
            String encodedPath = exportRequest.destUri.getEncodedPath();
            if (!this.mReservedDestination.add(encodedPath)) {
                Log.w("VCardService", String.format("The path %s is already reserved. Reject export request", encodedPath));
                if (vCardImportExportListener != null) {
                    vCardImportExportListener.onExportFailed(exportRequest);
                }
            } else {
                if (vCardImportExportListener != null && (notificationOnExportProcessed = vCardImportExportListener.onExportProcessed(exportRequest, this.mCurrentJobId)) != null) {
                    startForeground(this.mCurrentJobId, notificationOnExportProcessed);
                }
                this.mCurrentJobId++;
            }
        } else if (vCardImportExportListener != null) {
            vCardImportExportListener.onExportFailed(exportRequest);
        }
    }

    private synchronized boolean tryExecute(ProcessorBase processorBase) {
        try {
            this.mExecutorService.execute(processorBase);
            mRunningJobMap.put(this.mCurrentJobId, processorBase);
        } catch (RejectedExecutionException e) {
            Log.w("VCardService", "Failed to excetute a job.", e);
            return false;
        }
        return true;
    }

    public synchronized void handleCancelRequest(CancelRequest cancelRequest, VCardImportExportListener vCardImportExportListener) {
        int i = cancelRequest.jobId;
        ProcessorBase processorBase = mRunningJobMap.get(i);
        mRunningJobMap.remove(i);
        if (processorBase != null) {
            processorBase.cancel(true);
            int type = processorBase.getType();
            if (vCardImportExportListener != null) {
                vCardImportExportListener.onCancelRequest(cancelRequest, type);
            }
            if (type == 2) {
                String encodedPath = ((ExportProcessor) processorBase).getRequest().destUri.getEncodedPath();
                Log.i("VCardService", String.format("Cancel reservation for the path %s if appropriate", encodedPath));
                if (!this.mReservedDestination.remove(encodedPath)) {
                    Log.w("VCardService", "Not reserved.");
                }
            }
        } else {
            Log.w("VCardService", String.format("Tried to remove unknown job (id: %d)", Integer.valueOf(i)));
        }
        stopServiceIfAppropriate();
    }

    private synchronized void stopServiceIfAppropriate() {
        if (mRunningJobMap.size() > 0) {
            int size = mRunningJobMap.size();
            int[] iArr = new int[size];
            for (int i = 0; i < size; i++) {
                int iKeyAt = mRunningJobMap.keyAt(i);
                if (!mRunningJobMap.valueAt(i).isDone()) {
                    Log.i("VCardService", String.format("Found unfinished job (id: %d)", Integer.valueOf(iKeyAt)));
                    for (int i2 = 0; i2 < i; i2++) {
                        mRunningJobMap.remove(iArr[i2]);
                    }
                    return;
                }
                iArr[i] = iKeyAt;
            }
            mRunningJobMap.clear();
        }
        if (!this.mRemainingScannerConnections.isEmpty()) {
            Log.i("VCardService", "MediaScanner update is in progress.");
        } else if (this.mCaching) {
            Log.d("VCardService", "[stopServiceIfAppropriate] caching vcard file, can't stop service");
        } else {
            Log.i("VCardService", "No unfinished job. Stop this service.");
            stopSelf();
        }
    }

    synchronized void updateMediaScanner(String str) {
        if (this.mExecutorService.isShutdown()) {
            Log.w("VCardService", "MediaScanner update is requested after executor's being shut down. Ignoring the update request");
            return;
        }
        CustomMediaScannerConnectionClient customMediaScannerConnectionClient = new CustomMediaScannerConnectionClient(str);
        this.mRemainingScannerConnections.add(customMediaScannerConnectionClient);
        customMediaScannerConnectionClient.start();
    }

    private synchronized void removeConnectionClient(CustomMediaScannerConnectionClient customMediaScannerConnectionClient) {
        this.mRemainingScannerConnections.remove(customMediaScannerConnectionClient);
        stopServiceIfAppropriate();
    }

    synchronized void handleFinishImportNotification(int i, boolean z) {
        mRunningJobMap.remove(i);
        stopServiceIfAppropriate();
    }

    synchronized void handleFinishExportNotification(int i, boolean z) {
        ProcessorBase processorBase = mRunningJobMap.get(i);
        mRunningJobMap.remove(i);
        if (processorBase == null) {
            Log.w("VCardService", String.format("Tried to remove unknown job (id: %d)", Integer.valueOf(i)));
        } else if (!(processorBase instanceof ExportProcessor)) {
            Log.w("VCardService", String.format("Removed job (id: %s) isn't ExportProcessor", Integer.valueOf(i)));
        } else {
            this.mReservedDestination.remove(((ExportProcessor) processorBase).getRequest().destUri.getEncodedPath());
        }
        stopServiceIfAppropriate();
    }

    private synchronized void cancelAllRequestsAndShutdown() {
        for (int i = 0; i < mRunningJobMap.size(); i++) {
            mRunningJobMap.valueAt(i).cancel(true);
        }
        mRunningJobMap.clear();
    }

    private void clearCache() {
        for (String str : fileList()) {
            if (str.startsWith("import_tmp_")) {
                Log.i("VCardService", "Remove a temporary file: " + str);
                deleteFile(str);
            }
        }
    }

    public static synchronized boolean isProcessing(int i) {
        Log.d("VCardService", "mRunningJobMap.size() : " + mRunningJobMap.size());
        if (mRunningJobMap.size() <= 0) {
            return false;
        }
        if (mRunningJobMap.size() > 0) {
            for (int i2 = 0; i2 < mRunningJobMap.size(); i2++) {
                ProcessorBase processorBaseValueAt = mRunningJobMap.valueAt(i2);
                Log.d("VCardService", "[isProcessing] processor.getType(): " + processorBaseValueAt.getType() + " | requestType : " + i);
                if (processorBaseValueAt.getType() == i) {
                    return true;
                }
            }
        }
        return false;
    }
}
