package android.util;

import android.app.Instrumentation;
import android.media.TtmlUtils;
import com.android.internal.util.XmlUtils;
import org.xmlpull.v1.XmlPullParser;

class XmlPullAttributes implements AttributeSet {
    XmlPullParser mParser;

    public XmlPullAttributes(XmlPullParser xmlPullParser) {
        this.mParser = xmlPullParser;
    }

    @Override
    public int getAttributeCount() {
        return this.mParser.getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int i) {
        return this.mParser.getAttributeNamespace(i);
    }

    @Override
    public String getAttributeName(int i) {
        return this.mParser.getAttributeName(i);
    }

    @Override
    public String getAttributeValue(int i) {
        return this.mParser.getAttributeValue(i);
    }

    @Override
    public String getAttributeValue(String str, String str2) {
        return this.mParser.getAttributeValue(str, str2);
    }

    @Override
    public String getPositionDescription() {
        return this.mParser.getPositionDescription();
    }

    @Override
    public int getAttributeNameResource(int i) {
        return 0;
    }

    @Override
    public int getAttributeListValue(String str, String str2, String[] strArr, int i) {
        return XmlUtils.convertValueToList(getAttributeValue(str, str2), strArr, i);
    }

    @Override
    public boolean getAttributeBooleanValue(String str, String str2, boolean z) {
        return XmlUtils.convertValueToBoolean(getAttributeValue(str, str2), z);
    }

    @Override
    public int getAttributeResourceValue(String str, String str2, int i) {
        return XmlUtils.convertValueToInt(getAttributeValue(str, str2), i);
    }

    @Override
    public int getAttributeIntValue(String str, String str2, int i) {
        return XmlUtils.convertValueToInt(getAttributeValue(str, str2), i);
    }

    @Override
    public int getAttributeUnsignedIntValue(String str, String str2, int i) {
        return XmlUtils.convertValueToUnsignedInt(getAttributeValue(str, str2), i);
    }

    @Override
    public float getAttributeFloatValue(String str, String str2, float f) {
        String attributeValue = getAttributeValue(str, str2);
        if (attributeValue != null) {
            return Float.parseFloat(attributeValue);
        }
        return f;
    }

    @Override
    public int getAttributeListValue(int i, String[] strArr, int i2) {
        return XmlUtils.convertValueToList(getAttributeValue(i), strArr, i2);
    }

    @Override
    public boolean getAttributeBooleanValue(int i, boolean z) {
        return XmlUtils.convertValueToBoolean(getAttributeValue(i), z);
    }

    @Override
    public int getAttributeResourceValue(int i, int i2) {
        return XmlUtils.convertValueToInt(getAttributeValue(i), i2);
    }

    @Override
    public int getAttributeIntValue(int i, int i2) {
        return XmlUtils.convertValueToInt(getAttributeValue(i), i2);
    }

    @Override
    public int getAttributeUnsignedIntValue(int i, int i2) {
        return XmlUtils.convertValueToUnsignedInt(getAttributeValue(i), i2);
    }

    @Override
    public float getAttributeFloatValue(int i, float f) {
        String attributeValue = getAttributeValue(i);
        if (attributeValue != null) {
            return Float.parseFloat(attributeValue);
        }
        return f;
    }

    @Override
    public String getIdAttribute() {
        return getAttributeValue(null, Instrumentation.REPORT_KEY_IDENTIFIER);
    }

    @Override
    public String getClassAttribute() {
        return getAttributeValue(null, "class");
    }

    @Override
    public int getIdAttributeResourceValue(int i) {
        return getAttributeResourceValue(null, Instrumentation.REPORT_KEY_IDENTIFIER, i);
    }

    @Override
    public int getStyleAttribute() {
        return getAttributeResourceValue(null, TtmlUtils.TAG_STYLE, 0);
    }
}
