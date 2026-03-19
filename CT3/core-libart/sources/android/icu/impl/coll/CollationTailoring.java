package android.icu.impl.coll;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2_32;
import android.icu.impl.coll.SharedObject;
import android.icu.text.UnicodeSet;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.VersionInfo;
import java.util.Map;

public final class CollationTailoring {

    static final boolean f51assertionsDisabled;
    public CollationData data;
    public Map<Integer, Integer> maxExpansions;
    CollationData ownedData;
    private String rules;
    private UResourceBundle rulesResource;
    public SharedObject.Reference<CollationSettings> settings;
    Trie2_32 trie;
    UnicodeSet unsafeBackwardSet;
    public ULocale actualLocale = ULocale.ROOT;
    public int version = 0;

    static {
        f51assertionsDisabled = !CollationTailoring.class.desiredAssertionStatus();
    }

    CollationTailoring(SharedObject.Reference<CollationSettings> baseSettings) {
        if (baseSettings != null) {
            if (!f51assertionsDisabled) {
                if (!(((CollationSettings) baseSettings.readOnly()).reorderCodes.length == 0)) {
                    throw new AssertionError();
                }
            }
            if (!f51assertionsDisabled) {
                if (!(((CollationSettings) baseSettings.readOnly()).reorderTable == null)) {
                    throw new AssertionError();
                }
            }
            if (!f51assertionsDisabled) {
                if (!(((CollationSettings) baseSettings.readOnly()).minHighNoReorder == 0)) {
                    throw new AssertionError();
                }
            }
            this.settings = baseSettings.m77clone();
            return;
        }
        this.settings = new SharedObject.Reference<>(new CollationSettings());
    }

    void ensureOwnedData() {
        if (this.ownedData == null) {
            Normalizer2Impl nfcImpl = Norm2AllModes.getNFCInstance().impl;
            this.ownedData = new CollationData(nfcImpl);
        }
        this.data = this.ownedData;
    }

    void setRules(String r) {
        boolean z = false;
        if (!f51assertionsDisabled) {
            if (this.rules == null && this.rulesResource == null) {
                z = true;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        this.rules = r;
    }

    void setRulesResource(UResourceBundle res) {
        boolean z = false;
        if (!f51assertionsDisabled) {
            if (this.rules == null && this.rulesResource == null) {
                z = true;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        this.rulesResource = res;
    }

    public String getRules() {
        if (this.rules != null) {
            return this.rules;
        }
        if (this.rulesResource != null) {
            return this.rulesResource.getString();
        }
        return "";
    }

    static VersionInfo makeBaseVersion(VersionInfo ucaVersion) {
        return VersionInfo.getInstance(VersionInfo.UCOL_BUILDER_VERSION.getMajor(), (ucaVersion.getMajor() << 3) + ucaVersion.getMinor(), ucaVersion.getMilli() << 6, 0);
    }

    void setVersion(int baseVersion, int rulesVersion) {
        int r = (rulesVersion >> 16) & Normalizer2Impl.JAMO_VT;
        int s = (rulesVersion >> 16) & 255;
        int t = (rulesVersion >> 8) & 255;
        int q = rulesVersion & 255;
        this.version = (VersionInfo.UCOL_BUILDER_VERSION.getMajor() << 24) | (16760832 & baseVersion) | (((r >> 6) + r) & 16128) | (((s << 3) + (s >> 5) + t + (q << 4) + (q >> 4)) & 255);
    }

    int getUCAVersion() {
        return ((this.version >> 12) & 4080) | ((this.version >> 14) & 3);
    }
}
