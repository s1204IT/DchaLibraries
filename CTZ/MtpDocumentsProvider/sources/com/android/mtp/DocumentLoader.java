package com.android.mtp;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.mtp.MtpObjectInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

class DocumentLoader implements AutoCloseable {
    static final boolean $assertionsDisabled = false;
    private Thread mBackgroundThread;
    private final MtpDatabase mDatabase;
    private final MtpDeviceRecord mDevice;
    private final MtpManager mMtpManager;
    private final ContentResolver mResolver;
    private final TaskList mTaskList = new TaskList();

    DocumentLoader(MtpDeviceRecord mtpDeviceRecord, MtpManager mtpManager, ContentResolver contentResolver, MtpDatabase mtpDatabase) {
        this.mDevice = mtpDeviceRecord;
        this.mMtpManager = mtpManager;
        this.mResolver = contentResolver;
        this.mDatabase = mtpDatabase;
    }

    synchronized Cursor queryChildDocuments(String[] strArr, Identifier identifier) throws IOException {
        LoaderTask loaderTaskFindTask;
        loaderTaskFindTask = this.mTaskList.findTask(identifier);
        if (loaderTaskFindTask == null) {
            if (identifier.mDocumentId == null) {
                throw new FileNotFoundException("Parent not found.");
            }
            loaderTaskFindTask = new LoaderTask(this.mMtpManager, this.mDatabase, this.mDevice.operationsSupported, identifier);
            loaderTaskFindTask.loadObjectHandles();
            loaderTaskFindTask.loadObjectInfoList(10);
        } else {
            this.mTaskList.remove(loaderTaskFindTask);
        }
        this.mTaskList.addFirst(loaderTaskFindTask);
        if (loaderTaskFindTask.getState() == 1) {
            resume();
        }
        return loaderTaskFindTask.createCursor(this.mResolver, strArr);
    }

    synchronized void resume() {
        if (this.mBackgroundThread == null) {
            this.mBackgroundThread = new BackgroundLoaderThread();
            this.mBackgroundThread.start();
        }
    }

    synchronized LoaderTask getNextTaskOrReleaseBackgroundThread() {
        Preconditions.checkState(this.mBackgroundThread != null);
        for (LoaderTask loaderTask : this.mTaskList) {
            if (loaderTask.getState() == 1) {
                return loaderTask;
            }
        }
        Identifier unmappedDocumentsParent = this.mDatabase.getUnmappedDocumentsParent(this.mDevice.deviceId);
        if (unmappedDocumentsParent != null) {
            LoaderTask loaderTaskFindTask = this.mTaskList.findTask(unmappedDocumentsParent);
            if (loaderTaskFindTask != null) {
                Preconditions.checkState(loaderTaskFindTask.getState() != 1);
                this.mTaskList.remove(loaderTaskFindTask);
            }
            LoaderTask loaderTask2 = new LoaderTask(this.mMtpManager, this.mDatabase, this.mDevice.operationsSupported, unmappedDocumentsParent);
            loaderTask2.loadObjectHandles();
            this.mTaskList.addFirst(loaderTask2);
            return loaderTask2;
        }
        this.mBackgroundThread = null;
        return null;
    }

    @Override
    public void close() throws InterruptedException {
        Thread thread;
        synchronized (this) {
            this.mTaskList.clear();
            thread = this.mBackgroundThread;
        }
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
    }

    synchronized void clearCompletedTasks() {
        this.mTaskList.clearCompletedTasks();
    }

    void cancelTask(Identifier identifier) {
        LoaderTask loaderTaskFindTask;
        synchronized (this) {
            loaderTaskFindTask = this.mTaskList.findTask(identifier);
        }
        if (loaderTaskFindTask != null) {
            loaderTaskFindTask.cancel();
            this.mTaskList.remove(loaderTaskFindTask);
        }
    }

    private class BackgroundLoaderThread extends Thread {
        private BackgroundLoaderThread() {
        }

        @Override
        public void run() {
            LoaderTask nextTaskOrReleaseBackgroundThread;
            Process.setThreadPriority(10);
            while (!Thread.interrupted() && (nextTaskOrReleaseBackgroundThread = DocumentLoader.this.getNextTaskOrReleaseBackgroundThread()) != null) {
                nextTaskOrReleaseBackgroundThread.loadObjectInfoList(20);
                boolean z = true;
                if (nextTaskOrReleaseBackgroundThread.getState() == 4 || (nextTaskOrReleaseBackgroundThread.mLastNotified.getTime() >= new Date().getTime() - 500 && nextTaskOrReleaseBackgroundThread.getState() == 1)) {
                    z = false;
                }
                if (z) {
                    nextTaskOrReleaseBackgroundThread.notify(DocumentLoader.this.mResolver);
                }
            }
        }
    }

    private static class TaskList extends LinkedList<LoaderTask> {
        private TaskList() {
        }

        LoaderTask findTask(Identifier identifier) {
            for (int i = 0; i < size(); i++) {
                if (get(i).mIdentifier.equals(identifier)) {
                    return get(i);
                }
            }
            return null;
        }

        void clearCompletedTasks() {
            int i = 0;
            while (i < size()) {
                if (get(i).getState() == 2) {
                    remove(i);
                } else {
                    i++;
                }
            }
        }
    }

    private static class LoaderTask {
        static final boolean $assertionsDisabled = false;
        final MtpDatabase mDatabase;
        IOException mError;
        final Identifier mIdentifier;
        final MtpManager mManager;
        final int[] mOperationsSupported;
        int[] mObjectHandles = null;
        int mState = 0;
        int mPosition = 0;
        Date mLastNotified = new Date();

        LoaderTask(MtpManager mtpManager, MtpDatabase mtpDatabase, int[] iArr, Identifier identifier) {
            this.mManager = mtpManager;
            this.mDatabase = mtpDatabase;
            this.mOperationsSupported = iArr;
            this.mIdentifier = identifier;
        }

        synchronized void loadObjectHandles() {
            this.mPosition = 0;
            int i = this.mIdentifier.mObjectHandle;
            if (this.mIdentifier.mDocumentType == 1) {
                i = -1;
            }
            try {
                this.mObjectHandles = this.mManager.getObjectHandles(this.mIdentifier.mDeviceId, this.mIdentifier.mStorageId, i);
                this.mState = 1;
            } catch (IOException e) {
                this.mError = e;
                this.mState = 3;
            }
        }

        synchronized Cursor createCursor(ContentResolver contentResolver, String[] strArr) throws IOException {
            Cursor cursorQueryChildDocuments;
            Bundle bundle = new Bundle();
            int state = getState();
            if (state == 1) {
                bundle.putBoolean("loading", true);
            } else if (state == 3) {
                throw this.mError;
            }
            cursorQueryChildDocuments = this.mDatabase.queryChildDocuments(strArr, this.mIdentifier.mDocumentId);
            cursorQueryChildDocuments.setExtras(bundle);
            cursorQueryChildDocuments.setNotificationUri(contentResolver, createUri());
            return cursorQueryChildDocuments;
        }

        void loadObjectInfoList(int i) {
            synchronized (this) {
                if (this.mState != 1) {
                    return;
                }
                if (this.mPosition == 0) {
                    try {
                        this.mDatabase.getMapper().startAddingDocuments(this.mIdentifier.mDocumentId);
                    } catch (FileNotFoundException e) {
                        this.mError = e;
                        this.mState = 3;
                        return;
                    }
                }
                ArrayList arrayList = new ArrayList();
                int i2 = this.mPosition + i;
                while (this.mPosition < this.mObjectHandles.length && this.mPosition < i2) {
                    try {
                        arrayList.add(this.mManager.getObjectInfo(this.mIdentifier.mDeviceId, this.mObjectHandles[this.mPosition]));
                    } catch (IOException e2) {
                        Log.e("MtpDocumentsProvider", "Failed to load object info", e2);
                    }
                    this.mPosition++;
                }
                long[] jArr = new long[arrayList.size()];
                for (int i3 = 0; i3 < arrayList.size(); i3++) {
                    MtpObjectInfo mtpObjectInfo = (MtpObjectInfo) arrayList.get(i3);
                    if (mtpObjectInfo.getCompressedSizeLong() != 4294967295L) {
                        jArr[i3] = mtpObjectInfo.getCompressedSizeLong();
                    } else if (!MtpDeviceRecord.isSupported(this.mOperationsSupported, 38914) || !MtpDeviceRecord.isSupported(this.mOperationsSupported, 38915)) {
                        jArr[i3] = -1;
                    } else {
                        try {
                            jArr[i3] = this.mManager.getObjectSizeLong(this.mIdentifier.mDeviceId, mtpObjectInfo.getObjectHandle(), mtpObjectInfo.getFormat());
                        } catch (IOException e3) {
                            Log.e("MtpDocumentsProvider", "Failed to get object size property.", e3);
                            jArr[i3] = -1;
                        }
                    }
                }
                synchronized (this) {
                    if (this.mState != 1) {
                        return;
                    }
                    try {
                        this.mDatabase.getMapper().putChildDocuments(this.mIdentifier.mDeviceId, this.mIdentifier.mDocumentId, this.mOperationsSupported, (MtpObjectInfo[]) arrayList.toArray(new MtpObjectInfo[arrayList.size()]), jArr);
                        if (this.mPosition >= this.mObjectHandles.length) {
                            try {
                                this.mDatabase.getMapper().stopAddingDocuments(this.mIdentifier.mDocumentId);
                                this.mState = 2;
                            } catch (FileNotFoundException e4) {
                                this.mError = e4;
                                this.mState = 3;
                            }
                        }
                    } catch (FileNotFoundException e5) {
                        this.mError = e5;
                        this.mState = 3;
                    }
                }
            }
        }

        synchronized void cancel() {
            this.mDatabase.getMapper().cancelAddingDocuments(this.mIdentifier.mDocumentId);
            this.mState = 4;
        }

        int getState() {
            return this.mState;
        }

        void notify(ContentResolver contentResolver) {
            contentResolver.notifyChange(createUri(), (ContentObserver) null, false);
            this.mLastNotified = new Date();
        }

        private Uri createUri() {
            return DocumentsContract.buildChildDocumentsUri("com.android.mtp.documents", this.mIdentifier.mDocumentId);
        }
    }
}
