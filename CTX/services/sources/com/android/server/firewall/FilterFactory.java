package com.android.server.firewall;

import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class FilterFactory {
    private final String mTag;

    public abstract Filter newFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException;

    protected FilterFactory(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.mTag = str;
    }

    public String getTagName() {
        return this.mTag;
    }
}
