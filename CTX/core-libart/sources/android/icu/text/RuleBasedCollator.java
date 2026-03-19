package android.icu.text;

import android.icu.impl.ClassLoaderUtil;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Utility;
import android.icu.impl.coll.BOCSU;
import android.icu.impl.coll.Collation;
import android.icu.impl.coll.CollationCompare;
import android.icu.impl.coll.CollationData;
import android.icu.impl.coll.CollationFastLatin;
import android.icu.impl.coll.CollationIterator;
import android.icu.impl.coll.CollationKeys;
import android.icu.impl.coll.CollationLoader;
import android.icu.impl.coll.CollationRoot;
import android.icu.impl.coll.CollationSettings;
import android.icu.impl.coll.CollationTailoring;
import android.icu.impl.coll.ContractionsAndExpansions;
import android.icu.impl.coll.FCDUTF16CollationIterator;
import android.icu.impl.coll.SharedObject;
import android.icu.impl.coll.TailoredSet;
import android.icu.impl.coll.UTF16CollationIterator;
import android.icu.util.ULocale;
import android.icu.util.VersionInfo;
import java.lang.reflect.InvocationTargetException;
import java.text.CharacterIterator;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class RuleBasedCollator extends Collator {
    static final boolean $assertionsDisabled = false;
    private boolean actualLocaleIsSameAsValid;
    private CollationBuffer collationBuffer;
    CollationData data;
    private Lock frozenLock;
    SharedObject.Reference<CollationSettings> settings;
    CollationTailoring tailoring;
    private ULocale validLocale;

    public RuleBasedCollator(String str) throws Exception {
        if (str == null) {
            throw new IllegalArgumentException("Collation rules can not be null");
        }
        this.validLocale = ULocale.ROOT;
        internalBuildTailoring(str);
    }

    private final void internalBuildTailoring(String str) throws Exception {
        CollationTailoring root = CollationRoot.getRoot();
        try {
            Class<?> clsLoadClass = ClassLoaderUtil.getClassLoader(getClass()).loadClass("android.icu.impl.coll.CollationBuilder");
            CollationTailoring collationTailoring = (CollationTailoring) clsLoadClass.getMethod("parseAndBuild", String.class).invoke(clsLoadClass.getConstructor(CollationTailoring.class).newInstance(root), str);
            collationTailoring.actualLocale = null;
            adoptTailoring(collationTailoring);
        } catch (InvocationTargetException e) {
            throw ((Exception) e.getTargetException());
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    private final void initMaxExpansions() {
        synchronized (this.tailoring) {
            if (this.tailoring.maxExpansions == null) {
                this.tailoring.maxExpansions = CollationElementIterator.computeMaxExpansions(this.tailoring.data);
            }
        }
    }

    public CollationElementIterator getCollationElementIterator(String str) {
        initMaxExpansions();
        return new CollationElementIterator(str, this);
    }

    public CollationElementIterator getCollationElementIterator(CharacterIterator characterIterator) {
        initMaxExpansions();
        return new CollationElementIterator((CharacterIterator) characterIterator.clone(), this);
    }

    public CollationElementIterator getCollationElementIterator(UCharacterIterator uCharacterIterator) {
        initMaxExpansions();
        return new CollationElementIterator(uCharacterIterator, this);
    }

    @Override
    public boolean isFrozen() {
        return this.frozenLock != null;
    }

    @Override
    public Collator freeze() {
        if (!isFrozen()) {
            this.frozenLock = new ReentrantLock();
            if (this.collationBuffer == null) {
                this.collationBuffer = new CollationBuffer(this.data);
            }
        }
        return this;
    }

    @Override
    public RuleBasedCollator cloneAsThawed() {
        try {
            RuleBasedCollator ruleBasedCollator = (RuleBasedCollator) super.clone();
            ruleBasedCollator.settings = this.settings.m2clone();
            ruleBasedCollator.collationBuffer = null;
            ruleBasedCollator.frozenLock = null;
            return ruleBasedCollator;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    private void checkNotFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen RuleBasedCollator");
        }
    }

    private final CollationSettings getOwnedSettings() {
        return (CollationSettings) this.settings.copyOnWrite();
    }

    private final CollationSettings getDefaultSettings() {
        return (CollationSettings) this.tailoring.settings.readOnly();
    }

    @Deprecated
    public void setHiraganaQuaternary(boolean z) {
        checkNotFrozen();
    }

    @Deprecated
    public void setHiraganaQuaternaryDefault() {
        checkNotFrozen();
    }

    public void setUpperCaseFirst(boolean z) {
        checkNotFrozen();
        if (z == isUpperCaseFirst()) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setCaseFirst(z ? CollationSettings.CASE_FIRST_AND_UPPER_MASK : 0);
        setFastLatinOptions(ownedSettings);
    }

    public void setLowerCaseFirst(boolean z) {
        checkNotFrozen();
        if (z == isLowerCaseFirst()) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setCaseFirst(z ? 512 : 0);
        setFastLatinOptions(ownedSettings);
    }

    public final void setCaseFirstDefault() {
        checkNotFrozen();
        CollationSettings defaultSettings = getDefaultSettings();
        if (this.settings.readOnly() == defaultSettings) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setCaseFirstDefault(defaultSettings.options);
        setFastLatinOptions(ownedSettings);
    }

    public void setAlternateHandlingDefault() {
        checkNotFrozen();
        CollationSettings defaultSettings = getDefaultSettings();
        if (this.settings.readOnly() == defaultSettings) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setAlternateHandlingDefault(defaultSettings.options);
        setFastLatinOptions(ownedSettings);
    }

    public void setCaseLevelDefault() {
        checkNotFrozen();
        CollationSettings defaultSettings = getDefaultSettings();
        if (this.settings.readOnly() == defaultSettings) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setFlagDefault(1024, defaultSettings.options);
        setFastLatinOptions(ownedSettings);
    }

    public void setDecompositionDefault() {
        checkNotFrozen();
        CollationSettings defaultSettings = getDefaultSettings();
        if (this.settings.readOnly() == defaultSettings) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setFlagDefault(1, defaultSettings.options);
        setFastLatinOptions(ownedSettings);
    }

    public void setFrenchCollationDefault() {
        checkNotFrozen();
        CollationSettings defaultSettings = getDefaultSettings();
        if (this.settings.readOnly() == defaultSettings) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setFlagDefault(2048, defaultSettings.options);
        setFastLatinOptions(ownedSettings);
    }

    public void setStrengthDefault() {
        checkNotFrozen();
        CollationSettings defaultSettings = getDefaultSettings();
        if (this.settings.readOnly() == defaultSettings) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setStrengthDefault(defaultSettings.options);
        setFastLatinOptions(ownedSettings);
    }

    public void setNumericCollationDefault() {
        checkNotFrozen();
        CollationSettings defaultSettings = getDefaultSettings();
        if (this.settings.readOnly() == defaultSettings) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setFlagDefault(2, defaultSettings.options);
        setFastLatinOptions(ownedSettings);
    }

    public void setFrenchCollation(boolean z) {
        checkNotFrozen();
        if (z == isFrenchCollation()) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setFlag(2048, z);
        setFastLatinOptions(ownedSettings);
    }

    public void setAlternateHandlingShifted(boolean z) {
        checkNotFrozen();
        if (z == isAlternateHandlingShifted()) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setAlternateHandlingShifted(z);
        setFastLatinOptions(ownedSettings);
    }

    public void setCaseLevel(boolean z) {
        checkNotFrozen();
        if (z == isCaseLevel()) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setFlag(1024, z);
        setFastLatinOptions(ownedSettings);
    }

    @Override
    public void setDecomposition(int i) {
        boolean z;
        checkNotFrozen();
        switch (i) {
            case 16:
                z = false;
                break;
            case 17:
                z = true;
                break;
            default:
                throw new IllegalArgumentException("Wrong decomposition mode.");
        }
        if (z == ((CollationSettings) this.settings.readOnly()).getFlag(1)) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setFlag(1, z);
        setFastLatinOptions(ownedSettings);
    }

    @Override
    public void setStrength(int i) {
        checkNotFrozen();
        if (i == getStrength()) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setStrength(i);
        setFastLatinOptions(ownedSettings);
    }

    @Override
    public RuleBasedCollator setMaxVariable(int i) {
        int i2;
        if (i != -1) {
            if (4096 <= i && i <= 4099) {
                i2 = i - 4096;
            } else {
                throw new IllegalArgumentException("illegal max variable group " + i);
            }
        } else {
            i2 = -1;
        }
        if (i2 == ((CollationSettings) this.settings.readOnly()).getMaxVariable()) {
            return this;
        }
        CollationSettings defaultSettings = getDefaultSettings();
        if (this.settings.readOnly() == defaultSettings && i2 < 0) {
            return this;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        if (i == -1) {
            i = defaultSettings.getMaxVariable() + 4096;
        }
        long lastPrimaryForGroup = this.data.getLastPrimaryForGroup(i);
        ownedSettings.setMaxVariable(i2, defaultSettings.options);
        ownedSettings.variableTop = lastPrimaryForGroup;
        setFastLatinOptions(ownedSettings);
        return this;
    }

    @Override
    public int getMaxVariable() {
        return 4096 + ((CollationSettings) this.settings.readOnly()).getMaxVariable();
    }

    @Override
    @Deprecated
    public int setVariableTop(String str) {
        long jNextCE;
        long jNextCE2;
        checkNotFrozen();
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("Variable top argument string can not be null or zero in length.");
        }
        boolean zIsNumeric = ((CollationSettings) this.settings.readOnly()).isNumeric();
        if (((CollationSettings) this.settings.readOnly()).dontCheckFCD()) {
            UTF16CollationIterator uTF16CollationIterator = new UTF16CollationIterator(this.data, zIsNumeric, str, 0);
            jNextCE = uTF16CollationIterator.nextCE();
            jNextCE2 = uTF16CollationIterator.nextCE();
        } else {
            FCDUTF16CollationIterator fCDUTF16CollationIterator = new FCDUTF16CollationIterator(this.data, zIsNumeric, str, 0);
            jNextCE = fCDUTF16CollationIterator.nextCE();
            jNextCE2 = fCDUTF16CollationIterator.nextCE();
        }
        if (jNextCE == Collation.NO_CE || jNextCE2 != Collation.NO_CE) {
            throw new IllegalArgumentException("Variable top argument string must map to exactly one collation element");
        }
        internalSetVariableTop(jNextCE >>> 32);
        return (int) ((CollationSettings) this.settings.readOnly()).variableTop;
    }

    @Override
    @Deprecated
    public void setVariableTop(int i) {
        checkNotFrozen();
        internalSetVariableTop(((long) i) & 4294967295L);
    }

    private void internalSetVariableTop(long j) {
        if (j != ((CollationSettings) this.settings.readOnly()).variableTop) {
            int groupForPrimary = this.data.getGroupForPrimary(j);
            if (groupForPrimary < 4096 || 4099 < groupForPrimary) {
                throw new IllegalArgumentException("The variable top must be a primary weight in the space/punctuation/symbols/currency symbols range");
            }
            long lastPrimaryForGroup = this.data.getLastPrimaryForGroup(groupForPrimary);
            if (lastPrimaryForGroup != ((CollationSettings) this.settings.readOnly()).variableTop) {
                CollationSettings ownedSettings = getOwnedSettings();
                ownedSettings.setMaxVariable(groupForPrimary - 4096, getDefaultSettings().options);
                ownedSettings.variableTop = lastPrimaryForGroup;
                setFastLatinOptions(ownedSettings);
            }
        }
    }

    public void setNumericCollation(boolean z) {
        checkNotFrozen();
        if (z == getNumericCollation()) {
            return;
        }
        CollationSettings ownedSettings = getOwnedSettings();
        ownedSettings.setFlag(2, z);
        setFastLatinOptions(ownedSettings);
    }

    @Override
    public void setReorderCodes(int... iArr) {
        checkNotFrozen();
        int length = iArr != null ? iArr.length : 0;
        if (length == 1 && iArr[0] == 103) {
            length = 0;
        }
        if (length == 0) {
            if (((CollationSettings) this.settings.readOnly()).reorderCodes.length == 0) {
                return;
            }
        } else if (Arrays.equals(iArr, ((CollationSettings) this.settings.readOnly()).reorderCodes)) {
            return;
        }
        CollationSettings defaultSettings = getDefaultSettings();
        if (length == 1 && iArr[0] == -1) {
            if (this.settings.readOnly() != defaultSettings) {
                CollationSettings ownedSettings = getOwnedSettings();
                ownedSettings.copyReorderingFrom(defaultSettings);
                setFastLatinOptions(ownedSettings);
                return;
            }
            return;
        }
        CollationSettings ownedSettings2 = getOwnedSettings();
        if (length == 0) {
            ownedSettings2.resetReordering();
        } else {
            ownedSettings2.setReordering(this.data, (int[]) iArr.clone());
        }
        setFastLatinOptions(ownedSettings2);
    }

    private void setFastLatinOptions(CollationSettings collationSettings) {
        collationSettings.fastLatinOptions = CollationFastLatin.getOptions(this.data, collationSettings, collationSettings.fastLatinPrimaries);
    }

    public String getRules() {
        return this.tailoring.getRules();
    }

    public String getRules(boolean z) {
        if (!z) {
            return this.tailoring.getRules();
        }
        return CollationLoader.getRootRules() + this.tailoring.getRules();
    }

    @Override
    public UnicodeSet getTailoredSet() {
        UnicodeSet unicodeSet = new UnicodeSet();
        if (this.data.base != null) {
            new TailoredSet(unicodeSet).forData(this.data);
        }
        return unicodeSet;
    }

    public void getContractionsAndExpansions(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, boolean z) throws Exception {
        if (unicodeSet != null) {
            unicodeSet.clear();
        }
        if (unicodeSet2 != null) {
            unicodeSet2.clear();
        }
        new ContractionsAndExpansions(unicodeSet, unicodeSet2, null, z).forData(this.data);
    }

    void internalAddContractions(int i, UnicodeSet unicodeSet) {
        new ContractionsAndExpansions(unicodeSet, null, null, false).forCodePoint(this.data, i);
    }

    @Override
    public CollationKey getCollationKey(String str) throws Throwable {
        CollationBuffer collationBuffer = null;
        if (str == null) {
            return null;
        }
        try {
            CollationBuffer collationBuffer2 = getCollationBuffer();
            try {
                CollationKey collationKey = getCollationKey(str, collationBuffer2);
                releaseCollationBuffer(collationBuffer2);
                return collationKey;
            } catch (Throwable th) {
                th = th;
                collationBuffer = collationBuffer2;
                releaseCollationBuffer(collationBuffer);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private CollationKey getCollationKey(String str, CollationBuffer collationBuffer) {
        collationBuffer.rawCollationKey = getRawCollationKey(str, collationBuffer.rawCollationKey, collationBuffer);
        return new CollationKey(str, collationBuffer.rawCollationKey);
    }

    @Override
    public RawCollationKey getRawCollationKey(String str, RawCollationKey rawCollationKey) throws Throwable {
        CollationBuffer collationBuffer = null;
        if (str == null) {
            return null;
        }
        try {
            CollationBuffer collationBuffer2 = getCollationBuffer();
            try {
                RawCollationKey rawCollationKey2 = getRawCollationKey(str, rawCollationKey, collationBuffer2);
                releaseCollationBuffer(collationBuffer2);
                return rawCollationKey2;
            } catch (Throwable th) {
                th = th;
                collationBuffer = collationBuffer2;
                releaseCollationBuffer(collationBuffer);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static final class CollationKeyByteSink extends CollationKeys.SortKeyByteSink {
        private RawCollationKey key_;

        CollationKeyByteSink(RawCollationKey rawCollationKey) {
            super(rawCollationKey.bytes);
            this.key_ = rawCollationKey;
        }

        @Override
        protected void AppendBeyondCapacity(byte[] bArr, int i, int i2, int i3) {
            if (Resize(i2, i3)) {
                System.arraycopy(bArr, i, this.buffer_, i3, i2);
            }
        }

        @Override
        protected boolean Resize(int i, int i2) {
            int length = this.buffer_.length * 2;
            int i3 = i2 + (2 * i);
            if (length >= i3) {
                i3 = length;
            }
            if (i3 < 200) {
                i3 = 200;
            }
            byte[] bArr = new byte[i3];
            System.arraycopy(this.buffer_, 0, bArr, 0, i2);
            this.key_.bytes = bArr;
            this.buffer_ = bArr;
            return true;
        }
    }

    private RawCollationKey getRawCollationKey(CharSequence charSequence, RawCollationKey rawCollationKey, CollationBuffer collationBuffer) {
        if (rawCollationKey == null) {
            rawCollationKey = new RawCollationKey(simpleKeyLengthEstimate(charSequence));
        } else if (rawCollationKey.bytes == null) {
            rawCollationKey.bytes = new byte[simpleKeyLengthEstimate(charSequence)];
        }
        CollationKeyByteSink collationKeyByteSink = new CollationKeyByteSink(rawCollationKey);
        writeSortKey(charSequence, collationKeyByteSink, collationBuffer);
        rawCollationKey.size = collationKeyByteSink.NumberOfBytesAppended();
        return rawCollationKey;
    }

    private int simpleKeyLengthEstimate(CharSequence charSequence) {
        return (2 * charSequence.length()) + 10;
    }

    private void writeSortKey(CharSequence charSequence, CollationKeyByteSink collationKeyByteSink, CollationBuffer collationBuffer) {
        boolean zIsNumeric = ((CollationSettings) this.settings.readOnly()).isNumeric();
        if (((CollationSettings) this.settings.readOnly()).dontCheckFCD()) {
            collationBuffer.leftUTF16CollIter.setText(zIsNumeric, charSequence, 0);
            CollationKeys.writeSortKeyUpToQuaternary(collationBuffer.leftUTF16CollIter, this.data.compressibleBytes, (CollationSettings) this.settings.readOnly(), collationKeyByteSink, 1, CollationKeys.SIMPLE_LEVEL_FALLBACK, true);
        } else {
            collationBuffer.leftFCDUTF16Iter.setText(zIsNumeric, charSequence, 0);
            CollationKeys.writeSortKeyUpToQuaternary(collationBuffer.leftFCDUTF16Iter, this.data.compressibleBytes, (CollationSettings) this.settings.readOnly(), collationKeyByteSink, 1, CollationKeys.SIMPLE_LEVEL_FALLBACK, true);
        }
        if (((CollationSettings) this.settings.readOnly()).getStrength() == 15) {
            writeIdenticalLevel(charSequence, collationKeyByteSink);
        }
        collationKeyByteSink.Append(0);
    }

    private void writeIdenticalLevel(CharSequence charSequence, CollationKeyByteSink collationKeyByteSink) {
        int iWriteIdenticalLevelRun;
        int iDecompose = this.data.nfcImpl.decompose(charSequence, 0, charSequence.length(), null);
        collationKeyByteSink.Append(1);
        collationKeyByteSink.key_.size = collationKeyByteSink.NumberOfBytesAppended();
        if (iDecompose != 0) {
            iWriteIdenticalLevelRun = BOCSU.writeIdenticalLevelRun(0, charSequence, 0, iDecompose, collationKeyByteSink.key_);
        } else {
            iWriteIdenticalLevelRun = 0;
        }
        if (iDecompose < charSequence.length()) {
            int length = charSequence.length() - iDecompose;
            StringBuilder sb = new StringBuilder();
            this.data.nfcImpl.decompose(charSequence, iDecompose, charSequence.length(), sb, length);
            BOCSU.writeIdenticalLevelRun(iWriteIdenticalLevelRun, sb, 0, sb.length(), collationKeyByteSink.key_);
        }
        collationKeyByteSink.setBufferAndAppended(collationKeyByteSink.key_.bytes, collationKeyByteSink.key_.size);
    }

    @Deprecated
    public long[] internalGetCEs(CharSequence charSequence) throws Throwable {
        CollationBuffer collationBuffer;
        CollationIterator collationIterator;
        try {
            collationBuffer = getCollationBuffer();
            try {
                boolean zIsNumeric = ((CollationSettings) this.settings.readOnly()).isNumeric();
                if (((CollationSettings) this.settings.readOnly()).dontCheckFCD()) {
                    collationBuffer.leftUTF16CollIter.setText(zIsNumeric, charSequence, 0);
                    collationIterator = collationBuffer.leftUTF16CollIter;
                } else {
                    collationBuffer.leftFCDUTF16Iter.setText(zIsNumeric, charSequence, 0);
                    collationIterator = collationBuffer.leftFCDUTF16Iter;
                }
                int iFetchCEs = collationIterator.fetchCEs() - 1;
                long[] jArr = new long[iFetchCEs];
                System.arraycopy(collationIterator.getCEs(), 0, jArr, 0, iFetchCEs);
                releaseCollationBuffer(collationBuffer);
                return jArr;
            } catch (Throwable th) {
                th = th;
                releaseCollationBuffer(collationBuffer);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            collationBuffer = null;
        }
    }

    @Override
    public int getStrength() {
        return ((CollationSettings) this.settings.readOnly()).getStrength();
    }

    @Override
    public int getDecomposition() {
        return (((CollationSettings) this.settings.readOnly()).options & 1) != 0 ? 17 : 16;
    }

    public boolean isUpperCaseFirst() {
        return ((CollationSettings) this.settings.readOnly()).getCaseFirst() == 768;
    }

    public boolean isLowerCaseFirst() {
        return ((CollationSettings) this.settings.readOnly()).getCaseFirst() == 512;
    }

    public boolean isAlternateHandlingShifted() {
        return ((CollationSettings) this.settings.readOnly()).getAlternateHandling();
    }

    public boolean isCaseLevel() {
        return (((CollationSettings) this.settings.readOnly()).options & 1024) != 0;
    }

    public boolean isFrenchCollation() {
        return (((CollationSettings) this.settings.readOnly()).options & 2048) != 0;
    }

    @Deprecated
    public boolean isHiraganaQuaternary() {
        return false;
    }

    @Override
    public int getVariableTop() {
        return (int) ((CollationSettings) this.settings.readOnly()).variableTop;
    }

    public boolean getNumericCollation() {
        return (((CollationSettings) this.settings.readOnly()).options & 2) != 0;
    }

    @Override
    public int[] getReorderCodes() {
        return (int[]) ((CollationSettings) this.settings.readOnly()).reorderCodes.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        RuleBasedCollator ruleBasedCollator = (RuleBasedCollator) obj;
        if (!((CollationSettings) this.settings.readOnly()).equals(ruleBasedCollator.settings.readOnly())) {
            return false;
        }
        if (this.data == ruleBasedCollator.data) {
            return true;
        }
        boolean z = this.data.base == null;
        boolean z2 = ruleBasedCollator.data.base == null;
        if (z != z2) {
            return false;
        }
        String rules = this.tailoring.getRules();
        String rules2 = ruleBasedCollator.tailoring.getRules();
        return ((z || rules.length() != 0) && ((z2 || rules2.length() != 0) && rules.equals(rules2))) || getTailoredSet().equals(ruleBasedCollator.getTailoredSet());
    }

    @Override
    public int hashCode() {
        int iHashCode = ((CollationSettings) this.settings.readOnly()).hashCode();
        if (this.data.base == null) {
            return iHashCode;
        }
        UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(getTailoredSet());
        while (unicodeSetIterator.next() && unicodeSetIterator.codepoint != UnicodeSetIterator.IS_STRING) {
            iHashCode ^= this.data.getCE32(unicodeSetIterator.codepoint);
        }
        return iHashCode;
    }

    @Override
    public int compare(String str, String str2) {
        return doCompare(str, str2);
    }

    private static abstract class NFDIterator {
        private String decomp;
        private int index;

        protected abstract int nextRawCodePoint();

        NFDIterator() {
        }

        final void reset() {
            this.index = -1;
        }

        final int nextCodePoint() {
            if (this.index >= 0) {
                if (this.index == this.decomp.length()) {
                    this.index = -1;
                } else {
                    int iCodePointAt = Character.codePointAt(this.decomp, this.index);
                    this.index += Character.charCount(iCodePointAt);
                    return iCodePointAt;
                }
            }
            return nextRawCodePoint();
        }

        final int nextDecomposedCodePoint(Normalizer2Impl normalizer2Impl, int i) {
            if (this.index >= 0) {
                return i;
            }
            this.decomp = normalizer2Impl.getDecomposition(i);
            if (this.decomp == null) {
                return i;
            }
            int iCodePointAt = Character.codePointAt(this.decomp, 0);
            this.index = Character.charCount(iCodePointAt);
            return iCodePointAt;
        }
    }

    private static class UTF16NFDIterator extends NFDIterator {
        protected int pos;
        protected CharSequence s;

        UTF16NFDIterator() {
        }

        void setText(CharSequence charSequence, int i) {
            reset();
            this.s = charSequence;
            this.pos = i;
        }

        @Override
        protected int nextRawCodePoint() {
            if (this.pos == this.s.length()) {
                return -1;
            }
            int iCodePointAt = Character.codePointAt(this.s, this.pos);
            this.pos += Character.charCount(iCodePointAt);
            return iCodePointAt;
        }
    }

    private static final class FCDUTF16NFDIterator extends UTF16NFDIterator {
        private StringBuilder str;

        FCDUTF16NFDIterator() {
        }

        void setText(Normalizer2Impl normalizer2Impl, CharSequence charSequence, int i) {
            reset();
            int iMakeFCD = normalizer2Impl.makeFCD(charSequence, i, charSequence.length(), null);
            if (iMakeFCD == charSequence.length()) {
                this.s = charSequence;
                this.pos = i;
                return;
            }
            if (this.str == null) {
                this.str = new StringBuilder();
            } else {
                this.str.setLength(0);
            }
            this.str.append(charSequence, i, iMakeFCD);
            normalizer2Impl.makeFCD(charSequence, iMakeFCD, charSequence.length(), new Normalizer2Impl.ReorderingBuffer(normalizer2Impl, this.str, charSequence.length() - i));
            this.s = this.str;
            this.pos = 0;
        }
    }

    private static final int compareNFDIter(Normalizer2Impl normalizer2Impl, NFDIterator nFDIterator, NFDIterator nFDIterator2) {
        while (true) {
            int iNextCodePoint = nFDIterator.nextCodePoint();
            int iNextCodePoint2 = nFDIterator2.nextCodePoint();
            if (iNextCodePoint == iNextCodePoint2) {
                if (iNextCodePoint < 0) {
                    return 0;
                }
            } else {
                int iNextDecomposedCodePoint = -2;
                int iNextDecomposedCodePoint2 = iNextCodePoint < 0 ? -2 : iNextCodePoint == 65534 ? -1 : nFDIterator.nextDecomposedCodePoint(normalizer2Impl, iNextCodePoint);
                if (iNextCodePoint2 >= 0) {
                    if (iNextCodePoint2 != 65534) {
                        iNextDecomposedCodePoint = nFDIterator2.nextDecomposedCodePoint(normalizer2Impl, iNextCodePoint2);
                    } else {
                        iNextDecomposedCodePoint = -1;
                    }
                }
                if (iNextDecomposedCodePoint2 < iNextDecomposedCodePoint) {
                    return -1;
                }
                if (iNextDecomposedCodePoint2 > iNextDecomposedCodePoint) {
                    return 1;
                }
            }
        }
    }

    @Override
    @Deprecated
    protected int doCompare(CharSequence charSequence, CharSequence charSequence2) throws Throwable {
        int iCompareUTF16;
        CollationBuffer collationBuffer;
        int iCompareUpToQuaternary;
        CollationBuffer collationBuffer2;
        if (charSequence == charSequence2) {
            return 0;
        }
        int i = 0;
        while (true) {
            if (i == charSequence.length()) {
                if (i == charSequence2.length()) {
                    return 0;
                }
            } else {
                if (i == charSequence2.length() || charSequence.charAt(i) != charSequence2.charAt(i)) {
                    break;
                }
                i++;
            }
        }
        CollationSettings collationSettings = (CollationSettings) this.settings.readOnly();
        boolean zIsNumeric = collationSettings.isNumeric();
        if (i > 0 && ((i != charSequence.length() && this.data.isUnsafeBackward(charSequence.charAt(i), zIsNumeric)) || (i != charSequence2.length() && this.data.isUnsafeBackward(charSequence2.charAt(i), zIsNumeric)))) {
            do {
                i--;
                if (i <= 0) {
                    break;
                }
            } while (this.data.isUnsafeBackward(charSequence.charAt(i), zIsNumeric));
        }
        int i2 = collationSettings.fastLatinOptions;
        if (i2 >= 0 && ((i == charSequence.length() || charSequence.charAt(i) <= 383) && (i == charSequence2.length() || charSequence2.charAt(i) <= 383))) {
            iCompareUTF16 = CollationFastLatin.compareUTF16(this.data.fastLatinTable, collationSettings.fastLatinPrimaries, i2, charSequence, charSequence2, i);
        } else {
            iCompareUTF16 = -2;
        }
        if (iCompareUTF16 == -2) {
            try {
                collationBuffer = getCollationBuffer();
                try {
                    if (collationSettings.dontCheckFCD()) {
                        collationBuffer.leftUTF16CollIter.setText(zIsNumeric, charSequence, i);
                        collationBuffer.rightUTF16CollIter.setText(zIsNumeric, charSequence2, i);
                        iCompareUpToQuaternary = CollationCompare.compareUpToQuaternary(collationBuffer.leftUTF16CollIter, collationBuffer.rightUTF16CollIter, collationSettings);
                    } else {
                        collationBuffer.leftFCDUTF16Iter.setText(zIsNumeric, charSequence, i);
                        collationBuffer.rightFCDUTF16Iter.setText(zIsNumeric, charSequence2, i);
                        iCompareUpToQuaternary = CollationCompare.compareUpToQuaternary(collationBuffer.leftFCDUTF16Iter, collationBuffer.rightFCDUTF16Iter, collationSettings);
                    }
                    iCompareUTF16 = iCompareUpToQuaternary;
                    releaseCollationBuffer(collationBuffer);
                } catch (Throwable th) {
                    th = th;
                    releaseCollationBuffer(collationBuffer);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                collationBuffer = null;
            }
        }
        if (iCompareUTF16 != 0 || collationSettings.getStrength() < 15) {
            return iCompareUTF16;
        }
        try {
            collationBuffer2 = getCollationBuffer();
            try {
                Normalizer2Impl normalizer2Impl = this.data.nfcImpl;
                if (collationSettings.dontCheckFCD()) {
                    collationBuffer2.leftUTF16NFDIter.setText(charSequence, i);
                    collationBuffer2.rightUTF16NFDIter.setText(charSequence2, i);
                    int iCompareNFDIter = compareNFDIter(normalizer2Impl, collationBuffer2.leftUTF16NFDIter, collationBuffer2.rightUTF16NFDIter);
                    releaseCollationBuffer(collationBuffer2);
                    return iCompareNFDIter;
                }
                collationBuffer2.leftFCDUTF16NFDIter.setText(normalizer2Impl, charSequence, i);
                collationBuffer2.rightFCDUTF16NFDIter.setText(normalizer2Impl, charSequence2, i);
                int iCompareNFDIter2 = compareNFDIter(normalizer2Impl, collationBuffer2.leftFCDUTF16NFDIter, collationBuffer2.rightFCDUTF16NFDIter);
                releaseCollationBuffer(collationBuffer2);
                return iCompareNFDIter2;
            } catch (Throwable th3) {
                th = th3;
                releaseCollationBuffer(collationBuffer2);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            collationBuffer2 = null;
        }
    }

    RuleBasedCollator(CollationTailoring collationTailoring, ULocale uLocale) {
        this.data = collationTailoring.data;
        this.settings = collationTailoring.settings.m2clone();
        this.tailoring = collationTailoring;
        this.validLocale = uLocale;
        this.actualLocaleIsSameAsValid = false;
    }

    private void adoptTailoring(CollationTailoring collationTailoring) {
        this.data = collationTailoring.data;
        this.settings = collationTailoring.settings.m2clone();
        this.tailoring = collationTailoring;
        this.validLocale = collationTailoring.actualLocale;
        this.actualLocaleIsSameAsValid = false;
    }

    final boolean isUnsafe(int i) {
        return this.data.isUnsafeBackward(i, ((CollationSettings) this.settings.readOnly()).isNumeric());
    }

    private static final class CollationBuffer {
        FCDUTF16CollationIterator leftFCDUTF16Iter;
        FCDUTF16NFDIterator leftFCDUTF16NFDIter;
        UTF16CollationIterator leftUTF16CollIter;
        UTF16NFDIterator leftUTF16NFDIter;
        RawCollationKey rawCollationKey;
        FCDUTF16CollationIterator rightFCDUTF16Iter;
        FCDUTF16NFDIterator rightFCDUTF16NFDIter;
        UTF16CollationIterator rightUTF16CollIter;
        UTF16NFDIterator rightUTF16NFDIter;

        private CollationBuffer(CollationData collationData) {
            this.leftUTF16CollIter = new UTF16CollationIterator(collationData);
            this.rightUTF16CollIter = new UTF16CollationIterator(collationData);
            this.leftFCDUTF16Iter = new FCDUTF16CollationIterator(collationData);
            this.rightFCDUTF16Iter = new FCDUTF16CollationIterator(collationData);
            this.leftUTF16NFDIter = new UTF16NFDIterator();
            this.rightUTF16NFDIter = new UTF16NFDIterator();
            this.leftFCDUTF16NFDIter = new FCDUTF16NFDIterator();
            this.rightFCDUTF16NFDIter = new FCDUTF16NFDIterator();
        }
    }

    @Override
    public VersionInfo getVersion() {
        int i = this.tailoring.version;
        int major = VersionInfo.UCOL_RUNTIME_VERSION.getMajor();
        return VersionInfo.getInstance((i >>> 24) + (major << 4) + (major >> 4), (i >> 16) & 255, (i >> 8) & 255, i & 255);
    }

    @Override
    public VersionInfo getUCAVersion() {
        VersionInfo version = getVersion();
        return VersionInfo.getInstance(version.getMinor() >> 3, version.getMinor() & 7, version.getMilli() >> 6, 0);
    }

    private final CollationBuffer getCollationBuffer() {
        if (isFrozen()) {
            this.frozenLock.lock();
        } else if (this.collationBuffer == null) {
            this.collationBuffer = new CollationBuffer(this.data);
        }
        return this.collationBuffer;
    }

    private final void releaseCollationBuffer(CollationBuffer collationBuffer) {
        if (isFrozen()) {
            this.frozenLock.unlock();
        }
    }

    @Override
    public ULocale getLocale(ULocale.Type type) {
        if (type == ULocale.ACTUAL_LOCALE) {
            return this.actualLocaleIsSameAsValid ? this.validLocale : this.tailoring.actualLocale;
        }
        if (type == ULocale.VALID_LOCALE) {
            return this.validLocale;
        }
        throw new IllegalArgumentException("unknown ULocale.Type " + type);
    }

    @Override
    void setLocale(ULocale uLocale, ULocale uLocale2) {
        if (Utility.objectEquals(uLocale2, this.tailoring.actualLocale)) {
            this.actualLocaleIsSameAsValid = false;
        } else {
            this.actualLocaleIsSameAsValid = true;
        }
        this.validLocale = uLocale;
    }
}
