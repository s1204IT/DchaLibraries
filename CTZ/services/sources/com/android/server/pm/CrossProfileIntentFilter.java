package com.android.server.pm;

import android.content.IntentFilter;
import com.android.internal.util.XmlUtils;
import com.android.server.backup.BackupManagerConstants;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class CrossProfileIntentFilter extends IntentFilter {
    private static final String ATTR_FILTER = "filter";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_OWNER_PACKAGE = "ownerPackage";
    private static final String ATTR_TARGET_USER_ID = "targetUserId";
    private static final String TAG = "CrossProfileIntentFilter";
    final int mFlags;
    final String mOwnerPackage;
    final int mTargetUserId;

    CrossProfileIntentFilter(IntentFilter intentFilter, String str, int i, int i2) {
        super(intentFilter);
        this.mTargetUserId = i;
        this.mOwnerPackage = str;
        this.mFlags = i2;
    }

    public int getTargetUserId() {
        return this.mTargetUserId;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public String getOwnerPackage() {
        return this.mOwnerPackage;
    }

    CrossProfileIntentFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        this.mTargetUserId = getIntFromXml(xmlPullParser, ATTR_TARGET_USER_ID, -10000);
        this.mOwnerPackage = getStringFromXml(xmlPullParser, ATTR_OWNER_PACKAGE, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mFlags = getIntFromXml(xmlPullParser, ATTR_FLAGS, 0);
        int depth = xmlPullParser.getDepth();
        String name = xmlPullParser.getName();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            name = xmlPullParser.getName();
            if (next != 3 && next != 4 && next == 2) {
                if (name.equals(ATTR_FILTER)) {
                    break;
                }
                PackageManagerService.reportSettingsProblem(5, "Unknown element under crossProfile-intent-filters: " + name + " at " + xmlPullParser.getPositionDescription());
                XmlUtils.skipCurrentTag(xmlPullParser);
            }
        }
        if (name.equals(ATTR_FILTER)) {
            readFromXml(xmlPullParser);
            return;
        }
        PackageManagerService.reportSettingsProblem(5, "Missing element under CrossProfileIntentFilter: filter at " + xmlPullParser.getPositionDescription());
        XmlUtils.skipCurrentTag(xmlPullParser);
    }

    String getStringFromXml(XmlPullParser xmlPullParser, String str, String str2) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            PackageManagerService.reportSettingsProblem(5, "Missing element under CrossProfileIntentFilter: " + str + " at " + xmlPullParser.getPositionDescription());
            return str2;
        }
        return attributeValue;
    }

    int getIntFromXml(XmlPullParser xmlPullParser, String str, int i) {
        String stringFromXml = getStringFromXml(xmlPullParser, str, null);
        if (stringFromXml != null) {
            return Integer.parseInt(stringFromXml);
        }
        return i;
    }

    @Override
    public void writeToXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.attribute(null, ATTR_TARGET_USER_ID, Integer.toString(this.mTargetUserId));
        xmlSerializer.attribute(null, ATTR_FLAGS, Integer.toString(this.mFlags));
        xmlSerializer.attribute(null, ATTR_OWNER_PACKAGE, this.mOwnerPackage);
        xmlSerializer.startTag(null, ATTR_FILTER);
        super.writeToXml(xmlSerializer);
        xmlSerializer.endTag(null, ATTR_FILTER);
    }

    public String toString() {
        return "CrossProfileIntentFilter{0x" + Integer.toHexString(System.identityHashCode(this)) + " " + Integer.toString(this.mTargetUserId) + "}";
    }

    boolean equalsIgnoreFilter(CrossProfileIntentFilter crossProfileIntentFilter) {
        return this.mTargetUserId == crossProfileIntentFilter.mTargetUserId && this.mOwnerPackage.equals(crossProfileIntentFilter.mOwnerPackage) && this.mFlags == crossProfileIntentFilter.mFlags;
    }
}
