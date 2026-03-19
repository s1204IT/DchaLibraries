package java.security.cert;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public abstract class PKIXCertPathChecker implements CertPathChecker, Cloneable {
    public abstract void check(Certificate certificate, Collection<String> collection) throws CertPathValidatorException;

    public abstract Set<String> getSupportedExtensions();

    @Override
    public abstract void init(boolean z) throws CertPathValidatorException;

    @Override
    public abstract boolean isForwardCheckingSupported();

    protected PKIXCertPathChecker() {
    }

    @Override
    public void check(Certificate certificate) throws CertPathValidatorException {
        check(certificate, Collections.emptySet());
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }
}
