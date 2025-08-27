package android.support.v4.util;

/* loaded from: classes.dex */
public class Preconditions {
    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }
}
