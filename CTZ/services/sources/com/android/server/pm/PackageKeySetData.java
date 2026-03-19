package com.android.server.pm;

import android.util.ArrayMap;
import com.android.internal.util.ArrayUtils;

public class PackageKeySetData {
    static final long KEYSET_UNASSIGNED = -1;
    private final ArrayMap<String, Long> mKeySetAliases;
    private long mProperSigningKeySet;
    private long[] mUpgradeKeySets;

    PackageKeySetData() {
        this.mKeySetAliases = new ArrayMap<>();
        this.mProperSigningKeySet = -1L;
    }

    PackageKeySetData(PackageKeySetData packageKeySetData) {
        this.mKeySetAliases = new ArrayMap<>();
        this.mProperSigningKeySet = packageKeySetData.mProperSigningKeySet;
        this.mUpgradeKeySets = ArrayUtils.cloneOrNull(packageKeySetData.mUpgradeKeySets);
        this.mKeySetAliases.putAll((ArrayMap<? extends String, ? extends Long>) packageKeySetData.mKeySetAliases);
    }

    protected void setProperSigningKeySet(long j) {
        this.mProperSigningKeySet = j;
    }

    protected long getProperSigningKeySet() {
        return this.mProperSigningKeySet;
    }

    protected void addUpgradeKeySet(String str) {
        if (str == null) {
            return;
        }
        Long l = this.mKeySetAliases.get(str);
        if (l != null) {
            this.mUpgradeKeySets = ArrayUtils.appendLong(this.mUpgradeKeySets, l.longValue());
            return;
        }
        throw new IllegalArgumentException("Upgrade keyset alias " + str + "does not refer to a defined keyset alias!");
    }

    protected void addUpgradeKeySetById(long j) {
        this.mUpgradeKeySets = ArrayUtils.appendLong(this.mUpgradeKeySets, j);
    }

    protected void removeAllUpgradeKeySets() {
        this.mUpgradeKeySets = null;
    }

    protected long[] getUpgradeKeySets() {
        return this.mUpgradeKeySets;
    }

    protected ArrayMap<String, Long> getAliases() {
        return this.mKeySetAliases;
    }

    protected void setAliases(ArrayMap<String, Long> arrayMap) {
        removeAllDefinedKeySets();
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            this.mKeySetAliases.put(arrayMap.keyAt(i), arrayMap.valueAt(i));
        }
    }

    protected void addDefinedKeySet(long j, String str) {
        this.mKeySetAliases.put(str, Long.valueOf(j));
    }

    protected void removeAllDefinedKeySets() {
        int size = this.mKeySetAliases.size();
        for (int i = 0; i < size; i++) {
            this.mKeySetAliases.removeAt(i);
        }
    }

    protected boolean isUsingDefinedKeySets() {
        return this.mKeySetAliases.size() > 0;
    }

    protected boolean isUsingUpgradeKeySets() {
        return this.mUpgradeKeySets != null && this.mUpgradeKeySets.length > 0;
    }
}
