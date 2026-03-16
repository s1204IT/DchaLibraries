package android.os;

import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Bundle extends BaseBundle implements Cloneable, Parcelable {
    public static final Parcelable.Creator<Bundle> CREATOR;
    public static final Bundle EMPTY = new Bundle();
    static final Parcel EMPTY_PARCEL;
    private boolean mAllowFds;
    private boolean mFdsKnown;
    private boolean mHasFds;

    static {
        EMPTY.mMap = ArrayMap.EMPTY;
        EMPTY_PARCEL = BaseBundle.EMPTY_PARCEL;
        CREATOR = new Parcelable.Creator<Bundle>() {
            @Override
            public Bundle createFromParcel(Parcel in) {
                return in.readBundle();
            }

            @Override
            public Bundle[] newArray(int size) {
                return new Bundle[size];
            }
        };
    }

    public Bundle() {
        this.mHasFds = false;
        this.mFdsKnown = true;
        this.mAllowFds = true;
    }

    Bundle(Parcel parcelledData) {
        super(parcelledData);
        this.mHasFds = false;
        this.mFdsKnown = true;
        this.mAllowFds = true;
        this.mHasFds = this.mParcelledData.hasFileDescriptors();
        this.mFdsKnown = true;
    }

    Bundle(Parcel parcelledData, int length) {
        super(parcelledData, length);
        this.mHasFds = false;
        this.mFdsKnown = true;
        this.mAllowFds = true;
        this.mHasFds = this.mParcelledData.hasFileDescriptors();
        this.mFdsKnown = true;
    }

    public Bundle(ClassLoader loader) {
        super(loader);
        this.mHasFds = false;
        this.mFdsKnown = true;
        this.mAllowFds = true;
    }

    public Bundle(int capacity) {
        super(capacity);
        this.mHasFds = false;
        this.mFdsKnown = true;
        this.mAllowFds = true;
    }

    public Bundle(Bundle b) {
        super(b);
        this.mHasFds = false;
        this.mFdsKnown = true;
        this.mAllowFds = true;
        this.mHasFds = b.mHasFds;
        this.mFdsKnown = b.mFdsKnown;
    }

    public Bundle(PersistableBundle b) {
        super(b);
        this.mHasFds = false;
        this.mFdsKnown = true;
        this.mAllowFds = true;
    }

    public static Bundle forPair(String key, String value) {
        Bundle b = new Bundle(1);
        b.putString(key, value);
        return b;
    }

    @Override
    public void setClassLoader(ClassLoader loader) {
        super.setClassLoader(loader);
    }

    @Override
    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }

    public boolean setAllowFds(boolean allowFds) {
        boolean orig = this.mAllowFds;
        this.mAllowFds = allowFds;
        return orig;
    }

    public Object clone() {
        return new Bundle(this);
    }

    @Override
    public void clear() {
        super.clear();
        this.mHasFds = false;
        this.mFdsKnown = true;
    }

    public void putAll(Bundle bundle) {
        unparcel();
        bundle.unparcel();
        this.mMap.putAll((ArrayMap<? extends String, ? extends Object>) bundle.mMap);
        this.mHasFds |= bundle.mHasFds;
        this.mFdsKnown = this.mFdsKnown && bundle.mFdsKnown;
    }

    public boolean hasFileDescriptors() {
        if (!this.mFdsKnown) {
            boolean fdFound = false;
            if (this.mParcelledData != null) {
                if (this.mParcelledData.hasFileDescriptors()) {
                    fdFound = true;
                }
            } else {
                int i = this.mMap.size() - 1;
                while (true) {
                    if (i < 0) {
                        break;
                    }
                    Object obj = this.mMap.valueAt(i);
                    if (obj instanceof Parcelable) {
                        if ((((Parcelable) obj).describeContents() & 1) != 0) {
                            fdFound = true;
                            break;
                        }
                    } else if (obj instanceof Parcelable[]) {
                        Parcelable[] array = (Parcelable[]) obj;
                        int n = array.length - 1;
                        while (true) {
                            if (n < 0) {
                                break;
                            }
                            if ((array[n].describeContents() & 1) == 0) {
                                n--;
                            } else {
                                fdFound = true;
                                break;
                            }
                        }
                    } else if (obj instanceof SparseArray) {
                        SparseArray<? extends Parcelable> array2 = (SparseArray) obj;
                        int n2 = array2.size() - 1;
                        while (true) {
                            if (n2 < 0) {
                                break;
                            }
                            if ((((Parcelable) array2.valueAt(n2)).describeContents() & 1) == 0) {
                                n2--;
                            } else {
                                fdFound = true;
                                break;
                            }
                        }
                    } else if (obj instanceof ArrayList) {
                        ArrayList array3 = (ArrayList) obj;
                        if (!array3.isEmpty() && (array3.get(0) instanceof Parcelable)) {
                            int n3 = array3.size() - 1;
                            while (true) {
                                if (n3 >= 0) {
                                    Parcelable p = (Parcelable) array3.get(n3);
                                    if (p == null || (p.describeContents() & 1) == 0) {
                                        n3--;
                                    } else {
                                        fdFound = true;
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    i--;
                }
            }
            this.mHasFds = fdFound;
            this.mFdsKnown = true;
        }
        return this.mHasFds;
    }

    @Override
    public void putByte(String key, byte value) {
        super.putByte(key, value);
    }

    @Override
    public void putChar(String key, char value) {
        super.putChar(key, value);
    }

    @Override
    public void putShort(String key, short value) {
        super.putShort(key, value);
    }

    @Override
    public void putFloat(String key, float value) {
        super.putFloat(key, value);
    }

    @Override
    public void putCharSequence(String key, CharSequence value) {
        super.putCharSequence(key, value);
    }

    public void putParcelable(String key, Parcelable value) {
        unparcel();
        this.mMap.put(key, value);
        this.mFdsKnown = false;
    }

    public void putSize(String key, Size value) {
        unparcel();
        this.mMap.put(key, value);
    }

    public void putSizeF(String key, SizeF value) {
        unparcel();
        this.mMap.put(key, value);
    }

    public void putParcelableArray(String key, Parcelable[] value) {
        unparcel();
        this.mMap.put(key, value);
        this.mFdsKnown = false;
    }

    public void putParcelableArrayList(String key, ArrayList<? extends Parcelable> value) {
        unparcel();
        this.mMap.put(key, value);
        this.mFdsKnown = false;
    }

    public void putParcelableList(String key, List<? extends Parcelable> value) {
        unparcel();
        this.mMap.put(key, value);
        this.mFdsKnown = false;
    }

    public void putSparseParcelableArray(String key, SparseArray<? extends Parcelable> value) {
        unparcel();
        this.mMap.put(key, value);
        this.mFdsKnown = false;
    }

    @Override
    public void putIntegerArrayList(String key, ArrayList<Integer> value) {
        super.putIntegerArrayList(key, value);
    }

    @Override
    public void putStringArrayList(String key, ArrayList<String> value) {
        super.putStringArrayList(key, value);
    }

    @Override
    public void putCharSequenceArrayList(String key, ArrayList<CharSequence> value) {
        super.putCharSequenceArrayList(key, value);
    }

    @Override
    public void putSerializable(String key, Serializable value) {
        super.putSerializable(key, value);
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        super.putByteArray(key, value);
    }

    @Override
    public void putShortArray(String key, short[] value) {
        super.putShortArray(key, value);
    }

    @Override
    public void putCharArray(String key, char[] value) {
        super.putCharArray(key, value);
    }

    @Override
    public void putFloatArray(String key, float[] value) {
        super.putFloatArray(key, value);
    }

    @Override
    public void putCharSequenceArray(String key, CharSequence[] value) {
        super.putCharSequenceArray(key, value);
    }

    public void putBundle(String key, Bundle value) {
        unparcel();
        this.mMap.put(key, value);
    }

    public void putBinder(String key, IBinder value) {
        unparcel();
        this.mMap.put(key, value);
    }

    @Deprecated
    public void putIBinder(String key, IBinder value) {
        unparcel();
        this.mMap.put(key, value);
    }

    @Override
    public byte getByte(String key) {
        return super.getByte(key);
    }

    @Override
    public Byte getByte(String key, byte defaultValue) {
        return super.getByte(key, defaultValue);
    }

    @Override
    public char getChar(String key) {
        return super.getChar(key);
    }

    @Override
    public char getChar(String key, char defaultValue) {
        return super.getChar(key, defaultValue);
    }

    @Override
    public short getShort(String key) {
        return super.getShort(key);
    }

    @Override
    public short getShort(String key, short defaultValue) {
        return super.getShort(key, defaultValue);
    }

    @Override
    public float getFloat(String key) {
        return super.getFloat(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return super.getFloat(key, defaultValue);
    }

    @Override
    public CharSequence getCharSequence(String key) {
        return super.getCharSequence(key);
    }

    @Override
    public CharSequence getCharSequence(String key, CharSequence defaultValue) {
        return super.getCharSequence(key, defaultValue);
    }

    public Size getSize(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        try {
            return (Size) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Size", e);
            return null;
        }
    }

    public SizeF getSizeF(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        try {
            return (SizeF) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "SizeF", e);
            return null;
        }
    }

    public Bundle getBundle(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (Bundle) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Bundle", e);
            return null;
        }
    }

    public <T extends Parcelable> T getParcelable(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (T) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Parcelable", e);
            return null;
        }
    }

    public Parcelable[] getParcelableArray(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (Parcelable[]) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Parcelable[]", e);
            return null;
        }
    }

    public <T extends Parcelable> ArrayList<T> getParcelableArrayList(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ArrayList) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "ArrayList", e);
            return null;
        }
    }

    public <T extends Parcelable> SparseArray<T> getSparseParcelableArray(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (SparseArray) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "SparseArray", e);
            return null;
        }
    }

    @Override
    public Serializable getSerializable(String key) {
        return super.getSerializable(key);
    }

    @Override
    public ArrayList<Integer> getIntegerArrayList(String key) {
        return super.getIntegerArrayList(key);
    }

    @Override
    public ArrayList<String> getStringArrayList(String key) {
        return super.getStringArrayList(key);
    }

    @Override
    public ArrayList<CharSequence> getCharSequenceArrayList(String key) {
        return super.getCharSequenceArrayList(key);
    }

    @Override
    public byte[] getByteArray(String key) {
        return super.getByteArray(key);
    }

    @Override
    public short[] getShortArray(String key) {
        return super.getShortArray(key);
    }

    @Override
    public char[] getCharArray(String key) {
        return super.getCharArray(key);
    }

    @Override
    public float[] getFloatArray(String key) {
        return super.getFloatArray(key);
    }

    @Override
    public CharSequence[] getCharSequenceArray(String key) {
        return super.getCharSequenceArray(key);
    }

    public IBinder getBinder(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (IBinder) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "IBinder", e);
            return null;
        }
    }

    @Deprecated
    public IBinder getIBinder(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (IBinder) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "IBinder", e);
            return null;
        }
    }

    @Override
    public int describeContents() {
        if (!hasFileDescriptors()) {
            return 0;
        }
        int mask = 0 | 1;
        return mask;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        boolean oldAllowFds = parcel.pushAllowFds(this.mAllowFds);
        try {
            super.writeToParcelInner(parcel, flags);
        } finally {
            parcel.restoreAllowFds(oldAllowFds);
        }
    }

    public void readFromParcel(Parcel parcel) {
        super.readFromParcelInner(parcel);
        this.mHasFds = this.mParcelledData.hasFileDescriptors();
        this.mFdsKnown = true;
    }

    public synchronized String toString() {
        String str;
        if (this.mParcelledData != null) {
            if (this.mParcelledData == EMPTY_PARCEL) {
                str = "Bundle[EMPTY_PARCEL]";
            } else {
                str = "Bundle[mParcelledData.dataSize=" + this.mParcelledData.dataSize() + "]";
            }
        } else {
            str = "Bundle[" + this.mMap.toString() + "]";
        }
        return str;
    }
}
