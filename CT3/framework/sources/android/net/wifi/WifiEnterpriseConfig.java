package android.net.wifi;

import android.net.ProxyInfo;
import android.os.BatteryStats;
import android.os.Parcel;
import android.os.Parcelable;
import android.security.Credentials;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class WifiEnterpriseConfig implements Parcelable {
    public static final String CA_CERT2_PREFIX = "keystore://WAPISERVERCERT_";
    public static final String CA_CERT_ALIAS_DELIMITER = " ";
    public static final String CA_CERT_PREFIX = "keystore://CACERT_";
    private static final String CLIENT_CERT2_PREFIX = "keystore://WAPIUSERCERT_";
    public static final String CLIENT_CERT_PREFIX = "keystore://USRCERT_";
    public static final String EAP_KEY = "eap";
    public static final String EMPTY_VALUE = "NULL";
    public static final String ENGINE_DISABLE = "0";
    public static final String ENGINE_ENABLE = "1";
    public static final String ENGINE_ID_KEYSTORE = "keystore";
    public static final String ENGINE_KEY = "engine";
    public static final String KEYSTORES_URI = "keystores://";
    public static final String KEYSTORE_URI = "keystore://";
    public static final String OPP_KEY_CACHING = "proactive_key_caching";
    public static final String PASSWORD_KEY = "password";
    public static final String PHASE2_KEY = "phase2";
    public static final String PLMN_KEY = "plmn";
    public static final String REALM_KEY = "realm";
    private static final String TAG = "WifiEnterpriseConfig";
    public static final String USER_PRIVATE_KEY2_PREFIX = "keystore://WAPIUSERCERT_";
    private X509Certificate[] mCaCerts;
    private X509Certificate mClientCertificate;
    private PrivateKey mClientPrivateKey;
    private int mEapMethod;
    private HashMap<String, String> mFields;
    private int mPhase2Method;
    public static final String IDENTITY_KEY = "identity";
    public static final String ANON_IDENTITY_KEY = "anonymous_identity";
    public static final String CLIENT_CERT_KEY = "client_cert";
    public static final String CA_CERT_KEY = "ca_cert";
    public static final String CA_CERT2_KEY = "ca_cert2";
    public static final String SUBJECT_MATCH_KEY = "subject_match";
    public static final String ENGINE_ID_KEY = "engine_id";
    public static final String PRIVATE_KEY_ID_KEY = "key_id";
    public static final String ALTSUBJECT_MATCH_KEY = "altsubject_match";
    public static final String DOM_SUFFIX_MATCH_KEY = "domain_suffix_match";
    public static final String CA_PATH_KEY = "ca_path";
    private static final String[] SUPPLICANT_CONFIG_KEYS = {IDENTITY_KEY, ANON_IDENTITY_KEY, "password", CLIENT_CERT_KEY, CA_CERT_KEY, CA_CERT2_KEY, SUBJECT_MATCH_KEY, "engine", ENGINE_ID_KEY, PRIVATE_KEY_ID_KEY, ALTSUBJECT_MATCH_KEY, DOM_SUFFIX_MATCH_KEY, CA_PATH_KEY};
    public static final Parcelable.Creator<WifiEnterpriseConfig> CREATOR = new Parcelable.Creator<WifiEnterpriseConfig>() {
        @Override
        public WifiEnterpriseConfig createFromParcel(Parcel in) {
            WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                String key = in.readString();
                String value = in.readString();
                enterpriseConfig.mFields.put(key, value);
            }
            enterpriseConfig.mEapMethod = in.readInt();
            enterpriseConfig.mPhase2Method = in.readInt();
            enterpriseConfig.mCaCerts = readCertificates(in);
            PrivateKey userKey = null;
            int len = in.readInt();
            if (len > 0) {
                try {
                    byte[] bytes = new byte[len];
                    in.readByteArray(bytes);
                    String algorithm = in.readString();
                    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
                    userKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
                } catch (NoSuchAlgorithmException e) {
                    userKey = null;
                } catch (InvalidKeySpecException e2) {
                    userKey = null;
                }
            }
            enterpriseConfig.mClientPrivateKey = userKey;
            enterpriseConfig.mClientCertificate = readCertificate(in);
            return enterpriseConfig;
        }

        private X509Certificate[] readCertificates(Parcel in) {
            X509Certificate[] certs = null;
            int len = in.readInt();
            if (len > 0) {
                certs = new X509Certificate[len];
                for (int i = 0; i < len; i++) {
                    certs[i] = readCertificate(in);
                }
            }
            return certs;
        }

        private X509Certificate readCertificate(Parcel in) {
            int len = in.readInt();
            if (len <= 0) {
                return null;
            }
            try {
                byte[] bytes = new byte[len];
                in.readByteArray(bytes);
                CertificateFactory cFactory = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cFactory.generateCertificate(new ByteArrayInputStream(bytes));
                return cert;
            } catch (CertificateException e) {
                return null;
            }
        }

        @Override
        public WifiEnterpriseConfig[] newArray(int size) {
            return new WifiEnterpriseConfig[size];
        }
    };

    public interface SupplicantLoader {
        String loadValue(String str);
    }

    public interface SupplicantSaver {
        boolean saveValue(String str, String str2);
    }

    public WifiEnterpriseConfig() {
        this.mFields = new HashMap<>();
        this.mEapMethod = -1;
        this.mPhase2Method = 0;
    }

    public WifiEnterpriseConfig(WifiEnterpriseConfig source) {
        this.mFields = new HashMap<>();
        this.mEapMethod = -1;
        this.mPhase2Method = 0;
        for (String key : source.mFields.keySet()) {
            this.mFields.put(key, source.mFields.get(key));
        }
        this.mEapMethod = source.mEapMethod;
        this.mPhase2Method = source.mPhase2Method;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mFields.size());
        for (Map.Entry<String, String> entry : this.mFields.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
        dest.writeInt(this.mEapMethod);
        dest.writeInt(this.mPhase2Method);
        writeCertificates(dest, this.mCaCerts);
        if (this.mClientPrivateKey != null) {
            String algorithm = this.mClientPrivateKey.getAlgorithm();
            byte[] userKeyBytes = this.mClientPrivateKey.getEncoded();
            dest.writeInt(userKeyBytes.length);
            dest.writeByteArray(userKeyBytes);
            dest.writeString(algorithm);
        } else {
            dest.writeInt(0);
        }
        writeCertificate(dest, this.mClientCertificate);
    }

    private void writeCertificates(Parcel dest, X509Certificate[] cert) {
        if (cert != null && cert.length != 0) {
            dest.writeInt(cert.length);
            for (X509Certificate x509Certificate : cert) {
                writeCertificate(dest, x509Certificate);
            }
            return;
        }
        dest.writeInt(0);
    }

    private void writeCertificate(Parcel dest, X509Certificate cert) {
        if (cert != null) {
            try {
                byte[] certBytes = cert.getEncoded();
                dest.writeInt(certBytes.length);
                dest.writeByteArray(certBytes);
                return;
            } catch (CertificateEncodingException e) {
                dest.writeInt(0);
                return;
            }
        }
        dest.writeInt(0);
    }

    public static final class Eap {
        public static final int AKA = 5;
        public static final int AKA_PRIME = 6;
        public static final int FAST = 8;
        public static final int NONE = -1;
        public static final int PEAP = 0;
        public static final int PWD = 3;
        public static final int SIM = 4;
        public static final int TLS = 1;
        public static final int TTLS = 2;
        public static final int UNAUTH_TLS = 7;
        public static final String[] strings = {"PEAP", "TLS", "TTLS", "PWD", "SIM", "AKA", "AKA'", "WFA-UNAUTH-TLS", "FAST"};

        private Eap() {
        }
    }

    public static final class Phase2 {
        private static final String AUTHEAP_PREFIX = "autheap=";
        private static final String AUTH_PREFIX = "auth=";
        public static final int GTC = 4;
        public static final int MSCHAP = 2;
        public static final int MSCHAPV2 = 3;
        public static final int NONE = 0;
        public static final int PAP = 1;
        public static final String[] strings = {WifiEnterpriseConfig.EMPTY_VALUE, "PAP", "MSCHAP", "MSCHAPV2", "GTC"};

        private Phase2() {
        }
    }

    public boolean saveToSupplicant(SupplicantSaver saver, WifiConfiguration config) {
        if (!isEapMethodValid() && !config.isWapi()) {
            return false;
        }
        for (String key : this.mFields.keySet()) {
            if (!saver.saveValue(key, this.mFields.get(key))) {
                return false;
            }
        }
        if (!config.isWapi() && !saver.saveValue(EAP_KEY, Eap.strings[this.mEapMethod])) {
            return false;
        }
        if (this.mEapMethod != 1 && this.mPhase2Method != 0) {
            boolean is_autheap = this.mEapMethod == 2 && this.mPhase2Method == 4;
            String prefix = is_autheap ? "autheap=" : "auth=";
            String value = convertToQuotedString(prefix + Phase2.strings[this.mPhase2Method]);
            return saver.saveValue(PHASE2_KEY, value);
        }
        if (this.mPhase2Method == 0) {
            return saver.saveValue(PHASE2_KEY, null);
        }
        Log.e(TAG, "WiFi enterprise configuration is invalid as it supplies a phase 2 method but the phase1 method does not support it.");
        return false;
    }

    public void loadFromSupplicant(SupplicantLoader loader) {
        for (String key : SUPPLICANT_CONFIG_KEYS) {
            String value = loader.loadValue(key);
            if (value == null) {
                this.mFields.put(key, EMPTY_VALUE);
            } else {
                this.mFields.put(key, value);
            }
        }
        String eapMethod = loader.loadValue(EAP_KEY);
        this.mEapMethod = getStringIndex(Eap.strings, eapMethod, -1);
        String phase2Method = removeDoubleQuotes(loader.loadValue(PHASE2_KEY));
        if (phase2Method.startsWith("auth=")) {
            phase2Method = phase2Method.substring("auth=".length());
        } else if (phase2Method.startsWith("autheap=")) {
            phase2Method = phase2Method.substring("autheap=".length());
        }
        this.mPhase2Method = getStringIndex(Phase2.strings, phase2Method, 0);
    }

    public void setEapMethod(int eapMethod) {
        switch (eapMethod) {
            case 0:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
                break;
            case 1:
            case 7:
                setPhase2Method(0);
                break;
            default:
                throw new IllegalArgumentException("Unknown EAP method");
        }
        this.mEapMethod = eapMethod;
        this.mFields.put(OPP_KEY_CACHING, ENGINE_ENABLE);
    }

    public int getEapMethod() {
        return this.mEapMethod;
    }

    public void setPhase2Method(int phase2Method) {
        switch (phase2Method) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                this.mPhase2Method = phase2Method;
                return;
            default:
                throw new IllegalArgumentException("Unknown Phase 2 method");
        }
    }

    public int getPhase2Method() {
        return this.mPhase2Method;
    }

    public void setIdentity(String identity) {
        setFieldValue(IDENTITY_KEY, identity, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getIdentity() {
        return getFieldValue(IDENTITY_KEY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public void setAnonymousIdentity(String anonymousIdentity) {
        setFieldValue(ANON_IDENTITY_KEY, anonymousIdentity, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getAnonymousIdentity() {
        return getFieldValue(ANON_IDENTITY_KEY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public void setPassword(String password) {
        setFieldValue("password", password, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getPassword() {
        return getFieldValue("password", ProxyInfo.LOCAL_EXCL_LIST);
    }

    public static String encodeCaCertificateAlias(String alias) {
        byte[] bytes = alias.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte o : bytes) {
            sb.append(String.format("%02x", Integer.valueOf(o & BatteryStats.HistoryItem.CMD_NULL)));
        }
        return sb.toString();
    }

    public static String decodeCaCertificateAlias(String alias) {
        byte[] data = new byte[alias.length() >> 1];
        int n = 0;
        int position = 0;
        while (n < alias.length()) {
            data[position] = (byte) Integer.parseInt(alias.substring(n, n + 2), 16);
            n += 2;
            position++;
        }
        try {
            return new String(data, StandardCharsets.UTF_8);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return alias;
        }
    }

    public void setCaCertificateAlias(String alias) {
        setFieldValue(CA_CERT_KEY, alias, CA_CERT_PREFIX);
    }

    public void setCaCertificateAliases(String[] aliases) {
        if (aliases == null) {
            setFieldValue(CA_CERT_KEY, null, CA_CERT_PREFIX);
            return;
        }
        if (aliases.length == 1) {
            setCaCertificateAlias(aliases[0]);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aliases.length; i++) {
            if (i > 0) {
                sb.append(CA_CERT_ALIAS_DELIMITER);
            }
            sb.append(encodeCaCertificateAlias(Credentials.CA_CERTIFICATE + aliases[i]));
        }
        setFieldValue(CA_CERT_KEY, sb.toString(), KEYSTORES_URI);
    }

    public String getCaCertificateAlias() {
        return getFieldValue(CA_CERT_KEY, CA_CERT_PREFIX);
    }

    public String[] getCaCertificateAliases() {
        String value = getFieldValue(CA_CERT_KEY, ProxyInfo.LOCAL_EXCL_LIST);
        if (value.startsWith(CA_CERT_PREFIX)) {
            return new String[]{getFieldValue(CA_CERT_KEY, CA_CERT_PREFIX)};
        }
        if (value.startsWith(KEYSTORES_URI)) {
            String values = value.substring(KEYSTORES_URI.length());
            String[] aliases = TextUtils.split(values, CA_CERT_ALIAS_DELIMITER);
            for (int i = 0; i < aliases.length; i++) {
                aliases[i] = decodeCaCertificateAlias(aliases[i]);
                if (aliases[i].startsWith(Credentials.CA_CERTIFICATE)) {
                    aliases[i] = aliases[i].substring(Credentials.CA_CERTIFICATE.length());
                }
            }
            if (aliases.length != 0) {
                return aliases;
            }
            return null;
        }
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return new String[]{value};
    }

    public void setCaCertificate(X509Certificate cert) {
        if (cert != null) {
            if (cert.getBasicConstraints() >= 0) {
                this.mCaCerts = new X509Certificate[]{cert};
                return;
            }
            throw new IllegalArgumentException("Not a CA certificate");
        }
        this.mCaCerts = null;
    }

    public X509Certificate getCaCertificate() {
        if (this.mCaCerts == null || this.mCaCerts.length <= 0) {
            return null;
        }
        return this.mCaCerts[0];
    }

    public void setCaCertificates(X509Certificate[] certs) {
        if (certs != null) {
            X509Certificate[] newCerts = new X509Certificate[certs.length];
            for (int i = 0; i < certs.length; i++) {
                if (certs[i].getBasicConstraints() >= 0) {
                    newCerts[i] = certs[i];
                } else {
                    throw new IllegalArgumentException("Not a CA certificate");
                }
            }
            this.mCaCerts = newCerts;
            return;
        }
        this.mCaCerts = null;
    }

    public X509Certificate[] getCaCertificates() {
        if (this.mCaCerts == null || this.mCaCerts.length <= 0) {
            return null;
        }
        return this.mCaCerts;
    }

    public void resetCaCertificate() {
        this.mCaCerts = null;
    }

    public void setCaPath(String path) {
        setFieldValue(CA_PATH_KEY, path);
    }

    public String getCaPath() {
        return getFieldValue(CA_PATH_KEY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public void setClientCertificateAlias(String alias) {
        setFieldValue(CLIENT_CERT_KEY, alias, CLIENT_CERT_PREFIX);
        setFieldValue(PRIVATE_KEY_ID_KEY, alias, Credentials.USER_PRIVATE_KEY);
        if (TextUtils.isEmpty(alias)) {
            this.mFields.put("engine", ENGINE_DISABLE);
            this.mFields.put(ENGINE_ID_KEY, EMPTY_VALUE);
        } else {
            this.mFields.put("engine", ENGINE_ENABLE);
            this.mFields.put(ENGINE_ID_KEY, convertToQuotedString(ENGINE_ID_KEYSTORE));
        }
    }

    public String getClientCertificateAlias() {
        return getFieldValue(CLIENT_CERT_KEY, CLIENT_CERT_PREFIX);
    }

    public void setClientKeyEntry(PrivateKey privateKey, X509Certificate clientCertificate) {
        if (clientCertificate != null) {
            if (clientCertificate.getBasicConstraints() != -1) {
                throw new IllegalArgumentException("Cannot be a CA certificate");
            }
            if (privateKey == null) {
                throw new IllegalArgumentException("Client cert without a private key");
            }
            if (privateKey.getEncoded() == null) {
                throw new IllegalArgumentException("Private key cannot be encoded");
            }
        }
        this.mClientPrivateKey = privateKey;
        this.mClientCertificate = clientCertificate;
    }

    public X509Certificate getClientCertificate() {
        return this.mClientCertificate;
    }

    public void resetClientKeyEntry() {
        this.mClientPrivateKey = null;
        this.mClientCertificate = null;
    }

    public PrivateKey getClientPrivateKey() {
        return this.mClientPrivateKey;
    }

    public void setSubjectMatch(String subjectMatch) {
        setFieldValue(SUBJECT_MATCH_KEY, subjectMatch, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getSubjectMatch() {
        return getFieldValue(SUBJECT_MATCH_KEY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public void setAltSubjectMatch(String altSubjectMatch) {
        setFieldValue(ALTSUBJECT_MATCH_KEY, altSubjectMatch, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getAltSubjectMatch() {
        return getFieldValue(ALTSUBJECT_MATCH_KEY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public void setDomainSuffixMatch(String domain) {
        setFieldValue(DOM_SUFFIX_MATCH_KEY, domain);
    }

    public String getDomainSuffixMatch() {
        return getFieldValue(DOM_SUFFIX_MATCH_KEY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public void setRealm(String realm) {
        setFieldValue(REALM_KEY, realm, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getRealm() {
        return getFieldValue(REALM_KEY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public void setPlmn(String plmn) {
        setFieldValue(PLMN_KEY, plmn, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getPlmn() {
        return getFieldValue(PLMN_KEY, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public String getKeyId(WifiEnterpriseConfig current) {
        if (this.mEapMethod == -1) {
            return current != null ? current.getKeyId(null) : EMPTY_VALUE;
        }
        if (!isEapMethodValid()) {
            return EMPTY_VALUE;
        }
        return Eap.strings[this.mEapMethod] + "_" + Phase2.strings[this.mPhase2Method];
    }

    private String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    private int getStringIndex(String[] arr, String toBeFound, int defaultIndex) {
        if (TextUtils.isEmpty(toBeFound)) {
            return defaultIndex;
        }
        for (int i = 0; i < arr.length; i++) {
            if (toBeFound.equals(arr[i])) {
                return i;
            }
        }
        return defaultIndex;
    }

    public String getFieldValue(String key, String prefix) {
        String value = this.mFields.get(key);
        if (TextUtils.isEmpty(value) || EMPTY_VALUE.equals(value)) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        String value2 = removeDoubleQuotes(value);
        if (value2.startsWith(prefix)) {
            return value2.substring(prefix.length());
        }
        return value2;
    }

    public void setFieldValue(String key, String value, String prefix) {
        if (TextUtils.isEmpty(value)) {
            this.mFields.put(key, EMPTY_VALUE);
        } else {
            this.mFields.put(key, convertToQuotedString(prefix + value));
        }
    }

    public void setFieldValue(String key, String value) {
        if (TextUtils.isEmpty(value)) {
            this.mFields.put(key, EMPTY_VALUE);
        } else {
            this.mFields.put(key, convertToQuotedString(value));
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (String key : this.mFields.keySet()) {
            String value = "password".equals(key) ? "<removed>" : this.mFields.get(key);
            sb.append(key).append(CA_CERT_ALIAS_DELIMITER).append(value).append("\n");
        }
        return sb.toString();
    }

    private boolean isEapMethodValid() {
        if (this.mEapMethod == -1) {
            Log.e(TAG, "WiFi enterprise configuration is invalid as it supplies no EAP method.");
            return false;
        }
        if (this.mEapMethod < 0 || this.mEapMethod >= Eap.strings.length) {
            Log.e(TAG, "mEapMethod is invald for WiFi enterprise configuration: " + this.mEapMethod);
            return false;
        }
        if (this.mPhase2Method < 0 || this.mPhase2Method >= Phase2.strings.length) {
            Log.e(TAG, "mPhase2Method is invald for WiFi enterprise configuration: " + this.mPhase2Method);
            return false;
        }
        return true;
    }

    public void setCaCertificateWapiAlias(String alias) {
        setFieldValue(CA_CERT2_KEY, alias, CA_CERT2_PREFIX);
    }

    public String getCaCertificateWapiAlias() {
        return getFieldValue(CA_CERT2_KEY, CA_CERT2_PREFIX);
    }

    public void setClientCertificateWapiAlias(String alias) {
        setFieldValue(CLIENT_CERT_KEY, alias, "keystore://WAPIUSERCERT_");
        setFieldValue(PRIVATE_KEY_ID_KEY, alias, "keystore://WAPIUSERCERT_");
        if (TextUtils.isEmpty(alias)) {
            this.mFields.put("engine", ENGINE_DISABLE);
            this.mFields.put(ENGINE_ID_KEY, EMPTY_VALUE);
        } else {
            this.mFields.put("engine", ENGINE_ENABLE);
            this.mFields.put(ENGINE_ID_KEY, convertToQuotedString(ENGINE_ID_KEYSTORE));
        }
    }

    public String getClientCertificateWapiAlias() {
        return getFieldValue(CLIENT_CERT_KEY, "keystore://WAPIUSERCERT_");
    }
}
