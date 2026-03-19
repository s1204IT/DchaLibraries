package com.android.org.conscrypt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private final File addedDir;
    private final File deletedDir;
    private final File systemDir;

    private interface CertSelector {
        boolean match(X509Certificate x509Certificate);
    }

    public static final boolean isSystem(String str) {
        return str.startsWith(PREFIX_SYSTEM);
    }

    public static final boolean isUser(String str) {
        return str.startsWith(PREFIX_USER);
    }

    private static class PreloadHolder {
        private static File defaultCaCertsAddedDir;
        private static File defaultCaCertsDeletedDir;
        private static File defaultCaCertsSystemDir;

        private PreloadHolder() {
        }

        static {
            String str = System.getenv("ANDROID_ROOT");
            String str2 = System.getenv("ANDROID_DATA");
            defaultCaCertsSystemDir = new File(str + "/etc/security/cacerts");
            TrustedCertificateStore.setDefaultUserDirectory(new File(str2 + "/misc/keychain"));
        }
    }

    static {
        try {
            CERT_FACTORY = CertificateFactory.getInstance("X509");
        } catch (CertificateException e) {
            throw new AssertionError(e);
        }
    }

    public static void setDefaultUserDirectory(File file) {
        File unused = PreloadHolder.defaultCaCertsAddedDir = new File(file, "cacerts-added");
        File unused2 = PreloadHolder.defaultCaCertsDeletedDir = new File(file, "cacerts-removed");
    }

    public TrustedCertificateStore() {
        this(PreloadHolder.defaultCaCertsSystemDir, PreloadHolder.defaultCaCertsAddedDir, PreloadHolder.defaultCaCertsDeletedDir);
    }

    public TrustedCertificateStore(File file, File file2, File file3) {
        this.systemDir = file;
        this.addedDir = file2;
        this.deletedDir = file3;
    }

    public Certificate getCertificate(String str) {
        return getCertificate(str, false);
    }

    public Certificate getCertificate(String str, boolean z) {
        X509Certificate certificate;
        File fileFileForAlias = fileForAlias(str);
        if (fileFileForAlias == null || ((isUser(str) && isTombstone(fileFileForAlias)) || (certificate = readCertificate(fileFileForAlias)) == null || (isSystem(str) && !z && isDeletedSystemCertificate(certificate)))) {
            return null;
        }
        return certificate;
    }

    private File fileForAlias(String str) {
        File file;
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        if (isSystem(str)) {
            file = new File(this.systemDir, str.substring(PREFIX_SYSTEM.length()));
        } else {
            if (!isUser(str)) {
                return null;
            }
            file = new File(this.addedDir, str.substring(PREFIX_USER.length()));
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
        BufferedInputStream bufferedInputStream;
        BufferedInputStream bufferedInputStream2 = null;
        if (!file.isFile()) {
            return null;
        }
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        } catch (IOException e) {
            bufferedInputStream = null;
        } catch (CertificateException e2) {
            bufferedInputStream = null;
        } catch (Throwable th) {
            th = th;
        }
        try {
            X509Certificate x509Certificate = (X509Certificate) CERT_FACTORY.generateCertificate(bufferedInputStream);
            IoUtils.closeQuietly(bufferedInputStream);
            return x509Certificate;
        } catch (IOException e3) {
            IoUtils.closeQuietly(bufferedInputStream);
            return null;
        } catch (CertificateException e4) {
            IoUtils.closeQuietly(bufferedInputStream);
            return null;
        } catch (Throwable th2) {
            th = th2;
            bufferedInputStream2 = bufferedInputStream;
            IoUtils.closeQuietly(bufferedInputStream2);
            throw th;
        }
    }

    private void writeCertificate(File file, X509Certificate x509Certificate) throws Throwable {
        File parentFile = file.getParentFile();
        parentFile.mkdirs();
        parentFile.setReadable(true, false);
        parentFile.setExecutable(true, false);
        FileOutputStream fileOutputStream = null;
        try {
            FileOutputStream fileOutputStream2 = new FileOutputStream(file);
            try {
                fileOutputStream2.write(x509Certificate.getEncoded());
                IoUtils.closeQuietly(fileOutputStream2);
                file.setReadable(true, false);
            } catch (Throwable th) {
                th = th;
                fileOutputStream = fileOutputStream2;
                IoUtils.closeQuietly(fileOutputStream);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private boolean isDeletedSystemCertificate(X509Certificate x509Certificate) {
        return getCertificateFile(this.deletedDir, x509Certificate).exists();
    }

    public Date getCreationDate(String str) {
        File fileFileForAlias;
        if (!containsAlias(str) || (fileFileForAlias = fileForAlias(str)) == null) {
            return null;
        }
        long jLastModified = fileFileForAlias.lastModified();
        if (jLastModified == 0) {
            return null;
        }
        return new Date(jLastModified);
    }

    public Set<String> aliases() {
        HashSet hashSet = new HashSet();
        addAliases(hashSet, PREFIX_USER, this.addedDir);
        addAliases(hashSet, PREFIX_SYSTEM, this.systemDir);
        return hashSet;
    }

    public Set<String> userAliases() {
        HashSet hashSet = new HashSet();
        addAliases(hashSet, PREFIX_USER, this.addedDir);
        return hashSet;
    }

    private void addAliases(Set<String> set, String str, File file) {
        String[] list = file.list();
        if (list == null) {
            return;
        }
        for (String str2 : list) {
            String str3 = str + str2;
            if (containsAlias(str3)) {
                set.add(str3);
            }
        }
    }

    public Set<String> allSystemAliases() {
        HashSet hashSet = new HashSet();
        String[] list = this.systemDir.list();
        if (list == null) {
            return hashSet;
        }
        for (String str : list) {
            String str2 = PREFIX_SYSTEM + str;
            if (containsAlias(str2, true)) {
                hashSet.add(str2);
            }
        }
        return hashSet;
    }

    public boolean containsAlias(String str) {
        return containsAlias(str, false);
    }

    private boolean containsAlias(String str, boolean z) {
        return getCertificate(str, z) != null;
    }

    public String getCertificateAlias(Certificate certificate) {
        return getCertificateAlias(certificate, false);
    }

    public String getCertificateAlias(Certificate certificate, boolean z) {
        if (certificate == null || !(certificate instanceof X509Certificate)) {
            return null;
        }
        X509Certificate x509Certificate = (X509Certificate) certificate;
        File certificateFile = getCertificateFile(this.addedDir, x509Certificate);
        if (certificateFile.exists()) {
            return PREFIX_USER + certificateFile.getName();
        }
        if (!z && isDeletedSystemCertificate(x509Certificate)) {
            return null;
        }
        File certificateFile2 = getCertificateFile(this.systemDir, x509Certificate);
        if (!certificateFile2.exists()) {
            return null;
        }
        return PREFIX_SYSTEM + certificateFile2.getName();
    }

    public boolean isUserAddedCertificate(X509Certificate x509Certificate) {
        return getCertificateFile(this.addedDir, x509Certificate).exists();
    }

    public File getCertificateFile(File file, final X509Certificate x509Certificate) {
        return (File) findCert(file, x509Certificate.getSubjectX500Principal(), new CertSelector() {
            @Override
            public boolean match(X509Certificate x509Certificate2) {
                return x509Certificate2.equals(x509Certificate);
            }
        }, File.class);
    }

    public X509Certificate getTrustAnchor(final X509Certificate x509Certificate) {
        CertSelector certSelector = new CertSelector() {
            @Override
            public boolean match(X509Certificate x509Certificate2) {
                return x509Certificate2.getPublicKey().equals(x509Certificate.getPublicKey());
            }
        };
        X509Certificate x509Certificate2 = (X509Certificate) findCert(this.addedDir, x509Certificate.getSubjectX500Principal(), certSelector, X509Certificate.class);
        if (x509Certificate2 != null) {
            return x509Certificate2;
        }
        X509Certificate x509Certificate3 = (X509Certificate) findCert(this.systemDir, x509Certificate.getSubjectX500Principal(), certSelector, X509Certificate.class);
        if (x509Certificate3 != null && !isDeletedSystemCertificate(x509Certificate3)) {
            return x509Certificate3;
        }
        return null;
    }

    public X509Certificate findIssuer(final X509Certificate x509Certificate) {
        CertSelector certSelector = new CertSelector() {
            @Override
            public boolean match(X509Certificate x509Certificate2) {
                try {
                    x509Certificate.verify(x509Certificate2.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
        X500Principal issuerX500Principal = x509Certificate.getIssuerX500Principal();
        X509Certificate x509Certificate2 = (X509Certificate) findCert(this.addedDir, issuerX500Principal, certSelector, X509Certificate.class);
        if (x509Certificate2 != null) {
            return x509Certificate2;
        }
        X509Certificate x509Certificate3 = (X509Certificate) findCert(this.systemDir, issuerX500Principal, certSelector, X509Certificate.class);
        if (x509Certificate3 != null && !isDeletedSystemCertificate(x509Certificate3)) {
            return x509Certificate3;
        }
        return null;
    }

    public Set<X509Certificate> findAllIssuers(final X509Certificate x509Certificate) {
        CertSelector certSelector = new CertSelector() {
            @Override
            public boolean match(X509Certificate x509Certificate2) {
                try {
                    x509Certificate.verify(x509Certificate2.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
        X500Principal issuerX500Principal = x509Certificate.getIssuerX500Principal();
        Set<X509Certificate> set = (Set) findCert(this.addedDir, issuerX500Principal, certSelector, Set.class);
        if (set == null) {
            set = null;
        }
        Set<X509Certificate> set2 = (Set) findCert(this.systemDir, issuerX500Principal, new CertSelector() {
            @Override
            public boolean match(X509Certificate x509Certificate2) {
                try {
                    if (TrustedCertificateStore.this.isDeletedSystemCertificate(x509Certificate2)) {
                        return false;
                    }
                    x509Certificate.verify(x509Certificate2.getPublicKey());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }, Set.class);
        if (set2 != null) {
            if (set != null) {
                set.addAll(set2);
                set2 = set;
            }
        } else {
            set2 = set;
        }
        return set2 != null ? set2 : Collections.emptySet();
    }

    private static boolean isSelfIssuedCertificate(OpenSSLX509Certificate openSSLX509Certificate) {
        long context = openSSLX509Certificate.getContext();
        return NativeCrypto.X509_check_issued(context, openSSLX509Certificate, context, openSSLX509Certificate) == 0;
    }

    private static OpenSSLX509Certificate convertToOpenSSLIfNeeded(X509Certificate x509Certificate) throws CertificateException {
        if (x509Certificate == null) {
            return null;
        }
        if (x509Certificate instanceof OpenSSLX509Certificate) {
            return (OpenSSLX509Certificate) x509Certificate;
        }
        try {
            return OpenSSLX509Certificate.fromX509Der(x509Certificate.getEncoded());
        } catch (Exception e) {
            throw new CertificateException(e);
        }
    }

    public List<X509Certificate> getCertificateChain(X509Certificate x509Certificate) throws CertificateException {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        OpenSSLX509Certificate openSSLX509CertificateConvertToOpenSSLIfNeeded = convertToOpenSSLIfNeeded(x509Certificate);
        linkedHashSet.add(openSSLX509CertificateConvertToOpenSSLIfNeeded);
        while (!isSelfIssuedCertificate(openSSLX509CertificateConvertToOpenSSLIfNeeded) && (openSSLX509CertificateConvertToOpenSSLIfNeeded = convertToOpenSSLIfNeeded(findIssuer(openSSLX509CertificateConvertToOpenSSLIfNeeded))) != null && !linkedHashSet.contains(openSSLX509CertificateConvertToOpenSSLIfNeeded)) {
            linkedHashSet.add(openSSLX509CertificateConvertToOpenSSLIfNeeded);
        }
        return new ArrayList(linkedHashSet);
    }

    private <T> T findCert(File file, X500Principal x500Principal, CertSelector certSelector, Class<T> cls) {
        ?? r4;
        String strHash = hash(x500Principal);
        int i = 0;
        T t = null;
        while (true) {
            ?? r3 = (T) file(file, strHash, i);
            if (!r3.isFile()) {
                if (cls == Boolean.class) {
                    return (T) Boolean.FALSE;
                }
                if (cls == File.class) {
                    return r3;
                }
                if (cls == Set.class) {
                    return t;
                }
                return null;
            }
            if (!isTombstone(r3) && (r4 = (T) readCertificate(r3)) != 0 && certSelector.match(r4)) {
                if (cls == X509Certificate.class) {
                    return r4;
                }
                if (cls == Boolean.class) {
                    return (T) Boolean.TRUE;
                }
                if (cls == File.class) {
                    return r3;
                }
                if (cls == Set.class) {
                    if (t == null) {
                        t = (T) new HashSet();
                    }
                    ((Set) t).add(r4);
                } else {
                    throw new AssertionError();
                }
            }
            i++;
        }
    }

    private String hash(X500Principal x500Principal) {
        return Hex.intToHexString(NativeCrypto.X509_NAME_hash_old(x500Principal), 8);
    }

    private File file(File file, String str, int i) {
        return new File(file, str + '.' + i);
    }

    public void installCertificate(X509Certificate x509Certificate) throws Throwable {
        if (x509Certificate == null) {
            throw new NullPointerException("cert == null");
        }
        if (getCertificateFile(this.systemDir, x509Certificate).exists()) {
            File certificateFile = getCertificateFile(this.deletedDir, x509Certificate);
            if (certificateFile.exists() && !certificateFile.delete()) {
                throw new IOException("Could not remove " + certificateFile);
            }
            return;
        }
        File certificateFile2 = getCertificateFile(this.addedDir, x509Certificate);
        if (certificateFile2.exists()) {
            return;
        }
        writeCertificate(certificateFile2, x509Certificate);
    }

    public void deleteCertificateEntry(String str) throws Throwable {
        File fileFileForAlias;
        if (str == null || (fileFileForAlias = fileForAlias(str)) == null) {
            return;
        }
        if (isSystem(str)) {
            X509Certificate certificate = readCertificate(fileFileForAlias);
            if (certificate == null) {
                return;
            }
            File certificateFile = getCertificateFile(this.deletedDir, certificate);
            if (certificateFile.exists()) {
                return;
            }
            writeCertificate(certificateFile, certificate);
            return;
        }
        if (isUser(str)) {
            new FileOutputStream(fileFileForAlias).close();
            removeUnnecessaryTombstones(str);
        }
    }

    private void removeUnnecessaryTombstones(String str) throws IOException {
        if (!isUser(str)) {
            throw new AssertionError(str);
        }
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf == -1) {
            throw new AssertionError(str);
        }
        String strSubstring = str.substring(PREFIX_USER.length(), iLastIndexOf);
        int i = Integer.parseInt(str.substring(iLastIndexOf + 1));
        if (file(this.addedDir, strSubstring, i + 1).exists()) {
            return;
        }
        while (i >= 0) {
            File file = file(this.addedDir, strSubstring, i);
            if (isTombstone(file)) {
                if (!file.delete()) {
                    throw new IOException("Could not remove " + file);
                }
                i--;
            } else {
                return;
            }
        }
    }
}
