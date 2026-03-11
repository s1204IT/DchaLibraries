package android.support.v4.media;

import android.os.Bundle;

public final class SessionToken2 {
    private final SupportLibraryImpl mImpl;

    interface SupportLibraryImpl {
    }

    SessionToken2(SupportLibraryImpl supportLibraryImpl) {
        this.mImpl = supportLibraryImpl;
    }

    public static SessionToken2 fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        return bundle.getInt("android.media.token.type", -1) == 100 ? new SessionToken2(SessionToken2ImplLegacy.fromBundle(bundle)) : new SessionToken2(SessionToken2ImplBase.fromBundle(bundle));
    }

    public boolean equals(Object obj) {
        if (obj instanceof SessionToken2) {
            return this.mImpl.equals(((SessionToken2) obj).mImpl);
        }
        return false;
    }

    public int hashCode() {
        return this.mImpl.hashCode();
    }

    public String toString() {
        return this.mImpl.toString();
    }
}
