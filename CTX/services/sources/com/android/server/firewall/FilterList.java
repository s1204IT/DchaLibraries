package com.android.server.firewall;

import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

abstract class FilterList implements Filter {
    protected final ArrayList<Filter> children = new ArrayList<>();

    FilterList() {
    }

    public FilterList readFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            readChild(xmlPullParser);
        }
        return this;
    }

    protected void readChild(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        this.children.add(IntentFirewall.parseFilter(xmlPullParser));
    }
}
