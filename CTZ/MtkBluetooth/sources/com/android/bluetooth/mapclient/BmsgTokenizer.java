package com.android.bluetooth.mapclient;

import android.util.Log;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BmsgTokenizer {
    private final Matcher mMatcher;
    private final int mOffset;
    private int mPos;
    private final String mStr;

    public BmsgTokenizer(String str) {
        this(str, 0);
    }

    public BmsgTokenizer(String str, int i) {
        this.mPos = 0;
        this.mStr = str;
        this.mOffset = i;
        this.mMatcher = Pattern.compile("(([^:]*):(.*))?\r\n").matcher(str);
        this.mPos = this.mMatcher.regionStart();
    }

    public Property next(boolean z) throws ParseException {
        boolean z2 = false;
        do {
            this.mMatcher.region(this.mPos, this.mMatcher.regionEnd());
            if (!this.mMatcher.lookingAt()) {
                if (z) {
                    return null;
                }
                throw new ParseException("Property or empty line expected", pos());
            }
            this.mPos = this.mMatcher.end();
            if (this.mMatcher.group(1) != null) {
                z2 = true;
            }
        } while (!z2);
        return new Property(this.mMatcher.group(2), this.mMatcher.group(3));
    }

    public Property next() throws ParseException {
        return next(false);
    }

    public String remaining() {
        return this.mStr.substring(this.mPos);
    }

    public int pos() {
        return this.mPos + this.mOffset;
    }

    public static class Property {
        public final String name;
        public final String value;

        public Property(String str, String str2) {
            if (str == null || str2 == null) {
                throw new IllegalArgumentException();
            }
            this.name = str;
            this.value = str2;
            Log.v("BMSG >> ", toString());
        }

        public String toString() {
            return this.name + ":" + this.value;
        }

        public boolean equals(Object obj) {
            return (obj instanceof Property) && obj.name.equals(this.name) && obj.value.equals(this.value);
        }
    }
}
