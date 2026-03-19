package java.util.prefs;

public class InvalidPreferencesFormatException extends Exception {
    private static final long serialVersionUID = -791715184232119669L;

    public InvalidPreferencesFormatException(Throwable th) {
        super(th);
    }

    public InvalidPreferencesFormatException(String str) {
        super(str);
    }

    public InvalidPreferencesFormatException(String str, Throwable th) {
        super(str, th);
    }
}
