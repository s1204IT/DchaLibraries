package android.support.v4.view;

import android.view.MotionEvent;

public final class MotionEventCompat {
    @Deprecated
    public static int getActionMasked(MotionEvent event) {
        return event.getActionMasked();
    }

    public static boolean isFromSource(MotionEvent event, int source) {
        return (event.getSource() & source) == source;
    }
}
