package com.mediatek.plugin.builder;

import com.mediatek.plugin.element.Element;
import com.mediatek.plugin.element.Extension;
import com.mediatek.plugin.res.IResource;
import com.mediatek.plugin.utils.Log;
import org.xmlpull.v1.XmlPullParser;

class ExtensionBuilder extends Builder {
    private static final String SUPPORT_TAG = "extension";
    private static final String TAG = "PluginManager/ExtensionBuilder";
    private static final String VALUE_CLASS = "class";
    private static final String VALUE_ICON = "icon";
    private static final String VALUE_ID = "id";
    private static final String VALUE_NAME = "name";
    private static final String VALUE_PLUGIN_ID = "plugin-id";
    private static final String VALUE_POINT_ID = "point-id";

    ExtensionBuilder() {
    }

    @Override
    public String getSupportedTag() {
        return "extension";
    }

    @Override
    public Element parser(XmlPullParser xmlPullParser, IResource iResource) {
        Log.d(TAG, "<parser> START_TAG  >>>>>>>> name = " + xmlPullParser.getName());
        Extension extension = new Extension();
        extension.id = xmlPullParser.getAttributeValue(null, "id");
        extension.name = iResource.getString(xmlPullParser.getAttributeValue(null, "name"));
        extension.pluginId = xmlPullParser.getAttributeValue(null, VALUE_PLUGIN_ID);
        extension.extensionPointId = xmlPullParser.getAttributeValue(null, VALUE_POINT_ID);
        String attributeValue = xmlPullParser.getAttributeValue(null, VALUE_ICON);
        if (attributeValue != null) {
            extension.drawable = iResource.getDrawable(attributeValue);
        }
        extension.className = xmlPullParser.getAttributeValue(null, "class");
        Log.d(TAG, "<parser> START_TAG  <<<<<<<<<< name = " + xmlPullParser.getName());
        return extension;
    }
}
