package com.android.systemui.statusbar.phone;

import android.content.Context;
import com.android.systemui.R;

public class VelocityTrackerFactory {
    public static VelocityTrackerInterface obtain(Context ctx) {
        String tracker;
        tracker = ctx.getResources().getString(R.string.velocity_tracker_impl);
        switch (tracker) {
            case "noisy":
                return NoisyVelocityTracker.obtain();
            case "platform":
                return PlatformVelocityTracker.obtain();
            default:
                throw new IllegalStateException("Invalid tracker: " + tracker);
        }
    }
}
