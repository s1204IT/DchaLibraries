package sun.security.provider.certpath;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;

class ForwardState implements State {
    private static final Debug debug = Debug.getInstance("certpath");
    X509CertImpl cert;
    ArrayList<PKIXCertPathChecker> forwardCheckers;
    X500Principal issuerDN;
    HashSet<GeneralNameInterface> subjectNamesTraversed;
    int traversedCACerts;
    private boolean init = true;
    boolean keyParamsNeededFlag = false;

    ForwardState() {
    }

    @Override
    public boolean isInitial() {
        return this.init;
    }

    @Override
    public boolean keyParamsNeeded() {
        return this.keyParamsNeededFlag;
    }

    public String toString() {
        return "State [\n  issuerDN of last cert: " + ((Object) this.issuerDN) + "\n  traversedCACerts: " + this.traversedCACerts + "\n  init: " + String.valueOf(this.init) + "\n  keyParamsNeeded: " + String.valueOf(this.keyParamsNeededFlag) + "\n  subjectNamesTraversed: \n" + ((Object) this.subjectNamesTraversed) + "]\n";
    }

    public void initState(List<PKIXCertPathChecker> list) throws CertPathValidatorException {
        this.subjectNamesTraversed = new HashSet<>();
        this.traversedCACerts = 0;
        this.forwardCheckers = new ArrayList<>();
        for (PKIXCertPathChecker pKIXCertPathChecker : list) {
            if (pKIXCertPathChecker.isForwardCheckingSupported()) {
                pKIXCertPathChecker.init(true);
                this.forwardCheckers.add(pKIXCertPathChecker);
            }
        }
        this.init = true;
    }

    @Override
    public void updateState(X509Certificate x509Certificate) throws IOException, CertificateException, CertPathValidatorException {
        if (x509Certificate == null) {
            return;
        }
        X509CertImpl impl = X509CertImpl.toImpl(x509Certificate);
        if (PKIX.isDSAPublicKeyWithoutParams(impl.getPublicKey())) {
            this.keyParamsNeededFlag = true;
        }
        this.cert = impl;
        this.issuerDN = x509Certificate.getIssuerX500Principal();
        if (!X509CertImpl.isSelfIssued(x509Certificate) && !this.init && x509Certificate.getBasicConstraints() != -1) {
            this.traversedCACerts++;
        }
        if (this.init || !X509CertImpl.isSelfIssued(x509Certificate)) {
            this.subjectNamesTraversed.add(X500Name.asX500Name(x509Certificate.getSubjectX500Principal()));
            try {
                SubjectAlternativeNameExtension subjectAlternativeNameExtension = impl.getSubjectAlternativeNameExtension();
                if (subjectAlternativeNameExtension != null) {
                    Iterator<GeneralName> it = subjectAlternativeNameExtension.get(SubjectAlternativeNameExtension.SUBJECT_NAME).names().iterator();
                    while (it.hasNext()) {
                        this.subjectNamesTraversed.add(it.next().getName());
                    }
                }
            } catch (IOException e) {
                if (debug != null) {
                    debug.println("ForwardState.updateState() unexpected exception");
                    e.printStackTrace();
                }
                throw new CertPathValidatorException(e);
            }
        }
        this.init = false;
    }

    @Override
    public Object clone() {
        try {
            ForwardState forwardState = (ForwardState) super.clone();
            forwardState.forwardCheckers = (ArrayList) this.forwardCheckers.clone();
            ListIterator<PKIXCertPathChecker> listIterator = forwardState.forwardCheckers.listIterator();
            while (listIterator.hasNext()) {
                PKIXCertPathChecker next = listIterator.next();
                if (next instanceof Cloneable) {
                    listIterator.set((PKIXCertPathChecker) next.clone());
                }
            }
            forwardState.subjectNamesTraversed = (HashSet) this.subjectNamesTraversed.clone();
            return forwardState;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }
}
