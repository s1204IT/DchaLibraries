package com.android.systemui.charging;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import java.text.NumberFormat;

/* loaded from: classes.dex */
public class WirelessChargingLayout extends FrameLayout {
    public WirelessChargingLayout(Context context, int i, boolean z) {
        super(context);
        init(context, null, i, z);
    }

    public WirelessChargingLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet, false);
    }

    private void init(Context context, AttributeSet attributeSet, boolean z) {
        init(context, attributeSet, -1, false);
    }

    private void init(Context context, AttributeSet attributeSet, int i, boolean z) {
        inflate(context, R.layout.wireless_charging_layout, this);
        WirelessChargingView wirelessChargingView = (WirelessChargingView) findViewById(R.id.wireless_charging_view);
        TextView textView = (TextView) findViewById(R.id.wireless_charging_percentage);
        if (z) {
            wirelessChargingView.setPaintColor(-1);
            textView.setTextColor(-1);
        }
        if (i != -1) {
            textView.setText(NumberFormat.getPercentInstance().format(i / 100.0f));
            textView.setAlpha(0.0f);
        }
        long integer = context.getResources().getInteger(R.integer.wireless_charging_fade_offset);
        long integer2 = context.getResources().getInteger(R.integer.wireless_charging_fade_duration);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(textView, "textSize", context.getResources().getFloat(R.dimen.wireless_charging_anim_battery_level_text_size_start), context.getResources().getFloat(R.dimen.wireless_charging_anim_battery_level_text_size_end));
        objectAnimatorOfFloat.setInterpolator(new PathInterpolator(0.0f, 0.0f, 0.0f, 1.0f));
        objectAnimatorOfFloat.setDuration(context.getResources().getInteger(R.integer.wireless_charging_battery_level_text_scale_animation_duration));
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(textView, "alpha", 0.0f, 1.0f);
        objectAnimatorOfFloat2.setInterpolator(Interpolators.LINEAR);
        objectAnimatorOfFloat2.setDuration(context.getResources().getInteger(R.integer.wireless_charging_battery_level_text_opacity_duration));
        objectAnimatorOfFloat2.setStartDelay(context.getResources().getInteger(R.integer.wireless_charging_anim_opacity_offset));
        ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(textView, "alpha", 1.0f, 0.0f);
        objectAnimatorOfFloat3.setDuration(integer2);
        objectAnimatorOfFloat3.setInterpolator(Interpolators.LINEAR);
        objectAnimatorOfFloat3.setStartDelay(integer);
        ObjectAnimator objectAnimatorOfFloat4 = ObjectAnimator.ofFloat(wirelessChargingView, "alpha", 1.0f, 0.0f);
        objectAnimatorOfFloat4.setDuration(integer2);
        objectAnimatorOfFloat4.setInterpolator(Interpolators.LINEAR);
        objectAnimatorOfFloat4.setStartDelay(integer);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2, objectAnimatorOfFloat3, objectAnimatorOfFloat4);
        animatorSet.start();
    }
}
