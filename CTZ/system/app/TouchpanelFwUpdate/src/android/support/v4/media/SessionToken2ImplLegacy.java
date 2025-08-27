package android.support.v4.media;

import android.os.Bundle;
import android.support.v4.media.SessionToken2;
import android.support.v4.media.session.MediaSessionCompat;

/* loaded from: classes.dex */
final class SessionToken2ImplLegacy implements SessionToken2.SupportLibraryImpl {
    private final MediaSessionCompat.Token mLegacyToken;

    SessionToken2ImplLegacy(MediaSessionCompat.Token token) {
        this.mLegacyToken = token;
    }

    public int hashCode() {
        return this.mLegacyToken.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken2ImplLegacy)) {
            return false;
        }
        SessionToken2ImplLegacy other = (SessionToken2ImplLegacy) obj;
        return this.mLegacyToken.equals(other.mLegacyToken);
    }

    public String toString() {
        return "SessionToken2 {legacyToken=" + this.mLegacyToken + "}";
    }

    public static SessionToken2ImplLegacy fromBundle(Bundle bundle) {
        Bundle legacyTokenBundle = bundle.getBundle("android.media.token.LEGACY");
        return new SessionToken2ImplLegacy(MediaSessionCompat.Token.fromBundle(legacyTokenBundle));
    }
}
