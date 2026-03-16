package android.print;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ICancellationSignal;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.print.IPrintDocumentAdapter;
import android.print.IPrintJobStateChangeListener;
import android.print.PrintDocumentAdapter;
import android.printservice.PrintServiceInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.os.SomeArgs;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;

public final class PrintManager {
    public static final String ACTION_PRINT_DIALOG = "android.print.PRINT_DIALOG";
    public static final int APP_ID_ANY = -2;
    private static final boolean DEBUG = false;
    public static final String EXTRA_PRINT_DIALOG_INTENT = "android.print.intent.extra.EXTRA_PRINT_DIALOG_INTENT";
    public static final String EXTRA_PRINT_DOCUMENT_ADAPTER = "android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER";
    public static final String EXTRA_PRINT_JOB = "android.print.intent.extra.EXTRA_PRINT_JOB";
    private static final String LOG_TAG = "PrintManager";
    private static final int MSG_NOTIFY_PRINT_JOB_STATE_CHANGED = 1;
    private final int mAppId;
    private final Context mContext;
    private final Handler mHandler;
    private Map<PrintJobStateChangeListener, PrintJobStateChangeListenerWrapper> mPrintJobStateChangeListeners;
    private final IPrintManager mService;
    private final int mUserId;

    public interface PrintJobStateChangeListener {
        void onPrintJobStateChanged(PrintJobId printJobId);
    }

    public PrintManager(Context context, IPrintManager service, int userId, int appId) {
        this.mContext = context;
        this.mService = service;
        this.mUserId = userId;
        this.mAppId = appId;
        this.mHandler = new Handler(context.getMainLooper(), null, false) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        SomeArgs args = (SomeArgs) message.obj;
                        PrintJobStateChangeListenerWrapper wrapper = (PrintJobStateChangeListenerWrapper) args.arg1;
                        PrintJobStateChangeListener listener = wrapper.getListener();
                        if (listener != null) {
                            PrintJobId printJobId = (PrintJobId) args.arg2;
                            listener.onPrintJobStateChanged(printJobId);
                        }
                        args.recycle();
                        break;
                }
            }
        };
    }

    public PrintManager getGlobalPrintManagerForUser(int userId) {
        if (this.mService != null) {
            return new PrintManager(this.mContext, this.mService, userId, -2);
        }
        Log.w(LOG_TAG, "Feature android.software.print not available");
        return null;
    }

    PrintJobInfo getPrintJobInfo(PrintJobId printJobId) {
        try {
            return this.mService.getPrintJobInfo(printJobId, this.mAppId, this.mUserId);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting a print job info:" + printJobId, re);
            return null;
        }
    }

    public void addPrintJobStateChangeListener(PrintJobStateChangeListener listener) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        if (this.mPrintJobStateChangeListeners == null) {
            this.mPrintJobStateChangeListeners = new ArrayMap();
        }
        PrintJobStateChangeListenerWrapper wrappedListener = new PrintJobStateChangeListenerWrapper(listener, this.mHandler);
        try {
            this.mService.addPrintJobStateChangeListener(wrappedListener, this.mAppId, this.mUserId);
            this.mPrintJobStateChangeListeners.put(listener, wrappedListener);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error adding print job state change listener", re);
        }
    }

    public void removePrintJobStateChangeListener(PrintJobStateChangeListener listener) {
        PrintJobStateChangeListenerWrapper wrappedListener;
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        if (this.mPrintJobStateChangeListeners != null && (wrappedListener = this.mPrintJobStateChangeListeners.remove(listener)) != null) {
            if (this.mPrintJobStateChangeListeners.isEmpty()) {
                this.mPrintJobStateChangeListeners = null;
            }
            wrappedListener.destroy();
            try {
                this.mService.removePrintJobStateChangeListener(wrappedListener, this.mUserId);
            } catch (RemoteException re) {
                Log.e(LOG_TAG, "Error removing print job state change listener", re);
            }
        }
    }

    public PrintJob getPrintJob(PrintJobId printJobId) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return null;
        }
        try {
            PrintJobInfo printJob = this.mService.getPrintJobInfo(printJobId, this.mAppId, this.mUserId);
            if (printJob != null) {
                return new PrintJob(printJob, this);
            }
            return null;
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting print job", re);
            return null;
        }
    }

    public List<PrintJob> getPrintJobs() {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return Collections.emptyList();
        }
        try {
            List<PrintJobInfo> printJobInfos = this.mService.getPrintJobInfos(this.mAppId, this.mUserId);
            if (printJobInfos == null) {
                return Collections.emptyList();
            }
            int printJobCount = printJobInfos.size();
            List<PrintJob> printJobs = new ArrayList<>(printJobCount);
            for (int i = 0; i < printJobCount; i++) {
                printJobs.add(new PrintJob(printJobInfos.get(i), this));
            }
            return printJobs;
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting print jobs", re);
            return Collections.emptyList();
        }
    }

    void cancelPrintJob(PrintJobId printJobId) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        try {
            this.mService.cancelPrintJob(printJobId, this.mAppId, this.mUserId);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error canceling a print job: " + printJobId, re);
        }
    }

    void restartPrintJob(PrintJobId printJobId) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        try {
            this.mService.restartPrintJob(printJobId, this.mAppId, this.mUserId);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error restarting a print job: " + printJobId, re);
        }
    }

    public PrintJob print(String printJobName, PrintDocumentAdapter documentAdapter, PrintAttributes attributes) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return null;
        }
        if (!(this.mContext instanceof Activity)) {
            throw new IllegalStateException("Can print only from an activity");
        }
        if (TextUtils.isEmpty(printJobName)) {
            throw new IllegalArgumentException("printJobName cannot be empty");
        }
        if (documentAdapter == null) {
            throw new IllegalArgumentException("documentAdapter cannot be null");
        }
        PrintDocumentAdapterDelegate delegate = new PrintDocumentAdapterDelegate((Activity) this.mContext, documentAdapter);
        try {
            Bundle result = this.mService.print(printJobName, delegate, attributes, this.mContext.getPackageName(), this.mAppId, this.mUserId);
            if (result != null) {
                PrintJobInfo printJob = (PrintJobInfo) result.getParcelable(EXTRA_PRINT_JOB);
                IntentSender intent = (IntentSender) result.getParcelable(EXTRA_PRINT_DIALOG_INTENT);
                if (printJob == null || intent == null) {
                    return null;
                }
                try {
                    this.mContext.startIntentSender(intent, null, 0, 0, 0);
                    return new PrintJob(printJob, this);
                } catch (IntentSender.SendIntentException sie) {
                    Log.e(LOG_TAG, "Couldn't start print job config activity.", sie);
                    return null;
                }
            }
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error creating a print job", re);
        }
        return null;
    }

    public List<PrintServiceInfo> getEnabledPrintServices() {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return Collections.emptyList();
        }
        try {
            List<PrintServiceInfo> enabledServices = this.mService.getEnabledPrintServices(this.mUserId);
            if (enabledServices != null) {
                return enabledServices;
            }
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting the enabled print services", re);
        }
        return Collections.emptyList();
    }

    public List<PrintServiceInfo> getInstalledPrintServices() {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return Collections.emptyList();
        }
        try {
            List<PrintServiceInfo> installedServices = this.mService.getInstalledPrintServices(this.mUserId);
            if (installedServices != null) {
                return installedServices;
            }
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting the installed print services", re);
        }
        return Collections.emptyList();
    }

    public PrinterDiscoverySession createPrinterDiscoverySession() {
        if (this.mService != null) {
            return new PrinterDiscoverySession(this.mService, this.mContext, this.mUserId);
        }
        Log.w(LOG_TAG, "Feature android.software.print not available");
        return null;
    }

    private static final class PrintDocumentAdapterDelegate extends IPrintDocumentAdapter.Stub implements Application.ActivityLifecycleCallbacks {
        private Activity mActivity;
        private PrintDocumentAdapter mDocumentAdapter;
        private Handler mHandler;
        private final Object mLock = new Object();
        private IPrintDocumentAdapterObserver mObserver;
        private DestroyableCallback mPendingCallback;

        private interface DestroyableCallback {
            void destroy();
        }

        public PrintDocumentAdapterDelegate(Activity activity, PrintDocumentAdapter documentAdapter) {
            this.mActivity = activity;
            this.mDocumentAdapter = documentAdapter;
            this.mHandler = new MyHandler(this.mActivity.getMainLooper());
            this.mActivity.getApplication().registerActivityLifecycleCallbacks(this);
        }

        @Override
        public void setObserver(IPrintDocumentAdapterObserver observer) {
            boolean destroyed;
            synchronized (this.mLock) {
                this.mObserver = observer;
                destroyed = isDestroyedLocked();
            }
            if (destroyed && observer != null) {
                try {
                    observer.onDestroy();
                } catch (RemoteException re) {
                    Log.e(PrintManager.LOG_TAG, "Error announcing destroyed state", re);
                }
            }
        }

        @Override
        public void start() {
            synchronized (this.mLock) {
                if (!isDestroyedLocked()) {
                    this.mHandler.obtainMessage(1, this.mDocumentAdapter).sendToTarget();
                }
            }
        }

        @Override
        public void layout(PrintAttributes oldAttributes, PrintAttributes newAttributes, ILayoutResultCallback callback, Bundle metadata, int sequence) {
            ICancellationSignal cancellationTransport = CancellationSignal.createTransport();
            try {
                callback.onLayoutStarted(cancellationTransport, sequence);
                synchronized (this.mLock) {
                    if (!isDestroyedLocked()) {
                        CancellationSignal cancellationSignal = CancellationSignal.fromTransport(cancellationTransport);
                        SomeArgs args = SomeArgs.obtain();
                        args.arg1 = this.mDocumentAdapter;
                        args.arg2 = oldAttributes;
                        args.arg3 = newAttributes;
                        args.arg4 = cancellationSignal;
                        args.arg5 = new MyLayoutResultCallback(callback, sequence);
                        args.arg6 = metadata;
                        this.mHandler.obtainMessage(2, args).sendToTarget();
                    }
                }
            } catch (RemoteException re) {
                Log.e(PrintManager.LOG_TAG, "Error notifying for layout start", re);
            }
        }

        @Override
        public void write(PageRange[] pages, ParcelFileDescriptor fd, IWriteResultCallback callback, int sequence) {
            ICancellationSignal cancellationTransport = CancellationSignal.createTransport();
            try {
                callback.onWriteStarted(cancellationTransport, sequence);
                synchronized (this.mLock) {
                    if (!isDestroyedLocked()) {
                        CancellationSignal cancellationSignal = CancellationSignal.fromTransport(cancellationTransport);
                        SomeArgs args = SomeArgs.obtain();
                        args.arg1 = this.mDocumentAdapter;
                        args.arg2 = pages;
                        args.arg3 = fd;
                        args.arg4 = cancellationSignal;
                        args.arg5 = new MyWriteResultCallback(callback, fd, sequence);
                        this.mHandler.obtainMessage(3, args).sendToTarget();
                    }
                }
            } catch (RemoteException re) {
                Log.e(PrintManager.LOG_TAG, "Error notifying for write start", re);
            }
        }

        @Override
        public void finish() {
            synchronized (this.mLock) {
                if (!isDestroyedLocked()) {
                    this.mHandler.obtainMessage(4, this.mDocumentAdapter).sendToTarget();
                }
            }
        }

        @Override
        public void kill(String reason) {
            synchronized (this.mLock) {
                if (!isDestroyedLocked()) {
                    this.mHandler.obtainMessage(5, reason).sendToTarget();
                }
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            IPrintDocumentAdapterObserver observer = null;
            synchronized (this.mLock) {
                if (activity == this.mActivity) {
                    observer = this.mObserver;
                    destroyLocked();
                }
            }
            if (observer != null) {
                try {
                    observer.onDestroy();
                } catch (RemoteException re) {
                    Log.e(PrintManager.LOG_TAG, "Error announcing destroyed state", re);
                }
            }
        }

        private boolean isDestroyedLocked() {
            return this.mActivity == null;
        }

        private void destroyLocked() {
            this.mActivity.getApplication().unregisterActivityLifecycleCallbacks(this);
            this.mActivity = null;
            this.mDocumentAdapter = null;
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(3);
            this.mHandler.removeMessages(4);
            this.mHandler = null;
            this.mObserver = null;
            if (this.mPendingCallback != null) {
                this.mPendingCallback.destroy();
                this.mPendingCallback = null;
            }
        }

        private final class MyHandler extends Handler {
            public static final int MSG_ON_FINISH = 4;
            public static final int MSG_ON_KILL = 5;
            public static final int MSG_ON_LAYOUT = 2;
            public static final int MSG_ON_START = 1;
            public static final int MSG_ON_WRITE = 3;

            public MyHandler(Looper looper) {
                super(looper, null, true);
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        ((PrintDocumentAdapter) message.obj).onStart();
                        return;
                    case 2:
                        SomeArgs args = (SomeArgs) message.obj;
                        PrintDocumentAdapter adapter = (PrintDocumentAdapter) args.arg1;
                        PrintAttributes oldAttributes = (PrintAttributes) args.arg2;
                        PrintAttributes newAttributes = (PrintAttributes) args.arg3;
                        CancellationSignal cancellation = (CancellationSignal) args.arg4;
                        PrintDocumentAdapter.LayoutResultCallback callback = (PrintDocumentAdapter.LayoutResultCallback) args.arg5;
                        Bundle metadata = (Bundle) args.arg6;
                        args.recycle();
                        adapter.onLayout(oldAttributes, newAttributes, cancellation, callback, metadata);
                        return;
                    case 3:
                        SomeArgs args2 = (SomeArgs) message.obj;
                        PrintDocumentAdapter adapter2 = (PrintDocumentAdapter) args2.arg1;
                        PageRange[] pages = (PageRange[]) args2.arg2;
                        ParcelFileDescriptor fd = (ParcelFileDescriptor) args2.arg3;
                        CancellationSignal cancellation2 = (CancellationSignal) args2.arg4;
                        PrintDocumentAdapter.WriteResultCallback callback2 = (PrintDocumentAdapter.WriteResultCallback) args2.arg5;
                        args2.recycle();
                        adapter2.onWrite(pages, fd, cancellation2, callback2);
                        return;
                    case 4:
                        ((PrintDocumentAdapter) message.obj).onFinish();
                        synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                            PrintDocumentAdapterDelegate.this.destroyLocked();
                            break;
                        }
                        return;
                    case 5:
                        String reason = (String) message.obj;
                        throw new RuntimeException(reason);
                    default:
                        throw new IllegalArgumentException("Unknown message: " + message.what);
                }
            }
        }

        private final class MyLayoutResultCallback extends PrintDocumentAdapter.LayoutResultCallback implements DestroyableCallback {
            private ILayoutResultCallback mCallback;
            private final int mSequence;

            public MyLayoutResultCallback(ILayoutResultCallback callback, int sequence) {
                this.mCallback = callback;
                this.mSequence = sequence;
            }

            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                ILayoutResultCallback callback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    callback = this.mCallback;
                }
                if (callback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    if (info == null) {
                        throw new NullPointerException("document info cannot be null");
                    }
                    try {
                        callback.onLayoutFinished(info, changed, this.mSequence);
                    } catch (RemoteException re) {
                        Log.e(PrintManager.LOG_TAG, "Error calling onLayoutFinished", re);
                    }
                } finally {
                    destroy();
                }
            }

            @Override
            public void onLayoutFailed(CharSequence error) {
                ILayoutResultCallback callback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    callback = this.mCallback;
                }
                if (callback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    callback.onLayoutFailed(error, this.mSequence);
                } catch (RemoteException re) {
                    Log.e(PrintManager.LOG_TAG, "Error calling onLayoutFailed", re);
                } finally {
                    destroy();
                }
            }

            @Override
            public void onLayoutCancelled() {
                ILayoutResultCallback callback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    callback = this.mCallback;
                }
                if (callback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    callback.onLayoutCanceled(this.mSequence);
                } catch (RemoteException re) {
                    Log.e(PrintManager.LOG_TAG, "Error calling onLayoutFailed", re);
                } finally {
                    destroy();
                }
            }

            @Override
            public void destroy() {
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    this.mCallback = null;
                    PrintDocumentAdapterDelegate.this.mPendingCallback = null;
                }
            }
        }

        private final class MyWriteResultCallback extends PrintDocumentAdapter.WriteResultCallback implements DestroyableCallback {
            private IWriteResultCallback mCallback;
            private ParcelFileDescriptor mFd;
            private final int mSequence;

            public MyWriteResultCallback(IWriteResultCallback callback, ParcelFileDescriptor fd, int sequence) {
                this.mFd = fd;
                this.mSequence = sequence;
                this.mCallback = callback;
            }

            @Override
            public void onWriteFinished(PageRange[] pages) {
                IWriteResultCallback callback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    callback = this.mCallback;
                }
                if (callback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    if (pages == null) {
                        throw new IllegalArgumentException("pages cannot be null");
                    }
                    if (pages.length == 0) {
                        throw new IllegalArgumentException("pages cannot be empty");
                    }
                    try {
                        callback.onWriteFinished(pages, this.mSequence);
                    } catch (RemoteException re) {
                        Log.e(PrintManager.LOG_TAG, "Error calling onWriteFinished", re);
                    }
                } finally {
                    destroy();
                }
            }

            @Override
            public void onWriteFailed(CharSequence error) {
                IWriteResultCallback callback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    callback = this.mCallback;
                }
                if (callback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    callback.onWriteFailed(error, this.mSequence);
                } catch (RemoteException re) {
                    Log.e(PrintManager.LOG_TAG, "Error calling onWriteFailed", re);
                } finally {
                    destroy();
                }
            }

            @Override
            public void onWriteCancelled() {
                IWriteResultCallback callback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    callback = this.mCallback;
                }
                if (callback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    callback.onWriteCanceled(this.mSequence);
                } catch (RemoteException re) {
                    Log.e(PrintManager.LOG_TAG, "Error calling onWriteCanceled", re);
                } finally {
                    destroy();
                }
            }

            @Override
            public void destroy() {
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    IoUtils.closeQuietly(this.mFd);
                    this.mCallback = null;
                    this.mFd = null;
                    PrintDocumentAdapterDelegate.this.mPendingCallback = null;
                }
            }
        }
    }

    private static final class PrintJobStateChangeListenerWrapper extends IPrintJobStateChangeListener.Stub {
        private final WeakReference<Handler> mWeakHandler;
        private final WeakReference<PrintJobStateChangeListener> mWeakListener;

        public PrintJobStateChangeListenerWrapper(PrintJobStateChangeListener listener, Handler handler) {
            this.mWeakListener = new WeakReference<>(listener);
            this.mWeakHandler = new WeakReference<>(handler);
        }

        @Override
        public void onPrintJobStateChanged(PrintJobId printJobId) {
            Handler handler = this.mWeakHandler.get();
            PrintJobStateChangeListener listener = this.mWeakListener.get();
            if (handler != null && listener != null) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = this;
                args.arg2 = printJobId;
                handler.obtainMessage(1, args).sendToTarget();
            }
        }

        public void destroy() {
            this.mWeakListener.clear();
        }

        public PrintJobStateChangeListener getListener() {
            return this.mWeakListener.get();
        }
    }
}
