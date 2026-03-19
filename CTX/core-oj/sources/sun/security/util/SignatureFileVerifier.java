package sun.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.CodeSigner;
import java.security.CryptoPrimitive;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import sun.security.jca.Providers;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.ManifestDigester;

public class SignatureFileVerifier {
    private PKCS7 block;
    private CertificateFactory certificateFactory;
    private HashMap<String, MessageDigest> createdDigests;
    private ManifestDigester md;
    private String name;
    private byte[] sfBytes;
    private ArrayList<CodeSigner[]> signerCache;
    private boolean workaround = false;
    private static final Debug debug = Debug.getInstance("jar");
    private static final Set<CryptoPrimitive> DIGEST_PRIMITIVE_SET = Collections.unmodifiableSet(EnumSet.of(CryptoPrimitive.MESSAGE_DIGEST));
    private static final DisabledAlgorithmConstraints JAR_DISABLED_CHECK = new DisabledAlgorithmConstraints(DisabledAlgorithmConstraints.PROPERTY_JAR_DISABLED_ALGS);
    private static final String ATTR_DIGEST = "-DIGEST-Manifest-Main-Attributes".toUpperCase(Locale.ENGLISH);
    private static final char[] hexc = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public SignatureFileVerifier(ArrayList<CodeSigner[]> arrayList, ManifestDigester manifestDigester, String str, byte[] bArr) throws Throwable {
        Object objStartJarVerification;
        this.certificateFactory = null;
        try {
            objStartJarVerification = Providers.startJarVerification();
            try {
                this.block = new PKCS7(bArr);
                this.sfBytes = this.block.getContentInfo().getData();
                this.certificateFactory = CertificateFactory.getInstance("X509");
                Providers.stopJarVerification(objStartJarVerification);
                this.name = str.substring(0, str.lastIndexOf(".")).toUpperCase(Locale.ENGLISH);
                this.md = manifestDigester;
                this.signerCache = arrayList;
            } catch (Throwable th) {
                th = th;
                Providers.stopJarVerification(objStartJarVerification);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            objStartJarVerification = null;
        }
    }

    public boolean needSignatureFileBytes() {
        return this.sfBytes == null;
    }

    public boolean needSignatureFile(String str) {
        return this.name.equalsIgnoreCase(str);
    }

    public void setSignatureFile(byte[] bArr) {
        this.sfBytes = bArr;
    }

    public static boolean isBlockOrSF(String str) {
        if (str.endsWith(".SF") || str.endsWith(".DSA") || str.endsWith(".RSA") || str.endsWith(".EC")) {
            return true;
        }
        return false;
    }

    public static boolean isSigningRelated(String str) {
        String upperCase = str.toUpperCase(Locale.ENGLISH);
        if (!upperCase.startsWith("META-INF/")) {
            return false;
        }
        String strSubstring = upperCase.substring(9);
        if (strSubstring.indexOf(47) != -1) {
            return false;
        }
        if (isBlockOrSF(strSubstring) || strSubstring.equals("MANIFEST.MF")) {
            return true;
        }
        if (!strSubstring.startsWith("SIG-")) {
            return false;
        }
        int iLastIndexOf = strSubstring.lastIndexOf(46);
        if (iLastIndexOf != -1) {
            String strSubstring2 = strSubstring.substring(iLastIndexOf + 1);
            if (strSubstring2.length() > 3 || strSubstring2.length() < 1) {
                return false;
            }
            for (int i = 0; i < strSubstring2.length(); i++) {
                char cCharAt = strSubstring2.charAt(i);
                if ((cCharAt < 'A' || cCharAt > 'Z') && (cCharAt < '0' || cCharAt > '9')) {
                    return false;
                }
            }
        }
        return true;
    }

    private MessageDigest getDigest(String str) throws SignatureException {
        if (!JAR_DISABLED_CHECK.permits(DIGEST_PRIMITIVE_SET, str, null)) {
            throw new SignatureException("SignatureFile check failed. Disabled algorithm used: " + str);
        }
        if (this.createdDigests == null) {
            this.createdDigests = new HashMap<>();
        }
        MessageDigest messageDigest = this.createdDigests.get(str);
        if (messageDigest == null) {
            try {
                MessageDigest messageDigest2 = MessageDigest.getInstance(str);
                try {
                    this.createdDigests.put(str, messageDigest2);
                    return messageDigest2;
                } catch (NoSuchAlgorithmException e) {
                    return messageDigest2;
                }
            } catch (NoSuchAlgorithmException e2) {
                return messageDigest;
            }
        }
        return messageDigest;
    }

    public void process(Hashtable<String, CodeSigner[]> hashtable, List<Object> list) throws Throwable {
        Object objStartJarVerification;
        try {
            objStartJarVerification = Providers.startJarVerification();
            try {
                processImpl(hashtable, list);
                Providers.stopJarVerification(objStartJarVerification);
            } catch (Throwable th) {
                th = th;
                Providers.stopJarVerification(objStartJarVerification);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            objStartJarVerification = null;
        }
    }

    private void processImpl(Hashtable<String, CodeSigner[]> hashtable, List<Object> list) throws NoSuchAlgorithmException, SignatureException, IOException, CertificateException {
        Manifest manifest = new Manifest();
        manifest.read(new ByteArrayInputStream(this.sfBytes));
        String value = manifest.getMainAttributes().getValue(Attributes.Name.SIGNATURE_VERSION);
        if (value == null || !value.equalsIgnoreCase("1.0")) {
            return;
        }
        SignerInfo[] signerInfoArrVerify = this.block.verify(this.sfBytes);
        if (signerInfoArrVerify == null) {
            throw new SecurityException("cannot verify signature block file " + this.name);
        }
        CodeSigner[] signers = getSigners(signerInfoArrVerify, this.block);
        if (signers == null) {
            return;
        }
        boolean zVerifyManifestHash = verifyManifestHash(manifest, this.md, list);
        if (!zVerifyManifestHash && !verifyManifestMainAttrs(manifest, this.md)) {
            throw new SecurityException("Invalid signature file digest for Manifest main attributes");
        }
        for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
            String key = entry.getKey();
            if (zVerifyManifestHash || verifySection(entry.getValue(), key, this.md)) {
                if (key.startsWith("./")) {
                    key = key.substring(2);
                }
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                updateSigners(signers, hashtable, key);
                if (debug != null) {
                    debug.println("processSignature signed name = " + key);
                }
            } else if (debug != null) {
                debug.println("processSignature unsigned name = " + key);
            }
        }
        updateSigners(signers, hashtable, JarFile.MANIFEST_NAME);
    }

    private boolean verifyManifestHash(Manifest manifest, ManifestDigester manifestDigester, List<Object> list) throws SignatureException, IOException {
        boolean z = false;
        for (Map.Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
            String string = entry.getKey().toString();
            if (string.toUpperCase(Locale.ENGLISH).endsWith("-DIGEST-MANIFEST")) {
                String strSubstring = string.substring(0, string.length() - 16);
                list.add(string);
                list.add(entry.getValue());
                MessageDigest digest = getDigest(strSubstring);
                if (digest != null) {
                    byte[] bArrManifestDigest = manifestDigester.manifestDigest(digest);
                    byte[] bArrDecode = Base64.getMimeDecoder().decode((String) entry.getValue());
                    if (debug != null) {
                        debug.println("Signature File: Manifest digest " + digest.getAlgorithm());
                        debug.println("  sigfile  " + toHex(bArrDecode));
                        debug.println("  computed " + toHex(bArrManifestDigest));
                        debug.println();
                    }
                    if (MessageDigest.isEqual(bArrManifestDigest, bArrDecode)) {
                        z = true;
                    }
                }
            }
        }
        return z;
    }

    private boolean verifyManifestMainAttrs(Manifest manifest, ManifestDigester manifestDigester) throws SignatureException, IOException {
        MessageDigest digest;
        for (Map.Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
            String string = entry.getKey().toString();
            if (string.toUpperCase(Locale.ENGLISH).endsWith(ATTR_DIGEST) && (digest = getDigest(string.substring(0, string.length() - ATTR_DIGEST.length()))) != null) {
                byte[] bArrDigest = manifestDigester.get(ManifestDigester.MF_MAIN_ATTRS, false).digest(digest);
                byte[] bArrDecode = Base64.getMimeDecoder().decode((String) entry.getValue());
                if (debug != null) {
                    debug.println("Signature File: Manifest Main Attributes digest " + digest.getAlgorithm());
                    debug.println("  sigfile  " + toHex(bArrDecode));
                    debug.println("  computed " + toHex(bArrDigest));
                    debug.println();
                }
                if (!MessageDigest.isEqual(bArrDigest, bArrDecode)) {
                    if (debug == null) {
                        return false;
                    }
                    debug.println("Verification of Manifest main attributes failed");
                    debug.println();
                    return false;
                }
            }
        }
        return true;
    }

    private boolean verifySection(Attributes attributes, String str, ManifestDigester manifestDigester) throws SignatureException, IOException {
        MessageDigest digest;
        byte[] bArrDigest;
        ManifestDigester.Entry entry = manifestDigester.get(str, this.block.isOldStyle());
        if (entry == null) {
            throw new SecurityException("no manifest section for signature file entry " + str);
        }
        if (attributes == null) {
            return false;
        }
        boolean z = false;
        for (Map.Entry<Object, Object> entry2 : attributes.entrySet()) {
            String string = entry2.getKey().toString();
            if (string.toUpperCase(Locale.ENGLISH).endsWith("-DIGEST") && (digest = getDigest(string.substring(0, string.length() - 7))) != null) {
                byte[] bArrDecode = Base64.getMimeDecoder().decode((String) entry2.getValue());
                if (this.workaround) {
                    bArrDigest = entry.digestWorkaround(digest);
                } else {
                    bArrDigest = entry.digest(digest);
                }
                if (debug != null) {
                    debug.println("Signature Block File: " + str + " digest=" + digest.getAlgorithm());
                    Debug debug2 = debug;
                    StringBuilder sb = new StringBuilder();
                    sb.append("  expected ");
                    sb.append(toHex(bArrDecode));
                    debug2.println(sb.toString());
                    debug.println("  computed " + toHex(bArrDigest));
                    debug.println();
                }
                boolean z2 = true;
                if (!MessageDigest.isEqual(bArrDigest, bArrDecode)) {
                    if (!this.workaround) {
                        byte[] bArrDigestWorkaround = entry.digestWorkaround(digest);
                        if (MessageDigest.isEqual(bArrDigestWorkaround, bArrDecode)) {
                            if (debug != null) {
                                debug.println("  re-computed " + toHex(bArrDigestWorkaround));
                                debug.println();
                            }
                            this.workaround = true;
                            z = true;
                            if (!z2) {
                                throw new SecurityException("invalid " + digest.getAlgorithm() + " signature file digest for " + str);
                            }
                        }
                    }
                    z2 = false;
                    if (!z2) {
                    }
                } else {
                    z = true;
                    if (!z2) {
                    }
                }
            }
        }
        return z;
    }

    private CodeSigner[] getSigners(SignerInfo[] signerInfoArr, PKCS7 pkcs7) throws NoSuchAlgorithmException, SignatureException, IOException, CertificateException {
        ArrayList arrayList = null;
        for (SignerInfo signerInfo : signerInfoArr) {
            ArrayList<X509Certificate> certificateChain = signerInfo.getCertificateChain(pkcs7);
            CertPath certPathGenerateCertPath = this.certificateFactory.generateCertPath(certificateChain);
            if (arrayList == null) {
                arrayList = new ArrayList();
            }
            arrayList.add(new CodeSigner(certPathGenerateCertPath, signerInfo.getTimestamp()));
            if (debug != null) {
                debug.println("Signature Block Certificate: " + ((Object) certificateChain.get(0)));
            }
        }
        if (arrayList != null) {
            return (CodeSigner[]) arrayList.toArray(new CodeSigner[arrayList.size()]);
        }
        return null;
    }

    static String toHex(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer(bArr.length * 2);
        for (int i = 0; i < bArr.length; i++) {
            stringBuffer.append(hexc[(bArr[i] >> 4) & 15]);
            stringBuffer.append(hexc[bArr[i] & 15]);
        }
        return stringBuffer.toString();
    }

    static boolean contains(CodeSigner[] codeSignerArr, CodeSigner codeSigner) {
        for (CodeSigner codeSigner2 : codeSignerArr) {
            if (codeSigner2.equals(codeSigner)) {
                return true;
            }
        }
        return false;
    }

    static boolean isSubSet(CodeSigner[] codeSignerArr, CodeSigner[] codeSignerArr2) {
        if (codeSignerArr2 == codeSignerArr) {
            return true;
        }
        for (CodeSigner codeSigner : codeSignerArr) {
            if (!contains(codeSignerArr2, codeSigner)) {
                return false;
            }
        }
        return true;
    }

    static boolean matches(CodeSigner[] codeSignerArr, CodeSigner[] codeSignerArr2, CodeSigner[] codeSignerArr3) {
        if (codeSignerArr2 == null && codeSignerArr == codeSignerArr3) {
            return true;
        }
        if ((codeSignerArr2 != null && !isSubSet(codeSignerArr2, codeSignerArr)) || !isSubSet(codeSignerArr3, codeSignerArr)) {
            return false;
        }
        for (int i = 0; i < codeSignerArr.length; i++) {
            if (!((codeSignerArr2 != null && contains(codeSignerArr2, codeSignerArr[i])) || contains(codeSignerArr3, codeSignerArr[i]))) {
                return false;
            }
        }
        return true;
    }

    void updateSigners(CodeSigner[] codeSignerArr, Hashtable<String, CodeSigner[]> hashtable, String str) {
        CodeSigner[] codeSignerArr2;
        CodeSigner[] codeSignerArr3 = hashtable.get(str);
        int size = this.signerCache.size();
        do {
            size--;
            if (size != -1) {
                codeSignerArr2 = this.signerCache.get(size);
            } else {
                if (codeSignerArr3 != null) {
                    CodeSigner[] codeSignerArr4 = new CodeSigner[codeSignerArr3.length + codeSignerArr.length];
                    System.arraycopy(codeSignerArr3, 0, codeSignerArr4, 0, codeSignerArr3.length);
                    System.arraycopy(codeSignerArr, 0, codeSignerArr4, codeSignerArr3.length, codeSignerArr.length);
                    codeSignerArr = codeSignerArr4;
                }
                this.signerCache.add(codeSignerArr);
                hashtable.put(str, codeSignerArr);
                return;
            }
        } while (!matches(codeSignerArr2, codeSignerArr3, codeSignerArr));
        hashtable.put(str, codeSignerArr2);
    }
}
