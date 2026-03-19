package com.adobe.xmp.options;

import com.adobe.xmp.XMPException;
import com.mediatek.plugin.preload.SoOperater;

public final class PropertyOptions extends Options {
    public PropertyOptions() {
    }

    public PropertyOptions(int i) throws XMPException {
        super(i);
    }

    public PropertyOptions setHasQualifiers(boolean z) {
        setOption(16, z);
        return this;
    }

    public boolean isQualifier() {
        return getOption(32);
    }

    public PropertyOptions setQualifier(boolean z) {
        setOption(32, z);
        return this;
    }

    public boolean getHasLanguage() {
        return getOption(64);
    }

    public PropertyOptions setHasLanguage(boolean z) {
        setOption(64, z);
        return this;
    }

    public PropertyOptions setHasType(boolean z) {
        setOption(128, z);
        return this;
    }

    public boolean isStruct() {
        return getOption(256);
    }

    public PropertyOptions setStruct(boolean z) {
        setOption(256, z);
        return this;
    }

    public boolean isArray() {
        return getOption(512);
    }

    public PropertyOptions setArray(boolean z) {
        setOption(512, z);
        return this;
    }

    public boolean isArrayOrdered() {
        return getOption(SoOperater.STEP);
    }

    public PropertyOptions setArrayOrdered(boolean z) {
        setOption(SoOperater.STEP, z);
        return this;
    }

    public boolean isArrayAlternate() {
        return getOption(2048);
    }

    public PropertyOptions setArrayAlternate(boolean z) {
        setOption(2048, z);
        return this;
    }

    public boolean isArrayAltText() {
        return getOption(4096);
    }

    public boolean isSchemaNode() {
        return getOption(Integer.MIN_VALUE);
    }

    public PropertyOptions setSchemaNode(boolean z) {
        setOption(Integer.MIN_VALUE, z);
        return this;
    }

    public boolean isCompositeProperty() {
        return (getOptions() & 768) > 0;
    }

    public void mergeWith(PropertyOptions propertyOptions) throws XMPException {
        if (propertyOptions != null) {
            setOptions(propertyOptions.getOptions() | getOptions());
        }
    }

    @Override
    protected int getValidOptions() {
        return -2147475470;
    }

    @Override
    public void assertConsistency(int i) throws XMPException {
        if ((i & 256) > 0 && (i & 512) > 0) {
            throw new XMPException("IsStruct and IsArray options are mutually exclusive", 103);
        }
        if ((i & 2) > 0 && (i & 768) > 0) {
            throw new XMPException("Structs and arrays can't have \"value\" options", 103);
        }
    }
}
