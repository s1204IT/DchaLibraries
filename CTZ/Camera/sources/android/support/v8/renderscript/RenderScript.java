package android.support.v8.renderscript;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v8.renderscript.Element;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RenderScript {
    static Method registerNativeAllocation;
    static Method registerNativeFree;
    static boolean sInitialized;
    static int sPointerSize;
    static Object sRuntime;
    static boolean sUseGCHooks;
    private static boolean useNative;
    private Context mApplicationContext;
    long mContext;
    Element mElement_A_8;
    Element mElement_RGBA_4444;
    Element mElement_RGBA_8888;
    Element mElement_RGB_565;
    Element mElement_U8;
    Element mElement_UCHAR_4;
    long mIncCon;
    boolean mIncLoaded;
    MessageThread mMessageThread;
    private String mNativeLibDir;
    ReentrantReadWriteLock mRWLock;
    private static ArrayList<RenderScript> mProcessContextList = new ArrayList<>();
    private static String mBlackList = "";
    static Object lock = new Object();
    private static int sNative = -1;
    private static int sSdkVersion = -1;
    private static boolean useIOlib = false;
    private boolean mIsProcessContext = false;
    private boolean mEnableMultiInput = false;
    private int mDispatchAPILevel = 0;
    private int mContextFlags = 0;
    private int mContextSdkVersion = 0;
    private boolean mDestroyed = false;
    RSMessageHandler mMessageCallback = null;
    RSErrorHandler mErrorCallback = null;
    ContextType mContextType = ContextType.NORMAL;

    static native int rsnSystemGetPointerSize();

    native void nContextDeinitToClient(long j);

    native String nContextGetErrorMessage(long j);

    native int nContextGetUserMessage(long j, int[] iArr);

    native void nContextInitToClient(long j);

    native int nContextPeekMessage(long j, int[] iArr);

    native long nDeviceCreate();

    native long nIncDeviceCreate();

    native boolean nIncLoadSO(int i, String str);

    native boolean nLoadIOSO();

    native boolean nLoadSO(boolean z, int i, String str);

    native void rsnAllocationCopyToBitmap(long j, long j2, Bitmap bitmap);

    native long rsnAllocationCreateBitmapBackedAllocation(long j, long j2, int i, Bitmap bitmap, int i2);

    native long rsnAllocationCreateFromBitmap(long j, long j2, int i, Bitmap bitmap, int i2);

    native long rsnAllocationCreateTyped(long j, long j2, int i, int i2, long j3);

    native void rsnAllocationData1D(long j, long j2, int i, int i2, int i3, Object obj, int i4, int i5, int i6, boolean z);

    native void rsnAllocationData2D(long j, long j2, int i, int i2, int i3, int i4, int i5, int i6, Object obj, int i7, int i8, int i9, boolean z);

    native void rsnAllocationData3D(long j, long j2, int i, int i2, int i3, int i4, int i5, int i6, int i7, Object obj, int i8, int i9, int i10, boolean z);

    native long rsnContextCreate(long j, int i, int i2, int i3, String str);

    native void rsnContextDestroy(long j);

    native void rsnContextFinish(long j);

    native long rsnElementCreate(long j, long j2, int i, boolean z, int i2);

    native long rsnIncAllocationCreateTyped(long j, long j2, long j3, long j4, int i);

    native long rsnIncContextCreate(long j, int i, int i2, int i3);

    native void rsnIncContextDestroy(long j);

    native void rsnIncContextFinish(long j);

    native long rsnIncElementCreate(long j, long j2, int i, boolean z, int i2);

    native long rsnIncTypeCreate(long j, long j2, int i, int i2, int i3, boolean z, boolean z2, int i4);

    native void rsnObjDestroy(long j, long j2);

    native void rsnScriptForEach(long j, long j2, long j3, int i, long j4, long j5, boolean z);

    native void rsnScriptForEach(long j, long j2, long j3, int i, long j4, long j5, byte[] bArr, boolean z);

    native long rsnScriptIntrinsicCreate(long j, int i, long j2, boolean z);

    native void rsnScriptSetVarF(long j, long j2, int i, float f, boolean z);

    native void rsnScriptSetVarObj(long j, long j2, int i, long j3, boolean z);

    native long rsnTypeCreate(long j, long j2, int i, int i2, int i3, boolean z, boolean z2, int i4);

    boolean isUseNative() {
        return useNative;
    }

    private static boolean setupNative(int i, Context context) {
        int iIntValue;
        long jLongValue;
        if (Build.VERSION.SDK_INT < i && Build.VERSION.SDK_INT < 21) {
            sNative = 0;
        }
        if (sNative == -1) {
            try {
                iIntValue = ((Integer) Class.forName("android.os.SystemProperties").getDeclaredMethod("getInt", String.class, Integer.TYPE).invoke(null, "debug.rs.forcecompat", new Integer(0))).intValue();
            } catch (Exception e) {
                iIntValue = 0;
            }
            if (Build.VERSION.SDK_INT >= 19 && iIntValue == 0) {
                sNative = 1;
            } else {
                sNative = 0;
            }
            if (sNative == 1) {
                try {
                    ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
                    try {
                        jLongValue = ((Long) Class.forName("android.renderscript.RenderScript").getDeclaredMethod("getMinorID", new Class[0]).invoke(null, new Object[0])).longValue();
                    } catch (Exception e2) {
                        jLongValue = 0;
                    }
                    if (((PackageItemInfo) applicationInfo).metaData != null) {
                        if (((PackageItemInfo) applicationInfo).metaData.getBoolean("com.android.support.v8.renderscript.EnableAsyncTeardown") && jLongValue == 0) {
                            sNative = 0;
                        }
                        if (((PackageItemInfo) applicationInfo).metaData.getBoolean("com.android.support.v8.renderscript.EnableBlurWorkaround") && Build.VERSION.SDK_INT <= 19) {
                            sNative = 0;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e3) {
                    return true;
                }
            }
        }
        if (sNative != 1) {
            return false;
        }
        if (mBlackList.length() > 0) {
            if (mBlackList.contains('(' + Build.MANUFACTURER + ':' + Build.PRODUCT + ':' + Build.MODEL + ')')) {
                sNative = 0;
                return false;
            }
        }
        return true;
    }

    public enum ContextType {
        NORMAL(0),
        DEBUG(1),
        PROFILE(2);

        int mID;

        ContextType(int i) {
            this.mID = i;
        }
    }

    synchronized long nContextCreate(long j, int i, int i2, int i3, String str) {
        return rsnContextCreate(j, i, i2, i3, str);
    }

    synchronized void nContextDestroy() {
        validate();
        ReentrantReadWriteLock.WriteLock writeLock = this.mRWLock.writeLock();
        writeLock.lock();
        long j = this.mContext;
        this.mContext = 0L;
        writeLock.unlock();
        rsnContextDestroy(j);
    }

    synchronized void nContextFinish() {
        validate();
        rsnContextFinish(this.mContext);
    }

    void nObjDestroy(long j) {
        if (this.mContext != 0) {
            rsnObjDestroy(this.mContext, j);
        }
    }

    synchronized long nElementCreate(long j, int i, boolean z, int i2) {
        validate();
        return rsnElementCreate(this.mContext, j, i, z, i2);
    }

    synchronized long nTypeCreate(long j, int i, int i2, int i3, boolean z, boolean z2, int i4) {
        validate();
        return rsnTypeCreate(this.mContext, j, i, i2, i3, z, z2, i4);
    }

    synchronized long nAllocationCreateTyped(long j, int i, int i2, long j2) {
        validate();
        return rsnAllocationCreateTyped(this.mContext, j, i, i2, j2);
    }

    synchronized long nAllocationCreateFromBitmap(long j, int i, Bitmap bitmap, int i2) {
        validate();
        return rsnAllocationCreateFromBitmap(this.mContext, j, i, bitmap, i2);
    }

    synchronized long nAllocationCreateBitmapBackedAllocation(long j, int i, Bitmap bitmap, int i2) {
        validate();
        return rsnAllocationCreateBitmapBackedAllocation(this.mContext, j, i, bitmap, i2);
    }

    synchronized void nAllocationCopyToBitmap(long j, Bitmap bitmap) {
        validate();
        rsnAllocationCopyToBitmap(this.mContext, j, bitmap);
    }

    synchronized void nAllocationData1D(long j, int i, int i2, int i3, Object obj, int i4, Element.DataType dataType, int i5, boolean z) {
        validate();
        rsnAllocationData1D(this.mContext, j, i, i2, i3, obj, i4, dataType.mID, i5, z);
    }

    synchronized void nAllocationData2D(long j, int i, int i2, int i3, int i4, int i5, int i6, Object obj, int i7, Element.DataType dataType, int i8, boolean z) throws Throwable {
        try {
            validate();
            try {
                rsnAllocationData2D(this.mContext, j, i, i2, i3, i4, i5, i6, obj, i7, dataType.mID, i8, z);
            } catch (Throwable th) {
                th = th;
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    synchronized void nAllocationData3D(long j, int i, int i2, int i3, int i4, int i5, int i6, int i7, Object obj, int i8, Element.DataType dataType, int i9, boolean z) throws Throwable {
        try {
            validate();
            try {
                rsnAllocationData3D(this.mContext, j, i, i2, i3, i4, i5, i6, i7, obj, i8, dataType.mID, i9, z);
            } catch (Throwable th) {
                th = th;
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    synchronized void nScriptForEach(long j, int i, long j2, long j3, byte[] bArr, boolean z) {
        validate();
        if (bArr == null) {
            rsnScriptForEach(this.mContext, this.mIncCon, j, i, j2, j3, z);
        } else {
            rsnScriptForEach(this.mContext, this.mIncCon, j, i, j2, j3, bArr, z);
        }
    }

    synchronized void nScriptSetVarF(long j, int i, float f, boolean z) {
        validate();
        long j2 = this.mContext;
        if (z) {
            j2 = this.mIncCon;
        }
        rsnScriptSetVarF(j2, j, i, f, z);
    }

    synchronized void nScriptSetVarObj(long j, int i, long j2, boolean z) {
        validate();
        long j3 = this.mContext;
        if (z) {
            j3 = this.mIncCon;
        }
        rsnScriptSetVarObj(j3, j, i, j2, z);
    }

    synchronized long nScriptIntrinsicCreate(int i, long j, boolean z) {
        validate();
        if (z) {
            if (Build.VERSION.SDK_INT < 21) {
                Log.e("RenderScript_jni", "Incremental Intrinsics are not supported, please change targetSdkVersion to >= 21");
                throw new RSRuntimeException("Incremental Intrinsics are not supported before Lollipop (API 21)");
            }
            if (!this.mIncLoaded) {
                try {
                    System.loadLibrary("RSSupport");
                    if (!nIncLoadSO(23, this.mNativeLibDir + "/libRSSupport.so")) {
                        throw new RSRuntimeException("Error loading libRSSupport library for Incremental Intrinsic Support");
                    }
                    this.mIncLoaded = true;
                } catch (UnsatisfiedLinkError e) {
                    Log.e("RenderScript_jni", "Error loading RS Compat library for Incremental Intrinsic Support: " + e);
                    throw new RSRuntimeException("Error loading RS Compat library for Incremental Intrinsic Support: " + e);
                }
            }
            if (this.mIncCon == 0) {
                this.mIncCon = nIncContextCreate(nIncDeviceCreate(), 0, 0, 0);
            }
            return rsnScriptIntrinsicCreate(this.mIncCon, i, j, z);
        }
        return rsnScriptIntrinsicCreate(this.mContext, i, j, z);
    }

    synchronized long nIncContextCreate(long j, int i, int i2, int i3) {
        return rsnIncContextCreate(j, i, i2, i3);
    }

    synchronized void nIncContextDestroy() {
        validate();
        ReentrantReadWriteLock.WriteLock writeLock = this.mRWLock.writeLock();
        writeLock.lock();
        long j = this.mIncCon;
        this.mIncCon = 0L;
        writeLock.unlock();
        rsnIncContextDestroy(j);
    }

    synchronized void nIncContextFinish() {
        validate();
        rsnIncContextFinish(this.mIncCon);
    }

    synchronized long nIncElementCreate(long j, int i, boolean z, int i2) {
        validate();
        return rsnIncElementCreate(this.mIncCon, j, i, z, i2);
    }

    synchronized long nIncTypeCreate(long j, int i, int i2, int i3, boolean z, boolean z2, int i4) {
        validate();
        return rsnIncTypeCreate(this.mIncCon, j, i, i2, i3, z, z2, i4);
    }

    synchronized long nIncAllocationCreateTyped(long j, long j2, int i) {
        validate();
        return rsnIncAllocationCreateTyped(this.mContext, this.mIncCon, j, j2, i);
    }

    public static class RSMessageHandler implements Runnable {
        protected int[] mData;
        protected int mID;
        protected int mLength;

        @Override
        public void run() {
        }
    }

    public static class RSErrorHandler implements Runnable {
        protected String mErrorMessage;
        protected int mErrorNum;

        @Override
        public void run() {
        }
    }

    void validate() {
        if (this.mContext == 0) {
            throw new RSInvalidStateException("Calling RS with no Context active.");
        }
    }

    boolean usingIO() {
        return useIOlib;
    }

    static class MessageThread extends Thread {
        int[] mAuxData;
        RenderScript mRS;
        boolean mRun;

        MessageThread(RenderScript renderScript) {
            super("RSMessageThread");
            this.mRun = true;
            this.mAuxData = new int[2];
            this.mRS = renderScript;
        }

        @Override
        public void run() {
            int[] iArr = new int[16];
            this.mRS.nContextInitToClient(this.mRS.mContext);
            while (this.mRun) {
                iArr[0] = 0;
                int iNContextPeekMessage = this.mRS.nContextPeekMessage(this.mRS.mContext, this.mAuxData);
                int i = this.mAuxData[1];
                int i2 = this.mAuxData[0];
                if (iNContextPeekMessage == 4) {
                    if ((i >> 2) >= iArr.length) {
                        iArr = new int[(i + 3) >> 2];
                    }
                    if (this.mRS.nContextGetUserMessage(this.mRS.mContext, iArr) != 4) {
                        throw new RSDriverException("Error processing message from RenderScript.");
                    }
                    if (this.mRS.mMessageCallback != null) {
                        this.mRS.mMessageCallback.mData = iArr;
                        this.mRS.mMessageCallback.mID = i2;
                        this.mRS.mMessageCallback.mLength = i;
                        this.mRS.mMessageCallback.run();
                    } else {
                        throw new RSInvalidStateException("Received a message from the script with no message handler installed.");
                    }
                } else if (iNContextPeekMessage != 3) {
                    try {
                        sleep(1L, 0);
                    } catch (InterruptedException e) {
                    }
                } else {
                    String strNContextGetErrorMessage = this.mRS.nContextGetErrorMessage(this.mRS.mContext);
                    if (i2 >= 4096 || (i2 >= 2048 && (this.mRS.mContextType != ContextType.DEBUG || this.mRS.mErrorCallback == null))) {
                        Log.e("RenderScript_jni", "fatal RS error, " + strNContextGetErrorMessage);
                        throw new RSRuntimeException("Fatal error " + i2 + ", details: " + strNContextGetErrorMessage);
                    }
                    if (this.mRS.mErrorCallback != null) {
                        this.mRS.mErrorCallback.mErrorMessage = strNContextGetErrorMessage;
                        this.mRS.mErrorCallback.mErrorNum = i2;
                        this.mRS.mErrorCallback.run();
                    } else {
                        Log.e("RenderScript_jni", "non fatal RS error, " + strNContextGetErrorMessage);
                    }
                }
            }
        }
    }

    RenderScript(Context context) {
        if (context != null) {
            this.mApplicationContext = context.getApplicationContext();
            this.mNativeLibDir = this.mApplicationContext.getApplicationInfo().nativeLibraryDir;
        }
        this.mIncCon = 0L;
        this.mIncLoaded = false;
        this.mRWLock = new ReentrantReadWriteLock();
    }

    private static RenderScript internalCreate(Context context, int i, ContextType contextType, int i2) {
        String str;
        int i3;
        RenderScript renderScript = new RenderScript(context);
        if (sSdkVersion == -1) {
            sSdkVersion = i;
        } else if (sSdkVersion != i) {
            throw new RSRuntimeException("Can't have two contexts with different SDK versions in support lib");
        }
        useNative = setupNative(sSdkVersion, context);
        synchronized (lock) {
            str = null;
            if (!sInitialized) {
                try {
                    Class<?> cls = Class.forName("dalvik.system.VMRuntime");
                    sRuntime = cls.getDeclaredMethod("getRuntime", new Class[0]).invoke(null, new Object[0]);
                    registerNativeAllocation = cls.getDeclaredMethod("registerNativeAllocation", Integer.TYPE);
                    registerNativeFree = cls.getDeclaredMethod("registerNativeFree", Integer.TYPE);
                    sUseGCHooks = true;
                } catch (Exception e) {
                    Log.e("RenderScript_jni", "No GC methods");
                    sUseGCHooks = false;
                }
                try {
                    if (Build.VERSION.SDK_INT < 23 && renderScript.mNativeLibDir != null) {
                        System.load(renderScript.mNativeLibDir + "/librsjni.so");
                    } else {
                        System.loadLibrary("rsjni");
                    }
                    sInitialized = true;
                    sPointerSize = rsnSystemGetPointerSize();
                } catch (UnsatisfiedLinkError e2) {
                    Log.e("RenderScript_jni", "Error loading RS jni library: " + e2);
                    throw new RSRuntimeException("Error loading RS jni library: " + e2 + " Support lib API: 2301");
                }
            }
        }
        if (useNative) {
            Log.v("RenderScript_jni", "RS native mode");
        } else {
            Log.v("RenderScript_jni", "RS compat mode");
        }
        if (Build.VERSION.SDK_INT >= 14) {
            useIOlib = true;
        }
        if (i < Build.VERSION.SDK_INT) {
            i3 = Build.VERSION.SDK_INT;
        } else {
            i3 = i;
        }
        if (Build.VERSION.SDK_INT < 23 && renderScript.mNativeLibDir != null) {
            str = renderScript.mNativeLibDir + "/libRSSupport.so";
        }
        if (!renderScript.nLoadSO(useNative, i3, str)) {
            if (useNative) {
                Log.v("RenderScript_jni", "Unable to load libRS.so, falling back to compat mode");
                useNative = false;
            }
            try {
                if (Build.VERSION.SDK_INT < 23 && renderScript.mNativeLibDir != null) {
                    System.load(str);
                } else {
                    System.loadLibrary("RSSupport");
                }
                if (!renderScript.nLoadSO(false, i3, str)) {
                    Log.e("RenderScript_jni", "Error loading RS Compat library: nLoadSO() failed; Support lib version: 2301");
                    throw new RSRuntimeException("Error loading libRSSupport library, Support lib version: 2301");
                }
            } catch (UnsatisfiedLinkError e3) {
                Log.e("RenderScript_jni", "Error loading RS Compat library: " + e3 + " Support lib version: 2301");
                throw new RSRuntimeException("Error loading RS Compat library: " + e3 + " Support lib version: 2301");
            }
        }
        if (useIOlib) {
            try {
                System.loadLibrary("RSSupportIO");
            } catch (UnsatisfiedLinkError e4) {
                useIOlib = false;
            }
            if (!useIOlib || !renderScript.nLoadIOSO()) {
                Log.v("RenderScript_jni", "Unable to load libRSSupportIO.so, USAGE_IO not supported");
                useIOlib = false;
            }
        }
        if (i3 >= 23) {
            renderScript.mEnableMultiInput = true;
            try {
                System.loadLibrary("blasV8");
            } catch (UnsatisfiedLinkError e5) {
                Log.v("RenderScript_jni", "Unable to load BLAS lib, ONLY BNNM will be supported: " + e5);
            }
        }
        renderScript.mContext = renderScript.nContextCreate(renderScript.nDeviceCreate(), 0, i, contextType.mID, renderScript.mNativeLibDir);
        renderScript.mContextType = contextType;
        renderScript.mContextFlags = i2;
        renderScript.mContextSdkVersion = i;
        renderScript.mDispatchAPILevel = i3;
        if (renderScript.mContext == 0) {
            throw new RSDriverException("Failed to create RS context.");
        }
        renderScript.mMessageThread = new MessageThread(renderScript);
        renderScript.mMessageThread.start();
        return renderScript;
    }

    public static RenderScript create(Context context) {
        return create(context, ContextType.NORMAL);
    }

    public static RenderScript create(Context context, ContextType contextType) {
        return create(context, contextType, 0);
    }

    public static RenderScript create(Context context, ContextType contextType, int i) {
        return create(context, context.getApplicationInfo().targetSdkVersion, contextType, i);
    }

    public static RenderScript create(Context context, int i, ContextType contextType, int i2) {
        synchronized (mProcessContextList) {
            for (RenderScript renderScript : mProcessContextList) {
                if (renderScript.mContextType == contextType && renderScript.mContextFlags == i2 && renderScript.mContextSdkVersion == i) {
                    return renderScript;
                }
            }
            RenderScript renderScriptInternalCreate = internalCreate(context, i, contextType, i2);
            renderScriptInternalCreate.mIsProcessContext = true;
            mProcessContextList.add(renderScriptInternalCreate);
            return renderScriptInternalCreate;
        }
    }

    private void helpDestroy() {
        boolean z;
        boolean z2;
        synchronized (this) {
            z = false;
            if (this.mDestroyed) {
                z2 = false;
            } else {
                this.mDestroyed = true;
                z2 = true;
            }
        }
        if (z2) {
            nContextFinish();
            if (this.mIncCon != 0) {
                nIncContextFinish();
                nIncContextDestroy();
                this.mIncCon = 0L;
            }
            nContextDeinitToClient(this.mContext);
            this.mMessageThread.mRun = false;
            this.mMessageThread.interrupt();
            boolean z3 = false;
            while (!z) {
                try {
                    this.mMessageThread.join();
                    z = true;
                } catch (InterruptedException e) {
                    z3 = true;
                }
            }
            if (z3) {
                Log.v("RenderScript_jni", "Interrupted during wait for MessageThread to join");
                Thread.currentThread().interrupt();
            }
            nContextDestroy();
        }
    }

    protected void finalize() throws Throwable {
        helpDestroy();
        super.finalize();
    }

    public void destroy() {
        if (this.mIsProcessContext) {
            return;
        }
        validate();
        helpDestroy();
    }

    boolean isAlive() {
        return this.mContext != 0;
    }
}
