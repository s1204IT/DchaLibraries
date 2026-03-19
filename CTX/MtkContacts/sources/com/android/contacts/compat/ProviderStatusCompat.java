package com.android.contacts.compat;

public class ProviderStatusCompat {
    public static final int STATUS_BUSY;
    public static final int STATUS_EMPTY;
    public static final boolean USE_CURRENT_VERSION = CompatUtils.isMarshmallowCompatible();

    static {
        STATUS_EMPTY = USE_CURRENT_VERSION ? 2 : 4;
        boolean z = USE_CURRENT_VERSION;
        STATUS_BUSY = 1;
    }
}
