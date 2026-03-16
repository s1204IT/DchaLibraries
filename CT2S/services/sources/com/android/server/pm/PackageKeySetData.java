package com.android.server.pm;

import android.util.ArrayMap;
import com.android.internal.util.ArrayUtils;

public class PackageKeySetData {
    static final long KEYSET_UNASSIGNED = -1;
    private long[] mDefinedKeySets;
    private final ArrayMap<String, Long> mKeySetAliases;
    private long mProperSigningKeySet;
    private long[] mSigningKeySets;
    private long[] mUpgradeKeySets;

    PackageKeySetData() {
        this.mKeySetAliases = new ArrayMap<>();
        this.mProperSigningKeySet = -1L;
    }

    PackageKeySetData(PackageKeySetData original) {
        this.mKeySetAliases = new ArrayMap<>();
        this.mProperSigningKeySet = original.mProperSigningKeySet;
        this.mSigningKeySets = ArrayUtils.cloneOrNull(original.mSigningKeySets);
        this.mUpgradeKeySets = ArrayUtils.cloneOrNull(original.mUpgradeKeySets);
        this.mDefinedKeySets = ArrayUtils.cloneOrNull(original.mDefinedKeySets);
        this.mKeySetAliases.putAll((ArrayMap<? extends String, ? extends Long>) original.mKeySetAliases);
    }

    protected void setProperSigningKeySet(long ks) {
        if (ks != this.mProperSigningKeySet) {
            removeAllSigningKeySets();
            this.mProperSigningKeySet = ks;
            addSigningKeySet(ks);
        }
    }

    protected long getProperSigningKeySet() {
        return this.mProperSigningKeySet;
    }

    protected void addSigningKeySet(long ks) {
        this.mSigningKeySets = ArrayUtils.appendLong(this.mSigningKeySets, ks);
    }

    protected void removeSigningKeySet(long ks) {
        this.mSigningKeySets = ArrayUtils.removeLong(this.mSigningKeySets, ks);
    }

    protected void addUpgradeKeySet(String alias) {
        Long ks = this.mKeySetAliases.get(alias);
        if (ks != null) {
            this.mUpgradeKeySets = ArrayUtils.appendLong(this.mUpgradeKeySets, ks.longValue());
            return;
        }
        throw new IllegalArgumentException("Upgrade keyset alias " + alias + "does not refer to a defined keyset alias!");
    }

    protected void addUpgradeKeySetById(long ks) {
        this.mSigningKeySets = ArrayUtils.appendLong(this.mSigningKeySets, ks);
    }

    protected void addDefinedKeySet(long ks, String alias) {
        this.mDefinedKeySets = ArrayUtils.appendLong(this.mDefinedKeySets, ks);
        this.mKeySetAliases.put(alias, Long.valueOf(ks));
    }

    protected void removeAllSigningKeySets() {
        this.mProperSigningKeySet = -1L;
        this.mSigningKeySets = null;
    }

    protected void removeAllUpgradeKeySets() {
        this.mUpgradeKeySets = null;
    }

    protected void removeAllDefinedKeySets() {
        this.mDefinedKeySets = null;
        this.mKeySetAliases.clear();
    }

    protected boolean packageIsSignedBy(long ks) {
        return ArrayUtils.contains(this.mSigningKeySets, ks);
    }

    protected long[] getSigningKeySets() {
        return this.mSigningKeySets;
    }

    protected long[] getUpgradeKeySets() {
        return this.mUpgradeKeySets;
    }

    protected long[] getDefinedKeySets() {
        return this.mDefinedKeySets;
    }

    protected ArrayMap<String, Long> getAliases() {
        return this.mKeySetAliases;
    }

    protected boolean isUsingDefinedKeySets() {
        return this.mDefinedKeySets != null && this.mDefinedKeySets.length > 0;
    }

    protected boolean isUsingUpgradeKeySets() {
        return this.mUpgradeKeySets != null && this.mUpgradeKeySets.length > 0;
    }
}
