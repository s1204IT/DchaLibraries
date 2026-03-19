package com.android.okhttp;

import com.android.okhttp.internal.Util;

public final class Challenge {
    private final String nonce;
    private final String opaque;
    private final String qop;
    private final String realm;
    private final String scheme;
    private final String stale;

    public Challenge(String scheme, String realm) {
        this.scheme = scheme;
        this.realm = realm;
        this.nonce = null;
        this.stale = null;
        this.qop = null;
        this.opaque = null;
    }

    public Challenge(String scheme, String realm, String nonce, String stale, String qop, String opaque) {
        this.scheme = scheme;
        this.realm = realm;
        this.nonce = nonce;
        this.stale = stale;
        this.qop = qop;
        this.opaque = opaque;
    }

    public String getScheme() {
        return this.scheme;
    }

    public String getRealm() {
        if (System.getProperty("digest.gbarealm") != null && !System.getProperty("digest.gbarealm").isEmpty()) {
            return System.getProperty("digest.gbarealm");
        }
        return this.realm;
    }

    public String getNonce() {
        return this.nonce;
    }

    public String getStale() {
        return this.stale;
    }

    public String getQop() {
        return this.qop;
    }

    public String getOpaque() {
        return this.opaque;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof Challenge) && Util.equal(this.scheme, obj.scheme)) {
            return Util.equal(this.realm, obj.realm);
        }
        return false;
    }

    public int hashCode() {
        int result = (this.realm != null ? this.realm.hashCode() : 0) + 899;
        return (result * 31) + (this.scheme != null ? this.scheme.hashCode() : 0);
    }

    public String toString() {
        return this.scheme + " realm=\"" + this.realm + "\"";
    }
}
