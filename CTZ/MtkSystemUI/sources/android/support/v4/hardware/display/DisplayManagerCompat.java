package android.support.v4.hardware.display;

import android.content.Context;
import java.util.WeakHashMap;

public final class DisplayManagerCompat {
    private static final WeakHashMap<Context, DisplayManagerCompat> sInstances = new WeakHashMap<>();
    private final Context mContext;

    private DisplayManagerCompat(Context context) {
        this.mContext = context;
    }

    public static DisplayManagerCompat getInstance(Context context) {
        DisplayManagerCompat instance;
        synchronized (sInstances) {
            instance = sInstances.get(context);
            if (instance == null) {
                instance = new DisplayManagerCompat(context);
                sInstances.put(context, instance);
            }
        }
        return instance;
    }
}
