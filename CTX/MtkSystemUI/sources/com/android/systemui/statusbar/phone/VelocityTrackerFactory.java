package com.android.systemui.statusbar.phone;

import android.content.Context;
import com.android.systemui.R;

public class VelocityTrackerFactory {
    public static VelocityTrackerInterface obtain(Context context) {
        byte b;
        String string = context.getResources().getString(R.string.velocity_tracker_impl);
        int iHashCode = string.hashCode();
        if (iHashCode != 104998702) {
            b = (iHashCode == 1874684019 && string.equals("platform")) ? (byte) 1 : (byte) -1;
        } else if (string.equals("noisy")) {
            b = 0;
        }
        switch (b) {
            case 0:
                return NoisyVelocityTracker.obtain();
            case 1:
                return PlatformVelocityTracker.obtain();
            default:
                throw new IllegalStateException("Invalid tracker: " + string);
        }
    }
}
