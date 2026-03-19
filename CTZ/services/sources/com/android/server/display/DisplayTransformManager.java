package com.android.server.display;

import android.app.ActivityManager;
import android.content.res.Configuration;
import android.opengl.Matrix;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import java.lang.reflect.Array;
import java.util.Arrays;

public class DisplayTransformManager {
    private static final float COLOR_SATURATION_BOOSTED = 1.1f;
    private static final float COLOR_SATURATION_NATURAL = 1.0f;
    private static final int DISPLAY_COLOR_ENHANCED = 2;
    private static final int DISPLAY_COLOR_MANAGED = 0;
    private static final int DISPLAY_COLOR_UNMANAGED = 1;
    public static final int LEVEL_COLOR_MATRIX_GRAYSCALE = 200;
    public static final int LEVEL_COLOR_MATRIX_INVERT_COLOR = 300;
    public static final int LEVEL_COLOR_MATRIX_NIGHT_DISPLAY = 100;
    public static final int LEVEL_COLOR_MATRIX_SATURATION = 150;
    private static final String PERSISTENT_PROPERTY_DISPLAY_COLOR = "persist.sys.sf.native_mode";
    private static final String PERSISTENT_PROPERTY_SATURATION = "persist.sys.sf.color_saturation";
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    private static final int SURFACE_FLINGER_TRANSACTION_COLOR_MATRIX = 1015;
    private static final int SURFACE_FLINGER_TRANSACTION_DALTONIZER = 1014;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_COLOR = 1023;
    private static final int SURFACE_FLINGER_TRANSACTION_SATURATION = 1022;
    private static final String TAG = "DisplayTransformManager";

    @GuardedBy("mColorMatrix")
    private final SparseArray<float[]> mColorMatrix = new SparseArray<>(3);

    @GuardedBy("mColorMatrix")
    private final float[][] mTempColorMatrix = (float[][]) Array.newInstance((Class<?>) float.class, 2, 16);
    private final Object mDaltonizerModeLock = new Object();

    @GuardedBy("mDaltonizerModeLock")
    private int mDaltonizerMode = -1;

    DisplayTransformManager() {
    }

    public float[] getColorMatrix(int i) {
        float[] fArrCopyOf;
        synchronized (this.mColorMatrix) {
            float[] fArr = this.mColorMatrix.get(i);
            fArrCopyOf = fArr == null ? null : Arrays.copyOf(fArr, fArr.length);
        }
        return fArrCopyOf;
    }

    public void setColorMatrix(int i, float[] fArr) {
        if (fArr != null && fArr.length != 16) {
            throw new IllegalArgumentException("Expected length: 16 (4x4 matrix), actual length: " + fArr.length);
        }
        synchronized (this.mColorMatrix) {
            float[] fArr2 = this.mColorMatrix.get(i);
            if (!Arrays.equals(fArr2, fArr)) {
                if (fArr == null) {
                    this.mColorMatrix.remove(i);
                } else if (fArr2 == null) {
                    this.mColorMatrix.put(i, Arrays.copyOf(fArr, fArr.length));
                } else {
                    System.arraycopy(fArr, 0, fArr2, 0, fArr.length);
                }
                applyColorMatrix(computeColorMatrixLocked());
            }
        }
    }

    @GuardedBy("mColorMatrix")
    private float[] computeColorMatrixLocked() {
        int size = this.mColorMatrix.size();
        if (size == 0) {
            return null;
        }
        float[][] fArr = this.mTempColorMatrix;
        int i = 0;
        Matrix.setIdentityM(fArr[0], 0);
        while (i < size) {
            int i2 = i + 1;
            Matrix.multiplyMM(fArr[i2 % 2], 0, fArr[i % 2], 0, this.mColorMatrix.valueAt(i), 0);
            i = i2;
        }
        return fArr[size % 2];
    }

    public int getDaltonizerMode() {
        int i;
        synchronized (this.mDaltonizerModeLock) {
            i = this.mDaltonizerMode;
        }
        return i;
    }

    public void setDaltonizerMode(int i) {
        synchronized (this.mDaltonizerModeLock) {
            if (this.mDaltonizerMode != i) {
                this.mDaltonizerMode = i;
                applyDaltonizerMode(i);
            }
        }
    }

    private static void applyColorMatrix(float[] fArr) {
        IBinder service = ServiceManager.getService(SURFACE_FLINGER);
        if (service != null) {
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
            if (fArr != null) {
                parcelObtain.writeInt(1);
                for (int i = 0; i < 16; i++) {
                    parcelObtain.writeFloat(fArr[i]);
                }
            } else {
                parcelObtain.writeInt(0);
            }
            try {
                try {
                    service.transact(SURFACE_FLINGER_TRANSACTION_COLOR_MATRIX, parcelObtain, null, 0);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to set color transform", e);
                }
            } finally {
                parcelObtain.recycle();
            }
        }
    }

    private static void applyDaltonizerMode(int i) {
        IBinder service = ServiceManager.getService(SURFACE_FLINGER);
        if (service != null) {
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
            parcelObtain.writeInt(i);
            try {
                try {
                    service.transact(SURFACE_FLINGER_TRANSACTION_DALTONIZER, parcelObtain, null, 0);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to set Daltonizer mode", e);
                }
            } finally {
                parcelObtain.recycle();
            }
        }
    }

    public static boolean needsLinearColorMatrix() {
        return SystemProperties.getInt(PERSISTENT_PROPERTY_DISPLAY_COLOR, 1) != 1;
    }

    public static boolean needsLinearColorMatrix(int i) {
        return i != 2;
    }

    public boolean setColorMode(int i, float[] fArr) {
        if (i == 0) {
            applySaturation(1.0f);
            setDisplayColor(0);
        } else if (i == 1) {
            applySaturation(COLOR_SATURATION_BOOSTED);
            setDisplayColor(0);
        } else if (i == 2) {
            applySaturation(1.0f);
            setDisplayColor(1);
        } else if (i == 3) {
            applySaturation(1.0f);
            setDisplayColor(2);
        }
        setColorMatrix(100, fArr);
        updateConfiguration();
        return true;
    }

    private void applySaturation(float f) {
        SystemProperties.set(PERSISTENT_PROPERTY_SATURATION, Float.toString(f));
        IBinder service = ServiceManager.getService(SURFACE_FLINGER);
        if (service != null) {
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
            parcelObtain.writeFloat(f);
            try {
                try {
                    service.transact(SURFACE_FLINGER_TRANSACTION_SATURATION, parcelObtain, null, 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to set saturation", e);
                }
            } finally {
                parcelObtain.recycle();
            }
        }
    }

    private void setDisplayColor(int i) {
        SystemProperties.set(PERSISTENT_PROPERTY_DISPLAY_COLOR, Integer.toString(i));
        IBinder service = ServiceManager.getService(SURFACE_FLINGER);
        if (service != null) {
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
            parcelObtain.writeInt(i);
            try {
                try {
                    service.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_COLOR, parcelObtain, null, 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to set display color", e);
                }
            } finally {
                parcelObtain.recycle();
            }
        }
    }

    private void updateConfiguration() {
        try {
            ActivityManager.getService().updateConfiguration((Configuration) null);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not update configuration", e);
        }
    }
}
