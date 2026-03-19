package android.os;

public class ServiceSpecificException extends RuntimeException {
    public final int errorCode;

    public ServiceSpecificException(int i, String str) {
        super(str);
        this.errorCode = i;
    }

    public ServiceSpecificException(int i) {
        this.errorCode = i;
    }

    @Override
    public String toString() {
        return super.toString() + " (code " + this.errorCode + ")";
    }
}
