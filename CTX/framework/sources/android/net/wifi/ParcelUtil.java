package android.net.wifi;

import android.os.Parcel;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class ParcelUtil {
    public static void writePrivateKey(Parcel parcel, PrivateKey privateKey) {
        if (privateKey == null) {
            parcel.writeString(null);
        } else {
            parcel.writeString(privateKey.getAlgorithm());
            parcel.writeByteArray(privateKey.getEncoded());
        }
    }

    public static PrivateKey readPrivateKey(Parcel parcel) {
        String string = parcel.readString();
        if (string == null) {
            return null;
        }
        try {
            return KeyFactory.getInstance(string).generatePrivate(new PKCS8EncodedKeySpec(parcel.createByteArray()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return null;
        }
    }

    public static void writeCertificate(Parcel parcel, X509Certificate x509Certificate) {
        byte[] encoded;
        if (x509Certificate != null) {
            try {
                encoded = x509Certificate.getEncoded();
            } catch (CertificateEncodingException e) {
                encoded = null;
            }
        } else {
            encoded = null;
        }
        parcel.writeByteArray(encoded);
    }

    public static X509Certificate readCertificate(Parcel parcel) {
        byte[] bArrCreateByteArray = parcel.createByteArray();
        if (bArrCreateByteArray == null) {
            return null;
        }
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArrCreateByteArray));
        } catch (CertificateException e) {
            return null;
        }
    }

    public static void writeCertificates(Parcel parcel, X509Certificate[] x509CertificateArr) {
        if (x509CertificateArr == null || x509CertificateArr.length == 0) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(x509CertificateArr.length);
        for (X509Certificate x509Certificate : x509CertificateArr) {
            writeCertificate(parcel, x509Certificate);
        }
    }

    public static X509Certificate[] readCertificates(Parcel parcel) {
        int i = parcel.readInt();
        if (i == 0) {
            return null;
        }
        X509Certificate[] x509CertificateArr = new X509Certificate[i];
        for (int i2 = 0; i2 < i; i2++) {
            x509CertificateArr[i2] = readCertificate(parcel);
        }
        return x509CertificateArr;
    }
}
