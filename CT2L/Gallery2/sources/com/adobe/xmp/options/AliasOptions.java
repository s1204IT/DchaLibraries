package com.adobe.xmp.options;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import com.adobe.xmp.XMPException;

public final class AliasOptions extends Options {
    public AliasOptions() {
    }

    public AliasOptions(int options) throws XMPException {
        super(options);
    }

    public boolean isArray() {
        return getOption(NotificationCompat.FLAG_GROUP_SUMMARY);
    }

    public AliasOptions setArrayOrdered(boolean value) {
        setOption(1536, value);
        return this;
    }

    public boolean isArrayAltText() {
        return getOption(FragmentTransaction.TRANSIT_ENTER_MASK);
    }

    public AliasOptions setArrayAltText(boolean value) {
        setOption(7680, value);
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
