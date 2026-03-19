package com.android.printspooler.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.print.ILayoutResultCallback;
import android.print.IPrintDocumentAdapter;
import android.print.IPrintDocumentAdapterObserver;
import android.print.IWriteResultCallback;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.util.Log;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.printspooler.R;
import com.android.printspooler.model.RemotePrintDocument;
import com.android.printspooler.util.PageRangeUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.function.Consumer;
import libcore.io.IoUtils;

public final class RemotePrintDocument {
    private final RemoteAdapterDeathObserver mAdapterDeathObserver;
    private final Context mContext;
    private AsyncCommand mCurrentCommand;
    private final Looper mLooper;
    private AsyncCommand mNextCommand;
    private final IPrintDocumentAdapter mPrintDocumentAdapter;
    private final UpdateResultCallbacks mUpdateCallbacks;
    private final UpdateSpec mUpdateSpec = new UpdateSpec();
    private final CommandDoneCallback mCommandResultCallback = new CommandDoneCallback() {
        @Override
        public void onDone() {
            if (RemotePrintDocument.this.mCurrentCommand.isCompleted()) {
                if (RemotePrintDocument.this.mCurrentCommand instanceof LayoutCommand) {
                    if (RemotePrintDocument.this.mNextCommand == null) {
                        if (RemotePrintDocument.this.mUpdateSpec.pages == null || (!RemotePrintDocument.this.mDocumentInfo.changed && RemotePrintDocument.this.mDocumentInfo.pagesWrittenToFile != null && (RemotePrintDocument.this.mDocumentInfo.info.getPageCount() == -1 || PageRangeUtils.contains(RemotePrintDocument.this.mDocumentInfo.pagesWrittenToFile, RemotePrintDocument.this.mUpdateSpec.pages, RemotePrintDocument.this.mDocumentInfo.info.getPageCount())))) {
                            if (RemotePrintDocument.this.mUpdateSpec.pages != null) {
                                RemotePrintDocument.this.mDocumentInfo.pagesInFileToPrint = PageRangeUtils.computeWhichPagesInFileToPrint(RemotePrintDocument.this.mUpdateSpec.pages, RemotePrintDocument.this.mDocumentInfo.pagesWrittenToFile, RemotePrintDocument.this.mDocumentInfo.info.getPageCount());
                            }
                            RemotePrintDocument.this.mState = 3;
                            RemotePrintDocument.this.mDocumentInfo.updated = true;
                            RemotePrintDocument.this.notifyUpdateCompleted();
                        } else {
                            RemotePrintDocument.this.mNextCommand = new WriteCommand(RemotePrintDocument.this.mContext, RemotePrintDocument.this.mLooper, RemotePrintDocument.this.mPrintDocumentAdapter, RemotePrintDocument.this.mDocumentInfo, RemotePrintDocument.this.mDocumentInfo.info.getPageCount(), RemotePrintDocument.this.mUpdateSpec.pages, RemotePrintDocument.this.mDocumentInfo.fileProvider, RemotePrintDocument.this.mCommandResultCallback);
                        }
                    }
                } else {
                    RemotePrintDocument.this.mState = 3;
                    RemotePrintDocument.this.mDocumentInfo.updated = true;
                    RemotePrintDocument.this.notifyUpdateCompleted();
                }
                RemotePrintDocument.this.runPendingCommand();
                return;
            }
            if (RemotePrintDocument.this.mCurrentCommand.isFailed()) {
                RemotePrintDocument.this.mState = 4;
                CharSequence error = RemotePrintDocument.this.mCurrentCommand.getError();
                RemotePrintDocument.this.mCurrentCommand = null;
                RemotePrintDocument.this.mNextCommand = null;
                RemotePrintDocument.this.mUpdateSpec.reset();
                RemotePrintDocument.this.notifyUpdateFailed(error);
                return;
            }
            if (RemotePrintDocument.this.mCurrentCommand.isCanceled()) {
                if (RemotePrintDocument.this.mState == 6) {
                    RemotePrintDocument.this.mState = 7;
                    RemotePrintDocument.this.notifyUpdateCanceled();
                }
                if (RemotePrintDocument.this.mNextCommand != null) {
                    RemotePrintDocument.this.runPendingCommand();
                } else {
                    RemotePrintDocument.this.mUpdateSpec.reset();
                }
            }
        }
    };
    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            RemotePrintDocument.this.onPrintingAppDied();
        }
    };
    private int mState = 0;
    private final RemotePrintDocumentInfo mDocumentInfo = new RemotePrintDocumentInfo();

    private interface CommandDoneCallback {
        void onDone();
    }

    public interface RemoteAdapterDeathObserver {
        void onDied();
    }

    public static final class RemotePrintDocumentInfo {
        public PrintAttributes attributes;
        public boolean changed;
        public MutexFileProvider fileProvider;
        public PrintDocumentInfo info;
        public boolean laidout;
        public Bundle metadata;
        public PageRange[] pagesInFileToPrint;
        public PageRange[] pagesWrittenToFile;
        public boolean updated;
    }

    public interface UpdateResultCallbacks {
        void onUpdateCanceled();

        void onUpdateCompleted(RemotePrintDocumentInfo remotePrintDocumentInfo);

        void onUpdateFailed(CharSequence charSequence);
    }

    public RemotePrintDocument(Context context, IPrintDocumentAdapter iPrintDocumentAdapter, MutexFileProvider mutexFileProvider, RemoteAdapterDeathObserver remoteAdapterDeathObserver, UpdateResultCallbacks updateResultCallbacks) {
        this.mPrintDocumentAdapter = iPrintDocumentAdapter;
        this.mLooper = context.getMainLooper();
        this.mContext = context;
        this.mAdapterDeathObserver = remoteAdapterDeathObserver;
        this.mDocumentInfo.fileProvider = mutexFileProvider;
        this.mUpdateCallbacks = updateResultCallbacks;
        connectToRemoteDocument();
    }

    public void start() {
        if (this.mState == 4) {
            Log.w("RemotePrintDocument", "Failed before start.");
            return;
        }
        if (this.mState == 8) {
            Log.w("RemotePrintDocument", "Destroyed before start.");
            return;
        }
        if (this.mState != 0) {
            throw new IllegalStateException("Cannot start in state:" + stateToString(this.mState));
        }
        try {
            this.mPrintDocumentAdapter.start();
            this.mState = 1;
        } catch (RemoteException e) {
            Log.e("RemotePrintDocument", "Error calling start()", e);
            this.mState = 4;
        }
    }

    public boolean update(PrintAttributes printAttributes, PageRange[] pageRangeArr, boolean z) {
        boolean z2;
        PageRange[] pageRangeArr2;
        if (hasUpdateError()) {
            throw new IllegalStateException("Cannot update without a clearing the failure");
        }
        if (this.mState != 0 && this.mState != 5 && this.mState != 8) {
            boolean z3 = true;
            if (!this.mUpdateSpec.hasSameConstraints(printAttributes, z)) {
                if (this.mCurrentCommand != null && (this.mCurrentCommand.isRunning() || this.mCurrentCommand.isPending())) {
                    this.mCurrentCommand.cancel(false);
                }
                scheduleCommand(new LayoutCommand(this.mLooper, this.mPrintDocumentAdapter, this.mDocumentInfo, this.mDocumentInfo.attributes != null ? this.mDocumentInfo.attributes : new PrintAttributes.Builder().build(), printAttributes, z, this.mCommandResultCallback));
                this.mDocumentInfo.updated = false;
                this.mState = 2;
                z2 = z;
                pageRangeArr2 = pageRangeArr;
            } else if (((this.mCurrentCommand instanceof LayoutCommand) && (this.mCurrentCommand.isPending() || this.mCurrentCommand.isRunning())) || pageRangeArr == null || PageRangeUtils.contains(this.mUpdateSpec.pages, pageRangeArr, this.mDocumentInfo.info.getPageCount())) {
                z2 = z;
                pageRangeArr2 = pageRangeArr;
                z3 = false;
            } else {
                if ((this.mCurrentCommand instanceof WriteCommand) && (this.mCurrentCommand.isPending() || this.mCurrentCommand.isRunning())) {
                    this.mCurrentCommand.cancel(false);
                }
                z2 = z;
                pageRangeArr2 = pageRangeArr;
                scheduleCommand(new WriteCommand(this.mContext, this.mLooper, this.mPrintDocumentAdapter, this.mDocumentInfo, this.mDocumentInfo.info.getPageCount(), pageRangeArr2, this.mDocumentInfo.fileProvider, this.mCommandResultCallback));
                this.mDocumentInfo.updated = false;
                this.mState = 2;
            }
            this.mUpdateSpec.update(printAttributes, z2, pageRangeArr2);
            runPendingCommand();
            return z3;
        }
        throw new IllegalStateException("Cannot update in state:" + stateToString(this.mState));
    }

    public void finish() {
        if (this.mState != 1 && this.mState != 3 && this.mState != 4 && this.mState != 6 && this.mState != 7 && this.mState != 8) {
            throw new IllegalStateException("Cannot finish in state:" + stateToString(this.mState));
        }
        try {
            this.mPrintDocumentAdapter.finish();
            this.mState = 5;
        } catch (RemoteException e) {
            Log.e("RemotePrintDocument", "Error calling finish()");
            this.mState = 4;
        }
    }

    public void cancel(boolean z) {
        this.mNextCommand = null;
        if (this.mState != 2) {
            return;
        }
        this.mState = 6;
        this.mCurrentCommand.cancel(z);
    }

    public void destroy() {
        if (this.mState == 8) {
            throw new IllegalStateException("Cannot destroy in state:" + stateToString(this.mState));
        }
        this.mState = 8;
        disconnectFromRemoteDocument();
    }

    public void kill(String str) {
        try {
            this.mPrintDocumentAdapter.kill(str);
        } catch (RemoteException e) {
            Log.e("RemotePrintDocument", "Error calling kill()", e);
        }
    }

    public boolean isUpdating() {
        return this.mState == 2 || this.mState == 6;
    }

    public boolean isDestroyed() {
        return this.mState == 8;
    }

    public boolean hasUpdateError() {
        return this.mState == 4;
    }

    public boolean hasLaidOutPages() {
        return this.mDocumentInfo.info != null && this.mDocumentInfo.info.getPageCount() > 0;
    }

    public void clearUpdateError() {
        if (!hasUpdateError()) {
            throw new IllegalStateException("No update error to clear");
        }
        this.mState = 1;
    }

    public RemotePrintDocumentInfo getDocumentInfo() {
        return this.mDocumentInfo;
    }

    public void writeContent(ContentResolver contentResolver, Uri uri) throws Throwable {
        OutputStream outputStreamOpenOutputStream;
        File fileAcquireFile;
        FileInputStream fileInputStream;
        FileInputStream fileInputStream2 = null;
        try {
            fileAcquireFile = this.mDocumentInfo.fileProvider.acquireFile(null);
            try {
                fileInputStream = new FileInputStream(fileAcquireFile);
                try {
                    outputStreamOpenOutputStream = contentResolver.openOutputStream(uri);
                } catch (IOException e) {
                    e = e;
                    outputStreamOpenOutputStream = null;
                } catch (Throwable th) {
                    th = th;
                    outputStreamOpenOutputStream = null;
                }
            } catch (IOException e2) {
                e = e2;
                outputStreamOpenOutputStream = null;
            } catch (Throwable th2) {
                th = th2;
                outputStreamOpenOutputStream = null;
            }
        } catch (IOException e3) {
            e = e3;
            outputStreamOpenOutputStream = null;
            fileAcquireFile = null;
        } catch (Throwable th3) {
            th = th3;
            outputStreamOpenOutputStream = null;
            fileAcquireFile = null;
        }
        try {
            byte[] bArr = new byte[8192];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i < 0) {
                    break;
                } else {
                    outputStreamOpenOutputStream.write(bArr, 0, i);
                }
            }
            IoUtils.closeQuietly(fileInputStream);
            IoUtils.closeQuietly(outputStreamOpenOutputStream);
            if (fileAcquireFile == null) {
                return;
            }
        } catch (IOException e4) {
            e = e4;
            fileInputStream2 = fileInputStream;
            try {
                Log.e("RemotePrintDocument", "Error writing document content.", e);
                IoUtils.closeQuietly(fileInputStream2);
                IoUtils.closeQuietly(outputStreamOpenOutputStream);
                if (fileAcquireFile == null) {
                    return;
                }
            } catch (Throwable th4) {
                th = th4;
                IoUtils.closeQuietly(fileInputStream2);
                IoUtils.closeQuietly(outputStreamOpenOutputStream);
                if (fileAcquireFile != null) {
                    this.mDocumentInfo.fileProvider.releaseFile();
                }
                throw th;
            }
        } catch (Throwable th5) {
            th = th5;
            fileInputStream2 = fileInputStream;
            IoUtils.closeQuietly(fileInputStream2);
            IoUtils.closeQuietly(outputStreamOpenOutputStream);
            if (fileAcquireFile != null) {
            }
            throw th;
        }
        this.mDocumentInfo.fileProvider.releaseFile();
    }

    private void notifyUpdateCanceled() {
        this.mUpdateCallbacks.onUpdateCanceled();
    }

    private void notifyUpdateCompleted() {
        this.mUpdateCallbacks.onUpdateCompleted(this.mDocumentInfo);
    }

    private void notifyUpdateFailed(CharSequence charSequence) {
        this.mUpdateCallbacks.onUpdateFailed(charSequence);
    }

    private void connectToRemoteDocument() {
        try {
            this.mPrintDocumentAdapter.asBinder().linkToDeath(this.mDeathRecipient, 0);
            try {
                this.mPrintDocumentAdapter.setObserver(new PrintDocumentAdapterObserver(this));
            } catch (RemoteException e) {
                Log.w("RemotePrintDocument", "Error setting observer to the print adapter.");
                destroy();
            }
        } catch (RemoteException e2) {
            Log.w("RemotePrintDocument", "The printing process is dead.");
            destroy();
        }
    }

    private void disconnectFromRemoteDocument() {
        try {
            this.mPrintDocumentAdapter.setObserver((IPrintDocumentAdapterObserver) null);
        } catch (RemoteException e) {
            Log.w("RemotePrintDocument", "Error setting observer to the print adapter.");
        }
        this.mPrintDocumentAdapter.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
    }

    private void scheduleCommand(AsyncCommand asyncCommand) {
        if (this.mCurrentCommand == null) {
            this.mCurrentCommand = asyncCommand;
        } else {
            this.mNextCommand = asyncCommand;
        }
    }

    private void runPendingCommand() {
        if (this.mCurrentCommand != null && (this.mCurrentCommand.isCompleted() || this.mCurrentCommand.isCanceled())) {
            this.mCurrentCommand = this.mNextCommand;
            this.mNextCommand = null;
        }
        if (this.mCurrentCommand != null) {
            if (this.mCurrentCommand.isPending()) {
                this.mCurrentCommand.run();
                this.mState = 2;
                return;
            }
            return;
        }
        this.mState = 3;
    }

    private static String stateToString(int i) {
        switch (i) {
            case 1:
                return "STATE_STARTED";
            case 2:
                return "STATE_UPDATING";
            case 3:
                return "STATE_UPDATED";
            case 4:
                return "STATE_FAILED";
            case 5:
                return "STATE_FINISHED";
            case 6:
                return "STATE_CANCELING";
            case 7:
                return "STATE_CANCELED";
            case 8:
                return "STATE_DESTROYED";
            default:
                return "STATE_UNKNOWN";
        }
    }

    static final class UpdateSpec {
        final PrintAttributes attributes = new PrintAttributes.Builder().build();
        PageRange[] pages;
        boolean preview;

        UpdateSpec() {
        }

        public void update(PrintAttributes printAttributes, boolean z, PageRange[] pageRangeArr) {
            this.attributes.copyFrom(printAttributes);
            this.preview = z;
            this.pages = pageRangeArr != null ? (PageRange[]) Arrays.copyOf(pageRangeArr, pageRangeArr.length) : null;
        }

        public void reset() {
            this.attributes.clear();
            this.preview = false;
            this.pages = null;
        }

        public boolean hasSameConstraints(PrintAttributes printAttributes, boolean z) {
            return this.attributes.equals(printAttributes) && this.preview == z;
        }
    }

    private static abstract class AsyncCommand implements Runnable {
        private static int sSequenceCounter;
        protected final IPrintDocumentAdapter mAdapter;
        protected ICancellationSignal mCancellation;
        protected final RemotePrintDocumentInfo mDocument;
        protected final CommandDoneCallback mDoneCallback;
        private CharSequence mError;
        private final Handler mHandler;
        protected final int mSequence;
        private int mState;

        public AsyncCommand(Looper looper, IPrintDocumentAdapter iPrintDocumentAdapter, RemotePrintDocumentInfo remotePrintDocumentInfo, CommandDoneCallback commandDoneCallback) {
            int i = sSequenceCounter;
            sSequenceCounter = i + 1;
            this.mSequence = i;
            this.mState = 0;
            this.mHandler = new Handler(looper);
            this.mAdapter = iPrintDocumentAdapter;
            this.mDocument = remotePrintDocumentInfo;
            this.mDoneCallback = commandDoneCallback;
        }

        protected final boolean isCanceling() {
            return this.mState == 4;
        }

        public final boolean isCanceled() {
            return this.mState == 3;
        }

        protected void removeForceCancel() {
            this.mHandler.removeMessages(0);
        }

        public final void cancel(boolean z) {
            if (isRunning()) {
                canceling();
                if (this.mCancellation != null) {
                    try {
                        this.mCancellation.cancel();
                    } catch (RemoteException e) {
                        Log.w("RemotePrintDocument", "Error while canceling", e);
                    }
                }
            }
            if (isCanceling()) {
                if (z) {
                    this.mHandler.sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            ((RemotePrintDocument.AsyncCommand) obj).forceCancel();
                        }
                    }, this).setWhat(0), 1000L);
                }
            } else {
                canceled();
                this.mDoneCallback.onDone();
            }
        }

        protected final void canceling() {
            if (this.mState != 0 && this.mState != 1) {
                throw new IllegalStateException("Command not pending or running.");
            }
            this.mState = 4;
        }

        protected final void canceled() {
            if (this.mState != 4) {
                throw new IllegalStateException("Not canceling.");
            }
            this.mState = 3;
        }

        public final boolean isPending() {
            return this.mState == 0;
        }

        protected final void running() {
            if (this.mState != 0) {
                throw new IllegalStateException("Not pending.");
            }
            this.mState = 1;
        }

        public final boolean isRunning() {
            return this.mState == 1;
        }

        protected final void completed() {
            if (this.mState != 1 && this.mState != 4) {
                throw new IllegalStateException("Not running.");
            }
            this.mState = 2;
        }

        public final boolean isCompleted() {
            return this.mState == 2;
        }

        protected final void failed(CharSequence charSequence) {
            if (this.mState != 1 && this.mState != 4) {
                throw new IllegalStateException("Not running.");
            }
            this.mState = 5;
            this.mError = charSequence;
        }

        public final boolean isFailed() {
            return this.mState == 5;
        }

        public CharSequence getError() {
            return this.mError;
        }

        private void forceCancel() {
            if (isCanceling()) {
                failed("Command did not respond to cancellation in 1000 ms");
                this.mDoneCallback.onDone();
            }
        }
    }

    private static final class LayoutCommand extends AsyncCommand {
        private final Handler mHandler;
        private final Bundle mMetadata;
        private final PrintAttributes mNewAttributes;
        private final PrintAttributes mOldAttributes;
        private final ILayoutResultCallback mRemoteResultCallback;

        public LayoutCommand(Looper looper, IPrintDocumentAdapter iPrintDocumentAdapter, RemotePrintDocumentInfo remotePrintDocumentInfo, PrintAttributes printAttributes, PrintAttributes printAttributes2, boolean z, CommandDoneCallback commandDoneCallback) {
            super(looper, iPrintDocumentAdapter, remotePrintDocumentInfo, commandDoneCallback);
            this.mOldAttributes = new PrintAttributes.Builder().build();
            this.mNewAttributes = new PrintAttributes.Builder().build();
            this.mMetadata = new Bundle();
            this.mHandler = new LayoutHandler(looper);
            this.mRemoteResultCallback = new LayoutResultCallback(this.mHandler);
            this.mOldAttributes.copyFrom(printAttributes);
            this.mNewAttributes.copyFrom(printAttributes2);
            this.mMetadata.putBoolean("EXTRA_PRINT_PREVIEW", z);
        }

        @Override
        public void run() {
            running();
            try {
                this.mDocument.changed = false;
                this.mAdapter.layout(this.mOldAttributes, this.mNewAttributes, this.mRemoteResultCallback, this.mMetadata, this.mSequence);
            } catch (RemoteException e) {
                Log.e("RemotePrintDocument", "Error calling layout", e);
                handleOnLayoutFailed(null, this.mSequence);
            }
        }

        private void handleOnLayoutStarted(ICancellationSignal iCancellationSignal, int i) {
            if (i != this.mSequence) {
                return;
            }
            if (isCanceling()) {
                try {
                    iCancellationSignal.cancel();
                    return;
                } catch (RemoteException e) {
                    Log.e("RemotePrintDocument", "Error cancelling", e);
                    handleOnLayoutFailed(null, this.mSequence);
                    return;
                }
            }
            this.mCancellation = iCancellationSignal;
        }

        private void handleOnLayoutFinished(PrintDocumentInfo printDocumentInfo, boolean z, int i) {
            if (i != this.mSequence) {
                return;
            }
            completed();
            if (z || !equalsIgnoreSize(this.mDocument.info, printDocumentInfo)) {
                this.mDocument.pagesWrittenToFile = null;
                this.mDocument.pagesInFileToPrint = null;
                this.mDocument.changed = true;
            }
            this.mDocument.attributes = this.mNewAttributes;
            this.mDocument.metadata = this.mMetadata;
            this.mDocument.laidout = true;
            this.mDocument.info = printDocumentInfo;
            this.mCancellation = null;
            this.mDoneCallback.onDone();
        }

        private void handleOnLayoutFailed(CharSequence charSequence, int i) {
            if (i != this.mSequence) {
                return;
            }
            this.mDocument.laidout = false;
            failed(charSequence);
            this.mCancellation = null;
            this.mDoneCallback.onDone();
        }

        private void handleOnLayoutCanceled(int i) {
            if (i != this.mSequence) {
                return;
            }
            canceled();
            this.mCancellation = null;
            this.mDoneCallback.onDone();
        }

        private boolean equalsIgnoreSize(PrintDocumentInfo printDocumentInfo, PrintDocumentInfo printDocumentInfo2) {
            if (printDocumentInfo == printDocumentInfo2) {
                return true;
            }
            if (printDocumentInfo != null && printDocumentInfo2 != null && printDocumentInfo.getContentType() == printDocumentInfo2.getContentType() && printDocumentInfo.getPageCount() == printDocumentInfo2.getPageCount()) {
                return true;
            }
            return false;
        }

        private final class LayoutHandler extends Handler {
            public LayoutHandler(Looper looper) {
                super(looper, null, false);
            }

            @Override
            public void handleMessage(Message message) {
                int i;
                if (LayoutCommand.this.isFailed()) {
                }
                int i2 = message.what;
                CharSequence charSequence = null;
                switch (i2) {
                    case 1:
                        i = message.arg1;
                        break;
                    case 2:
                        LayoutCommand.this.removeForceCancel();
                        i = message.arg2;
                        break;
                    case 3:
                        charSequence = (CharSequence) message.obj;
                        LayoutCommand.this.removeForceCancel();
                        i = message.arg1;
                        break;
                    case 4:
                        if (!LayoutCommand.this.isCanceling()) {
                            Log.w("RemotePrintDocument", "Unexpected cancel");
                            i2 = 3;
                        }
                        LayoutCommand.this.removeForceCancel();
                        i = message.arg1;
                        break;
                    default:
                        i = -1;
                        break;
                }
                if (LayoutCommand.this.isCanceling() && i2 != 1) {
                    i2 = 4;
                }
                switch (i2) {
                    case 1:
                        LayoutCommand.this.handleOnLayoutStarted((ICancellationSignal) message.obj, i);
                        break;
                    case 2:
                        LayoutCommand.this.handleOnLayoutFinished((PrintDocumentInfo) message.obj, message.arg1 == 1, i);
                        break;
                    case 3:
                        LayoutCommand.this.handleOnLayoutFailed(charSequence, i);
                        break;
                    case 4:
                        LayoutCommand.this.handleOnLayoutCanceled(i);
                        break;
                }
            }
        }

        private static final class LayoutResultCallback extends ILayoutResultCallback.Stub {
            private final WeakReference<Handler> mWeakHandler;

            public LayoutResultCallback(Handler handler) {
                this.mWeakHandler = new WeakReference<>(handler);
            }

            public void onLayoutStarted(ICancellationSignal iCancellationSignal, int i) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(1, i, 0, iCancellationSignal).sendToTarget();
                }
            }

            public void onLayoutFinished(PrintDocumentInfo printDocumentInfo, boolean z, int i) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(2, z ? 1 : 0, i, printDocumentInfo).sendToTarget();
                }
            }

            public void onLayoutFailed(CharSequence charSequence, int i) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(3, i, 0, charSequence).sendToTarget();
                }
            }

            public void onLayoutCanceled(int i) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(4, i, 0).sendToTarget();
                }
            }
        }
    }

    private static final class WriteCommand extends AsyncCommand {
        private final Context mContext;
        private final MutexFileProvider mFileProvider;
        private final Handler mHandler;
        private final int mPageCount;
        private final PageRange[] mPages;
        private final IWriteResultCallback mRemoteResultCallback;
        private final CommandDoneCallback mWriteDoneCallback;

        public WriteCommand(Context context, Looper looper, IPrintDocumentAdapter iPrintDocumentAdapter, RemotePrintDocumentInfo remotePrintDocumentInfo, int i, PageRange[] pageRangeArr, MutexFileProvider mutexFileProvider, CommandDoneCallback commandDoneCallback) {
            super(looper, iPrintDocumentAdapter, remotePrintDocumentInfo, commandDoneCallback);
            this.mContext = context;
            this.mHandler = new WriteHandler(looper);
            this.mRemoteResultCallback = new WriteResultCallback(this.mHandler);
            this.mPageCount = i;
            this.mPages = (PageRange[]) Arrays.copyOf(pageRangeArr, pageRangeArr.length);
            this.mFileProvider = mutexFileProvider;
            this.mWriteDoneCallback = commandDoneCallback;
        }

        @Override
        public void run() {
            running();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voidArr) throws Throwable {
                    File fileAcquireFile;
                    FileInputStream fileInputStream;
                    FileInputStream fileInputStream2;
                    FileInputStream fileInputStream3;
                    ?? r3;
                    ?? fileOutputStream;
                    Throwable th;
                    ?? r1;
                    Throwable e;
                    try {
                        fileAcquireFile = WriteCommand.this.mFileProvider.acquireFile(null);
                        try {
                            ?? CreatePipe = ParcelFileDescriptor.createPipe();
                            r3 = CreatePipe[0];
                            try {
                                r1 = CreatePipe[1];
                                try {
                                    fileInputStream3 = new FileInputStream(r3.getFileDescriptor());
                                    try {
                                        fileOutputStream = new FileOutputStream(fileAcquireFile);
                                        try {
                                            try {
                                                WriteCommand.this.mAdapter.write(WriteCommand.this.mPages, (ParcelFileDescriptor) r1, WriteCommand.this.mRemoteResultCallback, WriteCommand.this.mSequence);
                                                r1.close();
                                                try {
                                                    try {
                                                        byte[] bArr = new byte[8192];
                                                        while (true) {
                                                            int i = fileInputStream3.read(bArr);
                                                            if (i < 0) {
                                                                break;
                                                            }
                                                            fileOutputStream.write(bArr, 0, i);
                                                        }
                                                        IoUtils.closeQuietly(fileInputStream3);
                                                        IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                                        IoUtils.closeQuietly((AutoCloseable) null);
                                                        IoUtils.closeQuietly((AutoCloseable) r3);
                                                    } catch (Throwable th2) {
                                                        r1 = 0;
                                                        th = th2;
                                                        IoUtils.closeQuietly(fileInputStream3);
                                                        IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                                        IoUtils.closeQuietly((AutoCloseable) r1);
                                                        IoUtils.closeQuietly((AutoCloseable) r3);
                                                        if (fileAcquireFile != null) {
                                                            WriteCommand.this.mFileProvider.releaseFile();
                                                        }
                                                        throw th;
                                                    }
                                                } catch (RemoteException | IOException e2) {
                                                    e = e2;
                                                    r1 = 0;
                                                    Log.e("RemotePrintDocument", "Error calling write()", e);
                                                    IoUtils.closeQuietly(fileInputStream3);
                                                    IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                                    IoUtils.closeQuietly((AutoCloseable) r1);
                                                    IoUtils.closeQuietly((AutoCloseable) r3);
                                                    if (fileAcquireFile != null) {
                                                    }
                                                }
                                            } catch (RemoteException | IOException e3) {
                                                e = e3;
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    } catch (RemoteException | IOException e4) {
                                        e = e4;
                                        fileOutputStream = 0;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        fileOutputStream = 0;
                                        fileInputStream3 = fileInputStream3;
                                        th = th;
                                        IoUtils.closeQuietly(fileInputStream3);
                                        IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                        IoUtils.closeQuietly((AutoCloseable) r1);
                                        IoUtils.closeQuietly((AutoCloseable) r3);
                                        if (fileAcquireFile != null) {
                                        }
                                        throw th;
                                    }
                                } catch (RemoteException | IOException e5) {
                                    e = e5;
                                    fileInputStream3 = null;
                                    fileOutputStream = 0;
                                } catch (Throwable th5) {
                                    th = th5;
                                    fileInputStream3 = null;
                                    fileOutputStream = 0;
                                }
                            } catch (RemoteException | IOException e6) {
                                e = e6;
                                fileInputStream3 = null;
                                r3 = r3;
                                fileOutputStream = fileInputStream3;
                                e = e;
                                r1 = fileOutputStream;
                                Log.e("RemotePrintDocument", "Error calling write()", e);
                                IoUtils.closeQuietly(fileInputStream3);
                                IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                IoUtils.closeQuietly((AutoCloseable) r1);
                                IoUtils.closeQuietly((AutoCloseable) r3);
                                if (fileAcquireFile != null) {
                                }
                                return null;
                            } catch (Throwable th6) {
                                th = th6;
                                fileInputStream3 = null;
                                r3 = r3;
                                fileOutputStream = fileInputStream3;
                                th = th;
                                r1 = fileOutputStream;
                                IoUtils.closeQuietly(fileInputStream3);
                                IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                IoUtils.closeQuietly((AutoCloseable) r1);
                                IoUtils.closeQuietly((AutoCloseable) r3);
                                if (fileAcquireFile != null) {
                                }
                                throw th;
                            }
                        } catch (RemoteException | IOException e7) {
                            e = e7;
                            fileInputStream2 = null;
                            fileInputStream3 = fileInputStream2;
                            r3 = fileInputStream2;
                            fileOutputStream = fileInputStream3;
                            e = e;
                            r1 = fileOutputStream;
                            Log.e("RemotePrintDocument", "Error calling write()", e);
                            IoUtils.closeQuietly(fileInputStream3);
                            IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                            IoUtils.closeQuietly((AutoCloseable) r1);
                            IoUtils.closeQuietly((AutoCloseable) r3);
                            if (fileAcquireFile != null) {
                            }
                            return null;
                        } catch (Throwable th7) {
                            th = th7;
                            fileInputStream = null;
                            fileInputStream3 = fileInputStream;
                            r3 = fileInputStream;
                            fileOutputStream = fileInputStream3;
                            th = th;
                            r1 = fileOutputStream;
                            IoUtils.closeQuietly(fileInputStream3);
                            IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                            IoUtils.closeQuietly((AutoCloseable) r1);
                            IoUtils.closeQuietly((AutoCloseable) r3);
                            if (fileAcquireFile != null) {
                            }
                            throw th;
                        }
                    } catch (RemoteException | IOException e8) {
                        e = e8;
                        fileAcquireFile = null;
                        fileInputStream2 = null;
                    } catch (Throwable th8) {
                        th = th8;
                        fileAcquireFile = null;
                        fileInputStream = null;
                    }
                    if (fileAcquireFile != null) {
                        WriteCommand.this.mFileProvider.releaseFile();
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        }

        private void handleOnWriteStarted(ICancellationSignal iCancellationSignal, int i) {
            if (i != this.mSequence) {
                return;
            }
            if (isCanceling()) {
                try {
                    iCancellationSignal.cancel();
                    return;
                } catch (RemoteException e) {
                    Log.e("RemotePrintDocument", "Error cancelling", e);
                    handleOnWriteFailed(null, i);
                    return;
                }
            }
            this.mCancellation = iCancellationSignal;
        }

        private void handleOnWriteFinished(PageRange[] pageRangeArr, int i) {
            if (i != this.mSequence) {
                return;
            }
            PageRange[] pageRangeArrNormalize = PageRangeUtils.normalize(pageRangeArr);
            PageRange[] pageRangeArrComputeWhichPagesInFileToPrint = PageRangeUtils.computeWhichPagesInFileToPrint(this.mPages, pageRangeArrNormalize, this.mPageCount);
            if (pageRangeArrComputeWhichPagesInFileToPrint == null) {
                this.mDocument.pagesWrittenToFile = null;
                this.mDocument.pagesInFileToPrint = null;
                failed(this.mContext.getString(R.string.print_error_default_message));
            } else {
                this.mDocument.pagesWrittenToFile = pageRangeArrNormalize;
                this.mDocument.pagesInFileToPrint = pageRangeArrComputeWhichPagesInFileToPrint;
                completed();
            }
            this.mCancellation = null;
            this.mWriteDoneCallback.onDone();
        }

        private void handleOnWriteFailed(CharSequence charSequence, int i) {
            if (i != this.mSequence) {
                return;
            }
            failed(charSequence);
            this.mCancellation = null;
            this.mWriteDoneCallback.onDone();
        }

        private void handleOnWriteCanceled(int i) {
            if (i != this.mSequence) {
                return;
            }
            canceled();
            this.mCancellation = null;
            this.mWriteDoneCallback.onDone();
        }

        private final class WriteHandler extends Handler {
            public WriteHandler(Looper looper) {
                super(looper, null, false);
            }

            @Override
            public void handleMessage(Message message) {
                if (WriteCommand.this.isFailed()) {
                }
                int i = message.what;
                CharSequence charSequence = null;
                int i2 = message.arg1;
                switch (i) {
                    case 3:
                        charSequence = (CharSequence) message.obj;
                    case 2:
                        WriteCommand.this.removeForceCancel();
                        break;
                    case 4:
                        if (!WriteCommand.this.isCanceling()) {
                            Log.w("RemotePrintDocument", "Unexpected cancel");
                            i = 3;
                        }
                        WriteCommand.this.removeForceCancel();
                        break;
                }
                if (WriteCommand.this.isCanceling() && i != 1) {
                    i = 4;
                }
                switch (i) {
                    case 1:
                        WriteCommand.this.handleOnWriteStarted((ICancellationSignal) message.obj, i2);
                        break;
                    case 2:
                        WriteCommand.this.handleOnWriteFinished((PageRange[]) message.obj, i2);
                        break;
                    case 3:
                        WriteCommand.this.handleOnWriteFailed(charSequence, i2);
                        break;
                    case 4:
                        WriteCommand.this.handleOnWriteCanceled(i2);
                        break;
                }
            }
        }

        private static final class WriteResultCallback extends IWriteResultCallback.Stub {
            private final WeakReference<Handler> mWeakHandler;

            public WriteResultCallback(Handler handler) {
                this.mWeakHandler = new WeakReference<>(handler);
            }

            public void onWriteStarted(ICancellationSignal iCancellationSignal, int i) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(1, i, 0, iCancellationSignal).sendToTarget();
                }
            }

            public void onWriteFinished(PageRange[] pageRangeArr, int i) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(2, i, 0, pageRangeArr).sendToTarget();
                }
            }

            public void onWriteFailed(CharSequence charSequence, int i) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(3, i, 0, charSequence).sendToTarget();
                }
            }

            public void onWriteCanceled(int i) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(4, i, 0).sendToTarget();
                }
            }
        }
    }

    private void onPrintingAppDied() {
        this.mState = 4;
        new Handler(this.mLooper).post(new Runnable() {
            @Override
            public void run() {
                RemotePrintDocument.this.mAdapterDeathObserver.onDied();
            }
        });
    }

    private static final class PrintDocumentAdapterObserver extends IPrintDocumentAdapterObserver.Stub {
        private final WeakReference<RemotePrintDocument> mWeakDocument;

        public PrintDocumentAdapterObserver(RemotePrintDocument remotePrintDocument) {
            this.mWeakDocument = new WeakReference<>(remotePrintDocument);
        }

        public void onDestroy() {
            RemotePrintDocument remotePrintDocument = this.mWeakDocument.get();
            if (remotePrintDocument != null) {
                remotePrintDocument.onPrintingAppDied();
            }
        }
    }
}
