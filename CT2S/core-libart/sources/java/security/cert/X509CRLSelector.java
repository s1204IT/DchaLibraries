package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.x501.Name;

public class X509CRLSelector implements CRLSelector {
    private X509Certificate certificateChecking;
    private long dateAndTime = -1;
    private ArrayList<String> issuerNames;
    private ArrayList<X500Principal> issuerPrincipals;
    private BigInteger maxCRL;
    private BigInteger minCRL;

    public void setIssuers(Collection<X500Principal> issuers) {
        if (issuers == null) {
            this.issuerNames = null;
            this.issuerPrincipals = null;
            return;
        }
        this.issuerNames = new ArrayList<>(issuers.size());
        this.issuerPrincipals = new ArrayList<>(issuers);
        for (X500Principal issuer : issuers) {
            this.issuerNames.add(issuer.getName(X500Principal.CANONICAL));
        }
    }

    public void setIssuerNames(Collection<?> names) throws IOException {
        if (names == null) {
            this.issuerNames = null;
            this.issuerPrincipals = null;
            return;
        }
        if (names.size() != 0) {
            this.issuerNames = new ArrayList<>(names.size());
            for (Object name : names) {
                if (name instanceof String) {
                    this.issuerNames.add(new Name((String) name).getName(X500Principal.CANONICAL));
                } else if (name instanceof byte[]) {
                    this.issuerNames.add(new Name((byte[]) name).getName(X500Principal.CANONICAL));
                } else {
                    throw new IOException("name neither a String nor a byte[]");
                }
            }
        }
    }

    public void addIssuer(X500Principal issuer) {
        if (issuer == null) {
            throw new NullPointerException("issuer == null");
        }
        if (this.issuerNames == null) {
            this.issuerNames = new ArrayList<>();
        }
        String name = issuer.getName(X500Principal.CANONICAL);
        if (!this.issuerNames.contains(name)) {
            this.issuerNames.add(name);
        }
        if (this.issuerPrincipals == null) {
            this.issuerPrincipals = new ArrayList<>(this.issuerNames.size());
        }
        int size = this.issuerNames.size() - 1;
        for (int i = this.issuerPrincipals.size(); i < size; i++) {
            this.issuerPrincipals.add(new X500Principal(this.issuerNames.get(i)));
        }
        this.issuerPrincipals.add(issuer);
    }

    public void addIssuerName(String iss_name) throws IOException {
        if (this.issuerNames == null) {
            this.issuerNames = new ArrayList<>();
        }
        if (iss_name == null) {
            iss_name = "";
        }
        String name = new Name(iss_name).getName(X500Principal.CANONICAL);
        if (!this.issuerNames.contains(name)) {
            this.issuerNames.add(name);
        }
    }

    public void addIssuerName(byte[] iss_name) throws IOException {
        if (iss_name == null) {
            throw new NullPointerException("iss_name == null");
        }
        if (this.issuerNames == null) {
            this.issuerNames = new ArrayList<>();
        }
        String name = new Name(iss_name).getName(X500Principal.CANONICAL);
        if (!this.issuerNames.contains(name)) {
            this.issuerNames.add(name);
        }
    }

    public void setMinCRLNumber(BigInteger minCRL) {
        this.minCRL = minCRL;
    }

    public void setMaxCRLNumber(BigInteger maxCRL) {
        this.maxCRL = maxCRL;
    }

    public void setDateAndTime(Date dateAndTime) {
        if (dateAndTime == null) {
            this.dateAndTime = -1L;
        } else {
            this.dateAndTime = dateAndTime.getTime();
        }
    }

    public void setCertificateChecking(X509Certificate cert) {
        this.certificateChecking = cert;
    }

    public Collection<X500Principal> getIssuers() {
        if (this.issuerNames == null) {
            return null;
        }
        if (this.issuerPrincipals == null) {
            this.issuerPrincipals = new ArrayList<>(this.issuerNames.size());
        }
        int size = this.issuerNames.size();
        for (int i = this.issuerPrincipals.size(); i < size; i++) {
            this.issuerPrincipals.add(new X500Principal(this.issuerNames.get(i)));
        }
        return Collections.unmodifiableCollection(this.issuerPrincipals);
    }

    public Collection<Object> getIssuerNames() {
        if (this.issuerNames == null) {
            return null;
        }
        return (Collection) this.issuerNames.clone();
    }

    public BigInteger getMinCRL() {
        return this.minCRL;
    }

    public BigInteger getMaxCRL() {
        return this.maxCRL;
    }

    public Date getDateAndTime() {
        if (this.dateAndTime == -1) {
            return null;
        }
        return new Date(this.dateAndTime);
    }

    public X509Certificate getCertificateChecking() {
        return this.certificateChecking;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("X509CRLSelector:\n[");
        if (this.issuerNames != null) {
            result.append("\n  IssuerNames:\n  [");
            int size = this.issuerNames.size();
            for (int i = 0; i < size; i++) {
                result.append("\n    " + this.issuerNames.get(i));
            }
            result.append("\n  ]");
        }
        if (this.minCRL != null) {
            result.append("\n  minCRL: " + this.minCRL);
        }
        if (this.maxCRL != null) {
            result.append("\n  maxCRL: " + this.maxCRL);
        }
        if (this.dateAndTime != -1) {
            result.append("\n  dateAndTime: " + new Date(this.dateAndTime));
        }
        if (this.certificateChecking != null) {
            result.append("\n  certificateChecking: " + this.certificateChecking);
        }
        result.append("\n]");
        return result.toString();
    }

    @Override
    public boolean match(CRL crl) {
        if (!(crl instanceof X509CRL)) {
            return false;
        }
        X509CRL crlist = (X509CRL) crl;
        if (this.issuerNames != null && !this.issuerNames.contains(crlist.getIssuerX500Principal().getName(X500Principal.CANONICAL))) {
            return false;
        }
        if (this.minCRL != null || this.maxCRL != null) {
            try {
                byte[] bytes = crlist.getExtensionValue("2.5.29.20");
                BigInteger crlNumber = new BigInteger((byte[]) ASN1Integer.getInstance().decode((byte[]) ASN1OctetString.getInstance().decode(bytes)));
                if (this.minCRL != null && crlNumber.compareTo(this.minCRL) < 0) {
                    return false;
                }
                if (this.maxCRL != null) {
                    if (crlNumber.compareTo(this.maxCRL) > 0) {
                        return false;
                    }
                }
            } catch (IOException e) {
                return false;
            }
        }
        if (this.dateAndTime != -1) {
            Date thisUp = crlist.getThisUpdate();
            Date nextUp = crlist.getNextUpdate();
            if (thisUp == null || nextUp == null) {
                return false;
            }
            if (this.dateAndTime < thisUp.getTime() || this.dateAndTime > nextUp.getTime()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object clone() {
        try {
            X509CRLSelector result = (X509CRLSelector) super.clone();
            if (this.issuerNames != null) {
                result.issuerNames = new ArrayList<>(this.issuerNames);
                return result;
            }
            return result;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
