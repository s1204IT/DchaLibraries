package android.security;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import android.util.Log;
import com.android.org.bouncycastle.util.io.pem.PemObject;
import com.android.org.bouncycastle.util.io.pem.PemReader;
import com.android.org.bouncycastle.util.io.pem.PemWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class Credentials {
    public static final String CA_CERTIFICATE = "CACERT_";
    public static final String EXTENSION_CER = ".cer";
    public static final String EXTENSION_CRT = ".crt";
    public static final String EXTENSION_P12 = ".p12";
    public static final String EXTENSION_PFX = ".pfx";
    public static final String EXTRA_CA_CERTIFICATES_DATA = "ca_certificates_data";
    public static final String EXTRA_CA_CERTIFICATES_NAME = "ca_certificates_name";
    public static final String EXTRA_INSTALL_AS_UID = "install_as_uid";
    public static final String EXTRA_PRIVATE_KEY = "PKEY";
    public static final String EXTRA_PUBLIC_KEY = "KEY";
    public static final String EXTRA_USER_CERTIFICATE_DATA = "user_certificate_data";
    public static final String EXTRA_USER_CERTIFICATE_NAME = "user_certificate_name";
    public static final String EXTRA_USER_PRIVATE_KEY_DATA = "user_private_key_data";
    public static final String EXTRA_USER_PRIVATE_KEY_NAME = "user_private_key_name";
    public static final String EXTRA_WAPI_SERVER_CERTIFICATE_DATA = "wapi_server_certificate_data";
    public static final String EXTRA_WAPI_SERVER_CERTIFICATE_NAME = "wapi_server_certificate_name";
    public static final String EXTRA_WAPI_USER_CERTIFICATE_DATA = "wapi_user_certificate_data";
    public static final String EXTRA_WAPI_USER_CERTIFICATE_NAME = "wapi_user_certificate_name";
    public static final String INSTALL_ACTION = "android.credentials.INSTALL";
    public static final String INSTALL_AS_USER_ACTION = "android.credentials.INSTALL_AS_USER";
    public static final String LOCKDOWN_VPN = "LOCKDOWN_VPN";
    private static final String LOGTAG = "Credentials";
    public static final String UNLOCK_ACTION = "com.android.credentials.UNLOCK";
    public static final String USER_CERTIFICATE = "USRCERT_";
    public static final String USER_PRIVATE_KEY = "USRPKEY_";
    public static final String USER_SECRET_KEY = "USRSKEY_";
    public static final String VPN = "VPN_";
    public static final String WAPI_SERVER_CERTIFICATE = "WAPISERVERCERT_";
    public static final String WAPI_USER_CERTIFICATE = "WAPIUSERCERT_";
    public static final String WIFI = "WIFI_";
    private static Credentials singleton;

    public static byte[] convertToPem(Certificate... certificateArr) throws IOException, CertificateEncodingException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PemWriter pemWriter = new PemWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.US_ASCII));
        for (Certificate certificate : certificateArr) {
            pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
        }
        pemWriter.close();
        return byteArrayOutputStream.toByteArray();
    }

    public static List<X509Certificate> convertFromPem(byte[] bArr) throws IOException, CertificateException {
        PemReader pemReader = new PemReader(new InputStreamReader(new ByteArrayInputStream(bArr), StandardCharsets.US_ASCII));
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            ArrayList arrayList = new ArrayList();
            while (true) {
                PemObject pemObject = pemReader.readPemObject();
                if (pemObject != null) {
                    if (pemObject.getType().equals("CERTIFICATE")) {
                        arrayList.add((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(pemObject.getContent())));
                    } else {
                        throw new IllegalArgumentException("Unknown type " + pemObject.getType());
                    }
                } else {
                    return arrayList;
                }
            }
        } finally {
            pemReader.close();
        }
    }

    public static Credentials getInstance() {
        if (singleton == null) {
            singleton = new Credentials();
        }
        return singleton;
    }

    public void unlock(Context context) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        try {
            context.startActivity(new Intent(UNLOCK_ACTION));
        } catch (ActivityNotFoundException e) {
            Log.w(LOGTAG, e.toString());
        }
    }

    public void install(Context context) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        try {
            context.startActivity(KeyChain.createInstallIntent());
        } catch (ActivityNotFoundException e) {
            Log.w(LOGTAG, e.toString());
        }
    }

    public void install(Context context, KeyPair keyPair) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        try {
            Intent intentCreateInstallIntent = KeyChain.createInstallIntent();
            intentCreateInstallIntent.putExtra(EXTRA_PRIVATE_KEY, keyPair.getPrivate().getEncoded());
            intentCreateInstallIntent.putExtra(EXTRA_PUBLIC_KEY, keyPair.getPublic().getEncoded());
            context.startActivity(intentCreateInstallIntent);
        } catch (ActivityNotFoundException e) {
            Log.w(LOGTAG, e.toString());
        }
    }

    public void install(Context context, String str, byte[] bArr) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        try {
            Intent intentCreateInstallIntent = KeyChain.createInstallIntent();
            intentCreateInstallIntent.putExtra(str, bArr);
            context.startActivity(intentCreateInstallIntent);
        } catch (ActivityNotFoundException e) {
            Log.w(LOGTAG, e.toString());
        }
    }

    public static boolean deleteAllTypesForAlias(KeyStore keyStore, String str) {
        return deleteAllTypesForAlias(keyStore, str, -1);
    }

    public static boolean deleteAllTypesForAlias(KeyStore keyStore, String str, int i) {
        return deleteCertificateTypesForAlias(keyStore, str, i) & deleteUserKeyTypeForAlias(keyStore, str, i);
    }

    public static boolean deleteCertificateTypesForAlias(KeyStore keyStore, String str) {
        return deleteCertificateTypesForAlias(keyStore, str, -1);
    }

    public static boolean deleteCertificateTypesForAlias(KeyStore keyStore, String str, int i) {
        return keyStore.delete(CA_CERTIFICATE + str, i) & keyStore.delete(USER_CERTIFICATE + str, i);
    }

    public static boolean deleteUserKeyTypeForAlias(KeyStore keyStore, String str) {
        return deleteUserKeyTypeForAlias(keyStore, str, -1);
    }

    public static boolean deleteUserKeyTypeForAlias(KeyStore keyStore, String str, int i) {
        if (!keyStore.delete(USER_PRIVATE_KEY + str, i)) {
            if (!keyStore.delete(USER_SECRET_KEY + str, i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean deleteLegacyKeyForAlias(KeyStore keyStore, String str, int i) {
        return keyStore.delete(USER_SECRET_KEY + str, i);
    }
}
