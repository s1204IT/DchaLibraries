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
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.security.auth.x500.X500Principal;

public class KeyManagerImpl extends X509ExtendedKeyManager {
    private final Hashtable<String, KeyStore.PrivateKeyEntry> hash = new Hashtable<>();

    public KeyManagerImpl(KeyStore keyStore, char[] pwd) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                try {
                    if (keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, new KeyStore.PasswordProtection(pwd));
                        this.hash.put(alias, entry);
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
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        String[] al = chooseAlias(keyTypes, issuers);
        if (al == null) {
            return null;
        }
        return al[0];
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        String[] al = chooseAlias(new String[]{keyType}, issuers);
        if (al == null) {
            return null;
        }
        return al[0];
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        if (alias != null && this.hash.containsKey(alias)) {
            Certificate[] certs = this.hash.get(alias).getCertificateChain();
            if (certs[0] instanceof X509Certificate) {
                X509Certificate[] xcerts = new X509Certificate[certs.length];
                for (int i = 0; i < certs.length; i++) {
                    xcerts[i] = (X509Certificate) certs[i];
                }
                return xcerts;
            }
        }
        return null;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return chooseAlias(new String[]{keyType}, issuers);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return chooseAlias(new String[]{keyType}, issuers);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        if (alias != null && this.hash.containsKey(alias)) {
            return this.hash.get(alias).getPrivateKey();
        }
        return null;
    }

    @Override
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        String[] al = chooseAlias(keyTypes, issuers);
        if (al == null) {
            return null;
        }
        return al[0];
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        String[] al = chooseAlias(new String[]{keyType}, issuers);
        if (al == null) {
            return null;
        }
        return al[0];
    }

    private String[] chooseAlias(String[] keyTypes, Principal[] issuers) {
        String upperCase;
        String strSubstring;
        if (keyTypes == null || keyTypes.length == 0) {
            return null;
        }
        List listAsList = issuers == null ? null : Arrays.asList(issuers);
        ArrayList<String> found = new ArrayList<>();
        Enumeration<String> aliases = this.hash.keys();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            KeyStore.PrivateKeyEntry entry = this.hash.get(alias);
            Certificate[] chain = entry.getCertificateChain();
            Certificate cert = chain[0];
            String certKeyAlg = cert.getPublicKey().getAlgorithm();
            if (cert instanceof X509Certificate) {
                upperCase = ((X509Certificate) cert).getSigAlgName().toUpperCase(Locale.US);
            } else {
                upperCase = null;
            }
            int i = 0;
            int length = keyTypes.length;
            while (true) {
                int i2 = i;
                if (i2 < length) {
                    String keyAlgorithm = keyTypes[i2];
                    if (keyAlgorithm != null) {
                        int index = keyAlgorithm.indexOf(95);
                        if (index == -1) {
                            strSubstring = null;
                        } else {
                            strSubstring = keyAlgorithm.substring(index + 1);
                            keyAlgorithm = keyAlgorithm.substring(0, index);
                        }
                        if (certKeyAlg.equals(keyAlgorithm) && (strSubstring == null || upperCase == null || upperCase.contains(strSubstring))) {
                            if (issuers == null || issuers.length == 0) {
                                found.add(alias);
                            } else {
                                for (Certificate certFromChain : chain) {
                                    if (certFromChain instanceof X509Certificate) {
                                        X509Certificate xcertFromChain = (X509Certificate) certFromChain;
                                        X500Principal issuerFromChain = xcertFromChain.getIssuerX500Principal();
                                        if (listAsList.contains(issuerFromChain)) {
                                            found.add(alias);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    i = i2 + 1;
                }
            }
        }
        if (!found.isEmpty()) {
            return (String[]) found.toArray(new String[found.size()]);
        }
        return null;
    }
}
