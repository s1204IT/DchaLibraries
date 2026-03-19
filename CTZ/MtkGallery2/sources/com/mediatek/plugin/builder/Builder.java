package com.mediatek.plugin.builder;

import com.mediatek.plugin.element.Element;
import com.mediatek.plugin.res.IResource;
import org.xmlpull.v1.XmlPullParser;

public abstract class Builder {
    public abstract String getSupportedTag();

    public abstract Element parser(XmlPullParser xmlPullParser, IResource iResource);

    public void bind(Element element, Element element2) {
        element.addChild(element2);
        element2.setParent(element);
    }
}
