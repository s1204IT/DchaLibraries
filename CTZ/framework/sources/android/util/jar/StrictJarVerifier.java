package android.util.jar;

import android.security.keystore.KeyProperties;
import android.util.jar.StrictJarManifest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import sun.security.jca.Providers;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;

class StrictJarVerifier {
    private static final String[] DIGEST_ALGORITHMS = {KeyProperties.DIGEST_SHA512, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA256, "SHA1"};
    private static final String SF_ATTRIBUTE_ANDROID_APK_SIGNED_NAME = "X-Android-APK-Signed";
    private final String jarName;
    private final int mainAttributesEnd;
    private final StrictJarManifest manifest;
    private final HashMap<String, byte[]> metaEntries;
    private final boolean signatureSchemeRollbackProtectionsEnforced;
    private final Hashtable<String, HashMap<String, Attributes>> signatures = new Hashtable<>(5);
    private final Hashtable<String, Certificate[]> certificates = new Hashtable<>(5);
    private final Hashtable<String, Certificate[][]> verifiedEntries = new Hashtable<>();

    static class VerifierEntry extends OutputStream {
        private final Certificate[][] certChains;
        private final MessageDigest digest;
        private final byte[] hash;
        private final String name;
        private final Hashtable<String, Certificate[][]> verifiedEntries;

        VerifierEntry(String str, MessageDigest messageDigest, byte[] bArr, Certificate[][] certificateArr, Hashtable<String, Certificate[][]> hashtable) {
            this.name = str;
            this.digest = messageDigest;
            this.hash = bArr;
            this.certChains = certificateArr;
            this.verifiedEntries = hashtable;
        }

        @Override
        public void write(int i) {
            this.digest.update((byte) i);
        }

        @Override
        public void write(byte[] bArr, int i, int i2) {
            this.digest.update(bArr, i, i2);
        }

        void verify() {
            if (!StrictJarVerifier.verifyMessageDigest(this.digest.digest(), this.hash)) {
                throw StrictJarVerifier.invalidDigest("META-INF/MANIFEST.MF", this.name, this.name);
            }
            this.verifiedEntries.put(this.name, this.certChains);
        }
    }

    private static SecurityException invalidDigest(String str, String str2, String str3) {
        throw new SecurityException(str + " has invalid digest for " + str2 + " in " + str3);
    }

    private static SecurityException failedVerification(String str, String str2) {
        throw new SecurityException(str + " failed verification of " + str2);
    }

    private static SecurityException failedVerification(String str, String str2, Throwable th) {
        throw new SecurityException(str + " failed verification of " + str2, th);
    }

    StrictJarVerifier(String str, StrictJarManifest strictJarManifest, HashMap<String, byte[]> map, boolean z) {
        this.jarName = str;
        this.manifest = strictJarManifest;
        this.metaEntries = map;
        this.mainAttributesEnd = strictJarManifest.getMainAttributesEnd();
        this.signatureSchemeRollbackProtectionsEnforced = z;
    }

    VerifierEntry initEntry(String str) {
        Attributes attributes;
        if (this.manifest == null || this.signatures.isEmpty() || (attributes = this.manifest.getAttributes(str)) == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, HashMap<String, Attributes>> entry : this.signatures.entrySet()) {
            if (entry.getValue().get(str) != null) {
                Certificate[] certificateArr = this.certificates.get(entry.getKey());
                if (certificateArr != null) {
                    arrayList.add(certificateArr);
                }
            }
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        Certificate[][] certificateArr2 = (Certificate[][]) arrayList.toArray(new Certificate[arrayList.size()][]);
        for (int i = 0; i < DIGEST_ALGORITHMS.length; i++) {
            String str2 = DIGEST_ALGORITHMS[i];
            String value = attributes.getValue(str2 + "-Digest");
            if (value != null) {
                try {
                    return new VerifierEntry(str, MessageDigest.getInstance(str2), value.getBytes(StandardCharsets.ISO_8859_1), certificateArr2, this.verifiedEntries);
                } catch (NoSuchAlgorithmException e) {
                }
            }
        }
        return null;
    }

    void addMetaEntry(String str, byte[] bArr) {
        this.metaEntries.put(str.toUpperCase(Locale.US), bArr);
    }

    synchronized boolean readCertificates() {
        if (this.metaEntries.isEmpty()) {
            return false;
        }
        Iterator<String> it = this.metaEntries.keySet().iterator();
        while (it.hasNext()) {
            String next = it.next();
            if (next.endsWith(".DSA") || next.endsWith(".RSA") || next.endsWith(".EC")) {
                verifyCertificate(next);
                it.remove();
            }
        }
        return true;
    }

    static Certificate[] verifyBytes(byte[] bArr, byte[] bArr2) throws Throwable {
        Object objStartJarVerification;
        try {
            try {
                objStartJarVerification = Providers.startJarVerification();
            } catch (Throwable th) {
                th = th;
                objStartJarVerification = null;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            PKCS7 pkcs7 = new PKCS7(bArr);
            SignerInfo[] signerInfoArrVerify = pkcs7.verify(bArr2);
            if (signerInfoArrVerify == null || signerInfoArrVerify.length == 0) {
                throw new GeneralSecurityException("Failed to verify signature: no verified SignerInfos");
            }
            ArrayList certificateChain = signerInfoArrVerify[0].getCertificateChain(pkcs7);
            if (certificateChain == null) {
                throw new GeneralSecurityException("Failed to find verified SignerInfo certificate chain");
            }
            if (certificateChain.isEmpty()) {
                throw new GeneralSecurityException("Verified SignerInfo certificate chain is emtpy");
            }
            Certificate[] certificateArr = (Certificate[]) certificateChain.toArray(new X509Certificate[certificateChain.size()]);
            Providers.stopJarVerification(objStartJarVerification);
            return certificateArr;
        } catch (IOException e2) {
            e = e2;
            throw new GeneralSecurityException("IO exception verifying jar cert", e);
        } catch (Throwable th2) {
            th = th2;
            Providers.stopJarVerification(objStartJarVerification);
            throw th;
        }
    }

    private void verifyCertificate(String str) throws Throwable {
        byte[] bArr;
        String value;
        boolean z;
        boolean z2;
        StringBuilder sb = new StringBuilder();
        boolean z3 = false;
        sb.append(str.substring(0, str.lastIndexOf(46)));
        sb.append(".SF");
        String string = sb.toString();
        byte[] bArr2 = this.metaEntries.get(string);
        if (bArr2 == null || (bArr = this.metaEntries.get("META-INF/MANIFEST.MF")) == null) {
            return;
        }
        try {
            Certificate[] certificateArrVerifyBytes = verifyBytes(this.metaEntries.get(str), bArr2);
            if (certificateArrVerifyBytes != null) {
                this.certificates.put(string, certificateArrVerifyBytes);
            }
            Attributes attributes = new Attributes();
            HashMap<String, Attributes> map = new HashMap<>();
            try {
                new StrictJarManifestReader(bArr2, attributes).readEntries(map, null);
                if (this.signatureSchemeRollbackProtectionsEnforced && (value = attributes.getValue(SF_ATTRIBUTE_ANDROID_APK_SIGNED_NAME)) != null) {
                    StringTokenizer stringTokenizer = new StringTokenizer(value, ",");
                    while (true) {
                        if (stringTokenizer.hasMoreTokens()) {
                            String strTrim = stringTokenizer.nextToken().trim();
                            if (!strTrim.isEmpty()) {
                                try {
                                    int i = Integer.parseInt(strTrim);
                                    if (i != 2) {
                                        if (i == 3) {
                                            z = false;
                                            z2 = true;
                                            break;
                                        }
                                    } else {
                                        z2 = false;
                                        z = true;
                                        break;
                                    }
                                } catch (Exception e) {
                                }
                            }
                        } else {
                            z = false;
                            z2 = false;
                            break;
                        }
                    }
                    if (z) {
                        throw new SecurityException(string + " indicates " + this.jarName + " is signed using APK Signature Scheme v2, but no such signature was found. Signature stripped?");
                    }
                    if (z2) {
                        throw new SecurityException(string + " indicates " + this.jarName + " is signed using APK Signature Scheme v3, but no such signature was found. Signature stripped?");
                    }
                }
                if (attributes.get(Attributes.Name.SIGNATURE_VERSION) == null) {
                    return;
                }
                String value2 = attributes.getValue("Created-By");
                if (value2 != null && value2.indexOf("signtool") != -1) {
                    z3 = true;
                }
                if (this.mainAttributesEnd > 0 && !z3 && !verify(attributes, "-Digest-Manifest-Main-Attributes", bArr, 0, this.mainAttributesEnd, false, true)) {
                    throw failedVerification(this.jarName, string);
                }
                if (!verify(attributes, z3 ? "-Digest" : "-Digest-Manifest", bArr, 0, bArr.length, false, false)) {
                    for (Map.Entry<String, Attributes> entry : map.entrySet()) {
                        StrictJarManifest.Chunk chunk = this.manifest.getChunk(entry.getKey());
                        if (chunk == null) {
                            return;
                        }
                        if (!verify(entry.getValue(), "-Digest", bArr, chunk.start, chunk.end, z3, false)) {
                            throw invalidDigest(string, entry.getKey(), this.jarName);
                        }
                    }
                }
                this.metaEntries.put(string, null);
                this.signatures.put(string, map);
            } catch (IOException e2) {
            }
        } catch (GeneralSecurityException e3) {
            throw failedVerification(this.jarName, string, e3);
        }
    }

    boolean isSignedJar() {
        return this.certificates.size() > 0;
    }

    private boolean verify(Attributes attributes, String str, byte[] bArr, int i, int i2, boolean z, boolean z2) {
        for (int i3 = 0; i3 < DIGEST_ALGORITHMS.length; i3++) {
            String str2 = DIGEST_ALGORITHMS[i3];
            String value = attributes.getValue(str2 + str);
            if (value != null) {
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance(str2);
                    if (z) {
                        int i4 = i2 - 1;
                        if (bArr[i4] == 10 && bArr[i2 - 2] == 10) {
                            messageDigest.update(bArr, i, i4 - i);
                        } else {
                            messageDigest.update(bArr, i, i2 - i);
                        }
                    }
                    return verifyMessageDigest(messageDigest.digest(), value.getBytes(StandardCharsets.ISO_8859_1));
                } catch (NoSuchAlgorithmException e) {
                }
            }
        }
        return z2;
    }

    private static boolean verifyMessageDigest(byte[] bArr, byte[] bArr2) {
        try {
            return MessageDigest.isEqual(bArr, Base64.getDecoder().decode(bArr2));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    Certificate[][] getCertificateChains(String str) {
        return this.verifiedEntries.get(str);
    }

    void removeMetaEntries() {
        this.metaEntries.clear();
    }
}
