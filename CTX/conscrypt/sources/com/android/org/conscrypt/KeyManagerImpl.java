package com.android.org.conscrypt;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

class KeyManagerImpl extends X509ExtendedKeyManager {
    private final HashMap<String, KeyStore.PrivateKeyEntry> hash = new HashMap<>();

    KeyManagerImpl(KeyStore keyStore, char[] cArr) {
        try {
            Enumeration<String> enumerationAliases = keyStore.aliases();
            while (enumerationAliases.hasMoreElements()) {
                String strNextElement = enumerationAliases.nextElement();
                try {
                    if (keyStore.entryInstanceOf(strNextElement, KeyStore.PrivateKeyEntry.class)) {
                        this.hash.put(strNextElement, (KeyStore.PrivateKeyEntry) keyStore.getEntry(strNextElement, new KeyStore.PasswordProtection(cArr)));
                    }
                } catch (KeyStoreException e) {
                } catch (NoSuchAlgorithmException e2) {
                } catch (UnrecoverableEntryException e3) {
                }
            }
        } catch (KeyStoreException e4) {
        }
    }

    @Override
    public String chooseClientAlias(String[] strArr, Principal[] principalArr, Socket socket) {
        String[] strArrChooseAlias = chooseAlias(strArr, principalArr);
        if (strArrChooseAlias == null) {
            return null;
        }
        return strArrChooseAlias[0];
    }

    @Override
    public String chooseServerAlias(String str, Principal[] principalArr, Socket socket) {
        String[] strArrChooseAlias = chooseAlias(new String[]{str}, principalArr);
        if (strArrChooseAlias == null) {
            return null;
        }
        return strArrChooseAlias[0];
    }

    @Override
    public X509Certificate[] getCertificateChain(String str) {
        if (str != null && this.hash.containsKey(str)) {
            Certificate[] certificateChain = this.hash.get(str).getCertificateChain();
            if (certificateChain[0] instanceof X509Certificate) {
                X509Certificate[] x509CertificateArr = new X509Certificate[certificateChain.length];
                for (int i = 0; i < certificateChain.length; i++) {
                    x509CertificateArr[i] = (X509Certificate) certificateChain[i];
                }
                return x509CertificateArr;
            }
        }
        return null;
    }

    @Override
    public String[] getClientAliases(String str, Principal[] principalArr) {
        return chooseAlias(new String[]{str}, principalArr);
    }

    @Override
    public String[] getServerAliases(String str, Principal[] principalArr) {
        return chooseAlias(new String[]{str}, principalArr);
    }

    @Override
    public PrivateKey getPrivateKey(String str) {
        if (str == null || !this.hash.containsKey(str)) {
            return null;
        }
        return this.hash.get(str).getPrivateKey();
    }

    @Override
    public String chooseEngineClientAlias(String[] strArr, Principal[] principalArr, SSLEngine sSLEngine) {
        String[] strArrChooseAlias = chooseAlias(strArr, principalArr);
        if (strArrChooseAlias == null) {
            return null;
        }
        return strArrChooseAlias[0];
    }

    @Override
    public String chooseEngineServerAlias(String str, Principal[] principalArr, SSLEngine sSLEngine) {
        String[] strArrChooseAlias = chooseAlias(new String[]{str}, principalArr);
        if (strArrChooseAlias == null) {
            return null;
        }
        return strArrChooseAlias[0];
    }

    private String[] chooseAlias(String[] strArr, Principal[] principalArr) {
        List listAsList;
        String upperCase;
        String strSubstring;
        if (strArr == null || strArr.length == 0) {
            return null;
        }
        if (principalArr != null) {
            listAsList = Arrays.asList(principalArr);
        } else {
            listAsList = null;
        }
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, KeyStore.PrivateKeyEntry> entry : this.hash.entrySet()) {
            String key = entry.getKey();
            Certificate[] certificateChain = entry.getValue().getCertificateChain();
            int i = 0;
            Certificate certificate = certificateChain[0];
            String algorithm = certificate.getPublicKey().getAlgorithm();
            if (certificate instanceof X509Certificate) {
                upperCase = ((X509Certificate) certificate).getSigAlgName().toUpperCase(Locale.US);
            } else {
                upperCase = null;
            }
            int length = strArr.length;
            int i2 = 0;
            while (i2 < length) {
                String strSubstring2 = strArr[i2];
                if (strSubstring2 != null) {
                    int iIndexOf = strSubstring2.indexOf(95);
                    if (iIndexOf != -1) {
                        strSubstring = strSubstring2.substring(iIndexOf + 1);
                        strSubstring2 = strSubstring2.substring(i, iIndexOf);
                    } else {
                        strSubstring = null;
                    }
                    if (algorithm.equals(strSubstring2) && (strSubstring == null || upperCase == null || upperCase.contains(strSubstring))) {
                        if (principalArr == null || principalArr.length == 0) {
                            arrayList.add(key);
                        } else {
                            int length2 = certificateChain.length;
                            for (int i3 = i; i3 < length2; i3++) {
                                Certificate certificate2 = certificateChain[i3];
                                if ((certificate2 instanceof X509Certificate) && listAsList.contains(((X509Certificate) certificate2).getIssuerX500Principal())) {
                                    arrayList.add(key);
                                }
                            }
                        }
                    }
                }
                i2++;
                i = 0;
            }
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }
}
