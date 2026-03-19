package com.android.server.firewall;

import android.content.ComponentName;
import android.content.Intent;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class SenderPermissionFilter implements Filter {
    private static final String ATTR_NAME = "name";
    public static final FilterFactory FACTORY = new FilterFactory("sender-permission") {
        @Override
        public Filter newFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            String attributeValue = xmlPullParser.getAttributeValue(null, "name");
            if (attributeValue == null) {
                throw new XmlPullParserException("Permission name must be specified.", xmlPullParser, null);
            }
            return new SenderPermissionFilter(attributeValue);
        }
    };
    private final String mPermission;

    private SenderPermissionFilter(String str) {
        this.mPermission = str;
    }

    @Override
    public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
        return intentFirewall.checkComponentPermission(this.mPermission, i2, i, i3, true);
    }
}
