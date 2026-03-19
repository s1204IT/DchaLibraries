package com.mediatek.plugin.builder;

import com.mediatek.plugin.element.Element;
import com.mediatek.plugin.element.ExtensionPoint;
import com.mediatek.plugin.res.IResource;
import com.mediatek.plugin.utils.Log;
import org.xmlpull.v1.XmlPullParser;

class ExtensionPointBuilder extends Builder {
    private static final String SUPPORT_TAG = "extension-point";
    private static final String TAG = "PluginManager/ExtensionPointBuilder";
    private static final String VALUE_CLASS = "class";
    private static final String VALUE_ID = "id";

    ExtensionPointBuilder() {
    }

    @Override
    public String getSupportedTag() {
        return SUPPORT_TAG;
    }

    @Override
    public Element parser(XmlPullParser xmlPullParser, IResource iResource) {
        Log.d(TAG, "<parser> START_TAG  >>>>>>>> name = " + xmlPullParser.getName());
        ExtensionPoint extensionPoint = new ExtensionPoint();
        extensionPoint.id = xmlPullParser.getAttributeValue(null, "id");
        extensionPoint.className = xmlPullParser.getAttributeValue(null, "class");
        return extensionPoint;
    }
}
