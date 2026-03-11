package com.android.settingslib.display;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MathUtils;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.settingslib.R$string;
import java.util.Arrays;

public class DisplayDensityUtils {
    private final int mCurrentIndex;
    private final int mDefaultDensity;
    private final String[] mEntries;
    private final int[] mValues;
    public static final int SUMMARY_DEFAULT = R$string.screen_zoom_summary_default;
    private static final int SUMMARY_CUSTOM = R$string.screen_zoom_summary_custom;
    private static final int[] SUMMARIES_SMALLER = {R$string.screen_zoom_summary_small};
    private static final int[] SUMMARIES_LARGER = {R$string.screen_zoom_summary_large, R$string.screen_zoom_summary_very_large, R$string.screen_zoom_summary_extremely_large};

    public DisplayDensityUtils(Context context) {
        int displayIndex;
        int defaultDensity = getDefaultDisplayDensity(0);
        if (defaultDensity <= 0) {
            this.mEntries = null;
            this.mValues = null;
            this.mDefaultDensity = 0;
            this.mCurrentIndex = -1;
            return;
        }
        Resources res = context.getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();
        int currentDensity = metrics.densityDpi;
        int currentDensityIndex = -1;
        int minDimensionPx = Math.min(metrics.widthPixels, metrics.heightPixels);
        int maxDensity = (minDimensionPx * 160) / 320;
        float maxScale = Math.min(1.5f, maxDensity / defaultDensity);
        int numLarger = (int) MathUtils.constrain((maxScale - 1.0f) / 0.09f, 0.0f, SUMMARIES_LARGER.length);
        int numSmaller = (int) MathUtils.constrain(1.6666664f, 0.0f, SUMMARIES_SMALLER.length);
        String[] entries = new String[numSmaller + 1 + numLarger];
        int[] values = new int[entries.length];
        int curIndex = 0;
        if (numSmaller > 0) {
            float interval = 0.14999998f / numSmaller;
            for (int i = numSmaller - 1; i >= 0; i--) {
                int density = ((int) (defaultDensity * (1.0f - ((i + 1) * interval)))) & (-2);
                if (currentDensity == density) {
                    currentDensityIndex = curIndex;
                }
                entries[curIndex] = res.getString(SUMMARIES_SMALLER[i]);
                values[curIndex] = density;
                curIndex++;
            }
        }
        currentDensityIndex = currentDensity == defaultDensity ? curIndex : currentDensityIndex;
        values[curIndex] = defaultDensity;
        entries[curIndex] = res.getString(SUMMARY_DEFAULT);
        int curIndex2 = curIndex + 1;
        if (numLarger > 0) {
            float interval2 = (maxScale - 1.0f) / numLarger;
            for (int i2 = 0; i2 < numLarger; i2++) {
                int density2 = ((int) (defaultDensity * (((i2 + 1) * interval2) + 1.0f))) & (-2);
                if (currentDensity == density2) {
                    currentDensityIndex = curIndex2;
                }
                values[curIndex2] = density2;
                entries[curIndex2] = res.getString(SUMMARIES_LARGER[i2]);
                curIndex2++;
            }
        }
        if (currentDensityIndex >= 0) {
            displayIndex = currentDensityIndex;
        } else {
            int newLength = values.length + 1;
            values = Arrays.copyOf(values, newLength);
            values[curIndex2] = currentDensity;
            entries = (String[]) Arrays.copyOf(entries, newLength);
            entries[curIndex2] = res.getString(SUMMARY_CUSTOM, Integer.valueOf(currentDensity));
            displayIndex = curIndex2;
        }
        this.mDefaultDensity = defaultDensity;
        this.mCurrentIndex = displayIndex;
        this.mEntries = entries;
        this.mValues = values;
    }

    public String[] getEntries() {
        return this.mEntries;
    }

    public int[] getValues() {
        return this.mValues;
    }

    public int getCurrentIndex() {
        return this.mCurrentIndex;
    }

    public int getDefaultDensity() {
        return this.mDefaultDensity;
    }

    private static int getDefaultDisplayDensity(int displayId) {
        try {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            return wm.getInitialDisplayDensity(displayId);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static void clearForcedDisplayDensity(final int displayId) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                    wm.clearForcedDisplayDensity(displayId);
                } catch (RemoteException e) {
                    Log.w("DisplayDensityUtils", "Unable to clear forced display density setting");
                }
            }
        });
    }

    public static void setForcedDisplayDensity(final int displayId, final int density) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                    wm.setForcedDisplayDensity(displayId, density);
                } catch (RemoteException e) {
                    Log.w("DisplayDensityUtils", "Unable to save forced display density setting");
                }
            }
        });
    }
}
