package com.android.server.display;

import android.R;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.OverlayDisplayWindow;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OverlayDisplayAdapter extends DisplayAdapter {
    static final boolean DEBUG = false;
    private static final int MAX_HEIGHT = 4096;
    private static final int MAX_WIDTH = 4096;
    private static final int MIN_HEIGHT = 100;
    private static final int MIN_WIDTH = 100;
    static final String TAG = "OverlayDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "overlay:";
    private String mCurrentOverlaySetting;
    private final ArrayList<OverlayDisplayHandle> mOverlays;
    private final Handler mUiHandler;
    private static final Pattern DISPLAY_PATTERN = Pattern.compile("([^,]+)(,[a-z]+)*");
    private static final Pattern MODE_PATTERN = Pattern.compile("(\\d+)x(\\d+)/(\\d+)");

    public OverlayDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, Handler handler2) {
        super(syncRoot, context, handler, listener, TAG);
        this.mOverlays = new ArrayList<>();
        this.mCurrentOverlaySetting = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        this.mUiHandler = handler2;
    }

    @Override
    public void dumpLocked(PrintWriter printWriter) {
        super.dumpLocked(printWriter);
        printWriter.println("mCurrentOverlaySetting=" + this.mCurrentOverlaySetting);
        printWriter.println("mOverlays: size=" + this.mOverlays.size());
        Iterator<OverlayDisplayHandle> it = this.mOverlays.iterator();
        while (it.hasNext()) {
            it.next().dumpLocked(printWriter);
        }
    }

    @Override
    public void registerLocked() {
        super.registerLocked();
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                OverlayDisplayAdapter.this.getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor("overlay_display_devices"), true, new ContentObserver(OverlayDisplayAdapter.this.getHandler()) {
                    @Override
                    public void onChange(boolean z) {
                        OverlayDisplayAdapter.this.updateOverlayDisplayDevices();
                    }
                });
                OverlayDisplayAdapter.this.updateOverlayDisplayDevices();
            }
        });
    }

    private void updateOverlayDisplayDevices() {
        synchronized (getSyncRoot()) {
            updateOverlayDisplayDevicesLocked();
        }
    }

    private void updateOverlayDisplayDevicesLocked() {
        String string = Settings.Global.getString(getContext().getContentResolver(), "overlay_display_devices");
        if (string == null) {
            string = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String str = string;
        if (str.equals(this.mCurrentOverlaySetting)) {
            return;
        }
        this.mCurrentOverlaySetting = str;
        if (!this.mOverlays.isEmpty()) {
            Slog.i(TAG, "Dismissing all overlay display devices.");
            Iterator<OverlayDisplayHandle> it = this.mOverlays.iterator();
            while (it.hasNext()) {
                it.next().dismissLocked();
            }
            this.mOverlays.clear();
        }
        int i = 0;
        for (String str2 : str.split(";")) {
            Matcher matcher = DISPLAY_PATTERN.matcher(str2);
            if (!matcher.matches()) {
                Slog.w(TAG, "Malformed overlay display devices setting: " + str);
            } else {
                if (i >= 4) {
                    Slog.w(TAG, "Too many overlay display devices specified: " + str);
                    return;
                }
                int i2 = 1;
                String strGroup = matcher.group(1);
                int i3 = 2;
                String strGroup2 = matcher.group(2);
                ArrayList arrayList = new ArrayList();
                String[] strArrSplit = strGroup.split("\\|");
                int length = strArrSplit.length;
                int i4 = 0;
                while (i4 < length) {
                    String str3 = strArrSplit[i4];
                    Matcher matcher2 = MODE_PATTERN.matcher(str3);
                    if (matcher2.matches()) {
                        try {
                            int i5 = Integer.parseInt(matcher2.group(i2), 10);
                            int i6 = Integer.parseInt(matcher2.group(i3), 10);
                            int i7 = Integer.parseInt(matcher2.group(3), 10);
                            if (i5 >= 100 && i5 <= 4096 && i6 >= 100 && i6 <= 4096 && i7 >= 120 && i7 <= 640) {
                                arrayList.add(new OverlayMode(i5, i6, i7));
                            } else {
                                Slog.w(TAG, "Ignoring out-of-range overlay display mode: " + str3);
                            }
                        } catch (NumberFormatException e) {
                        }
                    } else if (str3.isEmpty()) {
                    }
                    i4++;
                    i2 = 1;
                    i3 = 2;
                }
                if (!arrayList.isEmpty()) {
                    int i8 = i + 1;
                    String string2 = getContext().getResources().getString(R.string.bugreport_option_full_summary, Integer.valueOf(i8));
                    int iChooseOverlayGravity = chooseOverlayGravity(i8);
                    boolean z = strGroup2 != null && strGroup2.contains(",secure");
                    Slog.i(TAG, "Showing overlay display device #" + i8 + ": name=" + string2 + ", modes=" + Arrays.toString(arrayList.toArray()));
                    this.mOverlays.add(new OverlayDisplayHandle(string2, arrayList, iChooseOverlayGravity, z, i8));
                    i = i8;
                }
            }
        }
    }

    private static int chooseOverlayGravity(int i) {
        switch (i) {
            case 1:
                return 51;
            case 2:
                return 85;
            case 3:
                return 53;
            default:
                return 83;
        }
    }

    private abstract class OverlayDisplayDevice extends DisplayDevice {
        private int mActiveMode;
        private final int mDefaultMode;
        private final long mDisplayPresentationDeadlineNanos;
        private DisplayDeviceInfo mInfo;
        private final Display.Mode[] mModes;
        private final String mName;
        private final List<OverlayMode> mRawModes;
        private final float mRefreshRate;
        private final boolean mSecure;
        private int mState;
        private Surface mSurface;
        private SurfaceTexture mSurfaceTexture;

        public abstract void onModeChangedLocked(int i);

        public OverlayDisplayDevice(IBinder iBinder, String str, List<OverlayMode> list, int i, int i2, float f, long j, boolean z, int i3, SurfaceTexture surfaceTexture, int i4) {
            super(OverlayDisplayAdapter.this, iBinder, OverlayDisplayAdapter.UNIQUE_ID_PREFIX + i4);
            this.mName = str;
            this.mRefreshRate = f;
            this.mDisplayPresentationDeadlineNanos = j;
            this.mSecure = z;
            this.mState = i3;
            this.mSurfaceTexture = surfaceTexture;
            this.mRawModes = list;
            this.mModes = new Display.Mode[list.size()];
            for (int i5 = 0; i5 < list.size(); i5++) {
                OverlayMode overlayMode = list.get(i5);
                this.mModes[i5] = DisplayAdapter.createMode(overlayMode.mWidth, overlayMode.mHeight, f);
            }
            this.mActiveMode = i;
            this.mDefaultMode = i2;
        }

        public void destroyLocked() {
            this.mSurfaceTexture = null;
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        @Override
        public boolean hasStableUniqueId() {
            return false;
        }

        @Override
        public void performTraversalLocked(SurfaceControl.Transaction transaction) {
            if (this.mSurfaceTexture != null) {
                if (this.mSurface == null) {
                    this.mSurface = new Surface(this.mSurfaceTexture);
                }
                setSurfaceLocked(transaction, this.mSurface);
            }
        }

        public void setStateLocked(int i) {
            this.mState = i;
            this.mInfo = null;
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                Display.Mode mode = this.mModes[this.mActiveMode];
                OverlayMode overlayMode = this.mRawModes.get(this.mActiveMode);
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = mode.getPhysicalWidth();
                this.mInfo.height = mode.getPhysicalHeight();
                this.mInfo.modeId = mode.getModeId();
                this.mInfo.defaultModeId = this.mModes[0].getModeId();
                this.mInfo.supportedModes = this.mModes;
                this.mInfo.densityDpi = overlayMode.mDensityDpi;
                this.mInfo.xDpi = overlayMode.mDensityDpi;
                this.mInfo.yDpi = overlayMode.mDensityDpi;
                this.mInfo.presentationDeadlineNanos = this.mDisplayPresentationDeadlineNanos + (1000000000 / ((long) ((int) this.mRefreshRate)));
                this.mInfo.flags = 64;
                if (this.mSecure) {
                    this.mInfo.flags |= 4;
                }
                this.mInfo.type = 4;
                this.mInfo.touch = 0;
                this.mInfo.state = this.mState;
            }
            return this.mInfo;
        }

        @Override
        public void requestDisplayModesLocked(int i, int i2) {
            int i3 = 0;
            if (i2 != 0) {
                while (true) {
                    if (i3 < this.mModes.length) {
                        if (this.mModes[i3].getModeId() == i2) {
                            break;
                        } else {
                            i3++;
                        }
                    } else {
                        i3 = -1;
                        break;
                    }
                }
            }
            if (i3 == -1) {
                Slog.w(OverlayDisplayAdapter.TAG, "Unable to locate mode " + i2 + ", reverting to default.");
                i3 = this.mDefaultMode;
            }
            if (this.mActiveMode == i3) {
                return;
            }
            this.mActiveMode = i3;
            this.mInfo = null;
            OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
            onModeChangedLocked(i3);
        }
    }

    private final class OverlayDisplayHandle implements OverlayDisplayWindow.Listener {
        private static final int DEFAULT_MODE_INDEX = 0;
        private OverlayDisplayDevice mDevice;
        private final int mGravity;
        private final List<OverlayMode> mModes;
        private final String mName;
        private final int mNumber;
        private final boolean mSecure;
        private OverlayDisplayWindow mWindow;
        private final Runnable mShowRunnable = new Runnable() {
            @Override
            public void run() {
                OverlayMode overlayMode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                OverlayDisplayWindow overlayDisplayWindow = new OverlayDisplayWindow(OverlayDisplayAdapter.this.getContext(), OverlayDisplayHandle.this.mName, overlayMode.mWidth, overlayMode.mHeight, overlayMode.mDensityDpi, OverlayDisplayHandle.this.mGravity, OverlayDisplayHandle.this.mSecure, OverlayDisplayHandle.this);
                overlayDisplayWindow.show();
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    OverlayDisplayHandle.this.mWindow = overlayDisplayWindow;
                }
            }
        };
        private final Runnable mDismissRunnable = new Runnable() {
            @Override
            public void run() {
                OverlayDisplayWindow overlayDisplayWindow;
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    overlayDisplayWindow = OverlayDisplayHandle.this.mWindow;
                    OverlayDisplayHandle.this.mWindow = null;
                }
                if (overlayDisplayWindow != null) {
                    overlayDisplayWindow.dismiss();
                }
            }
        };
        private final Runnable mResizeRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    if (OverlayDisplayHandle.this.mWindow == null) {
                        return;
                    }
                    OverlayMode overlayMode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                    OverlayDisplayHandle.this.mWindow.resize(overlayMode.mWidth, overlayMode.mHeight, overlayMode.mDensityDpi);
                }
            }
        };
        private int mActiveMode = 0;

        public OverlayDisplayHandle(String str, List<OverlayMode> list, int i, boolean z, int i2) {
            this.mName = str;
            this.mModes = list;
            this.mGravity = i;
            this.mSecure = z;
            this.mNumber = i2;
            showLocked();
        }

        private void showLocked() {
            OverlayDisplayAdapter.this.mUiHandler.post(this.mShowRunnable);
        }

        public void dismissLocked() {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mShowRunnable);
            OverlayDisplayAdapter.this.mUiHandler.post(this.mDismissRunnable);
        }

        private void onActiveModeChangedLocked(int i) {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mResizeRunnable);
            this.mActiveMode = i;
            if (this.mWindow != null) {
                OverlayDisplayAdapter.this.mUiHandler.post(this.mResizeRunnable);
            }
        }

        @Override
        public void onWindowCreated(SurfaceTexture surfaceTexture, float f, long j, int i) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                this.mDevice = new OverlayDisplayDevice(SurfaceControl.createDisplay(this.mName, this.mSecure), this.mName, this.mModes, this.mActiveMode, 0, f, j, this.mSecure, i, surfaceTexture, this.mNumber) {
                    {
                        OverlayDisplayAdapter overlayDisplayAdapter = OverlayDisplayAdapter.this;
                    }

                    @Override
                    public void onModeChangedLocked(int i2) {
                        OverlayDisplayHandle.this.onActiveModeChangedLocked(i2);
                    }
                };
                OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 1);
            }
        }

        @Override
        public void onWindowDestroyed() {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                if (this.mDevice != null) {
                    this.mDevice.destroyLocked();
                    OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 3);
                }
            }
        }

        @Override
        public void onStateChanged(int i) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                if (this.mDevice != null) {
                    this.mDevice.setStateLocked(i);
                    OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 2);
                }
            }
        }

        public void dumpLocked(PrintWriter printWriter) {
            printWriter.println("  " + this.mName + ":");
            StringBuilder sb = new StringBuilder();
            sb.append("    mModes=");
            sb.append(Arrays.toString(this.mModes.toArray()));
            printWriter.println(sb.toString());
            printWriter.println("    mActiveMode=" + this.mActiveMode);
            printWriter.println("    mGravity=" + this.mGravity);
            printWriter.println("    mSecure=" + this.mSecure);
            printWriter.println("    mNumber=" + this.mNumber);
            if (this.mWindow != null) {
                IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "    ");
                indentingPrintWriter.increaseIndent();
                DumpUtils.dumpAsync(OverlayDisplayAdapter.this.mUiHandler, this.mWindow, indentingPrintWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 200L);
            }
        }
    }

    private static final class OverlayMode {
        final int mDensityDpi;
        final int mHeight;
        final int mWidth;

        OverlayMode(int i, int i2, int i3) {
            this.mWidth = i;
            this.mHeight = i2;
            this.mDensityDpi = i3;
        }

        public String toString() {
            return "{width=" + this.mWidth + ", height=" + this.mHeight + ", densityDpi=" + this.mDensityDpi + "}";
        }
    }
}
