package android.view;

import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import com.android.internal.view.BaseIWindow;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class SurfaceView extends View {
    private static final boolean DEBUG = false;
    static final int GET_NEW_SURFACE_MSG = 2;
    static final int KEEP_SCREEN_ON_MSG = 1;
    private static final String TAG = "SurfaceView";
    static final int UPDATE_WINDOW_MSG = 3;
    final ArrayList<SurfaceHolder.Callback> mCallbacks;
    final Configuration mConfiguration;
    final Rect mContentInsets;
    private final ViewTreeObserver.OnPreDrawListener mDrawListener;
    boolean mDrawingStopped;
    int mFormat;
    private boolean mGlobalListenersAdded;
    final Handler mHandler;
    boolean mHaveFrame;
    int mHeight;
    boolean mIsCreating;
    long mLastLockTime;
    int mLastSurfaceHeight;
    int mLastSurfaceWidth;
    final WindowManager.LayoutParams mLayout;
    int mLeft;
    final int[] mLocation;
    final Surface mNewSurface;
    final Rect mOverscanInsets;
    boolean mReportDrawNeeded;
    int mRequestedFormat;
    int mRequestedHeight;
    boolean mRequestedVisible;
    int mRequestedWidth;
    final ViewTreeObserver.OnScrollChangedListener mScrollChangedListener;
    IWindowSession mSession;
    final Rect mStableInsets;
    final Surface mSurface;
    boolean mSurfaceCreated;
    final Rect mSurfaceFrame;
    private final SurfaceHolder mSurfaceHolder;
    final ReentrantLock mSurfaceLock;
    int mTop;
    private CompatibilityInfo.Translator mTranslator;
    boolean mUpdateWindowNeeded;
    boolean mViewVisibility;
    boolean mVisible;
    final Rect mVisibleInsets;
    int mWidth;
    final Rect mWinFrame;
    MyWindow mWindow;
    int mWindowType;
    boolean mWindowVisibility;

    public SurfaceView(Context context) {
        super(context);
        this.mCallbacks = new ArrayList<>();
        this.mLocation = new int[2];
        this.mSurfaceLock = new ReentrantLock();
        this.mSurface = new Surface();
        this.mNewSurface = new Surface();
        this.mDrawingStopped = true;
        this.mLayout = new WindowManager.LayoutParams();
        this.mVisibleInsets = new Rect();
        this.mWinFrame = new Rect();
        this.mOverscanInsets = new Rect();
        this.mContentInsets = new Rect();
        this.mStableInsets = new Rect();
        this.mConfiguration = new Configuration();
        this.mWindowType = 1001;
        this.mIsCreating = false;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        SurfaceView.this.setKeepScreenOn(msg.arg1 != 0);
                        break;
                    case 2:
                        SurfaceView.this.handleGetNewSurface();
                        break;
                    case 3:
                        SurfaceView.this.updateWindow(false, false);
                        break;
                }
            }
        };
        this.mScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                SurfaceView.this.updateWindow(false, false);
            }
        };
        this.mRequestedVisible = false;
        this.mWindowVisibility = false;
        this.mViewVisibility = false;
        this.mRequestedWidth = -1;
        this.mRequestedHeight = -1;
        this.mRequestedFormat = 4;
        this.mHaveFrame = false;
        this.mSurfaceCreated = false;
        this.mLastLockTime = 0L;
        this.mVisible = false;
        this.mLeft = -1;
        this.mTop = -1;
        this.mWidth = -1;
        this.mHeight = -1;
        this.mFormat = -1;
        this.mSurfaceFrame = new Rect();
        this.mLastSurfaceWidth = -1;
        this.mLastSurfaceHeight = -1;
        this.mDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                SurfaceView.this.mHaveFrame = SurfaceView.this.getWidth() > 0 && SurfaceView.this.getHeight() > 0;
                SurfaceView.this.updateWindow(false, false);
                return true;
            }
        };
        this.mSurfaceHolder = new SurfaceHolder() {
            private static final String LOG_TAG = "SurfaceHolder";

            @Override
            public boolean isCreating() {
                return SurfaceView.this.mIsCreating;
            }

            @Override
            public void addCallback(SurfaceHolder.Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    if (!SurfaceView.this.mCallbacks.contains(callback)) {
                        SurfaceView.this.mCallbacks.add(callback);
                    }
                }
            }

            @Override
            public void removeCallback(SurfaceHolder.Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    SurfaceView.this.mCallbacks.remove(callback);
                }
            }

            @Override
            public void setFixedSize(int width, int height) {
                if (SurfaceView.this.mRequestedWidth != width || SurfaceView.this.mRequestedHeight != height) {
                    SurfaceView.this.mRequestedWidth = width;
                    SurfaceView.this.mRequestedHeight = height;
                    SurfaceView.this.requestLayout();
                }
            }

            @Override
            public void setSizeFromLayout() {
                if (SurfaceView.this.mRequestedWidth != -1 || SurfaceView.this.mRequestedHeight != -1) {
                    SurfaceView surfaceView = SurfaceView.this;
                    SurfaceView.this.mRequestedHeight = -1;
                    surfaceView.mRequestedWidth = -1;
                    SurfaceView.this.requestLayout();
                }
            }

            @Override
            public void setFormat(int format) {
                if (format == -1) {
                    format = 4;
                }
                SurfaceView.this.mRequestedFormat = format;
                if (SurfaceView.this.mWindow != null) {
                    SurfaceView.this.updateWindow(false, false);
                }
            }

            @Override
            @Deprecated
            public void setType(int type) {
            }

            @Override
            public void setKeepScreenOn(boolean screenOn) {
                Message msg = SurfaceView.this.mHandler.obtainMessage(1);
                msg.arg1 = screenOn ? 1 : 0;
                SurfaceView.this.mHandler.sendMessage(msg);
            }

            @Override
            public Canvas lockCanvas() {
                return internalLockCanvas(null);
            }

            @Override
            public Canvas lockCanvas(Rect inOutDirty) {
                return internalLockCanvas(inOutDirty);
            }

            private final Canvas internalLockCanvas(Rect dirty) {
                SurfaceView.this.mSurfaceLock.lock();
                Canvas c = null;
                if (!SurfaceView.this.mDrawingStopped && SurfaceView.this.mWindow != null) {
                    try {
                        c = SurfaceView.this.mSurface.lockCanvas(dirty);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception locking surface", e);
                    }
                }
                if (c != null) {
                    SurfaceView.this.mLastLockTime = SystemClock.uptimeMillis();
                    return c;
                }
                long now = SystemClock.uptimeMillis();
                long nextTime = SurfaceView.this.mLastLockTime + 100;
                if (nextTime > now) {
                    try {
                        Thread.sleep(nextTime - now);
                    } catch (InterruptedException e2) {
                    }
                    now = SystemClock.uptimeMillis();
                }
                SurfaceView.this.mLastLockTime = now;
                SurfaceView.this.mSurfaceLock.unlock();
                return null;
            }

            @Override
            public void unlockCanvasAndPost(Canvas canvas) {
                if (canvas == null) {
                    Log.d(SurfaceView.TAG, "should not unlockCanvasAndPost with canvas: nulll");
                } else {
                    SurfaceView.this.mSurface.unlockCanvasAndPost(canvas);
                    SurfaceView.this.mSurfaceLock.unlock();
                }
            }

            @Override
            public Surface getSurface() {
                return SurfaceView.this.mSurface;
            }

            @Override
            public Rect getSurfaceFrame() {
                return SurfaceView.this.mSurfaceFrame;
            }
        };
        init();
    }

    public SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCallbacks = new ArrayList<>();
        this.mLocation = new int[2];
        this.mSurfaceLock = new ReentrantLock();
        this.mSurface = new Surface();
        this.mNewSurface = new Surface();
        this.mDrawingStopped = true;
        this.mLayout = new WindowManager.LayoutParams();
        this.mVisibleInsets = new Rect();
        this.mWinFrame = new Rect();
        this.mOverscanInsets = new Rect();
        this.mContentInsets = new Rect();
        this.mStableInsets = new Rect();
        this.mConfiguration = new Configuration();
        this.mWindowType = 1001;
        this.mIsCreating = false;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        SurfaceView.this.setKeepScreenOn(msg.arg1 != 0);
                        break;
                    case 2:
                        SurfaceView.this.handleGetNewSurface();
                        break;
                    case 3:
                        SurfaceView.this.updateWindow(false, false);
                        break;
                }
            }
        };
        this.mScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                SurfaceView.this.updateWindow(false, false);
            }
        };
        this.mRequestedVisible = false;
        this.mWindowVisibility = false;
        this.mViewVisibility = false;
        this.mRequestedWidth = -1;
        this.mRequestedHeight = -1;
        this.mRequestedFormat = 4;
        this.mHaveFrame = false;
        this.mSurfaceCreated = false;
        this.mLastLockTime = 0L;
        this.mVisible = false;
        this.mLeft = -1;
        this.mTop = -1;
        this.mWidth = -1;
        this.mHeight = -1;
        this.mFormat = -1;
        this.mSurfaceFrame = new Rect();
        this.mLastSurfaceWidth = -1;
        this.mLastSurfaceHeight = -1;
        this.mDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                SurfaceView.this.mHaveFrame = SurfaceView.this.getWidth() > 0 && SurfaceView.this.getHeight() > 0;
                SurfaceView.this.updateWindow(false, false);
                return true;
            }
        };
        this.mSurfaceHolder = new SurfaceHolder() {
            private static final String LOG_TAG = "SurfaceHolder";

            @Override
            public boolean isCreating() {
                return SurfaceView.this.mIsCreating;
            }

            @Override
            public void addCallback(SurfaceHolder.Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    if (!SurfaceView.this.mCallbacks.contains(callback)) {
                        SurfaceView.this.mCallbacks.add(callback);
                    }
                }
            }

            @Override
            public void removeCallback(SurfaceHolder.Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    SurfaceView.this.mCallbacks.remove(callback);
                }
            }

            @Override
            public void setFixedSize(int width, int height) {
                if (SurfaceView.this.mRequestedWidth != width || SurfaceView.this.mRequestedHeight != height) {
                    SurfaceView.this.mRequestedWidth = width;
                    SurfaceView.this.mRequestedHeight = height;
                    SurfaceView.this.requestLayout();
                }
            }

            @Override
            public void setSizeFromLayout() {
                if (SurfaceView.this.mRequestedWidth != -1 || SurfaceView.this.mRequestedHeight != -1) {
                    SurfaceView surfaceView = SurfaceView.this;
                    SurfaceView.this.mRequestedHeight = -1;
                    surfaceView.mRequestedWidth = -1;
                    SurfaceView.this.requestLayout();
                }
            }

            @Override
            public void setFormat(int format) {
                if (format == -1) {
                    format = 4;
                }
                SurfaceView.this.mRequestedFormat = format;
                if (SurfaceView.this.mWindow != null) {
                    SurfaceView.this.updateWindow(false, false);
                }
            }

            @Override
            @Deprecated
            public void setType(int type) {
            }

            @Override
            public void setKeepScreenOn(boolean screenOn) {
                Message msg = SurfaceView.this.mHandler.obtainMessage(1);
                msg.arg1 = screenOn ? 1 : 0;
                SurfaceView.this.mHandler.sendMessage(msg);
            }

            @Override
            public Canvas lockCanvas() {
                return internalLockCanvas(null);
            }

            @Override
            public Canvas lockCanvas(Rect inOutDirty) {
                return internalLockCanvas(inOutDirty);
            }

            private final Canvas internalLockCanvas(Rect dirty) {
                SurfaceView.this.mSurfaceLock.lock();
                Canvas c = null;
                if (!SurfaceView.this.mDrawingStopped && SurfaceView.this.mWindow != null) {
                    try {
                        c = SurfaceView.this.mSurface.lockCanvas(dirty);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception locking surface", e);
                    }
                }
                if (c != null) {
                    SurfaceView.this.mLastLockTime = SystemClock.uptimeMillis();
                    return c;
                }
                long now = SystemClock.uptimeMillis();
                long nextTime = SurfaceView.this.mLastLockTime + 100;
                if (nextTime > now) {
                    try {
                        Thread.sleep(nextTime - now);
                    } catch (InterruptedException e2) {
                    }
                    now = SystemClock.uptimeMillis();
                }
                SurfaceView.this.mLastLockTime = now;
                SurfaceView.this.mSurfaceLock.unlock();
                return null;
            }

            @Override
            public void unlockCanvasAndPost(Canvas canvas) {
                if (canvas == null) {
                    Log.d(SurfaceView.TAG, "should not unlockCanvasAndPost with canvas: nulll");
                } else {
                    SurfaceView.this.mSurface.unlockCanvasAndPost(canvas);
                    SurfaceView.this.mSurfaceLock.unlock();
                }
            }

            @Override
            public Surface getSurface() {
                return SurfaceView.this.mSurface;
            }

            @Override
            public Rect getSurfaceFrame() {
                return SurfaceView.this.mSurfaceFrame;
            }
        };
        init();
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mCallbacks = new ArrayList<>();
        this.mLocation = new int[2];
        this.mSurfaceLock = new ReentrantLock();
        this.mSurface = new Surface();
        this.mNewSurface = new Surface();
        this.mDrawingStopped = true;
        this.mLayout = new WindowManager.LayoutParams();
        this.mVisibleInsets = new Rect();
        this.mWinFrame = new Rect();
        this.mOverscanInsets = new Rect();
        this.mContentInsets = new Rect();
        this.mStableInsets = new Rect();
        this.mConfiguration = new Configuration();
        this.mWindowType = 1001;
        this.mIsCreating = false;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        SurfaceView.this.setKeepScreenOn(msg.arg1 != 0);
                        break;
                    case 2:
                        SurfaceView.this.handleGetNewSurface();
                        break;
                    case 3:
                        SurfaceView.this.updateWindow(false, false);
                        break;
                }
            }
        };
        this.mScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                SurfaceView.this.updateWindow(false, false);
            }
        };
        this.mRequestedVisible = false;
        this.mWindowVisibility = false;
        this.mViewVisibility = false;
        this.mRequestedWidth = -1;
        this.mRequestedHeight = -1;
        this.mRequestedFormat = 4;
        this.mHaveFrame = false;
        this.mSurfaceCreated = false;
        this.mLastLockTime = 0L;
        this.mVisible = false;
        this.mLeft = -1;
        this.mTop = -1;
        this.mWidth = -1;
        this.mHeight = -1;
        this.mFormat = -1;
        this.mSurfaceFrame = new Rect();
        this.mLastSurfaceWidth = -1;
        this.mLastSurfaceHeight = -1;
        this.mDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                SurfaceView.this.mHaveFrame = SurfaceView.this.getWidth() > 0 && SurfaceView.this.getHeight() > 0;
                SurfaceView.this.updateWindow(false, false);
                return true;
            }
        };
        this.mSurfaceHolder = new SurfaceHolder() {
            private static final String LOG_TAG = "SurfaceHolder";

            @Override
            public boolean isCreating() {
                return SurfaceView.this.mIsCreating;
            }

            @Override
            public void addCallback(SurfaceHolder.Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    if (!SurfaceView.this.mCallbacks.contains(callback)) {
                        SurfaceView.this.mCallbacks.add(callback);
                    }
                }
            }

            @Override
            public void removeCallback(SurfaceHolder.Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    SurfaceView.this.mCallbacks.remove(callback);
                }
            }

            @Override
            public void setFixedSize(int width, int height) {
                if (SurfaceView.this.mRequestedWidth != width || SurfaceView.this.mRequestedHeight != height) {
                    SurfaceView.this.mRequestedWidth = width;
                    SurfaceView.this.mRequestedHeight = height;
                    SurfaceView.this.requestLayout();
                }
            }

            @Override
            public void setSizeFromLayout() {
                if (SurfaceView.this.mRequestedWidth != -1 || SurfaceView.this.mRequestedHeight != -1) {
                    SurfaceView surfaceView = SurfaceView.this;
                    SurfaceView.this.mRequestedHeight = -1;
                    surfaceView.mRequestedWidth = -1;
                    SurfaceView.this.requestLayout();
                }
            }

            @Override
            public void setFormat(int format) {
                if (format == -1) {
                    format = 4;
                }
                SurfaceView.this.mRequestedFormat = format;
                if (SurfaceView.this.mWindow != null) {
                    SurfaceView.this.updateWindow(false, false);
                }
            }

            @Override
            @Deprecated
            public void setType(int type) {
            }

            @Override
            public void setKeepScreenOn(boolean screenOn) {
                Message msg = SurfaceView.this.mHandler.obtainMessage(1);
                msg.arg1 = screenOn ? 1 : 0;
                SurfaceView.this.mHandler.sendMessage(msg);
            }

            @Override
            public Canvas lockCanvas() {
                return internalLockCanvas(null);
            }

            @Override
            public Canvas lockCanvas(Rect inOutDirty) {
                return internalLockCanvas(inOutDirty);
            }

            private final Canvas internalLockCanvas(Rect dirty) {
                SurfaceView.this.mSurfaceLock.lock();
                Canvas c = null;
                if (!SurfaceView.this.mDrawingStopped && SurfaceView.this.mWindow != null) {
                    try {
                        c = SurfaceView.this.mSurface.lockCanvas(dirty);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception locking surface", e);
                    }
                }
                if (c != null) {
                    SurfaceView.this.mLastLockTime = SystemClock.uptimeMillis();
                    return c;
                }
                long now = SystemClock.uptimeMillis();
                long nextTime = SurfaceView.this.mLastLockTime + 100;
                if (nextTime > now) {
                    try {
                        Thread.sleep(nextTime - now);
                    } catch (InterruptedException e2) {
                    }
                    now = SystemClock.uptimeMillis();
                }
                SurfaceView.this.mLastLockTime = now;
                SurfaceView.this.mSurfaceLock.unlock();
                return null;
            }

            @Override
            public void unlockCanvasAndPost(Canvas canvas) {
                if (canvas == null) {
                    Log.d(SurfaceView.TAG, "should not unlockCanvasAndPost with canvas: nulll");
                } else {
                    SurfaceView.this.mSurface.unlockCanvasAndPost(canvas);
                    SurfaceView.this.mSurfaceLock.unlock();
                }
            }

            @Override
            public Surface getSurface() {
                return SurfaceView.this.mSurface;
            }

            @Override
            public Rect getSurfaceFrame() {
                return SurfaceView.this.mSurfaceFrame;
            }
        };
        init();
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mCallbacks = new ArrayList<>();
        this.mLocation = new int[2];
        this.mSurfaceLock = new ReentrantLock();
        this.mSurface = new Surface();
        this.mNewSurface = new Surface();
        this.mDrawingStopped = true;
        this.mLayout = new WindowManager.LayoutParams();
        this.mVisibleInsets = new Rect();
        this.mWinFrame = new Rect();
        this.mOverscanInsets = new Rect();
        this.mContentInsets = new Rect();
        this.mStableInsets = new Rect();
        this.mConfiguration = new Configuration();
        this.mWindowType = 1001;
        this.mIsCreating = false;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        SurfaceView.this.setKeepScreenOn(msg.arg1 != 0);
                        break;
                    case 2:
                        SurfaceView.this.handleGetNewSurface();
                        break;
                    case 3:
                        SurfaceView.this.updateWindow(false, false);
                        break;
                }
            }
        };
        this.mScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                SurfaceView.this.updateWindow(false, false);
            }
        };
        this.mRequestedVisible = false;
        this.mWindowVisibility = false;
        this.mViewVisibility = false;
        this.mRequestedWidth = -1;
        this.mRequestedHeight = -1;
        this.mRequestedFormat = 4;
        this.mHaveFrame = false;
        this.mSurfaceCreated = false;
        this.mLastLockTime = 0L;
        this.mVisible = false;
        this.mLeft = -1;
        this.mTop = -1;
        this.mWidth = -1;
        this.mHeight = -1;
        this.mFormat = -1;
        this.mSurfaceFrame = new Rect();
        this.mLastSurfaceWidth = -1;
        this.mLastSurfaceHeight = -1;
        this.mDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                SurfaceView.this.mHaveFrame = SurfaceView.this.getWidth() > 0 && SurfaceView.this.getHeight() > 0;
                SurfaceView.this.updateWindow(false, false);
                return true;
            }
        };
        this.mSurfaceHolder = new SurfaceHolder() {
            private static final String LOG_TAG = "SurfaceHolder";

            @Override
            public boolean isCreating() {
                return SurfaceView.this.mIsCreating;
            }

            @Override
            public void addCallback(SurfaceHolder.Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    if (!SurfaceView.this.mCallbacks.contains(callback)) {
                        SurfaceView.this.mCallbacks.add(callback);
                    }
                }
            }

            @Override
            public void removeCallback(SurfaceHolder.Callback callback) {
                synchronized (SurfaceView.this.mCallbacks) {
                    SurfaceView.this.mCallbacks.remove(callback);
                }
            }

            @Override
            public void setFixedSize(int width, int height) {
                if (SurfaceView.this.mRequestedWidth != width || SurfaceView.this.mRequestedHeight != height) {
                    SurfaceView.this.mRequestedWidth = width;
                    SurfaceView.this.mRequestedHeight = height;
                    SurfaceView.this.requestLayout();
                }
            }

            @Override
            public void setSizeFromLayout() {
                if (SurfaceView.this.mRequestedWidth != -1 || SurfaceView.this.mRequestedHeight != -1) {
                    SurfaceView surfaceView = SurfaceView.this;
                    SurfaceView.this.mRequestedHeight = -1;
                    surfaceView.mRequestedWidth = -1;
                    SurfaceView.this.requestLayout();
                }
            }

            @Override
            public void setFormat(int format) {
                if (format == -1) {
                    format = 4;
                }
                SurfaceView.this.mRequestedFormat = format;
                if (SurfaceView.this.mWindow != null) {
                    SurfaceView.this.updateWindow(false, false);
                }
            }

            @Override
            @Deprecated
            public void setType(int type) {
            }

            @Override
            public void setKeepScreenOn(boolean screenOn) {
                Message msg = SurfaceView.this.mHandler.obtainMessage(1);
                msg.arg1 = screenOn ? 1 : 0;
                SurfaceView.this.mHandler.sendMessage(msg);
            }

            @Override
            public Canvas lockCanvas() {
                return internalLockCanvas(null);
            }

            @Override
            public Canvas lockCanvas(Rect inOutDirty) {
                return internalLockCanvas(inOutDirty);
            }

            private final Canvas internalLockCanvas(Rect dirty) {
                SurfaceView.this.mSurfaceLock.lock();
                Canvas c = null;
                if (!SurfaceView.this.mDrawingStopped && SurfaceView.this.mWindow != null) {
                    try {
                        c = SurfaceView.this.mSurface.lockCanvas(dirty);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception locking surface", e);
                    }
                }
                if (c != null) {
                    SurfaceView.this.mLastLockTime = SystemClock.uptimeMillis();
                    return c;
                }
                long now = SystemClock.uptimeMillis();
                long nextTime = SurfaceView.this.mLastLockTime + 100;
                if (nextTime > now) {
                    try {
                        Thread.sleep(nextTime - now);
                    } catch (InterruptedException e2) {
                    }
                    now = SystemClock.uptimeMillis();
                }
                SurfaceView.this.mLastLockTime = now;
                SurfaceView.this.mSurfaceLock.unlock();
                return null;
            }

            @Override
            public void unlockCanvasAndPost(Canvas canvas) {
                if (canvas == null) {
                    Log.d(SurfaceView.TAG, "should not unlockCanvasAndPost with canvas: nulll");
                } else {
                    SurfaceView.this.mSurface.unlockCanvasAndPost(canvas);
                    SurfaceView.this.mSurfaceLock.unlock();
                }
            }

            @Override
            public Surface getSurface() {
                return SurfaceView.this.mSurface;
            }

            @Override
            public Rect getSurfaceFrame() {
                return SurfaceView.this.mSurfaceFrame;
            }
        };
        init();
    }

    private void init() {
        setWillNotDraw(true);
    }

    public SurfaceHolder getHolder() {
        return this.mSurfaceHolder;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mParent.requestTransparentRegion(this);
        this.mSession = getWindowSession();
        this.mLayout.token = getWindowToken();
        this.mLayout.setTitle(TAG);
        this.mViewVisibility = getVisibility() == 0;
        if (!this.mGlobalListenersAdded) {
            ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnScrollChangedListener(this.mScrollChangedListener);
            observer.addOnPreDrawListener(this.mDrawListener);
            this.mGlobalListenersAdded = true;
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        this.mWindowVisibility = visibility == 0;
        this.mRequestedVisible = this.mWindowVisibility && this.mViewVisibility;
        updateWindow(false, false);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        this.mViewVisibility = visibility == 0;
        boolean newRequestedVisible = this.mWindowVisibility && this.mViewVisibility;
        if (newRequestedVisible != this.mRequestedVisible) {
            requestLayout();
        }
        this.mRequestedVisible = newRequestedVisible;
        updateWindow(false, false);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.mGlobalListenersAdded) {
            ViewTreeObserver observer = getViewTreeObserver();
            observer.removeOnScrollChangedListener(this.mScrollChangedListener);
            observer.removeOnPreDrawListener(this.mDrawListener);
            this.mGlobalListenersAdded = false;
        }
        this.mRequestedVisible = false;
        updateWindow(false, false);
        this.mHaveFrame = false;
        if (this.mWindow != null) {
            try {
                this.mSession.remove(this.mWindow);
            } catch (RemoteException e) {
            }
            this.mWindow = null;
        }
        this.mSession = null;
        this.mLayout.token = null;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = this.mRequestedWidth >= 0 ? resolveSizeAndState(this.mRequestedWidth, widthMeasureSpec, 0) : getDefaultSize(0, widthMeasureSpec);
        int height = this.mRequestedHeight >= 0 ? resolveSizeAndState(this.mRequestedHeight, heightMeasureSpec, 0) : getDefaultSize(0, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        boolean result = super.setFrame(left, top, right, bottom);
        updateWindow(false, false);
        return result;
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        if (this.mWindowType == 1000) {
            return super.gatherTransparentRegion(region);
        }
        boolean opaque = true;
        if ((this.mPrivateFlags & 128) == 0) {
            opaque = super.gatherTransparentRegion(region);
        } else if (region != null) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                getLocationInWindow(this.mLocation);
                int l = this.mLocation[0];
                int t = this.mLocation[1];
                region.op(l, t, l + w, t + h, Region.Op.UNION);
            }
        }
        if (PixelFormat.formatHasAlpha(this.mRequestedFormat)) {
            return false;
        }
        return opaque;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mWindowType != 1000 && (this.mPrivateFlags & 128) == 0) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (this.mWindowType != 1000 && (this.mPrivateFlags & 128) == 128) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        super.dispatchDraw(canvas);
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay) {
        this.mWindowType = isMediaOverlay ? 1004 : 1001;
    }

    public void setZOrderOnTop(boolean onTop) {
        if (onTop) {
            this.mWindowType = 1000;
            this.mLayout.flags |= 131072;
        } else {
            this.mWindowType = 1001;
            this.mLayout.flags &= -131073;
        }
    }

    public void setSecure(boolean isSecure) {
        if (isSecure) {
            this.mLayout.flags |= 8192;
        } else {
            this.mLayout.flags &= -8193;
        }
    }

    public void setWindowType(int type) {
        this.mWindowType = type;
    }

    protected void updateWindow(boolean force, boolean redrawNeeded) {
        if (this.mHaveFrame) {
            ViewRootImpl viewRoot = getViewRootImpl();
            if (viewRoot != null) {
                this.mTranslator = viewRoot.mTranslator;
            }
            if (this.mTranslator != null) {
                this.mSurface.setCompatibilityTranslator(this.mTranslator);
            }
            int myWidth = this.mRequestedWidth;
            if (myWidth <= 0) {
                myWidth = getWidth();
            }
            int myHeight = this.mRequestedHeight;
            if (myHeight <= 0) {
                myHeight = getHeight();
            }
            getLocationInWindow(this.mLocation);
            boolean creating = this.mWindow == null;
            boolean formatChanged = this.mFormat != this.mRequestedFormat;
            boolean sizeChanged = (this.mWidth == myWidth && this.mHeight == myHeight) ? false : true;
            boolean visibleChanged = this.mVisible != this.mRequestedVisible;
            if (force || creating || formatChanged || sizeChanged || visibleChanged || this.mLeft != this.mLocation[0] || this.mTop != this.mLocation[1] || this.mUpdateWindowNeeded || this.mReportDrawNeeded || redrawNeeded) {
                try {
                    boolean visible = this.mRequestedVisible;
                    this.mVisible = visible;
                    this.mLeft = this.mLocation[0];
                    this.mTop = this.mLocation[1];
                    this.mWidth = myWidth;
                    this.mHeight = myHeight;
                    this.mFormat = this.mRequestedFormat;
                    this.mLayout.x = this.mLeft;
                    this.mLayout.y = this.mTop;
                    this.mLayout.width = getWidth();
                    this.mLayout.height = getHeight();
                    if (this.mTranslator != null) {
                        this.mTranslator.translateLayoutParamsInAppWindowToScreen(this.mLayout);
                    }
                    this.mLayout.format = this.mRequestedFormat;
                    this.mLayout.flags |= 16920;
                    if (!getContext().getResources().getCompatibilityInfo().supportsScreen()) {
                        this.mLayout.privateFlags |= 128;
                    }
                    this.mLayout.privateFlags |= 64;
                    if (this.mWindow == null) {
                        Display display = getDisplay();
                        this.mWindow = new MyWindow(this);
                        this.mLayout.type = this.mWindowType;
                        this.mLayout.gravity = 8388659;
                        this.mSession.addToDisplayWithoutInputChannel(this.mWindow, this.mWindow.mSeq, this.mLayout, this.mVisible ? 0 : 8, display.getDisplayId(), this.mContentInsets, this.mStableInsets);
                    }
                    this.mSurfaceLock.lock();
                    try {
                        this.mUpdateWindowNeeded = false;
                        boolean reportDrawNeeded = this.mReportDrawNeeded;
                        this.mReportDrawNeeded = false;
                        this.mDrawingStopped = !visible;
                        int relayoutResult = this.mSession.relayout(this.mWindow, this.mWindow.mSeq, this.mLayout, this.mWidth, this.mHeight, visible ? 0 : 8, 2, this.mWinFrame, this.mOverscanInsets, this.mContentInsets, this.mVisibleInsets, this.mStableInsets, this.mConfiguration, this.mNewSurface);
                        if ((relayoutResult & 2) != 0) {
                            this.mReportDrawNeeded = true;
                        }
                        this.mSurfaceFrame.left = 0;
                        this.mSurfaceFrame.top = 0;
                        if (this.mTranslator == null) {
                            this.mSurfaceFrame.right = this.mWinFrame.width();
                            this.mSurfaceFrame.bottom = this.mWinFrame.height();
                        } else {
                            float appInvertedScale = this.mTranslator.applicationInvertedScale;
                            this.mSurfaceFrame.right = (int) ((this.mWinFrame.width() * appInvertedScale) + 0.5f);
                            this.mSurfaceFrame.bottom = (int) ((this.mWinFrame.height() * appInvertedScale) + 0.5f);
                        }
                        int surfaceWidth = this.mSurfaceFrame.right;
                        int surfaceHeight = this.mSurfaceFrame.bottom;
                        boolean realSizeChanged = (this.mLastSurfaceWidth == surfaceWidth && this.mLastSurfaceHeight == surfaceHeight) ? false : true;
                        this.mLastSurfaceWidth = surfaceWidth;
                        this.mLastSurfaceHeight = surfaceHeight;
                        boolean redrawNeeded2 = redrawNeeded | creating | reportDrawNeeded;
                        SurfaceHolder.Callback[] callbacks = null;
                        boolean surfaceChanged = (relayoutResult & 4) != 0;
                        try {
                            if (this.mSurfaceCreated && (surfaceChanged || (!visible && visibleChanged))) {
                                this.mSurfaceCreated = false;
                                if (this.mSurface.isValid()) {
                                    callbacks = getSurfaceCallbacks();
                                    for (SurfaceHolder.Callback callback : callbacks) {
                                        callback.surfaceDestroyed(this.mSurfaceHolder);
                                    }
                                }
                            }
                            this.mSurface.transferFrom(this.mNewSurface);
                            if (visible && this.mSurface.isValid()) {
                                if (!this.mSurfaceCreated && (surfaceChanged || visibleChanged)) {
                                    this.mSurfaceCreated = true;
                                    this.mIsCreating = true;
                                    if (callbacks == null) {
                                        callbacks = getSurfaceCallbacks();
                                    }
                                    for (SurfaceHolder.Callback callback2 : callbacks) {
                                        callback2.surfaceCreated(this.mSurfaceHolder);
                                    }
                                }
                                if (creating || formatChanged || sizeChanged || visibleChanged || realSizeChanged) {
                                    if (callbacks == null) {
                                        callbacks = getSurfaceCallbacks();
                                    }
                                    for (SurfaceHolder.Callback callback3 : callbacks) {
                                        callback3.surfaceChanged(this.mSurfaceHolder, this.mFormat, myWidth, myHeight);
                                    }
                                }
                                if (redrawNeeded2) {
                                    if (callbacks == null) {
                                        callbacks = getSurfaceCallbacks();
                                    }
                                    SurfaceHolder.Callback[] arr$ = callbacks;
                                    for (SurfaceHolder.Callback c : arr$) {
                                        if (c instanceof SurfaceHolder.Callback2) {
                                            ((SurfaceHolder.Callback2) c).surfaceRedrawNeeded(this.mSurfaceHolder);
                                        }
                                    }
                                }
                            }
                            this.mIsCreating = false;
                            if (redrawNeeded2) {
                                this.mSession.finishDrawing(this.mWindow);
                            }
                            this.mSession.performDeferredDestroy(this.mWindow);
                        } catch (Throwable th) {
                            this.mIsCreating = false;
                            if (redrawNeeded2) {
                                this.mSession.finishDrawing(this.mWindow);
                            }
                            this.mSession.performDeferredDestroy(this.mWindow);
                            throw th;
                        }
                    } finally {
                        this.mSurfaceLock.unlock();
                    }
                } catch (RemoteException e) {
                }
            }
        }
    }

    private SurfaceHolder.Callback[] getSurfaceCallbacks() {
        SurfaceHolder.Callback[] callbacks;
        synchronized (this.mCallbacks) {
            callbacks = new SurfaceHolder.Callback[this.mCallbacks.size()];
            this.mCallbacks.toArray(callbacks);
        }
        return callbacks;
    }

    void handleGetNewSurface() {
        updateWindow(false, false);
    }

    public boolean isFixedSize() {
        return (this.mRequestedWidth == -1 && this.mRequestedHeight == -1) ? false : true;
    }

    private static class MyWindow extends BaseIWindow {
        private final WeakReference<SurfaceView> mSurfaceView;
        int mCurWidth = -1;
        int mCurHeight = -1;

        public MyWindow(SurfaceView surfaceView) {
            this.mSurfaceView = new WeakReference<>(surfaceView);
        }

        @Override
        public void resized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, boolean reportDraw, Configuration newConfig) {
            SurfaceView surfaceView = this.mSurfaceView.get();
            if (surfaceView != null) {
                surfaceView.mSurfaceLock.lock();
                try {
                    if (reportDraw) {
                        surfaceView.mUpdateWindowNeeded = true;
                        surfaceView.mReportDrawNeeded = true;
                        surfaceView.mHandler.sendEmptyMessage(3);
                    } else if (surfaceView.mWinFrame.width() != frame.width() || surfaceView.mWinFrame.height() != frame.height()) {
                        surfaceView.mUpdateWindowNeeded = true;
                        surfaceView.mHandler.sendEmptyMessage(3);
                    }
                } finally {
                    surfaceView.mSurfaceLock.unlock();
                }
            }
        }

        @Override
        public void dispatchAppVisibility(boolean visible) {
        }

        @Override
        public void dispatchGetNewSurface() {
            SurfaceView surfaceView = this.mSurfaceView.get();
            if (surfaceView != null) {
                Message msg = surfaceView.mHandler.obtainMessage(2);
                surfaceView.mHandler.sendMessage(msg);
            }
        }

        @Override
        public void windowFocusChanged(boolean hasFocus, boolean touchEnabled) {
            Log.w(SurfaceView.TAG, "Unexpected focus in surface: focus=" + hasFocus + ", touchEnabled=" + touchEnabled);
        }

        @Override
        public void executeCommand(String command, String parameters, ParcelFileDescriptor out) {
        }
    }
}
