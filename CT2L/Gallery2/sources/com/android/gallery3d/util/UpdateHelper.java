package com.android.gallery3d.util;

import com.android.gallery3d.common.Utils;

public class UpdateHelper {
    private boolean mUpdated = false;

    public int update(int original, int update) {
        if (original != update) {
            this.mUpdated = true;
            return update;
        }
        return original;
    }

    public long update(long original, long update) {
        if (original != update) {
            this.mUpdated = true;
            return update;
        }
        return original;
    }

    public double update(double original, double update) {
        if (original != update) {
            this.mUpdated = true;
            return update;
        }
        return original;
    }

    public <T> T update(T original, T update) {
        if (!Utils.equals(original, update)) {
            this.mUpdated = true;
            return update;
        }
        return original;
    }

    public boolean isUpdated() {
        return this.mUpdated;
    }
}
