package com.android.org.conscrypt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import libcore.io.IoUtils;
import libcore.util.BasicLruCache;

public class CertPinManager {
    private static final boolean DEBUG = false;
    private final TrustedCertificateStore certStore;
    private final Map<String, PinListEntry> entries;
    private final BasicLruCache<String, String> hostnameCache;
    private boolean initialized;
    private long lastModified;
    private final File pinFile;

    public CertPinManager(TrustedCertificateStore store) throws PinManagerException {
        this.entries = new HashMap();
        this.hostnameCache = new BasicLruCache<>(10);
        this.initialized = false;
        this.pinFile = new File("/data/misc/keychain/pins");
        this.certStore = store;
    }

    public CertPinManager(String path, TrustedCertificateStore store) throws PinManagerException {
        this.entries = new HashMap();
        this.hostnameCache = new BasicLruCache<>(10);
        this.initialized = false;
        if (path == null) {
            throw new NullPointerException("path == null");
        }
        this.pinFile = new File(path);
        this.certStore = store;
    }

    public boolean isChainValid(String hostname, List<X509Certificate> chain) throws PinManagerException {
        PinListEntry entry = lookup(hostname);
        if (entry == null) {
            return true;
        }
        return entry.isChainValid(chain);
    }

    private synchronized boolean ensureInitialized() throws PinManagerException {
        if (this.initialized && isCacheValid()) {
            return true;
        }
        String pinFileContents = readPinFile();
        if (pinFileContents != null) {
            for (String entry : getPinFileEntries(pinFileContents)) {
                try {
                    PinListEntry pin = new PinListEntry(entry, this.certStore);
                    this.entries.put(pin.getCommonName(), pin);
                } catch (PinEntryException e) {
                    log("Pinlist contains a malformed pin: " + entry, e);
                }
            }
            this.hostnameCache.evictAll();
            this.lastModified = this.pinFile.lastModified();
            this.initialized = true;
            return this.initialized;
        }
        return this.initialized;
    }

    private String readPinFile() throws PinManagerException {
        try {
            return IoUtils.readFileAsString(this.pinFile.getPath());
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e2) {
            throw new PinManagerException("Unexpected error reading pin list; failing.", e2);
        }
    }

    private static String[] getPinFileEntries(String pinFileContents) {
        return pinFileContents.split("\n");
    }

    private synchronized PinListEntry lookup(String hostname) throws PinManagerException {
        if (!ensureInitialized()) {
            return null;
        }
        String cn = (String) this.hostnameCache.get(hostname);
        if (cn != null) {
            return this.entries.get(cn);
        }
        String cn2 = getMatchingCN(hostname);
        if (cn2 == null) {
            return null;
        }
        this.hostnameCache.put(hostname, cn2);
        return this.entries.get(cn2);
    }

    private boolean isCacheValid() {
        return this.pinFile.lastModified() == this.lastModified;
    }

    private String getMatchingCN(String hostname) {
        String bestMatch = "";
        for (String cn : this.entries.keySet()) {
            if (cn.length() >= bestMatch.length() && isHostnameMatchedBy(hostname, cn)) {
                bestMatch = cn;
            }
        }
        return bestMatch;
    }

    private static boolean isHostnameMatchedBy(String hostName, String cn) {
        int suffixLength;
        int suffixStart;
        if (hostName == null || hostName.isEmpty() || cn == null || cn.isEmpty()) {
            return false;
        }
        String cn2 = cn.toLowerCase(Locale.US);
        if (!cn2.contains("*")) {
            return hostName.equals(cn2);
        }
        if (cn2.startsWith("*.") && hostName.regionMatches(0, cn2, 2, cn2.length() - 2)) {
            return true;
        }
        int asterisk = cn2.indexOf(42);
        int dot = cn2.indexOf(46);
        return asterisk <= dot && hostName.regionMatches(0, cn2, 0, asterisk) && hostName.indexOf(46, asterisk) >= (suffixStart = hostName.length() - (suffixLength = cn2.length() - (asterisk + 1))) && hostName.regionMatches(suffixStart, cn2, asterisk + 1, suffixLength);
    }

    private static void log(String s, Exception e) {
    }
}
