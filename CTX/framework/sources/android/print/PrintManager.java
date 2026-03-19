package android.print;

import android.annotation.SystemApi;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.drawable.Icon;
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
import android.print.IPrintServicesChangeListener;
import android.print.PrintDocumentAdapter;
import android.printservice.PrintServiceInfo;
import android.printservice.recommendation.IRecommendationsChangeListener;
import android.printservice.recommendation.RecommendationInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.Preconditions;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import libcore.io.IoUtils;

public final class PrintManager {
    public static final String ACTION_PRINT_DIALOG = "android.print.PRINT_DIALOG";
    public static final int ALL_SERVICES = 3;
    public static final int APP_ID_ANY = -2;
    private static final boolean DEBUG = false;
    public static final int DISABLED_SERVICES = 2;

    @SystemApi
    public static final int ENABLED_SERVICES = 1;
    public static final String EXTRA_PRINT_DIALOG_INTENT = "android.print.intent.extra.EXTRA_PRINT_DIALOG_INTENT";
    public static final String EXTRA_PRINT_DOCUMENT_ADAPTER = "android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER";
    public static final String EXTRA_PRINT_JOB = "android.print.intent.extra.EXTRA_PRINT_JOB";
    private static final String LOG_TAG = "PrintManager";
    private static final int MSG_NOTIFY_PRINT_JOB_STATE_CHANGED = 1;
    public static final String PRINT_SPOOLER_PACKAGE_NAME = "com.android.printspooler";
    private final int mAppId;
    private final Context mContext;
    private final Handler mHandler;
    private Map<PrintJobStateChangeListener, PrintJobStateChangeListenerWrapper> mPrintJobStateChangeListeners;
    private Map<PrintServiceRecommendationsChangeListener, PrintServiceRecommendationsChangeListenerWrapper> mPrintServiceRecommendationsChangeListeners;
    private Map<PrintServicesChangeListener, PrintServicesChangeListenerWrapper> mPrintServicesChangeListeners;
    private final IPrintManager mService;
    private final int mUserId;

    public interface PrintJobStateChangeListener {
        void onPrintJobStateChanged(PrintJobId printJobId);
    }

    @SystemApi
    public interface PrintServiceRecommendationsChangeListener {
        void onPrintServiceRecommendationsChanged();
    }

    @SystemApi
    public interface PrintServicesChangeListener {
        void onPrintServicesChanged();
    }

    public PrintManager(Context context, IPrintManager iPrintManager, int i, int i2) {
        this.mContext = context;
        this.mService = iPrintManager;
        this.mUserId = i;
        this.mAppId = i2;
        this.mHandler = new Handler(context.getMainLooper(), null, false) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    PrintJobStateChangeListener listener = ((PrintJobStateChangeListenerWrapper) someArgs.arg1).getListener();
                    if (listener != null) {
                        listener.onPrintJobStateChanged((PrintJobId) someArgs.arg2);
                    }
                    someArgs.recycle();
                }
            }
        };
    }

    public PrintManager getGlobalPrintManagerForUser(int i) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return null;
        }
        return new PrintManager(this.mContext, this.mService, i, -2);
    }

    PrintJobInfo getPrintJobInfo(PrintJobId printJobId) {
        try {
            return this.mService.getPrintJobInfo(printJobId, this.mAppId, this.mUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addPrintJobStateChangeListener(PrintJobStateChangeListener printJobStateChangeListener) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        if (this.mPrintJobStateChangeListeners == null) {
            this.mPrintJobStateChangeListeners = new ArrayMap();
        }
        PrintJobStateChangeListenerWrapper printJobStateChangeListenerWrapper = new PrintJobStateChangeListenerWrapper(printJobStateChangeListener, this.mHandler);
        try {
            this.mService.addPrintJobStateChangeListener(printJobStateChangeListenerWrapper, this.mAppId, this.mUserId);
            this.mPrintJobStateChangeListeners.put(printJobStateChangeListener, printJobStateChangeListenerWrapper);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removePrintJobStateChangeListener(PrintJobStateChangeListener printJobStateChangeListener) {
        PrintJobStateChangeListenerWrapper printJobStateChangeListenerWrapperRemove;
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        if (this.mPrintJobStateChangeListeners == null || (printJobStateChangeListenerWrapperRemove = this.mPrintJobStateChangeListeners.remove(printJobStateChangeListener)) == null) {
            return;
        }
        if (this.mPrintJobStateChangeListeners.isEmpty()) {
            this.mPrintJobStateChangeListeners = null;
        }
        printJobStateChangeListenerWrapperRemove.destroy();
        try {
            this.mService.removePrintJobStateChangeListener(printJobStateChangeListenerWrapperRemove, this.mUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public PrintJob getPrintJob(PrintJobId printJobId) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return null;
        }
        try {
            PrintJobInfo printJobInfo = this.mService.getPrintJobInfo(printJobId, this.mAppId, this.mUserId);
            if (printJobInfo != null) {
                return new PrintJob(printJobInfo, this);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Icon getCustomPrinterIcon(PrinterId printerId) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return null;
        }
        try {
            return this.mService.getCustomPrinterIcon(printerId, this.mUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
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
            int size = printJobInfos.size();
            ArrayList arrayList = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                arrayList.add(new PrintJob(printJobInfos.get(i), this));
            }
            return arrayList;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    void cancelPrintJob(PrintJobId printJobId) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        try {
            this.mService.cancelPrintJob(printJobId, this.mAppId, this.mUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    void restartPrintJob(PrintJobId printJobId) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        try {
            this.mService.restartPrintJob(printJobId, this.mAppId, this.mUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public PrintJob print(String str, PrintDocumentAdapter printDocumentAdapter, PrintAttributes printAttributes) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return null;
        }
        if (!(this.mContext instanceof Activity)) {
            throw new IllegalStateException("Can print only from an activity");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("printJobName cannot be empty");
        }
        if (printDocumentAdapter == null) {
            throw new IllegalArgumentException("documentAdapter cannot be null");
        }
        try {
            Bundle bundlePrint = this.mService.print(str, new PrintDocumentAdapterDelegate((Activity) this.mContext, printDocumentAdapter), printAttributes, this.mContext.getPackageName(), this.mAppId, this.mUserId);
            if (bundlePrint != null) {
                PrintJobInfo printJobInfo = (PrintJobInfo) bundlePrint.getParcelable(EXTRA_PRINT_JOB);
                IntentSender intentSender = (IntentSender) bundlePrint.getParcelable(EXTRA_PRINT_DIALOG_INTENT);
                if (printJobInfo == null || intentSender == null) {
                    return null;
                }
                try {
                    this.mContext.startIntentSender(intentSender, null, 0, 0, 0);
                    return new PrintJob(printJobInfo, this);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(LOG_TAG, "Couldn't start print job config activity.", e);
                }
            }
            return null;
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void addPrintServicesChangeListener(PrintServicesChangeListener printServicesChangeListener, Handler handler) {
        Preconditions.checkNotNull(printServicesChangeListener);
        if (handler == null) {
            handler = this.mHandler;
        }
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        if (this.mPrintServicesChangeListeners == null) {
            this.mPrintServicesChangeListeners = new ArrayMap();
        }
        PrintServicesChangeListenerWrapper printServicesChangeListenerWrapper = new PrintServicesChangeListenerWrapper(printServicesChangeListener, handler);
        try {
            this.mService.addPrintServicesChangeListener(printServicesChangeListenerWrapper, this.mUserId);
            this.mPrintServicesChangeListeners.put(printServicesChangeListener, printServicesChangeListenerWrapper);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void removePrintServicesChangeListener(PrintServicesChangeListener printServicesChangeListener) {
        PrintServicesChangeListenerWrapper printServicesChangeListenerWrapperRemove;
        Preconditions.checkNotNull(printServicesChangeListener);
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        if (this.mPrintServicesChangeListeners == null || (printServicesChangeListenerWrapperRemove = this.mPrintServicesChangeListeners.remove(printServicesChangeListener)) == null) {
            return;
        }
        if (this.mPrintServicesChangeListeners.isEmpty()) {
            this.mPrintServicesChangeListeners = null;
        }
        printServicesChangeListenerWrapperRemove.destroy();
        try {
            this.mService.removePrintServicesChangeListener(printServicesChangeListenerWrapperRemove, this.mUserId);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error removing print services change listener", e);
        }
    }

    @SystemApi
    public List<PrintServiceInfo> getPrintServices(int i) {
        Preconditions.checkFlagsArgument(i, 3);
        try {
            List<PrintServiceInfo> printServices = this.mService.getPrintServices(i, this.mUserId);
            if (printServices != null) {
                return printServices;
            }
            return Collections.emptyList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void addPrintServiceRecommendationsChangeListener(PrintServiceRecommendationsChangeListener printServiceRecommendationsChangeListener, Handler handler) {
        Preconditions.checkNotNull(printServiceRecommendationsChangeListener);
        if (handler == null) {
            handler = this.mHandler;
        }
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        if (this.mPrintServiceRecommendationsChangeListeners == null) {
            this.mPrintServiceRecommendationsChangeListeners = new ArrayMap();
        }
        PrintServiceRecommendationsChangeListenerWrapper printServiceRecommendationsChangeListenerWrapper = new PrintServiceRecommendationsChangeListenerWrapper(printServiceRecommendationsChangeListener, handler);
        try {
            this.mService.addPrintServiceRecommendationsChangeListener(printServiceRecommendationsChangeListenerWrapper, this.mUserId);
            this.mPrintServiceRecommendationsChangeListeners.put(printServiceRecommendationsChangeListener, printServiceRecommendationsChangeListenerWrapper);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void removePrintServiceRecommendationsChangeListener(PrintServiceRecommendationsChangeListener printServiceRecommendationsChangeListener) {
        PrintServiceRecommendationsChangeListenerWrapper printServiceRecommendationsChangeListenerWrapperRemove;
        Preconditions.checkNotNull(printServiceRecommendationsChangeListener);
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        if (this.mPrintServiceRecommendationsChangeListeners == null || (printServiceRecommendationsChangeListenerWrapperRemove = this.mPrintServiceRecommendationsChangeListeners.remove(printServiceRecommendationsChangeListener)) == null) {
            return;
        }
        if (this.mPrintServiceRecommendationsChangeListeners.isEmpty()) {
            this.mPrintServiceRecommendationsChangeListeners = null;
        }
        printServiceRecommendationsChangeListenerWrapperRemove.destroy();
        try {
            this.mService.removePrintServiceRecommendationsChangeListener(printServiceRecommendationsChangeListenerWrapperRemove, this.mUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public List<RecommendationInfo> getPrintServiceRecommendations() {
        try {
            List<RecommendationInfo> printServiceRecommendations = this.mService.getPrintServiceRecommendations(this.mUserId);
            if (printServiceRecommendations != null) {
                return printServiceRecommendations;
            }
            return Collections.emptyList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public PrinterDiscoverySession createPrinterDiscoverySession() {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return null;
        }
        return new PrinterDiscoverySession(this.mService, this.mContext, this.mUserId);
    }

    public void setPrintServiceEnabled(ComponentName componentName, boolean z) {
        if (this.mService == null) {
            Log.w(LOG_TAG, "Feature android.software.print not available");
            return;
        }
        try {
            this.mService.setPrintServiceEnabled(componentName, z, this.mUserId);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error enabling or disabling " + componentName, e);
        }
    }

    public static final class PrintDocumentAdapterDelegate extends IPrintDocumentAdapter.Stub implements Application.ActivityLifecycleCallbacks {
        private Activity mActivity;
        private PrintDocumentAdapter mDocumentAdapter;
        private Handler mHandler;
        private final Object mLock = new Object();
        private IPrintDocumentAdapterObserver mObserver;
        private DestroyableCallback mPendingCallback;

        private interface DestroyableCallback {
            void destroy();
        }

        public PrintDocumentAdapterDelegate(Activity activity, PrintDocumentAdapter printDocumentAdapter) {
            if (activity.isFinishing()) {
                throw new IllegalStateException("Cannot start printing for finishing activity");
            }
            this.mActivity = activity;
            this.mDocumentAdapter = printDocumentAdapter;
            this.mHandler = new MyHandler(this.mActivity.getMainLooper());
            this.mActivity.getApplication().registerActivityLifecycleCallbacks(this);
        }

        @Override
        public void setObserver(IPrintDocumentAdapterObserver iPrintDocumentAdapterObserver) {
            boolean zIsDestroyedLocked;
            synchronized (this.mLock) {
                this.mObserver = iPrintDocumentAdapterObserver;
                zIsDestroyedLocked = isDestroyedLocked();
            }
            if (zIsDestroyedLocked && iPrintDocumentAdapterObserver != null) {
                try {
                    iPrintDocumentAdapterObserver.onDestroy();
                } catch (RemoteException e) {
                    Log.e(PrintManager.LOG_TAG, "Error announcing destroyed state", e);
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
        public void layout(PrintAttributes printAttributes, PrintAttributes printAttributes2, ILayoutResultCallback iLayoutResultCallback, Bundle bundle, int i) {
            ICancellationSignal iCancellationSignalCreateTransport = CancellationSignal.createTransport();
            try {
                iLayoutResultCallback.onLayoutStarted(iCancellationSignalCreateTransport, i);
                synchronized (this.mLock) {
                    if (isDestroyedLocked()) {
                        return;
                    }
                    CancellationSignal cancellationSignalFromTransport = CancellationSignal.fromTransport(iCancellationSignalCreateTransport);
                    SomeArgs someArgsObtain = SomeArgs.obtain();
                    someArgsObtain.arg1 = this.mDocumentAdapter;
                    someArgsObtain.arg2 = printAttributes;
                    someArgsObtain.arg3 = printAttributes2;
                    someArgsObtain.arg4 = cancellationSignalFromTransport;
                    someArgsObtain.arg5 = new MyLayoutResultCallback(iLayoutResultCallback, i);
                    someArgsObtain.arg6 = bundle;
                    this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
                }
            } catch (RemoteException e) {
                Log.e(PrintManager.LOG_TAG, "Error notifying for layout start", e);
            }
        }

        @Override
        public void write(PageRange[] pageRangeArr, ParcelFileDescriptor parcelFileDescriptor, IWriteResultCallback iWriteResultCallback, int i) {
            ICancellationSignal iCancellationSignalCreateTransport = CancellationSignal.createTransport();
            try {
                iWriteResultCallback.onWriteStarted(iCancellationSignalCreateTransport, i);
                synchronized (this.mLock) {
                    if (isDestroyedLocked()) {
                        return;
                    }
                    CancellationSignal cancellationSignalFromTransport = CancellationSignal.fromTransport(iCancellationSignalCreateTransport);
                    SomeArgs someArgsObtain = SomeArgs.obtain();
                    someArgsObtain.arg1 = this.mDocumentAdapter;
                    someArgsObtain.arg2 = pageRangeArr;
                    someArgsObtain.arg3 = parcelFileDescriptor;
                    someArgsObtain.arg4 = cancellationSignalFromTransport;
                    someArgsObtain.arg5 = new MyWriteResultCallback(iWriteResultCallback, parcelFileDescriptor, i);
                    this.mHandler.obtainMessage(3, someArgsObtain).sendToTarget();
                }
            } catch (RemoteException e) {
                Log.e(PrintManager.LOG_TAG, "Error notifying for write start", e);
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
        public void kill(String str) {
            synchronized (this.mLock) {
                if (!isDestroyedLocked()) {
                    this.mHandler.obtainMessage(5, str).sendToTarget();
                }
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
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
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            IPrintDocumentAdapterObserver iPrintDocumentAdapterObserver;
            synchronized (this.mLock) {
                if (activity == this.mActivity) {
                    iPrintDocumentAdapterObserver = this.mObserver;
                    destroyLocked();
                } else {
                    iPrintDocumentAdapterObserver = null;
                }
            }
            if (iPrintDocumentAdapterObserver != null) {
                try {
                    iPrintDocumentAdapterObserver.onDestroy();
                } catch (RemoteException e) {
                    Log.e(PrintManager.LOG_TAG, "Error announcing destroyed state", e);
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
                        SomeArgs someArgs = (SomeArgs) message.obj;
                        PrintDocumentAdapter printDocumentAdapter = (PrintDocumentAdapter) someArgs.arg1;
                        PrintAttributes printAttributes = (PrintAttributes) someArgs.arg2;
                        PrintAttributes printAttributes2 = (PrintAttributes) someArgs.arg3;
                        CancellationSignal cancellationSignal = (CancellationSignal) someArgs.arg4;
                        PrintDocumentAdapter.LayoutResultCallback layoutResultCallback = (PrintDocumentAdapter.LayoutResultCallback) someArgs.arg5;
                        Bundle bundle = (Bundle) someArgs.arg6;
                        someArgs.recycle();
                        printDocumentAdapter.onLayout(printAttributes, printAttributes2, cancellationSignal, layoutResultCallback, bundle);
                        return;
                    case 3:
                        SomeArgs someArgs2 = (SomeArgs) message.obj;
                        PrintDocumentAdapter printDocumentAdapter2 = (PrintDocumentAdapter) someArgs2.arg1;
                        PageRange[] pageRangeArr = (PageRange[]) someArgs2.arg2;
                        ParcelFileDescriptor parcelFileDescriptor = (ParcelFileDescriptor) someArgs2.arg3;
                        CancellationSignal cancellationSignal2 = (CancellationSignal) someArgs2.arg4;
                        PrintDocumentAdapter.WriteResultCallback writeResultCallback = (PrintDocumentAdapter.WriteResultCallback) someArgs2.arg5;
                        someArgs2.recycle();
                        printDocumentAdapter2.onWrite(pageRangeArr, parcelFileDescriptor, cancellationSignal2, writeResultCallback);
                        return;
                    case 4:
                        ((PrintDocumentAdapter) message.obj).onFinish();
                        synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                            PrintDocumentAdapterDelegate.this.destroyLocked();
                            break;
                        }
                        return;
                    case 5:
                        throw new RuntimeException((String) message.obj);
                    default:
                        throw new IllegalArgumentException("Unknown message: " + message.what);
                }
            }
        }

        private final class MyLayoutResultCallback extends PrintDocumentAdapter.LayoutResultCallback implements DestroyableCallback {
            private ILayoutResultCallback mCallback;
            private final int mSequence;

            public MyLayoutResultCallback(ILayoutResultCallback iLayoutResultCallback, int i) {
                this.mCallback = iLayoutResultCallback;
                this.mSequence = i;
            }

            @Override
            public void onLayoutFinished(PrintDocumentInfo printDocumentInfo, boolean z) {
                ILayoutResultCallback iLayoutResultCallback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    iLayoutResultCallback = this.mCallback;
                }
                if (iLayoutResultCallback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    if (printDocumentInfo == null) {
                        throw new NullPointerException("document info cannot be null");
                    }
                    try {
                        iLayoutResultCallback.onLayoutFinished(printDocumentInfo, z, this.mSequence);
                    } catch (RemoteException e) {
                        Log.e(PrintManager.LOG_TAG, "Error calling onLayoutFinished", e);
                    }
                } finally {
                    destroy();
                }
            }

            @Override
            public void onLayoutFailed(CharSequence charSequence) {
                ILayoutResultCallback iLayoutResultCallback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    iLayoutResultCallback = this.mCallback;
                }
                if (iLayoutResultCallback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    try {
                        iLayoutResultCallback.onLayoutFailed(charSequence, this.mSequence);
                    } catch (RemoteException e) {
                        Log.e(PrintManager.LOG_TAG, "Error calling onLayoutFailed", e);
                    }
                } finally {
                    destroy();
                }
            }

            @Override
            public void onLayoutCancelled() {
                ILayoutResultCallback iLayoutResultCallback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    iLayoutResultCallback = this.mCallback;
                }
                if (iLayoutResultCallback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    try {
                        iLayoutResultCallback.onLayoutCanceled(this.mSequence);
                    } catch (RemoteException e) {
                        Log.e(PrintManager.LOG_TAG, "Error calling onLayoutFailed", e);
                    }
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

            public MyWriteResultCallback(IWriteResultCallback iWriteResultCallback, ParcelFileDescriptor parcelFileDescriptor, int i) {
                this.mFd = parcelFileDescriptor;
                this.mSequence = i;
                this.mCallback = iWriteResultCallback;
            }

            @Override
            public void onWriteFinished(PageRange[] pageRangeArr) {
                IWriteResultCallback iWriteResultCallback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    iWriteResultCallback = this.mCallback;
                }
                if (iWriteResultCallback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    if (pageRangeArr == null) {
                        throw new IllegalArgumentException("pages cannot be null");
                    }
                    if (pageRangeArr.length == 0) {
                        throw new IllegalArgumentException("pages cannot be empty");
                    }
                    try {
                        iWriteResultCallback.onWriteFinished(pageRangeArr, this.mSequence);
                    } catch (RemoteException e) {
                        Log.e(PrintManager.LOG_TAG, "Error calling onWriteFinished", e);
                    }
                } finally {
                    destroy();
                }
            }

            @Override
            public void onWriteFailed(CharSequence charSequence) {
                IWriteResultCallback iWriteResultCallback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    iWriteResultCallback = this.mCallback;
                }
                if (iWriteResultCallback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    try {
                        iWriteResultCallback.onWriteFailed(charSequence, this.mSequence);
                    } catch (RemoteException e) {
                        Log.e(PrintManager.LOG_TAG, "Error calling onWriteFailed", e);
                    }
                } finally {
                    destroy();
                }
            }

            @Override
            public void onWriteCancelled() {
                IWriteResultCallback iWriteResultCallback;
                synchronized (PrintDocumentAdapterDelegate.this.mLock) {
                    iWriteResultCallback = this.mCallback;
                }
                if (iWriteResultCallback == null) {
                    Log.e(PrintManager.LOG_TAG, "PrintDocumentAdapter is destroyed. Did you finish the printing activity before print completion or did you invoke a callback after finish?");
                    return;
                }
                try {
                    try {
                        iWriteResultCallback.onWriteCanceled(this.mSequence);
                    } catch (RemoteException e) {
                        Log.e(PrintManager.LOG_TAG, "Error calling onWriteCanceled", e);
                    }
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

    public static final class PrintJobStateChangeListenerWrapper extends IPrintJobStateChangeListener.Stub {
        private final WeakReference<Handler> mWeakHandler;
        private final WeakReference<PrintJobStateChangeListener> mWeakListener;

        public PrintJobStateChangeListenerWrapper(PrintJobStateChangeListener printJobStateChangeListener, Handler handler) {
            this.mWeakListener = new WeakReference<>(printJobStateChangeListener);
            this.mWeakHandler = new WeakReference<>(handler);
        }

        @Override
        public void onPrintJobStateChanged(PrintJobId printJobId) {
            Handler handler = this.mWeakHandler.get();
            PrintJobStateChangeListener printJobStateChangeListener = this.mWeakListener.get();
            if (handler != null && printJobStateChangeListener != null) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = this;
                someArgsObtain.arg2 = printJobId;
                handler.obtainMessage(1, someArgsObtain).sendToTarget();
            }
        }

        public void destroy() {
            this.mWeakListener.clear();
        }

        public PrintJobStateChangeListener getListener() {
            return this.mWeakListener.get();
        }
    }

    public static final class PrintServicesChangeListenerWrapper extends IPrintServicesChangeListener.Stub {
        private final WeakReference<Handler> mWeakHandler;
        private final WeakReference<PrintServicesChangeListener> mWeakListener;

        public PrintServicesChangeListenerWrapper(PrintServicesChangeListener printServicesChangeListener, Handler handler) {
            this.mWeakListener = new WeakReference<>(printServicesChangeListener);
            this.mWeakHandler = new WeakReference<>(handler);
        }

        @Override
        public void onPrintServicesChanged() {
            Handler handler = this.mWeakHandler.get();
            final PrintServicesChangeListener printServicesChangeListener = this.mWeakListener.get();
            if (handler != null && printServicesChangeListener != null) {
                Objects.requireNonNull(printServicesChangeListener);
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        printServicesChangeListener.onPrintServicesChanged();
                    }
                });
            }
        }

        public void destroy() {
            this.mWeakListener.clear();
        }
    }

    public static final class PrintServiceRecommendationsChangeListenerWrapper extends IRecommendationsChangeListener.Stub {
        private final WeakReference<Handler> mWeakHandler;
        private final WeakReference<PrintServiceRecommendationsChangeListener> mWeakListener;

        public PrintServiceRecommendationsChangeListenerWrapper(PrintServiceRecommendationsChangeListener printServiceRecommendationsChangeListener, Handler handler) {
            this.mWeakListener = new WeakReference<>(printServiceRecommendationsChangeListener);
            this.mWeakHandler = new WeakReference<>(handler);
        }

        @Override
        public void onRecommendationsChanged() {
            Handler handler = this.mWeakHandler.get();
            final PrintServiceRecommendationsChangeListener printServiceRecommendationsChangeListener = this.mWeakListener.get();
            if (handler != null && printServiceRecommendationsChangeListener != null) {
                Objects.requireNonNull(printServiceRecommendationsChangeListener);
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        printServiceRecommendationsChangeListener.onPrintServiceRecommendationsChanged();
                    }
                });
            }
        }

        public void destroy() {
            this.mWeakListener.clear();
        }
    }
}
