package android.security.keystore;

import java.security.Key;

public class AndroidKeyStoreKey implements Key {
    private final String mAlgorithm;
    private final String mAlias;
    private final int mUid;

    public AndroidKeyStoreKey(String str, int i, String str2) {
        this.mAlias = str;
        this.mUid = i;
        this.mAlgorithm = str2;
    }

    String getAlias() {
        return this.mAlias;
    }

    int getUid() {
        return this.mUid;
    }

    @Override
    public String getAlgorithm() {
        return this.mAlgorithm;
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public byte[] getEncoded() {
        return null;
    }

    public int hashCode() {
        return (31 * ((((this.mAlgorithm == null ? 0 : this.mAlgorithm.hashCode()) + 31) * 31) + (this.mAlias != null ? this.mAlias.hashCode() : 0))) + this.mUid;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AndroidKeyStoreKey androidKeyStoreKey = (AndroidKeyStoreKey) obj;
        if (this.mAlgorithm == null) {
            if (androidKeyStoreKey.mAlgorithm != null) {
                return false;
            }
        } else if (!this.mAlgorithm.equals(androidKeyStoreKey.mAlgorithm)) {
            return false;
        }
        if (this.mAlias == null) {
            if (androidKeyStoreKey.mAlias != null) {
                return false;
            }
        } else if (!this.mAlias.equals(androidKeyStoreKey.mAlias)) {
            return false;
        }
        if (this.mUid == androidKeyStoreKey.mUid) {
            return true;
        }
        return false;
    }
}
