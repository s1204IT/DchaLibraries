package android.os;

import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Bundle extends BaseBundle implements Cloneable, Parcelable {
    public static final Parcelable.Creator<Bundle> CREATOR;
    public static final Bundle EMPTY = new Bundle();

    @VisibleForTesting
    static final int FLAG_ALLOW_FDS = 1024;

    @VisibleForTesting
    static final int FLAG_HAS_FDS = 256;

    @VisibleForTesting
    static final int FLAG_HAS_FDS_KNOWN = 512;
    public static final Bundle STRIPPED;

    static {
        EMPTY.mMap = ArrayMap.EMPTY;
        STRIPPED = new Bundle();
        STRIPPED.putInt("STRIPPED", 1);
        CREATOR = new Parcelable.Creator<Bundle>() {
            @Override
            public Bundle createFromParcel(Parcel parcel) {
                return parcel.readBundle();
            }

            @Override
            public Bundle[] newArray(int i) {
                return new Bundle[i];
            }
        };
    }

    public Bundle() {
        this.mFlags = 1536;
    }

    @VisibleForTesting
    public Bundle(Parcel parcel) {
        super(parcel);
        this.mFlags = 1024;
        maybePrefillHasFds();
    }

    @VisibleForTesting
    public Bundle(Parcel parcel, int i) {
        super(parcel, i);
        this.mFlags = 1024;
        maybePrefillHasFds();
    }

    private void maybePrefillHasFds() {
        if (this.mParcelledData != null) {
            if (this.mParcelledData.hasFileDescriptors()) {
                this.mFlags |= 768;
            } else {
                this.mFlags |= 512;
            }
        }
    }

    public Bundle(ClassLoader classLoader) {
        super(classLoader);
        this.mFlags = 1536;
    }

    public Bundle(int i) {
        super(i);
        this.mFlags = 1536;
    }

    public Bundle(Bundle bundle) {
        super(bundle);
        this.mFlags = bundle.mFlags;
    }

    public Bundle(PersistableBundle persistableBundle) {
        super(persistableBundle);
        this.mFlags = 1536;
    }

    Bundle(boolean z) {
        super(z);
    }

    public static Bundle forPair(String str, String str2) {
        Bundle bundle = new Bundle(1);
        bundle.putString(str, str2);
        return bundle;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        super.setClassLoader(classLoader);
    }

    @Override
    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }

    public boolean setAllowFds(boolean z) {
        boolean z2 = (this.mFlags & 1024) != 0;
        if (z) {
            this.mFlags |= 1024;
        } else {
            this.mFlags &= -1025;
        }
        return z2;
    }

    public void setDefusable(boolean z) {
        if (z) {
            this.mFlags |= 1;
        } else {
            this.mFlags &= -2;
        }
    }

    public static Bundle setDefusable(Bundle bundle, boolean z) {
        if (bundle != null) {
            bundle.setDefusable(z);
        }
        return bundle;
    }

    public Object clone() {
        return new Bundle(this);
    }

    public Bundle deepCopy() {
        Bundle bundle = new Bundle(false);
        bundle.copyInternal(this, true);
        return bundle;
    }

    @Override
    public void clear() {
        super.clear();
        this.mFlags = 1536;
    }

    @Override
    public void remove(String str) {
        super.remove(str);
        if ((this.mFlags & 256) != 0) {
            this.mFlags &= -513;
        }
    }

    public void putAll(Bundle bundle) {
        unparcel();
        bundle.unparcel();
        this.mMap.putAll((ArrayMap<? extends String, ? extends Object>) bundle.mMap);
        if ((bundle.mFlags & 256) != 0) {
            this.mFlags |= 256;
        }
        if ((bundle.mFlags & 512) == 0) {
            this.mFlags &= -513;
        }
    }

    public int getSize() {
        if (this.mParcelledData != null) {
            return this.mParcelledData.dataSize();
        }
        return 0;
    }

    public boolean hasFileDescriptors() {
        boolean z;
        if ((this.mFlags & 512) == 0) {
            if (this.mParcelledData != null) {
                if (this.mParcelledData.hasFileDescriptors()) {
                    z = true;
                    break;
                }
                z = false;
                if (z) {
                    this.mFlags &= -257;
                } else {
                    this.mFlags |= 256;
                }
                this.mFlags |= 512;
            } else {
                z = false;
                for (int size = this.mMap.size() - 1; size >= 0; size--) {
                    Object objValueAt = this.mMap.valueAt(size);
                    if (objValueAt instanceof Parcelable) {
                        if ((((Parcelable) objValueAt).describeContents() & 1) != 0) {
                            z = true;
                            break;
                        }
                    } else if (objValueAt instanceof Parcelable[]) {
                        Parcelable[] parcelableArr = (Parcelable[]) objValueAt;
                        int length = parcelableArr.length - 1;
                        while (true) {
                            if (length < 0) {
                                break;
                            }
                            Parcelable parcelable = parcelableArr[length];
                            if (parcelable != null && (parcelable.describeContents() & 1) != 0) {
                                z = true;
                                break;
                            }
                            length--;
                        }
                    } else if (objValueAt instanceof SparseArray) {
                        SparseArray sparseArray = (SparseArray) objValueAt;
                        int size2 = sparseArray.size() - 1;
                        while (true) {
                            if (size2 < 0) {
                                break;
                            }
                            Parcelable parcelable2 = (Parcelable) sparseArray.valueAt(size2);
                            if (parcelable2 != null && (parcelable2.describeContents() & 1) != 0) {
                                z = true;
                                break;
                            }
                            size2--;
                        }
                    } else if (objValueAt instanceof ArrayList) {
                        ArrayList arrayList = (ArrayList) objValueAt;
                        if (!arrayList.isEmpty() && (arrayList.get(0) instanceof Parcelable)) {
                            int size3 = arrayList.size() - 1;
                            while (true) {
                                if (size3 < 0) {
                                    break;
                                }
                                Parcelable parcelable3 = (Parcelable) arrayList.get(size3);
                                if (parcelable3 != null && (parcelable3.describeContents() & 1) != 0) {
                                    z = true;
                                    break;
                                }
                                size3--;
                            }
                        }
                    }
                }
                if (z) {
                }
                this.mFlags |= 512;
            }
        }
        return (this.mFlags & 256) != 0;
    }

    public Bundle filterValues() {
        Bundle bundle;
        unparcel();
        if (this.mMap != null) {
            ArrayMap<String, Object> arrayMap = this.mMap;
            ArrayMap<String, Object> arrayMap2 = arrayMap;
            bundle = this;
            for (int size = arrayMap.size() - 1; size >= 0; size--) {
                Object objValueAt = arrayMap2.valueAt(size);
                if (!PersistableBundle.isValidType(objValueAt)) {
                    if (objValueAt instanceof Bundle) {
                        Bundle bundleFilterValues = ((Bundle) objValueAt).filterValues();
                        if (bundleFilterValues != objValueAt) {
                            if (arrayMap2 == this.mMap) {
                                bundle = new Bundle(this);
                                arrayMap2 = bundle.mMap;
                            }
                            arrayMap2.setValueAt(size, bundleFilterValues);
                        }
                    } else if (!objValueAt.getClass().getName().startsWith("android.")) {
                        if (arrayMap2 == this.mMap) {
                            bundle = new Bundle(this);
                            arrayMap2 = bundle.mMap;
                        }
                        arrayMap2.removeAt(size);
                    }
                }
            }
        } else {
            bundle = this;
        }
        this.mFlags |= 512;
        this.mFlags &= -257;
        return bundle;
    }

    @Override
    public void putByte(String str, byte b) {
        super.putByte(str, b);
    }

    @Override
    public void putChar(String str, char c) {
        super.putChar(str, c);
    }

    @Override
    public void putShort(String str, short s) {
        super.putShort(str, s);
    }

    @Override
    public void putFloat(String str, float f) {
        super.putFloat(str, f);
    }

    @Override
    public void putCharSequence(String str, CharSequence charSequence) {
        super.putCharSequence(str, charSequence);
    }

    public void putParcelable(String str, Parcelable parcelable) {
        unparcel();
        this.mMap.put(str, parcelable);
        this.mFlags &= -513;
    }

    public void putSize(String str, Size size) {
        unparcel();
        this.mMap.put(str, size);
    }

    public void putSizeF(String str, SizeF sizeF) {
        unparcel();
        this.mMap.put(str, sizeF);
    }

    public void putParcelableArray(String str, Parcelable[] parcelableArr) {
        unparcel();
        this.mMap.put(str, parcelableArr);
        this.mFlags &= -513;
    }

    public void putParcelableArrayList(String str, ArrayList<? extends Parcelable> arrayList) {
        unparcel();
        this.mMap.put(str, arrayList);
        this.mFlags &= -513;
    }

    public void putParcelableList(String str, List<? extends Parcelable> list) {
        unparcel();
        this.mMap.put(str, list);
        this.mFlags &= -513;
    }

    public void putSparseParcelableArray(String str, SparseArray<? extends Parcelable> sparseArray) {
        unparcel();
        this.mMap.put(str, sparseArray);
        this.mFlags &= -513;
    }

    @Override
    public void putIntegerArrayList(String str, ArrayList<Integer> arrayList) {
        super.putIntegerArrayList(str, arrayList);
    }

    @Override
    public void putStringArrayList(String str, ArrayList<String> arrayList) {
        super.putStringArrayList(str, arrayList);
    }

    @Override
    public void putCharSequenceArrayList(String str, ArrayList<CharSequence> arrayList) {
        super.putCharSequenceArrayList(str, arrayList);
    }

    @Override
    public void putSerializable(String str, Serializable serializable) {
        super.putSerializable(str, serializable);
    }

    @Override
    public void putByteArray(String str, byte[] bArr) {
        super.putByteArray(str, bArr);
    }

    @Override
    public void putShortArray(String str, short[] sArr) {
        super.putShortArray(str, sArr);
    }

    @Override
    public void putCharArray(String str, char[] cArr) {
        super.putCharArray(str, cArr);
    }

    @Override
    public void putFloatArray(String str, float[] fArr) {
        super.putFloatArray(str, fArr);
    }

    @Override
    public void putCharSequenceArray(String str, CharSequence[] charSequenceArr) {
        super.putCharSequenceArray(str, charSequenceArr);
    }

    public void putBundle(String str, Bundle bundle) {
        unparcel();
        this.mMap.put(str, bundle);
    }

    public void putBinder(String str, IBinder iBinder) {
        unparcel();
        this.mMap.put(str, iBinder);
    }

    @Deprecated
    public void putIBinder(String str, IBinder iBinder) {
        unparcel();
        this.mMap.put(str, iBinder);
    }

    @Override
    public byte getByte(String str) {
        return super.getByte(str);
    }

    @Override
    public Byte getByte(String str, byte b) {
        return super.getByte(str, b);
    }

    @Override
    public char getChar(String str) {
        return super.getChar(str);
    }

    @Override
    public char getChar(String str, char c) {
        return super.getChar(str, c);
    }

    @Override
    public short getShort(String str) {
        return super.getShort(str);
    }

    @Override
    public short getShort(String str, short s) {
        return super.getShort(str, s);
    }

    @Override
    public float getFloat(String str) {
        return super.getFloat(str);
    }

    @Override
    public float getFloat(String str, float f) {
        return super.getFloat(str, f);
    }

    @Override
    public CharSequence getCharSequence(String str) {
        return super.getCharSequence(str);
    }

    @Override
    public CharSequence getCharSequence(String str, CharSequence charSequence) {
        return super.getCharSequence(str, charSequence);
    }

    public Size getSize(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        try {
            return (Size) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Size", e);
            return null;
        }
    }

    public SizeF getSizeF(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        try {
            return (SizeF) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "SizeF", e);
            return null;
        }
    }

    public Bundle getBundle(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (Bundle) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Bundle", e);
            return null;
        }
    }

    public <T extends Parcelable> T getParcelable(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (T) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Parcelable", e);
            return null;
        }
    }

    public Parcelable[] getParcelableArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (Parcelable[]) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Parcelable[]", e);
            return null;
        }
    }

    public <T extends Parcelable> ArrayList<T> getParcelableArrayList(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (ArrayList) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "ArrayList", e);
            return null;
        }
    }

    public <T extends Parcelable> SparseArray<T> getSparseParcelableArray(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (SparseArray) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "SparseArray", e);
            return null;
        }
    }

    @Override
    public Serializable getSerializable(String str) {
        return super.getSerializable(str);
    }

    @Override
    public ArrayList<Integer> getIntegerArrayList(String str) {
        return super.getIntegerArrayList(str);
    }

    @Override
    public ArrayList<String> getStringArrayList(String str) {
        return super.getStringArrayList(str);
    }

    @Override
    public ArrayList<CharSequence> getCharSequenceArrayList(String str) {
        return super.getCharSequenceArrayList(str);
    }

    @Override
    public byte[] getByteArray(String str) {
        return super.getByteArray(str);
    }

    @Override
    public short[] getShortArray(String str) {
        return super.getShortArray(str);
    }

    @Override
    public char[] getCharArray(String str) {
        return super.getCharArray(str);
    }

    @Override
    public float[] getFloatArray(String str) {
        return super.getFloatArray(str);
    }

    @Override
    public CharSequence[] getCharSequenceArray(String str) {
        return super.getCharSequenceArray(str);
    }

    public IBinder getBinder(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (IBinder) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "IBinder", e);
            return null;
        }
    }

    @Deprecated
    public IBinder getIBinder(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (IBinder) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "IBinder", e);
            return null;
        }
    }

    @Override
    public int describeContents() {
        if (hasFileDescriptors()) {
            return 1;
        }
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        boolean zPushAllowFds = parcel.pushAllowFds((this.mFlags & 1024) != 0);
        try {
            super.writeToParcelInner(parcel, i);
        } finally {
            parcel.restoreAllowFds(zPushAllowFds);
        }
    }

    public void readFromParcel(Parcel parcel) {
        super.readFromParcelInner(parcel);
        this.mFlags = 1024;
        maybePrefillHasFds();
    }

    public synchronized String toString() {
        if (this.mParcelledData != null) {
            if (isEmptyParcel()) {
                return "Bundle[EMPTY_PARCEL]";
            }
            return "Bundle[mParcelledData.dataSize=" + this.mParcelledData.dataSize() + "]";
        }
        return "Bundle[" + this.mMap.toString() + "]";
    }

    public synchronized String toShortString() {
        if (this.mParcelledData != null) {
            if (isEmptyParcel()) {
                return "EMPTY_PARCEL";
            }
            return "mParcelledData.dataSize=" + this.mParcelledData.dataSize();
        }
        return this.mMap.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        if (this.mParcelledData != null) {
            if (isEmptyParcel()) {
                protoOutputStream.write(1120986464257L, 0);
            } else {
                protoOutputStream.write(1120986464257L, this.mParcelledData.dataSize());
            }
        } else {
            protoOutputStream.write(1138166333442L, this.mMap.toString());
        }
        protoOutputStream.end(jStart);
    }
}
