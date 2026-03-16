package android.service.wallpaper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.IWindowSession;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.ViewRootImpl;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import com.android.internal.os.HandlerCaller;
import com.android.internal.view.BaseIWindow;
import com.android.internal.view.BaseSurfaceHolder;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public abstract class WallpaperService extends Service {
    static final boolean DEBUG = false;
    private static final int DO_ATTACH = 10;
    private static final int DO_DETACH = 20;
    private static final int DO_SET_DESIRED_SIZE = 30;
    private static final int DO_SET_DISPLAY_PADDING = 40;
    private static final int MSG_TOUCH_EVENT = 10040;
    private static final int MSG_UPDATE_SURFACE = 10000;
    private static final int MSG_VISIBILITY_CHANGED = 10010;
    private static final int MSG_WALLPAPER_COMMAND = 10025;
    private static final int MSG_WALLPAPER_OFFSETS = 10020;
    private static final int MSG_WINDOW_MOVED = 10035;
    private static final int MSG_WINDOW_RESIZED = 10030;
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
        HandlerCaller mCaller;
        IWallpaperConnection mConnection;
        boolean mCreated;
        int mCurHeight;
        int mCurWidth;
        boolean mDestroyed;
        Display mDisplay;
        DisplayManager mDisplayManager;
        boolean mDrawingAllowed;
        boolean mFixedSizeAllowed;
        int mFormat;
        int mHeight;
        IWallpaperEngineWrapper mIWallpaperEngine;
        InputChannel mInputChannel;
        WallpaperInputEventReceiver mInputEventReceiver;
        private boolean mIsCircularEmulator;
        boolean mIsCreating;
        private boolean mIsEmulator;
        boolean mOffsetMessageEnqueued;
        boolean mOffsetsChanged;
        TypedValue mOutsetBottom;
        MotionEvent mPendingMove;
        boolean mPendingSync;
        float mPendingXOffset;
        float mPendingXOffsetStep;
        float mPendingYOffset;
        float mPendingYOffsetStep;
        boolean mReportedVisible;
        IWindowSession mSession;
        boolean mSurfaceCreated;
        int mType;
        boolean mVisible;
        int mWidth;
        private boolean mWindowIsRound;
        IBinder mWindowToken;
        boolean mInitializing = true;
        int mWindowFlags = 16;
        int mWindowPrivateFlags = 4;
        int mCurWindowFlags = this.mWindowFlags;
        int mCurWindowPrivateFlags = this.mWindowPrivateFlags;
        final Rect mVisibleInsets = new Rect();
        final Rect mWinFrame = new Rect();
        final Rect mOverscanInsets = new Rect();
        final Rect mContentInsets = new Rect();
        final Rect mStableInsets = new Rect();
        final Rect mDispatchedOverscanInsets = new Rect();
        final Rect mDispatchedContentInsets = new Rect();
        final Rect mDispatchedStableInsets = new Rect();
        final Rect mFinalSystemInsets = new Rect();
        final Rect mFinalStableInsets = new Rect();
        final Configuration mConfiguration = new Configuration();
        final WindowManager.LayoutParams mLayout = new WindowManager.LayoutParams();
        final Object mLock = new Object();
        final BaseSurfaceHolder mSurfaceHolder = new BaseSurfaceHolder() {
            {
                this.mRequestedFormat = 2;
            }

            @Override
            public boolean onAllowLockCanvas() {
                return Engine.this.mDrawingAllowed;
            }

            @Override
            public void onRelayoutContainer() {
                Message msg = Engine.this.mCaller.obtainMessage(10000);
                Engine.this.mCaller.sendMessage(msg);
            }

            @Override
            public void onUpdateSurface() {
                Message msg = Engine.this.mCaller.obtainMessage(10000);
                Engine.this.mCaller.sendMessage(msg);
            }

            @Override
            public boolean isCreating() {
                return Engine.this.mIsCreating;
            }

            @Override
            public void setFixedSize(int width, int height) {
                if (!Engine.this.mFixedSizeAllowed) {
                    throw new UnsupportedOperationException("Wallpapers currently only support sizing from layout");
                }
                super.setFixedSize(width, height);
            }

            @Override
            public void setKeepScreenOn(boolean screenOn) {
                throw new UnsupportedOperationException("Wallpapers do not support keep screen on");
            }
        };
        final BaseIWindow mWindow = new BaseIWindow() {
            @Override
            public void resized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, boolean reportDraw, Configuration newConfig) {
                Message msg = Engine.this.mCaller.obtainMessageI(WallpaperService.MSG_WINDOW_RESIZED, reportDraw ? 1 : 0);
                Engine.this.mCaller.sendMessage(msg);
            }

            @Override
            public void moved(int newX, int newY) {
                Message msg = Engine.this.mCaller.obtainMessageII(WallpaperService.MSG_WINDOW_MOVED, newX, newY);
                Engine.this.mCaller.sendMessage(msg);
            }

            @Override
            public void dispatchAppVisibility(boolean visible) {
                if (!Engine.this.mIWallpaperEngine.mIsPreview) {
                    Message msg = Engine.this.mCaller.obtainMessageI(WallpaperService.MSG_VISIBILITY_CHANGED, visible ? 1 : 0);
                    Engine.this.mCaller.sendMessage(msg);
                }
            }

            @Override
            public void dispatchWallpaperOffsets(float x, float y, float xStep, float yStep, boolean sync) {
                synchronized (Engine.this.mLock) {
                    Engine.this.mPendingXOffset = x;
                    Engine.this.mPendingYOffset = y;
                    Engine.this.mPendingXOffsetStep = xStep;
                    Engine.this.mPendingYOffsetStep = yStep;
                    if (sync) {
                        Engine.this.mPendingSync = true;
                    }
                    if (!Engine.this.mOffsetMessageEnqueued) {
                        Engine.this.mOffsetMessageEnqueued = true;
                        Message msg = Engine.this.mCaller.obtainMessage(WallpaperService.MSG_WALLPAPER_OFFSETS);
                        Engine.this.mCaller.sendMessage(msg);
                    }
                }
            }

            @Override
            public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras, boolean sync) {
                synchronized (Engine.this.mLock) {
                    WallpaperCommand cmd = new WallpaperCommand();
                    cmd.action = action;
                    cmd.x = x;
                    cmd.y = y;
                    cmd.z = z;
                    cmd.extras = extras;
                    cmd.sync = sync;
                    Message msg = Engine.this.mCaller.obtainMessage(WallpaperService.MSG_WALLPAPER_COMMAND);
                    msg.obj = cmd;
                    Engine.this.mCaller.sendMessage(msg);
                }
            }
        };
        private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayChanged(int displayId) {
                if (Engine.this.mDisplay.getDisplayId() == displayId) {
                    Engine.this.reportVisibility();
                }
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayAdded(int displayId) {
            }
        };

        public Engine() {
        }

        final class WallpaperInputEventReceiver extends InputEventReceiver {
            public WallpaperInputEventReceiver(InputChannel inputChannel, Looper looper) {
                super(inputChannel, looper);
            }

            @Override
            public void onInputEvent(InputEvent event) {
                boolean handled = false;
                try {
                    if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
                        MotionEvent dup = MotionEvent.obtainNoHistory((MotionEvent) event);
                        Engine.this.dispatchPointer(dup);
                        handled = true;
                    }
                } finally {
                    finishInputEvent(event, false);
                }
            }
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

        public void setTouchEventsEnabled(boolean enabled) {
            this.mWindowFlags = enabled ? this.mWindowFlags & (-17) : this.mWindowFlags | 16;
            if (this.mCreated) {
                updateSurface(false, false, false);
            }
        }

        public void setOffsetNotificationsEnabled(boolean enabled) {
            this.mWindowPrivateFlags = enabled ? this.mWindowPrivateFlags | 4 : this.mWindowPrivateFlags & (-5);
            if (this.mCreated) {
                updateSurface(false, false, false);
            }
        }

        public void setFixedSizeAllowed(boolean allowed) {
            this.mFixedSizeAllowed = allowed;
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
        }

        public void onDestroy() {
        }

        public void onVisibilityChanged(boolean visible) {
        }

        public void onApplyWindowInsets(WindowInsets insets) {
        }

        public void onTouchEvent(MotionEvent event) {
        }

        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
        }

        public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
            return null;
        }

        public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
        }

        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
        }

        public void onSurfaceCreated(SurfaceHolder holder) {
        }

        public void onSurfaceDestroyed(SurfaceHolder holder) {
        }

        protected void dump(String prefix, FileDescriptor fd, PrintWriter out, String[] args) {
            out.print(prefix);
            out.print("mInitializing=");
            out.print(this.mInitializing);
            out.print(" mDestroyed=");
            out.println(this.mDestroyed);
            out.print(prefix);
            out.print("mVisible=");
            out.print(this.mVisible);
            out.print(" mReportedVisible=");
            out.println(this.mReportedVisible);
            out.print(prefix);
            out.print("mDisplay=");
            out.println(this.mDisplay);
            out.print(prefix);
            out.print("mCreated=");
            out.print(this.mCreated);
            out.print(" mSurfaceCreated=");
            out.print(this.mSurfaceCreated);
            out.print(" mIsCreating=");
            out.print(this.mIsCreating);
            out.print(" mDrawingAllowed=");
            out.println(this.mDrawingAllowed);
            out.print(prefix);
            out.print("mWidth=");
            out.print(this.mWidth);
            out.print(" mCurWidth=");
            out.print(this.mCurWidth);
            out.print(" mHeight=");
            out.print(this.mHeight);
            out.print(" mCurHeight=");
            out.println(this.mCurHeight);
            out.print(prefix);
            out.print("mType=");
            out.print(this.mType);
            out.print(" mWindowFlags=");
            out.print(this.mWindowFlags);
            out.print(" mCurWindowFlags=");
            out.println(this.mCurWindowFlags);
            out.print(prefix);
            out.print("mWindowPrivateFlags=");
            out.print(this.mWindowPrivateFlags);
            out.print(" mCurWindowPrivateFlags=");
            out.println(this.mCurWindowPrivateFlags);
            out.print(prefix);
            out.print("mVisibleInsets=");
            out.print(this.mVisibleInsets.toShortString());
            out.print(" mWinFrame=");
            out.print(this.mWinFrame.toShortString());
            out.print(" mContentInsets=");
            out.println(this.mContentInsets.toShortString());
            out.print(prefix);
            out.print("mConfiguration=");
            out.println(this.mConfiguration);
            out.print(prefix);
            out.print("mLayout=");
            out.println(this.mLayout);
            synchronized (this.mLock) {
                out.print(prefix);
                out.print("mPendingXOffset=");
                out.print(this.mPendingXOffset);
                out.print(" mPendingXOffset=");
                out.println(this.mPendingXOffset);
                out.print(prefix);
                out.print("mPendingXOffsetStep=");
                out.print(this.mPendingXOffsetStep);
                out.print(" mPendingXOffsetStep=");
                out.println(this.mPendingXOffsetStep);
                out.print(prefix);
                out.print("mOffsetMessageEnqueued=");
                out.print(this.mOffsetMessageEnqueued);
                out.print(" mPendingSync=");
                out.println(this.mPendingSync);
                if (this.mPendingMove != null) {
                    out.print(prefix);
                    out.print("mPendingMove=");
                    out.println(this.mPendingMove);
                }
            }
        }

        private void dispatchPointer(MotionEvent event) {
            if (event.isTouchEvent()) {
                synchronized (this.mLock) {
                    if (event.getAction() == 2) {
                        this.mPendingMove = event;
                    } else {
                        this.mPendingMove = null;
                    }
                }
                Message msg = this.mCaller.obtainMessageO(WallpaperService.MSG_TOUCH_EVENT, event);
                this.mCaller.sendMessage(msg);
                return;
            }
            event.recycle();
        }

        void updateSurface(boolean forceRelayout, boolean forceReport, boolean redrawNeeded) {
            if (this.mDestroyed) {
                Log.w(WallpaperService.TAG, "Ignoring updateSurface: destroyed");
            }
            boolean fixedSize = false;
            int myWidth = this.mSurfaceHolder.getRequestedWidth();
            if (myWidth <= 0) {
                myWidth = -1;
            } else {
                fixedSize = true;
            }
            int myHeight = this.mSurfaceHolder.getRequestedHeight();
            if (myHeight <= 0) {
                myHeight = -1;
            } else {
                fixedSize = true;
            }
            boolean creating = !this.mCreated;
            boolean surfaceCreating = !this.mSurfaceCreated;
            boolean formatChanged = this.mFormat != this.mSurfaceHolder.getRequestedFormat();
            boolean sizeChanged = (this.mWidth == myWidth && this.mHeight == myHeight) ? false : true;
            boolean insetsChanged = !this.mCreated;
            boolean typeChanged = this.mType != this.mSurfaceHolder.getRequestedType();
            boolean flagsChanged = (this.mCurWindowFlags == this.mWindowFlags && this.mCurWindowPrivateFlags == this.mWindowPrivateFlags) ? false : true;
            if (forceRelayout || creating || surfaceCreating || formatChanged || sizeChanged || typeChanged || flagsChanged || redrawNeeded || !this.mIWallpaperEngine.mShownReported) {
                try {
                    this.mWidth = myWidth;
                    this.mHeight = myHeight;
                    this.mFormat = this.mSurfaceHolder.getRequestedFormat();
                    this.mType = this.mSurfaceHolder.getRequestedType();
                    this.mLayout.x = 0;
                    this.mLayout.y = 0;
                    this.mLayout.width = myWidth;
                    this.mLayout.height = myHeight;
                    this.mLayout.format = this.mFormat;
                    this.mCurWindowFlags = this.mWindowFlags;
                    this.mLayout.flags = this.mWindowFlags | 512 | 256 | 8;
                    this.mCurWindowPrivateFlags = this.mWindowPrivateFlags;
                    this.mLayout.privateFlags = this.mWindowPrivateFlags;
                    this.mLayout.memoryType = this.mType;
                    this.mLayout.token = this.mWindowToken;
                    if (!this.mCreated) {
                        WindowManager windowService = (WindowManager) WallpaperService.this.getSystemService(Context.WINDOW_SERVICE);
                        TypedArray windowStyle = WallpaperService.this.obtainStyledAttributes(R.styleable.Window);
                        Display display = windowService.getDefaultDisplay();
                        boolean shouldUseBottomOutset = display.getDisplayId() == 0;
                        if (shouldUseBottomOutset && windowStyle.hasValue(52)) {
                            if (this.mOutsetBottom == null) {
                                this.mOutsetBottom = new TypedValue();
                            }
                            windowStyle.getValue(52, this.mOutsetBottom);
                        } else {
                            this.mOutsetBottom = null;
                        }
                        this.mWindowIsRound = WallpaperService.this.getResources().getBoolean(R.bool.config_windowIsRound);
                        windowStyle.recycle();
                        this.mIsEmulator = Build.HARDWARE.contains("goldfish");
                        this.mIsCircularEmulator = SystemProperties.getBoolean(ViewRootImpl.PROPERTY_EMULATOR_CIRCULAR, false);
                        this.mLayout.type = this.mIWallpaperEngine.mWindowType;
                        this.mLayout.gravity = 8388659;
                        this.mLayout.setTitle(WallpaperService.this.getClass().getName());
                        this.mLayout.windowAnimations = R.style.Animation_Wallpaper;
                        this.mInputChannel = new InputChannel();
                        if (this.mSession.addToDisplay(this.mWindow, this.mWindow.mSeq, this.mLayout, 0, 0, this.mContentInsets, this.mStableInsets, this.mInputChannel) < 0) {
                            Log.w(WallpaperService.TAG, "Failed to add window while updating wallpaper surface.");
                            return;
                        } else {
                            this.mCreated = true;
                            this.mInputEventReceiver = new WallpaperInputEventReceiver(this.mInputChannel, Looper.myLooper());
                        }
                    }
                    this.mSurfaceHolder.mSurfaceLock.lock();
                    this.mDrawingAllowed = true;
                    if (!fixedSize) {
                        this.mLayout.surfaceInsets.set(this.mIWallpaperEngine.mDisplayPadding);
                    } else {
                        this.mLayout.surfaceInsets.set(0, 0, 0, 0);
                    }
                    int relayoutResult = this.mSession.relayout(this.mWindow, this.mWindow.mSeq, this.mLayout, this.mWidth, this.mHeight, 0, 0, this.mWinFrame, this.mOverscanInsets, this.mContentInsets, this.mVisibleInsets, this.mStableInsets, this.mConfiguration, this.mSurfaceHolder.mSurface);
                    int w = this.mWinFrame.width();
                    int h = this.mWinFrame.height();
                    if (!fixedSize) {
                        Rect padding = this.mIWallpaperEngine.mDisplayPadding;
                        w += padding.left + padding.right;
                        h += padding.top + padding.bottom;
                        this.mOverscanInsets.left += padding.left;
                        this.mOverscanInsets.top += padding.top;
                        this.mOverscanInsets.right += padding.right;
                        this.mOverscanInsets.bottom += padding.bottom;
                        this.mContentInsets.left += padding.left;
                        this.mContentInsets.top += padding.top;
                        this.mContentInsets.right += padding.right;
                        this.mContentInsets.bottom += padding.bottom;
                        this.mStableInsets.left += padding.left;
                        this.mStableInsets.top += padding.top;
                        this.mStableInsets.right += padding.right;
                        this.mStableInsets.bottom += padding.bottom;
                    }
                    if (this.mCurWidth != w) {
                        sizeChanged = true;
                        this.mCurWidth = w;
                    }
                    if (this.mCurHeight != h) {
                        sizeChanged = true;
                        this.mCurHeight = h;
                    }
                    boolean insetsChanged2 = insetsChanged | (!this.mDispatchedOverscanInsets.equals(this.mOverscanInsets)) | (!this.mDispatchedContentInsets.equals(this.mContentInsets)) | (!this.mDispatchedStableInsets.equals(this.mStableInsets));
                    this.mSurfaceHolder.setSurfaceFrameSize(w, h);
                    this.mSurfaceHolder.mSurfaceLock.unlock();
                    if (!this.mSurfaceHolder.mSurface.isValid()) {
                        reportSurfaceDestroyed();
                        return;
                    }
                    boolean didSurface = false;
                    try {
                        this.mSurfaceHolder.ungetCallbacks();
                        if (surfaceCreating) {
                            this.mIsCreating = true;
                            didSurface = true;
                            onSurfaceCreated(this.mSurfaceHolder);
                            SurfaceHolder.Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (SurfaceHolder.Callback callback : callbacks) {
                                    callback.surfaceCreated(this.mSurfaceHolder);
                                }
                            }
                        }
                        redrawNeeded |= creating || (relayoutResult & 2) != 0;
                        if (forceReport || creating || surfaceCreating || formatChanged || sizeChanged) {
                            didSurface = true;
                            onSurfaceChanged(this.mSurfaceHolder, this.mFormat, this.mCurWidth, this.mCurHeight);
                            SurfaceHolder.Callback[] callbacks2 = this.mSurfaceHolder.getCallbacks();
                            if (callbacks2 != null) {
                                for (SurfaceHolder.Callback callback2 : callbacks2) {
                                    callback2.surfaceChanged(this.mSurfaceHolder, this.mFormat, this.mCurWidth, this.mCurHeight);
                                }
                            }
                        }
                        if (insetsChanged2) {
                            this.mDispatchedOverscanInsets.set(this.mOverscanInsets);
                            this.mDispatchedContentInsets.set(this.mContentInsets);
                            this.mDispatchedStableInsets.set(this.mStableInsets);
                            boolean isRound = (this.mIsEmulator && this.mIsCircularEmulator) || this.mWindowIsRound;
                            this.mFinalSystemInsets.set(this.mDispatchedOverscanInsets);
                            this.mFinalStableInsets.set(this.mDispatchedStableInsets);
                            if (this.mOutsetBottom != null) {
                                DisplayMetrics metrics = WallpaperService.this.getResources().getDisplayMetrics();
                                this.mFinalSystemInsets.bottom = ((int) this.mOutsetBottom.getDimension(metrics)) + this.mIWallpaperEngine.mDisplayPadding.bottom;
                            }
                            WindowInsets insets = new WindowInsets(this.mFinalSystemInsets, null, this.mFinalStableInsets, isRound);
                            onApplyWindowInsets(insets);
                        }
                        if (redrawNeeded) {
                            onSurfaceRedrawNeeded(this.mSurfaceHolder);
                            SurfaceHolder.Callback[] callbacks3 = this.mSurfaceHolder.getCallbacks();
                            if (callbacks3 != null) {
                                for (SurfaceHolder.Callback c : callbacks3) {
                                    if (c instanceof SurfaceHolder.Callback2) {
                                        ((SurfaceHolder.Callback2) c).surfaceRedrawNeeded(this.mSurfaceHolder);
                                    }
                                }
                            }
                        }
                        if (didSurface && !this.mReportedVisible) {
                            if (this.mIsCreating) {
                                onVisibilityChanged(true);
                            }
                            onVisibilityChanged(false);
                        }
                        this.mIsCreating = false;
                        this.mSurfaceCreated = true;
                        if (redrawNeeded) {
                            this.mSession.finishDrawing(this.mWindow);
                        }
                        this.mIWallpaperEngine.reportShown();
                    } catch (Throwable th) {
                        this.mIsCreating = false;
                        this.mSurfaceCreated = true;
                        if (redrawNeeded) {
                            this.mSession.finishDrawing(this.mWindow);
                        }
                        this.mIWallpaperEngine.reportShown();
                        throw th;
                    }
                } catch (RemoteException e) {
                }
            }
        }

        void attach(IWallpaperEngineWrapper wrapper) {
            if (!this.mDestroyed) {
                this.mIWallpaperEngine = wrapper;
                this.mCaller = wrapper.mCaller;
                this.mConnection = wrapper.mConnection;
                this.mWindowToken = wrapper.mWindowToken;
                this.mSurfaceHolder.setSizeFromLayout();
                this.mInitializing = true;
                this.mSession = WindowManagerGlobal.getWindowSession();
                this.mWindow.setSession(this.mSession);
                this.mDisplayManager = (DisplayManager) WallpaperService.this.getSystemService(Context.DISPLAY_SERVICE);
                this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mCaller.getHandler());
                this.mDisplay = this.mDisplayManager.getDisplay(0);
                onCreate(this.mSurfaceHolder);
                this.mInitializing = false;
                this.mReportedVisible = false;
                updateSurface(false, false, false);
            }
        }

        void doDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            if (!this.mDestroyed) {
                this.mIWallpaperEngine.mReqWidth = desiredWidth;
                this.mIWallpaperEngine.mReqHeight = desiredHeight;
                onDesiredSizeChanged(desiredWidth, desiredHeight);
                doOffsetsChanged(true);
            }
        }

        void doDisplayPaddingChanged(Rect padding) {
            if (!this.mDestroyed && !this.mIWallpaperEngine.mDisplayPadding.equals(padding)) {
                this.mIWallpaperEngine.mDisplayPadding.set(padding);
                updateSurface(true, false, false);
            }
        }

        void doVisibilityChanged(boolean visible) {
            if (!this.mDestroyed) {
                this.mVisible = visible;
                reportVisibility();
            }
        }

        void reportVisibility() {
            if (!this.mDestroyed) {
                boolean visible = (this.mDisplay != null) && this.mVisible && this.mDisplay.getState() != 1;
                if (this.mReportedVisible != visible) {
                    this.mReportedVisible = visible;
                    if (visible) {
                        doOffsetsChanged(false);
                        updateSurface(false, false, false);
                    }
                    onVisibilityChanged(visible);
                }
            }
        }

        void doOffsetsChanged(boolean always) {
            float xOffset;
            float yOffset;
            float xOffsetStep;
            float yOffsetStep;
            boolean sync;
            if (!this.mDestroyed) {
                if (always || this.mOffsetsChanged) {
                    synchronized (this.mLock) {
                        xOffset = this.mPendingXOffset;
                        yOffset = this.mPendingYOffset;
                        xOffsetStep = this.mPendingXOffsetStep;
                        yOffsetStep = this.mPendingYOffsetStep;
                        sync = this.mPendingSync;
                        this.mPendingSync = false;
                        this.mOffsetMessageEnqueued = false;
                    }
                    if (this.mSurfaceCreated) {
                        if (this.mReportedVisible) {
                            int availw = this.mIWallpaperEngine.mReqWidth - this.mCurWidth;
                            int xPixels = availw > 0 ? -((int) ((availw * xOffset) + 0.5f)) : 0;
                            int availh = this.mIWallpaperEngine.mReqHeight - this.mCurHeight;
                            int yPixels = availh > 0 ? -((int) ((availh * yOffset) + 0.5f)) : 0;
                            onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixels, yPixels);
                        } else {
                            this.mOffsetsChanged = true;
                        }
                    }
                    if (sync) {
                        try {
                            this.mSession.wallpaperOffsetsComplete(this.mWindow.asBinder());
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        }

        void doCommand(WallpaperCommand cmd) {
            Bundle result;
            if (!this.mDestroyed) {
                result = onCommand(cmd.action, cmd.x, cmd.y, cmd.z, cmd.extras, cmd.sync);
            } else {
                result = null;
            }
            if (cmd.sync) {
                try {
                    this.mSession.wallpaperCommandComplete(this.mWindow.asBinder(), result);
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
                    for (SurfaceHolder.Callback c : callbacks) {
                        c.surfaceDestroyed(this.mSurfaceHolder);
                    }
                }
                onSurfaceDestroyed(this.mSurfaceHolder);
            }
        }

        void detach() {
            if (!this.mDestroyed) {
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

        IWallpaperEngineWrapper(WallpaperService context, IWallpaperConnection conn, IBinder windowToken, int windowType, boolean isPreview, int reqWidth, int reqHeight, Rect padding) {
            this.mCaller = new HandlerCaller(context, context.getMainLooper(), this, true);
            this.mConnection = conn;
            this.mWindowToken = windowToken;
            this.mWindowType = windowType;
            this.mIsPreview = isPreview;
            this.mReqWidth = reqWidth;
            this.mReqHeight = reqHeight;
            this.mDisplayPadding.set(padding);
            Message msg = this.mCaller.obtainMessage(10);
            this.mCaller.sendMessage(msg);
        }

        @Override
        public void setDesiredSize(int width, int height) {
            Message msg = this.mCaller.obtainMessageII(30, width, height);
            this.mCaller.sendMessage(msg);
        }

        @Override
        public void setDisplayPadding(Rect padding) {
            Message msg = this.mCaller.obtainMessageO(40, padding);
            this.mCaller.sendMessage(msg);
        }

        @Override
        public void setVisibility(boolean visible) {
            Message msg = this.mCaller.obtainMessageI(WallpaperService.MSG_VISIBILITY_CHANGED, visible ? 1 : 0);
            this.mCaller.sendMessage(msg);
        }

        @Override
        public void dispatchPointer(MotionEvent event) {
            if (this.mEngine != null) {
                this.mEngine.dispatchPointer(event);
            } else {
                event.recycle();
            }
        }

        @Override
        public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras) {
            if (this.mEngine != null) {
                this.mEngine.mWindow.dispatchWallpaperCommand(action, x, y, z, extras, false);
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
        public void destroy() {
            Message msg = this.mCaller.obtainMessage(20);
            this.mCaller.sendMessage(msg);
        }

        @Override
        public void executeMessage(Message message) {
            switch (message.what) {
                case 10:
                    try {
                        this.mConnection.attachEngine(this);
                        Engine engine = WallpaperService.this.onCreateEngine();
                        this.mEngine = engine;
                        WallpaperService.this.mActiveEngines.add(engine);
                        engine.attach(this);
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
                    break;
                case 10000:
                    break;
                case WallpaperService.MSG_VISIBILITY_CHANGED:
                    this.mEngine.doVisibilityChanged(message.arg1 != 0);
                    return;
                case WallpaperService.MSG_WALLPAPER_OFFSETS:
                    this.mEngine.doOffsetsChanged(true);
                    return;
                case WallpaperService.MSG_WALLPAPER_COMMAND:
                    WallpaperCommand cmd = (WallpaperCommand) message.obj;
                    this.mEngine.doCommand(cmd);
                    return;
                case WallpaperService.MSG_WINDOW_RESIZED:
                    boolean reportDraw = message.arg1 != 0;
                    this.mEngine.updateSurface(true, false, reportDraw);
                    this.mEngine.doOffsetsChanged(true);
                    return;
                case WallpaperService.MSG_WINDOW_MOVED:
                    return;
                case WallpaperService.MSG_TOUCH_EVENT:
                    boolean skip = false;
                    MotionEvent ev = (MotionEvent) message.obj;
                    if (ev.getAction() == 2) {
                        synchronized (this.mEngine.mLock) {
                            if (this.mEngine.mPendingMove == ev) {
                                this.mEngine.mPendingMove = null;
                            } else {
                                skip = true;
                            }
                            break;
                        }
                    }
                    if (!skip) {
                        this.mEngine.onTouchEvent(ev);
                    }
                    ev.recycle();
                    return;
                default:
                    Log.w(WallpaperService.TAG, "Unknown message type " + message.what);
                    return;
            }
            this.mEngine.updateSurface(true, false, false);
        }
    }

    class IWallpaperServiceWrapper extends IWallpaperService.Stub {
        private final WallpaperService mTarget;

        public IWallpaperServiceWrapper(WallpaperService context) {
            this.mTarget = context;
        }

        @Override
        public void attach(IWallpaperConnection conn, IBinder windowToken, int windowType, boolean isPreview, int reqWidth, int reqHeight, Rect padding) {
            WallpaperService.this.new IWallpaperEngineWrapper(this.mTarget, conn, windowToken, windowType, isPreview, reqWidth, reqHeight, padding);
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
    protected void dump(FileDescriptor fd, PrintWriter out, String[] args) {
        out.print("State of wallpaper ");
        out.print(this);
        out.println(":");
        for (int i = 0; i < this.mActiveEngines.size(); i++) {
            Engine engine = this.mActiveEngines.get(i);
            out.print("  Engine ");
            out.print(engine);
            out.println(":");
            engine.dump("    ", fd, out, args);
        }
    }
}
