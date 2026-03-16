package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.Phonenumber;
import gov.nist.core.Separators;
import java.util.Arrays;

public final class PhoneNumberMatch {
    private final Phonenumber.PhoneNumber number;
    private final String rawString;
    private final int start;

    PhoneNumberMatch(int start, String rawString, Phonenumber.PhoneNumber number) {
        if (start < 0) {
            throw new IllegalArgumentException("Start index must be >= 0.");
        }
        if (rawString == null || number == null) {
            throw new NullPointerException();
        }
        this.start = start;
        this.rawString = rawString;
        this.number = number;
    }

    public Phonenumber.PhoneNumber number() {
        return this.number;
    }

    public int start() {
        return this.start;
    }

    public int end() {
        return this.start + this.rawString.length();
    }

    public String rawString() {
        return this.rawString;
    }

    public int hashCode() {
        return Arrays.hashCode(new Object[]{Integer.valueOf(this.start), this.rawString, this.number});
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PhoneNumberMatch)) {
            return false;
        }
        PhoneNumberMatch other = (PhoneNumberMatch) obj;
        return this.rawString.equals(other.rawString) && this.start == other.start && this.number.equals(other.number);
    }

    public String toString() {
        return "PhoneNumberMatch [" + start() + Separators.COMMA + end() + ") " + this.rawString;
    }
}
