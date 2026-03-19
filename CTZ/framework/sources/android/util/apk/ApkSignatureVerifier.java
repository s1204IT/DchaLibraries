package android.util.apk;

import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.os.Trace;
import android.util.apk.ApkSignatureSchemeV3Verifier;
import android.util.jar.StrictJarFile;
import com.android.internal.util.ArrayUtils;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import libcore.io.IoUtils;

public class ApkSignatureVerifier {
    private static final AtomicReference<byte[]> sBuffer = new AtomicReference<>();

    public static PackageParser.SigningDetails verify(String str, @PackageParser.SigningDetails.SignatureSchemeVersion int i) throws PackageParser.PackageParserException {
        int[] iArr;
        if (i > 3) {
            throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No signature found in package of version " + i + " or newer for package " + str);
        }
        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "verifyV3");
        try {
            try {
                ApkSignatureSchemeV3Verifier.VerifiedSigner verifiedSignerVerify = ApkSignatureSchemeV3Verifier.verify(str);
                Signature[] signatureArrConvertToSignatures = convertToSignatures(new Certificate[][]{verifiedSignerVerify.certs});
                Signature[] signatureArr = null;
                if (verifiedSignerVerify.por != null) {
                    signatureArr = new Signature[verifiedSignerVerify.por.certs.size()];
                    iArr = new int[verifiedSignerVerify.por.flagsList.size()];
                    for (int i2 = 0; i2 < signatureArr.length; i2++) {
                        signatureArr[i2] = new Signature(verifiedSignerVerify.por.certs.get(i2).getEncoded());
                        iArr[i2] = verifiedSignerVerify.por.flagsList.get(i2).intValue();
                    }
                } else {
                    iArr = null;
                }
                PackageParser.SigningDetails signingDetails = new PackageParser.SigningDetails(signatureArrConvertToSignatures, 3, signatureArr, iArr);
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                return signingDetails;
            } catch (SignatureNotFoundException e) {
                if (i >= 3) {
                    throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No APK Signature Scheme v3 signature in package " + str, e);
                }
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                if (i > 2) {
                    throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No signature found in package of version " + i + " or newer for package " + str);
                }
                Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "verifyV2");
                try {
                    try {
                        try {
                            PackageParser.SigningDetails signingDetails2 = new PackageParser.SigningDetails(convertToSignatures(ApkSignatureSchemeV2Verifier.verify(str)), 2);
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                            return signingDetails2;
                        } catch (SignatureNotFoundException e2) {
                            if (i >= 2) {
                                throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No APK Signature Scheme v2 signature in package " + str, e2);
                            }
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                            if (i <= 1) {
                                return verifyV1Signature(str, true);
                            }
                            throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No signature found in package of version " + i + " or newer for package " + str);
                        }
                    } catch (Exception e3) {
                        throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + str + " using APK Signature Scheme v2", e3);
                    }
                } catch (Throwable th) {
                    Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                    throw th;
                }
            } catch (Exception e4) {
                throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + str + " using APK Signature Scheme v3", e4);
            }
        } catch (Throwable th2) {
            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
            throw th2;
        }
    }

    private static PackageParser.SigningDetails verifyV1Signature(String str, boolean z) throws Throwable {
        StrictJarFile strictJarFile;
        try {
            try {
                Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "strictJarFileCtor");
                strictJarFile = new StrictJarFile(str, true, z);
            } catch (Throwable th) {
                th = th;
                strictJarFile = null;
            }
        } catch (IOException | RuntimeException e) {
            e = e;
        } catch (GeneralSecurityException e2) {
            e = e2;
        }
        try {
            try {
                ArrayList<ZipEntry> arrayList = new ArrayList();
                ZipEntry zipEntryFindEntry = strictJarFile.findEntry(PackageParser.ANDROID_MANIFEST_FILENAME);
                if (zipEntryFindEntry == null) {
                    throw new PackageParser.PackageParserException(-101, "Package " + str + " has no manifest");
                }
                Certificate[][] certificateArrLoadCertificates = loadCertificates(strictJarFile, zipEntryFindEntry);
                if (ArrayUtils.isEmpty(certificateArrLoadCertificates)) {
                    throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Package " + str + " has no certificates at entry " + PackageParser.ANDROID_MANIFEST_FILENAME);
                }
                Signature[] signatureArrConvertToSignatures = convertToSignatures(certificateArrLoadCertificates);
                if (z) {
                    for (ZipEntry zipEntry : strictJarFile) {
                        if (!zipEntry.isDirectory()) {
                            String name = zipEntry.getName();
                            if (!name.startsWith("META-INF/") && !name.equals(PackageParser.ANDROID_MANIFEST_FILENAME)) {
                                arrayList.add(zipEntry);
                            }
                        }
                    }
                    for (ZipEntry zipEntry2 : arrayList) {
                        Certificate[][] certificateArrLoadCertificates2 = loadCertificates(strictJarFile, zipEntry2);
                        if (ArrayUtils.isEmpty(certificateArrLoadCertificates2)) {
                            throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Package " + str + " has no certificates at entry " + zipEntry2.getName());
                        }
                        if (!Signature.areExactMatch(signatureArrConvertToSignatures, convertToSignatures(certificateArrLoadCertificates2))) {
                            throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES, "Package " + str + " has mismatched certificates at entry " + zipEntry2.getName());
                        }
                    }
                }
                PackageParser.SigningDetails signingDetails = new PackageParser.SigningDetails(signatureArrConvertToSignatures, 1);
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                closeQuietly(strictJarFile);
                return signingDetails;
            } catch (IOException | RuntimeException e3) {
                e = e3;
                throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + str, e);
            }
        } catch (GeneralSecurityException e4) {
            e = e4;
            throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING, "Failed to collect certificates from " + str, e);
        } catch (Throwable th2) {
            th = th2;
            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
            closeQuietly(strictJarFile);
            throw th;
        }
    }

    private static Certificate[][] loadCertificates(StrictJarFile strictJarFile, ZipEntry zipEntry) throws Throwable {
        Throwable e;
        try {
            try {
                InputStream inputStream = strictJarFile.getInputStream(zipEntry);
                try {
                    readFullyIgnoringContents(inputStream);
                    Certificate[][] certificateChains = strictJarFile.getCertificateChains(zipEntry);
                    IoUtils.closeQuietly(inputStream);
                    return certificateChains;
                } catch (IOException | RuntimeException e2) {
                    e = e2;
                    throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed reading " + zipEntry.getName() + " in " + strictJarFile, e);
                }
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
        } catch (IOException | RuntimeException e3) {
            e = e3;
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    private static void readFullyIgnoringContents(InputStream inputStream) throws IOException {
        byte[] andSet = sBuffer.getAndSet(null);
        if (andSet == null) {
            andSet = new byte[4096];
        }
        while (inputStream.read(andSet, 0, andSet.length) != -1) {
        }
        sBuffer.set(andSet);
    }

    public static Signature[] convertToSignatures(Certificate[][] certificateArr) throws CertificateEncodingException {
        Signature[] signatureArr = new Signature[certificateArr.length];
        for (int i = 0; i < certificateArr.length; i++) {
            signatureArr[i] = new Signature(certificateArr[i]);
        }
        return signatureArr;
    }

    private static void closeQuietly(StrictJarFile strictJarFile) {
        if (strictJarFile != null) {
            try {
                strictJarFile.close();
            } catch (Exception e) {
            }
        }
    }

    public static PackageParser.SigningDetails plsCertsNoVerifyOnlyCerts(String str, int i) throws PackageParser.PackageParserException {
        int[] iArr;
        if (i > 3) {
            throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No signature found in package of version " + i + " or newer for package " + str);
        }
        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "certsOnlyV3");
        try {
            try {
                ApkSignatureSchemeV3Verifier.VerifiedSigner verifiedSignerPlsCertsNoVerifyOnlyCerts = ApkSignatureSchemeV3Verifier.plsCertsNoVerifyOnlyCerts(str);
                Signature[] signatureArrConvertToSignatures = convertToSignatures(new Certificate[][]{verifiedSignerPlsCertsNoVerifyOnlyCerts.certs});
                Signature[] signatureArr = null;
                if (verifiedSignerPlsCertsNoVerifyOnlyCerts.por != null) {
                    signatureArr = new Signature[verifiedSignerPlsCertsNoVerifyOnlyCerts.por.certs.size()];
                    iArr = new int[verifiedSignerPlsCertsNoVerifyOnlyCerts.por.flagsList.size()];
                    for (int i2 = 0; i2 < signatureArr.length; i2++) {
                        signatureArr[i2] = new Signature(verifiedSignerPlsCertsNoVerifyOnlyCerts.por.certs.get(i2).getEncoded());
                        iArr[i2] = verifiedSignerPlsCertsNoVerifyOnlyCerts.por.flagsList.get(i2).intValue();
                    }
                } else {
                    iArr = null;
                }
                PackageParser.SigningDetails signingDetails = new PackageParser.SigningDetails(signatureArrConvertToSignatures, 3, signatureArr, iArr);
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                return signingDetails;
            } catch (SignatureNotFoundException e) {
                if (i >= 3) {
                    throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No APK Signature Scheme v3 signature in package " + str, e);
                }
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                if (i > 2) {
                    throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No signature found in package of version " + i + " or newer for package " + str);
                }
                Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "certsOnlyV2");
                try {
                    try {
                        try {
                            PackageParser.SigningDetails signingDetails2 = new PackageParser.SigningDetails(convertToSignatures(ApkSignatureSchemeV2Verifier.plsCertsNoVerifyOnlyCerts(str)), 2);
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                            return signingDetails2;
                        } catch (SignatureNotFoundException e2) {
                            if (i >= 2) {
                                throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No APK Signature Scheme v2 signature in package " + str, e2);
                            }
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                            if (i <= 1) {
                                return verifyV1Signature(str, false);
                            }
                            throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "No signature found in package of version " + i + " or newer for package " + str);
                        }
                    } catch (Exception e3) {
                        throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + str + " using APK Signature Scheme v2", e3);
                    }
                } catch (Throwable th) {
                    Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                    throw th;
                }
            } catch (Exception e4) {
                throw new PackageParser.PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + str + " using APK Signature Scheme v3", e4);
            }
        } catch (Throwable th2) {
            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
            throw th2;
        }
    }

    public static byte[] getVerityRootHash(String str) throws SignatureNotFoundException, IOException, SecurityException {
        try {
            return ApkSignatureSchemeV3Verifier.getVerityRootHash(str);
        } catch (SignatureNotFoundException e) {
            return ApkSignatureSchemeV2Verifier.getVerityRootHash(str);
        }
    }

    public static byte[] generateApkVerity(String str, ByteBufferFactory byteBufferFactory) throws SignatureNotFoundException, NoSuchAlgorithmException, DigestException, IOException, SecurityException {
        try {
            return ApkSignatureSchemeV3Verifier.generateApkVerity(str, byteBufferFactory);
        } catch (SignatureNotFoundException e) {
            return ApkSignatureSchemeV2Verifier.generateApkVerity(str, byteBufferFactory);
        }
    }

    public static byte[] generateFsverityRootHash(String str) throws NoSuchAlgorithmException, DigestException, IOException {
        try {
            return ApkSignatureSchemeV3Verifier.generateFsverityRootHash(str);
        } catch (SignatureNotFoundException e) {
            try {
                return ApkSignatureSchemeV2Verifier.generateFsverityRootHash(str);
            } catch (SignatureNotFoundException e2) {
                return null;
            }
        }
    }

    public static class Result {
        public final Certificate[][] certs;
        public final int signatureSchemeVersion;
        public final Signature[] sigs;

        public Result(Certificate[][] certificateArr, Signature[] signatureArr, int i) {
            this.certs = certificateArr;
            this.sigs = signatureArr;
            this.signatureSchemeVersion = i;
        }
    }
}
