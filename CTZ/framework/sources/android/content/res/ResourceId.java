package android.content.res;

import android.text.Spanned;

public final class ResourceId {
    public static final int ID_NULL = 0;

    public static boolean isValid(int i) {
        return (i == -1 || ((-16777216) & i) == 0 || (i & Spanned.SPAN_PRIORITY) == 0) ? false : true;
    }
}
