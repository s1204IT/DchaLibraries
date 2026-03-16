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
import com.android.printspooler.R;
import com.android.printspooler.util.PageRangeUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
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
                        if (RemotePrintDocument.this.mUpdateSpec.pages == null || (!RemotePrintDocument.this.mDocumentInfo.changed && (RemotePrintDocument.this.mDocumentInfo.info.getPageCount() == -1 || PageRangeUtils.contains(RemotePrintDocument.this.mDocumentInfo.writtenPages, RemotePrintDocument.this.mUpdateSpec.pages, RemotePrintDocument.this.mDocumentInfo.info.getPageCount())))) {
                            if (RemotePrintDocument.this.mUpdateSpec.pages != null) {
                                RemotePrintDocument.this.mDocumentInfo.printedPages = PageRangeUtils.computePrintedPages(RemotePrintDocument.this.mUpdateSpec.pages, RemotePrintDocument.this.mDocumentInfo.writtenPages, RemotePrintDocument.this.mDocumentInfo.info.getPageCount());
                            }
                            RemotePrintDocument.this.mState = 3;
                            RemotePrintDocument.this.notifyUpdateCompleted();
                        } else {
                            RemotePrintDocument.this.mNextCommand = new WriteCommand(RemotePrintDocument.this.mContext, RemotePrintDocument.this.mLooper, RemotePrintDocument.this.mPrintDocumentAdapter, RemotePrintDocument.this.mDocumentInfo, RemotePrintDocument.this.mDocumentInfo.info.getPageCount(), RemotePrintDocument.this.mUpdateSpec.pages, RemotePrintDocument.this.mDocumentInfo.fileProvider, RemotePrintDocument.this.mCommandResultCallback);
                        }
                    }
                } else {
                    RemotePrintDocument.this.mState = 3;
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
                RemotePrintDocument.this.runPendingCommand();
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
        public PageRange[] printedPages;
        public PageRange[] writtenPages;
    }

    public interface UpdateResultCallbacks {
        void onUpdateCanceled();

        void onUpdateCompleted(RemotePrintDocumentInfo remotePrintDocumentInfo);

        void onUpdateFailed(CharSequence charSequence);
    }

    public RemotePrintDocument(Context context, IPrintDocumentAdapter adapter, MutexFileProvider fileProvider, RemoteAdapterDeathObserver deathObserver, UpdateResultCallbacks callbacks) {
        this.mPrintDocumentAdapter = adapter;
        this.mLooper = context.getMainLooper();
        this.mContext = context;
        this.mAdapterDeathObserver = deathObserver;
        this.mDocumentInfo.fileProvider = fileProvider;
        this.mUpdateCallbacks = callbacks;
        connectToRemoteDocument();
    }

    public void start() {
        if (this.mState != 0) {
            throw new IllegalStateException("Cannot start in state:" + stateToString(this.mState));
        }
        try {
            this.mPrintDocumentAdapter.start();
            this.mState = 1;
        } catch (RemoteException re) {
            Log.e("RemotePrintDocument", "Error calling start()", re);
            this.mState = 4;
        }
    }

    public boolean update(PrintAttributes attributes, PageRange[] pages, boolean preview) {
        boolean willUpdate;
        if (hasUpdateError()) {
            throw new IllegalStateException("Cannot update without a clearing the failure");
        }
        if (this.mState == 0 || this.mState == 5 || this.mState == 8) {
            throw new IllegalStateException("Cannot update in state:" + stateToString(this.mState));
        }
        if (!this.mUpdateSpec.hasSameConstraints(attributes, preview)) {
            willUpdate = true;
            if (this.mCurrentCommand != null && (this.mCurrentCommand.isRunning() || this.mCurrentCommand.isPending())) {
                this.mCurrentCommand.cancel();
            }
            PrintAttributes oldAttributes = this.mDocumentInfo.attributes != null ? this.mDocumentInfo.attributes : new PrintAttributes.Builder().build();
            AsyncCommand command = new LayoutCommand(this.mLooper, this.mPrintDocumentAdapter, this.mDocumentInfo, oldAttributes, attributes, preview, this.mCommandResultCallback);
            scheduleCommand(command);
            this.mState = 2;
        } else if ((!(this.mCurrentCommand instanceof LayoutCommand) || (!this.mCurrentCommand.isPending() && !this.mCurrentCommand.isRunning())) && pages != null && !PageRangeUtils.contains(this.mUpdateSpec.pages, pages, this.mDocumentInfo.info.getPageCount())) {
            willUpdate = true;
            if ((this.mCurrentCommand instanceof WriteCommand) && (this.mCurrentCommand.isPending() || this.mCurrentCommand.isRunning())) {
                this.mCurrentCommand.cancel();
            }
            AsyncCommand command2 = new WriteCommand(this.mContext, this.mLooper, this.mPrintDocumentAdapter, this.mDocumentInfo, this.mDocumentInfo.info.getPageCount(), pages, this.mDocumentInfo.fileProvider, this.mCommandResultCallback);
            scheduleCommand(command2);
            this.mState = 2;
        } else {
            willUpdate = false;
        }
        this.mUpdateSpec.update(attributes, preview, pages);
        runPendingCommand();
        return willUpdate;
    }

    public void finish() {
        if (this.mState != 1 && this.mState != 3 && this.mState != 4 && this.mState != 6 && this.mState != 7) {
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

    public void cancel() {
        if (this.mState != 6) {
            if (this.mState != 2) {
                throw new IllegalStateException("Cannot cancel in state:" + stateToString(this.mState));
            }
            this.mState = 6;
            this.mCurrentCommand.cancel();
        }
    }

    public void destroy() {
        if (this.mState == 8) {
            throw new IllegalStateException("Cannot destroy in state:" + stateToString(this.mState));
        }
        this.mState = 8;
        disconnectFromRemoteDocument();
    }

    public void kill(String reason) {
        try {
            this.mPrintDocumentAdapter.kill(reason);
        } catch (RemoteException re) {
            Log.e("RemotePrintDocument", "Error calling kill()", re);
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
        InputStream in;
        File file = null;
        InputStream in2 = null;
        OutputStream out = null;
        try {
            try {
                file = this.mDocumentInfo.fileProvider.acquireFile(null);
                in = new FileInputStream(file);
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            out = contentResolver.openOutputStream(uri);
            byte[] buffer = new byte[8192];
            while (true) {
                int readByteCount = in.read(buffer);
                if (readByteCount < 0) {
                    break;
                } else {
                    out.write(buffer, 0, readByteCount);
                }
            }
            IoUtils.closeQuietly(in);
            IoUtils.closeQuietly(out);
            if (file != null) {
                this.mDocumentInfo.fileProvider.releaseFile();
            }
        } catch (IOException e2) {
            e = e2;
            in2 = in;
            Log.e("RemotePrintDocument", "Error writing document content.", e);
            IoUtils.closeQuietly(in2);
            IoUtils.closeQuietly(out);
            if (file != null) {
                this.mDocumentInfo.fileProvider.releaseFile();
            }
        } catch (Throwable th2) {
            th = th2;
            in2 = in;
            IoUtils.closeQuietly(in2);
            IoUtils.closeQuietly(out);
            if (file != null) {
                this.mDocumentInfo.fileProvider.releaseFile();
            }
            throw th;
        }
    }

    private void notifyUpdateCanceled() {
        this.mUpdateCallbacks.onUpdateCanceled();
    }

    private void notifyUpdateCompleted() {
        this.mUpdateCallbacks.onUpdateCompleted(this.mDocumentInfo);
    }

    private void notifyUpdateFailed(CharSequence error) {
        this.mUpdateCallbacks.onUpdateFailed(error);
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

    private void scheduleCommand(AsyncCommand command) {
        if (this.mCurrentCommand == null) {
            this.mCurrentCommand = command;
        } else {
            this.mNextCommand = command;
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
            }
            this.mState = 2;
            return;
        }
        this.mState = 3;
    }

    private static String stateToString(int state) {
        switch (state) {
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

        public void update(PrintAttributes attributes, boolean preview, PageRange[] pages) {
            this.attributes.copyFrom(attributes);
            this.preview = preview;
            this.pages = pages != null ? (PageRange[]) Arrays.copyOf(pages, pages.length) : null;
        }

        public void reset() {
            this.attributes.clear();
            this.preview = false;
            this.pages = null;
        }

        public boolean hasSameConstraints(PrintAttributes attributes, boolean preview) {
            return this.attributes.equals(attributes) && this.preview == preview;
        }
    }

    private static abstract class AsyncCommand implements Runnable {
        private static int sSequenceCounter;
        protected final IPrintDocumentAdapter mAdapter;
        protected ICancellationSignal mCancellation;
        protected final RemotePrintDocumentInfo mDocument;
        protected final CommandDoneCallback mDoneCallback;
        private CharSequence mError;
        protected final int mSequence;
        private int mState;

        public AsyncCommand(IPrintDocumentAdapter adapter, RemotePrintDocumentInfo document, CommandDoneCallback doneCallback) {
            int i = sSequenceCounter;
            sSequenceCounter = i + 1;
            this.mSequence = i;
            this.mState = 0;
            this.mAdapter = adapter;
            this.mDocument = document;
            this.mDoneCallback = doneCallback;
        }

        protected final boolean isCanceling() {
            return this.mState == 4;
        }

        public final boolean isCanceled() {
            return this.mState == 3;
        }

        public final void cancel() {
            if (isRunning()) {
                canceling();
                if (this.mCancellation != null) {
                    try {
                        this.mCancellation.cancel();
                        return;
                    } catch (RemoteException re) {
                        Log.w("RemotePrintDocument", "Error while canceling", re);
                        return;
                    }
                }
                return;
            }
            canceled();
            this.mDoneCallback.onDone();
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

        protected final void failed(CharSequence error) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not running.");
            }
            this.mState = 5;
            this.mError = error;
        }

        public final boolean isFailed() {
            return this.mState == 5;
        }

        public CharSequence getError() {
            return this.mError;
        }
    }

    private static final class LayoutCommand extends AsyncCommand {
        private final Handler mHandler;
        private final Bundle mMetadata;
        private final PrintAttributes mNewAttributes;
        private final PrintAttributes mOldAttributes;
        private final ILayoutResultCallback mRemoteResultCallback;

        public LayoutCommand(Looper looper, IPrintDocumentAdapter adapter, RemotePrintDocumentInfo document, PrintAttributes oldAttributes, PrintAttributes newAttributes, boolean preview, CommandDoneCallback callback) {
            super(adapter, document, callback);
            this.mOldAttributes = new PrintAttributes.Builder().build();
            this.mNewAttributes = new PrintAttributes.Builder().build();
            this.mMetadata = new Bundle();
            this.mHandler = new LayoutHandler(looper);
            this.mRemoteResultCallback = new LayoutResultCallback(this.mHandler);
            this.mOldAttributes.copyFrom(oldAttributes);
            this.mNewAttributes.copyFrom(newAttributes);
            this.mMetadata.putBoolean("EXTRA_PRINT_PREVIEW", preview);
        }

        @Override
        public void run() {
            running();
            try {
                this.mDocument.changed = false;
                this.mAdapter.layout(this.mOldAttributes, this.mNewAttributes, this.mRemoteResultCallback, this.mMetadata, this.mSequence);
            } catch (RemoteException re) {
                Log.e("RemotePrintDocument", "Error calling layout", re);
                handleOnLayoutFailed(null, this.mSequence);
            }
        }

        private void handleOnLayoutStarted(ICancellationSignal cancellation, int sequence) {
            if (sequence == this.mSequence) {
                if (isCanceling()) {
                    try {
                        cancellation.cancel();
                        return;
                    } catch (RemoteException re) {
                        Log.e("RemotePrintDocument", "Error cancelling", re);
                        handleOnLayoutFailed(null, this.mSequence);
                        return;
                    }
                }
                this.mCancellation = cancellation;
            }
        }

        private void handleOnLayoutFinished(PrintDocumentInfo info, boolean changed, int sequence) {
            if (sequence == this.mSequence) {
                completed();
                if (changed || !equalsIgnoreSize(this.mDocument.info, info)) {
                    this.mDocument.writtenPages = null;
                    this.mDocument.printedPages = null;
                    this.mDocument.changed = true;
                }
                this.mDocument.attributes = this.mNewAttributes;
                this.mDocument.metadata = this.mMetadata;
                this.mDocument.laidout = true;
                this.mDocument.info = info;
                this.mCancellation = null;
                this.mDoneCallback.onDone();
            }
        }

        private void handleOnLayoutFailed(CharSequence error, int sequence) {
            if (sequence == this.mSequence) {
                this.mDocument.laidout = false;
                failed(error);
                this.mCancellation = null;
                this.mDoneCallback.onDone();
            }
        }

        private void handleOnLayoutCanceled(int sequence) {
            if (sequence == this.mSequence) {
                canceled();
                this.mCancellation = null;
                this.mDoneCallback.onDone();
            }
        }

        private boolean equalsIgnoreSize(PrintDocumentInfo lhs, PrintDocumentInfo rhs) {
            if (lhs == rhs) {
                return true;
            }
            if (lhs != null && rhs != null) {
                return lhs.getContentType() == rhs.getContentType() && lhs.getPageCount() == rhs.getPageCount();
            }
            return false;
        }

        private final class LayoutHandler extends Handler {
            public LayoutHandler(Looper looper) {
                super(looper, null, false);
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        ICancellationSignal cancellation = (ICancellationSignal) message.obj;
                        int sequence = message.arg1;
                        LayoutCommand.this.handleOnLayoutStarted(cancellation, sequence);
                        break;
                    case 2:
                        PrintDocumentInfo info = (PrintDocumentInfo) message.obj;
                        boolean changed = message.arg1 == 1;
                        int sequence2 = message.arg2;
                        LayoutCommand.this.handleOnLayoutFinished(info, changed, sequence2);
                        break;
                    case 3:
                        CharSequence error = (CharSequence) message.obj;
                        int sequence3 = message.arg1;
                        LayoutCommand.this.handleOnLayoutFailed(error, sequence3);
                        break;
                    case 4:
                        int sequence4 = message.arg1;
                        LayoutCommand.this.handleOnLayoutCanceled(sequence4);
                        break;
                }
            }
        }

        private static final class LayoutResultCallback extends ILayoutResultCallback.Stub {
            private final WeakReference<Handler> mWeakHandler;

            public LayoutResultCallback(Handler handler) {
                this.mWeakHandler = new WeakReference<>(handler);
            }

            public void onLayoutStarted(ICancellationSignal cancellation, int sequence) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(1, sequence, 0, cancellation).sendToTarget();
                }
            }

            public void onLayoutFinished(PrintDocumentInfo info, boolean changed, int sequence) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(2, changed ? 1 : 0, sequence, info).sendToTarget();
                }
            }

            public void onLayoutFailed(CharSequence error, int sequence) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(3, sequence, 0, error).sendToTarget();
                }
            }

            public void onLayoutCanceled(int sequence) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(4, sequence, 0).sendToTarget();
                }
            }
        }
    }

    private static final class WriteCommand extends AsyncCommand {
        private final Context mContext;
        private final CommandDoneCallback mDoneCallback;
        private final MutexFileProvider mFileProvider;
        private final Handler mHandler;
        private final int mPageCount;
        private final PageRange[] mPages;
        private final IWriteResultCallback mRemoteResultCallback;

        public WriteCommand(Context context, Looper looper, IPrintDocumentAdapter adapter, RemotePrintDocumentInfo document, int pageCount, PageRange[] pages, MutexFileProvider fileProvider, CommandDoneCallback callback) {
            super(adapter, document, callback);
            this.mContext = context;
            this.mHandler = new WriteHandler(looper);
            this.mRemoteResultCallback = new WriteResultCallback(this.mHandler);
            this.mPageCount = pageCount;
            this.mPages = (PageRange[]) Arrays.copyOf(pages, pages.length);
            this.mFileProvider = fileProvider;
            this.mDoneCallback = callback;
        }

        @Override
        public void run() {
            running();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) throws Throwable {
                    Exception e;
                    InputStream in;
                    OutputStream out;
                    byte[] buffer;
                    File file = null;
                    InputStream in2 = null;
                    OutputStream out2 = null;
                    ParcelFileDescriptor source = null;
                    ParcelFileDescriptor sink = null;
                    try {
                        try {
                            file = WriteCommand.this.mFileProvider.acquireFile(null);
                            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                            source = pipe[0];
                            sink = pipe[1];
                            in = new FileInputStream(source.getFileDescriptor());
                            try {
                                out = new FileOutputStream(file);
                            } catch (RemoteException e2) {
                                e = e2;
                                in2 = in;
                            } catch (IOException e3) {
                                e = e3;
                                in2 = in;
                            } catch (Throwable th) {
                                th = th;
                                in2 = in;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } catch (RemoteException e4) {
                        e = e4;
                    } catch (IOException e5) {
                        e = e5;
                    }
                    try {
                        WriteCommand.this.mAdapter.write(WriteCommand.this.mPages, sink, WriteCommand.this.mRemoteResultCallback, WriteCommand.this.mSequence);
                        sink.close();
                        sink = null;
                        buffer = new byte[8192];
                    } catch (RemoteException e6) {
                        e = e6;
                        out2 = out;
                        in2 = in;
                        e = e;
                        Log.e("RemotePrintDocument", "Error calling write()", e);
                        IoUtils.closeQuietly(in2);
                        IoUtils.closeQuietly(out2);
                        IoUtils.closeQuietly(sink);
                        IoUtils.closeQuietly(source);
                        if (file != null) {
                            return null;
                        }
                        WriteCommand.this.mFileProvider.releaseFile();
                        return null;
                    } catch (IOException e7) {
                        e = e7;
                        out2 = out;
                        in2 = in;
                        e = e;
                        Log.e("RemotePrintDocument", "Error calling write()", e);
                        IoUtils.closeQuietly(in2);
                        IoUtils.closeQuietly(out2);
                        IoUtils.closeQuietly(sink);
                        IoUtils.closeQuietly(source);
                        if (file != null) {
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        out2 = out;
                        in2 = in;
                    }
                    while (true) {
                        int readByteCount = in.read(buffer);
                        if (readByteCount < 0) {
                            break;
                        }
                        out.write(buffer, 0, readByteCount);
                        IoUtils.closeQuietly(in2);
                        IoUtils.closeQuietly(out2);
                        IoUtils.closeQuietly(sink);
                        IoUtils.closeQuietly(source);
                        if (file != null) {
                            WriteCommand.this.mFileProvider.releaseFile();
                        }
                        throw th;
                    }
                    IoUtils.closeQuietly(in);
                    IoUtils.closeQuietly(out);
                    IoUtils.closeQuietly((AutoCloseable) null);
                    IoUtils.closeQuietly(source);
                    if (file == null) {
                        return null;
                    }
                    WriteCommand.this.mFileProvider.releaseFile();
                    out2 = out;
                    in2 = in;
                    return null;
                    IoUtils.closeQuietly(in2);
                    IoUtils.closeQuietly(out2);
                    IoUtils.closeQuietly(sink);
                    IoUtils.closeQuietly(source);
                    if (file != null) {
                    }
                    throw th;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }

        private void handleOnWriteStarted(ICancellationSignal cancellation, int sequence) {
            if (sequence == this.mSequence) {
                if (isCanceling()) {
                    try {
                        cancellation.cancel();
                        return;
                    } catch (RemoteException re) {
                        Log.e("RemotePrintDocument", "Error cancelling", re);
                        handleOnWriteFailed(null, sequence);
                        return;
                    }
                }
                this.mCancellation = cancellation;
            }
        }

        private void handleOnWriteFinished(PageRange[] pages, int sequence) {
            if (sequence == this.mSequence) {
                PageRange[] writtenPages = PageRangeUtils.normalize(pages);
                PageRange[] printedPages = PageRangeUtils.computePrintedPages(this.mPages, writtenPages, this.mPageCount);
                if (printedPages != null) {
                    this.mDocument.writtenPages = writtenPages;
                    this.mDocument.printedPages = printedPages;
                    completed();
                } else {
                    this.mDocument.writtenPages = null;
                    this.mDocument.printedPages = null;
                    failed(this.mContext.getString(R.string.print_error_default_message));
                }
                this.mCancellation = null;
                this.mDoneCallback.onDone();
            }
        }

        private void handleOnWriteFailed(CharSequence error, int sequence) {
            if (sequence == this.mSequence) {
                failed(error);
                this.mCancellation = null;
                this.mDoneCallback.onDone();
            }
        }

        private void handleOnWriteCanceled(int sequence) {
            if (sequence == this.mSequence) {
                canceled();
                this.mCancellation = null;
                this.mDoneCallback.onDone();
            }
        }

        private final class WriteHandler extends Handler {
            public WriteHandler(Looper looper) {
                super(looper, null, false);
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        ICancellationSignal cancellation = (ICancellationSignal) message.obj;
                        int sequence = message.arg1;
                        WriteCommand.this.handleOnWriteStarted(cancellation, sequence);
                        break;
                    case 2:
                        PageRange[] pages = (PageRange[]) message.obj;
                        int sequence2 = message.arg1;
                        WriteCommand.this.handleOnWriteFinished(pages, sequence2);
                        break;
                    case 3:
                        CharSequence error = (CharSequence) message.obj;
                        int sequence3 = message.arg1;
                        WriteCommand.this.handleOnWriteFailed(error, sequence3);
                        break;
                    case 4:
                        int sequence4 = message.arg1;
                        WriteCommand.this.handleOnWriteCanceled(sequence4);
                        break;
                }
            }
        }

        private static final class WriteResultCallback extends IWriteResultCallback.Stub {
            private final WeakReference<Handler> mWeakHandler;

            public WriteResultCallback(Handler handler) {
                this.mWeakHandler = new WeakReference<>(handler);
            }

            public void onWriteStarted(ICancellationSignal cancellation, int sequence) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(1, sequence, 0, cancellation).sendToTarget();
                }
            }

            public void onWriteFinished(PageRange[] pages, int sequence) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(2, sequence, 0, pages).sendToTarget();
                }
            }

            public void onWriteFailed(CharSequence error, int sequence) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(3, sequence, 0, error).sendToTarget();
                }
            }

            public void onWriteCanceled(int sequence) {
                Handler handler = this.mWeakHandler.get();
                if (handler != null) {
                    handler.obtainMessage(4, sequence, 0).sendToTarget();
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

        public PrintDocumentAdapterObserver(RemotePrintDocument document) {
            this.mWeakDocument = new WeakReference<>(document);
        }

        public void onDestroy() {
            RemotePrintDocument document = this.mWeakDocument.get();
            if (document != null) {
                document.onPrintingAppDied();
            }
        }
    }
}
