package com.android.server.display;

import android.R;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.Curve;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.DisplayViewport;
import android.hardware.display.IDisplayManager;
import android.hardware.display.IDisplayManagerCallback;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.WifiDisplayStatus;
import android.hardware.input.InputManagerInternal;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.IntArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Spline;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.AnimationThread;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.display.DisplayAdapter;
import com.android.server.wm.SurfaceAnimationThread;
import com.android.server.wm.WindowManagerInternal;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.display.MtkDisplayManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DisplayManagerService extends SystemService {
    private static final boolean DEBUG = true;
    private static final String FORCE_WIFI_DISPLAY_ENABLE = "persist.debug.wfd.enable";
    private static final int MSG_DELIVER_DISPLAY_EVENT = 3;
    private static final int MSG_LOAD_BRIGHTNESS_CONFIGURATION = 7;
    private static final int MSG_REGISTER_ADDITIONAL_DISPLAY_ADAPTERS = 2;
    private static final int MSG_REGISTER_BRIGHTNESS_TRACKER = 6;
    private static final int MSG_REGISTER_DEFAULT_DISPLAY_ADAPTERS = 1;
    private static final int MSG_REQUEST_TRAVERSAL = 4;
    private static final int MSG_UPDATE_VIEWPORT = 5;
    private static final String TAG = "DisplayManagerService";
    private static final long WAIT_FOR_DEFAULT_DISPLAY_TIMEOUT = 10000;
    public final SparseArray<CallbackRecord> mCallbacks;
    private final Context mContext;
    private int mCurrentUserId;
    private final int mDefaultDisplayDefaultColorMode;
    private final DisplayViewport mDefaultViewport;
    private final SparseArray<IntArray> mDisplayAccessUIDs;
    private final DisplayAdapterListener mDisplayAdapterListener;
    private final ArrayList<DisplayAdapter> mDisplayAdapters;
    private final ArrayList<DisplayDevice> mDisplayDevices;
    private DisplayPowerController mDisplayPowerController;
    private final CopyOnWriteArrayList<DisplayManagerInternal.DisplayTransactionListener> mDisplayTransactionListeners;
    private final DisplayViewport mExternalTouchViewport;
    private int mGlobalDisplayBrightness;
    private int mGlobalDisplayState;
    private final DisplayManagerHandler mHandler;
    private final Injector mInjector;
    private InputManagerInternal mInputManagerInternal;
    private final SparseArray<LogicalDisplay> mLogicalDisplays;
    private final Curve mMinimumBrightnessCurve;
    private final Spline mMinimumBrightnessSpline;
    public MtkDisplayManagerService mMtkDisplayManagerService;
    private int mNextNonDefaultDisplayId;
    public boolean mOnlyCore;
    private boolean mPendingTraversal;
    private final PersistentDataStore mPersistentDataStore;
    private IMediaProjectionManager mProjectionService;
    public boolean mSafeMode;
    private final boolean mSingleDisplayDemoMode;
    private Point mStableDisplaySize;
    private final SyncRoot mSyncRoot;
    private final ArrayList<CallbackRecord> mTempCallbacks;
    private final DisplayViewport mTempDefaultViewport;
    private final DisplayInfo mTempDisplayInfo;
    private final ArrayList<Runnable> mTempDisplayStateWorkQueue;
    private final DisplayViewport mTempExternalTouchViewport;
    private final ArrayList<DisplayViewport> mTempVirtualTouchViewports;
    private final Handler mUiHandler;
    private VirtualDisplayAdapter mVirtualDisplayAdapter;
    private final ArrayList<DisplayViewport> mVirtualTouchViewports;
    private WifiDisplayAdapter mWifiDisplayAdapter;
    private int mWifiDisplayScanRequestCount;
    private WindowManagerInternal mWindowManagerInternal;

    public static final class SyncRoot {
    }

    public DisplayManagerService(Context context) {
        this(context, new Injector());
    }

    @VisibleForTesting
    DisplayManagerService(Context context, Injector injector) {
        super(context);
        this.mSyncRoot = new SyncRoot();
        this.mCallbacks = new SparseArray<>();
        this.mDisplayAdapters = new ArrayList<>();
        this.mDisplayDevices = new ArrayList<>();
        this.mLogicalDisplays = new SparseArray<>();
        this.mNextNonDefaultDisplayId = 1;
        this.mDisplayTransactionListeners = new CopyOnWriteArrayList<>();
        this.mGlobalDisplayState = 2;
        this.mGlobalDisplayBrightness = -1;
        this.mStableDisplaySize = new Point();
        this.mDefaultViewport = new DisplayViewport();
        this.mExternalTouchViewport = new DisplayViewport();
        this.mVirtualTouchViewports = new ArrayList<>();
        this.mPersistentDataStore = new PersistentDataStore();
        this.mTempCallbacks = new ArrayList<>();
        this.mTempDisplayInfo = new DisplayInfo();
        this.mTempDefaultViewport = new DisplayViewport();
        this.mTempExternalTouchViewport = new DisplayViewport();
        this.mTempVirtualTouchViewports = new ArrayList<>();
        this.mTempDisplayStateWorkQueue = new ArrayList<>();
        this.mDisplayAccessUIDs = new SparseArray<>();
        this.mMtkDisplayManagerService = MtkSystemServiceFactory.getInstance().makeMtkDisplayManagerService();
        this.mInjector = injector;
        this.mContext = context;
        this.mHandler = new DisplayManagerHandler(DisplayThread.get().getLooper());
        this.mUiHandler = UiThread.getHandler();
        this.mDisplayAdapterListener = new DisplayAdapterListener();
        this.mSingleDisplayDemoMode = SystemProperties.getBoolean("persist.demo.singledisplay", false);
        Resources resources = this.mContext.getResources();
        this.mDefaultDisplayDefaultColorMode = this.mContext.getResources().getInteger(R.integer.config_batteryHistoryStorageSize);
        float[] floatArray = getFloatArray(resources.obtainTypedArray(R.array.config_cdma_home_system));
        float[] floatArray2 = getFloatArray(resources.obtainTypedArray(R.array.config_cdma_international_roaming_indicators));
        this.mMinimumBrightnessCurve = new Curve(floatArray, floatArray2);
        this.mMinimumBrightnessSpline = Spline.createSpline(floatArray, floatArray2);
        this.mGlobalDisplayBrightness = ((PowerManager) this.mContext.getSystemService("power")).getDefaultScreenBrightnessSetting();
        this.mCurrentUserId = 0;
    }

    public void setupSchedulerPolicies() {
        Process.setThreadGroupAndCpuset(DisplayThread.get().getThreadId(), 5);
        Process.setThreadGroupAndCpuset(AnimationThread.get().getThreadId(), 5);
        Process.setThreadGroupAndCpuset(SurfaceAnimationThread.get().getThreadId(), 5);
    }

    @Override
    public void onStart() {
        synchronized (this.mSyncRoot) {
            this.mPersistentDataStore.loadIfNeeded();
            loadStableDisplayValuesLocked();
        }
        this.mHandler.sendEmptyMessage(1);
        publishBinderService("display", new BinderService(), true);
        publishLocalService(DisplayManagerInternal.class, new LocalService());
        publishLocalService(DisplayTransformManager.class, new DisplayTransformManager());
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 100) {
            synchronized (this.mSyncRoot) {
                long jUptimeMillis = SystemClock.uptimeMillis() + this.mInjector.getDefaultDisplayDelayTimeout();
                while (true) {
                    if (this.mLogicalDisplays.get(0) != null && this.mVirtualDisplayAdapter != null) {
                    }
                    long jUptimeMillis2 = jUptimeMillis - SystemClock.uptimeMillis();
                    if (jUptimeMillis2 <= 0) {
                        throw new RuntimeException("Timeout waiting for default display to be initialized. DefaultDisplay=" + this.mLogicalDisplays.get(0) + ", mVirtualDisplayAdapter=" + this.mVirtualDisplayAdapter);
                    }
                    Slog.d(TAG, "waitForDefaultDisplay: waiting, timeout=" + jUptimeMillis2);
                    try {
                        this.mSyncRoot.wait(jUptimeMillis2);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    @Override
    public void onSwitchUser(int i) {
        int userSerialNumber = getUserManager().getUserSerialNumber(i);
        synchronized (this.mSyncRoot) {
            if (this.mCurrentUserId != i) {
                this.mCurrentUserId = i;
                this.mDisplayPowerController.setBrightnessConfiguration(this.mPersistentDataStore.getBrightnessConfiguration(userSerialNumber));
            }
            this.mDisplayPowerController.onSwitchUser(i);
        }
    }

    public void windowManagerAndInputReady() {
        synchronized (this.mSyncRoot) {
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
            scheduleTraversalLocked(false);
        }
    }

    public void systemReady(boolean z, boolean z2) {
        synchronized (this.mSyncRoot) {
            this.mSafeMode = z;
            this.mOnlyCore = z2;
        }
        this.mHandler.sendEmptyMessage(2);
        this.mHandler.sendEmptyMessage(6);
    }

    @VisibleForTesting
    Handler getDisplayHandler() {
        return this.mHandler;
    }

    private void loadStableDisplayValuesLocked() {
        Point stableDisplaySize = this.mPersistentDataStore.getStableDisplaySize();
        if (stableDisplaySize.x > 0 && stableDisplaySize.y > 0) {
            this.mStableDisplaySize.set(stableDisplaySize.x, stableDisplaySize.y);
            return;
        }
        Resources resources = this.mContext.getResources();
        int integer = resources.getInteger(R.integer.config_dropboxLowPriorityBroadcastRateLimitPeriod);
        int integer2 = resources.getInteger(R.integer.config_dreamsBatteryLevelMinimumWhenPowered);
        if (integer > 0 && integer2 > 0) {
            setStableDisplaySizeLocked(integer, integer2);
        }
    }

    private Point getStableDisplaySizeInternal() {
        Point point = new Point();
        synchronized (this.mSyncRoot) {
            if (this.mStableDisplaySize.x > 0 && this.mStableDisplaySize.y > 0) {
                point.set(this.mStableDisplaySize.x, this.mStableDisplaySize.y);
            }
        }
        return point;
    }

    private void registerDisplayTransactionListenerInternal(DisplayManagerInternal.DisplayTransactionListener displayTransactionListener) {
        this.mDisplayTransactionListeners.add(displayTransactionListener);
    }

    private void unregisterDisplayTransactionListenerInternal(DisplayManagerInternal.DisplayTransactionListener displayTransactionListener) {
        this.mDisplayTransactionListeners.remove(displayTransactionListener);
    }

    private void setDisplayInfoOverrideFromWindowManagerInternal(int i, DisplayInfo displayInfo) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay logicalDisplay = this.mLogicalDisplays.get(i);
            this.mMtkDisplayManagerService.setDisplayInfoForFullscreenSwitch(displayInfo);
            if (logicalDisplay != null && logicalDisplay.setDisplayInfoOverrideFromWindowManagerLocked(displayInfo)) {
                sendDisplayEventLocked(i, 2);
                scheduleTraversalLocked(false);
            }
        }
    }

    private void getNonOverrideDisplayInfoInternal(int i, DisplayInfo displayInfo) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay logicalDisplay = this.mLogicalDisplays.get(i);
            if (logicalDisplay != null) {
                logicalDisplay.getNonOverrideDisplayInfoLocked(displayInfo);
            }
        }
    }

    @VisibleForTesting
    void performTraversalInternal(SurfaceControl.Transaction transaction) {
        synchronized (this.mSyncRoot) {
            if (this.mPendingTraversal) {
                this.mPendingTraversal = false;
                performTraversalLocked(transaction);
                Iterator<DisplayManagerInternal.DisplayTransactionListener> it = this.mDisplayTransactionListeners.iterator();
                while (it.hasNext()) {
                    it.next().onDisplayTransaction();
                }
            }
        }
    }

    private void requestGlobalDisplayStateInternal(int i, int i2) {
        if (i == 0) {
            i = 2;
        }
        if (i != 1) {
            if (i2 < 0) {
                i2 = -1;
            } else if (i2 > 255) {
                i2 = 255;
            }
        } else {
            i2 = 0;
        }
        synchronized (this.mTempDisplayStateWorkQueue) {
            try {
                synchronized (this.mSyncRoot) {
                    if (this.mGlobalDisplayState == i && this.mGlobalDisplayBrightness == i2) {
                        return;
                    }
                    Trace.traceBegin(131072L, "requestGlobalDisplayState(" + Display.stateToString(i) + ", brightness=" + i2 + ")");
                    this.mGlobalDisplayState = i;
                    this.mGlobalDisplayBrightness = i2;
                    applyGlobalDisplayStateLocked(this.mTempDisplayStateWorkQueue);
                    for (int i3 = 0; i3 < this.mTempDisplayStateWorkQueue.size(); i3++) {
                        this.mTempDisplayStateWorkQueue.get(i3).run();
                    }
                    Trace.traceEnd(131072L);
                }
            } finally {
                this.mTempDisplayStateWorkQueue.clear();
            }
        }
    }

    private DisplayInfo getDisplayInfoInternal(int i, int i2) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay logicalDisplay = this.mLogicalDisplays.get(i);
            if (logicalDisplay != null) {
                DisplayInfo displayInfoForFullscreenSwitch = this.mMtkDisplayManagerService.getDisplayInfoForFullscreenSwitch(logicalDisplay.getDisplayInfoLocked());
                if (displayInfoForFullscreenSwitch.hasAccess(i2) || isUidPresentOnDisplayInternal(i2, i)) {
                    return displayInfoForFullscreenSwitch;
                }
            }
            return null;
        }
    }

    private int[] getDisplayIdsInternal(int i) {
        int[] iArrCopyOfRange;
        synchronized (this.mSyncRoot) {
            int size = this.mLogicalDisplays.size();
            iArrCopyOfRange = new int[size];
            int i2 = 0;
            for (int i3 = 0; i3 < size; i3++) {
                if (this.mLogicalDisplays.valueAt(i3).getDisplayInfoLocked().hasAccess(i)) {
                    iArrCopyOfRange[i2] = this.mLogicalDisplays.keyAt(i3);
                    i2++;
                }
            }
            if (i2 != size) {
                iArrCopyOfRange = Arrays.copyOfRange(iArrCopyOfRange, 0, i2);
            }
        }
        return iArrCopyOfRange;
    }

    private void registerCallbackInternal(IDisplayManagerCallback iDisplayManagerCallback, int i) {
        synchronized (this.mSyncRoot) {
            if (this.mCallbacks.get(i) != null) {
                throw new SecurityException("The calling process has already registered an IDisplayManagerCallback.");
            }
            CallbackRecord callbackRecord = new CallbackRecord(i, iDisplayManagerCallback);
            try {
                iDisplayManagerCallback.asBinder().linkToDeath(callbackRecord, 0);
                this.mCallbacks.put(i, callbackRecord);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onCallbackDied(CallbackRecord callbackRecord) {
        synchronized (this.mSyncRoot) {
            this.mCallbacks.remove(callbackRecord.mPid);
            stopWifiDisplayScanLocked(callbackRecord);
        }
    }

    private void startWifiDisplayScanInternal(int i) {
        synchronized (this.mSyncRoot) {
            CallbackRecord callbackRecord = this.mCallbacks.get(i);
            if (callbackRecord == null) {
                throw new IllegalStateException("The calling process has not registered an IDisplayManagerCallback.");
            }
            startWifiDisplayScanLocked(callbackRecord);
        }
    }

    private void startWifiDisplayScanLocked(CallbackRecord callbackRecord) {
        if (!callbackRecord.mWifiDisplayScanRequested) {
            callbackRecord.mWifiDisplayScanRequested = true;
            int i = this.mWifiDisplayScanRequestCount;
            this.mWifiDisplayScanRequestCount = i + 1;
            if (i == 0 && this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestStartScanLocked();
            }
        }
    }

    private void stopWifiDisplayScanInternal(int i) {
        synchronized (this.mSyncRoot) {
            CallbackRecord callbackRecord = this.mCallbacks.get(i);
            if (callbackRecord == null) {
                throw new IllegalStateException("The calling process has not registered an IDisplayManagerCallback.");
            }
            stopWifiDisplayScanLocked(callbackRecord);
        }
    }

    private void stopWifiDisplayScanLocked(CallbackRecord callbackRecord) {
        if (callbackRecord.mWifiDisplayScanRequested) {
            callbackRecord.mWifiDisplayScanRequested = false;
            int i = this.mWifiDisplayScanRequestCount - 1;
            this.mWifiDisplayScanRequestCount = i;
            if (i == 0) {
                if (this.mWifiDisplayAdapter != null) {
                    this.mWifiDisplayAdapter.requestStopScanLocked();
                }
            } else if (this.mWifiDisplayScanRequestCount < 0) {
                Slog.wtf(TAG, "mWifiDisplayScanRequestCount became negative: " + this.mWifiDisplayScanRequestCount);
                this.mWifiDisplayScanRequestCount = 0;
            }
        }
    }

    private void connectWifiDisplayInternal(String str) {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestConnectLocked(str);
            }
        }
    }

    private void pauseWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestPauseLocked();
            }
        }
    }

    private void resumeWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestResumeLocked();
            }
        }
    }

    private void disconnectWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestDisconnectLocked();
            }
        }
    }

    private void renameWifiDisplayInternal(String str, String str2) {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestRenameLocked(str, str2);
            }
        }
    }

    private void forgetWifiDisplayInternal(String str) {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestForgetLocked(str);
            }
        }
    }

    private WifiDisplayStatus getWifiDisplayStatusInternal() {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                return this.mWifiDisplayAdapter.getWifiDisplayStatusLocked();
            }
            return new WifiDisplayStatus();
        }
    }

    private void requestColorModeInternal(int i, int i2) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay logicalDisplay = this.mLogicalDisplays.get(i);
            if (logicalDisplay != null && logicalDisplay.getRequestedColorModeLocked() != i2) {
                logicalDisplay.setRequestedColorModeLocked(i2);
                scheduleTraversalLocked(false);
            }
        }
    }

    private void setSaturationLevelInternal(float f) {
        if (f < 0.0f || f > 1.0f) {
            throw new IllegalArgumentException("Saturation level must be between 0 and 1");
        }
        ((DisplayTransformManager) LocalServices.getService(DisplayTransformManager.class)).setColorMatrix(DisplayTransformManager.LEVEL_COLOR_MATRIX_SATURATION, f == 1.0f ? null : computeSaturationMatrix(f));
    }

    private static float[] computeSaturationMatrix(float f) {
        float f2 = 1.0f - f;
        float[] fArr = {0.231f * f2, 0.715f * f2, 0.072f * f2};
        return new float[]{fArr[0] + f, fArr[0], fArr[0], 0.0f, fArr[1], fArr[1] + f, fArr[1], 0.0f, fArr[2], fArr[2], fArr[2] + f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    }

    private int createVirtualDisplayInternal(IVirtualDisplayCallback iVirtualDisplayCallback, IMediaProjection iMediaProjection, int i, String str, String str2, int i2, int i3, int i4, Surface surface, int i5, String str3) {
        synchronized (this.mSyncRoot) {
            if (this.mVirtualDisplayAdapter == null) {
                Slog.w(TAG, "Rejecting request to create private virtual display because the virtual display adapter is not available.");
                return -1;
            }
            DisplayDevice displayDeviceCreateVirtualDisplayLocked = this.mVirtualDisplayAdapter.createVirtualDisplayLocked(iVirtualDisplayCallback, iMediaProjection, i, str, str2, i2, i3, i4, surface, i5, str3);
            if (displayDeviceCreateVirtualDisplayLocked == null) {
                return -1;
            }
            handleDisplayDeviceAddedLocked(displayDeviceCreateVirtualDisplayLocked);
            LogicalDisplay logicalDisplayFindLogicalDisplayForDeviceLocked = findLogicalDisplayForDeviceLocked(displayDeviceCreateVirtualDisplayLocked);
            if (logicalDisplayFindLogicalDisplayForDeviceLocked != null) {
                return logicalDisplayFindLogicalDisplayForDeviceLocked.getDisplayIdLocked();
            }
            Slog.w(TAG, "Rejecting request to create virtual display because the logical display was not created.");
            this.mVirtualDisplayAdapter.releaseVirtualDisplayLocked(iVirtualDisplayCallback.asBinder());
            handleDisplayDeviceRemovedLocked(displayDeviceCreateVirtualDisplayLocked);
            return -1;
        }
    }

    private void resizeVirtualDisplayInternal(IBinder iBinder, int i, int i2, int i3) {
        synchronized (this.mSyncRoot) {
            if (this.mVirtualDisplayAdapter == null) {
                return;
            }
            this.mVirtualDisplayAdapter.resizeVirtualDisplayLocked(iBinder, i, i2, i3);
        }
    }

    private void setVirtualDisplaySurfaceInternal(IBinder iBinder, Surface surface) {
        synchronized (this.mSyncRoot) {
            if (this.mVirtualDisplayAdapter == null) {
                return;
            }
            this.mVirtualDisplayAdapter.setVirtualDisplaySurfaceLocked(iBinder, surface);
        }
    }

    private void releaseVirtualDisplayInternal(IBinder iBinder) {
        synchronized (this.mSyncRoot) {
            if (this.mVirtualDisplayAdapter == null) {
                return;
            }
            DisplayDevice displayDeviceReleaseVirtualDisplayLocked = this.mVirtualDisplayAdapter.releaseVirtualDisplayLocked(iBinder);
            if (displayDeviceReleaseVirtualDisplayLocked != null) {
                handleDisplayDeviceRemovedLocked(displayDeviceReleaseVirtualDisplayLocked);
            }
        }
    }

    private void registerDefaultDisplayAdapters() {
        synchronized (this.mSyncRoot) {
            registerDisplayAdapterLocked(new LocalDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener));
            this.mVirtualDisplayAdapter = this.mInjector.getVirtualDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener);
            if (this.mVirtualDisplayAdapter != null) {
                registerDisplayAdapterLocked(this.mVirtualDisplayAdapter);
            }
        }
    }

    private void registerAdditionalDisplayAdapters() {
        synchronized (this.mSyncRoot) {
            if (shouldRegisterNonEssentialDisplayAdaptersLocked()) {
                registerOverlayDisplayAdapterLocked();
                registerWifiDisplayAdapterLocked();
            }
        }
    }

    private void registerOverlayDisplayAdapterLocked() {
        registerDisplayAdapterLocked(new OverlayDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener, this.mUiHandler));
    }

    private void registerWifiDisplayAdapterLocked() {
        if (this.mContext.getResources().getBoolean(R.^attr-private.iconfactoryBadgeSize) || SystemProperties.getInt(FORCE_WIFI_DISPLAY_ENABLE, -1) == 1 || SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1")) {
            this.mWifiDisplayAdapter = new WifiDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener, this.mPersistentDataStore);
            registerDisplayAdapterLocked(this.mWifiDisplayAdapter);
        }
    }

    private void registerVirtualDisplayAdapterLocked() {
        this.mVirtualDisplayAdapter = new VirtualDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener);
        registerDisplayAdapterLocked(this.mVirtualDisplayAdapter);
    }

    private boolean shouldRegisterNonEssentialDisplayAdaptersLocked() {
        return (this.mSafeMode || this.mOnlyCore) ? false : true;
    }

    private void registerDisplayAdapterLocked(DisplayAdapter displayAdapter) {
        this.mDisplayAdapters.add(displayAdapter);
        displayAdapter.registerLocked();
    }

    private void handleDisplayDeviceAdded(DisplayDevice displayDevice) {
        synchronized (this.mSyncRoot) {
            handleDisplayDeviceAddedLocked(displayDevice);
        }
    }

    private void handleDisplayDeviceAddedLocked(DisplayDevice displayDevice) {
        DisplayDeviceInfo displayDeviceInfoLocked = displayDevice.getDisplayDeviceInfoLocked();
        if (this.mDisplayDevices.contains(displayDevice)) {
            Slog.w(TAG, "Attempted to add already added display device: " + displayDeviceInfoLocked);
            return;
        }
        Slog.i(TAG, "Display device added: " + displayDeviceInfoLocked);
        displayDevice.mDebugLastLoggedDeviceInfo = displayDeviceInfoLocked;
        this.mDisplayDevices.add(displayDevice);
        addLogicalDisplayLocked(displayDevice);
        Runnable runnableUpdateDisplayStateLocked = updateDisplayStateLocked(displayDevice);
        if (runnableUpdateDisplayStateLocked != null) {
            runnableUpdateDisplayStateLocked.run();
        }
        scheduleTraversalLocked(false);
    }

    private void handleDisplayDeviceChanged(DisplayDevice displayDevice) {
        synchronized (this.mSyncRoot) {
            DisplayDeviceInfo displayDeviceInfoLocked = displayDevice.getDisplayDeviceInfoLocked();
            if (!this.mDisplayDevices.contains(displayDevice)) {
                Slog.w(TAG, "Attempted to change non-existent display device: " + displayDeviceInfoLocked);
                return;
            }
            int iDiff = displayDevice.mDebugLastLoggedDeviceInfo.diff(displayDeviceInfoLocked);
            if (iDiff == 1) {
                Slog.i(TAG, "Display device changed state: \"" + displayDeviceInfoLocked.name + "\", " + Display.stateToString(displayDeviceInfoLocked.state));
            } else if (iDiff != 0) {
                Slog.i(TAG, "Display device changed: " + displayDeviceInfoLocked);
            }
            if ((iDiff & 4) != 0) {
                try {
                    this.mPersistentDataStore.setColorMode(displayDevice, displayDeviceInfoLocked.colorMode);
                    this.mPersistentDataStore.saveIfNeeded();
                } catch (Throwable th) {
                    this.mPersistentDataStore.saveIfNeeded();
                    throw th;
                }
            }
            displayDevice.mDebugLastLoggedDeviceInfo = displayDeviceInfoLocked;
            displayDevice.applyPendingDisplayDeviceInfoChangesLocked();
            if (updateLogicalDisplaysLocked()) {
                scheduleTraversalLocked(false);
            }
        }
    }

    private void handleDisplayDeviceRemoved(DisplayDevice displayDevice) {
        synchronized (this.mSyncRoot) {
            handleDisplayDeviceRemovedLocked(displayDevice);
        }
    }

    private void handleDisplayDeviceRemovedLocked(DisplayDevice displayDevice) {
        DisplayDeviceInfo displayDeviceInfoLocked = displayDevice.getDisplayDeviceInfoLocked();
        if (!this.mDisplayDevices.remove(displayDevice)) {
            Slog.w(TAG, "Attempted to remove non-existent display device: " + displayDeviceInfoLocked);
            return;
        }
        Slog.i(TAG, "Display device removed: " + displayDeviceInfoLocked);
        displayDevice.mDebugLastLoggedDeviceInfo = displayDeviceInfoLocked;
        updateLogicalDisplaysLocked();
        scheduleTraversalLocked(false);
    }

    private void applyGlobalDisplayStateLocked(List<Runnable> list) {
        int size = this.mDisplayDevices.size();
        for (int i = 0; i < size; i++) {
            Runnable runnableUpdateDisplayStateLocked = updateDisplayStateLocked(this.mDisplayDevices.get(i));
            if (runnableUpdateDisplayStateLocked != null) {
                list.add(runnableUpdateDisplayStateLocked);
            }
        }
    }

    private Runnable updateDisplayStateLocked(DisplayDevice displayDevice) {
        if ((displayDevice.getDisplayDeviceInfoLocked().flags & 32) == 0) {
            return displayDevice.requestDisplayStateLocked(this.mGlobalDisplayState, this.mGlobalDisplayBrightness);
        }
        return null;
    }

    private LogicalDisplay addLogicalDisplayLocked(DisplayDevice displayDevice) {
        DisplayDeviceInfo displayDeviceInfoLocked = displayDevice.getDisplayDeviceInfoLocked();
        boolean z = (displayDeviceInfoLocked.flags & 1) != 0;
        if (z && this.mLogicalDisplays.get(0) != null) {
            Slog.w(TAG, "Ignoring attempt to add a second default display: " + displayDeviceInfoLocked);
            z = false;
        }
        if (!z && this.mSingleDisplayDemoMode) {
            Slog.i(TAG, "Not creating a logical display for a secondary display  because single display demo mode is enabled: " + displayDeviceInfoLocked);
            return null;
        }
        int iAssignDisplayIdLocked = assignDisplayIdLocked(z);
        LogicalDisplay logicalDisplay = new LogicalDisplay(iAssignDisplayIdLocked, assignLayerStackLocked(iAssignDisplayIdLocked), displayDevice);
        logicalDisplay.updateLocked(this.mDisplayDevices);
        if (!logicalDisplay.isValidLocked()) {
            Slog.w(TAG, "Ignoring display device because the logical display created from it was not considered valid: " + displayDeviceInfoLocked);
            return null;
        }
        configureColorModeLocked(logicalDisplay, displayDevice);
        if (z) {
            recordStableDisplayStatsIfNeededLocked(logicalDisplay);
        }
        this.mLogicalDisplays.put(iAssignDisplayIdLocked, logicalDisplay);
        if (z) {
            this.mSyncRoot.notifyAll();
        }
        sendDisplayEventLocked(iAssignDisplayIdLocked, 1);
        return logicalDisplay;
    }

    private int assignDisplayIdLocked(boolean z) {
        if (z) {
            return 0;
        }
        int i = this.mNextNonDefaultDisplayId;
        this.mNextNonDefaultDisplayId = i + 1;
        return i;
    }

    private int assignLayerStackLocked(int i) {
        return i;
    }

    private void configureColorModeLocked(LogicalDisplay logicalDisplay, DisplayDevice displayDevice) {
        if (logicalDisplay.getPrimaryDisplayDeviceLocked() == displayDevice) {
            int colorMode = this.mPersistentDataStore.getColorMode(displayDevice);
            if (colorMode == -1) {
                if ((displayDevice.getDisplayDeviceInfoLocked().flags & 1) != 0) {
                    colorMode = this.mDefaultDisplayDefaultColorMode;
                } else {
                    colorMode = 0;
                }
            }
            logicalDisplay.setRequestedColorModeLocked(colorMode);
        }
    }

    private void recordStableDisplayStatsIfNeededLocked(LogicalDisplay logicalDisplay) {
        if (this.mStableDisplaySize.x <= 0 && this.mStableDisplaySize.y <= 0) {
            DisplayInfo displayInfoLocked = logicalDisplay.getDisplayInfoLocked();
            setStableDisplaySizeLocked(displayInfoLocked.getNaturalWidth(), displayInfoLocked.getNaturalHeight());
        }
    }

    private void setStableDisplaySizeLocked(int i, int i2) {
        this.mStableDisplaySize = new Point(i, i2);
        try {
            this.mPersistentDataStore.setStableDisplaySize(this.mStableDisplaySize);
        } finally {
            this.mPersistentDataStore.saveIfNeeded();
        }
    }

    @VisibleForTesting
    Curve getMinimumBrightnessCurveInternal() {
        return this.mMinimumBrightnessCurve;
    }

    private void setBrightnessConfigurationForUserInternal(BrightnessConfiguration brightnessConfiguration, int i, String str) {
        validateBrightnessConfiguration(brightnessConfiguration);
        int userSerialNumber = getUserManager().getUserSerialNumber(i);
        synchronized (this.mSyncRoot) {
            try {
                this.mPersistentDataStore.setBrightnessConfigurationForUser(brightnessConfiguration, userSerialNumber, str);
                this.mPersistentDataStore.saveIfNeeded();
                if (i == this.mCurrentUserId) {
                    this.mDisplayPowerController.setBrightnessConfiguration(brightnessConfiguration);
                }
            } catch (Throwable th) {
                this.mPersistentDataStore.saveIfNeeded();
                throw th;
            }
        }
    }

    @VisibleForTesting
    void validateBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration) {
        if (brightnessConfiguration != null && isBrightnessConfigurationTooDark(brightnessConfiguration)) {
            throw new IllegalArgumentException("brightness curve is too dark");
        }
    }

    private boolean isBrightnessConfigurationTooDark(BrightnessConfiguration brightnessConfiguration) {
        Pair curve = brightnessConfiguration.getCurve();
        float[] fArr = (float[]) curve.first;
        float[] fArr2 = (float[]) curve.second;
        for (int i = 0; i < fArr.length; i++) {
            if (fArr2[i] < this.mMinimumBrightnessSpline.interpolate(fArr[i])) {
                return true;
            }
        }
        return false;
    }

    private void loadBrightnessConfiguration() {
        synchronized (this.mSyncRoot) {
            this.mDisplayPowerController.setBrightnessConfiguration(this.mPersistentDataStore.getBrightnessConfiguration(getUserManager().getUserSerialNumber(this.mCurrentUserId)));
        }
    }

    private boolean updateLogicalDisplaysLocked() {
        int size = this.mLogicalDisplays.size();
        boolean z = false;
        while (true) {
            int i = size - 1;
            if (size > 0) {
                int iKeyAt = this.mLogicalDisplays.keyAt(i);
                LogicalDisplay logicalDisplayValueAt = this.mLogicalDisplays.valueAt(i);
                this.mTempDisplayInfo.copyFrom(logicalDisplayValueAt.getDisplayInfoLocked());
                logicalDisplayValueAt.updateLocked(this.mDisplayDevices);
                if (!logicalDisplayValueAt.isValidLocked()) {
                    this.mLogicalDisplays.removeAt(i);
                    sendDisplayEventLocked(iKeyAt, 3);
                } else if (this.mTempDisplayInfo.equals(logicalDisplayValueAt.getDisplayInfoLocked())) {
                    size = i;
                } else {
                    sendDisplayEventLocked(iKeyAt, 2);
                }
                z = true;
                size = i;
            } else {
                return z;
            }
        }
    }

    private void performTraversalLocked(SurfaceControl.Transaction transaction) {
        clearViewportsLocked();
        int size = this.mDisplayDevices.size();
        for (int i = 0; i < size; i++) {
            DisplayDevice displayDevice = this.mDisplayDevices.get(i);
            configureDisplayLocked(transaction, displayDevice);
            displayDevice.performTraversalLocked(transaction);
        }
        if (this.mInputManagerInternal != null) {
            this.mHandler.sendEmptyMessage(5);
        }
    }

    private void setDisplayPropertiesInternal(int i, boolean z, float f, int i2, boolean z2) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay logicalDisplay = this.mLogicalDisplays.get(i);
            if (logicalDisplay == null) {
                return;
            }
            if (logicalDisplay.hasContentLocked() != z) {
                Slog.d(TAG, "Display " + i + " hasContent flag changed: hasContent=" + z + ", inTraversal=" + z2);
                logicalDisplay.setHasContentLocked(z);
                scheduleTraversalLocked(z2);
            }
            if (i2 == 0 && f != 0.0f) {
                i2 = logicalDisplay.getDisplayInfoLocked().findDefaultModeByRefreshRate(f);
            }
            if (logicalDisplay.getRequestedModeIdLocked() != i2) {
                Slog.d(TAG, "Display " + i + " switching to mode " + i2);
                logicalDisplay.setRequestedModeIdLocked(i2);
                scheduleTraversalLocked(z2);
            }
        }
    }

    private void setDisplayOffsetsInternal(int i, int i2, int i3) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay logicalDisplay = this.mLogicalDisplays.get(i);
            if (logicalDisplay == null) {
                return;
            }
            if (logicalDisplay.getDisplayOffsetXLocked() != i2 || logicalDisplay.getDisplayOffsetYLocked() != i3) {
                Slog.d(TAG, "Display " + i + " burn-in offset set to (" + i2 + ", " + i3 + ")");
                logicalDisplay.setDisplayOffsetsLocked(i2, i3);
                scheduleTraversalLocked(false);
            }
        }
    }

    private void setDisplayAccessUIDsInternal(SparseArray<IntArray> sparseArray) {
        synchronized (this.mSyncRoot) {
            this.mDisplayAccessUIDs.clear();
            for (int size = sparseArray.size() - 1; size >= 0; size--) {
                this.mDisplayAccessUIDs.append(sparseArray.keyAt(size), sparseArray.valueAt(size));
            }
        }
    }

    private boolean isUidPresentOnDisplayInternal(int i, int i2) {
        boolean z;
        synchronized (this.mSyncRoot) {
            IntArray intArray = this.mDisplayAccessUIDs.get(i2);
            z = (intArray == null || intArray.indexOf(i) == -1) ? false : true;
        }
        return z;
    }

    private void clearViewportsLocked() {
        this.mDefaultViewport.valid = false;
        this.mExternalTouchViewport.valid = false;
        this.mVirtualTouchViewports.clear();
    }

    private void configureDisplayLocked(SurfaceControl.Transaction transaction, DisplayDevice displayDevice) {
        DisplayDeviceInfo displayDeviceInfoLocked = displayDevice.getDisplayDeviceInfoLocked();
        boolean z = (displayDeviceInfoLocked.flags & 128) != 0;
        LogicalDisplay logicalDisplayFindLogicalDisplayForDeviceLocked = findLogicalDisplayForDeviceLocked(displayDevice);
        if (!z) {
            if (logicalDisplayFindLogicalDisplayForDeviceLocked != null && !logicalDisplayFindLogicalDisplayForDeviceLocked.hasContentLocked()) {
                logicalDisplayFindLogicalDisplayForDeviceLocked = null;
            }
            if (logicalDisplayFindLogicalDisplayForDeviceLocked == null) {
                logicalDisplayFindLogicalDisplayForDeviceLocked = this.mLogicalDisplays.get(0);
            }
        }
        if (logicalDisplayFindLogicalDisplayForDeviceLocked == null) {
            Slog.w(TAG, "Missing logical display to use for physical display device: " + displayDevice.getDisplayDeviceInfoLocked());
            return;
        }
        logicalDisplayFindLogicalDisplayForDeviceLocked.configureDisplayLocked(transaction, displayDevice, displayDeviceInfoLocked.state == 1);
        if (!this.mDefaultViewport.valid && (displayDeviceInfoLocked.flags & 1) != 0) {
            setViewportLocked(this.mDefaultViewport, logicalDisplayFindLogicalDisplayForDeviceLocked, displayDevice);
        }
        if (!this.mExternalTouchViewport.valid && displayDeviceInfoLocked.touch == 2) {
            setViewportLocked(this.mExternalTouchViewport, logicalDisplayFindLogicalDisplayForDeviceLocked, displayDevice);
        }
        if (displayDeviceInfoLocked.touch == 3 && !TextUtils.isEmpty(displayDeviceInfoLocked.uniqueId)) {
            setViewportLocked(getVirtualTouchViewportLocked(displayDeviceInfoLocked.uniqueId), logicalDisplayFindLogicalDisplayForDeviceLocked, displayDevice);
        }
    }

    private DisplayViewport getVirtualTouchViewportLocked(String str) {
        int size = this.mVirtualTouchViewports.size();
        for (int i = 0; i < size; i++) {
            DisplayViewport displayViewport = this.mVirtualTouchViewports.get(i);
            if (str.equals(displayViewport.uniqueId)) {
                return displayViewport;
            }
        }
        DisplayViewport displayViewport2 = new DisplayViewport();
        displayViewport2.uniqueId = str;
        this.mVirtualTouchViewports.add(displayViewport2);
        return displayViewport2;
    }

    private static void setViewportLocked(DisplayViewport displayViewport, LogicalDisplay logicalDisplay, DisplayDevice displayDevice) {
        displayViewport.valid = true;
        displayViewport.displayId = logicalDisplay.getDisplayIdLocked();
        displayDevice.populateViewportLocked(displayViewport);
    }

    private LogicalDisplay findLogicalDisplayForDeviceLocked(DisplayDevice displayDevice) {
        int size = this.mLogicalDisplays.size();
        for (int i = 0; i < size; i++) {
            LogicalDisplay logicalDisplayValueAt = this.mLogicalDisplays.valueAt(i);
            if (logicalDisplayValueAt.getPrimaryDisplayDeviceLocked() == displayDevice) {
                return logicalDisplayValueAt;
            }
        }
        return null;
    }

    private void sendDisplayEventLocked(int i, int i2) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, i, i2));
    }

    private void scheduleTraversalLocked(boolean z) {
        if (!this.mPendingTraversal && this.mWindowManagerInternal != null) {
            this.mPendingTraversal = true;
            if (!z) {
                this.mHandler.sendEmptyMessage(4);
            }
        }
    }

    private void deliverDisplayEvent(int i, int i2) {
        int size;
        int i3;
        Slog.d(TAG, "Delivering display event: displayId=" + i + ", event=" + i2);
        synchronized (this.mSyncRoot) {
            size = this.mCallbacks.size();
            this.mTempCallbacks.clear();
            for (int i4 = 0; i4 < size; i4++) {
                this.mTempCallbacks.add(this.mCallbacks.valueAt(i4));
            }
        }
        for (i3 = 0; i3 < size; i3++) {
            this.mTempCallbacks.get(i3).notifyDisplayEventAsync(i, i2);
        }
        this.mTempCallbacks.clear();
    }

    private IMediaProjectionManager getProjectionService() {
        if (this.mProjectionService == null) {
            this.mProjectionService = IMediaProjectionManager.Stub.asInterface(ServiceManager.getService("media_projection"));
        }
        return this.mProjectionService;
    }

    private UserManager getUserManager() {
        return (UserManager) this.mContext.getSystemService(UserManager.class);
    }

    private void dumpInternal(PrintWriter printWriter) {
        printWriter.println("DISPLAY MANAGER (dumpsys display)");
        synchronized (this.mSyncRoot) {
            printWriter.println("  mOnlyCode=" + this.mOnlyCore);
            printWriter.println("  mSafeMode=" + this.mSafeMode);
            printWriter.println("  mPendingTraversal=" + this.mPendingTraversal);
            printWriter.println("  mGlobalDisplayState=" + Display.stateToString(this.mGlobalDisplayState));
            printWriter.println("  mNextNonDefaultDisplayId=" + this.mNextNonDefaultDisplayId);
            printWriter.println("  mDefaultViewport=" + this.mDefaultViewport);
            printWriter.println("  mExternalTouchViewport=" + this.mExternalTouchViewport);
            printWriter.println("  mVirtualTouchViewports=" + this.mVirtualTouchViewports);
            printWriter.println("  mDefaultDisplayDefaultColorMode=" + this.mDefaultDisplayDefaultColorMode);
            printWriter.println("  mSingleDisplayDemoMode=" + this.mSingleDisplayDemoMode);
            printWriter.println("  mWifiDisplayScanRequestCount=" + this.mWifiDisplayScanRequestCount);
            printWriter.println("  mStableDisplaySize=" + this.mStableDisplaySize);
            PrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "    ");
            indentingPrintWriter.increaseIndent();
            printWriter.println();
            printWriter.println("Display Adapters: size=" + this.mDisplayAdapters.size());
            for (DisplayAdapter displayAdapter : this.mDisplayAdapters) {
                printWriter.println("  " + displayAdapter.getName());
                displayAdapter.dumpLocked(indentingPrintWriter);
            }
            printWriter.println();
            printWriter.println("Display Devices: size=" + this.mDisplayDevices.size());
            for (DisplayDevice displayDevice : this.mDisplayDevices) {
                printWriter.println("  " + displayDevice.getDisplayDeviceInfoLocked());
                displayDevice.dumpLocked(indentingPrintWriter);
            }
            int size = this.mLogicalDisplays.size();
            printWriter.println();
            printWriter.println("Logical Displays: size=" + size);
            for (int i = 0; i < size; i++) {
                int iKeyAt = this.mLogicalDisplays.keyAt(i);
                LogicalDisplay logicalDisplayValueAt = this.mLogicalDisplays.valueAt(i);
                printWriter.println("  Display " + iKeyAt + ":");
                logicalDisplayValueAt.dumpLocked(indentingPrintWriter);
            }
            int size2 = this.mCallbacks.size();
            printWriter.println();
            printWriter.println("Callbacks: size=" + size2);
            for (int i2 = 0; i2 < size2; i2++) {
                CallbackRecord callbackRecordValueAt = this.mCallbacks.valueAt(i2);
                printWriter.println("  " + i2 + ": mPid=" + callbackRecordValueAt.mPid + ", mWifiDisplayScanRequested=" + callbackRecordValueAt.mWifiDisplayScanRequested);
            }
            if (this.mDisplayPowerController != null) {
                this.mDisplayPowerController.dump(printWriter);
            }
            printWriter.println();
            this.mPersistentDataStore.dump(printWriter);
        }
    }

    private boolean isSinkEnabledInternal() {
        boolean ifSinkEnabledLocked;
        if (!SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1")) {
            return false;
        }
        synchronized (this.mSyncRoot) {
            ifSinkEnabledLocked = this.mWifiDisplayAdapter != null ? this.mWifiDisplayAdapter.getIfSinkEnabledLocked() : false;
        }
        return ifSinkEnabledLocked;
    }

    private void enableSinkInternal(boolean z) {
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1")) {
            synchronized (this.mSyncRoot) {
                if (this.mWifiDisplayAdapter != null) {
                    this.mWifiDisplayAdapter.requestEnableSinkLocked(z);
                }
            }
        }
    }

    private void waitWifiDisplayConnectionInternal(Surface surface) {
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1")) {
            synchronized (this.mSyncRoot) {
                if (this.mWifiDisplayAdapter != null) {
                    this.mWifiDisplayAdapter.requestWaitConnectionLocked(surface);
                }
            }
        }
    }

    private void suspendWifiDisplayInternal(boolean z, Surface surface) {
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1")) {
            synchronized (this.mSyncRoot) {
                if (this.mWifiDisplayAdapter != null) {
                    this.mWifiDisplayAdapter.requestSuspendDisplayLocked(z, surface);
                }
            }
        }
    }

    private void sendUibcInputEventInternal(String str) {
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_uibc_support").equals("1")) {
            synchronized (this.mSyncRoot) {
                if (this.mWifiDisplayAdapter != null) {
                    this.mWifiDisplayAdapter.sendUibcInputEventLocked(str);
                }
            }
        }
    }

    private static float[] getFloatArray(TypedArray typedArray) {
        int length = typedArray.length();
        float[] fArr = new float[length];
        for (int i = 0; i < length; i++) {
            fArr[i] = typedArray.getFloat(i, Float.NaN);
        }
        typedArray.recycle();
        return fArr;
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        VirtualDisplayAdapter getVirtualDisplayAdapter(SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener) {
            return new VirtualDisplayAdapter(syncRoot, context, handler, listener);
        }

        long getDefaultDisplayDelayTimeout() {
            return 10000L;
        }
    }

    @VisibleForTesting
    DisplayDeviceInfo getDisplayDeviceInfoInternal(int i) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay logicalDisplay = this.mLogicalDisplays.get(i);
            if (logicalDisplay != null) {
                return logicalDisplay.getPrimaryDisplayDeviceLocked().getDisplayDeviceInfoLocked();
            }
            return null;
        }
    }

    private final class DisplayManagerHandler extends Handler {
        public DisplayManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 7) {
                switch (i) {
                    case 1:
                        DisplayManagerService.this.registerDefaultDisplayAdapters();
                        return;
                    case 2:
                        DisplayManagerService.this.registerAdditionalDisplayAdapters();
                        return;
                    case 3:
                        DisplayManagerService.this.deliverDisplayEvent(message.arg1, message.arg2);
                        return;
                    case 4:
                        DisplayManagerService.this.mWindowManagerInternal.requestTraversalFromDisplayManager();
                        return;
                    case 5:
                        synchronized (DisplayManagerService.this.mSyncRoot) {
                            DisplayManagerService.this.mTempDefaultViewport.copyFrom(DisplayManagerService.this.mDefaultViewport);
                            DisplayManagerService.this.mTempExternalTouchViewport.copyFrom(DisplayManagerService.this.mExternalTouchViewport);
                            if (!DisplayManagerService.this.mTempVirtualTouchViewports.equals(DisplayManagerService.this.mVirtualTouchViewports)) {
                                DisplayManagerService.this.mTempVirtualTouchViewports.clear();
                                Iterator it = DisplayManagerService.this.mVirtualTouchViewports.iterator();
                                while (it.hasNext()) {
                                    DisplayManagerService.this.mTempVirtualTouchViewports.add(((DisplayViewport) it.next()).makeCopy());
                                }
                            }
                            break;
                        }
                        DisplayManagerService.this.mInputManagerInternal.setDisplayViewports(DisplayManagerService.this.mTempDefaultViewport, DisplayManagerService.this.mTempExternalTouchViewport, DisplayManagerService.this.mTempVirtualTouchViewports);
                        return;
                    default:
                        return;
                }
            }
            DisplayManagerService.this.loadBrightnessConfiguration();
        }
    }

    private final class DisplayAdapterListener implements DisplayAdapter.Listener {
        private DisplayAdapterListener() {
        }

        @Override
        public void onDisplayDeviceEvent(DisplayDevice displayDevice, int i) {
            switch (i) {
                case 1:
                    DisplayManagerService.this.handleDisplayDeviceAdded(displayDevice);
                    break;
                case 2:
                    DisplayManagerService.this.handleDisplayDeviceChanged(displayDevice);
                    break;
                case 3:
                    DisplayManagerService.this.handleDisplayDeviceRemoved(displayDevice);
                    break;
            }
        }

        @Override
        public void onTraversalRequested() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayManagerService.this.scheduleTraversalLocked(false);
            }
        }
    }

    private final class CallbackRecord implements IBinder.DeathRecipient {
        private final IDisplayManagerCallback mCallback;
        public final int mPid;
        public boolean mWifiDisplayScanRequested;

        public CallbackRecord(int i, IDisplayManagerCallback iDisplayManagerCallback) {
            this.mPid = i;
            this.mCallback = iDisplayManagerCallback;
        }

        @Override
        public void binderDied() {
            Slog.d(DisplayManagerService.TAG, "Display listener for pid " + this.mPid + " died.");
            DisplayManagerService.this.onCallbackDied(this);
        }

        public void notifyDisplayEventAsync(int i, int i2) {
            try {
                this.mCallback.onDisplayEvent(i, i2);
            } catch (RemoteException e) {
                Slog.w(DisplayManagerService.TAG, "Failed to notify process " + this.mPid + " that displays changed, assuming it died.", e);
                binderDied();
            }
        }
    }

    @VisibleForTesting
    final class BinderService extends IDisplayManager.Stub {
        BinderService() {
        }

        public DisplayInfo getDisplayInfo(int i) {
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getDisplayInfoInternal(i, callingUid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int[] getDisplayIds() {
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getDisplayIdsInternal(callingUid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public Point getStableDisplaySize() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getStableDisplaySizeInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void registerCallback(IDisplayManagerCallback iDisplayManagerCallback) {
            if (iDisplayManagerCallback == null) {
                throw new IllegalArgumentException("listener must not be null");
            }
            int callingPid = Binder.getCallingPid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.registerCallbackInternal(iDisplayManagerCallback, callingPid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void startWifiDisplayScan() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to start wifi display scans");
            int callingPid = Binder.getCallingPid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.startWifiDisplayScanInternal(callingPid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void stopWifiDisplayScan() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to stop wifi display scans");
            int callingPid = Binder.getCallingPid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.stopWifiDisplayScanInternal(callingPid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void connectWifiDisplay(String str) {
            if (str != null) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to connect to a wifi display");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.connectWifiDisplayInternal(str);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("address must not be null");
        }

        public void disconnectWifiDisplay() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.disconnectWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void renameWifiDisplay(String str, String str2) {
            if (str != null) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to rename to a wifi display");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.renameWifiDisplayInternal(str, str2);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("address must not be null");
        }

        public void forgetWifiDisplay(String str) {
            if (str != null) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to forget to a wifi display");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.forgetWifiDisplayInternal(str);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("address must not be null");
        }

        public void pauseWifiDisplay() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to pause a wifi display session");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.pauseWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void resumeWifiDisplay() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to resume a wifi display session");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.resumeWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public WifiDisplayStatus getWifiDisplayStatus() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getWifiDisplayStatusInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void requestColorMode(int i, int i2) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_COLOR_MODE", "Permission required to change the display color mode");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.requestColorModeInternal(i, i2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setSaturationLevel(float f) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_SATURATION", "Permission required to set display saturation level");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setSaturationLevelInternal(f);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int createVirtualDisplay(IVirtualDisplayCallback iVirtualDisplayCallback, IMediaProjection iMediaProjection, String str, String str2, int i, int i2, int i3, Surface surface, int i4, String str3) {
            int iApplyVirtualDisplayFlags;
            int callingUid = Binder.getCallingUid();
            if (!validatePackageName(callingUid, str)) {
                throw new SecurityException("packageName must match the calling uid");
            }
            if (iVirtualDisplayCallback == null) {
                throw new IllegalArgumentException("appToken must not be null");
            }
            if (TextUtils.isEmpty(str2)) {
                throw new IllegalArgumentException("name must be non-null and non-empty");
            }
            if (i <= 0 || i2 <= 0 || i3 <= 0) {
                throw new IllegalArgumentException("width, height, and densityDpi must be greater than 0");
            }
            if (surface != null && surface.isSingleBuffered()) {
                throw new IllegalArgumentException("Surface can't be single-buffered");
            }
            if ((i4 & 1) != 0) {
                iApplyVirtualDisplayFlags = i4 | 16;
                if ((iApplyVirtualDisplayFlags & 32) != 0) {
                    throw new IllegalArgumentException("Public display must not be marked as SHOW_WHEN_LOCKED_INSECURE");
                }
            } else {
                iApplyVirtualDisplayFlags = i4;
            }
            if ((iApplyVirtualDisplayFlags & 8) != 0) {
                iApplyVirtualDisplayFlags &= -17;
            }
            if (iMediaProjection != null) {
                try {
                    if (!DisplayManagerService.this.getProjectionService().isValidMediaProjection(iMediaProjection)) {
                        throw new SecurityException("Invalid media projection");
                    }
                    iApplyVirtualDisplayFlags = iMediaProjection.applyVirtualDisplayFlags(iApplyVirtualDisplayFlags);
                } catch (RemoteException e) {
                    throw new SecurityException("unable to validate media projection or flags");
                }
            }
            int i5 = iApplyVirtualDisplayFlags;
            if (callingUid != 1000 && (i5 & 16) != 0 && !canProjectVideo(iMediaProjection)) {
                throw new SecurityException("Requires CAPTURE_VIDEO_OUTPUT or CAPTURE_SECURE_VIDEO_OUTPUT permission, or an appropriate MediaProjection token in order to create a screen sharing virtual display.");
            }
            if ((i5 & 4) != 0 && !canProjectSecureVideo(iMediaProjection)) {
                throw new SecurityException("Requires CAPTURE_SECURE_VIDEO_OUTPUT or an appropriate MediaProjection token to create a secure virtual display.");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.createVirtualDisplayInternal(iVirtualDisplayCallback, iMediaProjection, callingUid, str, str2, i, i2, i3, surface, i5, str3);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void resizeVirtualDisplay(IVirtualDisplayCallback iVirtualDisplayCallback, int i, int i2, int i3) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.resizeVirtualDisplayInternal(iVirtualDisplayCallback.asBinder(), i, i2, i3);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setVirtualDisplaySurface(IVirtualDisplayCallback iVirtualDisplayCallback, Surface surface) {
            if (surface != null && surface.isSingleBuffered()) {
                throw new IllegalArgumentException("Surface can't be single-buffered");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setVirtualDisplaySurfaceInternal(iVirtualDisplayCallback.asBinder(), surface);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void releaseVirtualDisplay(IVirtualDisplayCallback iVirtualDisplayCallback) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.releaseVirtualDisplayInternal(iVirtualDisplayCallback.asBinder());
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(DisplayManagerService.this.mContext, DisplayManagerService.TAG, printWriter)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.dumpInternal(printWriter);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public ParceledListSlice<BrightnessChangeEvent> getBrightnessEvents(String str) {
            ParceledListSlice<BrightnessChangeEvent> brightnessEvents;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.BRIGHTNESS_SLIDER_USAGE", "Permission to read brightness events.");
            int callingUid = Binder.getCallingUid();
            int iNoteOp = ((AppOpsManager) DisplayManagerService.this.mContext.getSystemService(AppOpsManager.class)).noteOp(43, callingUid, str);
            boolean z = false;
            if (iNoteOp != 3 ? iNoteOp == 0 : DisplayManagerService.this.mContext.checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") == 0) {
                z = true;
            }
            int userId = UserHandle.getUserId(callingUid);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    brightnessEvents = DisplayManagerService.this.mDisplayPowerController.getBrightnessEvents(userId, z);
                }
                return brightnessEvents;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats() {
            ParceledListSlice<AmbientBrightnessDayStats> ambientBrightnessStats;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_LIGHT_STATS", "Permission required to to access ambient light stats.");
            int userId = UserHandle.getUserId(Binder.getCallingUid());
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    ambientBrightnessStats = DisplayManagerService.this.mDisplayPowerController.getAmbientBrightnessStats(userId);
                }
                return ambientBrightnessStats;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setBrightnessConfigurationForUser(BrightnessConfiguration brightnessConfiguration, int i, String str) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to change the display's brightness configuration");
            if (i != UserHandle.getCallingUserId()) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "Permission required to change the display brightness configuration of another user");
            }
            if (str != null && !validatePackageName(getCallingUid(), str)) {
                str = null;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setBrightnessConfigurationForUserInternal(brightnessConfiguration, i, str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public BrightnessConfiguration getBrightnessConfigurationForUser(int i) {
            BrightnessConfiguration brightnessConfiguration;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to read the display's brightness configuration");
            if (i != UserHandle.getCallingUserId()) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "Permission required to read the display brightness configuration of another user");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                int userSerialNumber = DisplayManagerService.this.getUserManager().getUserSerialNumber(i);
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    brightnessConfiguration = DisplayManagerService.this.mPersistentDataStore.getBrightnessConfiguration(userSerialNumber);
                    if (brightnessConfiguration == null) {
                        brightnessConfiguration = DisplayManagerService.this.mDisplayPowerController.getDefaultBrightnessConfiguration();
                    }
                }
                return brightnessConfiguration;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public BrightnessConfiguration getDefaultBrightnessConfiguration() {
            BrightnessConfiguration defaultBrightnessConfiguration;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to read the display's default brightness configuration");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    defaultBrightnessConfiguration = DisplayManagerService.this.mDisplayPowerController.getDefaultBrightnessConfiguration();
                }
                return defaultBrightnessConfiguration;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setTemporaryBrightness(int i) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to set the display's brightness");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    DisplayManagerService.this.mDisplayPowerController.setTemporaryBrightness(i);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setTemporaryAutoBrightnessAdjustment(float f) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to set the display's auto brightness adjustment");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    DisplayManagerService.this.mDisplayPowerController.setTemporaryAutoBrightnessAdjustment(f);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                new DisplayManagerShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public Curve getMinimumBrightnessCurve() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getMinimumBrightnessCurveInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        void setBrightness(int i) {
            Settings.System.putIntForUser(DisplayManagerService.this.mContext.getContentResolver(), "screen_brightness", i, -2);
        }

        void resetBrightnessConfiguration() {
            DisplayManagerService.this.setBrightnessConfigurationForUserInternal(null, DisplayManagerService.this.mContext.getUserId(), DisplayManagerService.this.mContext.getPackageName());
        }

        private boolean validatePackageName(int i, String str) {
            String[] packagesForUid;
            if (str != null && (packagesForUid = DisplayManagerService.this.mContext.getPackageManager().getPackagesForUid(i)) != null) {
                for (String str2 : packagesForUid) {
                    if (str2.equals(str)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean canProjectVideo(IMediaProjection iMediaProjection) {
            if (iMediaProjection != null) {
                try {
                    if (iMediaProjection.canProjectVideo()) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Slog.e(DisplayManagerService.TAG, "Unable to query projection service for permissions", e);
                }
            }
            if (DisplayManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_VIDEO_OUTPUT") == 0) {
                return true;
            }
            return canProjectSecureVideo(iMediaProjection);
        }

        private boolean canProjectSecureVideo(IMediaProjection iMediaProjection) {
            if (iMediaProjection != null) {
                try {
                    if (iMediaProjection.canProjectSecureVideo()) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Slog.e(DisplayManagerService.TAG, "Unable to query projection service for permissions", e);
                }
            }
            return DisplayManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_SECURE_VIDEO_OUTPUT") == 0;
        }

        public boolean isSinkEnabled() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.isSinkEnabledInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void enableSink(boolean z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.enableSinkInternal(z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void waitWifiDisplayConnection(Surface surface) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.waitWifiDisplayConnectionInternal(surface);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void suspendWifiDisplay(boolean z, Surface surface) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.suspendWifiDisplayInternal(z, surface);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void sendUibcInputEvent(String str) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.sendUibcInputEventInternal(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private final class LocalService extends DisplayManagerInternal {
        private LocalService() {
        }

        public void initPowerManagement(final DisplayManagerInternal.DisplayPowerCallbacks displayPowerCallbacks, Handler handler, SensorManager sensorManager) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayBlanker displayBlanker = new DisplayBlanker() {
                    @Override
                    public void requestDisplayState(int i, int i2) {
                        if (i == 1) {
                            DisplayManagerService.this.requestGlobalDisplayStateInternal(i, i2);
                        }
                        displayPowerCallbacks.onDisplayStateChange(i);
                        if (i != 1) {
                            DisplayManagerService.this.requestGlobalDisplayStateInternal(i, i2);
                        }
                    }
                };
                DisplayManagerService.this.mDisplayPowerController = new DisplayPowerController(DisplayManagerService.this.mContext, displayPowerCallbacks, handler, sensorManager, displayBlanker);
            }
            DisplayManagerService.this.mHandler.sendEmptyMessage(7);
        }

        public boolean requestPowerState(DisplayManagerInternal.DisplayPowerRequest displayPowerRequest, boolean z) {
            boolean zRequestPowerState;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                zRequestPowerState = DisplayManagerService.this.mDisplayPowerController.requestPowerState(displayPowerRequest, z);
            }
            return zRequestPowerState;
        }

        public boolean isProximitySensorAvailable() {
            boolean zIsProximitySensorAvailable;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                zIsProximitySensorAvailable = DisplayManagerService.this.mDisplayPowerController.isProximitySensorAvailable();
            }
            return zIsProximitySensorAvailable;
        }

        public DisplayInfo getDisplayInfo(int i) {
            return DisplayManagerService.this.getDisplayInfoInternal(i, Process.myUid());
        }

        public void registerDisplayTransactionListener(DisplayManagerInternal.DisplayTransactionListener displayTransactionListener) {
            if (displayTransactionListener != null) {
                DisplayManagerService.this.registerDisplayTransactionListenerInternal(displayTransactionListener);
                return;
            }
            throw new IllegalArgumentException("listener must not be null");
        }

        public void unregisterDisplayTransactionListener(DisplayManagerInternal.DisplayTransactionListener displayTransactionListener) {
            if (displayTransactionListener != null) {
                DisplayManagerService.this.unregisterDisplayTransactionListenerInternal(displayTransactionListener);
                return;
            }
            throw new IllegalArgumentException("listener must not be null");
        }

        public void setDisplayInfoOverrideFromWindowManager(int i, DisplayInfo displayInfo) {
            DisplayManagerService.this.setDisplayInfoOverrideFromWindowManagerInternal(i, displayInfo);
        }

        public void getNonOverrideDisplayInfo(int i, DisplayInfo displayInfo) {
            DisplayManagerService.this.getNonOverrideDisplayInfoInternal(i, displayInfo);
        }

        public void performTraversal(SurfaceControl.Transaction transaction) {
            DisplayManagerService.this.performTraversalInternal(transaction);
        }

        public void setDisplayProperties(int i, boolean z, float f, int i2, boolean z2) {
            DisplayManagerService.this.setDisplayPropertiesInternal(i, z, f, i2, z2);
        }

        public void setDisplayOffsets(int i, int i2, int i3) {
            DisplayManagerService.this.setDisplayOffsetsInternal(i, i2, i3);
        }

        public void setDisplayAccessUIDs(SparseArray<IntArray> sparseArray) {
            DisplayManagerService.this.setDisplayAccessUIDsInternal(sparseArray);
        }

        public boolean isUidPresentOnDisplay(int i, int i2) {
            return DisplayManagerService.this.isUidPresentOnDisplayInternal(i, i2);
        }

        public void persistBrightnessTrackerState() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayManagerService.this.mDisplayPowerController.persistBrightnessTrackerState();
            }
        }

        public void onOverlayChanged() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                for (int i = 0; i < DisplayManagerService.this.mDisplayDevices.size(); i++) {
                    ((DisplayDevice) DisplayManagerService.this.mDisplayDevices.get(i)).onOverlayChangedLocked();
                }
            }
        }
    }
}
