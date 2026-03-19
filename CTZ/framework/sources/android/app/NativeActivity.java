package android.app;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.AttributeSet;
import android.view.InputQueue;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import dalvik.system.BaseDexClassLoader;
import java.io.File;

public class NativeActivity extends Activity implements SurfaceHolder.Callback2, InputQueue.Callback, ViewTreeObserver.OnGlobalLayoutListener {
    private static final String KEY_NATIVE_SAVED_STATE = "android:native_state";
    public static final String META_DATA_FUNC_NAME = "android.app.func_name";
    public static final String META_DATA_LIB_NAME = "android.app.lib_name";
    private InputQueue mCurInputQueue;
    private SurfaceHolder mCurSurfaceHolder;
    private boolean mDestroyed;
    private boolean mDispatchingUnhandledKey;
    private InputMethodManager mIMM;
    int mLastContentHeight;
    int mLastContentWidth;
    int mLastContentX;
    int mLastContentY;
    final int[] mLocation = new int[2];
    private NativeContentView mNativeContentView;
    private long mNativeHandle;

    private native String getDlError();

    private native long loadNativeCode(String str, String str2, MessageQueue messageQueue, String str3, String str4, String str5, int i, AssetManager assetManager, byte[] bArr, ClassLoader classLoader, String str6);

    private native void onConfigurationChangedNative(long j);

    private native void onContentRectChangedNative(long j, int i, int i2, int i3, int i4);

    private native void onInputQueueCreatedNative(long j, long j2);

    private native void onInputQueueDestroyedNative(long j, long j2);

    private native void onLowMemoryNative(long j);

    private native void onPauseNative(long j);

    private native void onResumeNative(long j);

    private native byte[] onSaveInstanceStateNative(long j);

    private native void onStartNative(long j);

    private native void onStopNative(long j);

    private native void onSurfaceChangedNative(long j, Surface surface, int i, int i2, int i3);

    private native void onSurfaceCreatedNative(long j, Surface surface);

    private native void onSurfaceDestroyedNative(long j);

    private native void onSurfaceRedrawNeededNative(long j, Surface surface);

    private native void onWindowFocusChangedNative(long j, boolean z);

    private native void unloadNativeCode(long j);

    static class NativeContentView extends View {
        NativeActivity mActivity;

        public NativeContentView(Context context) {
            super(context);
        }

        public NativeContentView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        String str;
        byte[] byteArray;
        String str2 = "main";
        this.mIMM = (InputMethodManager) getSystemService(InputMethodManager.class);
        getWindow().takeSurface(this);
        getWindow().takeInputQueue(this);
        getWindow().setFormat(4);
        getWindow().setSoftInputMode(16);
        this.mNativeContentView = new NativeContentView(this);
        this.mNativeContentView.mActivity = this;
        setContentView(this.mNativeContentView);
        this.mNativeContentView.requestFocus();
        this.mNativeContentView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        try {
            ActivityInfo activityInfo = getPackageManager().getActivityInfo(getIntent().getComponent(), 128);
            if (activityInfo.metaData == null) {
                str = "ANativeActivity_onCreate";
            } else {
                String string = activityInfo.metaData.getString(META_DATA_LIB_NAME);
                if (string != null) {
                    str2 = string;
                }
                String string2 = activityInfo.metaData.getString(META_DATA_FUNC_NAME);
                if (string2 != null) {
                    str = string2;
                }
            }
            BaseDexClassLoader baseDexClassLoader = (BaseDexClassLoader) getClassLoader();
            String strFindLibrary = baseDexClassLoader.findLibrary(str2);
            if (strFindLibrary == null) {
                throw new IllegalArgumentException("Unable to find native library " + str2 + " using classloader: " + baseDexClassLoader.toString());
            }
            if (bundle != null) {
                byteArray = bundle.getByteArray(KEY_NATIVE_SAVED_STATE);
            } else {
                byteArray = null;
            }
            this.mNativeHandle = loadNativeCode(strFindLibrary, str, Looper.myQueue(), getAbsolutePath(getFilesDir()), getAbsolutePath(getObbDir()), getAbsolutePath(getExternalFilesDir(null)), Build.VERSION.SDK_INT, getAssets(), byteArray, baseDexClassLoader, baseDexClassLoader.getLdLibraryPath());
            if (this.mNativeHandle == 0) {
                throw new UnsatisfiedLinkError("Unable to load native library \"" + strFindLibrary + "\": " + getDlError());
            }
            super.onCreate(bundle);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Error getting activity info", e);
        }
    }

    private static String getAbsolutePath(File file) {
        if (file != null) {
            return file.getAbsolutePath();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        this.mDestroyed = true;
        if (this.mCurSurfaceHolder != null) {
            onSurfaceDestroyedNative(this.mNativeHandle);
            this.mCurSurfaceHolder = null;
        }
        if (this.mCurInputQueue != null) {
            onInputQueueDestroyedNative(this.mNativeHandle, this.mCurInputQueue.getNativePtr());
            this.mCurInputQueue = null;
        }
        unloadNativeCode(this.mNativeHandle);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        onPauseNative(this.mNativeHandle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onResumeNative(this.mNativeHandle);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        byte[] bArrOnSaveInstanceStateNative = onSaveInstanceStateNative(this.mNativeHandle);
        if (bArrOnSaveInstanceStateNative != null) {
            bundle.putByteArray(KEY_NATIVE_SAVED_STATE, bArrOnSaveInstanceStateNative);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        onStartNative(this.mNativeHandle);
    }

    @Override
    protected void onStop() {
        super.onStop();
        onStopNative(this.mNativeHandle);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (!this.mDestroyed) {
            onConfigurationChangedNative(this.mNativeHandle);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!this.mDestroyed) {
            onLowMemoryNative(this.mNativeHandle);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (!this.mDestroyed) {
            onWindowFocusChangedNative(this.mNativeHandle, z);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (!this.mDestroyed) {
            this.mCurSurfaceHolder = surfaceHolder;
            onSurfaceCreatedNative(this.mNativeHandle, surfaceHolder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        if (!this.mDestroyed) {
            this.mCurSurfaceHolder = surfaceHolder;
            onSurfaceChangedNative(this.mNativeHandle, surfaceHolder.getSurface(), i, i2, i3);
        }
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder surfaceHolder) {
        if (!this.mDestroyed) {
            this.mCurSurfaceHolder = surfaceHolder;
            onSurfaceRedrawNeededNative(this.mNativeHandle, surfaceHolder.getSurface());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.mCurSurfaceHolder = null;
        if (!this.mDestroyed) {
            onSurfaceDestroyedNative(this.mNativeHandle);
        }
    }

    @Override
    public void onInputQueueCreated(InputQueue inputQueue) {
        if (!this.mDestroyed) {
            this.mCurInputQueue = inputQueue;
            onInputQueueCreatedNative(this.mNativeHandle, inputQueue.getNativePtr());
        }
    }

    @Override
    public void onInputQueueDestroyed(InputQueue inputQueue) {
        if (!this.mDestroyed) {
            onInputQueueDestroyedNative(this.mNativeHandle, inputQueue.getNativePtr());
            this.mCurInputQueue = null;
        }
    }

    @Override
    public void onGlobalLayout() {
        this.mNativeContentView.getLocationInWindow(this.mLocation);
        int width = this.mNativeContentView.getWidth();
        int height = this.mNativeContentView.getHeight();
        if (this.mLocation[0] != this.mLastContentX || this.mLocation[1] != this.mLastContentY || width != this.mLastContentWidth || height != this.mLastContentHeight) {
            this.mLastContentX = this.mLocation[0];
            this.mLastContentY = this.mLocation[1];
            this.mLastContentWidth = width;
            this.mLastContentHeight = height;
            if (!this.mDestroyed) {
                onContentRectChangedNative(this.mNativeHandle, this.mLastContentX, this.mLastContentY, this.mLastContentWidth, this.mLastContentHeight);
            }
        }
    }

    void setWindowFlags(int i, int i2) {
        getWindow().setFlags(i, i2);
    }

    void setWindowFormat(int i) {
        getWindow().setFormat(i);
    }

    void showIme(int i) {
        this.mIMM.showSoftInput(this.mNativeContentView, i);
    }

    void hideIme(int i) {
        this.mIMM.hideSoftInputFromWindow(this.mNativeContentView.getWindowToken(), i);
    }
}
