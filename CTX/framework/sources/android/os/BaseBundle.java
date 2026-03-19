package android.os;

import android.util.ArrayMap;
import android.util.Log;
import android.util.MathUtils;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

public class BaseBundle {
    private static final int BUNDLE_MAGIC = 1279544898;
    private static final int BUNDLE_MAGIC_NATIVE = 1279544900;
    static final boolean DEBUG = false;
    static final int FLAG_DEFUSABLE = 1;
    private static final boolean LOG_DEFUSABLE = false;
    private static final String TAG = "Bundle";
    private static volatile boolean sShouldDefuse = false;
    private ClassLoader mClassLoader;

    @VisibleForTesting
    public int mFlags;
    ArrayMap<String, Object> mMap;
    private boolean mParcelledByNative;
    Parcel mParcelledData;

    public static void setShouldDefuse(boolean z) {
        sShouldDefuse = z;
    }

    static final class NoImagePreloadHolder {
        public static final Parcel EMPTY_PARCEL = Parcel.obtain();

        NoImagePreloadHolder() {
        }
    }

    BaseBundle(ClassLoader classLoader, int i) {
        this.mMap = null;
        this.mParcelledData = null;
        this.mMap = i > 0 ? new ArrayMap<>(i) : new ArrayMap<>();
        this.mClassLoader = classLoader == null ? getClass().getClassLoader() : classLoader;
    }

    BaseBundle() {
        this((ClassLoader) null, 0);
    }

    BaseBundle(Parcel parcel) {
        this.mMap = null;
        this.mParcelledData = null;
        readFromParcelInner(parcel);
    }

    BaseBundle(Parcel parcel, int i) {
        this.mMap = null;
        this.mParcelledData = null;
        readFromParcelInner(parcel, i);
    }

    BaseBundle(ClassLoader classLoader) {
        this(classLoader, 0);
    }

    BaseBundle(int i) {
        this((ClassLoader) null, i);
    }

    BaseBundle(BaseBundle baseBundle) {
        this.mMap = null;
        this.mParcelledData = null;
        copyInternal(baseBundle, false);
    }

    BaseBundle(boolean z) {
        this.mMap = null;
        this.mParcelledData = null;
    }

    public String getPairValue() {
        unparcel();
        int size = this.mMap.size();
        if (size > 1) {
            Log.w(TAG, "getPairValue() used on Bundle with multiple pairs.");
        }
        if (size == 0) {
            return null;
        }
        Object objValueAt = this.mMap.valueAt(0);
        try {
            return (String) objValueAt;
        } catch (ClassCastException e) {
            typeWarning("getPairValue()", objValueAt, "String", e);
            return null;
        }
    }

    void setClassLoader(ClassLoader classLoader) {
        this.mClassLoader = classLoader;
    }

    ClassLoader getClassLoader() {
        return this.mClassLoader;
    }

    void unparcel() {
        synchronized (this) {
            Parcel parcel = this.mParcelledData;
            if (parcel != null) {
                initializeFromParcelLocked(parcel, true, this.mParcelledByNative);
            }
        }
    }

    private void initializeFromParcelLocked(Parcel parcel, boolean z, boolean z2) {
        if (isEmptyParcel(parcel)) {
            if (this.mMap == null) {
                this.mMap = new ArrayMap<>(1);
            } else {
                this.mMap.erase();
            }
            this.mParcelledData = null;
            this.mParcelledByNative = false;
            return;
        }
        int i = parcel.readInt();
        if (i < 0) {
            return;
        }
        ArrayMap<String, Object> arrayMap = this.mMap;
        if (arrayMap == null) {
            arrayMap = new ArrayMap<>(i);
        } else {
            arrayMap.erase();
            arrayMap.ensureCapacity(i);
        }
        try {
            try {
                if (z2) {
                    parcel.readArrayMapSafelyInternal(arrayMap, i, this.mClassLoader);
                } else {
                    parcel.readArrayMapInternal(arrayMap, i, this.mClassLoader);
                }
                this.mMap = arrayMap;
            } catch (BadParcelableException e) {
                if (!sShouldDefuse) {
                    throw e;
                }
                Log.w(TAG, "Failed to parse Bundle, but defusing quietly", e);
                arrayMap.erase();
                this.mMap = arrayMap;
                if (z) {
                }
            }
            if (z) {
                recycleParcel(parcel);
            }
            this.mParcelledData = null;
            this.mParcelledByNative = false;
        } catch (Throwable th) {
            this.mMap = arrayMap;
            if (z) {
                recycleParcel(parcel);
            }
            this.mParcelledData = null;
            this.mParcelledByNative = false;
            throw th;
        }
    }

    public boolean isParcelled() {
        return this.mParcelledData != null;
    }

    public boolean isEmptyParcel() {
        return isEmptyParcel(this.mParcelledData);
    }

    private static boolean isEmptyParcel(Parcel parcel) {
        return parcel == NoImagePreloadHolder.EMPTY_PARCEL;
    }

    private static void recycleParcel(Parcel parcel) {
        if (parcel != null && !isEmptyParcel(parcel)) {
            parcel.recycle();
        }
    }

    ArrayMap<String, Object> getMap() {
        unparcel();
        return this.mMap;
    }

    public int size() {
        unparcel();
        return this.mMap.size();
    }

    public boolean isEmpty() {
        unparcel();
        return this.mMap.isEmpty();
    }

    public boolean maybeIsEmpty() {
        if (isParcelled()) {
            return isEmptyParcel();
        }
        return isEmpty();
    }

    public static boolean kindofEquals(BaseBundle baseBundle, BaseBundle baseBundle2) {
        return baseBundle == baseBundle2 || (baseBundle != null && baseBundle.kindofEquals(baseBundle2));
    }

    public boolean kindofEquals(BaseBundle baseBundle) {
        if (baseBundle == null || isParcelled() != baseBundle.isParcelled()) {
            return false;
        }
        if (isParcelled()) {
            return this.mParcelledData.compareData(baseBundle.mParcelledData) == 0;
        }
        return this.mMap.equals(baseBundle.mMap);
    }

    public void clear() {
        unparcel();
        this.mMap.clear();
    }

    void copyInternal(BaseBundle baseBundle, boolean z) {
        synchronized (baseBundle) {
            if (baseBundle.mParcelledData != null) {
                if (baseBundle.isEmptyParcel()) {
                    this.mParcelledData = NoImagePreloadHolder.EMPTY_PARCEL;
                    this.mParcelledByNative = false;
                } else {
                    this.mParcelledData = Parcel.obtain();
                    this.mParcelledData.appendFrom(baseBundle.mParcelledData, 0, baseBundle.mParcelledData.dataSize());
                    this.mParcelledData.setDataPosition(0);
                    this.mParcelledByNative = baseBundle.mParcelledByNative;
                }
            } else {
                this.mParcelledData = null;
                this.mParcelledByNative = false;
            }
            if (baseBundle.mMap != null) {
                if (!z) {
                    this.mMap = new ArrayMap<>(baseBundle.mMap);
                } else {
                    ArrayMap<String, Object> arrayMap = baseBundle.mMap;
                    int size = arrayMap.size();
                    this.mMap = new ArrayMap<>(size);
                    for (int i = 0; i < size; i++) {
                        this.mMap.append(arrayMap.keyAt(i), deepCopyValue(arrayMap.valueAt(i)));
                    }
                }
            } else {
                this.mMap = null;
            }
            this.mClassLoader = baseBundle.mClassLoader;
        }
    }

    Object deepCopyValue(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Bundle) {
            return ((Bundle) obj).deepCopy();
        }
        if (obj instanceof PersistableBundle) {
            return ((PersistableBundle) obj).deepCopy();
        }
        if (obj instanceof ArrayList) {
            return deepcopyArrayList((ArrayList) obj);
        }
        if (obj.getClass().isArray()) {
            if (obj instanceof int[]) {
                return ((int[]) obj).clone();
            }
            if (obj instanceof long[]) {
                return ((long[]) obj).clone();
            }
            if (obj instanceof float[]) {
                return ((float[]) obj).clone();
            }
            if (obj instanceof double[]) {
                return ((double[]) obj).clone();
            }
            if (obj instanceof Object[]) {
                return ((Object[]) obj).clone();
            }
            if (obj instanceof byte[]) {
                return ((byte[]) obj).clone();
            }
            if (obj instanceof short[]) {
                return ((short[]) obj).clone();
            }
            if (obj instanceof char[]) {
                return ((char[]) obj).clone();
            }
        }
        return obj;
    }

    ArrayList deepcopyArrayList(ArrayList arrayList) {
        int size = arrayList.size();
        ArrayList arrayList2 = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            arrayList2.add(deepCopyValue(arrayList.get(i)));
        }
        return arrayList2;
    }

    public boolean containsKey(String str) {
        unparcel();
        return this.mMap.containsKey(str);
    }

    public Object get(String str) {
        unparcel();
        return this.mMap.get(str);
    }

    public void remove(String str) {
        unparcel();
        this.mMap.remove(str);
    }

    public void putAll(PersistableBundle persistableBundle) {
        unparcel();
        persistableBundle.unparcel();
        this.mMap.putAll((ArrayMap<? extends String, ? extends Object>) persistableBundle.mMap);
    }

    void putAll(ArrayMap arrayMap) {
        unparcel();
        this.mMap.putAll((ArrayMap<? extends String, ? extends Object>) arrayMap);
    }

    public Set<String> keySet() {
        unparcel();
        return this.mMap.keySet();
    }

    public void putBoolean(String str, boolean z) {
        unparcel();
        this.mMap.put(str, Boolean.valueOf(z));
    }

    void putByte(String str, byte b) {
        unparcel();
        this.mMap.put(str, Byte.valueOf(b));
    }

    void putChar(String str, char c) {
        unparcel();
        this.mMap.put(str, Character.valueOf(c));
    }

    void putShort(String str, short s) {
        unparcel();
        this.mMap.put(str, Short.valueOf(s));
    }

    public void putInt(String str, int i) {
        unparcel();
        this.mMap.put(str, Integer.valueOf(i));
    }

    public void putLong(String str, long j) {
        unparcel();
        this.mMap.put(str, Long.valueOf(j));
    }

    void putFloat(String str, float f) {
        unparcel();
        this.mMap.put(str, Float.valueOf(f));
    }

    public void putDouble(String str, double d) {
        unparcel();
        this.mMap.put(str, Double.valueOf(d));
    }

    public void putString(String str, String str2) {
        unparcel();
        this.mMap.put(str, str2);
    }

    void putCharSequence(String str, CharSequence charSequence) {
        unparcel();
        this.mMap.put(str, charSequence);
    }

    void putIntegerArrayList(String str, ArrayList<Integer> arrayList) {
        unparcel();
        this.mMap.put(str, arrayList);
    }

    void putStringArrayList(String str, ArrayList<String> arrayList) {
        unparcel();
        this.mMap.put(str, arrayList);
    }

    void putCharSequenceArrayList(String str, ArrayList<CharSequence> arrayList) {
        unparcel();
        this.mMap.put(str, arrayList);
    }

    void putSerializable(String str, Serializable serializable) {
        unparcel();
        this.mMap.put(str, serializable);
    }

    public void putBooleanArray(String str, boolean[] zArr) {
        unparcel();
        this.mMap.put(str, zArr);
    }

    void putByteArray(String str, byte[] bArr) {
        unparcel();
        this.mMap.put(str, bArr);
    }

    void putShortArray(String str, short[] sArr) {
        unparcel();
        this.mMap.put(str, sArr);
    }

    void putCharArray(String str, char[] cArr) {
        unparcel();
        this.mMap.put(str, cArr);
    }

    public void putIntArray(String str, int[] iArr) {
        unparcel();
        this.mMap.put(str, iArr);
    }

    public void putLongArray(String str, long[] jArr) {
        unparcel();
        this.mMap.put(str, jArr);
    }

    void putFloatArray(String str, float[] fArr) {
        unparcel();
        this.mMap.put(str, fArr);
    }

    public void putDoubleArray(String str, double[] dArr) {
        unparcel();
        this.mMap.put(str, dArr);
    }

    public void putStringArray(String str, String[] strArr) {
        unparcel();
        this.mMap.put(str, strArr);
    }

    void putCharSequenceArray(String str, CharSequence[] charSequenceArr) {
        unparcel();
        this.mMap.put(str, charSequenceArr);
    }

    public boolean getBoolean(String str) {
        unparcel();
        return getBoolean(str, false);
    }

    void typeWarning(String str, Object obj, String str2, Object obj2, ClassCastException classCastException) {
        Log.w(TAG, "Key " + str + " expected " + str2 + " but value was a " + obj.getClass().getName() + ".  The default value " + obj2 + " was returned.");
        Log.w(TAG, "Attempt to cast generated internal exception:", classCastException);
    }

    void typeWarning(String str, Object obj, String str2, ClassCastException classCastException) {
        typeWarning(str, obj, str2, "<null>", classCastException);
    }

    public boolean getBoolean(String str, boolean z) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return z;
        }
        try {
            return ((Boolean) obj).booleanValue();
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Boolean", Boolean.valueOf(z), e);
            return z;
        }
    }

    byte getByte(String str) {
        unparcel();
        return getByte(str, (byte) 0).byteValue();
    }

    Byte getByte(String str, byte b) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return Byte.valueOf(b);
        }
        try {
            return (Byte) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Byte", Byte.valueOf(b), e);
            return Byte.valueOf(b);
        }
    }

    char getChar(String str) {
        unparcel();
        return getChar(str, (char) 0);
    }

    char getChar(String str, char c) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return c;
        }
        try {
            return ((Character) obj).charValue();
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Character", Character.valueOf(c), e);
            return c;
        }
    }

    short getShort(String str) {
        unparcel();
        return getShort(str, (short) 0);
    }

    short getShort(String str, short s) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return s;
        }
        try {
            return ((Short) obj).shortValue();
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Short", Short.valueOf(s), e);
            return s;
        }
    }

    public int getInt(String str) {
        unparcel();
        return getInt(str, 0);
    }

    public int getInt(String str, int i) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return i;
        }
        try {
            return ((Integer) obj).intValue();
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Integer", Integer.valueOf(i), e);
            return i;
        }
    }

    public long getLong(String str) {
        unparcel();
        return getLong(str, 0L);
    }

    public long getLong(String str, long j) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return j;
        }
        try {
            return ((Long) obj).longValue();
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Long", Long.valueOf(j), e);
            return j;
        }
    }

    float getFloat(String str) {
        unparcel();
        return getFloat(str, 0.0f);
    }

    float getFloat(String str, float f) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return f;
        }
        try {
            return ((Float) obj).floatValue();
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Float", Float.valueOf(f), e);
            return f;
        }
    }

    public double getDouble(String str) {
        unparcel();
        return getDouble(str, 0.0d);
    }

    public double getDouble(String str, double d) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return d;
        }
        try {
            return ((Double) obj).doubleValue();
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Double", Double.valueOf(d), e);
            return d;
        }
    }

    public String getString(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        try {
            return (String) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "String", e);
            return null;
        }
    }

    public String getString(String str, String str2) {
        String string = getString(str);
        return string == null ? str2 : string;
    }

    CharSequence getCharSequence(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        try {
            return (CharSequence) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "CharSequence", e);
            return null;
        }
    }

    CharSequence getCharSequence(String str, CharSequence charSequence) {
        CharSequence charSequence2 = getCharSequence(str);
        return charSequence2 == null ? charSequence : charSequence2;
    }

    Serializable getSerializable(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (Serializable) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Serializable", e);
            return null;
        }
    }

    ArrayList<Integer> getIntegerArrayList(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (ArrayList) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "ArrayList<Integer>", e);
            return null;
        }
    }

    ArrayList<String> getStringArrayList(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (ArrayList) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "ArrayList<String>", e);
            return null;
        }
    }

    ArrayList<CharSequence> getCharSequenceArrayList(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (ArrayList) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "ArrayList<CharSequence>", e);
            return null;
        }
    }

    public boolean[] getBooleanArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (boolean[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "byte[]", e);
            return null;
        }
    }

    byte[] getByteArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (byte[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "byte[]", e);
            return null;
        }
    }

    short[] getShortArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (short[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "short[]", e);
            return null;
        }
    }

    char[] getCharArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (char[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "char[]", e);
            return null;
        }
    }

    public int[] getIntArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (int[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "int[]", e);
            return null;
        }
    }

    public long[] getLongArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (long[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "long[]", e);
            return null;
        }
    }

    float[] getFloatArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (float[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "float[]", e);
            return null;
        }
    }

    public double[] getDoubleArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (double[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "double[]", e);
            return null;
        }
    }

    public String[] getStringArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (String[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "String[]", e);
            return null;
        }
    }

    CharSequence[] getCharSequenceArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (CharSequence[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "CharSequence[]", e);
            return null;
        }
    }

    void writeToParcelInner(Parcel parcel, int i) {
        if (parcel.hasReadWriteHelper()) {
            unparcel();
        }
        synchronized (this) {
            Parcel parcel2 = this.mParcelledData;
            int i2 = BUNDLE_MAGIC;
            if (parcel2 != null) {
                if (this.mParcelledData == NoImagePreloadHolder.EMPTY_PARCEL) {
                    parcel.writeInt(0);
                } else {
                    int iDataSize = this.mParcelledData.dataSize();
                    parcel.writeInt(iDataSize);
                    if (this.mParcelledByNative) {
                        i2 = BUNDLE_MAGIC_NATIVE;
                    }
                    parcel.writeInt(i2);
                    parcel.appendFrom(this.mParcelledData, 0, iDataSize);
                }
                return;
            }
            ArrayMap<String, Object> arrayMap = this.mMap;
            if (arrayMap == null || arrayMap.size() <= 0) {
                parcel.writeInt(0);
                return;
            }
            int iDataPosition = parcel.dataPosition();
            parcel.writeInt(-1);
            parcel.writeInt(BUNDLE_MAGIC);
            int iDataPosition2 = parcel.dataPosition();
            parcel.writeArrayMapInternal(arrayMap);
            int iDataPosition3 = parcel.dataPosition();
            parcel.setDataPosition(iDataPosition);
            parcel.writeInt(iDataPosition3 - iDataPosition2);
            parcel.setDataPosition(iDataPosition3);
        }
    }

    void readFromParcelInner(Parcel parcel) {
        readFromParcelInner(parcel, parcel.readInt());
    }

    private void readFromParcelInner(Parcel parcel, int i) {
        if (i < 0) {
            throw new RuntimeException("Bad length in parcel: " + i);
        }
        if (i == 0) {
            this.mParcelledData = NoImagePreloadHolder.EMPTY_PARCEL;
            this.mParcelledByNative = false;
            return;
        }
        int i2 = parcel.readInt();
        boolean z = i2 == BUNDLE_MAGIC;
        boolean z2 = i2 == BUNDLE_MAGIC_NATIVE;
        if (!z && !z2) {
            throw new IllegalStateException("Bad magic number for Bundle: 0x" + Integer.toHexString(i2));
        }
        if (parcel.hasReadWriteHelper()) {
            synchronized (this) {
                initializeFromParcelLocked(parcel, false, z2);
            }
            return;
        }
        int iDataPosition = parcel.dataPosition();
        parcel.setDataPosition(MathUtils.addOrThrow(iDataPosition, i));
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.setDataPosition(0);
        parcelObtain.appendFrom(parcel, iDataPosition, i);
        parcelObtain.adoptClassCookies(parcel);
        parcelObtain.setDataPosition(0);
        this.mParcelledData = parcelObtain;
        this.mParcelledByNative = z2;
    }

    public static void dumpStats(IndentingPrintWriter indentingPrintWriter, String str, Object obj) {
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.writeValue(obj);
        int iDataPosition = parcelObtain.dataPosition();
        parcelObtain.recycle();
        if (iDataPosition > 1024) {
            indentingPrintWriter.println(str + " [size=" + iDataPosition + "]");
            if (obj instanceof BaseBundle) {
                dumpStats(indentingPrintWriter, (BaseBundle) obj);
            } else if (obj instanceof SparseArray) {
                dumpStats(indentingPrintWriter, (SparseArray) obj);
            }
        }
    }

    public static void dumpStats(IndentingPrintWriter indentingPrintWriter, SparseArray sparseArray) {
        indentingPrintWriter.increaseIndent();
        if (sparseArray == null) {
            indentingPrintWriter.println("[null]");
            return;
        }
        for (int i = 0; i < sparseArray.size(); i++) {
            dumpStats(indentingPrintWriter, "0x" + Integer.toHexString(sparseArray.keyAt(i)), sparseArray.valueAt(i));
        }
        indentingPrintWriter.decreaseIndent();
    }

    public static void dumpStats(IndentingPrintWriter indentingPrintWriter, BaseBundle baseBundle) {
        indentingPrintWriter.increaseIndent();
        if (baseBundle == null) {
            indentingPrintWriter.println("[null]");
            return;
        }
        ArrayMap<String, Object> map = baseBundle.getMap();
        for (int i = 0; i < map.size(); i++) {
            dumpStats(indentingPrintWriter, map.keyAt(i), map.valueAt(i));
        }
        indentingPrintWriter.decreaseIndent();
    }
}
