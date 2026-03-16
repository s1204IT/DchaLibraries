package android.util;

import android.app.Instrumentation;
import android.media.TtmlUtils;
import com.android.internal.util.XmlUtils;
import org.xmlpull.v1.XmlPullParser;

class XmlPullAttributes implements AttributeSet {
    XmlPullParser mParser;

    public XmlPullAttributes(XmlPullParser parser) {
        this.mParser = parser;
    }

    @Override
    public int getAttributeCount() {
        return this.mParser.getAttributeCount();
    }

    @Override
    public String getAttributeName(int index) {
        return this.mParser.getAttributeName(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return this.mParser.getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return this.mParser.getAttributeValue(namespace, name);
    }

    @Override
    public String getPositionDescription() {
        return this.mParser.getPositionDescription();
    }

    @Override
    public int getAttributeNameResource(int index) {
        return 0;
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        return XmlUtils.convertValueToList(getAttributeValue(namespace, attribute), options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
        return XmlUtils.convertValueToBoolean(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        return XmlUtils.convertValueToInt(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        return XmlUtils.convertValueToInt(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
        return XmlUtils.convertValueToUnsignedInt(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
        String s = getAttributeValue(namespace, attribute);
        if (s != null) {
            float defaultValue2 = Float.parseFloat(s);
            return defaultValue2;
        }
        return defaultValue;
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue) {
        return XmlUtils.convertValueToList(getAttributeValue(index), options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        return XmlUtils.convertValueToBoolean(getAttributeValue(index), defaultValue);
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        return XmlUtils.convertValueToInt(getAttributeValue(index), defaultValue);
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        return XmlUtils.convertValueToInt(getAttributeValue(index), defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return XmlUtils.convertValueToUnsignedInt(getAttributeValue(index), defaultValue);
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        String s = getAttributeValue(index);
        if (s != null) {
            float defaultValue2 = Float.parseFloat(s);
            return defaultValue2;
        }
        return defaultValue;
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
    public int getIdAttributeResourceValue(int defaultValue) {
        return getAttributeResourceValue(null, Instrumentation.REPORT_KEY_IDENTIFIER, defaultValue);
    }

    @Override
    public int getStyleAttribute() {
        return getAttributeResourceValue(null, TtmlUtils.TAG_STYLE, 0);
    }
}
