package android.content.res;

import android.os.IBinder;

public final class ResourcesKey {
    public final int mDisplayId;
    private final int mHash;
    public final Configuration mOverrideConfiguration = new Configuration();
    final String mResDir;
    final float mScale;
    private final IBinder mToken;

    public ResourcesKey(String resDir, int displayId, Configuration overrideConfiguration, float scale, IBinder token) {
        this.mResDir = resDir;
        this.mDisplayId = displayId;
        if (overrideConfiguration != null) {
            this.mOverrideConfiguration.setTo(overrideConfiguration);
        }
        this.mScale = scale;
        this.mToken = token;
        int hash = (this.mResDir == null ? 0 : this.mResDir.hashCode()) + 527;
        this.mHash = (((((hash * 31) + this.mDisplayId) * 31) + (this.mOverrideConfiguration != null ? this.mOverrideConfiguration.hashCode() : 0)) * 31) + Float.floatToIntBits(this.mScale);
    }

    public boolean hasOverrideConfiguration() {
        return !Configuration.EMPTY.equals(this.mOverrideConfiguration);
    }

    public int hashCode() {
        return this.mHash;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ResourcesKey)) {
            return false;
        }
        ResourcesKey peer = (ResourcesKey) obj;
        if (this.mResDir == null && peer.mResDir != null) {
            return false;
        }
        if (this.mResDir != null && peer.mResDir == null) {
            return false;
        }
        if ((this.mResDir == null || peer.mResDir == null || this.mResDir.equals(peer.mResDir)) && this.mDisplayId == peer.mDisplayId) {
            return (this.mOverrideConfiguration == peer.mOverrideConfiguration || !(this.mOverrideConfiguration == null || peer.mOverrideConfiguration == null || !this.mOverrideConfiguration.equals(peer.mOverrideConfiguration))) && this.mScale == peer.mScale;
        }
        return false;
    }

    public String toString() {
        return Integer.toHexString(this.mHash);
    }
}
