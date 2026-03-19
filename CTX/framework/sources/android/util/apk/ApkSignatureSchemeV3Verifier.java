package android.util.apk;

import android.app.backup.FullBackup;
import android.os.Build;
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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ApkSignatureSchemeV3Verifier {
    private static final int APK_SIGNATURE_SCHEME_V3_BLOCK_ID = -262969152;
    private static final int PROOF_OF_ROTATION_ATTR_ID = 1000370060;
    public static final int SF_ATTRIBUTE_ANDROID_APK_SIGNED_ID = 3;

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

    public static VerifiedSigner verify(String str) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(str, true);
    }

    public static VerifiedSigner plsCertsNoVerifyOnlyCerts(String str) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(str, false);
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
        return ApkSigningBlockUtils.findSignature(randomAccessFile, APK_SIGNATURE_SCHEME_V3_BLOCK_ID);
    }

    private static VerifiedSigner verify(RandomAccessFile randomAccessFile, SignatureInfo signatureInfo, boolean z) throws IOException, SecurityException, PlatformNotSupportedException {
        ArrayMap arrayMap = new ArrayMap();
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            try {
                ByteBuffer lengthPrefixedSlice = ApkSigningBlockUtils.getLengthPrefixedSlice(signatureInfo.signatureBlock);
                int i = 0;
                VerifiedSigner verifiedSignerVerifySigner = null;
                while (lengthPrefixedSlice.hasRemaining()) {
                    try {
                        i++;
                        verifiedSignerVerifySigner = verifySigner(ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice), arrayMap, certificateFactory);
                    } catch (PlatformNotSupportedException e) {
                    } catch (IOException | SecurityException | BufferUnderflowException e2) {
                        throw new SecurityException("Failed to parse/verify signer #" + i + " block", e2);
                    }
                }
                if (i < 1 || verifiedSignerVerifySigner == null) {
                    throw new SecurityException("No signers found");
                }
                if (i != 1) {
                    throw new SecurityException("APK Signature Scheme V3 only supports one signer: multiple signers found.");
                }
                if (arrayMap.isEmpty()) {
                    throw new SecurityException("No content digests found");
                }
                if (z) {
                    ApkSigningBlockUtils.verifyIntegrity(arrayMap, randomAccessFile, signatureInfo);
                }
                if (arrayMap.containsKey(3)) {
                    verifiedSignerVerifySigner.verityRootHash = ApkSigningBlockUtils.parseVerityDigestAndVerifySourceLength((byte[]) arrayMap.get(3), randomAccessFile.length(), signatureInfo);
                }
                return verifiedSignerVerifySigner;
            } catch (IOException e3) {
                throw new SecurityException("Failed to read list of signers", e3);
            }
        } catch (CertificateException e4) {
            throw new RuntimeException("Failed to obtain X.509 CertificateFactory", e4);
        }
    }

    private static VerifiedSigner verifySigner(ByteBuffer byteBuffer, Map<Integer, byte[]> map, CertificateFactory certificateFactory) throws IOException, SecurityException, PlatformNotSupportedException {
        ByteBuffer lengthPrefixedSlice = ApkSigningBlockUtils.getLengthPrefixedSlice(byteBuffer);
        int i = byteBuffer.getInt();
        int i2 = byteBuffer.getInt();
        if (Build.VERSION.SDK_INT < i || Build.VERSION.SDK_INT > i2) {
            throw new PlatformNotSupportedException("Signer not supported by this platform version. This platform: " + Build.VERSION.SDK_INT + ", signer minSdkVersion: " + i + ", maxSdkVersion: " + i2);
        }
        ByteBuffer lengthPrefixedSlice2 = ApkSigningBlockUtils.getLengthPrefixedSlice(byteBuffer);
        byte[] lengthPrefixedByteArray = ApkSigningBlockUtils.readLengthPrefixedByteArray(byteBuffer);
        ArrayList arrayList = new ArrayList();
        byte[] lengthPrefixedByteArray2 = null;
        int i3 = -1;
        int i4 = 0;
        while (lengthPrefixedSlice2.hasRemaining()) {
            i4++;
            try {
                ByteBuffer lengthPrefixedSlice3 = ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice2);
                if (lengthPrefixedSlice3.remaining() < 8) {
                    throw new SecurityException("Signature record too short");
                }
                int i5 = lengthPrefixedSlice3.getInt();
                arrayList.add(Integer.valueOf(i5));
                if (isSupportedSignatureAlgorithm(i5)) {
                    if (i3 == -1 || ApkSigningBlockUtils.compareSignatureAlgorithm(i5, i3) > 0) {
                        lengthPrefixedByteArray2 = ApkSigningBlockUtils.readLengthPrefixedByteArray(lengthPrefixedSlice3);
                        i3 = i5;
                    }
                }
            } catch (IOException | BufferUnderflowException e) {
                throw new SecurityException("Failed to parse signature record #" + i4, e);
            }
        }
        if (i3 == -1) {
            if (i4 == 0) {
                throw new SecurityException("No signatures found");
            }
            throw new SecurityException("No supported signatures found");
        }
        String signatureAlgorithmJcaKeyAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmJcaKeyAlgorithm(i3);
        Pair<String, ? extends AlgorithmParameterSpec> signatureAlgorithmJcaSignatureAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmJcaSignatureAlgorithm(i3);
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
            int i6 = 0;
            while (lengthPrefixedSlice4.hasRemaining()) {
                i6++;
                try {
                    ByteBuffer lengthPrefixedSlice5 = ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice4);
                    if (lengthPrefixedSlice5.remaining() < 8) {
                        throw new IOException("Record too short");
                    }
                    int i7 = lengthPrefixedSlice5.getInt();
                    arrayList2.add(Integer.valueOf(i7));
                    if (i7 == i3) {
                        lengthPrefixedByteArray3 = ApkSigningBlockUtils.readLengthPrefixedByteArray(lengthPrefixedSlice5);
                    }
                } catch (IOException | BufferUnderflowException e2) {
                    throw new IOException("Failed to parse digest record #" + i6, e2);
                }
            }
            if (!arrayList.equals(arrayList2)) {
                throw new SecurityException("Signature algorithms don't match between digests and signatures records");
            }
            int signatureAlgorithmContentDigestAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmContentDigestAlgorithm(i3);
            byte[] bArrPut = map.put(Integer.valueOf(signatureAlgorithmContentDigestAlgorithm), lengthPrefixedByteArray3);
            if (bArrPut != null && !MessageDigest.isEqual(bArrPut, lengthPrefixedByteArray3)) {
                throw new SecurityException(ApkSigningBlockUtils.getContentDigestAlgorithmJcaDigestAlgorithm(signatureAlgorithmContentDigestAlgorithm) + " contents digest does not match the digest specified by a preceding signer");
            }
            ByteBuffer lengthPrefixedSlice6 = ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice);
            ArrayList arrayList3 = new ArrayList();
            int i8 = 0;
            while (lengthPrefixedSlice6.hasRemaining()) {
                i8++;
                byte[] lengthPrefixedByteArray4 = ApkSigningBlockUtils.readLengthPrefixedByteArray(lengthPrefixedSlice6);
                try {
                    arrayList3.add(new VerbatimX509Certificate((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(lengthPrefixedByteArray4)), lengthPrefixedByteArray4));
                } catch (CertificateException e3) {
                    throw new SecurityException("Failed to decode certificate #" + i8, e3);
                }
            }
            if (arrayList3.isEmpty()) {
                throw new SecurityException("No certificates listed");
            }
            if (!Arrays.equals(lengthPrefixedByteArray, ((X509Certificate) arrayList3.get(0)).getPublicKey().getEncoded())) {
                throw new SecurityException("Public key mismatch between certificate and signature record");
            }
            if (lengthPrefixedSlice.getInt() != i) {
                throw new SecurityException("minSdkVersion mismatch between signed and unsigned in v3 signer block.");
            }
            if (lengthPrefixedSlice.getInt() != i2) {
                throw new SecurityException("maxSdkVersion mismatch between signed and unsigned in v3 signer block.");
            }
            return verifyAdditionalAttributes(ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice), arrayList3, certificateFactory);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e4) {
            throw new SecurityException("Failed to verify " + str + " signature", e4);
        }
    }

    private static VerifiedSigner verifyAdditionalAttributes(ByteBuffer byteBuffer, List<X509Certificate> list, CertificateFactory certificateFactory) throws IOException {
        X509Certificate[] x509CertificateArr = (X509Certificate[]) list.toArray(new X509Certificate[list.size()]);
        VerifiedProofOfRotation verifiedProofOfRotationVerifyProofOfRotationStruct = null;
        while (byteBuffer.hasRemaining()) {
            ByteBuffer lengthPrefixedSlice = ApkSigningBlockUtils.getLengthPrefixedSlice(byteBuffer);
            if (lengthPrefixedSlice.remaining() < 4) {
                throw new IOException("Remaining buffer too short to contain additional attribute ID. Remaining: " + lengthPrefixedSlice.remaining());
            }
            if (lengthPrefixedSlice.getInt() == PROOF_OF_ROTATION_ATTR_ID) {
                if (verifiedProofOfRotationVerifyProofOfRotationStruct != null) {
                    throw new SecurityException("Encountered multiple Proof-of-rotation records when verifying APK Signature Scheme v3 signature");
                }
                verifiedProofOfRotationVerifyProofOfRotationStruct = verifyProofOfRotationStruct(lengthPrefixedSlice, certificateFactory);
                try {
                    if (verifiedProofOfRotationVerifyProofOfRotationStruct.certs.size() > 0 && !Arrays.equals(verifiedProofOfRotationVerifyProofOfRotationStruct.certs.get(verifiedProofOfRotationVerifyProofOfRotationStruct.certs.size() - 1).getEncoded(), x509CertificateArr[0].getEncoded())) {
                        throw new SecurityException("Terminal certificate in Proof-of-rotation record does not match APK signing certificate");
                    }
                } catch (CertificateEncodingException e) {
                    throw new SecurityException("Failed to encode certificate when comparing Proof-of-rotation record and signing certificate", e);
                }
            }
        }
        return new VerifiedSigner(x509CertificateArr, verifiedProofOfRotationVerifyProofOfRotationStruct);
    }

    private static VerifiedProofOfRotation verifyProofOfRotationStruct(ByteBuffer byteBuffer, CertificateFactory certificateFactory) throws IOException, SecurityException {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        int i = 0;
        try {
            byteBuffer.getInt();
            HashSet hashSet = new HashSet();
            int i2 = -1;
            VerbatimX509Certificate verbatimX509Certificate = null;
            while (byteBuffer.hasRemaining()) {
                i++;
                ByteBuffer lengthPrefixedSlice = ApkSigningBlockUtils.getLengthPrefixedSlice(byteBuffer);
                ByteBuffer lengthPrefixedSlice2 = ApkSigningBlockUtils.getLengthPrefixedSlice(lengthPrefixedSlice);
                int i3 = lengthPrefixedSlice.getInt();
                int i4 = lengthPrefixedSlice.getInt();
                byte[] lengthPrefixedByteArray = ApkSigningBlockUtils.readLengthPrefixedByteArray(lengthPrefixedSlice);
                if (verbatimX509Certificate != null) {
                    Pair<String, ? extends AlgorithmParameterSpec> signatureAlgorithmJcaSignatureAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmJcaSignatureAlgorithm(i2);
                    PublicKey publicKey = verbatimX509Certificate.getPublicKey();
                    Signature signature = Signature.getInstance(signatureAlgorithmJcaSignatureAlgorithm.first);
                    signature.initVerify(publicKey);
                    if (signatureAlgorithmJcaSignatureAlgorithm.second != 0) {
                        signature.setParameter((AlgorithmParameterSpec) signatureAlgorithmJcaSignatureAlgorithm.second);
                    }
                    signature.update(lengthPrefixedSlice2);
                    if (!signature.verify(lengthPrefixedByteArray)) {
                        throw new SecurityException("Unable to verify signature of certificate #" + i + " using " + signatureAlgorithmJcaSignatureAlgorithm.first + " when verifying Proof-of-rotation record");
                    }
                }
                lengthPrefixedSlice2.rewind();
                byte[] lengthPrefixedByteArray2 = ApkSigningBlockUtils.readLengthPrefixedByteArray(lengthPrefixedSlice2);
                int i5 = lengthPrefixedSlice2.getInt();
                if (verbatimX509Certificate != null && i2 != i5) {
                    throw new SecurityException("Signing algorithm ID mismatch for certificate #" + i + " when verifying Proof-of-rotation record");
                }
                verbatimX509Certificate = new VerbatimX509Certificate((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(lengthPrefixedByteArray2)), lengthPrefixedByteArray2);
                if (hashSet.contains(verbatimX509Certificate)) {
                    throw new SecurityException("Encountered duplicate entries in Proof-of-rotation record at certificate #" + i + ".  All signing certificates should be unique");
                }
                hashSet.add(verbatimX509Certificate);
                arrayList.add(verbatimX509Certificate);
                arrayList2.add(Integer.valueOf(i3));
                i2 = i4;
            }
            return new VerifiedProofOfRotation(arrayList, arrayList2);
        } catch (IOException | BufferUnderflowException e) {
            throw new IOException("Failed to parse Proof-of-rotation record", e);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | SignatureException e2) {
            throw new SecurityException("Failed to verify signature over signed data for certificate #0 when verifying Proof-of-rotation record", e2);
        } catch (CertificateException e3) {
            throw new SecurityException("Failed to decode certificate #0 when verifying Proof-of-rotation record", e3);
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

    public static class VerifiedProofOfRotation {
        public final List<X509Certificate> certs;
        public final List<Integer> flagsList;

        public VerifiedProofOfRotation(List<X509Certificate> list, List<Integer> list2) {
            this.certs = list;
            this.flagsList = list2;
        }
    }

    public static class VerifiedSigner {
        public final X509Certificate[] certs;
        public final VerifiedProofOfRotation por;
        public byte[] verityRootHash;

        public VerifiedSigner(X509Certificate[] x509CertificateArr, VerifiedProofOfRotation verifiedProofOfRotation) {
            this.certs = x509CertificateArr;
            this.por = verifiedProofOfRotation;
        }
    }

    private static class PlatformNotSupportedException extends Exception {
        PlatformNotSupportedException(String str) {
            super(str);
        }
    }
}
