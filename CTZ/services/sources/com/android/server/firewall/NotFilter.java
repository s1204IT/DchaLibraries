package com.android.server.firewall;

import android.content.ComponentName;
import android.content.Intent;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class NotFilter implements Filter {
    public static final FilterFactory FACTORY = new FilterFactory("not") {
        @Override
        public Filter newFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            Filter filter = null;
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                Filter filter2 = IntentFirewall.parseFilter(xmlPullParser);
                if (filter == null) {
                    filter = filter2;
                } else {
                    throw new XmlPullParserException("<not> tag can only contain a single child filter.", xmlPullParser, null);
                }
            }
            return new NotFilter(filter);
        }
    };
    private final Filter mChild;

    private NotFilter(Filter filter) {
        this.mChild = filter;
    }

    @Override
    public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
        return !this.mChild.matches(intentFirewall, componentName, intent, i, i2, str, i3);
    }
}
