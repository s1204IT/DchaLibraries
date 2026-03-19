package android.nfc.cardemulation;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class AidGroup implements Parcelable {
    public static final Parcelable.Creator<AidGroup> CREATOR = new Parcelable.Creator<AidGroup>() {
        @Override
        public AidGroup createFromParcel(Parcel parcel) {
            String string = parcel.readString();
            int i = parcel.readInt();
            ArrayList arrayList = new ArrayList();
            if (i > 0) {
                parcel.readStringList(arrayList);
            }
            return new AidGroup(arrayList, string);
        }

        @Override
        public AidGroup[] newArray(int i) {
            return new AidGroup[i];
        }
    };
    public static final int MAX_NUM_AIDS = 256;
    static final String TAG = "AidGroup";
    final List<String> aids;
    final String category;
    final String description;

    public AidGroup(List<String> list, String str) {
        if (list == null || list.size() == 0) {
            throw new IllegalArgumentException("No AIDS in AID group.");
        }
        if (list.size() > 256) {
            throw new IllegalArgumentException("Too many AIDs in AID group.");
        }
        for (String str2 : list) {
            if (!CardEmulation.isValidAid(str2)) {
                throw new IllegalArgumentException("AID " + str2 + " is not a valid AID.");
            }
        }
        if (isValidCategory(str)) {
            this.category = str;
        } else {
            this.category = "other";
        }
        this.aids = new ArrayList(list.size());
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            this.aids.add(it.next().toUpperCase());
        }
        this.description = null;
    }

    AidGroup(String str, String str2) {
        this.aids = new ArrayList();
        this.category = str;
        this.description = str2;
    }

    public String getCategory() {
        return this.category;
    }

    public List<String> getAids() {
        return this.aids;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Category: " + this.category + ", AIDs:");
        Iterator<String> it = this.aids.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.category);
        parcel.writeInt(this.aids.size());
        if (this.aids.size() > 0) {
            parcel.writeStringList(this.aids);
        }
    }

    public static AidGroup createFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        ArrayList arrayList = new ArrayList();
        int eventType = xmlPullParser.getEventType();
        int depth = xmlPullParser.getDepth();
        boolean z = false;
        String str = null;
        while (eventType != 1 && xmlPullParser.getDepth() >= depth) {
            String name = xmlPullParser.getName();
            if (eventType == 2) {
                if (name.equals("aid")) {
                    if (z) {
                        String attributeValue = xmlPullParser.getAttributeValue(null, "value");
                        if (attributeValue != null) {
                            arrayList.add(attributeValue.toUpperCase());
                        }
                    } else {
                        Log.d(TAG, "Ignoring <aid> tag while not in group");
                    }
                } else if (name.equals("aid-group")) {
                    String attributeValue2 = xmlPullParser.getAttributeValue(null, "category");
                    if (attributeValue2 == null) {
                        Log.e(TAG, "<aid-group> tag without valid category");
                        return null;
                    }
                    str = attributeValue2;
                    z = true;
                } else {
                    Log.d(TAG, "Ignoring unexpected tag: " + name);
                }
            } else if (eventType == 3 && name.equals("aid-group") && z && arrayList.size() > 0) {
                return new AidGroup(arrayList, str);
            }
            eventType = xmlPullParser.next();
        }
        return null;
    }

    public void writeAsXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, "aid-group");
        xmlSerializer.attribute(null, "category", this.category);
        for (String str : this.aids) {
            xmlSerializer.startTag(null, "aid");
            xmlSerializer.attribute(null, "value", str);
            xmlSerializer.endTag(null, "aid");
        }
        xmlSerializer.endTag(null, "aid-group");
    }

    static boolean isValidCategory(String str) {
        return CardEmulation.CATEGORY_PAYMENT.equals(str) || "other".equals(str);
    }
}
