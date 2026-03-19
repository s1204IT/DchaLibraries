package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.x509.CRLNumberExtension;
import sun.security.x509.X500Name;

public class X509CRLSelector implements CRLSelector {
    private static final Debug debug;
    private X509Certificate certChecking;
    private Date dateAndTime;
    private HashSet<Object> issuerNames;
    private HashSet<X500Principal> issuerX500Principals;
    private BigInteger maxCRL;
    private BigInteger minCRL;
    private long skew = 0;

    static {
        CertPathHelperImpl.initialize();
        debug = Debug.getInstance("certpath");
    }

    public void setIssuers(Collection<X500Principal> collection) {
        if (collection == null || collection.isEmpty()) {
            this.issuerNames = null;
            this.issuerX500Principals = null;
            return;
        }
        this.issuerX500Principals = new HashSet<>(collection);
        this.issuerNames = new HashSet<>();
        Iterator<X500Principal> it = this.issuerX500Principals.iterator();
        while (it.hasNext()) {
            this.issuerNames.add(it.next().getEncoded());
        }
    }

    public void setIssuerNames(Collection<?> collection) throws IOException {
        if (collection == null || collection.size() == 0) {
            this.issuerNames = null;
            this.issuerX500Principals = null;
        } else {
            HashSet<Object> hashSetCloneAndCheckIssuerNames = cloneAndCheckIssuerNames(collection);
            this.issuerX500Principals = parseIssuerNames(hashSetCloneAndCheckIssuerNames);
            this.issuerNames = hashSetCloneAndCheckIssuerNames;
        }
    }

    public void addIssuer(X500Principal x500Principal) {
        addIssuerNameInternal(x500Principal.getEncoded(), x500Principal);
    }

    public void addIssuerName(String str) throws IOException {
        addIssuerNameInternal(str, new X500Name(str).asX500Principal());
    }

    public void addIssuerName(byte[] bArr) throws IOException {
        addIssuerNameInternal(bArr.clone(), new X500Name(bArr).asX500Principal());
    }

    private void addIssuerNameInternal(Object obj, X500Principal x500Principal) {
        if (this.issuerNames == null) {
            this.issuerNames = new HashSet<>();
        }
        if (this.issuerX500Principals == null) {
            this.issuerX500Principals = new HashSet<>();
        }
        this.issuerNames.add(obj);
        this.issuerX500Principals.add(x500Principal);
    }

    private static HashSet<Object> cloneAndCheckIssuerNames(Collection<?> collection) throws IOException {
        HashSet<Object> hashSet = new HashSet<>();
        for (Object obj : collection) {
            boolean z = obj instanceof byte[];
            if (!z && !(obj instanceof String)) {
                throw new IOException("name not byte array or String");
            }
            if (z) {
                hashSet.add(((byte[]) obj).clone());
            } else {
                hashSet.add(obj);
            }
        }
        return hashSet;
    }

    private static HashSet<Object> cloneIssuerNames(Collection<Object> collection) {
        try {
            return cloneAndCheckIssuerNames(collection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HashSet<X500Principal> parseIssuerNames(Collection<Object> collection) throws IOException {
        HashSet<X500Principal> hashSet = new HashSet<>();
        for (Object obj : collection) {
            if (obj instanceof String) {
                hashSet.add(new X500Name((String) obj).asX500Principal());
            } else {
                try {
                    hashSet.add(new X500Principal((byte[]) obj));
                } catch (IllegalArgumentException e) {
                    throw ((IOException) new IOException("Invalid name").initCause(e));
                }
            }
        }
        return hashSet;
    }

    public void setMinCRLNumber(BigInteger bigInteger) {
        this.minCRL = bigInteger;
    }

    public void setMaxCRLNumber(BigInteger bigInteger) {
        this.maxCRL = bigInteger;
    }

    public void setDateAndTime(Date date) {
        if (date == null) {
            this.dateAndTime = null;
        } else {
            this.dateAndTime = new Date(date.getTime());
        }
        this.skew = 0L;
    }

    void setDateAndTime(Date date, long j) {
        this.dateAndTime = date == null ? null : new Date(date.getTime());
        this.skew = j;
    }

    public void setCertificateChecking(X509Certificate x509Certificate) {
        this.certChecking = x509Certificate;
    }

    public Collection<X500Principal> getIssuers() {
        if (this.issuerX500Principals == null) {
            return null;
        }
        return Collections.unmodifiableCollection(this.issuerX500Principals);
    }

    public Collection<Object> getIssuerNames() {
        if (this.issuerNames == null) {
            return null;
        }
        return cloneIssuerNames(this.issuerNames);
    }

    public BigInteger getMinCRL() {
        return this.minCRL;
    }

    public BigInteger getMaxCRL() {
        return this.maxCRL;
    }

    public Date getDateAndTime() {
        if (this.dateAndTime == null) {
            return null;
        }
        return (Date) this.dateAndTime.clone();
    }

    public X509Certificate getCertificateChecking() {
        return this.certChecking;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("X509CRLSelector: [\n");
        if (this.issuerNames != null) {
            stringBuffer.append("  IssuerNames:\n");
            Iterator<Object> it = this.issuerNames.iterator();
            while (it.hasNext()) {
                stringBuffer.append("    " + it.next() + "\n");
            }
        }
        if (this.minCRL != null) {
            stringBuffer.append("  minCRLNumber: " + ((Object) this.minCRL) + "\n");
        }
        if (this.maxCRL != null) {
            stringBuffer.append("  maxCRLNumber: " + ((Object) this.maxCRL) + "\n");
        }
        if (this.dateAndTime != null) {
            stringBuffer.append("  dateAndTime: " + ((Object) this.dateAndTime) + "\n");
        }
        if (this.certChecking != null) {
            stringBuffer.append("  Certificate being checked: " + ((Object) this.certChecking) + "\n");
        }
        stringBuffer.append("]");
        return stringBuffer.toString();
    }

    @Override
    public boolean match(CRL crl) {
        if (!(crl instanceof X509CRL)) {
            return false;
        }
        X509CRL x509crl = (X509CRL) crl;
        if (this.issuerNames != null) {
            X500Principal issuerX500Principal = x509crl.getIssuerX500Principal();
            Iterator<X500Principal> it = this.issuerX500Principals.iterator();
            boolean z = false;
            while (!z && it.hasNext()) {
                if (it.next().equals(issuerX500Principal)) {
                    z = true;
                }
            }
            if (!z) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: issuer DNs don't match");
                }
                return false;
            }
        }
        if (this.minCRL != null || this.maxCRL != null) {
            byte[] extensionValue = x509crl.getExtensionValue("2.5.29.20");
            if (extensionValue == null && debug != null) {
                debug.println("X509CRLSelector.match: no CRLNumber");
            }
            try {
                BigInteger bigInteger = new CRLNumberExtension(Boolean.FALSE, new DerInputStream(extensionValue).getOctetString()).get("value");
                if (this.minCRL != null && bigInteger.compareTo(this.minCRL) < 0) {
                    if (debug != null) {
                        debug.println("X509CRLSelector.match: CRLNumber too small");
                    }
                    return false;
                }
                if (this.maxCRL != null && bigInteger.compareTo(this.maxCRL) > 0) {
                    if (debug != null) {
                        debug.println("X509CRLSelector.match: CRLNumber too large");
                    }
                    return false;
                }
            } catch (IOException e) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: exception in decoding CRL number");
                }
                return false;
            }
        }
        if (this.dateAndTime != null) {
            Date thisUpdate = x509crl.getThisUpdate();
            Date nextUpdate = x509crl.getNextUpdate();
            if (nextUpdate == null) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: nextUpdate null");
                }
                return false;
            }
            Date date = this.dateAndTime;
            Date date2 = this.dateAndTime;
            if (this.skew > 0) {
                date = new Date(this.dateAndTime.getTime() + this.skew);
                date2 = new Date(this.dateAndTime.getTime() - this.skew);
            }
            if (date2.after(nextUpdate) || date.before(thisUpdate)) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: update out-of-range");
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public Object clone() {
        try {
            X509CRLSelector x509CRLSelector = (X509CRLSelector) super.clone();
            if (this.issuerNames != null) {
                x509CRLSelector.issuerNames = new HashSet<>(this.issuerNames);
                x509CRLSelector.issuerX500Principals = new HashSet<>(this.issuerX500Principals);
            }
            return x509CRLSelector;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }
}
