package android.content;

public class ActivityNotFoundException extends RuntimeException {
    public ActivityNotFoundException() {
    }

    public ActivityNotFoundException(String str) {
        super(str);
    }
}
