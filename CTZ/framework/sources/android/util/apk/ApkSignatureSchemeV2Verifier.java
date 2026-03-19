package android.util.apk;

import android.app.backup.FullBackup;
import android.util.ArrayMap;
import android.util.Pair;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ApkSignatureSchemeV2Verifier {
    private static final int APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 1896449818;
    public static final int SF_ATTRIBUTE_ANDROID_APK_SIGNED_ID = 2;
    private static final int STRIPPING_PROTECTION_ATTR_ID = -1091571699;

    public static boolean hasSignature(String str) throws Exception {
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(str, FullBackup.ROOT_TREE_TOKEN);
            try {
                findSignature(randomAccessFile);
                return true;
            } finally {
                $closeResource(null, randomAccessFile);
            }
        } catch (SignatureNotFoundException e) {
            return false;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public static X509Certificate[][] verify(String str) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(str, true).certs;
    }

    public static X509Certificate[][] plsCertsNoVerifyOnlyCerts(String str) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(str, false).certs;
    }

    private static VerifiedSigner verify(String str, boolean z) throws Exception {
        RandomAccessFile randomAccessFile = new RandomAccessFile(str, FullBackup.ROOT_TREE_TOKEN);
        try {
            return verify(randomAccessFile, z);
        } finally {
            $closeResource(null, randomAccessFile);
        }
    }

    private static VerifiedSigner verify(RandomAccessFile randomAccessFile, boolean z) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(randomAccessFile, findSignature(randomAccessFile), z);
    }

    private static SignatureInfo findSignature(RandomAccessFile randomAccessFile) throws SignatureNotFoundException, IOException {
        return ApkSigningBlockUtils.findSignature(randomAccessFile, APK_SIGNATURE_SCHEME_V2_BLOCK_ID);
    }

    private static VerifiedSigner verify(RandomAccessFile randomAccessFile, SignatureInfo signatureInfo, boolean z) throws SecurityException, IOException {
        ArrayMap arrayMap = new ArrayMap();
        ArrayList arrayList = new ArrayList();
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            try {
                ByteBuffer lengthPrefixedSlice = ApkSigningBlockUtils.getLengthPrefixedSlice(signatureInfo.signatureBlock);
                int i = 0;
                while (lengthPrefixedSlice.hasRemaining()) {
                    i++;
                    try {
                        arrayList.add(verifySigner(ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice), arrayMap, certificateFactory));
                    } catch (IOException | SecurityException | BufferUnderflowException e) {
                        throw new SecurityException("Failed to parse/verify signer #" + i + " block", e);
                    }
                }
                if (i < 1) {
                    throw new SecurityException("No signers found");
                }
                if (arrayMap.isEmpty()) {
                    throw new SecurityException("No content digests found");
                }
                if (z) {
                    ApkSigningBlockUtils.verifyIntegrity(arrayMap, randomAccessFile, signatureInfo);
                }
                byte[] verityDigestAndVerifySourceLength = null;
                if (arrayMap.containsKey(3)) {
                    verityDigestAndVerifySourceLength = ApkSigningBlockUtils.parseVerityDigestAndVerifySourceLength((byte[]) arrayMap.get(3), randomAccessFile.length(), signatureInfo);
                }
                return new VerifiedSigner((X509Certificate[][]) arrayList.toArray(new X509Certificate[arrayList.size()][]), verityDigestAndVerifySourceLength);
            } catch (IOException e2) {
                throw new SecurityException("Failed to read list of signers", e2);
            }
        } catch (CertificateException e3) {
            throw new RuntimeException("Failed to obtain X.509 CertificateFactory", e3);
        }
    }

    private static X509Certificate[] verifySigner(ByteBuffer byteBuffer, Map<Integer, byte[]> map, CertificateFactory certificateFactory) throws IOException, SecurityException {
        ByteBuffer lengthPrefixedSlice = ApkSigningBlockUtils.getLengthPrefixedSlice(byteBuffer);
        ByteBuffer lengthPrefixedSlice2 = ApkSigningBlockUtils.getLengthPrefixedSlice(byteBuffer);
        byte[] lengthPrefixedByteArray = ApkSigningBlockUtils.readLengthPrefixedByteArray(byteBuffer);
        ArrayList arrayList = new ArrayList();
        byte[] lengthPrefixedByteArray2 = null;
        int i = -1;
        int i2 = 0;
        while (lengthPrefixedSlice2.hasRemaining()) {
            i2++;
            try {
                ByteBuffer lengthPrefixedSlice3 = ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice2);
                if (lengthPrefixedSlice3.remaining() < 8) {
                    throw new SecurityException("Signature record too short");
                }
                int i3 = lengthPrefixedSlice3.getInt();
                arrayList.add(Integer.valueOf(i3));
                if (isSupportedSignatureAlgorithm(i3)) {
                    if (i == -1 || ApkSigningBlockUtils.compareSignatureAlgorithm(i3, i) > 0) {
                        lengthPrefixedByteArray2 = ApkSigningBlockUtils.readLengthPrefixedByteArray(lengthPrefixedSlice3);
                        i = i3;
                    }
                }
            } catch (IOException | BufferUnderflowException e) {
                throw new SecurityException("Failed to parse signature record #" + i2, e);
            }
        }
        if (i == -1) {
            if (i2 == 0) {
                throw new SecurityException("No signatures found");
            }
            throw new SecurityException("No supported signatures found");
        }
        String signatureAlgorithmJcaKeyAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmJcaKeyAlgorithm(i);
        Pair<String, ? extends AlgorithmParameterSpec> signatureAlgorithmJcaSignatureAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmJcaSignatureAlgorithm(i);
        String str = signatureAlgorithmJcaSignatureAlgorithm.first;
        AlgorithmParameterSpec algorithmParameterSpec = (AlgorithmParameterSpec) signatureAlgorithmJcaSignatureAlgorithm.second;
        try {
            PublicKey publicKeyGeneratePublic = KeyFactory.getInstance(signatureAlgorithmJcaKeyAlgorithm).generatePublic(new X509EncodedKeySpec(lengthPrefixedByteArray));
            Signature signature = Signature.getInstance(str);
            signature.initVerify(publicKeyGeneratePublic);
            if (algorithmParameterSpec != null) {
                signature.setParameter(algorithmParameterSpec);
            }
            signature.update(lengthPrefixedSlice);
            if (!signature.verify(lengthPrefixedByteArray2)) {
                throw new SecurityException(str + " signature did not verify");
            }
            lengthPrefixedSlice.clear();
            ByteBuffer lengthPrefixedSlice4 = ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice);
            ArrayList arrayList2 = new ArrayList();
            byte[] lengthPrefixedByteArray3 = null;
            int i4 = 0;
            while (lengthPrefixedSlice4.hasRemaining()) {
                i4++;
                try {
                    ByteBuffer lengthPrefixedSlice5 = ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice4);
                    if (lengthPrefixedSlice5.remaining() < 8) {
                        throw new IOException("Record too short");
                    }
                    int i5 = lengthPrefixedSlice5.getInt();
                    arrayList2.add(Integer.valueOf(i5));
                    if (i5 == i) {
                        lengthPrefixedByteArray3 = ApkSigningBlockUtils.readLengthPrefixedByteArray(lengthPrefixedSlice5);
                    }
                } catch (IOException | BufferUnderflowException e2) {
                    throw new IOException("Failed to parse digest record #" + i4, e2);
                }
            }
            if (!arrayList.equals(arrayList2)) {
                throw new SecurityException("Signature algorithms don't match between digests and signatures records");
            }
            int signatureAlgorithmContentDigestAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmContentDigestAlgorithm(i);
            byte[] bArrPut = map.put(Integer.valueOf(signatureAlgorithmContentDigestAlgorithm), lengthPrefixedByteArray3);
            if (bArrPut != null && !MessageDigest.isEqual(bArrPut, lengthPrefixedByteArray3)) {
                throw new SecurityException(ApkSigningBlockUtils.getContentDigestAlgorithmJcaDigestAlgorithm(signatureAlgorithmContentDigestAlgorithm) + " contents digest does not match the digest specified by a preceding signer");
            }
            ByteBuffer lengthPrefixedSlice6 = ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice);
            ArrayList arrayList3 = new ArrayList();
            int i6 = 0;
            while (lengthPrefixedSlice6.hasRemaining()) {
                i6++;
                byte[] lengthPrefixedByteArray4 = ApkSigningBlockUtils.readLengthPrefixedByteArray(lengthPrefixedSlice6);
                try {
                    arrayList3.add(new VerbatimX509Certificate((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(lengthPrefixedByteArray4)), lengthPrefixedByteArray4));
                } catch (CertificateException e3) {
                    throw new SecurityException("Failed to decode certificate #" + i6, e3);
                }
            }
            if (arrayList3.isEmpty()) {
                throw new SecurityException("No certificates listed");
            }
            if (!Arrays.equals(lengthPrefixedByteArray, ((X509Certificate) arrayList3.get(0)).getPublicKey().getEncoded())) {
                throw new SecurityException("Public key mismatch between certificate and signature record");
            }
            verifyAdditionalAttributes(ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice));
            return (X509Certificate[]) arrayList3.toArray(new X509Certificate[arrayList3.size()]);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e4) {
            throw new SecurityException("Failed to verify " + str + " signature", e4);
        }
    }

    private static void verifyAdditionalAttributes(ByteBuffer byteBuffer) throws IOException, SecurityException {
        while (byteBuffer.hasRemaining()) {
            ByteBuffer lengthPrefixedSlice = ApkSigningBlockUtils.getLengthPrefixedSlice(byteBuffer);
            if (lengthPrefixedSlice.remaining() < 4) {
                throw new IOException("Remaining buffer too short to contain additional attribute ID. Remaining: " + lengthPrefixedSlice.remaining());
            }
            if (lengthPrefixedSlice.getInt() == STRIPPING_PROTECTION_ATTR_ID) {
                if (lengthPrefixedSlice.remaining() < 4) {
                    throw new IOException("V2 Signature Scheme Stripping Protection Attribute  value too small.  Expected 4 bytes, but found " + lengthPrefixedSlice.remaining());
                }
                if (lengthPrefixedSlice.getInt() == 3) {
                    throw new SecurityException("V2 signature indicates APK is signed using APK Signature Scheme v3, but none was found. Signature stripped?");
                }
            }
        }
    }

    static byte[] getVerityRootHash(String str) throws Exception {
        RandomAccessFile randomAccessFile = new RandomAccessFile(str, FullBackup.ROOT_TREE_TOKEN);
        try {
            findSignature(randomAccessFile);
            return verify(randomAccessFile, false).verityRootHash;
        } finally {
            $closeResource(null, randomAccessFile);
        }
    }

    static byte[] generateApkVerity(String str, ByteBufferFactory byteBufferFactory) throws Exception {
        RandomAccessFile randomAccessFile = new RandomAccessFile(str, FullBackup.ROOT_TREE_TOKEN);
        try {
            return ApkSigningBlockUtils.generateApkVerity(str, byteBufferFactory, findSignature(randomAccessFile));
        } finally {
            $closeResource(null, randomAccessFile);
        }
    }

    static byte[] generateFsverityRootHash(String str) throws Exception {
        RandomAccessFile randomAccessFile = new RandomAccessFile(str, FullBackup.ROOT_TREE_TOKEN);
        Throwable th = null;
        try {
            SignatureInfo signatureInfoFindSignature = findSignature(randomAccessFile);
            VerifiedSigner verifiedSignerVerify = verify(randomAccessFile, false);
            if (verifiedSignerVerify.verityRootHash == null) {
                return null;
            }
            return ApkVerityBuilder.generateFsverityRootHash(randomAccessFile, ByteBuffer.wrap(verifiedSignerVerify.verityRootHash), signatureInfoFindSignature);
        } finally {
            $closeResource(th, randomAccessFile);
        }
        $closeResource(th, randomAccessFile);
    }

    private static boolean isSupportedSignatureAlgorithm(int i) {
        if (i == 769 || i == 1057 || i == 1059 || i == 1061) {
            return true;
        }
        switch (i) {
            case 257:
            case 258:
            case 259:
            case 260:
                return true;
            default:
                switch (i) {
                    case 513:
                    case 514:
                        return true;
                    default:
                        return false;
                }
        }
    }

    public static class VerifiedSigner {
        public final X509Certificate[][] certs;
        public final byte[] verityRootHash;

        public VerifiedSigner(X509Certificate[][] x509CertificateArr, byte[] bArr) {
            this.certs = x509CertificateArr;
            this.verityRootHash = bArr;
        }
    }
}
