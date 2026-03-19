package android.service.wallpaper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.util.Log;
import android.view.Display;
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
        private int mDisplayState;
        boolean mDrawingAllowed;
        boolean mFixedSizeAllowed;
        int mFormat;
        int mHeight;
        IWallpaperEngineWrapper mIWallpaperEngine;
        InputChannel mInputChannel;
        WallpaperInputEventReceiver mInputEventReceiver;
        boolean mIsCreating;
        boolean mOffsetMessageEnqueued;
        boolean mOffsetsChanged;
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
        final Rect mOutsets = new Rect();
        final Rect mDispatchedOverscanInsets = new Rect();
        final Rect mDispatchedContentInsets = new Rect();
        final Rect mDispatchedStableInsets = new Rect();
        final Rect mDispatchedOutsets = new Rect();
        final Rect mFinalSystemInsets = new Rect();
        final Rect mFinalStableInsets = new Rect();
        final Rect mBackdropFrame = new Rect();
        final Configuration mConfiguration = new Configuration();
        final WindowManager.LayoutParams mLayout = new WindowManager.LayoutParams();
        final Object mLock = new Object();
        final BaseSurfaceHolder mSurfaceHolder = new BaseSurfaceHolder() {
            {
                this.mRequestedFormat = 2;
            }

            public boolean onAllowLockCanvas() {
                return Engine.this.mDrawingAllowed;
            }

            public void onRelayoutContainer() {
                Message msg = Engine.this.mCaller.obtainMessage(10000);
                Engine.this.mCaller.sendMessage(msg);
            }

            public void onUpdateSurface() {
                Message msg = Engine.this.mCaller.obtainMessage(10000);
                Engine.this.mCaller.sendMessage(msg);
            }

            public boolean isCreating() {
                return Engine.this.mIsCreating;
            }

            public void setFixedSize(int width, int height) {
                if (!Engine.this.mFixedSizeAllowed) {
                    throw new UnsupportedOperationException("Wallpapers currently only support sizing from layout");
                }
                super.setFixedSize(width, height);
            }

            public void setKeepScreenOn(boolean screenOn) {
                throw new UnsupportedOperationException("Wallpapers do not support keep screen on");
            }

            public Canvas lockCanvas() {
                if (Engine.this.mDisplayState == 3 || Engine.this.mDisplayState == 4) {
                    try {
                        Engine.this.mSession.pokeDrawLock(Engine.this.mWindow);
                    } catch (RemoteException e) {
                    }
                }
                return super.lockCanvas();
            }
        };
        final BaseIWindow mWindow = new BaseIWindow() {
            public void resized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, Rect outsets, boolean reportDraw, Configuration newConfig, Rect backDropRect, boolean forceLayout, boolean alwaysConsumeNavBar) {
                Message msg = Engine.this.mCaller.obtainMessageIO(10030, reportDraw ? 1 : 0, outsets);
                Engine.this.mCaller.sendMessage(msg);
            }

            public void moved(int newX, int newY) {
                Message msg = Engine.this.mCaller.obtainMessageII(10035, newX, newY);
                Engine.this.mCaller.sendMessage(msg);
            }

            public void dispatchAppVisibility(boolean visible) {
                if (Engine.this.mIWallpaperEngine.mIsPreview) {
                    return;
                }
                Message msg = Engine.this.mCaller.obtainMessageI(10010, visible ? 1 : 0);
                Engine.this.mCaller.sendMessage(msg);
            }

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
                        Message msg = Engine.this.mCaller.obtainMessage(10020);
                        Engine.this.mCaller.sendMessage(msg);
                    }
                }
            }

            public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras, boolean sync) {
                synchronized (Engine.this.mLock) {
                    WallpaperCommand cmd = new WallpaperCommand();
                    cmd.action = action;
                    cmd.x = x;
                    cmd.y = y;
                    cmd.z = z;
                    cmd.extras = extras;
                    cmd.sync = sync;
                    Message msg = Engine.this.mCaller.obtainMessage(10025);
                    msg.obj = cmd;
                    Engine.this.mCaller.sendMessage(msg);
                }
            }
        };
        private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayChanged(int displayId) {
                if (Engine.this.mDisplay.getDisplayId() != displayId) {
                    return;
                }
                Engine.this.reportVisibility();
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
            int i;
            if (enabled) {
                i = this.mWindowFlags & (-17);
            } else {
                i = this.mWindowFlags | 16;
            }
            this.mWindowFlags = i;
            if (!this.mCreated) {
                return;
            }
            updateSurface(false, false, false);
        }

        public void setOffsetNotificationsEnabled(boolean enabled) {
            int i;
            if (enabled) {
                i = this.mWindowPrivateFlags | 4;
            } else {
                i = this.mWindowPrivateFlags & (-5);
            }
            this.mWindowPrivateFlags = i;
            if (!this.mCreated) {
                return;
            }
            updateSurface(false, false, false);
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
                Message msg = this.mCaller.obtainMessageO(10040, event);
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
            if (!forceRelayout && !creating && !surfaceCreating && !formatChanged && !sizeChanged && !typeChanged && !flagsChanged && !redrawNeeded && this.mIWallpaperEngine.mShownReported) {
                return;
            }
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
                    TypedArray windowStyle = WallpaperService.this.obtainStyledAttributes(R.styleable.Window);
                    windowStyle.recycle();
                    this.mLayout.type = this.mIWallpaperEngine.mWindowType;
                    this.mLayout.gravity = 8388659;
                    this.mLayout.setTitle(WallpaperService.this.getClass().getName());
                    this.mLayout.windowAnimations = 16974578;
                    this.mInputChannel = new InputChannel();
                    if (this.mSession.addToDisplay(this.mWindow, this.mWindow.mSeq, this.mLayout, 0, 0, this.mContentInsets, this.mStableInsets, this.mOutsets, this.mInputChannel) < 0) {
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
                    this.mLayout.surfaceInsets.left += this.mOutsets.left;
                    this.mLayout.surfaceInsets.top += this.mOutsets.top;
                    this.mLayout.surfaceInsets.right += this.mOutsets.right;
                    this.mLayout.surfaceInsets.bottom += this.mOutsets.bottom;
                } else {
                    this.mLayout.surfaceInsets.set(0, 0, 0, 0);
                }
                int relayoutResult = this.mSession.relayout(this.mWindow, this.mWindow.mSeq, this.mLayout, this.mWidth, this.mHeight, 0, 0, this.mWinFrame, this.mOverscanInsets, this.mContentInsets, this.mVisibleInsets, this.mStableInsets, this.mOutsets, this.mBackdropFrame, this.mConfiguration, this.mSurfaceHolder.mSurface);
                int w = this.mWinFrame.width();
                int h = this.mWinFrame.height();
                if (!fixedSize) {
                    Rect padding = this.mIWallpaperEngine.mDisplayPadding;
                    w += padding.left + padding.right + this.mOutsets.left + this.mOutsets.right;
                    h += padding.top + padding.bottom + this.mOutsets.top + this.mOutsets.bottom;
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
                boolean insetsChanged2 = insetsChanged | (!this.mDispatchedOverscanInsets.equals(this.mOverscanInsets)) | (!this.mDispatchedContentInsets.equals(this.mContentInsets)) | (!this.mDispatchedStableInsets.equals(this.mStableInsets)) | (!this.mDispatchedOutsets.equals(this.mOutsets));
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
                        this.mDispatchedOverscanInsets.left += this.mOutsets.left;
                        this.mDispatchedOverscanInsets.top += this.mOutsets.top;
                        this.mDispatchedOverscanInsets.right += this.mOutsets.right;
                        this.mDispatchedOverscanInsets.bottom += this.mOutsets.bottom;
                        this.mDispatchedContentInsets.set(this.mContentInsets);
                        this.mDispatchedStableInsets.set(this.mStableInsets);
                        this.mDispatchedOutsets.set(this.mOutsets);
                        this.mFinalSystemInsets.set(this.mDispatchedOverscanInsets);
                        this.mFinalStableInsets.set(this.mDispatchedStableInsets);
                        WindowInsets insets = new WindowInsets(this.mFinalSystemInsets, null, this.mFinalStableInsets, WallpaperService.this.getResources().getConfiguration().isScreenRound(), false);
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

        void attach(IWallpaperEngineWrapper wrapper) {
            if (this.mDestroyed) {
                return;
            }
            this.mIWallpaperEngine = wrapper;
            this.mCaller = wrapper.mCaller;
            this.mConnection = wrapper.mConnection;
            this.mWindowToken = wrapper.mWindowToken;
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

        void doDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            if (this.mDestroyed) {
                return;
            }
            this.mIWallpaperEngine.mReqWidth = desiredWidth;
            this.mIWallpaperEngine.mReqHeight = desiredHeight;
            onDesiredSizeChanged(desiredWidth, desiredHeight);
            doOffsetsChanged(true);
        }

        void doDisplayPaddingChanged(Rect padding) {
            if (this.mDestroyed || this.mIWallpaperEngine.mDisplayPadding.equals(padding)) {
                return;
            }
            this.mIWallpaperEngine.mDisplayPadding.set(padding);
            updateSurface(true, false, false);
        }

        void doVisibilityChanged(boolean visible) {
            if (this.mDestroyed) {
                return;
            }
            this.mVisible = visible;
            reportVisibility();
        }

        void reportVisibility() {
            if (this.mDestroyed) {
                return;
            }
            this.mDisplayState = this.mDisplay == null ? 0 : this.mDisplay.getState();
            boolean visible = this.mVisible && this.mDisplayState != 1;
            if (this.mReportedVisible == visible) {
                return;
            }
            this.mReportedVisible = visible;
            if (visible) {
                doOffsetsChanged(false);
                updateSurface(false, false, false);
            }
            onVisibilityChanged(visible);
        }

        void doOffsetsChanged(boolean always) {
            float xOffset;
            float yOffset;
            float xOffsetStep;
            float yOffsetStep;
            boolean sync;
            if (this.mDestroyed) {
                return;
            }
            if (!always && !this.mOffsetsChanged) {
                return;
            }
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
            if (!sync) {
                return;
            }
            try {
                this.mSession.wallpaperOffsetsComplete(this.mWindow.asBinder());
            } catch (RemoteException e) {
            }
        }

        void doCommand(WallpaperCommand cmd) {
            Bundle bundleOnCommand;
            if (!this.mDestroyed) {
                bundleOnCommand = onCommand(cmd.action, cmd.x, cmd.y, cmd.z, cmd.extras, cmd.sync);
            } else {
                bundleOnCommand = null;
            }
            if (!cmd.sync) {
                return;
            }
            try {
                this.mSession.wallpaperCommandComplete(this.mWindow.asBinder(), bundleOnCommand);
            } catch (RemoteException e) {
            }
        }

        void reportSurfaceDestroyed() {
            if (!this.mSurfaceCreated) {
                return;
            }
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
            if (!this.mCreated) {
                return;
            }
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
            if (this.mInputChannel == null) {
                return;
            }
            this.mInputChannel.dispose();
            this.mInputChannel = null;
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
            Message msg = this.mCaller.obtainMessageI(10010, visible ? 1 : 0);
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
            if (this.mEngine == null) {
                return;
            }
            this.mEngine.mWindow.dispatchWallpaperCommand(action, x, y, z, extras, false);
        }

        public void reportShown() {
            if (this.mShownReported) {
                return;
            }
            this.mShownReported = true;
            try {
                this.mConnection.engineShown(this);
            } catch (RemoteException e) {
                Log.w(WallpaperService.TAG, "Wallpaper host disappeared", e);
            }
        }

        @Override
        public void destroy() {
            Message msg = this.mCaller.obtainMessage(20);
            this.mCaller.sendMessage(msg);
        }

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
                case 10010:
                    this.mEngine.doVisibilityChanged(message.arg1 != 0);
                    return;
                case 10020:
                    this.mEngine.doOffsetsChanged(true);
                    return;
                case 10025:
                    WallpaperCommand cmd = (WallpaperCommand) message.obj;
                    this.mEngine.doCommand(cmd);
                    return;
                case 10030:
                    boolean reportDraw = message.arg1 != 0;
                    this.mEngine.mOutsets.set((Rect) message.obj);
                    this.mEngine.updateSurface(true, false, reportDraw);
                    this.mEngine.doOffsetsChanged(true);
                    return;
                case 10035:
                    return;
                case 10040:
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
