package com.android.settingslib.display;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MathUtils;
import android.view.WindowManagerGlobal;
import com.android.settingslib.R;
import java.util.Arrays;

/* loaded from: classes.dex */
public class DisplayDensityUtils {
    private final int mCurrentIndex;
    private final int mDefaultDensity;
    private final String[] mEntries;
    private final int[] mValues;
    public static final int SUMMARY_DEFAULT = R.string.screen_zoom_summary_default;
    private static final int SUMMARY_CUSTOM = R.string.screen_zoom_summary_custom;
    private static final int[] SUMMARIES_SMALLER = {R.string.screen_zoom_summary_small};
    private static final int[] SUMMARIES_LARGER = {R.string.screen_zoom_summary_large, R.string.screen_zoom_summary_very_large, R.string.screen_zoom_summary_extremely_large};

    public DisplayDensityUtils(Context context) {
        int i;
        int i2;
        int defaultDisplayDensity = getDefaultDisplayDensity(0);
        if (defaultDisplayDensity <= 0) {
            this.mEntries = null;
            this.mValues = null;
            this.mDefaultDensity = 0;
            this.mCurrentIndex = -1;
            return;
        }
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getDisplay().getRealMetrics(displayMetrics);
        int i3 = displayMetrics.densityDpi;
        float f = defaultDisplayDensity;
        float fMin = Math.min(1.5f, ((160 * Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels)) / 320) / f) - 1.0f;
        int iConstrain = (int) MathUtils.constrain(fMin / 0.09f, 0.0f, SUMMARIES_LARGER.length);
        int iConstrain2 = (int) MathUtils.constrain(1.6666664f, 0.0f, SUMMARIES_SMALLER.length);
        String[] strArr = new String[1 + iConstrain2 + iConstrain];
        int[] iArrCopyOf = new int[strArr.length];
        if (iConstrain2 > 0) {
            float f2 = 0.14999998f / iConstrain2;
            i = -1;
            i2 = 0;
            for (int i4 = iConstrain2 - 1; i4 >= 0; i4--) {
                int i5 = ((int) ((1.0f - ((i4 + 1) * f2)) * f)) & (-2);
                if (i3 == i5) {
                    i = i2;
                }
                strArr[i2] = resources.getString(SUMMARIES_SMALLER[i4]);
                iArrCopyOf[i2] = i5;
                i2++;
            }
        } else {
            i = -1;
            i2 = 0;
        }
        i = i3 == defaultDisplayDensity ? i2 : i;
        iArrCopyOf[i2] = defaultDisplayDensity;
        strArr[i2] = resources.getString(SUMMARY_DEFAULT);
        int i6 = i2 + 1;
        if (iConstrain > 0) {
            float f3 = fMin / iConstrain;
            int i7 = 0;
            while (i7 < iConstrain) {
                int i8 = i7 + 1;
                int i9 = ((int) (((i8 * f3) + 1.0f) * f)) & (-2);
                if (i3 == i9) {
                    i = i6;
                }
                iArrCopyOf[i6] = i9;
                strArr[i6] = resources.getString(SUMMARIES_LARGER[i7]);
                i6++;
                i7 = i8;
            }
        }
        if (i < 0) {
            int length = iArrCopyOf.length + 1;
            iArrCopyOf = Arrays.copyOf(iArrCopyOf, length);
            iArrCopyOf[i6] = i3;
            strArr = (String[]) Arrays.copyOf(strArr, length);
            strArr[i6] = resources.getString(SUMMARY_CUSTOM, Integer.valueOf(i3));
            i = i6;
        }
        this.mDefaultDensity = defaultDisplayDensity;
        this.mCurrentIndex = i;
        this.mEntries = strArr;
        this.mValues = iArrCopyOf;
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

    private static int getDefaultDisplayDensity(int i) {
        try {
            return WindowManagerGlobal.getWindowManagerService().getInitialDisplayDensity(i);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static void clearForcedDisplayDensity(final int i) {
        final int iMyUserId = UserHandle.myUserId();
        AsyncTask.execute(new Runnable() { // from class: com.android.settingslib.display.-$$Lambda$DisplayDensityUtils$FjSo_v2dJihYeklLmCubVRPf_nw
            @Override // java.lang.Runnable
            public final void run() {
                DisplayDensityUtils.lambda$clearForcedDisplayDensity$0(i, iMyUserId);
            }
        });
    }

    static /* synthetic */ void lambda$clearForcedDisplayDensity$0(int i, int i2) {
        try {
            WindowManagerGlobal.getWindowManagerService().clearForcedDisplayDensityForUser(i, i2);
        } catch (RemoteException e) {
            Log.w("DisplayDensityUtils", "Unable to clear forced display density setting");
        }
    }

    public static void setForcedDisplayDensity(final int i, final int i2) {
        final int iMyUserId = UserHandle.myUserId();
        AsyncTask.execute(new Runnable() { // from class: com.android.settingslib.display.-$$Lambda$DisplayDensityUtils$jbnNZEy3zYf8rJTNV5wQSa3Z5eQ
            @Override // java.lang.Runnable
            public final void run() {
                DisplayDensityUtils.lambda$setForcedDisplayDensity$1(i, i2, iMyUserId);
            }
        });
    }

    static /* synthetic */ void lambda$setForcedDisplayDensity$1(int i, int i2, int i3) {
        try {
            WindowManagerGlobal.getWindowManagerService().setForcedDisplayDensityForUser(i, i2, i3);
        } catch (RemoteException e) {
            Log.w("DisplayDensityUtils", "Unable to save forced display density setting");
        }
    }
}
