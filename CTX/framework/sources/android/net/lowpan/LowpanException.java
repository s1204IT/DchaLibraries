package android.net.lowpan;

import android.os.ServiceSpecificException;
import android.util.AndroidException;

public class LowpanException extends AndroidException {
    public LowpanException() {
    }

    public LowpanException(String str) {
        super(str);
    }

    public LowpanException(String str, Throwable th) {
        super(str, th);
    }

    public LowpanException(Exception exc) {
        super(exc);
    }

    static LowpanException rethrowFromServiceSpecificException(ServiceSpecificException serviceSpecificException) throws LowpanException {
        switch (serviceSpecificException.errorCode) {
            case 2:
                throw new LowpanRuntimeException(serviceSpecificException.getMessage() != null ? serviceSpecificException.getMessage() : "Invalid argument", serviceSpecificException);
            case 3:
                throw new InterfaceDisabledException(serviceSpecificException);
            case 4:
                throw new WrongStateException(serviceSpecificException);
            case 5:
            case 6:
            case 8:
            case 9:
            default:
                throw new LowpanRuntimeException(serviceSpecificException);
            case 7:
                throw new LowpanRuntimeException(serviceSpecificException.getMessage() != null ? serviceSpecificException.getMessage() : "NCP problem", serviceSpecificException);
            case 10:
                throw new OperationCanceledException(serviceSpecificException);
            case 11:
                throw new LowpanException(serviceSpecificException.getMessage() != null ? serviceSpecificException.getMessage() : "Feature not supported", serviceSpecificException);
            case 12:
                throw new JoinFailedException(serviceSpecificException);
            case 13:
                throw new JoinFailedAtScanException(serviceSpecificException);
            case 14:
                throw new JoinFailedAtAuthException(serviceSpecificException);
            case 15:
                throw new NetworkAlreadyExistsException(serviceSpecificException);
        }
    }
}
