package com.android.systemui.statusbar.phone;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Rect;
import android.util.ArrayMap;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.LightBarTransitionsController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;

/* loaded from: classes.dex */
public class DarkIconDispatcherImpl implements DarkIconDispatcher {
    private float mDarkIntensity;
    private int mDarkModeIconColorSingleTone;
    private int mLightModeIconColorSingleTone;
    private final LightBarTransitionsController mTransitionsController;
    private final Rect mTintArea = new Rect();
    private final ArrayMap<Object, DarkIconDispatcher.DarkReceiver> mReceivers = new ArrayMap<>();
    private int mIconTint = -1;

    public DarkIconDispatcherImpl(Context context) {
        this.mDarkModeIconColorSingleTone = context.getColor(R.color.dark_mode_icon_color_single_tone);
        this.mLightModeIconColorSingleTone = context.getColor(R.color.light_mode_icon_color_single_tone);
        this.mTransitionsController = new LightBarTransitionsController(context, new LightBarTransitionsController.DarkIntensityApplier() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$DarkIconDispatcherImpl$oA-3cfsaKFqoqAQl8JID377YBJY
            @Override // com.android.systemui.statusbar.phone.LightBarTransitionsController.DarkIntensityApplier
            public final void applyDarkIntensity(float f) {
                this.f$0.setIconTintInternal(f);
            }
        });
    }

    @Override // com.android.systemui.statusbar.policy.DarkIconDispatcher
    public LightBarTransitionsController getTransitionsController() {
        return this.mTransitionsController;
    }

    @Override // com.android.systemui.statusbar.policy.DarkIconDispatcher
    public void addDarkReceiver(DarkIconDispatcher.DarkReceiver darkReceiver) {
        this.mReceivers.put(darkReceiver, darkReceiver);
        darkReceiver.onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
    }

    @Override // com.android.systemui.statusbar.policy.DarkIconDispatcher
    public void removeDarkReceiver(DarkIconDispatcher.DarkReceiver darkReceiver) {
        this.mReceivers.remove(darkReceiver);
    }

    @Override // com.android.systemui.statusbar.policy.DarkIconDispatcher
    public void applyDark(DarkIconDispatcher.DarkReceiver darkReceiver) {
        this.mReceivers.get(darkReceiver).onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
    }

    @Override // com.android.systemui.statusbar.policy.DarkIconDispatcher
    public void setIconsDarkArea(Rect rect) {
        if (rect == null && this.mTintArea.isEmpty()) {
            return;
        }
        if (rect == null) {
            this.mTintArea.setEmpty();
        } else {
            this.mTintArea.set(rect);
        }
        applyIconTint();
    }

    private void setIconTintInternal(float f) {
        this.mDarkIntensity = f;
        this.mIconTint = ((Integer) ArgbEvaluator.getInstance().evaluate(f, Integer.valueOf(this.mLightModeIconColorSingleTone), Integer.valueOf(this.mDarkModeIconColorSingleTone))).intValue();
        applyIconTint();
    }

    private void applyIconTint() {
        for (int i = 0; i < this.mReceivers.size(); i++) {
            this.mReceivers.valueAt(i).onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
        }
    }
}
