package com.android.server.pm;

import android.content.ComponentName;
import android.content.IntentFilter;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.PreferredComponent;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class PreferredActivity extends IntentFilter implements PreferredComponent.Callbacks {
    private static final boolean DEBUG_FILTERS = false;
    private static final String TAG = "PreferredActivity";
    final PreferredComponent mPref;

    PreferredActivity(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName, boolean z) {
        super(intentFilter);
        this.mPref = new PreferredComponent(this, i, componentNameArr, componentName, z);
    }

    PreferredActivity(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        this.mPref = new PreferredComponent(this, xmlPullParser);
    }

    public void writeToXml(XmlSerializer xmlSerializer, boolean z) throws IOException {
        this.mPref.writeToXml(xmlSerializer, z);
        xmlSerializer.startTag(null, "filter");
        super.writeToXml(xmlSerializer);
        xmlSerializer.endTag(null, "filter");
    }

    @Override
    public boolean onReadTag(String str, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        if (str.equals("filter")) {
            readFromXml(xmlPullParser);
            return true;
        }
        PackageManagerService.reportSettingsProblem(5, "Unknown element under <preferred-activities>: " + xmlPullParser.getName());
        XmlUtils.skipCurrentTag(xmlPullParser);
        return true;
    }

    public String toString() {
        return "PreferredActivity{0x" + Integer.toHexString(System.identityHashCode(this)) + " " + this.mPref.mComponent.flattenToShortString() + "}";
    }
}
