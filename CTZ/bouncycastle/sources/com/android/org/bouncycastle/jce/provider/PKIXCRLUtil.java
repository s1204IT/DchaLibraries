package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.jcajce.PKIXCRLStoreSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class PKIXCRLUtil {
    PKIXCRLUtil() {
    }

    public Set findCRLs(PKIXCRLStoreSelector pKIXCRLStoreSelector, Date date, List list, List list2) throws AnnotatedException {
        HashSet<X509CRL> hashSet = new HashSet();
        try {
            hashSet.addAll(findCRLs(pKIXCRLStoreSelector, list2));
            hashSet.addAll(findCRLs(pKIXCRLStoreSelector, list));
            HashSet hashSet2 = new HashSet();
            for (X509CRL x509crl : hashSet) {
                if (x509crl.getNextUpdate().after(date)) {
                    X509Certificate certificateChecking = pKIXCRLStoreSelector.getCertificateChecking();
                    if (certificateChecking != null) {
                        if (x509crl.getThisUpdate().before(certificateChecking.getNotAfter())) {
                            hashSet2.add(x509crl);
                        }
                    } else {
                        hashSet2.add(x509crl);
                    }
                }
            }
            return hashSet2;
        } catch (AnnotatedException e) {
            throw new AnnotatedException("Exception obtaining complete CRLs.", e);
        }
    }

    private final Collection findCRLs(PKIXCRLStoreSelector pKIXCRLStoreSelector, List list) throws AnnotatedException {
        HashSet hashSet = new HashSet();
        Iterator it = list.iterator();
        AnnotatedException annotatedException = null;
        boolean z = false;
        while (it.hasNext()) {
            try {
                hashSet.addAll(PKIXCRLStoreSelector.getCRLs(pKIXCRLStoreSelector, (CertStore) it.next()));
                z = true;
            } catch (CertStoreException e) {
                annotatedException = new AnnotatedException("Exception searching in X.509 CRL store.", e);
            }
        }
        if (!z && annotatedException != null) {
            throw annotatedException;
        }
        return hashSet;
    }
}
