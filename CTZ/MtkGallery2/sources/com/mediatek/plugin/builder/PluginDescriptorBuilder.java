package com.mediatek.plugin.builder;

import com.mediatek.plugin.element.Element;
import com.mediatek.plugin.element.PluginDescriptor;
import com.mediatek.plugin.res.IResource;
import org.xmlpull.v1.XmlPullParser;

class PluginDescriptorBuilder extends Builder {
    public static final String REQUIRE_MAX_HOST_VERSION = "require-max-host-version";
    public static final String REQUIRE_MIN_HOST_VERSION = "require-min-host-version";
    private static final String SUPPORT_TAG = "plugin";
    public static final String VALUE_CLASS = "class";
    public static final String VALUE_ID = "id";
    public static final String VALUE_NAME = "name";
    public static final String VALUE_REQUEST_ID = "require-plugin-id";
    public static final String VALUE_VERSION = "version";

    PluginDescriptorBuilder() {
    }

    @Override
    public String getSupportedTag() {
        return SUPPORT_TAG;
    }

    @Override
    public Element parser(XmlPullParser xmlPullParser, IResource iResource) {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.id = xmlPullParser.getAttributeValue(null, VALUE_ID);
        pluginDescriptor.name = xmlPullParser.getAttributeValue(null, VALUE_NAME);
        String attributeValue = xmlPullParser.getAttributeValue(null, VALUE_VERSION);
        if (attributeValue != null) {
            pluginDescriptor.version = Integer.parseInt(attributeValue);
        }
        pluginDescriptor.className = xmlPullParser.getAttributeValue(null, VALUE_CLASS);
        if (pluginDescriptor.className == null) {
            pluginDescriptor.className = "com.mediatek.plugin.Plugin";
        }
        String attributeValue2 = xmlPullParser.getAttributeValue(null, VALUE_REQUEST_ID);
        if (iResource != null) {
            pluginDescriptor.requiredPluginIds = iResource.getString(attributeValue2, "\\|");
        }
        String attributeValue3 = xmlPullParser.getAttributeValue(null, REQUIRE_MAX_HOST_VERSION);
        if (attributeValue3 != null) {
            pluginDescriptor.requireMaxHostVersion = Integer.parseInt(attributeValue3);
        }
        String attributeValue4 = xmlPullParser.getAttributeValue(null, REQUIRE_MIN_HOST_VERSION);
        if (attributeValue4 != null) {
            pluginDescriptor.requireMinHostVersion = Integer.parseInt(attributeValue4);
        }
        return pluginDescriptor;
    }
}
