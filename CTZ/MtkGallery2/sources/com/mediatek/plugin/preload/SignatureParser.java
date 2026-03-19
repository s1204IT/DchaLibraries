package com.mediatek.plugin.preload;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.TraceHelper;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SignatureParser {
    public static final int SIGNATURE_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -2;
    public static final int SIGNATURE_PARSE_FAILED_NO_CERTIFICATE = -1;
    public static final int SIGNATURE_PARSE_FAILED_UNEXCEPT_EXCEPTION = -3;
    private static final String TAG = "PluginManager/SignatureParser";
    private static AtomicReference<byte[]> sBuffer = new AtomicReference<>();

    private static Signature[] convertToSignatures(Certificate[] certificateArr) {
        TraceHelper.beginSection(">>>>SignatureParser-convertToSignatures");
        Signature[] signatureArr = new Signature[certificateArr.length];
        for (int i = 0; i < certificateArr.length; i++) {
            try {
                signatureArr[i] = new Signature(certificateArr[i].getEncoded());
            } catch (CertificateEncodingException e) {
                Log.e(TAG, "<convertToSignatures>", e);
            }
        }
        TraceHelper.endSection();
        return signatureArr;
    }

    private static Signature[] convertToSignatures(Certificate[][] certificateArr) {
        Signature[] signatureArr = new Signature[certificateArr.length];
        for (int i = 0; i < certificateArr.length; i++) {
            try {
                signatureArr[i] = new Signature(certificateArr[i][0].getEncoded());
            } catch (CertificateEncodingException e) {
                Log.e(TAG, "<convertToSignatures>", e);
            }
        }
        return signatureArr;
    }

    private static Certificate[] loadCertificates(JarFile jarFile, JarEntry jarEntry, byte[] bArr) {
        TraceHelper.beginSection(">>>>SignatureParser-loadCertificates");
        try {
            TraceHelper.beginSection(">>>>SignatureParser-loadCertificates-getInputStream");
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>SignatureParser-loadCertificates-read");
            while (inputStream.read(bArr, 0, bArr.length) != -1) {
            }
            TraceHelper.endSection();
            inputStream.close();
            TraceHelper.endSection();
            return jarEntry.getCertificates();
        } catch (IOException e) {
            Log.e(TAG, "<loadCertificates>", e);
            TraceHelper.endSection();
            return null;
        }
    }

    public static Signature[] parseSignature(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 64).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "<parseSignature> Faild to get signature of package " + context.getPackageName(), e);
            return null;
        }
    }

    public static Signature[] parseSignature(JarFile jarFile) {
        Enumeration<JarEntry> enumerationEntries = jarFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            TraceHelper.beginSection(">>>>SignatureParser-parseSignature-while");
            JarEntry jarEntryNextElement = enumerationEntries.nextElement();
            if (!jarEntryNextElement.isDirectory() && !jarEntryNextElement.getName().startsWith("META-INF/")) {
                Certificate[] certificateArrLoadCertificates = loadCertificates(jarFile, jarEntryNextElement, new byte[8192]);
                if (certificateArrLoadCertificates == null) {
                    Log.d(TAG, "<parseSignature> certs = null");
                } else {
                    Signature[] signatureArrConvertToSignatures = convertToSignatures(certificateArrLoadCertificates);
                    if (signatureArrConvertToSignatures != null) {
                        TraceHelper.endSection();
                        Log.d(TAG, "<parseSignature> entry name = " + jarEntryNextElement.getName());
                        return signatureArrConvertToSignatures;
                    }
                    TraceHelper.endSection();
                }
            }
        }
        return null;
    }

    private static long readFullyIgnoringContents(InputStream inputStream) throws IOException {
        byte[] andSet = sBuffer.getAndSet(null);
        if (andSet == null) {
            andSet = new byte[4096];
        }
        int i = 0;
        while (true) {
            int i2 = inputStream.read(andSet, 0, andSet.length);
            if (i2 != -1) {
                i += i2;
            } else {
                sBuffer.set(andSet);
                return i;
            }
        }
    }
}
