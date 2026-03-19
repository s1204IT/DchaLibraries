package com.android.server.firewall;

import android.content.ComponentName;
import android.content.Intent;
import java.io.IOException;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class CategoryFilter implements Filter {
    private static final String ATTR_NAME = "name";
    public static final FilterFactory FACTORY = new FilterFactory("category") {
        @Override
        public Filter newFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            String attributeValue = xmlPullParser.getAttributeValue(null, "name");
            if (attributeValue == null) {
                throw new XmlPullParserException("Category name must be specified.", xmlPullParser, null);
            }
            return new CategoryFilter(attributeValue);
        }
    };
    private final String mCategoryName;

    private CategoryFilter(String str) {
        this.mCategoryName = str;
    }

    @Override
    public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
        Set<String> categories = intent.getCategories();
        if (categories == null) {
            return false;
        }
        return categories.contains(this.mCategoryName);
    }
}
