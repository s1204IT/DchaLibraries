package com.android.server.firewall;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class PortFilter implements Filter {
    private static final String ATTR_EQUALS = "equals";
    private static final String ATTR_MAX = "max";
    private static final String ATTR_MIN = "min";
    public static final FilterFactory FACTORY = new FilterFactory("port") {
        @Override
        public Filter newFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            String attributeValue = xmlPullParser.getAttributeValue(null, PortFilter.ATTR_EQUALS);
            int i = -1;
            if (attributeValue != null) {
                try {
                    i = Integer.parseInt(attributeValue);
                } catch (NumberFormatException e) {
                    throw new XmlPullParserException("Invalid port value: " + attributeValue, xmlPullParser, null);
                }
            }
            int i2 = i;
            String attributeValue2 = xmlPullParser.getAttributeValue(null, PortFilter.ATTR_MIN);
            String attributeValue3 = xmlPullParser.getAttributeValue(null, PortFilter.ATTR_MAX);
            if (attributeValue2 != null || attributeValue3 != null) {
                if (attributeValue != null) {
                    throw new XmlPullParserException("Port filter cannot use both equals and range filtering", xmlPullParser, null);
                }
                if (attributeValue2 != null) {
                    try {
                        i = Integer.parseInt(attributeValue2);
                    } catch (NumberFormatException e2) {
                        throw new XmlPullParserException("Invalid minimum port value: " + attributeValue2, xmlPullParser, null);
                    }
                }
                if (attributeValue3 != null) {
                    try {
                        i2 = Integer.parseInt(attributeValue3);
                    } catch (NumberFormatException e3) {
                        throw new XmlPullParserException("Invalid maximum port value: " + attributeValue3, xmlPullParser, null);
                    }
                }
            }
            return new PortFilter(i, i2);
        }
    };
    private static final int NO_BOUND = -1;
    private final int mLowerBound;
    private final int mUpperBound;

    private PortFilter(int i, int i2) {
        this.mLowerBound = i;
        this.mUpperBound = i2;
    }

    @Override
    public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
        int port;
        Uri data = intent.getData();
        if (data != null) {
            port = data.getPort();
        } else {
            port = -1;
        }
        return port != -1 && (this.mLowerBound == -1 || this.mLowerBound <= port) && (this.mUpperBound == -1 || this.mUpperBound >= port);
    }
}
