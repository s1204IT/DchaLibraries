package com.android.mtp;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class PipeManager {
    final MtpDatabase mDatabase;
    final ExecutorService mExecutor;

    PipeManager(MtpDatabase mtpDatabase) {
        this(mtpDatabase, Executors.newSingleThreadExecutor());
    }

    PipeManager(MtpDatabase mtpDatabase, ExecutorService executorService) {
        this.mDatabase = mtpDatabase;
        this.mExecutor = executorService;
    }

    ParcelFileDescriptor readDocument(MtpManager mtpManager, Identifier identifier) throws IOException {
        ImportFileTask importFileTask = new ImportFileTask(mtpManager, identifier);
        this.mExecutor.execute(importFileTask);
        return importFileTask.getReadingFileDescriptor();
    }

    ParcelFileDescriptor readThumbnail(MtpManager mtpManager, Identifier identifier) throws IOException {
        GetThumbnailTask getThumbnailTask = new GetThumbnailTask(mtpManager, identifier);
        this.mExecutor.execute(getThumbnailTask);
        return getThumbnailTask.getReadingFileDescriptor();
    }

    private static abstract class Task implements Runnable {
        protected final ParcelFileDescriptor[] mDescriptors = ParcelFileDescriptor.createReliablePipe();
        protected final Identifier mIdentifier;
        protected final MtpManager mManager;

        Task(MtpManager mtpManager, Identifier identifier) throws IOException {
            this.mManager = mtpManager;
            this.mIdentifier = identifier;
        }

        ParcelFileDescriptor getReadingFileDescriptor() {
            return this.mDescriptors[0];
        }
    }

    private static class ImportFileTask extends Task {
        ImportFileTask(MtpManager mtpManager, Identifier identifier) throws IOException {
            super(mtpManager, identifier);
        }

        @Override
        public void run() {
            try {
                this.mManager.importFile(this.mIdentifier.mDeviceId, this.mIdentifier.mObjectHandle, this.mDescriptors[1]);
                this.mDescriptors[1].close();
            } catch (IOException e) {
                try {
                    this.mDescriptors[1].closeWithError("Failed to stream a file.");
                } catch (IOException e2) {
                    Log.w("MtpDocumentsProvider", e2.getMessage());
                }
            }
        }
    }

    private static class GetThumbnailTask extends Task {
        GetThumbnailTask(MtpManager mtpManager, Identifier identifier) throws IOException {
            super(mtpManager, identifier);
        }

        @Override
        public void run() {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(this.mDescriptors[1]);
                Throwable th = null;
                try {
                    try {
                        autoCloseOutputStream.write(this.mManager.getThumbnail(this.mIdentifier.mDeviceId, this.mIdentifier.mObjectHandle));
                    } catch (IOException e) {
                        this.mDescriptors[1].closeWithError("Failed to stream a thumbnail.");
                    }
                    autoCloseOutputStream.close();
                } finally {
                }
            } catch (IOException e2) {
                Log.w("MtpDocumentsProvider", e2.getMessage());
            }
        }
    }

    boolean close() throws InterruptedException {
        this.mExecutor.shutdownNow();
        return this.mExecutor.awaitTermination(2000L, TimeUnit.MILLISECONDS);
    }
}
