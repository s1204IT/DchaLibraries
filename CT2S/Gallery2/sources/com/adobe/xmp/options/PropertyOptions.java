package com.adobe.xmp.options;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import com.adobe.xmp.XMPException;

public final class PropertyOptions extends Options {
    public PropertyOptions() {
    }

    public PropertyOptions(int options) throws XMPException {
        super(options);
    }

    public PropertyOptions setHasQualifiers(boolean value) {
        setOption(16, value);
        return this;
    }

    public boolean isQualifier() {
        return getOption(32);
    }

    public PropertyOptions setQualifier(boolean value) {
        setOption(32, value);
        return this;
    }

    public boolean getHasLanguage() {
        return getOption(64);
    }

    public PropertyOptions setHasLanguage(boolean value) {
        setOption(64, value);
        return this;
    }

    public PropertyOptions setHasType(boolean value) {
        setOption(128, value);
        return this;
    }

    public boolean isStruct() {
        return getOption(NotificationCompat.FLAG_LOCAL_ONLY);
    }

    public PropertyOptions setStruct(boolean value) {
        setOption(NotificationCompat.FLAG_LOCAL_ONLY, value);
        return this;
    }

    public boolean isArray() {
        return getOption(NotificationCompat.FLAG_GROUP_SUMMARY);
    }

    public PropertyOptions setArray(boolean value) {
        setOption(NotificationCompat.FLAG_GROUP_SUMMARY, value);
        return this;
    }

    public boolean isArrayOrdered() {
        return getOption(1024);
    }

    public PropertyOptions setArrayOrdered(boolean value) {
        setOption(1024, value);
        return this;
    }

    public boolean isArrayAlternate() {
        return getOption(2048);
    }

    public PropertyOptions setArrayAlternate(boolean value) {
        setOption(2048, value);
        return this;
    }

    public boolean isArrayAltText() {
        return getOption(FragmentTransaction.TRANSIT_ENTER_MASK);
    }

    public boolean isSchemaNode() {
        return getOption(Integer.MIN_VALUE);
    }

    public PropertyOptions setSchemaNode(boolean value) {
        setOption(Integer.MIN_VALUE, value);
        return this;
    }

    public boolean isCompositeProperty() {
        return (getOptions() & 768) > 0;
    }

    public void mergeWith(PropertyOptions options) throws XMPException {
        if (options != null) {
            setOptions(getOptions() | options.getOptions());
        }
    }

    @Override
    protected int getValidOptions() {
        return -2147475470;
    }

    @Override
    public void assertConsistency(int options) throws XMPException {
        if ((options & NotificationCompat.FLAG_LOCAL_ONLY) > 0 && (options & NotificationCompat.FLAG_GROUP_SUMMARY) > 0) {
            throw new XMPException("IsStruct and IsArray options are mutually exclusive", 103);
        }
        if ((options & 2) > 0 && (options & 768) > 0) {
            throw new XMPException("Structs and arrays can't have \"value\" options", 103);
        }
    }
}
