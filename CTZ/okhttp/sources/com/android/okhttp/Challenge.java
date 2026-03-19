package com.android.okhttp;

import com.android.okhttp.internal.Util;

public final class Challenge {
    private final String realm;
    private final String scheme;

    public Challenge(String str, String str2) {
        this.scheme = str;
        this.realm = str2;
    }

    public String getScheme() {
        return this.scheme;
    }

    public String getRealm() {
        return this.realm;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Challenge) {
            Challenge challenge = (Challenge) obj;
            if (Util.equal(this.scheme, challenge.scheme) && Util.equal(this.realm, challenge.realm)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return (31 * (899 + (this.realm != null ? this.realm.hashCode() : 0))) + (this.scheme != null ? this.scheme.hashCode() : 0);
    }

    public String toString() {
        return this.scheme + " realm=\"" + this.realm + "\"";
    }
}
