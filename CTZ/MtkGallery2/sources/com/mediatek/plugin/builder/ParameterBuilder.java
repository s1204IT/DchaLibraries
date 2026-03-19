package com.mediatek.plugin.builder;

import com.mediatek.plugin.element.Element;
import com.mediatek.plugin.element.Parameter;
import com.mediatek.plugin.res.IResource;
import org.xmlpull.v1.XmlPullParser;

class ParameterBuilder extends Builder {
    private static final String SUPPORT_TAG = "parameter";
    private static final String VALUE_ID = "id";
    private static final String VALUE_V = "value";

    ParameterBuilder() {
    }

    @Override
    public String getSupportedTag() {
        return SUPPORT_TAG;
    }

    @Override
    public Element parser(XmlPullParser xmlPullParser, IResource iResource) {
        Parameter parameter = new Parameter();
        parameter.id = xmlPullParser.getAttributeValue(null, "id");
        parameter.value = xmlPullParser.getAttributeValue(null, VALUE_V);
        return parameter;
    }
}
