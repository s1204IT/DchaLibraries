package android.database;

import android.content.res.Resources;
import android.database.sqlite.SQLiteClosable;
import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseIntArray;
import com.android.internal.R;
import dalvik.annotation.optimization.FastNative;
import dalvik.system.CloseGuard;

public class CursorWindow extends SQLiteClosable implements Parcelable {
    private static final String STATS_TAG = "CursorWindowStats";
    private final CloseGuard mCloseGuard;
    private final String mName;
    private int mStartPos;
    public long mWindowPtr;
    private static int sCursorWindowSize = -1;
    public static final Parcelable.Creator<CursorWindow> CREATOR = new Parcelable.Creator<CursorWindow>() {
        @Override
        public CursorWindow createFromParcel(Parcel parcel) {
            return new CursorWindow(parcel);
        }

        @Override
        public CursorWindow[] newArray(int i) {
            return new CursorWindow[i];
        }
    };
    private static final LongSparseArray<Integer> sWindowToPidMap = new LongSparseArray<>();

    @FastNative
    private static native boolean nativeAllocRow(long j);

    @FastNative
    private static native void nativeClear(long j);

    private static native void nativeCopyStringToBuffer(long j, int i, int i2, CharArrayBuffer charArrayBuffer);

    private static native long nativeCreate(String str, int i);

    private static native long nativeCreateFromParcel(Parcel parcel);

    private static native void nativeDispose(long j);

    @FastNative
    private static native void nativeFreeLastRow(long j);

    private static native byte[] nativeGetBlob(long j, int i, int i2);

    @FastNative
    private static native double nativeGetDouble(long j, int i, int i2);

    @FastNative
    private static native long nativeGetLong(long j, int i, int i2);

    private static native String nativeGetName(long j);

    @FastNative
    private static native int nativeGetNumRows(long j);

    private static native String nativeGetString(long j, int i, int i2);

    @FastNative
    private static native int nativeGetType(long j, int i, int i2);

    private static native boolean nativePutBlob(long j, byte[] bArr, int i, int i2);

    @FastNative
    private static native boolean nativePutDouble(long j, double d, int i, int i2);

    @FastNative
    private static native boolean nativePutLong(long j, long j2, int i, int i2);

    @FastNative
    private static native boolean nativePutNull(long j, int i, int i2);

    private static native boolean nativePutString(long j, String str, int i, int i2);

    @FastNative
    private static native boolean nativeSetNumColumns(long j, int i);

    private static native void nativeWriteToParcel(long j, Parcel parcel);

    public CursorWindow(String str) {
        this(str, getCursorWindowSize());
    }

    public CursorWindow(String str, long j) {
        this.mCloseGuard = CloseGuard.get();
        this.mStartPos = 0;
        this.mName = (str == null || str.length() == 0) ? "<unnamed>" : str;
        this.mWindowPtr = nativeCreate(this.mName, (int) j);
        if (this.mWindowPtr == 0) {
            throw new CursorWindowAllocationException("Cursor window allocation of " + j + " bytes failed. " + printStats());
        }
        this.mCloseGuard.open("close");
        recordNewWindow(Binder.getCallingPid(), this.mWindowPtr);
    }

    @Deprecated
    public CursorWindow(boolean z) {
        this((String) null);
    }

    private CursorWindow(Parcel parcel) {
        this.mCloseGuard = CloseGuard.get();
        this.mStartPos = parcel.readInt();
        this.mWindowPtr = nativeCreateFromParcel(parcel);
        if (this.mWindowPtr == 0) {
            throw new CursorWindowAllocationException("Cursor window could not be created from binder.");
        }
        this.mName = nativeGetName(this.mWindowPtr);
        this.mCloseGuard.open("close");
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            dispose();
        } finally {
            super.finalize();
        }
    }

    private void dispose() {
        if (this.mCloseGuard != null) {
            this.mCloseGuard.close();
        }
        if (this.mWindowPtr != 0) {
            recordClosingOfWindow(this.mWindowPtr);
            nativeDispose(this.mWindowPtr);
            this.mWindowPtr = 0L;
        }
    }

    public String getName() {
        return this.mName;
    }

    public void clear() {
        acquireReference();
        try {
            this.mStartPos = 0;
            nativeClear(this.mWindowPtr);
        } finally {
            releaseReference();
        }
    }

    public int getStartPosition() {
        return this.mStartPos;
    }

    public void setStartPosition(int i) {
        this.mStartPos = i;
    }

    public int getNumRows() {
        acquireReference();
        try {
            return nativeGetNumRows(this.mWindowPtr);
        } finally {
            releaseReference();
        }
    }

    public boolean setNumColumns(int i) {
        acquireReference();
        try {
            return nativeSetNumColumns(this.mWindowPtr, i);
        } finally {
            releaseReference();
        }
    }

    public boolean allocRow() {
        acquireReference();
        try {
            return nativeAllocRow(this.mWindowPtr);
        } finally {
            releaseReference();
        }
    }

    public void freeLastRow() {
        acquireReference();
        try {
            nativeFreeLastRow(this.mWindowPtr);
        } finally {
            releaseReference();
        }
    }

    @Deprecated
    public boolean isNull(int i, int i2) {
        return getType(i, i2) == 0;
    }

    @Deprecated
    public boolean isBlob(int i, int i2) {
        int type = getType(i, i2);
        return type == 4 || type == 0;
    }

    @Deprecated
    public boolean isLong(int i, int i2) {
        return getType(i, i2) == 1;
    }

    @Deprecated
    public boolean isFloat(int i, int i2) {
        return getType(i, i2) == 2;
    }

    @Deprecated
    public boolean isString(int i, int i2) {
        int type = getType(i, i2);
        return type == 3 || type == 0;
    }

    public int getType(int i, int i2) {
        acquireReference();
        try {
            return nativeGetType(this.mWindowPtr, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public byte[] getBlob(int i, int i2) {
        acquireReference();
        try {
            return nativeGetBlob(this.mWindowPtr, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public String getString(int i, int i2) {
        acquireReference();
        try {
            return nativeGetString(this.mWindowPtr, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public void copyStringToBuffer(int i, int i2, CharArrayBuffer charArrayBuffer) {
        if (charArrayBuffer == null) {
            throw new IllegalArgumentException("CharArrayBuffer should not be null");
        }
        acquireReference();
        try {
            nativeCopyStringToBuffer(this.mWindowPtr, i - this.mStartPos, i2, charArrayBuffer);
        } finally {
            releaseReference();
        }
    }

    public long getLong(int i, int i2) {
        acquireReference();
        try {
            return nativeGetLong(this.mWindowPtr, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public double getDouble(int i, int i2) {
        acquireReference();
        try {
            return nativeGetDouble(this.mWindowPtr, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public short getShort(int i, int i2) {
        return (short) getLong(i, i2);
    }

    public int getInt(int i, int i2) {
        return (int) getLong(i, i2);
    }

    public float getFloat(int i, int i2) {
        return (float) getDouble(i, i2);
    }

    public boolean putBlob(byte[] bArr, int i, int i2) {
        acquireReference();
        try {
            return nativePutBlob(this.mWindowPtr, bArr, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public boolean putString(String str, int i, int i2) {
        acquireReference();
        try {
            return nativePutString(this.mWindowPtr, str, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public boolean putLong(long j, int i, int i2) {
        acquireReference();
        try {
            return nativePutLong(this.mWindowPtr, j, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public boolean putDouble(double d, int i, int i2) {
        acquireReference();
        try {
            return nativePutDouble(this.mWindowPtr, d, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public boolean putNull(int i, int i2) {
        acquireReference();
        try {
            return nativePutNull(this.mWindowPtr, i - this.mStartPos, i2);
        } finally {
            releaseReference();
        }
    }

    public static CursorWindow newFromParcel(Parcel parcel) {
        return CREATOR.createFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        acquireReference();
        try {
            parcel.writeInt(this.mStartPos);
            nativeWriteToParcel(this.mWindowPtr, parcel);
            releaseReference();
            if ((i & 1) != 0) {
            }
        } finally {
            releaseReference();
        }
    }

    @Override
    protected void onAllReferencesReleased() {
        dispose();
    }

    private void recordNewWindow(int i, long j) {
        synchronized (sWindowToPidMap) {
            sWindowToPidMap.put(j, Integer.valueOf(i));
            if (Log.isLoggable(STATS_TAG, 2)) {
                Log.i(STATS_TAG, "Created a new Cursor. " + printStats());
            }
        }
    }

    private void recordClosingOfWindow(long j) {
        synchronized (sWindowToPidMap) {
            if (sWindowToPidMap.size() == 0) {
                return;
            }
            sWindowToPidMap.delete(j);
        }
    }

    private String printStats() {
        StringBuilder sb = new StringBuilder();
        int iMyPid = Process.myPid();
        SparseIntArray sparseIntArray = new SparseIntArray();
        synchronized (sWindowToPidMap) {
            int size = sWindowToPidMap.size();
            if (size == 0) {
                return "";
            }
            for (int i = 0; i < size; i++) {
                int iIntValue = sWindowToPidMap.valueAt(i).intValue();
                sparseIntArray.put(iIntValue, sparseIntArray.get(iIntValue) + 1);
            }
            int size2 = sparseIntArray.size();
            int i2 = 0;
            for (int i3 = 0; i3 < size2; i3++) {
                sb.append(" (# cursors opened by ");
                int iKeyAt = sparseIntArray.keyAt(i3);
                if (iKeyAt == iMyPid) {
                    sb.append("this proc=");
                } else {
                    sb.append("pid " + iKeyAt + "=");
                }
                int i4 = sparseIntArray.get(iKeyAt);
                sb.append(i4 + ")");
                i2 += i4;
            }
            return "# Open Cursors=" + i2 + (sb.length() > 980 ? sb.substring(0, 980) : sb.toString());
        }
    }

    private static int getCursorWindowSize() {
        if (sCursorWindowSize < 0) {
            sCursorWindowSize = Resources.getSystem().getInteger(R.integer.config_cursorWindowSize) * 1024;
        }
        return sCursorWindowSize;
    }

    public String toString() {
        return getName() + " {" + Long.toHexString(this.mWindowPtr) + "}";
    }
}
