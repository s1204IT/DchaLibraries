package com.android.server.wifi.hotspot2;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

public class WfaCertBuilder {
    private static final String TAG = "WfaCertBuilder";

    public static Set<X509Certificate> loadCertsFromDisk(String str) {
        File[] fileArrListFiles;
        HashSet hashSet = new HashSet();
        try {
            fileArrListFiles = new File(str).listFiles();
        } catch (IOException | SecurityException | CertificateException e) {
            Log.e(TAG, "Unable to read cert " + e.getMessage());
        }
        if (fileArrListFiles != null && fileArrListFiles.length > 0) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            for (File file : fileArrListFiles) {
                FileInputStream fileInputStream = new FileInputStream(file);
                Certificate certificateGenerateCertificate = certificateFactory.generateCertificate(fileInputStream);
                if (certificateGenerateCertificate instanceof X509Certificate) {
                    hashSet.add((X509Certificate) certificateGenerateCertificate);
                }
                fileInputStream.close();
            }
            return hashSet;
        }
        return hashSet;
    }
}
