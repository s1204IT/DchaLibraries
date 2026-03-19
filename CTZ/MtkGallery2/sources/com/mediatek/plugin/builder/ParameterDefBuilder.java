package com.mediatek.plugin.builder;

import com.mediatek.plugin.element.Element;
import com.mediatek.plugin.element.ParameterDef;
import com.mediatek.plugin.res.IResource;
import org.xmlpull.v1.XmlPullParser;

class ParameterDefBuilder extends Builder {
    private static final String SUPPORT_TAG = "parameter-def";
    private static final String VALUE_ID = "id";
    private static final String VALUE_TYPE = "type";

    ParameterDefBuilder() {
    }

    @Override
    public String getSupportedTag() {
        return SUPPORT_TAG;
    }

    @Override
    public Element parser(XmlPullParser xmlPullParser, IResource iResource) {
        ParameterDef parameterDef = new ParameterDef();
        parameterDef.id = xmlPullParser.getAttributeValue(null, "id");
        String attributeValue = xmlPullParser.getAttributeValue(null, VALUE_TYPE);
        if (attributeValue != null) {
            parameterDef.type = ParameterDef.ParameterType.valueOf(attributeValue.toUpperCase());
        }
        return parameterDef;
    }
}
