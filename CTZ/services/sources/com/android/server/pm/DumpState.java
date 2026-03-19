package com.android.server.pm;

public final class DumpState {
    public static final int DUMP_ACTIVITY_RESOLVERS = 4;
    public static final int DUMP_CHANGES = 4194304;
    public static final int DUMP_COMPILER_STATS = 2097152;
    public static final int DUMP_CONTENT_RESOLVERS = 32;
    public static final int DUMP_DEXOPT = 1048576;
    public static final int DUMP_DOMAIN_PREFERRED = 262144;
    public static final int DUMP_FEATURES = 2;
    public static final int DUMP_FROZEN = 524288;
    public static final int DUMP_INSTALLS = 65536;
    public static final int DUMP_INTENT_FILTER_VERIFIERS = 131072;
    public static final int DUMP_KEYSETS = 16384;
    public static final int DUMP_LIBS = 1;
    public static final int DUMP_MESSAGES = 512;
    public static final int DUMP_PACKAGES = 128;
    public static final int DUMP_PERMISSIONS = 64;
    public static final int DUMP_PREFERRED = 4096;
    public static final int DUMP_PREFERRED_XML = 8192;
    public static final int DUMP_PROVIDERS = 1024;
    public static final int DUMP_RECEIVER_RESOLVERS = 16;
    public static final int DUMP_SERVICE_PERMISSIONS = 16777216;
    public static final int DUMP_SERVICE_RESOLVERS = 8;
    public static final int DUMP_SHARED_USERS = 256;
    public static final int DUMP_VERIFIERS = 2048;
    public static final int DUMP_VERSION = 32768;
    public static final int DUMP_VOLUMES = 8388608;
    public static final int OPTION_SHOW_FILTERS = 1;
    private int mOptions;
    private SharedUserSetting mSharedUser;
    private boolean mTitlePrinted;
    private int mTypes;

    public boolean isDumping(int i) {
        return (this.mTypes == 0 && i != 8192) || (i & this.mTypes) != 0;
    }

    public void setDump(int i) {
        this.mTypes = i | this.mTypes;
    }

    public boolean isOptionEnabled(int i) {
        return (i & this.mOptions) != 0;
    }

    public void setOptionEnabled(int i) {
        this.mOptions = i | this.mOptions;
    }

    public boolean onTitlePrinted() {
        boolean z = this.mTitlePrinted;
        this.mTitlePrinted = true;
        return z;
    }

    public boolean getTitlePrinted() {
        return this.mTitlePrinted;
    }

    public void setTitlePrinted(boolean z) {
        this.mTitlePrinted = z;
    }

    public SharedUserSetting getSharedUser() {
        return this.mSharedUser;
    }

    public void setSharedUser(SharedUserSetting sharedUserSetting) {
        this.mSharedUser = sharedUserSetting;
    }
}
