package android.content.pm;

import android.annotation.SystemApi;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

@SystemApi
public final class IntentFilterVerificationInfo implements Parcelable {
    private static final String ATTR_DOMAIN_NAME = "name";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_STATUS = "status";
    private static final String TAG_DOMAIN = "domain";
    private ArraySet<String> mDomains;
    private int mMainStatus;
    private String mPackageName;
    private static final String TAG = IntentFilterVerificationInfo.class.getName();
    public static final Parcelable.Creator<IntentFilterVerificationInfo> CREATOR = new Parcelable.Creator<IntentFilterVerificationInfo>() {
        @Override
        public IntentFilterVerificationInfo createFromParcel(Parcel parcel) {
            return new IntentFilterVerificationInfo(parcel);
        }

        @Override
        public IntentFilterVerificationInfo[] newArray(int i) {
            return new IntentFilterVerificationInfo[i];
        }
    };

    public IntentFilterVerificationInfo() {
        this.mDomains = new ArraySet<>();
        this.mPackageName = null;
        this.mMainStatus = 0;
    }

    public IntentFilterVerificationInfo(String str, ArraySet<String> arraySet) {
        this.mDomains = new ArraySet<>();
        this.mPackageName = str;
        this.mDomains = arraySet;
        this.mMainStatus = 0;
    }

    public IntentFilterVerificationInfo(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        this.mDomains = new ArraySet<>();
        readFromXml(xmlPullParser);
    }

    public IntentFilterVerificationInfo(Parcel parcel) {
        this.mDomains = new ArraySet<>();
        readFromParcel(parcel);
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getStatus() {
        return this.mMainStatus;
    }

    public void setStatus(int i) {
        if (i >= 0 && i <= 3) {
            this.mMainStatus = i;
            return;
        }
        Log.w(TAG, "Trying to set a non supported status: " + i);
    }

    public Set<String> getDomains() {
        return this.mDomains;
    }

    public void setDomains(ArraySet<String> arraySet) {
        this.mDomains = arraySet;
    }

    public String getDomainsString() {
        StringBuilder sb = new StringBuilder();
        for (String str : this.mDomains) {
            if (sb.length() > 0) {
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            }
            sb.append(str);
        }
        return sb.toString();
    }

    String getStringFromXml(XmlPullParser xmlPullParser, String str, String str2) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            Log.w(TAG, "Missing element under " + TAG + ": " + str + " at " + xmlPullParser.getPositionDescription());
            return str2;
        }
        return attributeValue;
    }

    int getIntFromXml(XmlPullParser xmlPullParser, String str, int i) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (TextUtils.isEmpty(attributeValue)) {
            Log.w(TAG, "Missing element under " + TAG + ": " + str + " at " + xmlPullParser.getPositionDescription());
            return i;
        }
        return Integer.parseInt(attributeValue);
    }

    public void readFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        this.mPackageName = getStringFromXml(xmlPullParser, "packageName", null);
        if (this.mPackageName == null) {
            Log.e(TAG, "Package name cannot be null!");
        }
        int intFromXml = getIntFromXml(xmlPullParser, "status", -1);
        if (intFromXml == -1) {
            Log.e(TAG, "Unknown status value: " + intFromXml);
        }
        this.mMainStatus = intFromXml;
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        String name = xmlPullParser.getName();
                        if (name.equals("domain")) {
                            String stringFromXml = getStringFromXml(xmlPullParser, "name", null);
                            if (!TextUtils.isEmpty(stringFromXml)) {
                                this.mDomains.add(stringFromXml);
                            }
                        } else {
                            Log.w(TAG, "Unknown tag parsing IntentFilter: " + name);
                        }
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    public void writeToXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.attribute(null, "packageName", this.mPackageName);
        xmlSerializer.attribute(null, "status", String.valueOf(this.mMainStatus));
        for (String str : this.mDomains) {
            xmlSerializer.startTag(null, "domain");
            xmlSerializer.attribute(null, "name", str);
            xmlSerializer.endTag(null, "domain");
        }
    }

    public String getStatusString() {
        return getStatusStringFromValue(((long) this.mMainStatus) << 32);
    }

    public static String getStatusStringFromValue(long j) {
        StringBuilder sb = new StringBuilder();
        switch ((int) (j >> 32)) {
            case 1:
                sb.append("ask");
                break;
            case 2:
                sb.append("always : ");
                sb.append(Long.toHexString(j & (-1)));
                break;
            case 3:
                sb.append("never");
                break;
            case 4:
                sb.append("always-ask");
                break;
            default:
                sb.append("undefined");
                break;
        }
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void readFromParcel(Parcel parcel) {
        this.mPackageName = parcel.readString();
        this.mMainStatus = parcel.readInt();
        ArrayList arrayList = new ArrayList();
        parcel.readStringList(arrayList);
        this.mDomains.addAll(arrayList);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPackageName);
        parcel.writeInt(this.mMainStatus);
        parcel.writeStringList(new ArrayList(this.mDomains));
    }
}
