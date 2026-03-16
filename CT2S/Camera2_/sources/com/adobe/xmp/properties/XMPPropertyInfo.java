package com.adobe.xmp.properties;

import com.adobe.xmp.options.PropertyOptions;

public interface XMPPropertyInfo extends XMPProperty {
    String getNamespace();

    @Override
    PropertyOptions getOptions();

    String getPath();

    @Override
    Object getValue();
}
