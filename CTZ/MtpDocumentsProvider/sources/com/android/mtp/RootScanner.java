package com.android.mtp;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Process;
import android.provider.DocumentsContract;
import android.util.Log;
import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class RootScanner {
    private UpdateRootsRunnable mCurrentTask;
    final MtpDatabase mDatabase;
    ExecutorService mExecutor;
    final MtpManager mManager;
    final ContentResolver mResolver;

    RootScanner(ContentResolver contentResolver, MtpManager mtpManager, MtpDatabase mtpDatabase) {
        this.mResolver = contentResolver;
        this.mManager = mtpManager;
        this.mDatabase = mtpDatabase;
    }

    void notifyChange() {
        this.mResolver.notifyChange(DocumentsContract.buildRootsUri("com.android.mtp.documents"), (ContentObserver) null, false);
    }

    synchronized CountDownLatch resume() {
        if (this.mExecutor == null) {
            this.mExecutor = Executors.newSingleThreadExecutor();
        }
        if (this.mCurrentTask != null) {
            this.mCurrentTask.stop();
        }
        this.mCurrentTask = new UpdateRootsRunnable();
        this.mExecutor.execute(this.mCurrentTask);
        return this.mCurrentTask.mFirstScanCompleted;
    }

    synchronized void pause() throws InterruptedException, TimeoutException {
        if (this.mExecutor == null) {
            return;
        }
        this.mExecutor.shutdownNow();
        try {
            if (this.mExecutor.awaitTermination(2000L, TimeUnit.MILLISECONDS)) {
            } else {
                throw new TimeoutException("Timeout for terminating RootScanner's background thread.");
            }
        } finally {
            this.mExecutor = null;
        }
    }

    private final class UpdateRootsRunnable implements Runnable {
        final CountDownLatch mFirstScanCompleted;
        final CountDownLatch mStopped;

        private UpdateRootsRunnable() {
            this.mStopped = new CountDownLatch(1);
            this.mFirstScanCompleted = new CountDownLatch(1);
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            int i = 0;
            while (this.mStopped.getCount() > 0) {
                MtpDeviceRecord[] devices = RootScanner.this.mManager.getDevices();
                try {
                    RootScanner.this.mDatabase.getMapper().startAddingDocuments(null);
                    boolean z = false;
                    for (MtpDeviceRecord mtpDeviceRecord : devices) {
                        if (RootScanner.this.mDatabase.getMapper().putDeviceDocument(mtpDeviceRecord)) {
                            z = true;
                        }
                    }
                    if (RootScanner.this.mDatabase.getMapper().stopAddingDocuments(null)) {
                        z = true;
                    }
                    for (MtpDeviceRecord mtpDeviceRecord2 : devices) {
                        String documentIdForDevice = RootScanner.this.mDatabase.getDocumentIdForDevice(mtpDeviceRecord2.deviceId);
                        if (documentIdForDevice != null) {
                            try {
                                RootScanner.this.mDatabase.getMapper().startAddingDocuments(documentIdForDevice);
                                if (RootScanner.this.mDatabase.getMapper().putStorageDocuments(documentIdForDevice, mtpDeviceRecord2.operationsSupported, mtpDeviceRecord2.roots)) {
                                    z = true;
                                }
                                if (RootScanner.this.mDatabase.getMapper().stopAddingDocuments(documentIdForDevice)) {
                                    z = true;
                                }
                            } catch (FileNotFoundException e) {
                                Log.e("MtpDocumentsProvider", "Parent document is gone.", e);
                            }
                        }
                    }
                    if (z) {
                        RootScanner.this.notifyChange();
                    }
                    this.mFirstScanCompleted.countDown();
                    i++;
                    if (devices.length != 0) {
                        try {
                            this.mStopped.await(((long) i) > 10 ? 30000L : 2000L, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e2) {
                            return;
                        }
                    } else {
                        return;
                    }
                } catch (FileNotFoundException e3) {
                    Log.e("MtpDocumentsProvider", "Unexpected FileNotFoundException", e3);
                    throw new AssertionError("Unexpected exception for the top parent", e3);
                }
            }
        }

        void stop() {
            this.mStopped.countDown();
        }
    }
}
