package com.android.providers.contacts.util;

import android.util.Log;
import com.android.providers.contacts.AbstractContactsProvider;

public class CappedStringBuilder {
    private final int mCapSize;
    private boolean mOver;
    private final StringBuilder mStringBuilder = new StringBuilder();

    public CappedStringBuilder(int i) {
        this.mCapSize = i;
    }

    public void clear() {
        this.mOver = false;
        this.mStringBuilder.setLength(0);
    }

    public int length() {
        return this.mStringBuilder.length();
    }

    public String toString() {
        return this.mStringBuilder.toString();
    }

    public CappedStringBuilder append(char c) {
        if (canAppend(this.mStringBuilder.length() + 1)) {
            this.mStringBuilder.append(c);
        }
        return this;
    }

    public CappedStringBuilder append(String str) {
        if (canAppend(this.mStringBuilder.length() + str.length())) {
            this.mStringBuilder.append(str);
        }
        return this;
    }

    private boolean canAppend(int i) {
        if (!this.mOver && i <= this.mCapSize) {
            return true;
        }
        if (!this.mOver && AbstractContactsProvider.VERBOSE_LOGGING) {
            Log.w("ContactsProvider", "String too long! new length=" + i);
        }
        this.mOver = true;
        return false;
    }
}
