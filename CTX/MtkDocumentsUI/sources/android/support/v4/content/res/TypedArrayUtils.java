package android.support.v4.content.res;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import org.xmlpull.v1.XmlPullParser;

public class TypedArrayUtils {
    public static boolean hasAttribute(XmlPullParser parser, String attrName) {
        return parser.getAttributeValue("http://schemas.android.com/apk/res/android", attrName) != null;
    }

    public static float getNamedFloat(TypedArray a, XmlPullParser parser, String attrName, int resId, float defaultValue) {
        boolean hasAttr = hasAttribute(parser, attrName);
        if (!hasAttr) {
            return defaultValue;
        }
        return a.getFloat(resId, defaultValue);
    }

    public static boolean getNamedBoolean(TypedArray a, XmlPullParser parser, String attrName, int resId, boolean defaultValue) {
        boolean hasAttr = hasAttribute(parser, attrName);
        if (!hasAttr) {
            return defaultValue;
        }
        return a.getBoolean(resId, defaultValue);
    }

    public static int getNamedInt(TypedArray a, XmlPullParser parser, String attrName, int resId, int defaultValue) {
        boolean hasAttr = hasAttribute(parser, attrName);
        if (!hasAttr) {
            return defaultValue;
        }
        return a.getInt(resId, defaultValue);
    }

    public static int getNamedColor(TypedArray a, XmlPullParser parser, String attrName, int resId, int defaultValue) {
        boolean hasAttr = hasAttribute(parser, attrName);
        if (!hasAttr) {
            return defaultValue;
        }
        return a.getColor(resId, defaultValue);
    }

    public static int getNamedResourceId(TypedArray a, XmlPullParser parser, String attrName, int resId, int defaultValue) {
        boolean hasAttr = hasAttribute(parser, attrName);
        if (!hasAttr) {
            return defaultValue;
        }
        return a.getResourceId(resId, defaultValue);
    }

    public static String getNamedString(TypedArray a, XmlPullParser parser, String attrName, int resId) {
        boolean hasAttr = hasAttribute(parser, attrName);
        if (!hasAttr) {
            return null;
        }
        return a.getString(resId);
    }

    public static TypedValue peekNamedValue(TypedArray a, XmlPullParser parser, String attrName, int resId) {
        boolean hasAttr = hasAttribute(parser, attrName);
        if (!hasAttr) {
            return null;
        }
        return a.peekValue(resId);
    }

    public static TypedArray obtainAttributes(Resources res, Resources.Theme theme, AttributeSet set, int[] attrs) {
        if (theme == null) {
            return res.obtainAttributes(set, attrs);
        }
        return theme.obtainStyledAttributes(set, attrs, 0, 0);
    }
}
