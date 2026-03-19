package com.adobe.xmp.options;

import com.adobe.xmp.XMPException;

public final class AliasOptions extends Options {
    public AliasOptions() {
    }

    public AliasOptions(int i) throws XMPException {
        super(i);
    }

    public boolean isArray() {
        return getOption(512);
    }

    public AliasOptions setArrayOrdered(boolean z) {
        setOption(1536, z);
        return this;
    }

    public boolean isArrayAltText() {
        return getOption(4096);
    }

    public AliasOptions setArrayAltText(boolean z) {
        setOption(7680, z);
        return this;
    }

    public PropertyOptions toPropertyOptions() throws XMPException {
        return new PropertyOptions(getOptions());
    }

    @Override
    protected int getValidOptions() {
        return 7680;
    }
}
