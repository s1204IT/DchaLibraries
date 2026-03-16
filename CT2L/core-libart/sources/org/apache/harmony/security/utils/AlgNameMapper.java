package org.apache.harmony.security.utils;

import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.harmony.security.fortress.Services;

public class AlgNameMapper {
    private static AlgNameMapperSource source = null;
    private static volatile int cacheVersion = -1;
    private static final String[] serviceName = {"Cipher", "AlgorithmParameters", "Signature"};
    private static final String[][] knownAlgMappings = {new String[]{"1.2.840.10040.4.1", "DSA"}, new String[]{"1.2.840.10040.4.3", "SHA1withDSA"}, new String[]{"1.2.840.113549.1.1.1", "RSA"}, new String[]{"1.2.840.113549.1.1.4", "MD5withRSA"}, new String[]{"1.2.840.113549.1.1.5", "SHA1withRSA"}, new String[]{"1.2.840.113549.1.3.1", "DiffieHellman"}, new String[]{"1.2.840.113549.1.5.3", "pbeWithMD5AndDES-CBC"}, new String[]{"1.2.840.113549.1.12.1.3", "pbeWithSHAAnd3-KeyTripleDES-CBC"}, new String[]{"1.2.840.113549.1.12.1.6", "pbeWithSHAAnd40BitRC2-CBC"}, new String[]{"1.2.840.113549.3.2", "RC2-CBC"}, new String[]{"1.2.840.113549.3.3", "RC2-EBC"}, new String[]{"1.2.840.113549.3.4", "RC4"}, new String[]{"1.2.840.113549.3.5", "RC4WithMAC"}, new String[]{"1.2.840.113549.3.6", "DESx-CBC"}, new String[]{"1.2.840.113549.3.7", "TripleDES-CBC"}, new String[]{"1.2.840.113549.3.8", "rc5CBC"}, new String[]{"1.2.840.113549.3.9", "RC5-CBC"}, new String[]{"1.2.840.113549.3.10", "DESCDMF"}, new String[]{"2.23.42.9.11.4.1", "ECDSA"}};
    private static final Map<String, String> alg2OidMap = new HashMap();
    private static final Map<String, String> oid2AlgMap = new HashMap();
    private static final Map<String, String> algAliasesMap = new HashMap();

    static {
        String[][] arr$ = knownAlgMappings;
        for (String[] element : arr$) {
            String algUC = element[1].toUpperCase(Locale.US);
            alg2OidMap.put(algUC, element[0]);
            oid2AlgMap.put(element[0], algUC);
            algAliasesMap.put(algUC, element[1]);
        }
    }

    private AlgNameMapper() {
    }

    private static synchronized void checkCacheVersion() {
        int newCacheVersion = Services.getCacheVersion();
        if (newCacheVersion != cacheVersion) {
            Provider[] pl = Security.getProviders();
            for (Provider element : pl) {
                selectEntries(element);
            }
            cacheVersion = newCacheVersion;
        }
    }

    public static String map2OID(String algName) {
        checkCacheVersion();
        String result = alg2OidMap.get(algName.toUpperCase(Locale.US));
        if (result == null) {
            AlgNameMapperSource s = source;
            if (s != null) {
                return s.mapNameToOid(algName);
            }
            return null;
        }
        return result;
    }

    public static String map2AlgName(String oid) {
        checkCacheVersion();
        String algUC = oid2AlgMap.get(oid);
        if (algUC != null) {
            return algAliasesMap.get(algUC);
        }
        AlgNameMapperSource s = source;
        if (s != null) {
            return s.mapOidToName(oid);
        }
        return null;
    }

    public static String getStandardName(String algName) {
        return algAliasesMap.get(algName.toUpperCase(Locale.US));
    }

    private static void selectEntries(Provider p) {
        Set<Map.Entry<Object, Object>> entrySet = p.entrySet();
        String[] arr$ = serviceName;
        for (String service : arr$) {
            String keyPrfix2find = "Alg.Alias." + service + ".";
            for (Map.Entry<Object, Object> me : entrySet) {
                String key = (String) me.getKey();
                if (key.startsWith(keyPrfix2find)) {
                    String alias = key.substring(keyPrfix2find.length());
                    String alg = (String) me.getValue();
                    String algUC = alg.toUpperCase(Locale.US);
                    if (isOID(alias)) {
                        if (alias.startsWith("OID.")) {
                            alias = alias.substring(4);
                        }
                        boolean oid2AlgContains = oid2AlgMap.containsKey(alias);
                        boolean alg2OidContains = alg2OidMap.containsKey(algUC);
                        if (!oid2AlgContains || !alg2OidContains) {
                            if (!oid2AlgContains) {
                                oid2AlgMap.put(alias, algUC);
                            }
                            if (!alg2OidContains) {
                                alg2OidMap.put(algUC, alias);
                            }
                            algAliasesMap.put(algUC, alg);
                        }
                    } else if (!algAliasesMap.containsKey(alias.toUpperCase(Locale.US))) {
                        algAliasesMap.put(alias.toUpperCase(Locale.US), alg);
                    }
                }
            }
        }
    }

    public static boolean isOID(String alias) {
        return org.apache.harmony.security.asn1.ObjectIdentifier.isOID(normalize(alias));
    }

    public static String normalize(String oid) {
        return oid.startsWith("OID.") ? oid.substring(4) : oid;
    }

    public static void setSource(AlgNameMapperSource source2) {
        source = source2;
    }
}
