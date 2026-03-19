package android.security.keystore;

public class DeviceIdAttestationException extends Exception {
    public DeviceIdAttestationException(String str) {
        super(str);
    }

    public DeviceIdAttestationException(String str, Throwable th) {
        super(str, th);
    }
}
