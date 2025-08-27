package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class VelocityTrackerFactory {
    /* JADX WARN: Removed duplicated region for block: B:13:0x002e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static VelocityTrackerInterface obtain(Context context) throws Resources.NotFoundException {
        char c;
        String string = context.getResources().getString(R.string.velocity_tracker_impl);
        int iHashCode = string.hashCode();
        if (iHashCode != 104998702) {
            c = (iHashCode == 1874684019 && string.equals("platform")) ? (char) 1 : (char) 65535;
        } else if (string.equals("noisy")) {
            c = 0;
        }
        switch (c) {
            case 0:
                return NoisyVelocityTracker.obtain();
            case 1:
                return PlatformVelocityTracker.obtain();
            default:
                throw new IllegalStateException("Invalid tracker: " + string);
        }
    }
}
