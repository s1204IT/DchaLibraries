package android.support.v4.media;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.SessionToken2;
import android.support.v4.media.session.MediaSessionCompat;

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

    @Override
    public int getUid() {
        return -1;
    }

    @Override
    @NonNull
    public String getPackageName() {
        return null;
    }

    @Override
    @Nullable
    public String getServiceName() {
        return null;
    }

    @Override
    public ComponentName getComponentName() {
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("android.media.token.type", 100);
        bundle.putBundle("android.media.token.LEGACY", this.mLegacyToken.toBundle());
        return bundle;
    }

    @Override
    public Object getBinder() {
        return this.mLegacyToken;
    }

    public static SessionToken2ImplLegacy fromBundle(@NonNull Bundle bundle) {
        Bundle legacyTokenBundle = bundle.getBundle("android.media.token.LEGACY");
        return new SessionToken2ImplLegacy(MediaSessionCompat.Token.fromBundle(legacyTokenBundle));
    }
}
