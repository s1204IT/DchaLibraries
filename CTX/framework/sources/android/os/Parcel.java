package android.os;

import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.ExceptionUtils;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.widget.GridLayout;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;
import dalvik.system.VMRuntime;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import libcore.util.SneakyThrow;

public final class Parcel {
    private static final boolean DEBUG_ARRAY_MAP = false;
    private static final boolean DEBUG_RECYCLE = false;
    private static final int EX_BAD_PARCELABLE = -2;
    private static final int EX_HAS_REPLY_HEADER = -128;
    private static final int EX_ILLEGAL_ARGUMENT = -3;
    private static final int EX_ILLEGAL_STATE = -5;
    private static final int EX_NETWORK_MAIN_THREAD = -6;
    private static final int EX_NULL_POINTER = -4;
    private static final int EX_PARCELABLE = -9;
    private static final int EX_SECURITY = -1;
    private static final int EX_SERVICE_SPECIFIC = -8;
    private static final int EX_TRANSACTION_FAILED = -129;
    private static final int EX_UNSUPPORTED_OPERATION = -7;
    private static final int POOL_SIZE = 6;
    private static final String TAG = "Parcel";
    private static final int VAL_BOOLEAN = 9;
    private static final int VAL_BOOLEANARRAY = 23;
    private static final int VAL_BUNDLE = 3;
    private static final int VAL_BYTE = 20;
    private static final int VAL_BYTEARRAY = 13;
    private static final int VAL_CHARSEQUENCE = 10;
    private static final int VAL_CHARSEQUENCEARRAY = 24;
    private static final int VAL_DOUBLE = 8;
    private static final int VAL_DOUBLEARRAY = 28;
    private static final int VAL_FLOAT = 7;
    private static final int VAL_IBINDER = 15;
    private static final int VAL_INTARRAY = 18;
    private static final int VAL_INTEGER = 1;
    private static final int VAL_LIST = 11;
    private static final int VAL_LONG = 6;
    private static final int VAL_LONGARRAY = 19;
    private static final int VAL_MAP = 2;
    private static final int VAL_NULL = -1;
    private static final int VAL_OBJECTARRAY = 17;
    private static final int VAL_PARCELABLE = 4;
    private static final int VAL_PARCELABLEARRAY = 16;
    private static final int VAL_PERSISTABLEBUNDLE = 25;
    private static final int VAL_SERIALIZABLE = 21;
    private static final int VAL_SHORT = 5;
    private static final int VAL_SIZE = 26;
    private static final int VAL_SIZEF = 27;
    private static final int VAL_SPARSEARRAY = 12;
    private static final int VAL_SPARSEBOOLEANARRAY = 22;
    private static final int VAL_STRING = 0;
    private static final int VAL_STRINGARRAY = 14;
    private static final int WRITE_EXCEPTION_STACK_TRACE_THRESHOLD_MS = 1000;
    private static volatile long sLastWriteExceptionStackTrace;
    private static boolean sParcelExceptionStackTrace;
    private ArrayMap<Class, Object> mClassCookies;
    private long mNativePtr;
    private long mNativeSize;
    private boolean mOwnsNativeParcelObject;
    private ReadWriteHelper mReadWriteHelper = ReadWriteHelper.DEFAULT;
    private RuntimeException mStack;
    private static final Parcel[] sOwnedPool = new Parcel[6];
    private static final Parcel[] sHolderPool = new Parcel[6];
    public static final Parcelable.Creator<String> STRING_CREATOR = new Parcelable.Creator<String>() {
        @Override
        public String createFromParcel(Parcel parcel) {
            return parcel.readString();
        }

        @Override
        public String[] newArray(int i) {
            return new String[i];
        }
    };
    private static final HashMap<ClassLoader, HashMap<String, Parcelable.Creator<?>>> mCreators = new HashMap<>();

    @Deprecated
    static native void closeFileDescriptor(FileDescriptor fileDescriptor) throws IOException;

    @Deprecated
    static native FileDescriptor dupFileDescriptor(FileDescriptor fileDescriptor) throws IOException;

    public static native long getGlobalAllocCount();

    public static native long getGlobalAllocSize();

    private static native long nativeAppendFrom(long j, long j2, int i, int i2);

    private static native int nativeCompareData(long j, long j2);

    private static native long nativeCreate();

    private static native byte[] nativeCreateByteArray(long j);

    @CriticalNative
    private static native int nativeDataAvail(long j);

    @CriticalNative
    private static native int nativeDataCapacity(long j);

    @CriticalNative
    private static native int nativeDataPosition(long j);

    @CriticalNative
    private static native int nativeDataSize(long j);

    private static native void nativeDestroy(long j);

    private static native void nativeEnforceInterface(long j, String str);

    private static native long nativeFreeBuffer(long j);

    @CriticalNative
    private static native long nativeGetBlobAshmemSize(long j);

    @CriticalNative
    private static native boolean nativeHasFileDescriptors(long j);

    private static native byte[] nativeMarshall(long j);

    @CriticalNative
    private static native boolean nativePushAllowFds(long j, boolean z);

    private static native byte[] nativeReadBlob(long j);

    private static native boolean nativeReadByteArray(long j, byte[] bArr, int i);

    @CriticalNative
    private static native double nativeReadDouble(long j);

    private static native FileDescriptor nativeReadFileDescriptor(long j);

    @CriticalNative
    private static native float nativeReadFloat(long j);

    @CriticalNative
    private static native int nativeReadInt(long j);

    @CriticalNative
    private static native long nativeReadLong(long j);

    static native String nativeReadString(long j);

    private static native IBinder nativeReadStrongBinder(long j);

    @CriticalNative
    private static native void nativeRestoreAllowFds(long j, boolean z);

    @FastNative
    private static native void nativeSetDataCapacity(long j, int i);

    @CriticalNative
    private static native void nativeSetDataPosition(long j, int i);

    @FastNative
    private static native long nativeSetDataSize(long j, int i);

    private static native long nativeUnmarshall(long j, byte[] bArr, int i, int i2);

    private static native void nativeWriteBlob(long j, byte[] bArr, int i, int i2);

    private static native void nativeWriteByteArray(long j, byte[] bArr, int i, int i2);

    @FastNative
    private static native void nativeWriteDouble(long j, double d);

    private static native long nativeWriteFileDescriptor(long j, FileDescriptor fileDescriptor);

    @FastNative
    private static native void nativeWriteFloat(long j, float f);

    @FastNative
    private static native void nativeWriteInt(long j, int i);

    private static native void nativeWriteInterfaceToken(long j, String str);

    @FastNative
    private static native void nativeWriteLong(long j, long j2);

    static native void nativeWriteString(long j, String str);

    private static native void nativeWriteStrongBinder(long j, IBinder iBinder);

    @Deprecated
    static native FileDescriptor openFileDescriptor(String str, int i) throws FileNotFoundException;

    public static class ReadWriteHelper {
        public static final ReadWriteHelper DEFAULT = new ReadWriteHelper();

        public void writeString(Parcel parcel, String str) {
            Parcel.nativeWriteString(parcel.mNativePtr, str);
        }

        public String readString(Parcel parcel) {
            return Parcel.nativeReadString(parcel.mNativePtr);
        }
    }

    public static Parcel obtain() {
        Parcel[] parcelArr = sOwnedPool;
        synchronized (parcelArr) {
            for (int i = 0; i < 6; i++) {
                try {
                    Parcel parcel = parcelArr[i];
                    if (parcel != null) {
                        parcelArr[i] = null;
                        parcel.mReadWriteHelper = ReadWriteHelper.DEFAULT;
                        return parcel;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return new Parcel(0L);
        }
    }

    public final void recycle() {
        Parcel[] parcelArr;
        freeBuffer();
        if (this.mOwnsNativeParcelObject) {
            parcelArr = sOwnedPool;
        } else {
            this.mNativePtr = 0L;
            parcelArr = sHolderPool;
        }
        synchronized (parcelArr) {
            for (int i = 0; i < 6; i++) {
                try {
                    if (parcelArr[i] == null) {
                        parcelArr[i] = this;
                        return;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    public void setReadWriteHelper(ReadWriteHelper readWriteHelper) {
        if (readWriteHelper == null) {
            readWriteHelper = ReadWriteHelper.DEFAULT;
        }
        this.mReadWriteHelper = readWriteHelper;
    }

    public boolean hasReadWriteHelper() {
        return (this.mReadWriteHelper == null || this.mReadWriteHelper == ReadWriteHelper.DEFAULT) ? false : true;
    }

    public final int dataSize() {
        return nativeDataSize(this.mNativePtr);
    }

    public final int dataAvail() {
        return nativeDataAvail(this.mNativePtr);
    }

    public final int dataPosition() {
        return nativeDataPosition(this.mNativePtr);
    }

    public final int dataCapacity() {
        return nativeDataCapacity(this.mNativePtr);
    }

    public final void setDataSize(int i) {
        updateNativeSize(nativeSetDataSize(this.mNativePtr, i));
    }

    public final void setDataPosition(int i) {
        nativeSetDataPosition(this.mNativePtr, i);
    }

    public final void setDataCapacity(int i) {
        nativeSetDataCapacity(this.mNativePtr, i);
    }

    public final boolean pushAllowFds(boolean z) {
        return nativePushAllowFds(this.mNativePtr, z);
    }

    public final void restoreAllowFds(boolean z) {
        nativeRestoreAllowFds(this.mNativePtr, z);
    }

    public final byte[] marshall() {
        return nativeMarshall(this.mNativePtr);
    }

    public final void unmarshall(byte[] bArr, int i, int i2) {
        updateNativeSize(nativeUnmarshall(this.mNativePtr, bArr, i, i2));
    }

    public final void appendFrom(Parcel parcel, int i, int i2) {
        updateNativeSize(nativeAppendFrom(this.mNativePtr, parcel.mNativePtr, i, i2));
    }

    public final int compareData(Parcel parcel) {
        return nativeCompareData(this.mNativePtr, parcel.mNativePtr);
    }

    public final void setClassCookie(Class cls, Object obj) {
        if (this.mClassCookies == null) {
            this.mClassCookies = new ArrayMap<>();
        }
        this.mClassCookies.put(cls, obj);
    }

    public final Object getClassCookie(Class cls) {
        if (this.mClassCookies != null) {
            return this.mClassCookies.get(cls);
        }
        return null;
    }

    public final void adoptClassCookies(Parcel parcel) {
        this.mClassCookies = parcel.mClassCookies;
    }

    public Map<Class, Object> copyClassCookies() {
        return new ArrayMap(this.mClassCookies);
    }

    public void putClassCookies(Map<Class, Object> map) {
        if (map == null) {
            return;
        }
        if (this.mClassCookies == null) {
            this.mClassCookies = new ArrayMap<>();
        }
        this.mClassCookies.putAll(map);
    }

    public final boolean hasFileDescriptors() {
        return nativeHasFileDescriptors(this.mNativePtr);
    }

    public final void writeInterfaceToken(String str) {
        nativeWriteInterfaceToken(this.mNativePtr, str);
    }

    public final void enforceInterface(String str) {
        nativeEnforceInterface(this.mNativePtr, str);
    }

    public final void writeByteArray(byte[] bArr) {
        writeByteArray(bArr, 0, bArr != null ? bArr.length : 0);
    }

    public final void writeByteArray(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            writeInt(-1);
        } else {
            Arrays.checkOffsetAndCount(bArr.length, i, i2);
            nativeWriteByteArray(this.mNativePtr, bArr, i, i2);
        }
    }

    public final void writeBlob(byte[] bArr) {
        writeBlob(bArr, 0, bArr != null ? bArr.length : 0);
    }

    public final void writeBlob(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            writeInt(-1);
        } else {
            Arrays.checkOffsetAndCount(bArr.length, i, i2);
            nativeWriteBlob(this.mNativePtr, bArr, i, i2);
        }
    }

    public final void writeInt(int i) {
        nativeWriteInt(this.mNativePtr, i);
    }

    public final void writeLong(long j) {
        nativeWriteLong(this.mNativePtr, j);
    }

    public final void writeFloat(float f) {
        nativeWriteFloat(this.mNativePtr, f);
    }

    public final void writeDouble(double d) {
        nativeWriteDouble(this.mNativePtr, d);
    }

    public final void writeString(String str) {
        this.mReadWriteHelper.writeString(this, str);
    }

    public void writeStringNoHelper(String str) {
        nativeWriteString(this.mNativePtr, str);
    }

    public final void writeBoolean(boolean z) {
        writeInt(z ? 1 : 0);
    }

    public final void writeCharSequence(CharSequence charSequence) {
        TextUtils.writeToParcel(charSequence, this, 0);
    }

    public final void writeStrongBinder(IBinder iBinder) {
        nativeWriteStrongBinder(this.mNativePtr, iBinder);
    }

    public final void writeStrongInterface(IInterface iInterface) {
        writeStrongBinder(iInterface == null ? null : iInterface.asBinder());
    }

    public final void writeFileDescriptor(FileDescriptor fileDescriptor) {
        updateNativeSize(nativeWriteFileDescriptor(this.mNativePtr, fileDescriptor));
    }

    private void updateNativeSize(long j) {
        if (this.mOwnsNativeParcelObject) {
            if (j > 2147483647L) {
                j = 2147483647L;
            }
            if (j != this.mNativeSize) {
                int i = (int) (j - this.mNativeSize);
                if (i > 0) {
                    VMRuntime.getRuntime().registerNativeAllocation(i);
                } else {
                    VMRuntime.getRuntime().registerNativeFree(-i);
                }
                this.mNativeSize = j;
            }
        }
    }

    public final void writeRawFileDescriptor(FileDescriptor fileDescriptor) {
        nativeWriteFileDescriptor(this.mNativePtr, fileDescriptor);
    }

    public final void writeRawFileDescriptorArray(FileDescriptor[] fileDescriptorArr) {
        if (fileDescriptorArr != null) {
            writeInt(fileDescriptorArr.length);
            for (FileDescriptor fileDescriptor : fileDescriptorArr) {
                writeRawFileDescriptor(fileDescriptor);
            }
            return;
        }
        writeInt(-1);
    }

    public final void writeByte(byte b) {
        writeInt(b);
    }

    public final void writeMap(Map map) {
        writeMapInternal(map);
    }

    void writeMapInternal(Map<String, Object> map) {
        if (map == null) {
            writeInt(-1);
            return;
        }
        Set<Map.Entry<String, Object>> setEntrySet = map.entrySet();
        int size = setEntrySet.size();
        writeInt(size);
        for (Map.Entry<String, Object> entry : setEntrySet) {
            writeValue(entry.getKey());
            writeValue(entry.getValue());
            size--;
        }
        if (size != 0) {
            throw new BadParcelableException("Map size does not match number of entries!");
        }
    }

    void writeArrayMapInternal(ArrayMap<String, Object> arrayMap) {
        if (arrayMap == null) {
            writeInt(-1);
            return;
        }
        int size = arrayMap.size();
        writeInt(size);
        for (int i = 0; i < size; i++) {
            writeString(arrayMap.keyAt(i));
            writeValue(arrayMap.valueAt(i));
        }
    }

    public void writeArrayMap(ArrayMap<String, Object> arrayMap) {
        writeArrayMapInternal(arrayMap);
    }

    public void writeArraySet(ArraySet<? extends Object> arraySet) {
        int size = arraySet != null ? arraySet.size() : -1;
        writeInt(size);
        for (int i = 0; i < size; i++) {
            writeValue(arraySet.valueAt(i));
        }
    }

    public final void writeBundle(Bundle bundle) {
        if (bundle == null) {
            writeInt(-1);
        } else {
            bundle.writeToParcel(this, 0);
        }
    }

    public final void writePersistableBundle(PersistableBundle persistableBundle) {
        if (persistableBundle == null) {
            writeInt(-1);
        } else {
            persistableBundle.writeToParcel(this, 0);
        }
    }

    public final void writeSize(Size size) {
        writeInt(size.getWidth());
        writeInt(size.getHeight());
    }

    public final void writeSizeF(SizeF sizeF) {
        writeFloat(sizeF.getWidth());
        writeFloat(sizeF.getHeight());
    }

    public final void writeList(List list) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        int size = list.size();
        writeInt(size);
        for (int i = 0; i < size; i++) {
            writeValue(list.get(i));
        }
    }

    public final void writeArray(Object[] objArr) {
        if (objArr == null) {
            writeInt(-1);
            return;
        }
        writeInt(objArr.length);
        for (Object obj : objArr) {
            writeValue(obj);
        }
    }

    public final void writeSparseArray(SparseArray<Object> sparseArray) {
        if (sparseArray == null) {
            writeInt(-1);
            return;
        }
        int size = sparseArray.size();
        writeInt(size);
        for (int i = 0; i < size; i++) {
            writeInt(sparseArray.keyAt(i));
            writeValue(sparseArray.valueAt(i));
        }
    }

    public final void writeSparseBooleanArray(SparseBooleanArray sparseBooleanArray) {
        if (sparseBooleanArray == null) {
            writeInt(-1);
            return;
        }
        int size = sparseBooleanArray.size();
        writeInt(size);
        for (int i = 0; i < size; i++) {
            writeInt(sparseBooleanArray.keyAt(i));
            writeByte(sparseBooleanArray.valueAt(i) ? (byte) 1 : (byte) 0);
        }
    }

    public final void writeSparseIntArray(SparseIntArray sparseIntArray) {
        if (sparseIntArray == null) {
            writeInt(-1);
            return;
        }
        int size = sparseIntArray.size();
        writeInt(size);
        for (int i = 0; i < size; i++) {
            writeInt(sparseIntArray.keyAt(i));
            writeInt(sparseIntArray.valueAt(i));
        }
    }

    public final void writeBooleanArray(boolean[] zArr) {
        if (zArr != null) {
            writeInt(zArr.length);
            for (boolean z : zArr) {
                writeInt(z ? 1 : 0);
            }
            return;
        }
        writeInt(-1);
    }

    public final boolean[] createBooleanArray() {
        int i = readInt();
        if (i >= 0 && i <= (dataAvail() >> 2)) {
            boolean[] zArr = new boolean[i];
            for (int i2 = 0; i2 < i; i2++) {
                zArr[i2] = readInt() != 0;
            }
            return zArr;
        }
        return null;
    }

    public final void readBooleanArray(boolean[] zArr) {
        int i = readInt();
        if (i == zArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                zArr[i2] = readInt() != 0;
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final void writeCharArray(char[] cArr) {
        if (cArr != null) {
            writeInt(cArr.length);
            for (char c : cArr) {
                writeInt(c);
            }
            return;
        }
        writeInt(-1);
    }

    public final char[] createCharArray() {
        int i = readInt();
        if (i >= 0 && i <= (dataAvail() >> 2)) {
            char[] cArr = new char[i];
            for (int i2 = 0; i2 < i; i2++) {
                cArr[i2] = (char) readInt();
            }
            return cArr;
        }
        return null;
    }

    public final void readCharArray(char[] cArr) {
        int i = readInt();
        if (i == cArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                cArr[i2] = (char) readInt();
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final void writeIntArray(int[] iArr) {
        if (iArr != null) {
            writeInt(iArr.length);
            for (int i : iArr) {
                writeInt(i);
            }
            return;
        }
        writeInt(-1);
    }

    public final int[] createIntArray() {
        int i = readInt();
        if (i >= 0 && i <= (dataAvail() >> 2)) {
            int[] iArr = new int[i];
            for (int i2 = 0; i2 < i; i2++) {
                iArr[i2] = readInt();
            }
            return iArr;
        }
        return null;
    }

    public final void readIntArray(int[] iArr) {
        int i = readInt();
        if (i == iArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                iArr[i2] = readInt();
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final void writeLongArray(long[] jArr) {
        if (jArr != null) {
            writeInt(jArr.length);
            for (long j : jArr) {
                writeLong(j);
            }
            return;
        }
        writeInt(-1);
    }

    public final long[] createLongArray() {
        int i = readInt();
        if (i >= 0 && i <= (dataAvail() >> 3)) {
            long[] jArr = new long[i];
            for (int i2 = 0; i2 < i; i2++) {
                jArr[i2] = readLong();
            }
            return jArr;
        }
        return null;
    }

    public final void readLongArray(long[] jArr) {
        int i = readInt();
        if (i == jArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                jArr[i2] = readLong();
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final void writeFloatArray(float[] fArr) {
        if (fArr != null) {
            writeInt(fArr.length);
            for (float f : fArr) {
                writeFloat(f);
            }
            return;
        }
        writeInt(-1);
    }

    public final float[] createFloatArray() {
        int i = readInt();
        if (i >= 0 && i <= (dataAvail() >> 2)) {
            float[] fArr = new float[i];
            for (int i2 = 0; i2 < i; i2++) {
                fArr[i2] = readFloat();
            }
            return fArr;
        }
        return null;
    }

    public final void readFloatArray(float[] fArr) {
        int i = readInt();
        if (i == fArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                fArr[i2] = readFloat();
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final void writeDoubleArray(double[] dArr) {
        if (dArr != null) {
            writeInt(dArr.length);
            for (double d : dArr) {
                writeDouble(d);
            }
            return;
        }
        writeInt(-1);
    }

    public final double[] createDoubleArray() {
        int i = readInt();
        if (i >= 0 && i <= (dataAvail() >> 3)) {
            double[] dArr = new double[i];
            for (int i2 = 0; i2 < i; i2++) {
                dArr[i2] = readDouble();
            }
            return dArr;
        }
        return null;
    }

    public final void readDoubleArray(double[] dArr) {
        int i = readInt();
        if (i == dArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                dArr[i2] = readDouble();
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final void writeStringArray(String[] strArr) {
        if (strArr != null) {
            writeInt(strArr.length);
            for (String str : strArr) {
                writeString(str);
            }
            return;
        }
        writeInt(-1);
    }

    public final String[] createStringArray() {
        int i = readInt();
        if (i >= 0) {
            String[] strArr = new String[i];
            for (int i2 = 0; i2 < i; i2++) {
                strArr[i2] = readString();
            }
            return strArr;
        }
        return null;
    }

    public final void readStringArray(String[] strArr) {
        int i = readInt();
        if (i == strArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                strArr[i2] = readString();
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final void writeBinderArray(IBinder[] iBinderArr) {
        if (iBinderArr != null) {
            writeInt(iBinderArr.length);
            for (IBinder iBinder : iBinderArr) {
                writeStrongBinder(iBinder);
            }
            return;
        }
        writeInt(-1);
    }

    public final void writeCharSequenceArray(CharSequence[] charSequenceArr) {
        if (charSequenceArr != null) {
            writeInt(charSequenceArr.length);
            for (CharSequence charSequence : charSequenceArr) {
                writeCharSequence(charSequence);
            }
            return;
        }
        writeInt(-1);
    }

    public final void writeCharSequenceList(ArrayList<CharSequence> arrayList) {
        if (arrayList != null) {
            int size = arrayList.size();
            writeInt(size);
            for (int i = 0; i < size; i++) {
                writeCharSequence(arrayList.get(i));
            }
            return;
        }
        writeInt(-1);
    }

    public final IBinder[] createBinderArray() {
        int i = readInt();
        if (i >= 0) {
            IBinder[] iBinderArr = new IBinder[i];
            for (int i2 = 0; i2 < i; i2++) {
                iBinderArr[i2] = readStrongBinder();
            }
            return iBinderArr;
        }
        return null;
    }

    public final void readBinderArray(IBinder[] iBinderArr) {
        int i = readInt();
        if (i == iBinderArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                iBinderArr[i2] = readStrongBinder();
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final <T extends Parcelable> void writeTypedList(List<T> list) {
        writeTypedList(list, 0);
    }

    public <T extends Parcelable> void writeTypedList(List<T> list, int i) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        int size = list.size();
        writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            writeTypedObject(list.get(i2), i);
        }
    }

    public final void writeStringList(List<String> list) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        int size = list.size();
        writeInt(size);
        for (int i = 0; i < size; i++) {
            writeString(list.get(i));
        }
    }

    public final void writeBinderList(List<IBinder> list) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        int size = list.size();
        writeInt(size);
        for (int i = 0; i < size; i++) {
            writeStrongBinder(list.get(i));
        }
    }

    public final <T extends Parcelable> void writeParcelableList(List<T> list, int i) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        int size = list.size();
        writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            writeParcelable(list.get(i2), i);
        }
    }

    public final <T extends Parcelable> void writeTypedArray(T[] tArr, int i) {
        if (tArr != null) {
            writeInt(tArr.length);
            for (T t : tArr) {
                writeTypedObject(t, i);
            }
            return;
        }
        writeInt(-1);
    }

    public final <T extends Parcelable> void writeTypedObject(T t, int i) {
        if (t != null) {
            writeInt(1);
            t.writeToParcel(this, i);
        } else {
            writeInt(0);
        }
    }

    public final void writeValue(Object obj) {
        if (obj == null) {
            writeInt(-1);
            return;
        }
        if (obj instanceof String) {
            writeInt(0);
            writeString((String) obj);
            return;
        }
        if (obj instanceof Integer) {
            writeInt(1);
            writeInt(((Integer) obj).intValue());
            return;
        }
        if (obj instanceof Map) {
            writeInt(2);
            writeMap((Map) obj);
            return;
        }
        if (obj instanceof Bundle) {
            writeInt(3);
            writeBundle((Bundle) obj);
            return;
        }
        if (obj instanceof PersistableBundle) {
            writeInt(25);
            writePersistableBundle((PersistableBundle) obj);
            return;
        }
        if (obj instanceof Parcelable) {
            writeInt(4);
            writeParcelable((Parcelable) obj, 0);
            return;
        }
        if (obj instanceof Short) {
            writeInt(5);
            writeInt(((Short) obj).intValue());
            return;
        }
        if (obj instanceof Long) {
            writeInt(6);
            writeLong(((Long) obj).longValue());
            return;
        }
        if (obj instanceof Float) {
            writeInt(7);
            writeFloat(((Float) obj).floatValue());
            return;
        }
        if (obj instanceof Double) {
            writeInt(8);
            writeDouble(((Double) obj).doubleValue());
            return;
        }
        if (obj instanceof Boolean) {
            writeInt(9);
            writeInt(((Boolean) obj).booleanValue() ? 1 : 0);
            return;
        }
        if (obj instanceof CharSequence) {
            writeInt(10);
            writeCharSequence((CharSequence) obj);
            return;
        }
        if (obj instanceof List) {
            writeInt(11);
            writeList((List) obj);
            return;
        }
        if (obj instanceof SparseArray) {
            writeInt(12);
            writeSparseArray((SparseArray) obj);
            return;
        }
        if (obj instanceof boolean[]) {
            writeInt(23);
            writeBooleanArray((boolean[]) obj);
            return;
        }
        if (obj instanceof byte[]) {
            writeInt(13);
            writeByteArray((byte[]) obj);
            return;
        }
        if (obj instanceof String[]) {
            writeInt(14);
            writeStringArray((String[]) obj);
            return;
        }
        if (obj instanceof CharSequence[]) {
            writeInt(24);
            writeCharSequenceArray((CharSequence[]) obj);
            return;
        }
        if (obj instanceof IBinder) {
            writeInt(15);
            writeStrongBinder((IBinder) obj);
            return;
        }
        if (obj instanceof Parcelable[]) {
            writeInt(16);
            writeParcelableArray((Parcelable[]) obj, 0);
            return;
        }
        if (obj instanceof int[]) {
            writeInt(18);
            writeIntArray((int[]) obj);
            return;
        }
        if (obj instanceof long[]) {
            writeInt(19);
            writeLongArray((long[]) obj);
            return;
        }
        if (obj instanceof Byte) {
            writeInt(20);
            writeInt(((Byte) obj).byteValue());
            return;
        }
        if (obj instanceof Size) {
            writeInt(26);
            writeSize((Size) obj);
            return;
        }
        if (obj instanceof SizeF) {
            writeInt(27);
            writeSizeF((SizeF) obj);
            return;
        }
        if (obj instanceof double[]) {
            writeInt(28);
            writeDoubleArray((double[]) obj);
            return;
        }
        Class<?> cls = obj.getClass();
        if (cls.isArray() && cls.getComponentType() == Object.class) {
            writeInt(17);
            writeArray((Object[]) obj);
        } else if (obj instanceof Serializable) {
            writeInt(21);
            writeSerializable((Serializable) obj);
        } else {
            throw new RuntimeException("Parcel: unable to marshal value " + obj);
        }
    }

    public final void writeParcelable(Parcelable parcelable, int i) {
        if (parcelable == null) {
            writeString(null);
        } else {
            writeParcelableCreator(parcelable);
            parcelable.writeToParcel(this, i);
        }
    }

    public final void writeParcelableCreator(Parcelable parcelable) {
        writeString(parcelable.getClass().getName());
    }

    public final void writeSerializable(Serializable serializable) {
        if (serializable == null) {
            writeString(null);
            return;
        }
        String name = serializable.getClass().getName();
        writeString(name);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(serializable);
            objectOutputStream.close();
            writeByteArray(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Parcelable encountered IOException writing serializable object (name = " + name + ")", e);
        }
    }

    public static void setStackTraceParceling(boolean z) {
        sParcelExceptionStackTrace = z;
    }

    public final void writeException(Exception exc) {
        int i;
        if ((exc instanceof Parcelable) && exc.getClass().getClassLoader() == Parcelable.class.getClassLoader()) {
            i = -9;
        } else if (exc instanceof SecurityException) {
            i = -1;
        } else if (exc instanceof BadParcelableException) {
            i = -2;
        } else if (exc instanceof IllegalArgumentException) {
            i = -3;
        } else if (exc instanceof NullPointerException) {
            i = -4;
        } else if (exc instanceof IllegalStateException) {
            i = -5;
        } else if (exc instanceof NetworkOnMainThreadException) {
            i = -6;
        } else if (exc instanceof UnsupportedOperationException) {
            i = -7;
        } else if (exc instanceof ServiceSpecificException) {
            i = -8;
        } else {
            i = 0;
        }
        writeInt(i);
        StrictMode.clearGatheredViolations();
        if (i == 0) {
            if (exc instanceof RuntimeException) {
                throw ((RuntimeException) exc);
            }
            throw new RuntimeException(exc);
        }
        writeString(exc.getMessage());
        long jElapsedRealtime = sParcelExceptionStackTrace ? SystemClock.elapsedRealtime() : 0L;
        if (sParcelExceptionStackTrace && jElapsedRealtime - sLastWriteExceptionStackTrace > 1000) {
            sLastWriteExceptionStackTrace = jElapsedRealtime;
            int iDataPosition = dataPosition();
            writeInt(0);
            StackTraceElement[] stackTrace = exc.getStackTrace();
            int iMin = Math.min(stackTrace.length, 5);
            StringBuilder sb = new StringBuilder();
            for (int i2 = 0; i2 < iMin; i2++) {
                sb.append("\tat ");
                sb.append(stackTrace[i2]);
                sb.append('\n');
            }
            writeString(sb.toString());
            int iDataPosition2 = dataPosition();
            setDataPosition(iDataPosition);
            writeInt(iDataPosition2 - iDataPosition);
            setDataPosition(iDataPosition2);
        } else {
            writeInt(0);
        }
        switch (i) {
            case -9:
                int iDataPosition3 = dataPosition();
                writeInt(0);
                writeParcelable((Parcelable) exc, 1);
                int iDataPosition4 = dataPosition();
                setDataPosition(iDataPosition3);
                writeInt(iDataPosition4 - iDataPosition3);
                setDataPosition(iDataPosition4);
                return;
            case -8:
                writeInt(((ServiceSpecificException) exc).errorCode);
                return;
            default:
                return;
        }
    }

    public final void writeNoException() {
        if (StrictMode.hasGatheredViolations()) {
            writeInt(-128);
            int iDataPosition = dataPosition();
            writeInt(0);
            StrictMode.writeGatheredViolationsToParcel(this);
            int iDataPosition2 = dataPosition();
            setDataPosition(iDataPosition);
            writeInt(iDataPosition2 - iDataPosition);
            setDataPosition(iDataPosition2);
            return;
        }
        writeInt(0);
    }

    public final void readException() {
        int exceptionCode = readExceptionCode();
        if (exceptionCode != 0) {
            readException(exceptionCode, readString());
        }
    }

    public final int readExceptionCode() {
        int i = readInt();
        if (i == -128) {
            if (readInt() == 0) {
                Log.e(TAG, "Unexpected zero-sized Parcel reply header.");
                return 0;
            }
            StrictMode.readAndHandleBinderCallViolations(this);
            return 0;
        }
        return i;
    }

    public final void readException(int i, String str) {
        String string;
        if (readInt() > 0) {
            string = readString();
        } else {
            string = null;
        }
        Exception excCreateException = createException(i, str);
        if (string != null) {
            RemoteException remoteException = new RemoteException("Remote stack trace:\n" + string, null, false, false);
            try {
                Throwable rootCause = ExceptionUtils.getRootCause(excCreateException);
                if (rootCause != null) {
                    rootCause.initCause(remoteException);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Cannot set cause " + remoteException + " for " + excCreateException, e);
            }
        }
        SneakyThrow.sneakyThrow(excCreateException);
    }

    private Exception createException(int i, String str) {
        switch (i) {
            case -9:
                if (readInt() > 0) {
                    return (Exception) readParcelable(Parcelable.class.getClassLoader());
                }
                return new RuntimeException(str + " [missing Parcelable]");
            case -8:
                return new ServiceSpecificException(readInt(), str);
            case -7:
                return new UnsupportedOperationException(str);
            case -6:
                return new NetworkOnMainThreadException();
            case -5:
                return new IllegalStateException(str);
            case -4:
                return new NullPointerException(str);
            case -3:
                return new IllegalArgumentException(str);
            case -2:
                return new BadParcelableException(str);
            case -1:
                return new SecurityException(str);
            default:
                return new RuntimeException("Unknown exception code: " + i + " msg " + str);
        }
    }

    public final int readInt() {
        return nativeReadInt(this.mNativePtr);
    }

    public final long readLong() {
        return nativeReadLong(this.mNativePtr);
    }

    public final float readFloat() {
        return nativeReadFloat(this.mNativePtr);
    }

    public final double readDouble() {
        return nativeReadDouble(this.mNativePtr);
    }

    public final String readString() {
        return this.mReadWriteHelper.readString(this);
    }

    public String readStringNoHelper() {
        return nativeReadString(this.mNativePtr);
    }

    public final boolean readBoolean() {
        return readInt() != 0;
    }

    public final CharSequence readCharSequence() {
        return TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(this);
    }

    public final IBinder readStrongBinder() {
        return nativeReadStrongBinder(this.mNativePtr);
    }

    public final ParcelFileDescriptor readFileDescriptor() {
        FileDescriptor fileDescriptorNativeReadFileDescriptor = nativeReadFileDescriptor(this.mNativePtr);
        if (fileDescriptorNativeReadFileDescriptor != null) {
            return new ParcelFileDescriptor(fileDescriptorNativeReadFileDescriptor);
        }
        return null;
    }

    public final FileDescriptor readRawFileDescriptor() {
        return nativeReadFileDescriptor(this.mNativePtr);
    }

    public final FileDescriptor[] createRawFileDescriptorArray() {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        FileDescriptor[] fileDescriptorArr = new FileDescriptor[i];
        for (int i2 = 0; i2 < i; i2++) {
            fileDescriptorArr[i2] = readRawFileDescriptor();
        }
        return fileDescriptorArr;
    }

    public final void readRawFileDescriptorArray(FileDescriptor[] fileDescriptorArr) {
        int i = readInt();
        if (i == fileDescriptorArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                fileDescriptorArr[i2] = readRawFileDescriptor();
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    public final byte readByte() {
        return (byte) (readInt() & 255);
    }

    public final void readMap(Map map, ClassLoader classLoader) {
        readMapInternal(map, readInt(), classLoader);
    }

    public final void readList(List list, ClassLoader classLoader) {
        readListInternal(list, readInt(), classLoader);
    }

    public final HashMap readHashMap(ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        HashMap map = new HashMap(i);
        readMapInternal(map, i, classLoader);
        return map;
    }

    public final Bundle readBundle() {
        return readBundle(null);
    }

    public final Bundle readBundle(ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        Bundle bundle = new Bundle(this, i);
        if (classLoader != null) {
            bundle.setClassLoader(classLoader);
        }
        return bundle;
    }

    public final PersistableBundle readPersistableBundle() {
        return readPersistableBundle(null);
    }

    public final PersistableBundle readPersistableBundle(ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        PersistableBundle persistableBundle = new PersistableBundle(this, i);
        if (classLoader != null) {
            persistableBundle.setClassLoader(classLoader);
        }
        return persistableBundle;
    }

    public final Size readSize() {
        return new Size(readInt(), readInt());
    }

    public final SizeF readSizeF() {
        return new SizeF(readFloat(), readFloat());
    }

    public final byte[] createByteArray() {
        return nativeCreateByteArray(this.mNativePtr);
    }

    public final void readByteArray(byte[] bArr) {
        if (!nativeReadByteArray(this.mNativePtr, bArr, bArr != null ? bArr.length : 0)) {
            throw new RuntimeException("bad array lengths");
        }
    }

    public final byte[] readBlob() {
        return nativeReadBlob(this.mNativePtr);
    }

    public final String[] readStringArray() {
        int i = readInt();
        if (i >= 0) {
            String[] strArr = new String[i];
            for (int i2 = 0; i2 < i; i2++) {
                strArr[i2] = readString();
            }
            return strArr;
        }
        return null;
    }

    public final CharSequence[] readCharSequenceArray() {
        int i = readInt();
        if (i >= 0) {
            CharSequence[] charSequenceArr = new CharSequence[i];
            for (int i2 = 0; i2 < i; i2++) {
                charSequenceArr[i2] = readCharSequence();
            }
            return charSequenceArr;
        }
        return null;
    }

    public final ArrayList<CharSequence> readCharSequenceList() {
        int i = readInt();
        if (i >= 0) {
            ArrayList<CharSequence> arrayList = new ArrayList<>(i);
            for (int i2 = 0; i2 < i; i2++) {
                arrayList.add(readCharSequence());
            }
            return arrayList;
        }
        return null;
    }

    public final ArrayList readArrayList(ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        ArrayList arrayList = new ArrayList(i);
        readListInternal(arrayList, i, classLoader);
        return arrayList;
    }

    public final Object[] readArray(ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        Object[] objArr = new Object[i];
        readArrayInternal(objArr, i, classLoader);
        return objArr;
    }

    public final SparseArray readSparseArray(ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        SparseArray sparseArray = new SparseArray(i);
        readSparseArrayInternal(sparseArray, i, classLoader);
        return sparseArray;
    }

    public final SparseBooleanArray readSparseBooleanArray() {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray(i);
        readSparseBooleanArrayInternal(sparseBooleanArray, i);
        return sparseBooleanArray;
    }

    public final SparseIntArray readSparseIntArray() {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        SparseIntArray sparseIntArray = new SparseIntArray(i);
        readSparseIntArrayInternal(sparseIntArray, i);
        return sparseIntArray;
    }

    public final <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> creator) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        GridLayout.Assoc assoc = (ArrayList<T>) new ArrayList(i);
        while (i > 0) {
            assoc.add(readTypedObject(creator));
            i--;
        }
        return assoc;
    }

    public final <T> void readTypedList(List<T> list, Parcelable.Creator<T> creator) {
        int size = list.size();
        int i = readInt();
        int i2 = 0;
        while (i2 < size && i2 < i) {
            list.set(i2, readTypedObject(creator));
            i2++;
        }
        while (i2 < i) {
            list.add(readTypedObject(creator));
            i2++;
        }
        while (i2 < size) {
            list.remove(i);
            i2++;
        }
    }

    public final ArrayList<String> createStringArrayList() {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        ArrayList<String> arrayList = new ArrayList<>(i);
        while (i > 0) {
            arrayList.add(readString());
            i--;
        }
        return arrayList;
    }

    public final ArrayList<IBinder> createBinderArrayList() {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        ArrayList<IBinder> arrayList = new ArrayList<>(i);
        while (i > 0) {
            arrayList.add(readStrongBinder());
            i--;
        }
        return arrayList;
    }

    public final void readStringList(List<String> list) {
        int size = list.size();
        int i = readInt();
        int i2 = 0;
        while (i2 < size && i2 < i) {
            list.set(i2, readString());
            i2++;
        }
        while (i2 < i) {
            list.add(readString());
            i2++;
        }
        while (i2 < size) {
            list.remove(i);
            i2++;
        }
    }

    public final void readBinderList(List<IBinder> list) {
        int size = list.size();
        int i = readInt();
        int i2 = 0;
        while (i2 < size && i2 < i) {
            list.set(i2, readStrongBinder());
            i2++;
        }
        while (i2 < i) {
            list.add(readStrongBinder());
            i2++;
        }
        while (i2 < size) {
            list.remove(i);
            i2++;
        }
    }

    public final <T extends Parcelable> List<T> readParcelableList(List<T> list, ClassLoader classLoader) {
        int i = readInt();
        if (i == -1) {
            list.clear();
            return list;
        }
        int size = list.size();
        int i2 = 0;
        while (i2 < size && i2 < i) {
            list.set(i2, readParcelable(classLoader));
            i2++;
        }
        while (i2 < i) {
            list.add(readParcelable(classLoader));
            i2++;
        }
        while (i2 < size) {
            list.remove(i);
            i2++;
        }
        return list;
    }

    public final <T> T[] createTypedArray(Parcelable.Creator<T> creator) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        T[] tArrNewArray = creator.newArray(i);
        for (int i2 = 0; i2 < i; i2++) {
            tArrNewArray[i2] = readTypedObject(creator);
        }
        return tArrNewArray;
    }

    public final <T> void readTypedArray(T[] tArr, Parcelable.Creator<T> creator) {
        int i = readInt();
        if (i == tArr.length) {
            for (int i2 = 0; i2 < i; i2++) {
                tArr[i2] = readTypedObject(creator);
            }
            return;
        }
        throw new RuntimeException("bad array lengths");
    }

    @Deprecated
    public final <T> T[] readTypedArray(Parcelable.Creator<T> creator) {
        return (T[]) createTypedArray(creator);
    }

    public final <T> T readTypedObject(Parcelable.Creator<T> creator) {
        if (readInt() != 0) {
            return creator.createFromParcel(this);
        }
        return null;
    }

    public final <T extends Parcelable> void writeParcelableArray(T[] tArr, int i) {
        if (tArr != null) {
            writeInt(tArr.length);
            for (T t : tArr) {
                writeParcelable(t, i);
            }
            return;
        }
        writeInt(-1);
    }

    public final Object readValue(ClassLoader classLoader) {
        int i = readInt();
        switch (i) {
            case -1:
                return null;
            case 0:
                return readString();
            case 1:
                return Integer.valueOf(readInt());
            case 2:
                return readHashMap(classLoader);
            case 3:
                return readBundle(classLoader);
            case 4:
                return readParcelable(classLoader);
            case 5:
                return Short.valueOf((short) readInt());
            case 6:
                return Long.valueOf(readLong());
            case 7:
                return Float.valueOf(readFloat());
            case 8:
                return Double.valueOf(readDouble());
            case 9:
                return Boolean.valueOf(readInt() == 1);
            case 10:
                return readCharSequence();
            case 11:
                return readArrayList(classLoader);
            case 12:
                return readSparseArray(classLoader);
            case 13:
                return createByteArray();
            case 14:
                return readStringArray();
            case 15:
                return readStrongBinder();
            case 16:
                return readParcelableArray(classLoader);
            case 17:
                return readArray(classLoader);
            case 18:
                return createIntArray();
            case 19:
                return createLongArray();
            case 20:
                return Byte.valueOf(readByte());
            case 21:
                return readSerializable(classLoader);
            case 22:
                return readSparseBooleanArray();
            case 23:
                return createBooleanArray();
            case 24:
                return readCharSequenceArray();
            case 25:
                return readPersistableBundle(classLoader);
            case 26:
                return readSize();
            case 27:
                return readSizeF();
            case 28:
                return createDoubleArray();
            default:
                throw new RuntimeException("Parcel " + this + ": Unmarshalling unknown type code " + i + " at offset " + (dataPosition() - 4));
        }
    }

    public final <T extends Parcelable> T readParcelable(ClassLoader classLoader) {
        Parcelable.Creator<?> parcelableCreator = readParcelableCreator(classLoader);
        if (parcelableCreator == null) {
            return null;
        }
        if (parcelableCreator instanceof Parcelable.ClassLoaderCreator) {
            return (T) ((Parcelable.ClassLoaderCreator) parcelableCreator).createFromParcel(this, classLoader);
        }
        return (T) parcelableCreator.createFromParcel(this);
    }

    public final <T extends Parcelable> T readCreator(Parcelable.Creator<?> creator, ClassLoader classLoader) {
        if (creator instanceof Parcelable.ClassLoaderCreator) {
            return (T) ((Parcelable.ClassLoaderCreator) creator).createFromParcel(this, classLoader);
        }
        return (T) creator.createFromParcel(this);
    }

    public final Parcelable.Creator<?> readParcelableCreator(ClassLoader classLoader) {
        Parcelable.Creator<?> creator;
        String string = readString();
        if (string == null) {
            return null;
        }
        synchronized (mCreators) {
            HashMap<String, Parcelable.Creator<?>> map = mCreators.get(classLoader);
            if (map == null) {
                map = new HashMap<>();
                mCreators.put(classLoader, map);
            }
            creator = map.get(string);
            if (creator == null) {
                if (classLoader == null) {
                    try {
                        try {
                            try {
                                classLoader = getClass().getClassLoader();
                            } catch (ClassNotFoundException e) {
                                Log.e(TAG, "Class not found when unmarshalling: " + string, e);
                                throw new BadParcelableException("ClassNotFoundException when unmarshalling: " + string);
                            }
                        } catch (NoSuchFieldException e2) {
                            throw new BadParcelableException("Parcelable protocol requires a Parcelable.Creator object called CREATOR on class " + string);
                        }
                    } catch (IllegalAccessException e3) {
                        Log.e(TAG, "Illegal access when unmarshalling: " + string, e3);
                        throw new BadParcelableException("IllegalAccessException when unmarshalling: " + string);
                    }
                }
                Class<?> cls = Class.forName(string, false, classLoader);
                if (!Parcelable.class.isAssignableFrom(cls)) {
                    throw new BadParcelableException("Parcelable protocol requires subclassing from Parcelable on class " + string);
                }
                Field field = cls.getField("CREATOR");
                if ((field.getModifiers() & 8) == 0) {
                    throw new BadParcelableException("Parcelable protocol requires the CREATOR object to be static on class " + string);
                }
                if (!Parcelable.Creator.class.isAssignableFrom(field.getType())) {
                    throw new BadParcelableException("Parcelable protocol requires a Parcelable.Creator object called CREATOR on class " + string);
                }
                creator = (Parcelable.Creator) field.get(null);
                if (creator == null) {
                    throw new BadParcelableException("Parcelable protocol requires a non-null Parcelable.Creator object called CREATOR on class " + string);
                }
                map.put(string, creator);
            }
        }
        return creator;
    }

    public final Parcelable[] readParcelableArray(ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        Parcelable[] parcelableArr = new Parcelable[i];
        for (int i2 = 0; i2 < i; i2++) {
            parcelableArr[i2] = readParcelable(classLoader);
        }
        return parcelableArr;
    }

    public final <T extends Parcelable> T[] readParcelableArray(ClassLoader classLoader, Class<T> cls) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        T[] tArr = (T[]) ((Parcelable[]) Array.newInstance((Class<?>) cls, i));
        for (int i2 = 0; i2 < i; i2++) {
            tArr[i2] = readParcelable(classLoader);
        }
        return tArr;
    }

    public final Serializable readSerializable() {
        return readSerializable(null);
    }

    private final Serializable readSerializable(final ClassLoader classLoader) {
        String string = readString();
        if (string == null) {
            return null;
        }
        try {
            return (Serializable) new ObjectInputStream(new ByteArrayInputStream(createByteArray())) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException {
                    Class<?> cls;
                    if (classLoader != null && (cls = Class.forName(objectStreamClass.getName(), false, classLoader)) != null) {
                        return cls;
                    }
                    return super.resolveClass(objectStreamClass);
                }
            }.readObject();
        } catch (IOException e) {
            throw new RuntimeException("Parcelable encountered IOException reading a Serializable object (name = " + string + ")", e);
        } catch (ClassNotFoundException e2) {
            throw new RuntimeException("Parcelable encountered ClassNotFoundException reading a Serializable object (name = " + string + ")", e2);
        }
    }

    protected static final Parcel obtain(int i) {
        throw new UnsupportedOperationException();
    }

    protected static final Parcel obtain(long j) {
        Parcel[] parcelArr = sHolderPool;
        synchronized (parcelArr) {
            for (int i = 0; i < 6; i++) {
                try {
                    Parcel parcel = parcelArr[i];
                    if (parcel != null) {
                        parcelArr[i] = null;
                        parcel.init(j);
                        return parcel;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return new Parcel(j);
        }
    }

    private Parcel(long j) {
        init(j);
    }

    private void init(long j) {
        if (j != 0) {
            this.mNativePtr = j;
            this.mOwnsNativeParcelObject = false;
        } else {
            this.mNativePtr = nativeCreate();
            this.mOwnsNativeParcelObject = true;
        }
    }

    private void freeBuffer() {
        if (this.mOwnsNativeParcelObject) {
            updateNativeSize(nativeFreeBuffer(this.mNativePtr));
        }
        this.mReadWriteHelper = ReadWriteHelper.DEFAULT;
    }

    private void destroy() {
        if (this.mNativePtr != 0) {
            if (this.mOwnsNativeParcelObject) {
                nativeDestroy(this.mNativePtr);
                updateNativeSize(0L);
            }
            this.mNativePtr = 0L;
        }
        this.mReadWriteHelper = null;
    }

    protected void finalize() throws Throwable {
        destroy();
    }

    void readMapInternal(Map map, int i, ClassLoader classLoader) {
        while (i > 0) {
            map.put(readValue(classLoader), readValue(classLoader));
            i--;
        }
    }

    void readArrayMapInternal(ArrayMap arrayMap, int i, ClassLoader classLoader) {
        while (i > 0) {
            arrayMap.append(readString(), readValue(classLoader));
            i--;
        }
        arrayMap.validate();
    }

    void readArrayMapSafelyInternal(ArrayMap arrayMap, int i, ClassLoader classLoader) {
        while (i > 0) {
            arrayMap.put(readString(), readValue(classLoader));
            i--;
        }
    }

    public void readArrayMap(ArrayMap arrayMap, ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return;
        }
        readArrayMapInternal(arrayMap, i, classLoader);
    }

    public ArraySet<? extends Object> readArraySet(ClassLoader classLoader) {
        int i = readInt();
        if (i < 0) {
            return null;
        }
        ArraySet<? extends Object> arraySet = new ArraySet<>(i);
        for (int i2 = 0; i2 < i; i2++) {
            arraySet.append(readValue(classLoader));
        }
        return arraySet;
    }

    private void readListInternal(List list, int i, ClassLoader classLoader) {
        while (i > 0) {
            list.add(readValue(classLoader));
            i--;
        }
    }

    private void readArrayInternal(Object[] objArr, int i, ClassLoader classLoader) {
        for (int i2 = 0; i2 < i; i2++) {
            objArr[i2] = readValue(classLoader);
        }
    }

    private void readSparseArrayInternal(SparseArray sparseArray, int i, ClassLoader classLoader) {
        while (i > 0) {
            sparseArray.append(readInt(), readValue(classLoader));
            i--;
        }
    }

    private void readSparseBooleanArrayInternal(SparseBooleanArray sparseBooleanArray, int i) {
        while (i > 0) {
            int i2 = readInt();
            boolean z = true;
            if (readByte() != 1) {
                z = false;
            }
            sparseBooleanArray.append(i2, z);
            i--;
        }
    }

    private void readSparseIntArrayInternal(SparseIntArray sparseIntArray, int i) {
        while (i > 0) {
            sparseIntArray.append(readInt(), readInt());
            i--;
        }
    }

    public long getBlobAshmemSize() {
        return nativeGetBlobAshmemSize(this.mNativePtr);
    }
}
