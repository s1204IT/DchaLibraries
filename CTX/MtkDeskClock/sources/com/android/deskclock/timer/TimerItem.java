package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.TimerTextController;
import com.android.deskclock.Utils;
import com.android.deskclock.data.Timer;

public class TimerItem extends LinearLayout {
    private TimerCircleView mCircleView;
    private TextView mLabelView;
    private Timer.State mLastState;
    private Button mResetAddButton;
    private TextView mTimerText;
    private TimerTextController mTimerTextController;

    public TimerItem(Context context) {
        this(context, null);
    }

    public TimerItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLabelView = (TextView) findViewById(R.id.timer_label);
        this.mResetAddButton = (Button) findViewById(R.id.reset_add);
        this.mCircleView = (TimerCircleView) findViewById(R.id.timer_time);
        this.mTimerText = (TextView) findViewById(R.id.timer_time_text);
        this.mTimerTextController = new TimerTextController(this.mTimerText);
        Context context = this.mTimerText.getContext();
        int iResolveColor = ThemeUtils.resolveColor(context, R.attr.colorAccent);
        this.mTimerText.setTextColor(new ColorStateList(new int[][]{new int[]{-16843518, -16842919}, new int[0]}, new int[]{ThemeUtils.resolveColor(context, android.R.attr.textColorPrimary), iResolveColor}));
    }

    void update(Timer timer) {
        this.mTimerTextController.setTimeString(timer.getRemainingTime());
        String label = timer.getLabel();
        if (!TextUtils.equals(label, this.mLabelView.getText())) {
            this.mLabelView.setText(label);
        }
        boolean z = SystemClock.elapsedRealtime() % 1000 < 500;
        if (this.mCircleView != null) {
            boolean z2 = (timer.isExpired() || timer.isMissed()) && z;
            this.mCircleView.setVisibility(z2 ? 4 : 0);
            if (!z2) {
                this.mCircleView.update(timer);
            }
        }
        if (!timer.isPaused() || !z || this.mTimerText.isPressed()) {
            this.mTimerText.setAlpha(1.0f);
        } else {
            this.mTimerText.setAlpha(0.0f);
        }
        if (timer.getState() != this.mLastState) {
            this.mLastState = timer.getState();
            Context context = getContext();
            switch (this.mLastState) {
                case RESET:
                case PAUSED:
                    this.mResetAddButton.setText(R.string.timer_reset);
                    this.mResetAddButton.setContentDescription(null);
                    this.mTimerText.setClickable(true);
                    this.mTimerText.setActivated(false);
                    this.mTimerText.setImportantForAccessibility(1);
                    ViewCompat.setAccessibilityDelegate(this.mTimerText, new Utils.ClickAccessibilityDelegate(context.getString(R.string.timer_start), true));
                    break;
                case RUNNING:
                    String string = context.getString(R.string.timer_plus_one);
                    this.mResetAddButton.setText(R.string.timer_add_minute);
                    this.mResetAddButton.setContentDescription(string);
                    this.mTimerText.setClickable(true);
                    this.mTimerText.setActivated(false);
                    this.mTimerText.setImportantForAccessibility(1);
                    ViewCompat.setAccessibilityDelegate(this.mTimerText, new Utils.ClickAccessibilityDelegate(context.getString(R.string.timer_pause)));
                    break;
                case EXPIRED:
                case MISSED:
                    String string2 = context.getString(R.string.timer_plus_one);
                    this.mResetAddButton.setText(R.string.timer_add_minute);
                    this.mResetAddButton.setContentDescription(string2);
                    this.mTimerText.setClickable(false);
                    this.mTimerText.setActivated(true);
                    this.mTimerText.setImportantForAccessibility(2);
                    break;
            }
        }
    }
}
