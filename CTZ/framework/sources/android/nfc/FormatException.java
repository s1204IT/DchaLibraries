package android.nfc;

public class FormatException extends Exception {
    public FormatException() {
    }

    public FormatException(String str) {
        super(str);
    }

    public FormatException(String str, Throwable th) {
        super(str, th);
    }
}
