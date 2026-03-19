package android.icu.impl.coll;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Trie2_32;
import android.icu.impl.coll.SharedObject;
import android.icu.text.UnicodeSet;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.VersionInfo;
import java.util.Map;

public final class CollationTailoring {
    static final boolean $assertionsDisabled = false;
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

    CollationTailoring(SharedObject.Reference<CollationSettings> reference) {
        if (reference != null) {
            this.settings = reference.m2clone();
        } else {
            this.settings = new SharedObject.Reference<>(new CollationSettings());
        }
    }

    void ensureOwnedData() {
        if (this.ownedData == null) {
            this.ownedData = new CollationData(Norm2AllModes.getNFCInstance().impl);
        }
        this.data = this.ownedData;
    }

    void setRules(String str) {
        this.rules = str;
    }

    void setRulesResource(UResourceBundle uResourceBundle) {
        this.rulesResource = uResourceBundle;
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

    static VersionInfo makeBaseVersion(VersionInfo versionInfo) {
        return VersionInfo.getInstance(VersionInfo.UCOL_BUILDER_VERSION.getMajor(), (versionInfo.getMajor() << 3) + versionInfo.getMinor(), versionInfo.getMilli() << 6, 0);
    }

    void setVersion(int i, int i2) {
        int i3 = i2 >> 16;
        int i4 = 65280 & i3;
        int i5 = i3 & 255;
        int i6 = (i2 >> 8) & 255;
        int i7 = i2 & 255;
        this.version = (i & 16760832) | (VersionInfo.UCOL_BUILDER_VERSION.getMajor() << 24) | ((i4 + (i4 >> 6)) & 16128) | (((i5 << 3) + (i5 >> 5) + i6 + (i7 << 4) + (i7 >> 4)) & 255);
    }

    int getUCAVersion() {
        return ((this.version >> 12) & 4080) | ((this.version >> 14) & 3);
    }
}
