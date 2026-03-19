package com.android.org.conscrypt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import libcore.io.IoUtils;

public class TrustedCertificateStore {
    private static final CertificateFactory CERT_FACTORY;
    private static final String PREFIX_SYSTEM = "system:";
    private static final String PREFIX_USER = "user:";
    private static File defaultCaCertsAddedDir;
    private static File defaultCaCertsDeletedDir;
    private static File defaultCaCertsSystemDir;
    private final File addedDir;
    private final File deletedDir;
    private final File systemDir;

    private interface CertSelector {
        boolean match(X509Certificate x509Certificate);
    }

    public static final boolean isSystem(String alias) {
        return alias.startsWith(PREFIX_SYSTEM);
    }

    public static final boolean isUser(String alias) {
        return alias.startsWith(PREFIX_USER);
    }

    static {
        String ANDROID_ROOT = System.getenv("ANDROID_ROOT");
        String ANDROID_DATA = System.getenv("ANDROID_DATA");
        defaultCaCertsSystemDir = new File(ANDROID_ROOT + "/etc/security/cacerts");
        setDefaultUserDirectory(new File(ANDROID_DATA + "/misc/keychain"));
        try {
            CERT_FACTORY = CertificateFactory.getInstance("X509");
        } catch (CertificateException e) {
            throw new AssertionError(e);
        }
    }

    public static void setDefaultUserDirectory(File root) {
        defaultCaCertsAddedDir = new File(root, "cacerts-added");
        defaultCaCertsDeletedDir = new File(root, "cacerts-removed");
    }

    public TrustedCertificateStore() {
        this(defaultCaCertsSystemDir, defaultCaCertsAddedDir, defaultCaCertsDeletedDir);
    }

    public TrustedCertificateStore(File systemDir, File addedDir, File deletedDir) {
        this.systemDir = systemDir;
        this.addedDir = addedDir;
        this.deletedDir = deletedDir;
    }

    public Certificate getCertificate(String alias) {
        return getCertificate(alias, false);
    }

    public Certificate getCertificate(String alias, boolean includeDeletedSystem) {
        X509Certificate cert;
        File file = fileForAlias(alias);
        if (file == null || ((isUser(alias) && isTombstone(file)) || (cert = readCertificate(file)) == null || (isSystem(alias) && !includeDeletedSystem && isDeletedSystemCertificate(cert)))) {
            return null;
        }
        return cert;
    }

    private File fileForAlias(String alias) {
        File file;
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        if (isSystem(alias)) {
            file = new File(this.systemDir, alias.substring(PREFIX_SYSTEM.length()));
        } else {
            if (!isUser(alias)) {
                return null;
            }
            file = new File(this.addedDir, alias.substring(PREFIX_USER.length()));
        }
        if (!file.exists() || isTombstone(file)) {
            return null;
        }
        return file;
    }

    private boolean isTombstone(File file) {
        return file.length() == 0;
    }

    private X509Certificate readCertificate(File file) throws Throwable {
        InputStream is;
        if (!file.isFile()) {
            return null;
        }
        InputStream is2 = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
        } catch (IOException e) {
        } catch (CertificateException e2) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            X509Certificate x509Certificate = (X509Certificate) CERT_FACTORY.generateCertificate(is);
            IoUtils.closeQuietly(is);
            return x509Certificate;
        } catch (IOException e3) {
            is2 = is;
            IoUtils.closeQuietly(is2);
            return null;
        } catch (CertificateException e4) {
            is2 = is;
            IoUtils.closeQuietly(is2);
            return null;
        } catch (Throwable th2) {
            th = th2;
            is2 = is;
            IoUtils.closeQuietly(is2);
            throw th;
        }
    }

    private void writeCertificate(File file, X509Certificate cert) throws Throwable {
        OutputStream os;
        File dir = file.getParentFile();
        dir.mkdirs();
        dir.setReadable(true, false);
        dir.setExecutable(true, false);
        OutputStream os2 = null;
        try {
            os = new FileOutputStream(file);
        } catch (Throwable th) {
            th = th;
        }
        try {
            os.write(cert.getEncoded());
            IoUtils.closeQuietly(os);
            file.setReadable(true, false);
        } catch (Throwable th2) {
            th = th2;
            os2 = os;
            IoUtils.closeQuietly(os2);
            throw th;
        }
    }

    private boolean isDeletedSystemCertificate(X509Certificate x) {
        return getCertificateFile(this.deletedDir, x).exists();
    }

    public Date getCreationDate(String alias) {
        File file;
        if (!containsAlias(alias) || (file = fileForAlias(alias)) == null) {
            return null;
        }
        long time = file.lastModified();
        if (time == 0) {
            return null;
        }
        return new Date(time);
    }

    public Set<String> aliases() {
        Set<String> result = new HashSet<>();
        addAliases(result, PREFIX_USER, this.addedDir);
        addAliases(result, PREFIX_SYSTEM, this.systemDir);
        return result;
    }

    public Set<String> userAliases() {
        Set<String> result = new HashSet<>();
        addAliases(result, PREFIX_USER, this.addedDir);
        return result;
    }

    private void addAliases(Set<String> result, String prefix, File dir) {
        String[] files = dir.list();
        if (files == null) {
            return;
        }
        for (String filename : files) {
            String alias = prefix + filename;
            if (containsAlias(alias)) {
                result.add(alias);
            }
        }
    }

    public Set<String> allSystemAliases() {
        Set<String> result = new HashSet<>();
        String[] files = this.systemDir.list();
        if (files == null) {
            return result;
        }
        for (String filename : files) {
            String alias = PREFIX_SYSTEM + filename;
            if (containsAlias(alias, true)) {
                result.add(alias);
            }
        }
        return result;
    }

    public boolean containsAlias(String alias) {
        return containsAlias(alias, false);
    }

    private boolean containsAlias(String alias, boolean includeDeletedSystem) {
        return getCertificate(alias, includeDeletedSystem) != null;
    }

    public String getCertificateAlias(Certificate c) {
        return getCertificateAlias(c, false);
    }

    public String getCertificateAlias(Certificate c, boolean includeDeletedSystem) {
        if (c == null || !(c instanceof X509Certificate)) {
            return null;
        }
        X509Certificate x = (X509Certificate) c;
        File user = getCertificateFile(this.addedDir, x);
        if (user.exists()) {
            return PREFIX_USER + user.getName();
        }
        if (!includeDeletedSystem && isDeletedSystemCertificate(x)) {
            return null;
        }
        File system = getCertificateFile(this.systemDir, x);
        if (system.exists()) {
            return PREFIX_SYSTEM + system.getName();
        }
        return null;
    }

    public boolean isUserAddedCertificate(X509Certificate cert) {
        return getCertificateFile(this.addedDir, cert).exists();
    }

    public File getCertificateFile(File dir, final X509Certificate x) {
        CertSelector selector = new CertSelector() {
            @Override
            public boolean match(X509Certificate cert) {
                return cert.equals(x);
            }
        };
        return (File) findCert(dir, x.getSubjectX500Principal(), selector, File.class);
    }

    public X509Certificate getTrustAnchor(final X509Certificate c) {
        CertSelector selector = new CertSelector() {
            @Override
            public boolean match(X509Certificate ca) {
                return ca.getPublicKey().equals(c.getPublicKey());
            }
        };
        X509Certificate user = (X509Certificate) findCert(this.addedDir, c.getSubjectX500Principal(), selector, X509Certificate.class);
        if (user != null) {
            return user;
        }
        X509Certificate system = (X509Certificate) findCert(this.systemDir, c.getSubjectX500Principal(), selector, X509Certificate.class);
        if (system == null || isDeletedSystemCertificate(system)) {
            return null;
        }
        return system;
    }

    public X509Certificate findIssuer(final X509Certificate c) {
        CertSelector selector = new CertSelector() {
            @Override
            public boolean match(X509Certificate ca) {
                try {
                    c.verify(ca.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
        X500Principal issuer = c.getIssuerX500Principal();
        X509Certificate user = (X509Certificate) findCert(this.addedDir, issuer, selector, X509Certificate.class);
        if (user != null) {
            return user;
        }
        X509Certificate system = (X509Certificate) findCert(this.systemDir, issuer, selector, X509Certificate.class);
        if (system == null || isDeletedSystemCertificate(system)) {
            return null;
        }
        return system;
    }

    public Set<X509Certificate> findAllIssuers(final X509Certificate c) {
        Set<X509Certificate> issuers = null;
        CertSelector selector = new CertSelector() {
            @Override
            public boolean match(X509Certificate ca) {
                try {
                    c.verify(ca.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
        X500Principal issuer = c.getIssuerX500Principal();
        Set<X509Certificate> userAddedCerts = (Set) findCert(this.addedDir, issuer, selector, Set.class);
        if (userAddedCerts != null) {
            issuers = userAddedCerts;
        }
        CertSelector selector2 = new CertSelector() {
            @Override
            public boolean match(X509Certificate ca) {
                try {
                    if (TrustedCertificateStore.this.isDeletedSystemCertificate(ca)) {
                        return false;
                    }
                    c.verify(ca.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
        Set<X509Certificate> systemCerts = (Set) findCert(this.systemDir, issuer, selector2, Set.class);
        if (systemCerts != null) {
            if (issuers != null) {
                issuers.addAll(systemCerts);
            } else {
                issuers = systemCerts;
            }
        }
        if (issuers != null) {
            return issuers;
        }
        Set<X509Certificate> issuers2 = Collections.emptySet();
        return issuers2;
    }

    private static boolean isSelfIssuedCertificate(OpenSSLX509Certificate cert) {
        long ctx = cert.getContext();
        return NativeCrypto.X509_check_issued(ctx, ctx) == 0;
    }

    private static OpenSSLX509Certificate convertToOpenSSLIfNeeded(X509Certificate cert) throws CertificateException {
        if (cert == null) {
            return null;
        }
        if (cert instanceof OpenSSLX509Certificate) {
            return (OpenSSLX509Certificate) cert;
        }
        try {
            return OpenSSLX509Certificate.fromX509Der(cert.getEncoded());
        } catch (Exception e) {
            throw new CertificateException(e);
        }
    }

    public List<X509Certificate> getCertificateChain(X509Certificate leaf) throws CertificateException {
        LinkedHashSet<org.conscrypt.OpenSSLX509Certificate> chain = new LinkedHashSet<>();
        OpenSSLX509Certificate cert = convertToOpenSSLIfNeeded(leaf);
        chain.add(cert);
        while (!isSelfIssuedCertificate(cert) && (cert = convertToOpenSSLIfNeeded(findIssuer(cert))) != null && !chain.contains(cert)) {
            chain.add(cert);
        }
        return new ArrayList(chain);
    }

    private <T> T findCert(File file, X500Principal x500Principal, CertSelector certSelector, Class<T> cls) {
        ?? r0;
        T t = null;
        String strHash = hash(x500Principal);
        int i = 0;
        while (true) {
            ?? r2 = (T) file(file, strHash, i);
            if (!r2.isFile()) {
                if (cls == Boolean.class) {
                    return (T) Boolean.FALSE;
                }
                if (cls == File.class) {
                    return r2;
                }
                if (cls == Set.class) {
                    return t;
                }
                return null;
            }
            if (!isTombstone(r2) && (r0 = (T) readCertificate(r2)) != 0 && certSelector.match(r0)) {
                if (cls == X509Certificate.class) {
                    return r0;
                }
                if (cls == Boolean.class) {
                    return (T) Boolean.TRUE;
                }
                if (cls == File.class) {
                    return r2;
                }
                if (cls == Set.class) {
                    if (t == null) {
                        t = (T) new HashSet();
                    }
                    ((Set) t).add(r0);
                } else {
                    throw new AssertionError();
                }
            }
            i++;
        }
    }

    private String hash(X500Principal name) {
        int hash = NativeCrypto.X509_NAME_hash_old(name);
        return Hex.intToHexString(hash, 8);
    }

    private File file(File dir, String hash, int index) {
        return new File(dir, hash + '.' + index);
    }

    public void installCertificate(X509Certificate cert) throws Throwable {
        if (cert == null) {
            throw new NullPointerException("cert == null");
        }
        File system = getCertificateFile(this.systemDir, cert);
        if (system.exists()) {
            File deleted = getCertificateFile(this.deletedDir, cert);
            if (deleted.exists() && !deleted.delete()) {
                throw new IOException("Could not remove " + deleted);
            }
            return;
        }
        File user = getCertificateFile(this.addedDir, cert);
        if (user.exists()) {
            return;
        }
        writeCertificate(user, cert);
    }

    public void deleteCertificateEntry(String alias) throws Throwable {
        File file;
        if (alias == null || (file = fileForAlias(alias)) == null) {
            return;
        }
        if (isSystem(alias)) {
            X509Certificate cert = readCertificate(file);
            if (cert == null) {
                return;
            }
            File deleted = getCertificateFile(this.deletedDir, cert);
            if (deleted.exists()) {
                return;
            }
            writeCertificate(deleted, cert);
            return;
        }
        if (!isUser(alias)) {
            return;
        }
        new FileOutputStream(file).close();
        removeUnnecessaryTombstones(alias);
    }

    private void removeUnnecessaryTombstones(String alias) throws IOException {
        if (!isUser(alias)) {
            throw new AssertionError(alias);
        }
        int dotIndex = alias.lastIndexOf(46);
        if (dotIndex == -1) {
            throw new AssertionError(alias);
        }
        String hash = alias.substring(PREFIX_USER.length(), dotIndex);
        int lastTombstoneIndex = Integer.parseInt(alias.substring(dotIndex + 1));
        if (file(this.addedDir, hash, lastTombstoneIndex + 1).exists()) {
            return;
        }
        while (lastTombstoneIndex >= 0) {
            File file = file(this.addedDir, hash, lastTombstoneIndex);
            if (!isTombstone(file)) {
                return;
            }
            if (!file.delete()) {
                throw new IOException("Could not remove " + file);
            }
            lastTombstoneIndex--;
        }
    }
}
