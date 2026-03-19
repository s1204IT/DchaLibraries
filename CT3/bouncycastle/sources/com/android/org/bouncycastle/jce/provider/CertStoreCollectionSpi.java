package com.android.org.bouncycastle.jce.provider;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CRL;
import java.security.cert.CRLSelector;
import java.security.cert.CertSelector;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertStoreSpi;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CertStoreCollectionSpi extends CertStoreSpi {
    private CollectionCertStoreParameters params;

    public CertStoreCollectionSpi(CertStoreParameters certStoreParameters) throws InvalidAlgorithmParameterException {
        super(certStoreParameters);
        if (certStoreParameters instanceof CollectionCertStoreParameters) {
            this.params = certStoreParameters;
            return;
        }
        throw new InvalidAlgorithmParameterException("org.bouncycastle.jce.provider.CertStoreCollectionSpi: parameter must be a CollectionCertStoreParameters object\n" + certStoreParameters.toString());
    }

    @Override
    public Collection engineGetCertificates(CertSelector certSelector) throws CertStoreException {
        List col = new ArrayList();
        Iterator<?> it = this.params.getCollection().iterator();
        if (certSelector == 0) {
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof Certificate) {
                    col.add(obj);
                }
            }
        } else {
            while (it.hasNext()) {
                ?? next = it.next();
                if ((next instanceof Certificate) && certSelector.match(next)) {
                    col.add(next);
                }
            }
        }
        return col;
    }

    @Override
    public Collection engineGetCRLs(CRLSelector cRLSelector) throws CertStoreException {
        List col = new ArrayList();
        Iterator<?> it = this.params.getCollection().iterator();
        if (cRLSelector == 0) {
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof CRL) {
                    col.add(obj);
                }
            }
        } else {
            while (it.hasNext()) {
                ?? next = it.next();
                if ((next instanceof CRL) && cRLSelector.match(next)) {
                    col.add(next);
                }
            }
        }
        return col;
    }
}
