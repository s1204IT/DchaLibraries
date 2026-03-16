package com.android.server.pm;

import android.content.pm.PackageParser;
import android.util.ArraySet;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.util.Collection;
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
    private final Map<String, PackageSetting> mPackages;
    private static long lastIssuedKeySetId = 0;
    private static long lastIssuedKeyId = 0;
    private final LongSparseArray<KeySetHandle> mKeySets = new LongSparseArray<>();
    private final LongSparseArray<PublicKey> mPublicKeys = new LongSparseArray<>();
    protected final LongSparseArray<ArraySet<Long>> mKeySetMapping = new LongSparseArray<>();

    public KeySetManagerService(Map<String, PackageSetting> packages) {
        this.mPackages = packages;
    }

    public boolean packageIsSignedByLPr(String packageName, KeySetHandle ks) {
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("Invalid package name");
        }
        if (pkg.keySetData == null) {
            throw new NullPointerException("Package has no KeySet data");
        }
        long id = getIdByKeySetLPr(ks);
        if (id == -1) {
            return false;
        }
        return pkg.keySetData.packageIsSignedBy(id);
    }

    public boolean packageIsSignedByExactlyLPr(String packageName, KeySetHandle ks) {
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("Invalid package name");
        }
        if (pkg.keySetData == null || pkg.keySetData.getProperSigningKeySet() == -1) {
            throw new NullPointerException("Package has no KeySet data");
        }
        long id = getIdByKeySetLPr(ks);
        return pkg.keySetData.getProperSigningKeySet() == id;
    }

    public void addDefinedKeySetToPackageLPw(String packageName, ArraySet<PublicKey> keys, String alias) {
        if (packageName == null || keys == null || alias == null) {
            Slog.w(TAG, "Got null argument for a defined keyset, ignoring!");
            return;
        }
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("Unknown package");
        }
        KeySetHandle ks = addKeySetLPw(keys);
        long id = getIdByKeySetLPr(ks);
        pkg.keySetData.addDefinedKeySet(id, alias);
    }

    public void addUpgradeKeySetToPackageLPw(String packageName, String alias) {
        if (packageName == null || alias == null) {
            Slog.w(TAG, "Got null argument for a defined keyset, ignoring!");
            return;
        }
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("Unknown package");
        }
        pkg.keySetData.addUpgradeKeySet(alias);
    }

    public void addSigningKeySetToPackageLPw(String packageName, ArraySet<PublicKey> signingKeys) {
        if (packageName == null || signingKeys == null) {
            Slog.w(TAG, "Got null argument for a signing keyset, ignoring!");
            return;
        }
        KeySetHandle ks = addKeySetLPw(signingKeys);
        long id = getIdByKeySetLPr(ks);
        ArraySet<Long> publicKeyIds = this.mKeySetMapping.get(id);
        if (publicKeyIds == null) {
            throw new NullPointerException("Got invalid KeySet id");
        }
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new NullPointerException("No such package!");
        }
        pkg.keySetData.setProperSigningKeySet(id);
        for (int keySetIndex = 0; keySetIndex < this.mKeySets.size(); keySetIndex++) {
            long keySetID = this.mKeySets.keyAt(keySetIndex);
            ArraySet<Long> definedKeys = this.mKeySetMapping.get(keySetID);
            if (publicKeyIds.containsAll(definedKeys)) {
                pkg.keySetData.addSigningKeySet(keySetID);
            }
        }
    }

    private long getIdByKeySetLPr(KeySetHandle ks) {
        for (int keySetIndex = 0; keySetIndex < this.mKeySets.size(); keySetIndex++) {
            KeySetHandle value = this.mKeySets.valueAt(keySetIndex);
            if (ks.equals(value)) {
                return this.mKeySets.keyAt(keySetIndex);
            }
        }
        return -1L;
    }

    public KeySetHandle getKeySetByIdLPr(long id) {
        return this.mKeySets.get(id);
    }

    public KeySetHandle getKeySetByAliasAndPackageNameLPr(String packageName, String alias) {
        PackageSetting p = this.mPackages.get(packageName);
        if (p == null || p.keySetData == null) {
            return null;
        }
        Long keySetId = p.keySetData.getAliases().get(alias);
        if (keySetId == null) {
            throw new IllegalArgumentException("Unknown KeySet alias: " + alias);
        }
        return this.mKeySets.get(keySetId.longValue());
    }

    public ArraySet<PublicKey> getPublicKeysFromKeySetLPr(long id) {
        if (this.mKeySetMapping.get(id) == null) {
            return null;
        }
        ArraySet<PublicKey> mPubKeys = new ArraySet<>();
        Iterator<Long> it = this.mKeySetMapping.get(id).iterator();
        while (it.hasNext()) {
            long pkId = it.next().longValue();
            mPubKeys.add(this.mPublicKeys.get(pkId));
        }
        return mPubKeys;
    }

    public KeySetHandle getSigningKeySetByPackageNameLPr(String packageName) {
        PackageSetting p = this.mPackages.get(packageName);
        if (p == null || p.keySetData == null || p.keySetData.getProperSigningKeySet() == -1) {
            return null;
        }
        return this.mKeySets.get(p.keySetData.getProperSigningKeySet());
    }

    public ArraySet<KeySetHandle> getUpgradeKeySetsByPackageNameLPr(String packageName) {
        ArraySet<KeySetHandle> upgradeKeySets = new ArraySet<>();
        PackageSetting p = this.mPackages.get(packageName);
        if (p == null) {
            throw new NullPointerException("Unknown package");
        }
        if (p.keySetData == null) {
            throw new IllegalArgumentException("Package has no keySet data");
        }
        if (p.keySetData.isUsingUpgradeKeySets()) {
            long[] arr$ = p.keySetData.getUpgradeKeySets();
            for (long l : arr$) {
                upgradeKeySets.add(this.mKeySets.get(l));
            }
        }
        return upgradeKeySets;
    }

    private KeySetHandle addKeySetLPw(ArraySet<PublicKey> keys) {
        if (keys == null) {
            throw new NullPointerException("Provided keys cannot be null");
        }
        ArraySet<Long> addedKeyIds = new ArraySet<>(keys.size());
        for (PublicKey k : keys) {
            addedKeyIds.add(Long.valueOf(addPublicKeyLPw(k)));
        }
        long existingKeySetId = getIdFromKeyIdsLPr(addedKeyIds);
        if (existingKeySetId != -1) {
            return this.mKeySets.get(existingKeySetId);
        }
        KeySetHandle ks = new KeySetHandle();
        long id = getFreeKeySetIDLPw();
        this.mKeySets.put(id, ks);
        this.mKeySetMapping.put(id, addedKeyIds);
        for (String pkgName : this.mPackages.keySet()) {
            PackageSetting p = this.mPackages.get(pkgName);
            if (p.keySetData != null) {
                long pProperSigning = p.keySetData.getProperSigningKeySet();
                if (pProperSigning != -1) {
                    ArraySet<Long> pSigningKeys = this.mKeySetMapping.get(pProperSigning);
                    if (pSigningKeys.containsAll(addedKeyIds)) {
                        p.keySetData.addSigningKeySet(id);
                    }
                }
            }
        }
        return ks;
    }

    private long addPublicKeyLPw(PublicKey key) {
        long existingKeyId = getIdForPublicKeyLPr(key);
        if (existingKeyId == -1) {
            long id = getFreePublicKeyIdLPw();
            this.mPublicKeys.put(id, key);
            return id;
        }
        return existingKeyId;
    }

    private long getIdFromKeyIdsLPr(Set<Long> publicKeyIds) {
        for (int keyMapIndex = 0; keyMapIndex < this.mKeySetMapping.size(); keyMapIndex++) {
            ArraySet<Long> value = this.mKeySetMapping.valueAt(keyMapIndex);
            if (value.equals(publicKeyIds)) {
                return this.mKeySetMapping.keyAt(keyMapIndex);
            }
        }
        return -1L;
    }

    private long getIdForPublicKeyLPr(PublicKey k) {
        String encodedPublicKey = new String(k.getEncoded());
        for (int publicKeyIndex = 0; publicKeyIndex < this.mPublicKeys.size(); publicKeyIndex++) {
            PublicKey value = this.mPublicKeys.valueAt(publicKeyIndex);
            String encodedExistingKey = new String(value.getEncoded());
            if (encodedPublicKey.equals(encodedExistingKey)) {
                return this.mPublicKeys.keyAt(publicKeyIndex);
            }
        }
        return -1L;
    }

    private long getFreeKeySetIDLPw() {
        lastIssuedKeySetId++;
        return lastIssuedKeySetId;
    }

    private long getFreePublicKeyIdLPw() {
        lastIssuedKeyId++;
        return lastIssuedKeyId;
    }

    public void removeAppKeySetDataLPw(String packageName) {
        ArraySet<Long> deletableKeySets = getOriginalKeySetsByPackageNameLPr(packageName);
        ArraySet<Long> deletableKeys = new ArraySet<>();
        for (Long ks : deletableKeySets) {
            ArraySet<Long> knownKeys = this.mKeySetMapping.get(ks.longValue());
            ArraySet<Long> knownKeys2 = knownKeys;
            if (knownKeys2 != null) {
                deletableKeys.addAll((ArraySet<? extends Long>) knownKeys2);
            }
        }
        for (String pkgName : this.mPackages.keySet()) {
            if (!pkgName.equals(packageName)) {
                ArraySet<Long> knownKeySets = getOriginalKeySetsByPackageNameLPr(pkgName);
                deletableKeySets.removeAll((Collection<?>) knownKeySets);
                new ArraySet<>();
                for (Long ks2 : knownKeySets) {
                    ArraySet<Long> knownKeys3 = this.mKeySetMapping.get(ks2.longValue());
                    ArraySet<Long> knownKeys4 = knownKeys3;
                    if (knownKeys4 != null) {
                        deletableKeys.removeAll((Collection<?>) knownKeys4);
                    }
                }
            }
        }
        for (Long ks3 : deletableKeySets) {
            this.mKeySets.delete(ks3.longValue());
            this.mKeySetMapping.delete(ks3.longValue());
        }
        for (Long keyId : deletableKeys) {
            this.mPublicKeys.delete(keyId.longValue());
        }
        Iterator<String> it = this.mPackages.keySet().iterator();
        while (it.hasNext()) {
            PackageSetting p = this.mPackages.get(it.next());
            Iterator<Long> it2 = deletableKeySets.iterator();
            while (it2.hasNext()) {
                p.keySetData.removeSigningKeySet(it2.next().longValue());
            }
        }
        PackageSetting p2 = this.mPackages.get(packageName);
        clearPackageKeySetDataLPw(p2);
    }

    private void clearPackageKeySetDataLPw(PackageSetting p) {
        p.keySetData.removeAllSigningKeySets();
        p.keySetData.removeAllUpgradeKeySets();
        p.keySetData.removeAllDefinedKeySets();
    }

    private ArraySet<Long> getOriginalKeySetsByPackageNameLPr(String packageName) {
        PackageSetting p = this.mPackages.get(packageName);
        if (p == null) {
            throw new NullPointerException("Unknown package");
        }
        if (p.keySetData == null) {
            throw new IllegalArgumentException("Package has no keySet data");
        }
        ArraySet<Long> knownKeySets = new ArraySet<>();
        knownKeySets.add(Long.valueOf(p.keySetData.getProperSigningKeySet()));
        if (p.keySetData.isUsingDefinedKeySets()) {
            long[] arr$ = p.keySetData.getDefinedKeySets();
            for (long ks : arr$) {
                knownKeySets.add(Long.valueOf(ks));
            }
        }
        return knownKeySets;
    }

    public String encodePublicKey(PublicKey k) throws IOException {
        return new String(Base64.encode(k.getEncoded(), 0));
    }

    public void dumpLPr(PrintWriter pw, String packageName, PackageManagerService.DumpState dumpState) {
        boolean printedHeader = false;
        for (Map.Entry<String, PackageSetting> e : this.mPackages.entrySet()) {
            String keySetPackage = e.getKey();
            if (packageName == null || packageName.equals(keySetPackage)) {
                if (!printedHeader) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Key Set Manager:");
                    printedHeader = true;
                }
                PackageSetting pkg = e.getValue();
                pw.print("  [");
                pw.print(keySetPackage);
                pw.println("]");
                if (pkg.keySetData != null) {
                    boolean printedLabel = false;
                    for (Map.Entry<String, Long> entry : pkg.keySetData.getAliases().entrySet()) {
                        if (!printedLabel) {
                            pw.print("      KeySets Aliases: ");
                            printedLabel = true;
                        } else {
                            pw.print(", ");
                        }
                        pw.print(entry.getKey());
                        pw.print('=');
                        pw.print(Long.toString(entry.getValue().longValue()));
                    }
                    if (printedLabel) {
                        pw.println("");
                    }
                    boolean printedLabel2 = false;
                    if (pkg.keySetData.isUsingDefinedKeySets()) {
                        long[] arr$ = pkg.keySetData.getDefinedKeySets();
                        for (long keySetId : arr$) {
                            if (!printedLabel2) {
                                pw.print("      Defined KeySets: ");
                                printedLabel2 = true;
                            } else {
                                pw.print(", ");
                            }
                            pw.print(Long.toString(keySetId));
                        }
                    }
                    if (printedLabel2) {
                        pw.println("");
                    }
                    boolean printedLabel3 = false;
                    long[] signingKeySets = pkg.keySetData.getSigningKeySets();
                    if (signingKeySets != null) {
                        for (long keySetId2 : signingKeySets) {
                            if (!printedLabel3) {
                                pw.print("      Signing KeySets: ");
                                printedLabel3 = true;
                            } else {
                                pw.print(", ");
                            }
                            pw.print(Long.toString(keySetId2));
                        }
                    }
                    if (printedLabel3) {
                        pw.println("");
                    }
                    boolean printedLabel4 = false;
                    if (pkg.keySetData.isUsingUpgradeKeySets()) {
                        long[] arr$2 = pkg.keySetData.getUpgradeKeySets();
                        for (long keySetId3 : arr$2) {
                            if (!printedLabel4) {
                                pw.print("      Upgrade KeySets: ");
                                printedLabel4 = true;
                            } else {
                                pw.print(", ");
                            }
                            pw.print(Long.toString(keySetId3));
                        }
                    }
                    if (printedLabel4) {
                        pw.println("");
                    }
                }
            }
        }
    }

    void writeKeySetManagerServiceLPr(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, "keyset-settings");
        serializer.attribute(null, "version", Integer.toString(1));
        writePublicKeysLPr(serializer);
        writeKeySetsLPr(serializer);
        serializer.startTag(null, "lastIssuedKeyId");
        serializer.attribute(null, "value", Long.toString(lastIssuedKeyId));
        serializer.endTag(null, "lastIssuedKeyId");
        serializer.startTag(null, "lastIssuedKeySetId");
        serializer.attribute(null, "value", Long.toString(lastIssuedKeySetId));
        serializer.endTag(null, "lastIssuedKeySetId");
        serializer.endTag(null, "keyset-settings");
    }

    void writePublicKeysLPr(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, "keys");
        for (int pKeyIndex = 0; pKeyIndex < this.mPublicKeys.size(); pKeyIndex++) {
            long id = this.mPublicKeys.keyAt(pKeyIndex);
            PublicKey key = this.mPublicKeys.valueAt(pKeyIndex);
            String encodedKey = encodePublicKey(key);
            serializer.startTag(null, "public-key");
            serializer.attribute(null, "identifier", Long.toString(id));
            serializer.attribute(null, "value", encodedKey);
            serializer.endTag(null, "public-key");
        }
        serializer.endTag(null, "keys");
    }

    void writeKeySetsLPr(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, "keysets");
        for (int keySetIndex = 0; keySetIndex < this.mKeySetMapping.size(); keySetIndex++) {
            long id = this.mKeySetMapping.keyAt(keySetIndex);
            ArraySet<Long> keys = this.mKeySetMapping.valueAt(keySetIndex);
            serializer.startTag(null, "keyset");
            serializer.attribute(null, "identifier", Long.toString(id));
            Iterator<Long> it = keys.iterator();
            while (it.hasNext()) {
                long keyId = it.next().longValue();
                serializer.startTag(null, "key-id");
                serializer.attribute(null, "identifier", Long.toString(keyId));
                serializer.endTag(null, "key-id");
            }
            serializer.endTag(null, "keyset");
        }
        serializer.endTag(null, "keysets");
    }

    void readKeySetsLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        String recordedVersion = parser.getAttributeValue(null, "version");
        if (recordedVersion == null || Integer.parseInt(recordedVersion) != 1) {
            while (true) {
                int type = parser.next();
                if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                }
            }
            for (PackageSetting p : this.mPackages.values()) {
                clearPackageKeySetDataLPw(p);
            }
            return;
        }
        while (true) {
            int type2 = parser.next();
            if (type2 == 1) {
                return;
            }
            if (type2 != 3 || parser.getDepth() > outerDepth) {
                if (type2 != 3 && type2 != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals("keys")) {
                        readKeysLPw(parser);
                    } else if (tagName.equals("keysets")) {
                        readKeySetListLPw(parser);
                    } else if (tagName.equals("lastIssuedKeyId")) {
                        lastIssuedKeyId = Long.parseLong(parser.getAttributeValue(null, "value"));
                    } else if (tagName.equals("lastIssuedKeySetId")) {
                        lastIssuedKeySetId = Long.parseLong(parser.getAttributeValue(null, "value"));
                    }
                }
            } else {
                return;
            }
        }
    }

    void readKeysLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals("public-key")) {
                        readPublicKeyLPw(parser);
                    }
                }
            } else {
                return;
            }
        }
    }

    void readKeySetListLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        long currentKeySetId = 0;
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals("keyset")) {
                        currentKeySetId = readIdentifierLPw(parser);
                        this.mKeySets.put(currentKeySetId, new KeySetHandle());
                        this.mKeySetMapping.put(currentKeySetId, new ArraySet<>());
                    } else if (tagName.equals("key-id")) {
                        long id = readIdentifierLPw(parser);
                        this.mKeySetMapping.get(currentKeySetId).add(Long.valueOf(id));
                    }
                }
            } else {
                return;
            }
        }
    }

    long readIdentifierLPw(XmlPullParser parser) throws XmlPullParserException {
        return Long.parseLong(parser.getAttributeValue(null, "identifier"));
    }

    void readPublicKeyLPw(XmlPullParser parser) throws XmlPullParserException {
        String encodedID = parser.getAttributeValue(null, "identifier");
        long identifier = Long.parseLong(encodedID);
        String encodedPublicKey = parser.getAttributeValue(null, "value");
        PublicKey pub = PackageParser.parsePublicKey(encodedPublicKey);
        if (pub != null) {
            this.mPublicKeys.put(identifier, pub);
        }
    }
}
