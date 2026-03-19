package java.security.cert;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;

public class CertPathValidatorException extends GeneralSecurityException {
    private static final long serialVersionUID = -3083180014971893139L;
    private CertPath certPath;
    private int index;
    private Reason reason;

    public enum BasicReason implements Reason {
        UNSPECIFIED,
        EXPIRED,
        NOT_YET_VALID,
        REVOKED,
        UNDETERMINED_REVOCATION_STATUS,
        INVALID_SIGNATURE,
        ALGORITHM_CONSTRAINED
    }

    public interface Reason extends Serializable {
    }

    public CertPathValidatorException() {
        this(null, null);
    }

    public CertPathValidatorException(String str) {
        this(str, null);
    }

    public CertPathValidatorException(Throwable th) {
        this(th == null ? null : th.toString(), th);
    }

    public CertPathValidatorException(String str, Throwable th) {
        this(str, th, null, -1);
    }

    public CertPathValidatorException(String str, Throwable th, CertPath certPath, int i) {
        this(str, th, certPath, i, BasicReason.UNSPECIFIED);
    }

    public CertPathValidatorException(String str, Throwable th, CertPath certPath, int i, Reason reason) {
        super(str, th);
        this.index = -1;
        this.reason = BasicReason.UNSPECIFIED;
        if (certPath == null && i != -1) {
            throw new IllegalArgumentException();
        }
        if (i < -1 || (certPath != null && i >= certPath.getCertificates().size())) {
            throw new IndexOutOfBoundsException();
        }
        if (reason == null) {
            throw new NullPointerException("reason can't be null");
        }
        this.certPath = certPath;
        this.index = i;
        this.reason = reason;
    }

    public CertPath getCertPath() {
        return this.certPath;
    }

    public int getIndex() {
        return this.index;
    }

    public Reason getReason() {
        return this.reason;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.reason == null) {
            this.reason = BasicReason.UNSPECIFIED;
        }
        if (this.certPath == null && this.index != -1) {
            throw new InvalidObjectException("certpath is null and index != -1");
        }
        if (this.index < -1 || (this.certPath != null && this.index >= this.certPath.getCertificates().size())) {
            throw new InvalidObjectException("index out of range");
        }
    }
}
