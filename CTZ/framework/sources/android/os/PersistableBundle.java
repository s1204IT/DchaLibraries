package android.os;

import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class PersistableBundle extends BaseBundle implements Cloneable, Parcelable, XmlUtils.WriteMapCallback {
    public static final Parcelable.Creator<PersistableBundle> CREATOR;
    public static final PersistableBundle EMPTY = new PersistableBundle();
    private static final String TAG_PERSISTABLEMAP = "pbundle_as_map";

    static {
        EMPTY.mMap = ArrayMap.EMPTY;
        CREATOR = new Parcelable.Creator<PersistableBundle>() {
            @Override
            public PersistableBundle createFromParcel(Parcel parcel) {
                return parcel.readPersistableBundle();
            }

            @Override
            public PersistableBundle[] newArray(int i) {
                return new PersistableBundle[i];
            }
        };
    }

    public static boolean isValidType(Object obj) {
        return (obj instanceof Integer) || (obj instanceof Long) || (obj instanceof Double) || (obj instanceof String) || (obj instanceof int[]) || (obj instanceof long[]) || (obj instanceof double[]) || (obj instanceof String[]) || (obj instanceof PersistableBundle) || obj == null || (obj instanceof Boolean) || (obj instanceof boolean[]);
    }

    public PersistableBundle() {
        this.mFlags = 1;
    }

    public PersistableBundle(int i) {
        super(i);
        this.mFlags = 1;
    }

    public PersistableBundle(PersistableBundle persistableBundle) {
        super(persistableBundle);
        this.mFlags = persistableBundle.mFlags;
    }

    public PersistableBundle(Bundle bundle) {
        this(bundle.getMap());
    }

    private PersistableBundle(ArrayMap<String, Object> arrayMap) {
        this.mFlags = 1;
        putAll(arrayMap);
        int size = this.mMap.size();
        for (int i = 0; i < size; i++) {
            Object objValueAt = this.mMap.valueAt(i);
            if (objValueAt instanceof ArrayMap) {
                this.mMap.setValueAt(i, new PersistableBundle((ArrayMap<String, Object>) objValueAt));
            } else if (objValueAt instanceof Bundle) {
                this.mMap.setValueAt(i, new PersistableBundle((Bundle) objValueAt));
            } else if (!isValidType(objValueAt)) {
                throw new IllegalArgumentException("Bad value in PersistableBundle key=" + this.mMap.keyAt(i) + " value=" + objValueAt);
            }
        }
    }

    PersistableBundle(Parcel parcel, int i) {
        super(parcel, i);
        this.mFlags = 1;
    }

    PersistableBundle(boolean z) {
        super(z);
    }

    public static PersistableBundle forPair(String str, String str2) {
        PersistableBundle persistableBundle = new PersistableBundle(1);
        persistableBundle.putString(str, str2);
        return persistableBundle;
    }

    public Object clone() {
        return new PersistableBundle(this);
    }

    public PersistableBundle deepCopy() {
        PersistableBundle persistableBundle = new PersistableBundle(false);
        persistableBundle.copyInternal(this, true);
        return persistableBundle;
    }

    public void putPersistableBundle(String str, PersistableBundle persistableBundle) {
        unparcel();
        this.mMap.put(str, persistableBundle);
    }

    public PersistableBundle getPersistableBundle(String str) {
        unparcel();
        Object obj = this.mMap.get(str);
        if (obj == null) {
            return null;
        }
        try {
            return (PersistableBundle) obj;
        } catch (ClassCastException e) {
            typeWarning(str, obj, "Bundle", e);
            return null;
        }
    }

    @Override
    public void writeUnknownObject(Object obj, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (obj instanceof PersistableBundle) {
            xmlSerializer.startTag(null, TAG_PERSISTABLEMAP);
            xmlSerializer.attribute(null, "name", str);
            ((PersistableBundle) obj).saveToXml(xmlSerializer);
            xmlSerializer.endTag(null, TAG_PERSISTABLEMAP);
            return;
        }
        throw new XmlPullParserException("Unknown Object o=" + obj);
    }

    public void saveToXml(XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        unparcel();
        XmlUtils.writeMapXml(this.mMap, xmlSerializer, this);
    }

    static class MyReadMapCallback implements XmlUtils.ReadMapCallback {
        MyReadMapCallback() {
        }

        @Override
        public Object readThisUnknownObjectXml(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
            if (PersistableBundle.TAG_PERSISTABLEMAP.equals(str)) {
                return PersistableBundle.restoreFromXml(xmlPullParser);
            }
            throw new XmlPullParserException("Unknown tag=" + str);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        boolean zPushAllowFds = parcel.pushAllowFds(false);
        try {
            writeToParcelInner(parcel, i);
        } finally {
            parcel.restoreAllowFds(zPushAllowFds);
        }
    }

    public static PersistableBundle restoreFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int next;
        int depth = xmlPullParser.getDepth();
        String name = xmlPullParser.getName();
        String[] strArr = new String[1];
        do {
            next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() >= depth)) {
                return EMPTY;
            }
        } while (next != 2);
        return new PersistableBundle(XmlUtils.readThisArrayMapXml(xmlPullParser, name, strArr, new MyReadMapCallback()));
    }

    public synchronized String toString() {
        if (this.mParcelledData != null) {
            if (isEmptyParcel()) {
                return "PersistableBundle[EMPTY_PARCEL]";
            }
            return "PersistableBundle[mParcelledData.dataSize=" + this.mParcelledData.dataSize() + "]";
        }
        return "PersistableBundle[" + this.mMap.toString() + "]";
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
