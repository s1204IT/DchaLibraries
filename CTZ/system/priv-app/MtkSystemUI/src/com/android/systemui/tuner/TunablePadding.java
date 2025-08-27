package com.android.systemui.tuner;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import com.android.systemui.Dependency;
import com.android.systemui.tuner.TunerService;

/* loaded from: classes.dex */
public class TunablePadding implements TunerService.Tunable {
    private final int mDefaultSize;
    private final float mDensity;
    private final int mFlags;
    private final View mView;

    private TunablePadding(String str, int i, int i2, View view) {
        this.mDefaultSize = i;
        this.mFlags = i2;
        this.mView = view;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) view.getContext().getSystemService(WindowManager.class)).getDefaultDisplay().getMetrics(displayMetrics);
        this.mDensity = displayMetrics.density;
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, str);
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        int i = this.mDefaultSize;
        if (str2 != null) {
            i = (int) (Integer.parseInt(str2) * this.mDensity);
        }
        this.mView.setPadding(getPadding(i, this.mView.isLayoutRtl() ? 2 : 1), getPadding(i, 4), getPadding(i, this.mView.isLayoutRtl() ? 1 : 2), getPadding(i, 8));
    }

    private int getPadding(int i, int i2) {
        if ((i2 & this.mFlags) != 0) {
            return i;
        }
        return 0;
    }

    public void destroy() {
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }

    public static class TunablePaddingService {
        public TunablePadding add(View view, String str, int i, int i2) {
            if (view == null) {
                throw new IllegalArgumentException();
            }
            return new TunablePadding(str, i, i2, view);
        }
    }

    public static TunablePadding addTunablePadding(View view, String str, int i, int i2) {
        return ((TunablePaddingService) Dependency.get(TunablePaddingService.class)).add(view, str, i, i2);
    }
}
