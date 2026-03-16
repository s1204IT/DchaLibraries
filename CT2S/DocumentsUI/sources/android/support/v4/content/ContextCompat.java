package android.support.v4.content;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;

public class ContextCompat {
    public static final Drawable getDrawable(Context context, int id) {
        int version = Build.VERSION.SDK_INT;
        return version >= 21 ? ContextCompatApi21.getDrawable(context, id) : context.getResources().getDrawable(id);
    }
}
