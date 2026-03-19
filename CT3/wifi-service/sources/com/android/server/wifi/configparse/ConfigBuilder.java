package com.android.server.wifi.configparse;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Base64;
import android.util.Log;
import com.android.server.wifi.IMSIParameter;
import com.android.server.wifi.anqp.eap.AuthParam;
import com.android.server.wifi.anqp.eap.EAP;
import com.android.server.wifi.anqp.eap.EAPMethod;
import com.android.server.wifi.anqp.eap.NonEAPInnerAuth;
import com.android.server.wifi.hotspot2.omadm.PasspointManagementObjectManager;
import com.android.server.wifi.hotspot2.pps.Credential;
import com.android.server.wifi.hotspot2.pps.HomeSP;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.xml.sax.SAXException;

public class ConfigBuilder {

    private static final int[] f8comandroidserverwifianqpeapEAP$EAPMethodIDSwitchesValues = null;

    private static final int[] f9x4565a7a7 = null;
    private static final String CATag = "application/x-x509-ca-cert";
    private static final String KeyTag = "application/x-pkcs12";
    private static final String ProfileTag = "application/x-passpoint-profile";
    private static final String TAG = "WCFG";
    public static final String WifiConfigType = "application/x-wifi-config";
    private static final String X509 = "X.509";

    private static int[] m410x55a4ef8b() {
        if (f8comandroidserverwifianqpeapEAP$EAPMethodIDSwitchesValues != null) {
            return f8comandroidserverwifianqpeapEAP$EAPMethodIDSwitchesValues;
        }
        int[] iArr = new int[EAP.EAPMethodID.valuesCustom().length];
        try {
            iArr[EAP.EAPMethodID.EAP_3Com.ordinal()] = 10;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_AKA.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_AKAPrim.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_ActiontecWireless.ordinal()] = 11;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_EKE.ordinal()] = 12;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_FAST.ordinal()] = 13;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_GPSK.ordinal()] = 14;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_HTTPDigest.ordinal()] = 15;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_IKEv2.ordinal()] = 16;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_KEA.ordinal()] = 17;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_KEA_VALIDATE.ordinal()] = 18;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_LEAP.ordinal()] = 19;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_Link.ordinal()] = 20;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_MD5.ordinal()] = 21;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_MOBAC.ordinal()] = 22;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_MSCHAPv2.ordinal()] = 23;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_OTP.ordinal()] = 24;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_PAX.ordinal()] = 25;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_PEAP.ordinal()] = 26;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_POTP.ordinal()] = 27;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_PSK.ordinal()] = 28;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_PWD.ordinal()] = 29;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_RSA.ordinal()] = 30;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_SAKE.ordinal()] = 31;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_SIM.ordinal()] = 3;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_SPEKE.ordinal()] = 32;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_TEAP.ordinal()] = 33;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_TLS.ordinal()] = 4;
        } catch (NoSuchFieldError e28) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_TTLS.ordinal()] = 5;
        } catch (NoSuchFieldError e29) {
        }
        try {
            iArr[EAP.EAPMethodID.EAP_ZLXEAP.ordinal()] = 34;
        } catch (NoSuchFieldError e30) {
        }
        f8comandroidserverwifianqpeapEAP$EAPMethodIDSwitchesValues = iArr;
        return iArr;
    }

    private static int[] m411xba111b4b() {
        if (f9x4565a7a7 != null) {
            return f9x4565a7a7;
        }
        int[] iArr = new int[NonEAPInnerAuth.NonEAPType.valuesCustom().length];
        try {
            iArr[NonEAPInnerAuth.NonEAPType.CHAP.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[NonEAPInnerAuth.NonEAPType.MSCHAP.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[NonEAPInnerAuth.NonEAPType.MSCHAPv2.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[NonEAPInnerAuth.NonEAPType.PAP.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[NonEAPInnerAuth.NonEAPType.Reserved.ordinal()] = 10;
        } catch (NoSuchFieldError e5) {
        }
        f9x4565a7a7 = iArr;
        return iArr;
    }

    public static WifiConfiguration buildConfig(String uriString, byte[] data, Context context) throws GeneralSecurityException, SAXException, IOException {
        MIMEContainer inner;
        Log.d(TAG, "Content: " + (data != null ? data.length : -1));
        byte[] b64 = Base64.decode(new String(data, StandardCharsets.ISO_8859_1), 0);
        Log.d(TAG, "Decoded: " + b64.length + " bytes.");
        MIMEContainer mimeContainer = new MIMEContainer(new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(b64), StandardCharsets.ISO_8859_1)), null);
        if (!mimeContainer.isBase64()) {
            throw new IOException("Encoding for " + mimeContainer.getContentType() + " is not base64");
        }
        if (mimeContainer.getContentType().equals(WifiConfigType)) {
            byte[] wrappedContent = Base64.decode(mimeContainer.getText(), 0);
            Log.d(TAG, "Building container from '" + new String(wrappedContent, StandardCharsets.ISO_8859_1) + "'");
            inner = new MIMEContainer(new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(wrappedContent), StandardCharsets.ISO_8859_1)), null);
        } else {
            inner = mimeContainer;
        }
        return parse(inner);
    }

    private static WifiConfiguration parse(MIMEContainer root) throws GeneralSecurityException, SAXException, IOException {
        if (root.getMimeContainers() == null) {
            throw new IOException("Malformed MIME content: not multipart");
        }
        String moText = null;
        X509Certificate caCert = null;
        PrivateKey clientKey = null;
        List<X509Certificate> clientChain = null;
        for (MIMEContainer subContainer : root.getMimeContainers()) {
            Log.d(TAG, " + Content Type: " + subContainer.getContentType());
            String contentType = subContainer.getContentType();
            if (!contentType.equals(ProfileTag)) {
                if (!contentType.equals(CATag)) {
                    if (!contentType.equals(KeyTag)) {
                        continue;
                    } else {
                        if (!subContainer.isBase64()) {
                            throw new IOException("Can't read non base64 encoded key");
                        }
                        byte[] octets = Base64.decode(subContainer.getText(), 0);
                        KeyStore ks = KeyStore.getInstance("PKCS12");
                        ByteArrayInputStream in = new ByteArrayInputStream(octets);
                        ks.load(in, new char[0]);
                        in.close();
                        Log.d(TAG, "---- Start PKCS12 info " + octets.length + ", size " + ks.size());
                        Enumeration<String> aliases = ks.aliases();
                        while (aliases.hasMoreElements()) {
                            String alias = aliases.nextElement();
                            clientKey = (PrivateKey) ks.getKey(alias, null);
                            Log.d(TAG, "Key: " + clientKey.getFormat());
                            Certificate[] chain = ks.getCertificateChain(alias);
                            if (chain != null) {
                                clientChain = new ArrayList<>();
                                for (Certificate certificate : chain) {
                                    if (!(certificate instanceof X509Certificate)) {
                                        Log.w(TAG, "Element in cert chain is not an X509Certificate: " + certificate.getClass());
                                    }
                                    clientChain.add((X509Certificate) certificate);
                                }
                                Log.d(TAG, "Chain: " + clientChain.size());
                            }
                        }
                        Log.d(TAG, "---- End PKCS12 info.");
                    }
                } else {
                    if (!subContainer.isBase64()) {
                        throw new IOException("Can't read non base64 encoded cert");
                    }
                    byte[] octets2 = Base64.decode(subContainer.getText(), 0);
                    CertificateFactory factory = CertificateFactory.getInstance(X509);
                    caCert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(octets2));
                    Log.d(TAG, "Cert subject " + caCert.getSubjectX500Principal());
                    Log.d(TAG, "Full Cert: " + caCert);
                }
            } else {
                if (subContainer.isBase64()) {
                    byte[] octets3 = Base64.decode(subContainer.getText(), 0);
                    moText = new String(octets3, StandardCharsets.UTF_8);
                } else {
                    moText = subContainer.getText();
                }
                Log.d(TAG, "OMA: " + moText);
            }
        }
        if (moText == null) {
            throw new IOException("Missing profile");
        }
        HomeSP homeSP = PasspointManagementObjectManager.buildSP(moText);
        return buildConfig(homeSP, caCert, clientChain, clientKey);
    }

    private static WifiConfiguration buildConfig(HomeSP homeSP, X509Certificate caCert, List<X509Certificate> clientChain, PrivateKey key) throws GeneralSecurityException, IOException {
        WifiConfiguration config;
        Credential credential = homeSP.getCredential();
        EAP.EAPMethodID eapMethodID = credential.getEAPMethod().getEAPMethodID();
        switch (m410x55a4ef8b()[eapMethodID.ordinal()]) {
            case 1:
            case 2:
            case 3:
                if (key != null || clientChain != null || caCert != null) {
                    Log.i(TAG, "Client/CA cert and/or key included with " + eapMethodID + " profile");
                }
                config = buildSIMConfig(homeSP);
                break;
            case 4:
                config = buildTLSConfig(homeSP, clientChain, key);
                break;
            case 5:
                if (key != null || clientChain != null) {
                    Log.w(TAG, "Client cert and/or key included with EAP-TTLS profile");
                }
                config = buildTTLSConfig(homeSP);
                break;
            default:
                throw new IOException("Unsupported EAP Method: " + eapMethodID);
        }
        WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
        enterpriseConfig.setCaCertificate(caCert);
        enterpriseConfig.setAnonymousIdentity("anonymous@" + credential.getRealm());
        return config;
    }

    private static WifiConfiguration buildTTLSConfig(HomeSP homeSP) throws IOException {
        Credential credential = homeSP.getCredential();
        if (credential.getUserName() == null || credential.getPassword() == null) {
            throw new IOException("EAP-TTLS provisioned without user name or password");
        }
        EAPMethod eapMethod = credential.getEAPMethod();
        AuthParam authParam = eapMethod.getAuthParam();
        if (authParam == null || authParam.getAuthInfoID() != EAP.AuthInfoID.NonEAPInnerAuthType) {
            throw new IOException("Bad auth parameter for EAP-TTLS: " + authParam);
        }
        WifiConfiguration config = buildBaseConfiguration(homeSP);
        NonEAPInnerAuth ttlsParam = (NonEAPInnerAuth) authParam;
        WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
        enterpriseConfig.setPhase2Method(remapInnerMethod(ttlsParam.getType()));
        enterpriseConfig.setIdentity(credential.getUserName());
        enterpriseConfig.setPassword(credential.getPassword());
        return config;
    }

    private static WifiConfiguration buildTLSConfig(HomeSP homeSP, List<X509Certificate> clientChain, PrivateKey clientKey) throws GeneralSecurityException, IOException {
        Credential credential = homeSP.getCredential();
        X509Certificate clientCertificate = null;
        if (clientKey == null || clientChain == null) {
            throw new IOException("No key and/or cert passed for EAP-TLS");
        }
        if (credential.getCertType() != Credential.CertType.x509v3) {
            throw new IOException("Invalid certificate type for TLS: " + credential.getCertType());
        }
        byte[] reference = credential.getFingerPrint();
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        Iterator certificate$iterator = clientChain.iterator();
        while (true) {
            if (!certificate$iterator.hasNext()) {
                break;
            }
            X509Certificate certificate = (X509Certificate) certificate$iterator.next();
            digester.reset();
            byte[] fingerprint = digester.digest(certificate.getEncoded());
            if (Arrays.equals(reference, fingerprint)) {
                clientCertificate = certificate;
                break;
            }
        }
        if (clientCertificate == null) {
            throw new IOException("No certificate in chain matches supplied fingerprint");
        }
        String alias = Base64.encodeToString(reference, 0);
        WifiConfiguration config = buildBaseConfiguration(homeSP);
        WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
        enterpriseConfig.setClientCertificateAlias(alias);
        enterpriseConfig.setClientKeyEntry(clientKey, clientCertificate);
        return config;
    }

    private static WifiConfiguration buildSIMConfig(HomeSP homeSP) throws IOException {
        Credential credential = homeSP.getCredential();
        IMSIParameter credImsi = credential.getImsi();
        WifiConfiguration config = buildBaseConfiguration(homeSP);
        config.enterpriseConfig.setPlmn(credImsi.toString());
        return config;
    }

    private static WifiConfiguration buildBaseConfiguration(HomeSP homeSP) throws IOException {
        EAP.EAPMethodID eapMethodID = homeSP.getCredential().getEAPMethod().getEAPMethodID();
        WifiConfiguration config = new WifiConfiguration();
        config.FQDN = homeSP.getFQDN();
        HashSet<Long> roamingConsortiumIds = homeSP.getRoamingConsortiums();
        config.roamingConsortiumIds = new long[roamingConsortiumIds.size()];
        int i = 0;
        Iterator id$iterator = roamingConsortiumIds.iterator();
        while (id$iterator.hasNext()) {
            long id = ((Long) id$iterator.next()).longValue();
            config.roamingConsortiumIds[i] = id;
            i++;
        }
        config.providerFriendlyName = homeSP.getFriendlyName();
        config.allowedKeyManagement.set(2);
        config.allowedKeyManagement.set(3);
        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
        enterpriseConfig.setEapMethod(remapEAPMethod(eapMethodID));
        enterpriseConfig.setRealm(homeSP.getCredential().getRealm());
        config.enterpriseConfig = enterpriseConfig;
        config.updateIdentifier = null;
        return config;
    }

    private static int remapEAPMethod(EAP.EAPMethodID eapMethodID) throws IOException {
        switch (m410x55a4ef8b()[eapMethodID.ordinal()]) {
            case 1:
                return 5;
            case 2:
                return 6;
            case 3:
                return 4;
            case 4:
                return 1;
            case 5:
                return 2;
            default:
                throw new IOException("Bad EAP method: " + eapMethodID);
        }
    }

    private static int remapInnerMethod(NonEAPInnerAuth.NonEAPType type) throws IOException {
        switch (m411xba111b4b()[type.ordinal()]) {
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 1;
            default:
                throw new IOException("Inner method " + type + " not supported");
        }
    }
}
