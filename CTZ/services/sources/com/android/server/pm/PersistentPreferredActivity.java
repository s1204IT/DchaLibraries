package com.android.server.pm;

import android.content.ComponentName;
import android.content.IntentFilter;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class PersistentPreferredActivity extends IntentFilter {
    private static final String ATTR_FILTER = "filter";
    private static final String ATTR_NAME = "name";
    private static final boolean DEBUG_FILTERS = false;
    private static final String TAG = "PersistentPreferredActivity";
    final ComponentName mComponent;

    PersistentPreferredActivity(IntentFilter intentFilter, ComponentName componentName) {
        super(intentFilter);
        this.mComponent = componentName;
    }

    PersistentPreferredActivity(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, "name");
        this.mComponent = ComponentName.unflattenFromString(attributeValue);
        if (this.mComponent == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: Bad activity name " + attributeValue + " at " + xmlPullParser.getPositionDescription());
        }
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
                PackageManagerService.reportSettingsProblem(5, "Unknown element: " + name + " at " + xmlPullParser.getPositionDescription());
                XmlUtils.skipCurrentTag(xmlPullParser);
            }
        }
        if (name.equals(ATTR_FILTER)) {
            readFromXml(xmlPullParser);
            return;
        }
        PackageManagerService.reportSettingsProblem(5, "Missing element filter at " + xmlPullParser.getPositionDescription());
        XmlUtils.skipCurrentTag(xmlPullParser);
    }

    @Override
    public void writeToXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.attribute(null, "name", this.mComponent.flattenToShortString());
        xmlSerializer.startTag(null, ATTR_FILTER);
        super.writeToXml(xmlSerializer);
        xmlSerializer.endTag(null, ATTR_FILTER);
    }

    public String toString() {
        return "PersistentPreferredActivity{0x" + Integer.toHexString(System.identityHashCode(this)) + " " + this.mComponent.flattenToShortString() + "}";
    }
}
