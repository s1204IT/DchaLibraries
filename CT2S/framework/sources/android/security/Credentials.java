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
import java.io.Reader;
import java.io.Writer;
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
    public static final String INSTALL_ACTION = "android.credentials.INSTALL";
    public static final String INSTALL_AS_USER_ACTION = "android.credentials.INSTALL_AS_USER";
    public static final String LOCKDOWN_VPN = "LOCKDOWN_VPN";
    private static final String LOGTAG = "Credentials";
    public static final String UNLOCK_ACTION = "com.android.credentials.UNLOCK";
    public static final String USER_CERTIFICATE = "USRCERT_";
    public static final String USER_PRIVATE_KEY = "USRPKEY_";
    public static final String VPN = "VPN_";
    public static final String WIFI = "WIFI_";
    private static Credentials singleton;

    public static byte[] convertToPem(Certificate... objects) throws IOException, CertificateEncodingException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(bao, StandardCharsets.US_ASCII);
        PemWriter pw = new PemWriter(writer);
        for (Certificate o : objects) {
            pw.writeObject(new PemObject("CERTIFICATE", o.getEncoded()));
        }
        pw.close();
        return bao.toByteArray();
    }

    public static List<X509Certificate> convertFromPem(byte[] bytes) throws IOException, CertificateException {
        ByteArrayInputStream bai = new ByteArrayInputStream(bytes);
        Reader reader = new InputStreamReader(bai, StandardCharsets.US_ASCII);
        PemReader pr = new PemReader(reader);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        List<X509Certificate> result = new ArrayList<>();
        while (true) {
            PemObject o = pr.readPemObject();
            if (o != null) {
                if (o.getType().equals("CERTIFICATE")) {
                    Certificate c = cf.generateCertificate(new ByteArrayInputStream(o.getContent()));
                    result.add((X509Certificate) c);
                } else {
                    throw new IllegalArgumentException("Unknown type " + o.getType());
                }
            } else {
                pr.close();
                return result;
            }
        }
    }

    public static Credentials getInstance() {
        if (singleton == null) {
            singleton = new Credentials();
        }
        return singleton;
    }

    public void unlock(Context context) {
        if (BenesseExtension.getDchaState() == 0) {
            try {
                Intent intent = new Intent(UNLOCK_ACTION);
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w(LOGTAG, e.toString());
            }
        }
    }

    public void install(Context context) {
        try {
            Intent intent = KeyChain.createInstallIntent();
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(LOGTAG, e.toString());
        }
    }

    public void install(Context context, KeyPair pair) {
        try {
            Intent intent = KeyChain.createInstallIntent();
            intent.putExtra(EXTRA_PRIVATE_KEY, pair.getPrivate().getEncoded());
            intent.putExtra(EXTRA_PUBLIC_KEY, pair.getPublic().getEncoded());
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(LOGTAG, e.toString());
        }
    }

    public void install(Context context, String type, byte[] value) {
        try {
            Intent intent = KeyChain.createInstallIntent();
            intent.putExtra(type, value);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(LOGTAG, e.toString());
        }
    }

    static boolean deleteAllTypesForAlias(KeyStore keystore, String alias) {
        return keystore.delKey(USER_PRIVATE_KEY + alias) | deleteCertificateTypesForAlias(keystore, alias);
    }

    static boolean deleteCertificateTypesForAlias(KeyStore keystore, String alias) {
        return keystore.delete(USER_CERTIFICATE + alias) | keystore.delete(CA_CERTIFICATE + alias);
    }
}
