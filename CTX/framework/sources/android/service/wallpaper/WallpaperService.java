package android.service.wallpaper;

import android.app.Service;
import android.app.WallpaperColors;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.SettingsStringUtil;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.util.Log;
import android.util.MergedConfiguration;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.IWindowSession;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.HandlerCaller;
import com.android.internal.view.BaseIWindow;
import com.android.internal.view.BaseSurfaceHolder;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Supplier;

public abstract class WallpaperService extends Service {
    static final boolean DEBUG = false;
    private static final int DO_ATTACH = 10;
    private static final int DO_DETACH = 20;
    private static final int DO_IN_AMBIENT_MODE = 50;
    private static final int DO_SET_DESIRED_SIZE = 30;
    private static final int DO_SET_DISPLAY_PADDING = 40;
    private static final int MSG_REQUEST_WALLPAPER_COLORS = 10050;
    private static final int MSG_TOUCH_EVENT = 10040;
    private static final int MSG_UPDATE_SURFACE = 10000;
    private static final int MSG_VISIBILITY_CHANGED = 10010;
    private static final int MSG_WALLPAPER_COMMAND = 10025;
    private static final int MSG_WALLPAPER_OFFSETS = 10020;
    private static final int MSG_WINDOW_MOVED = 10035;
    private static final int MSG_WINDOW_RESIZED = 10030;
    private static final int NOTIFY_COLORS_RATE_LIMIT_MS = 1000;
    public static final String SERVICE_INTERFACE = "android.service.wallpaper.WallpaperService";
    public static final String SERVICE_META_DATA = "android.service.wallpaper";
    static final String TAG = "WallpaperService";
    private final ArrayList<Engine> mActiveEngines = new ArrayList<>();

    public abstract Engine onCreateEngine();

    static final class WallpaperCommand {
        String action;
        Bundle extras;
        boolean sync;
        int x;
        int y;
        int z;

        WallpaperCommand() {
        }
    }

    public class Engine {
        final Rect mBackdropFrame;
        HandlerCaller mCaller;
        private final Supplier<Long> mClockFunction;
        IWallpaperConnection mConnection;
        final Rect mContentInsets;
        boolean mCreated;
        int mCurHeight;
        int mCurWidth;
        int mCurWindowFlags;
        int mCurWindowPrivateFlags;
        boolean mDestroyed;
        final Rect mDispatchedContentInsets;
        DisplayCutout mDispatchedDisplayCutout;
        final Rect mDispatchedOutsets;
        final Rect mDispatchedOverscanInsets;
        final Rect mDispatchedStableInsets;
        Display mDisplay;
        final DisplayCutout.ParcelableWrapper mDisplayCutout;
        private final DisplayManager.DisplayListener mDisplayListener;
        DisplayManager mDisplayManager;
        private int mDisplayState;
        boolean mDrawingAllowed;
        final Rect mFinalStableInsets;
        final Rect mFinalSystemInsets;
        boolean mFixedSizeAllowed;
        int mFormat;
        private final Handler mHandler;
        int mHeight;
        IWallpaperEngineWrapper mIWallpaperEngine;
        boolean mInitializing;
        InputChannel mInputChannel;
        WallpaperInputEventReceiver mInputEventReceiver;
        boolean mIsCreating;
        boolean mIsInAmbientMode;
        private long mLastColorInvalidation;
        final WindowManager.LayoutParams mLayout;
        final Object mLock;
        final MergedConfiguration mMergedConfiguration;
        private final Runnable mNotifyColorsChanged;
        boolean mOffsetMessageEnqueued;
        boolean mOffsetsChanged;
        final Rect mOutsets;
        final Rect mOverscanInsets;
        MotionEvent mPendingMove;
        boolean mPendingSync;
        float mPendingXOffset;
        float mPendingXOffsetStep;
        float mPendingYOffset;
        float mPendingYOffsetStep;
        boolean mReportedVisible;
        IWindowSession mSession;
        final Rect mStableInsets;
        boolean mSurfaceCreated;
        final BaseSurfaceHolder mSurfaceHolder;
        int mType;
        boolean mVisible;
        final Rect mVisibleInsets;
        int mWidth;
        final Rect mWinFrame;
        final BaseIWindow mWindow;
        int mWindowFlags;
        int mWindowPrivateFlags;
        IBinder mWindowToken;

        final class WallpaperInputEventReceiver extends InputEventReceiver {
            public WallpaperInputEventReceiver(InputChannel inputChannel, Looper looper) {
                super(inputChannel, looper);
            }

            @Override
            public void onInputEvent(InputEvent inputEvent, int i) {
                boolean z = false;
                try {
                    if ((inputEvent instanceof MotionEvent) && (inputEvent.getSource() & 2) != 0) {
                        Engine.this.dispatchPointer(MotionEvent.obtainNoHistory((MotionEvent) inputEvent));
                        z = true;
                    }
                } finally {
                    finishInputEvent(inputEvent, false);
                }
            }
        }

        public Engine(WallpaperService wallpaperService) {
            this(new Supplier() {
                @Override
                public final Object get() {
                    return Long.valueOf(SystemClock.elapsedRealtime());
                }
            }, Handler.getMain());
        }

        @VisibleForTesting
        public Engine(Supplier<Long> supplier, Handler handler) {
            this.mInitializing = true;
            this.mWindowFlags = 16;
            this.mWindowPrivateFlags = 4;
            this.mCurWindowFlags = this.mWindowFlags;
            this.mCurWindowPrivateFlags = this.mWindowPrivateFlags;
            this.mVisibleInsets = new Rect();
            this.mWinFrame = new Rect();
            this.mOverscanInsets = new Rect();
            this.mContentInsets = new Rect();
            this.mStableInsets = new Rect();
            this.mOutsets = new Rect();
            this.mDispatchedOverscanInsets = new Rect();
            this.mDispatchedContentInsets = new Rect();
            this.mDispatchedStableInsets = new Rect();
            this.mDispatchedOutsets = new Rect();
            this.mFinalSystemInsets = new Rect();
            this.mFinalStableInsets = new Rect();
            this.mBackdropFrame = new Rect();
            this.mDisplayCutout = new DisplayCutout.ParcelableWrapper();
            this.mDispatchedDisplayCutout = DisplayCutout.NO_CUTOUT;
            this.mMergedConfiguration = new MergedConfiguration();
            this.mLayout = new WindowManager.LayoutParams();
            this.mLock = new Object();
            this.mNotifyColorsChanged = new Runnable() {
                @Override
                public final void run() {
                    this.f$0.notifyColorsChanged();
                }
            };
            this.mSurfaceHolder = new BaseSurfaceHolder() {
                {
                    this.mRequestedFormat = 2;
                }

                @Override
                public boolean onAllowLockCanvas() {
                    return Engine.this.mDrawingAllowed;
                }

                @Override
                public void onRelayoutContainer() {
                    Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessage(10000));
                }

                @Override
                public void onUpdateSurface() {
                    Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessage(10000));
                }

                @Override
                public boolean isCreating() {
                    return Engine.this.mIsCreating;
                }

                @Override
                public void setFixedSize(int i, int i2) {
                    if (!Engine.this.mFixedSizeAllowed) {
                        throw new UnsupportedOperationException("Wallpapers currently only support sizing from layout");
                    }
                    super.setFixedSize(i, i2);
                }

                @Override
                public void setKeepScreenOn(boolean z) {
                    throw new UnsupportedOperationException("Wallpapers do not support keep screen on");
                }

                private void prepareToDraw() {
                    if (Engine.this.mDisplayState == 3 || Engine.this.mDisplayState == 4) {
                        try {
                            Engine.this.mSession.pokeDrawLock(Engine.this.mWindow);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public Canvas lockCanvas() {
                    prepareToDraw();
                    return super.lockCanvas();
                }

                @Override
                public Canvas lockCanvas(Rect rect) {
                    prepareToDraw();
                    return super.lockCanvas(rect);
                }

                @Override
                public Canvas lockHardwareCanvas() {
                    prepareToDraw();
                    return super.lockHardwareCanvas();
                }
            };
            this.mWindow = new BaseIWindow() {
                @Override
                public void resized(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, boolean z, MergedConfiguration mergedConfiguration, Rect rect7, boolean z2, boolean z3, int i, DisplayCutout.ParcelableWrapper parcelableWrapper) {
                    Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessageIO(10030, z ? 1 : 0, rect6));
                }

                @Override
                public void moved(int i, int i2) {
                    Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessageII(10035, i, i2));
                }

                @Override
                public void dispatchAppVisibility(boolean z) {
                    if (!Engine.this.mIWallpaperEngine.mIsPreview) {
                        Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessageI(10010, z ? 1 : 0));
                    }
                }

                @Override
                public void dispatchWallpaperOffsets(float f, float f2, float f3, float f4, boolean z) {
                    synchronized (Engine.this.mLock) {
                        Engine.this.mPendingXOffset = f;
                        Engine.this.mPendingYOffset = f2;
                        Engine.this.mPendingXOffsetStep = f3;
                        Engine.this.mPendingYOffsetStep = f4;
                        if (z) {
                            Engine.this.mPendingSync = true;
                        }
                        if (!Engine.this.mOffsetMessageEnqueued) {
                            Engine.this.mOffsetMessageEnqueued = true;
                            Engine.this.mCaller.sendMessage(Engine.this.mCaller.obtainMessage(10020));
                        }
                    }
                }

                @Override
                public void dispatchWallpaperCommand(String str, int i, int i2, int i3, Bundle bundle, boolean z) {
                    synchronized (Engine.this.mLock) {
                        WallpaperCommand wallpaperCommand = new WallpaperCommand();
                        wallpaperCommand.action = str;
                        wallpaperCommand.x = i;
                        wallpaperCommand.y = i2;
                        wallpaperCommand.z = i3;
                        wallpaperCommand.extras = bundle;
                        wallpaperCommand.sync = z;
                        Message messageObtainMessage = Engine.this.mCaller.obtainMessage(10025);
                        messageObtainMessage.obj = wallpaperCommand;
                        Engine.this.mCaller.sendMessage(messageObtainMessage);
                    }
                }
            };
            this.mDisplayListener = new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayChanged(int i) throws Throwable {
                    if (Engine.this.mDisplay.getDisplayId() == i) {
                        Engine.this.reportVisibility();
                    }
                }

                @Override
                public void onDisplayRemoved(int i) {
                }

                @Override
                public void onDisplayAdded(int i) {
                }
            };
            this.mClockFunction = supplier;
            this.mHandler = handler;
        }

        public SurfaceHolder getSurfaceHolder() {
            return this.mSurfaceHolder;
        }

        public int getDesiredMinimumWidth() {
            return this.mIWallpaperEngine.mReqWidth;
        }

        public int getDesiredMinimumHeight() {
            return this.mIWallpaperEngine.mReqHeight;
        }

        public boolean isVisible() {
            return this.mReportedVisible;
        }

        public boolean isPreview() {
            return this.mIWallpaperEngine.mIsPreview;
        }

        public boolean isInAmbientMode() {
            return this.mIsInAmbientMode;
        }

        public void setTouchEventsEnabled(boolean z) throws Throwable {
            int i;
            if (z) {
                i = this.mWindowFlags & (-17);
            } else {
                i = this.mWindowFlags | 16;
            }
            this.mWindowFlags = i;
            if (this.mCreated) {
                updateSurface(false, false, false);
            }
        }

        public void setOffsetNotificationsEnabled(boolean z) throws Throwable {
            int i;
            if (z) {
                i = this.mWindowPrivateFlags | 4;
            } else {
                i = this.mWindowPrivateFlags & (-5);
            }
            this.mWindowPrivateFlags = i;
            if (this.mCreated) {
                updateSurface(false, false, false);
            }
        }

        public void setFixedSizeAllowed(boolean z) {
            this.mFixedSizeAllowed = z;
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
        }

        public void onDestroy() {
        }

        public void onVisibilityChanged(boolean z) {
        }

        public void onApplyWindowInsets(WindowInsets windowInsets) {
        }

        public void onTouchEvent(MotionEvent motionEvent) {
        }

        public void onOffsetsChanged(float f, float f2, float f3, float f4, int i, int i2) {
        }

        public Bundle onCommand(String str, int i, int i2, int i3, Bundle bundle, boolean z) {
            return null;
        }

        public void onAmbientModeChanged(boolean z, boolean z2) {
        }

        public void onDesiredSizeChanged(int i, int i2) {
        }

        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        }

        public void onSurfaceRedrawNeeded(SurfaceHolder surfaceHolder) {
        }

        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
        }

        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
        }

        public void notifyColorsChanged() {
            long jLongValue = this.mClockFunction.get().longValue();
            if (jLongValue - this.mLastColorInvalidation < 1000) {
                Log.w(WallpaperService.TAG, "This call has been deferred. You should only call notifyColorsChanged() once every 1.0 seconds.");
                if (!this.mHandler.hasCallbacks(this.mNotifyColorsChanged)) {
                    this.mHandler.postDelayed(this.mNotifyColorsChanged, 1000L);
                    return;
                }
                return;
            }
            this.mLastColorInvalidation = jLongValue;
            this.mHandler.removeCallbacks(this.mNotifyColorsChanged);
            try {
                WallpaperColors wallpaperColorsOnComputeColors = onComputeColors();
                if (this.mConnection != null) {
                    this.mConnection.onWallpaperColorsChanged(wallpaperColorsOnComputeColors);
                } else {
                    Log.w(WallpaperService.TAG, "Can't notify system because wallpaper connection was not established.");
                }
            } catch (RemoteException e) {
                Log.w(WallpaperService.TAG, "Can't notify system because wallpaper connection was lost.", e);
            }
        }

        public WallpaperColors onComputeColors() {
            return null;
        }

        @VisibleForTesting
        public void setCreated(boolean z) {
            this.mCreated = z;
        }

        protected void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.print(str);
            printWriter.print("mInitializing=");
            printWriter.print(this.mInitializing);
            printWriter.print(" mDestroyed=");
            printWriter.println(this.mDestroyed);
            printWriter.print(str);
            printWriter.print("mVisible=");
            printWriter.print(this.mVisible);
            printWriter.print(" mReportedVisible=");
            printWriter.println(this.mReportedVisible);
            printWriter.print(str);
            printWriter.print("mDisplay=");
            printWriter.println(this.mDisplay);
            printWriter.print(str);
            printWriter.print("mCreated=");
            printWriter.print(this.mCreated);
            printWriter.print(" mSurfaceCreated=");
            printWriter.print(this.mSurfaceCreated);
            printWriter.print(" mIsCreating=");
            printWriter.print(this.mIsCreating);
            printWriter.print(" mDrawingAllowed=");
            printWriter.println(this.mDrawingAllowed);
            printWriter.print(str);
            printWriter.print("mWidth=");
            printWriter.print(this.mWidth);
            printWriter.print(" mCurWidth=");
            printWriter.print(this.mCurWidth);
            printWriter.print(" mHeight=");
            printWriter.print(this.mHeight);
            printWriter.print(" mCurHeight=");
            printWriter.println(this.mCurHeight);
            printWriter.print(str);
            printWriter.print("mType=");
            printWriter.print(this.mType);
            printWriter.print(" mWindowFlags=");
            printWriter.print(this.mWindowFlags);
            printWriter.print(" mCurWindowFlags=");
            printWriter.println(this.mCurWindowFlags);
            printWriter.print(str);
            printWriter.print("mWindowPrivateFlags=");
            printWriter.print(this.mWindowPrivateFlags);
            printWriter.print(" mCurWindowPrivateFlags=");
            printWriter.println(this.mCurWindowPrivateFlags);
            printWriter.print(str);
            printWriter.print("mVisibleInsets=");
            printWriter.print(this.mVisibleInsets.toShortString());
            printWriter.print(" mWinFrame=");
            printWriter.print(this.mWinFrame.toShortString());
            printWriter.print(" mContentInsets=");
            printWriter.println(this.mContentInsets.toShortString());
            printWriter.print(str);
            printWriter.print("mConfiguration=");
            printWriter.println(this.mMergedConfiguration.getMergedConfiguration());
            printWriter.print(str);
            printWriter.print("mLayout=");
            printWriter.println(this.mLayout);
            synchronized (this.mLock) {
                printWriter.print(str);
                printWriter.print("mPendingXOffset=");
                printWriter.print(this.mPendingXOffset);
                printWriter.print(" mPendingXOffset=");
                printWriter.println(this.mPendingXOffset);
                printWriter.print(str);
                printWriter.print("mPendingXOffsetStep=");
                printWriter.print(this.mPendingXOffsetStep);
                printWriter.print(" mPendingXOffsetStep=");
                printWriter.println(this.mPendingXOffsetStep);
                printWriter.print(str);
                printWriter.print("mOffsetMessageEnqueued=");
                printWriter.print(this.mOffsetMessageEnqueued);
                printWriter.print(" mPendingSync=");
                printWriter.println(this.mPendingSync);
                if (this.mPendingMove != null) {
                    printWriter.print(str);
                    printWriter.print("mPendingMove=");
                    printWriter.println(this.mPendingMove);
                }
            }
        }

        private void dispatchPointer(MotionEvent motionEvent) {
            if (motionEvent.isTouchEvent()) {
                synchronized (this.mLock) {
                    if (motionEvent.getAction() == 2) {
                        this.mPendingMove = motionEvent;
                    } else {
                        this.mPendingMove = null;
                    }
                }
                this.mCaller.sendMessage(this.mCaller.obtainMessageO(10040, motionEvent));
                return;
            }
            motionEvent.recycle();
        }

        void updateSurface(boolean z, boolean z2, boolean z3) throws Throwable {
            boolean z4;
            boolean z5;
            boolean z6;
            boolean z7;
            boolean z8;
            boolean z9;
            if (this.mDestroyed) {
                Log.w(WallpaperService.TAG, "Ignoring updateSurface: destroyed");
            }
            int requestedWidth = this.mSurfaceHolder.getRequestedWidth();
            int i = -1;
            if (requestedWidth <= 0) {
                requestedWidth = -1;
                z4 = false;
            } else {
                z4 = true;
            }
            int requestedHeight = this.mSurfaceHolder.getRequestedHeight();
            if (requestedHeight > 0) {
                z4 = true;
                i = requestedHeight;
            }
            boolean z10 = !this.mCreated;
            boolean z11 = !this.mSurfaceCreated;
            boolean z12 = this.mFormat != this.mSurfaceHolder.getRequestedFormat();
            boolean z13 = (this.mWidth == requestedWidth && this.mHeight == i) ? false : true;
            boolean z14 = !this.mCreated;
            boolean z15 = this.mType != this.mSurfaceHolder.getRequestedType();
            boolean z16 = (this.mCurWindowFlags == this.mWindowFlags && this.mCurWindowPrivateFlags == this.mWindowPrivateFlags) ? false : true;
            if (z || z10 || z11 || z12 || z13 || z15 || z16 || z3 || !this.mIWallpaperEngine.mShownReported) {
                try {
                    this.mWidth = requestedWidth;
                    this.mHeight = i;
                    this.mFormat = this.mSurfaceHolder.getRequestedFormat();
                    this.mType = this.mSurfaceHolder.getRequestedType();
                    this.mLayout.x = 0;
                    this.mLayout.y = 0;
                    this.mLayout.width = requestedWidth;
                    this.mLayout.height = i;
                    this.mLayout.format = this.mFormat;
                    this.mCurWindowFlags = this.mWindowFlags;
                    this.mLayout.flags = this.mWindowFlags | 512 | 65536 | 256 | 8;
                    this.mCurWindowPrivateFlags = this.mWindowPrivateFlags;
                    this.mLayout.privateFlags = this.mWindowPrivateFlags;
                    this.mLayout.memoryType = this.mType;
                    this.mLayout.token = this.mWindowToken;
                    if (!this.mCreated) {
                        WallpaperService.this.obtainStyledAttributes(R.styleable.Window).recycle();
                        this.mLayout.type = this.mIWallpaperEngine.mWindowType;
                        this.mLayout.gravity = 8388659;
                        this.mLayout.setTitle(WallpaperService.this.getClass().getName());
                        this.mLayout.windowAnimations = R.style.Animation_Wallpaper;
                        this.mInputChannel = new InputChannel();
                        z6 = z13;
                        z5 = z12;
                        if (this.mSession.addToDisplay(this.mWindow, this.mWindow.mSeq, this.mLayout, 0, 0, this.mWinFrame, this.mContentInsets, this.mStableInsets, this.mOutsets, this.mDisplayCutout, this.mInputChannel) < 0) {
                            Log.w(WallpaperService.TAG, "Failed to add window while updating wallpaper surface.");
                            return;
                        } else {
                            this.mCreated = true;
                            this.mInputEventReceiver = new WallpaperInputEventReceiver(this.mInputChannel, Looper.myLooper());
                        }
                    } else {
                        z5 = z12;
                        z6 = z13;
                    }
                    this.mSurfaceHolder.mSurfaceLock.lock();
                    this.mDrawingAllowed = true;
                    if (!z4) {
                        this.mLayout.surfaceInsets.set(this.mIWallpaperEngine.mDisplayPadding);
                        this.mLayout.surfaceInsets.left += this.mOutsets.left;
                        this.mLayout.surfaceInsets.top += this.mOutsets.top;
                        this.mLayout.surfaceInsets.right += this.mOutsets.right;
                        this.mLayout.surfaceInsets.bottom += this.mOutsets.bottom;
                    } else {
                        this.mLayout.surfaceInsets.set(0, 0, 0, 0);
                    }
                    boolean z17 = z4;
                    int iRelayout = this.mSession.relayout(this.mWindow, this.mWindow.mSeq, this.mLayout, this.mWidth, this.mHeight, 0, 0, -1L, this.mWinFrame, this.mOverscanInsets, this.mContentInsets, this.mVisibleInsets, this.mStableInsets, this.mOutsets, this.mBackdropFrame, this.mDisplayCutout, this.mMergedConfiguration, this.mSurfaceHolder.mSurface);
                    int iWidth = this.mWinFrame.width();
                    int iHeight = this.mWinFrame.height();
                    if (!z17) {
                        Rect rect = this.mIWallpaperEngine.mDisplayPadding;
                        iWidth += rect.left + rect.right + this.mOutsets.left + this.mOutsets.right;
                        iHeight += rect.top + rect.bottom + this.mOutsets.top + this.mOutsets.bottom;
                        this.mOverscanInsets.left += rect.left;
                        this.mOverscanInsets.top += rect.top;
                        this.mOverscanInsets.right += rect.right;
                        this.mOverscanInsets.bottom += rect.bottom;
                        this.mContentInsets.left += rect.left;
                        this.mContentInsets.top += rect.top;
                        this.mContentInsets.right += rect.right;
                        this.mContentInsets.bottom += rect.bottom;
                        this.mStableInsets.left += rect.left;
                        this.mStableInsets.top += rect.top;
                        this.mStableInsets.right += rect.right;
                        this.mStableInsets.bottom += rect.bottom;
                        this.mDisplayCutout.set(this.mDisplayCutout.get().inset(-rect.left, -rect.top, -rect.right, -rect.bottom));
                    }
                    if (this.mCurWidth != iWidth) {
                        this.mCurWidth = iWidth;
                        z7 = true;
                    } else {
                        z7 = z6;
                    }
                    if (this.mCurHeight != iHeight) {
                        this.mCurHeight = iHeight;
                        z7 = true;
                    }
                    boolean z18 = z14 | (!this.mDispatchedOverscanInsets.equals(this.mOverscanInsets)) | (!this.mDispatchedContentInsets.equals(this.mContentInsets)) | (!this.mDispatchedStableInsets.equals(this.mStableInsets)) | (!this.mDispatchedOutsets.equals(this.mOutsets)) | (!this.mDispatchedDisplayCutout.equals(this.mDisplayCutout.get()));
                    this.mSurfaceHolder.setSurfaceFrameSize(iWidth, iHeight);
                    this.mSurfaceHolder.mSurfaceLock.unlock();
                    if (!this.mSurfaceHolder.mSurface.isValid()) {
                        reportSurfaceDestroyed();
                        return;
                    }
                    try {
                        this.mSurfaceHolder.ungetCallbacks();
                        if (z11) {
                            this.mIsCreating = true;
                            onSurfaceCreated(this.mSurfaceHolder);
                            SurfaceHolder.Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (SurfaceHolder.Callback callback : callbacks) {
                                    callback.surfaceCreated(this.mSurfaceHolder);
                                }
                            }
                            z9 = true;
                        } else {
                            z9 = false;
                        }
                        z8 = z3 | (z10 || (iRelayout & 2) != 0);
                        if (z2 || z10 || z11 || z5 || z7) {
                            try {
                                onSurfaceChanged(this.mSurfaceHolder, this.mFormat, this.mCurWidth, this.mCurHeight);
                                SurfaceHolder.Callback[] callbacks2 = this.mSurfaceHolder.getCallbacks();
                                if (callbacks2 != null) {
                                    for (SurfaceHolder.Callback callback2 : callbacks2) {
                                        callback2.surfaceChanged(this.mSurfaceHolder, this.mFormat, this.mCurWidth, this.mCurHeight);
                                    }
                                }
                                z9 = true;
                            } catch (Throwable th) {
                                th = th;
                                this.mIsCreating = false;
                                this.mSurfaceCreated = true;
                                if (z8) {
                                    this.mSession.finishDrawing(this.mWindow);
                                }
                                this.mIWallpaperEngine.reportShown();
                                throw th;
                            }
                        }
                        if (z18) {
                            this.mDispatchedOverscanInsets.set(this.mOverscanInsets);
                            this.mDispatchedOverscanInsets.left += this.mOutsets.left;
                            this.mDispatchedOverscanInsets.top += this.mOutsets.top;
                            this.mDispatchedOverscanInsets.right += this.mOutsets.right;
                            this.mDispatchedOverscanInsets.bottom += this.mOutsets.bottom;
                            this.mDispatchedContentInsets.set(this.mContentInsets);
                            this.mDispatchedStableInsets.set(this.mStableInsets);
                            this.mDispatchedOutsets.set(this.mOutsets);
                            this.mDispatchedDisplayCutout = this.mDisplayCutout.get();
                            this.mFinalSystemInsets.set(this.mDispatchedOverscanInsets);
                            this.mFinalStableInsets.set(this.mDispatchedStableInsets);
                            onApplyWindowInsets(new WindowInsets(this.mFinalSystemInsets, null, this.mFinalStableInsets, WallpaperService.this.getResources().getConfiguration().isScreenRound(), false, this.mDispatchedDisplayCutout));
                        }
                        if (z8) {
                            onSurfaceRedrawNeeded(this.mSurfaceHolder);
                            SurfaceHolder.Callback[] callbacks3 = this.mSurfaceHolder.getCallbacks();
                            if (callbacks3 != null) {
                                for (SurfaceHolder.Callback callback3 : callbacks3) {
                                    if (callback3 instanceof SurfaceHolder.Callback2) {
                                        ((SurfaceHolder.Callback2) callback3).surfaceRedrawNeeded(this.mSurfaceHolder);
                                    }
                                }
                            }
                        }
                        if (z9 && !this.mReportedVisible) {
                            if (this.mIsCreating) {
                                onVisibilityChanged(true);
                            }
                            onVisibilityChanged(false);
                        }
                        this.mIsCreating = false;
                        this.mSurfaceCreated = true;
                        if (z8) {
                            this.mSession.finishDrawing(this.mWindow);
                        }
                        this.mIWallpaperEngine.reportShown();
                    } catch (Throwable th2) {
                        th = th2;
                        z8 = z3;
                    }
                } catch (RemoteException e) {
                }
            }
        }

        void attach(IWallpaperEngineWrapper iWallpaperEngineWrapper) throws Throwable {
            if (this.mDestroyed) {
                return;
            }
            this.mIWallpaperEngine = iWallpaperEngineWrapper;
            this.mCaller = iWallpaperEngineWrapper.mCaller;
            this.mConnection = iWallpaperEngineWrapper.mConnection;
            this.mWindowToken = iWallpaperEngineWrapper.mWindowToken;
            this.mSurfaceHolder.setSizeFromLayout();
            this.mInitializing = true;
            this.mSession = WindowManagerGlobal.getWindowSession();
            this.mWindow.setSession(this.mSession);
            this.mLayout.packageName = WallpaperService.this.getPackageName();
            this.mDisplayManager = (DisplayManager) WallpaperService.this.getSystemService(Context.DISPLAY_SERVICE);
            this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mCaller.getHandler());
            this.mDisplay = this.mDisplayManager.getDisplay(0);
            this.mDisplayState = this.mDisplay.getState();
            onCreate(this.mSurfaceHolder);
            this.mInitializing = false;
            this.mReportedVisible = false;
            updateSurface(false, false, false);
        }

        @VisibleForTesting
        public void doAmbientModeChanged(boolean z, boolean z2) {
            if (!this.mDestroyed) {
                this.mIsInAmbientMode = z;
                if (this.mCreated) {
                    onAmbientModeChanged(z, z2);
                }
            }
        }

        void doDesiredSizeChanged(int i, int i2) {
            if (!this.mDestroyed) {
                this.mIWallpaperEngine.mReqWidth = i;
                this.mIWallpaperEngine.mReqHeight = i2;
                onDesiredSizeChanged(i, i2);
                doOffsetsChanged(true);
            }
        }

        void doDisplayPaddingChanged(Rect rect) throws Throwable {
            if (!this.mDestroyed && !this.mIWallpaperEngine.mDisplayPadding.equals(rect)) {
                this.mIWallpaperEngine.mDisplayPadding.set(rect);
                updateSurface(true, false, false);
            }
        }

        void doVisibilityChanged(boolean z) throws Throwable {
            if (!this.mDestroyed) {
                this.mVisible = z;
                reportVisibility();
            }
        }

        void reportVisibility() throws Throwable {
            if (!this.mDestroyed) {
                this.mDisplayState = this.mDisplay == null ? 0 : this.mDisplay.getState();
                boolean z = this.mVisible && this.mDisplayState != 1;
                if (this.mReportedVisible != z) {
                    this.mReportedVisible = z;
                    if (z) {
                        doOffsetsChanged(false);
                        updateSurface(false, false, false);
                    }
                    onVisibilityChanged(z);
                }
            }
        }

        void doOffsetsChanged(boolean z) {
            float f;
            float f2;
            float f3;
            float f4;
            boolean z2;
            int i;
            int i2;
            if (this.mDestroyed) {
                return;
            }
            if (!z && !this.mOffsetsChanged) {
                return;
            }
            synchronized (this.mLock) {
                f = this.mPendingXOffset;
                f2 = this.mPendingYOffset;
                f3 = this.mPendingXOffsetStep;
                f4 = this.mPendingYOffsetStep;
                z2 = this.mPendingSync;
                i = 0;
                this.mPendingSync = false;
                this.mOffsetMessageEnqueued = false;
            }
            if (this.mSurfaceCreated) {
                if (this.mReportedVisible) {
                    int i3 = this.mIWallpaperEngine.mReqWidth - this.mCurWidth;
                    if (i3 > 0) {
                        i2 = -((int) ((i3 * f) + 0.5f));
                    } else {
                        i2 = 0;
                    }
                    int i4 = this.mIWallpaperEngine.mReqHeight - this.mCurHeight;
                    if (i4 > 0) {
                        i = -((int) ((i4 * f2) + 0.5f));
                    }
                    onOffsetsChanged(f, f2, f3, f4, i2, i);
                } else {
                    this.mOffsetsChanged = true;
                }
            }
            if (z2) {
                try {
                    this.mSession.wallpaperOffsetsComplete(this.mWindow.asBinder());
                } catch (RemoteException e) {
                }
            }
        }

        void doCommand(WallpaperCommand wallpaperCommand) {
            Bundle bundleOnCommand;
            if (!this.mDestroyed) {
                bundleOnCommand = onCommand(wallpaperCommand.action, wallpaperCommand.x, wallpaperCommand.y, wallpaperCommand.z, wallpaperCommand.extras, wallpaperCommand.sync);
            } else {
                bundleOnCommand = null;
            }
            if (wallpaperCommand.sync) {
                try {
                    this.mSession.wallpaperCommandComplete(this.mWindow.asBinder(), bundleOnCommand);
                } catch (RemoteException e) {
                }
            }
        }

        void reportSurfaceDestroyed() {
            if (this.mSurfaceCreated) {
                this.mSurfaceCreated = false;
                this.mSurfaceHolder.ungetCallbacks();
                SurfaceHolder.Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
                if (callbacks != null) {
                    for (SurfaceHolder.Callback callback : callbacks) {
                        callback.surfaceDestroyed(this.mSurfaceHolder);
                    }
                }
                onSurfaceDestroyed(this.mSurfaceHolder);
            }
        }

        void detach() {
            if (this.mDestroyed) {
                return;
            }
            this.mDestroyed = true;
            if (this.mDisplayManager != null) {
                this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
            }
            if (this.mVisible) {
                this.mVisible = false;
                onVisibilityChanged(false);
            }
            reportSurfaceDestroyed();
            onDestroy();
            if (this.mCreated) {
                try {
                    if (this.mInputEventReceiver != null) {
                        this.mInputEventReceiver.dispose();
                        this.mInputEventReceiver = null;
                    }
                    this.mSession.remove(this.mWindow);
                } catch (RemoteException e) {
                }
                this.mSurfaceHolder.mSurface.release();
                this.mCreated = false;
                if (this.mInputChannel != null) {
                    this.mInputChannel.dispose();
                    this.mInputChannel = null;
                }
            }
        }
    }

    class IWallpaperEngineWrapper extends IWallpaperEngine.Stub implements HandlerCaller.Callback {
        private final HandlerCaller mCaller;
        final IWallpaperConnection mConnection;
        final Rect mDisplayPadding = new Rect();
        Engine mEngine;
        final boolean mIsPreview;
        int mReqHeight;
        int mReqWidth;
        boolean mShownReported;
        final IBinder mWindowToken;
        final int mWindowType;

        IWallpaperEngineWrapper(WallpaperService wallpaperService, IWallpaperConnection iWallpaperConnection, IBinder iBinder, int i, boolean z, int i2, int i3, Rect rect) {
            this.mCaller = new HandlerCaller(wallpaperService, wallpaperService.getMainLooper(), this, true);
            this.mConnection = iWallpaperConnection;
            this.mWindowToken = iBinder;
            this.mWindowType = i;
            this.mIsPreview = z;
            this.mReqWidth = i2;
            this.mReqHeight = i3;
            this.mDisplayPadding.set(rect);
            this.mCaller.sendMessage(this.mCaller.obtainMessage(10));
        }

        @Override
        public void setDesiredSize(int i, int i2) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageII(30, i, i2));
        }

        @Override
        public void setDisplayPadding(Rect rect) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageO(40, rect));
        }

        @Override
        public void setVisibility(boolean z) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageI(10010, z ? 1 : 0));
        }

        @Override
        public void setInAmbientMode(boolean z, boolean z2) throws RemoteException {
            this.mCaller.sendMessage(this.mCaller.obtainMessageII(50, z ? 1 : 0, z2 ? 1 : 0));
        }

        @Override
        public void dispatchPointer(MotionEvent motionEvent) {
            if (this.mEngine != null) {
                this.mEngine.dispatchPointer(motionEvent);
            } else {
                motionEvent.recycle();
            }
        }

        @Override
        public void dispatchWallpaperCommand(String str, int i, int i2, int i3, Bundle bundle) {
            if (this.mEngine != null) {
                this.mEngine.mWindow.dispatchWallpaperCommand(str, i, i2, i3, bundle, false);
            }
        }

        public void reportShown() {
            if (!this.mShownReported) {
                this.mShownReported = true;
                try {
                    this.mConnection.engineShown(this);
                } catch (RemoteException e) {
                    Log.w(WallpaperService.TAG, "Wallpaper host disappeared", e);
                }
            }
        }

        @Override
        public void requestWallpaperColors() {
            this.mCaller.sendMessage(this.mCaller.obtainMessage(10050));
        }

        @Override
        public void destroy() {
            this.mCaller.sendMessage(this.mCaller.obtainMessage(20));
        }

        @Override
        public void executeMessage(Message message) throws Throwable {
            switch (message.what) {
                case 10:
                    try {
                        this.mConnection.attachEngine(this);
                        Engine engineOnCreateEngine = WallpaperService.this.onCreateEngine();
                        this.mEngine = engineOnCreateEngine;
                        WallpaperService.this.mActiveEngines.add(engineOnCreateEngine);
                        engineOnCreateEngine.attach(this);
                        return;
                    } catch (RemoteException e) {
                        Log.w(WallpaperService.TAG, "Wallpaper host disappeared", e);
                        return;
                    }
                case 20:
                    WallpaperService.this.mActiveEngines.remove(this.mEngine);
                    this.mEngine.detach();
                    return;
                case 30:
                    this.mEngine.doDesiredSizeChanged(message.arg1, message.arg2);
                    return;
                case 40:
                    this.mEngine.doDisplayPaddingChanged((Rect) message.obj);
                    return;
                case 50:
                    this.mEngine.doAmbientModeChanged(message.arg1 != 0, message.arg2 != 0);
                    return;
                case 10000:
                    this.mEngine.updateSurface(true, false, false);
                    return;
                case 10010:
                    this.mEngine.doVisibilityChanged(message.arg1 != 0);
                    return;
                case 10020:
                    this.mEngine.doOffsetsChanged(true);
                    return;
                case 10025:
                    this.mEngine.doCommand((WallpaperCommand) message.obj);
                    return;
                case 10030:
                    boolean z = message.arg1 != 0;
                    this.mEngine.mOutsets.set((Rect) message.obj);
                    this.mEngine.updateSurface(true, false, z);
                    this.mEngine.doOffsetsChanged(true);
                    return;
                case 10035:
                    return;
                case 10040:
                    MotionEvent motionEvent = (MotionEvent) message.obj;
                    if (motionEvent.getAction() == 2) {
                        synchronized (this.mEngine.mLock) {
                            if (this.mEngine.mPendingMove == motionEvent) {
                                this.mEngine.mPendingMove = null;
                            } else {
                                z = true;
                            }
                            break;
                        }
                    }
                    if (!z) {
                        this.mEngine.onTouchEvent(motionEvent);
                    }
                    motionEvent.recycle();
                    return;
                case 10050:
                    if (this.mConnection != null) {
                        try {
                            this.mConnection.onWallpaperColorsChanged(this.mEngine.onComputeColors());
                            return;
                        } catch (RemoteException e2) {
                            return;
                        }
                    }
                    return;
                default:
                    Log.w(WallpaperService.TAG, "Unknown message type " + message.what);
                    return;
            }
        }
    }

    class IWallpaperServiceWrapper extends IWallpaperService.Stub {
        private final WallpaperService mTarget;

        public IWallpaperServiceWrapper(WallpaperService wallpaperService) {
            this.mTarget = wallpaperService;
        }

        @Override
        public void attach(IWallpaperConnection iWallpaperConnection, IBinder iBinder, int i, boolean z, int i2, int i3, Rect rect) {
            WallpaperService.this.new IWallpaperEngineWrapper(this.mTarget, iWallpaperConnection, iBinder, i, z, i2, i3, rect);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < this.mActiveEngines.size(); i++) {
            this.mActiveEngines.get(i).detach();
        }
        this.mActiveEngines.clear();
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new IWallpaperServiceWrapper(this);
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("State of wallpaper ");
        printWriter.print(this);
        printWriter.println(SettingsStringUtil.DELIMITER);
        for (int i = 0; i < this.mActiveEngines.size(); i++) {
            Engine engine = this.mActiveEngines.get(i);
            printWriter.print("  Engine ");
            printWriter.print(engine);
            printWriter.println(SettingsStringUtil.DELIMITER);
            engine.dump("    ", fileDescriptor, printWriter, strArr);
        }
    }
}
