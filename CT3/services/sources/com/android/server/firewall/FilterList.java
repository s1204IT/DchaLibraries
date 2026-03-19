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

    public FilterList readFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            readChild(parser);
        }
        return this;
    }

    protected void readChild(XmlPullParser parser) throws XmlPullParserException, IOException {
        Filter filter = IntentFirewall.parseFilter(parser);
        this.children.add(filter);
    }
}
