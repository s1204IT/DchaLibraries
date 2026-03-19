package com.android.server.pm;

import android.content.pm.PackageParser;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupManagerConstants;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class KeySetManagerService {
    public static final int CURRENT_VERSION = 1;
    public static final int FIRST_VERSION = 1;
    public static final long KEYSET_NOT_FOUND = -1;
    protected static final long PUBLIC_KEY_NOT_FOUND = -1;
    static final String TAG = "KeySetManagerService";
    private final ArrayMap<String, PackageSetting> mPackages;
    private long lastIssuedKeySetId = 0;
    private long lastIssuedKeyId = 0;
    private final LongSparseArray<KeySetHandle> mKeySets = new LongSparseArray<>();
    private final LongSparseArray<PublicKeyHandle> mPublicKeys = new LongSparseArray<>();
    protected final LongSparseArray<ArraySet<Long>> mKeySetMapping = new LongSparseArray<>();

    class PublicKeyHandle {
        private final long mId;
        private final PublicKey mKey;
        private int mRefCount;

        public PublicKeyHandle(long j, PublicKey publicKey) {
            this.mId = j;
            this.mRefCount = 1;
            this.mKey = publicKey;
        }

        private PublicKeyHandle(long j, int i, PublicKey publicKey) {
            this.mId = j;
            this.mRefCount = i;
            this.mKey = publicKey;
        }

        public long getId() {
            return this.mId;
        }

        public PublicKey getKey() {
            return this.mKey;
        }

        public int getRefCountLPr() {
            return this.mRefCount;
        }

        public void incrRefCountLPw() {
            this.mRefCount++;
        }

        public long decrRefCountLPw() {
            this.mRefCount--;
            return this.mRefCount;
        }
    }

    public KeySetManagerService(ArrayMap<String, PackageSetting> arrayMap) {
        this.mPackages = arrayMap;
    }

    public boolean packageIsSignedByLPr(String str, KeySetHandle keySetHandle) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            throw new NullPointerException("Invalid package name");
        }
        if (packageSetting.keySetData == null) {
            throw new NullPointerException("Package has no KeySet data");
        }
        long idByKeySetLPr = getIdByKeySetLPr(keySetHandle);
        if (idByKeySetLPr == -1) {
            return false;
        }
        return this.mKeySetMapping.get(packageSetting.keySetData.getProperSigningKeySet()).containsAll(this.mKeySetMapping.get(idByKeySetLPr));
    }

    public boolean packageIsSignedByExactlyLPr(String str, KeySetHandle keySetHandle) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null) {
            throw new NullPointerException("Invalid package name");
        }
        if (packageSetting.keySetData == null || packageSetting.keySetData.getProperSigningKeySet() == -1) {
            throw new NullPointerException("Package has no KeySet data");
        }
        long idByKeySetLPr = getIdByKeySetLPr(keySetHandle);
        if (idByKeySetLPr == -1) {
            return false;
        }
        return this.mKeySetMapping.get(packageSetting.keySetData.getProperSigningKeySet()).equals(this.mKeySetMapping.get(idByKeySetLPr));
    }

    public void assertScannedPackageValid(PackageParser.Package r7) throws PackageManagerException {
        if (r7 == null || r7.packageName == null) {
            throw new PackageManagerException(-2, "Passed invalid package to keyset validation.");
        }
        ArraySet arraySet = r7.mSigningDetails.publicKeys;
        if (arraySet == null || arraySet.size() <= 0 || arraySet.contains(null)) {
            throw new PackageManagerException(-2, "Package has invalid signing-key-set.");
        }
        ArrayMap arrayMap = r7.mKeySetMapping;
        if (arrayMap != null) {
            if (arrayMap.containsKey(null) || arrayMap.containsValue(null)) {
                throw new PackageManagerException(-2, "Package has null defined key set.");
            }
            int size = arrayMap.size();
            for (int i = 0; i < size; i++) {
                if (((ArraySet) arrayMap.valueAt(i)).size() <= 0 || ((ArraySet) arrayMap.valueAt(i)).contains(null)) {
                    throw new PackageManagerException(-2, "Package has null/no public keys for defined key-sets.");
                }
            }
        }
        ArraySet arraySet2 = r7.mUpgradeKeySets;
        if (arraySet2 != null) {
            if (arrayMap == null || !arrayMap.keySet().containsAll(arraySet2)) {
                throw new PackageManagerException(-2, "Package has upgrade-key-sets without corresponding definitions.");
            }
        }
    }

    public void addScannedPackageLPw(PackageParser.Package r4) {
        Preconditions.checkNotNull(r4, "Attempted to add null pkg to ksms.");
        Preconditions.checkNotNull(r4.packageName, "Attempted to add null pkg to ksms.");
        PackageSetting packageSetting = this.mPackages.get(r4.packageName);
        Preconditions.checkNotNull(packageSetting, "pkg: " + r4.packageName + "does not have a corresponding entry in mPackages.");
        addSigningKeySetToPackageLPw(packageSetting, r4.mSigningDetails.publicKeys);
        if (r4.mKeySetMapping != null) {
            addDefinedKeySetsToPackageLPw(packageSetting, r4.mKeySetMapping);
            if (r4.mUpgradeKeySets != null) {
                addUpgradeKeySetsToPackageLPw(packageSetting, r4.mUpgradeKeySets);
            }
        }
    }

    void addSigningKeySetToPackageLPw(PackageSetting packageSetting, ArraySet<PublicKey> arraySet) {
        long properSigningKeySet = packageSetting.keySetData.getProperSigningKeySet();
        if (properSigningKeySet != -1) {
            ArraySet<PublicKey> publicKeysFromKeySetLPr = getPublicKeysFromKeySetLPr(properSigningKeySet);
            if (publicKeysFromKeySetLPr != null && publicKeysFromKeySetLPr.equals(arraySet)) {
                return;
            } else {
                decrementKeySetLPw(properSigningKeySet);
            }
        }
        packageSetting.keySetData.setProperSigningKeySet(addKeySetLPw(arraySet).getId());
    }

    private long getIdByKeySetLPr(KeySetHandle keySetHandle) {
        for (int i = 0; i < this.mKeySets.size(); i++) {
            if (keySetHandle.equals(this.mKeySets.valueAt(i))) {
                return this.mKeySets.keyAt(i);
            }
        }
        return -1L;
    }

    void addDefinedKeySetsToPackageLPw(PackageSetting packageSetting, ArrayMap<String, ArraySet<PublicKey>> arrayMap) {
        ArrayMap<String, Long> aliases = packageSetting.keySetData.getAliases();
        ArrayMap<String, Long> arrayMap2 = new ArrayMap<>();
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            String strKeyAt = arrayMap.keyAt(i);
            ArraySet<PublicKey> arraySetValueAt = arrayMap.valueAt(i);
            if (strKeyAt != null && arraySetValueAt != null && arraySetValueAt.size() > 0) {
                arrayMap2.put(strKeyAt, Long.valueOf(addKeySetLPw(arraySetValueAt).getId()));
            }
        }
        int size2 = aliases.size();
        for (int i2 = 0; i2 < size2; i2++) {
            decrementKeySetLPw(aliases.valueAt(i2).longValue());
        }
        packageSetting.keySetData.removeAllUpgradeKeySets();
        packageSetting.keySetData.setAliases(arrayMap2);
    }

    void addUpgradeKeySetsToPackageLPw(PackageSetting packageSetting, ArraySet<String> arraySet) {
        int size = arraySet.size();
        for (int i = 0; i < size; i++) {
            packageSetting.keySetData.addUpgradeKeySet(arraySet.valueAt(i));
        }
    }

    public KeySetHandle getKeySetByAliasAndPackageNameLPr(String str, String str2) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null || packageSetting.keySetData == null) {
            return null;
        }
        Long l = packageSetting.keySetData.getAliases().get(str2);
        if (l == null) {
            throw new IllegalArgumentException("Unknown KeySet alias: " + str2);
        }
        return this.mKeySets.get(l.longValue());
    }

    public boolean isIdValidKeySetId(long j) {
        return this.mKeySets.get(j) != null;
    }

    public boolean shouldCheckUpgradeKeySetLocked(PackageSettingBase packageSettingBase, int i) {
        if (packageSettingBase == null || (i & 512) != 0 || packageSettingBase.isSharedUser() || !packageSettingBase.keySetData.isUsingUpgradeKeySets()) {
            return false;
        }
        long[] upgradeKeySets = packageSettingBase.keySetData.getUpgradeKeySets();
        for (int i2 = 0; i2 < upgradeKeySets.length; i2++) {
            if (!isIdValidKeySetId(upgradeKeySets[i2])) {
                StringBuilder sb = new StringBuilder();
                sb.append("Package ");
                sb.append(packageSettingBase.name != null ? packageSettingBase.name : "<null>");
                sb.append(" contains upgrade-key-set reference to unknown key-set: ");
                sb.append(upgradeKeySets[i2]);
                sb.append(" reverting to signatures check.");
                Slog.wtf(TAG, sb.toString());
                return false;
            }
        }
        return true;
    }

    public boolean checkUpgradeKeySetLocked(PackageSettingBase packageSettingBase, PackageParser.Package r6) {
        for (long j : packageSettingBase.keySetData.getUpgradeKeySets()) {
            ArraySet<PublicKey> publicKeysFromKeySetLPr = getPublicKeysFromKeySetLPr(j);
            if (publicKeysFromKeySetLPr != null && r6.mSigningDetails.publicKeys.containsAll(publicKeysFromKeySetLPr)) {
                return true;
            }
        }
        return false;
    }

    public ArraySet<PublicKey> getPublicKeysFromKeySetLPr(long j) {
        ArraySet<Long> arraySet = this.mKeySetMapping.get(j);
        if (arraySet == null) {
            return null;
        }
        ArraySet<PublicKey> arraySet2 = new ArraySet<>();
        int size = arraySet.size();
        for (int i = 0; i < size; i++) {
            arraySet2.add(this.mPublicKeys.get(arraySet.valueAt(i).longValue()).getKey());
        }
        return arraySet2;
    }

    public KeySetHandle getSigningKeySetByPackageNameLPr(String str) {
        PackageSetting packageSetting = this.mPackages.get(str);
        if (packageSetting == null || packageSetting.keySetData == null || packageSetting.keySetData.getProperSigningKeySet() == -1) {
            return null;
        }
        return this.mKeySets.get(packageSetting.keySetData.getProperSigningKeySet());
    }

    private KeySetHandle addKeySetLPw(ArraySet<PublicKey> arraySet) {
        if (arraySet == null || arraySet.size() == 0) {
            throw new IllegalArgumentException("Cannot add an empty set of keys!");
        }
        ArraySet<Long> arraySet2 = new ArraySet<>(arraySet.size());
        int size = arraySet.size();
        for (int i = 0; i < size; i++) {
            arraySet2.add(Long.valueOf(addPublicKeyLPw(arraySet.valueAt(i))));
        }
        long idFromKeyIdsLPr = getIdFromKeyIdsLPr(arraySet2);
        if (idFromKeyIdsLPr != -1) {
            for (int i2 = 0; i2 < size; i2++) {
                decrementPublicKeyLPw(arraySet2.valueAt(i2).longValue());
            }
            KeySetHandle keySetHandle = this.mKeySets.get(idFromKeyIdsLPr);
            keySetHandle.incrRefCountLPw();
            return keySetHandle;
        }
        long freeKeySetIDLPw = getFreeKeySetIDLPw();
        KeySetHandle keySetHandle2 = new KeySetHandle(freeKeySetIDLPw);
        this.mKeySets.put(freeKeySetIDLPw, keySetHandle2);
        this.mKeySetMapping.put(freeKeySetIDLPw, arraySet2);
        return keySetHandle2;
    }

    private void decrementKeySetLPw(long j) {
        KeySetHandle keySetHandle = this.mKeySets.get(j);
        if (keySetHandle != null && keySetHandle.decrRefCountLPw() <= 0) {
            ArraySet<Long> arraySet = this.mKeySetMapping.get(j);
            int size = arraySet.size();
            for (int i = 0; i < size; i++) {
                decrementPublicKeyLPw(arraySet.valueAt(i).longValue());
            }
            this.mKeySets.delete(j);
            this.mKeySetMapping.delete(j);
        }
    }

    private void decrementPublicKeyLPw(long j) {
        PublicKeyHandle publicKeyHandle = this.mPublicKeys.get(j);
        if (publicKeyHandle != null && publicKeyHandle.decrRefCountLPw() <= 0) {
            this.mPublicKeys.delete(j);
        }
    }

    private long addPublicKeyLPw(PublicKey publicKey) {
        Preconditions.checkNotNull(publicKey, "Cannot add null public key!");
        long idForPublicKeyLPr = getIdForPublicKeyLPr(publicKey);
        if (idForPublicKeyLPr != -1) {
            this.mPublicKeys.get(idForPublicKeyLPr).incrRefCountLPw();
            return idForPublicKeyLPr;
        }
        long freePublicKeyIdLPw = getFreePublicKeyIdLPw();
        this.mPublicKeys.put(freePublicKeyIdLPw, new PublicKeyHandle(freePublicKeyIdLPw, publicKey));
        return freePublicKeyIdLPw;
    }

    private long getIdFromKeyIdsLPr(Set<Long> set) {
        for (int i = 0; i < this.mKeySetMapping.size(); i++) {
            if (this.mKeySetMapping.valueAt(i).equals(set)) {
                return this.mKeySetMapping.keyAt(i);
            }
        }
        return -1L;
    }

    private long getIdForPublicKeyLPr(PublicKey publicKey) {
        String str = new String(publicKey.getEncoded());
        for (int i = 0; i < this.mPublicKeys.size(); i++) {
            if (str.equals(new String(this.mPublicKeys.valueAt(i).getKey().getEncoded()))) {
                return this.mPublicKeys.keyAt(i);
            }
        }
        return -1L;
    }

    private long getFreeKeySetIDLPw() {
        this.lastIssuedKeySetId++;
        return this.lastIssuedKeySetId;
    }

    private long getFreePublicKeyIdLPw() {
        this.lastIssuedKeyId++;
        return this.lastIssuedKeyId;
    }

    public void removeAppKeySetDataLPw(String str) {
        PackageSetting packageSetting = this.mPackages.get(str);
        Preconditions.checkNotNull(packageSetting, "pkg name: " + str + "does not have a corresponding entry in mPackages.");
        decrementKeySetLPw(packageSetting.keySetData.getProperSigningKeySet());
        ArrayMap<String, Long> aliases = packageSetting.keySetData.getAliases();
        for (int i = 0; i < aliases.size(); i++) {
            decrementKeySetLPw(aliases.valueAt(i).longValue());
        }
        clearPackageKeySetDataLPw(packageSetting);
    }

    private void clearPackageKeySetDataLPw(PackageSetting packageSetting) {
        packageSetting.keySetData.setProperSigningKeySet(-1L);
        packageSetting.keySetData.removeAllDefinedKeySets();
        packageSetting.keySetData.removeAllUpgradeKeySets();
    }

    public String encodePublicKey(PublicKey publicKey) throws IOException {
        return new String(Base64.encode(publicKey.getEncoded(), 2));
    }

    public void dumpLPr(PrintWriter printWriter, String str, DumpState dumpState) {
        boolean z;
        boolean z2;
        boolean z3 = false;
        for (Map.Entry<String, PackageSetting> entry : this.mPackages.entrySet()) {
            String key = entry.getKey();
            if (str == null || str.equals(key)) {
                if (!z3) {
                    if (dumpState.onTitlePrinted()) {
                        printWriter.println();
                    }
                    printWriter.println("Key Set Manager:");
                    z3 = true;
                }
                PackageSetting value = entry.getValue();
                printWriter.print("  [");
                printWriter.print(key);
                printWriter.println("]");
                if (value.keySetData != null) {
                    boolean z4 = false;
                    for (Map.Entry<String, Long> entry2 : value.keySetData.getAliases().entrySet()) {
                        if (!z4) {
                            printWriter.print("      KeySets Aliases: ");
                            z4 = true;
                        } else {
                            printWriter.print(", ");
                        }
                        printWriter.print(entry2.getKey());
                        printWriter.print('=');
                        printWriter.print(Long.toString(entry2.getValue().longValue()));
                    }
                    if (z4) {
                        printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                    if (value.keySetData.isUsingDefinedKeySets()) {
                        ArrayMap<String, Long> aliases = value.keySetData.getAliases();
                        int size = aliases.size();
                        z = false;
                        for (int i = 0; i < size; i++) {
                            if (!z) {
                                printWriter.print("      Defined KeySets: ");
                                z = true;
                            } else {
                                printWriter.print(", ");
                            }
                            printWriter.print(Long.toString(aliases.valueAt(i).longValue()));
                        }
                    } else {
                        z = false;
                    }
                    if (z) {
                        printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                    long properSigningKeySet = value.keySetData.getProperSigningKeySet();
                    printWriter.print("      Signing KeySets: ");
                    printWriter.print(Long.toString(properSigningKeySet));
                    printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    if (value.keySetData.isUsingUpgradeKeySets()) {
                        z2 = false;
                        for (long j : value.keySetData.getUpgradeKeySets()) {
                            if (!z2) {
                                printWriter.print("      Upgrade KeySets: ");
                                z2 = true;
                            } else {
                                printWriter.print(", ");
                            }
                            printWriter.print(Long.toString(j));
                        }
                    } else {
                        z2 = false;
                    }
                    if (z2) {
                        printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                }
            }
        }
    }

    void writeKeySetManagerServiceLPr(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, "keyset-settings");
        xmlSerializer.attribute(null, "version", Integer.toString(1));
        writePublicKeysLPr(xmlSerializer);
        writeKeySetsLPr(xmlSerializer);
        xmlSerializer.startTag(null, "lastIssuedKeyId");
        xmlSerializer.attribute(null, "value", Long.toString(this.lastIssuedKeyId));
        xmlSerializer.endTag(null, "lastIssuedKeyId");
        xmlSerializer.startTag(null, "lastIssuedKeySetId");
        xmlSerializer.attribute(null, "value", Long.toString(this.lastIssuedKeySetId));
        xmlSerializer.endTag(null, "lastIssuedKeySetId");
        xmlSerializer.endTag(null, "keyset-settings");
    }

    void writePublicKeysLPr(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, "keys");
        for (int i = 0; i < this.mPublicKeys.size(); i++) {
            long jKeyAt = this.mPublicKeys.keyAt(i);
            String strEncodePublicKey = encodePublicKey(this.mPublicKeys.valueAt(i).getKey());
            xmlSerializer.startTag(null, "public-key");
            xmlSerializer.attribute(null, "identifier", Long.toString(jKeyAt));
            xmlSerializer.attribute(null, "value", strEncodePublicKey);
            xmlSerializer.endTag(null, "public-key");
        }
        xmlSerializer.endTag(null, "keys");
    }

    void writeKeySetsLPr(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startTag(null, "keysets");
        for (int i = 0; i < this.mKeySetMapping.size(); i++) {
            long jKeyAt = this.mKeySetMapping.keyAt(i);
            ArraySet<Long> arraySetValueAt = this.mKeySetMapping.valueAt(i);
            xmlSerializer.startTag(null, "keyset");
            xmlSerializer.attribute(null, "identifier", Long.toString(jKeyAt));
            Iterator<Long> it = arraySetValueAt.iterator();
            while (it.hasNext()) {
                long jLongValue = it.next().longValue();
                xmlSerializer.startTag(null, "key-id");
                xmlSerializer.attribute(null, "identifier", Long.toString(jLongValue));
                xmlSerializer.endTag(null, "key-id");
            }
            xmlSerializer.endTag(null, "keyset");
        }
        xmlSerializer.endTag(null, "keysets");
    }

    void readKeySetsLPw(XmlPullParser xmlPullParser, ArrayMap<Long, Integer> arrayMap) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        String attributeValue = xmlPullParser.getAttributeValue(null, "version");
        if (attributeValue == null) {
            while (true) {
                int next = xmlPullParser.next();
                if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                    break;
                }
            }
            Iterator<PackageSetting> it = this.mPackages.values().iterator();
            while (it.hasNext()) {
                clearPackageKeySetDataLPw(it.next());
            }
            return;
        }
        Integer.parseInt(attributeValue);
        while (true) {
            int next2 = xmlPullParser.next();
            if (next2 == 1 || (next2 == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next2 != 3 && next2 != 4) {
                String name = xmlPullParser.getName();
                if (name.equals("keys")) {
                    readKeysLPw(xmlPullParser);
                } else if (name.equals("keysets")) {
                    readKeySetListLPw(xmlPullParser);
                } else if (name.equals("lastIssuedKeyId")) {
                    this.lastIssuedKeyId = Long.parseLong(xmlPullParser.getAttributeValue(null, "value"));
                } else if (name.equals("lastIssuedKeySetId")) {
                    this.lastIssuedKeySetId = Long.parseLong(xmlPullParser.getAttributeValue(null, "value"));
                }
            }
        }
        addRefCountsFromSavedPackagesLPw(arrayMap);
    }

    void readKeysLPw(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals("public-key")) {
                        readPublicKeyLPw(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readKeySetListLPw(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        long j = 0;
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        String name = xmlPullParser.getName();
                        if (name.equals("keyset")) {
                            j = Long.parseLong(xmlPullParser.getAttributeValue(null, "identifier"));
                            this.mKeySets.put(j, new KeySetHandle(j, 0));
                            this.mKeySetMapping.put(j, new ArraySet<>());
                        } else if (name.equals("key-id")) {
                            this.mKeySetMapping.get(j).add(Long.valueOf(Long.parseLong(xmlPullParser.getAttributeValue(null, "identifier"))));
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readPublicKeyLPw(XmlPullParser xmlPullParser) throws XmlPullParserException {
        long j = Long.parseLong(xmlPullParser.getAttributeValue(null, "identifier"));
        PublicKey publicKey = PackageParser.parsePublicKey(xmlPullParser.getAttributeValue(null, "value"));
        if (publicKey != null) {
            this.mPublicKeys.put(j, new PublicKeyHandle(j, 0, publicKey));
        }
    }

    private void addRefCountsFromSavedPackagesLPw(ArrayMap<Long, Integer> arrayMap) {
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            KeySetHandle keySetHandle = this.mKeySets.get(arrayMap.keyAt(i).longValue());
            if (keySetHandle == null) {
                Slog.wtf(TAG, "Encountered non-existent key-set reference when reading settings");
            } else {
                keySetHandle.setRefCountLPw(arrayMap.valueAt(i).intValue());
            }
        }
        ArraySet arraySet = new ArraySet();
        int size2 = this.mKeySets.size();
        for (int i2 = 0; i2 < size2; i2++) {
            if (this.mKeySets.valueAt(i2).getRefCountLPr() == 0) {
                Slog.wtf(TAG, "Encountered key-set w/out package references when reading settings");
                arraySet.add(Long.valueOf(this.mKeySets.keyAt(i2)));
            }
            ArraySet<Long> arraySetValueAt = this.mKeySetMapping.valueAt(i2);
            int size3 = arraySetValueAt.size();
            for (int i3 = 0; i3 < size3; i3++) {
                this.mPublicKeys.get(arraySetValueAt.valueAt(i3).longValue()).incrRefCountLPw();
            }
        }
        int size4 = arraySet.size();
        for (int i4 = 0; i4 < size4; i4++) {
            decrementKeySetLPw(((Long) arraySet.valueAt(i4)).longValue());
        }
    }
}
