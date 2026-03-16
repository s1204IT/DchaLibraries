package android.os;

import android.os.Parcelable;
import android.util.ArrayMap;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class PersistableBundle extends BaseBundle implements Cloneable, Parcelable, XmlUtils.WriteMapCallback {
    public static final Parcelable.Creator<PersistableBundle> CREATOR;
    public static final PersistableBundle EMPTY = new PersistableBundle();
    static final Parcel EMPTY_PARCEL;
    private static final String TAG_PERSISTABLEMAP = "pbundle_as_map";

    static {
        EMPTY.mMap = ArrayMap.EMPTY;
        EMPTY_PARCEL = BaseBundle.EMPTY_PARCEL;
        CREATOR = new Parcelable.Creator<PersistableBundle>() {
            @Override
            public PersistableBundle createFromParcel(Parcel in) {
                return in.readPersistableBundle();
            }

            @Override
            public PersistableBundle[] newArray(int size) {
                return new PersistableBundle[size];
            }
        };
    }

    public PersistableBundle() {
    }

    public PersistableBundle(int capacity) {
        super(capacity);
    }

    public PersistableBundle(PersistableBundle b) {
        super(b);
    }

    private PersistableBundle(Map<String, Object> map) {
        putAll(map);
        Set<String> keys = map.keySet();
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Map) {
                putPersistableBundle(key, new PersistableBundle((Map<String, Object>) value));
            } else if (!(value instanceof Integer) && !(value instanceof Long) && !(value instanceof Double) && !(value instanceof String) && !(value instanceof int[]) && !(value instanceof long[]) && !(value instanceof double[]) && !(value instanceof String[]) && !(value instanceof PersistableBundle) && value != null && !(value instanceof Boolean) && !(value instanceof boolean[])) {
                throw new IllegalArgumentException("Bad value in PersistableBundle key=" + key + " value=" + value);
            }
        }
    }

    PersistableBundle(Parcel parcelledData, int length) {
        super(parcelledData, length);
    }

    public static PersistableBundle forPair(String key, String value) {
        PersistableBundle b = new PersistableBundle(1);
        b.putString(key, value);
        return b;
    }

    public Object clone() {
        return new PersistableBundle(this);
    }

    public void putPersistableBundle(String key, PersistableBundle value) {
        unparcel();
        this.mMap.put(key, value);
    }

    public PersistableBundle getPersistableBundle(String key) {
        unparcel();
        Object o = this.mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (PersistableBundle) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, "Bundle", e);
            return null;
        }
    }

    @Override
    public void writeUnknownObject(Object v, String name, XmlSerializer out) throws XmlPullParserException, IOException {
        if (v instanceof PersistableBundle) {
            out.startTag(null, TAG_PERSISTABLEMAP);
            out.attribute(null, "name", name);
            ((PersistableBundle) v).saveToXml(out);
            out.endTag(null, TAG_PERSISTABLEMAP);
            return;
        }
        throw new XmlPullParserException("Unknown Object o=" + v);
    }

    public void saveToXml(XmlSerializer out) throws XmlPullParserException, IOException {
        unparcel();
        XmlUtils.writeMapXml(this.mMap, out, this);
    }

    static class MyReadMapCallback implements XmlUtils.ReadMapCallback {
        MyReadMapCallback() {
        }

        @Override
        public Object readThisUnknownObjectXml(XmlPullParser in, String tag) throws XmlPullParserException, IOException {
            if (PersistableBundle.TAG_PERSISTABLEMAP.equals(tag)) {
                return PersistableBundle.restoreFromXml(in);
            }
            throw new XmlPullParserException("Unknown tag=" + tag);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        boolean oldAllowFds = parcel.pushAllowFds(false);
        try {
            writeToParcelInner(parcel, flags);
        } finally {
            parcel.restoreAllowFds(oldAllowFds);
        }
    }

    public static PersistableBundle restoreFromXml(XmlPullParser in) throws XmlPullParserException, IOException {
        int event;
        int outerDepth = in.getDepth();
        String startTag = in.getName();
        String[] tagName = new String[1];
        do {
            event = in.next();
            if (event == 1 || (event == 3 && in.getDepth() >= outerDepth)) {
                return EMPTY;
            }
        } while (event != 2);
        return new PersistableBundle(XmlUtils.readThisMapXml(in, startTag, tagName, new MyReadMapCallback()));
    }

    public synchronized String toString() {
        String str;
        if (this.mParcelledData != null) {
            if (this.mParcelledData == EMPTY_PARCEL) {
                str = "PersistableBundle[EMPTY_PARCEL]";
            } else {
                str = "PersistableBundle[mParcelledData.dataSize=" + this.mParcelledData.dataSize() + "]";
            }
        } else {
            str = "PersistableBundle[" + this.mMap.toString() + "]";
        }
        return str;
    }
}
