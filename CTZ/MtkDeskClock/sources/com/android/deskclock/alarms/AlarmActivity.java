package com.android.deskclock.alarms;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.util.Property;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.BaseActivity;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.CircleView;

public class AlarmActivity extends BaseActivity implements View.OnClickListener, View.OnTouchListener {
    private static final int ALARM_BOUNCE_DURATION_MILLIS = 500;
    private static final int ALERT_DISMISS_DELAY_MILLIS = 2000;
    private static final int ALERT_FADE_DURATION_MILLIS = 500;
    private static final int ALERT_REVEAL_DURATION_MILLIS = 500;
    private static final int BUTTON_DRAWABLE_ALPHA_DEFAULT = 165;
    private static final float BUTTON_SCALE_DEFAULT = 0.7f;
    private static final int PULSE_DURATION_MILLIS = 1000;
    private AccessibilityManager mAccessibilityManager;
    private ValueAnimator mAlarmAnimator;
    private ImageView mAlarmButton;
    private boolean mAlarmHandled;
    private AlarmInstance mAlarmInstance;
    private TextView mAlertInfoView;
    private TextView mAlertTitleView;
    private ViewGroup mAlertView;
    private ViewGroup mContentView;
    private int mCurrentHourColor;
    private ValueAnimator mDismissAnimator;
    private ImageView mDismissButton;
    private TextView mHintView;
    private ValueAnimator mPulseAnimator;
    private boolean mReceiverRegistered;
    private boolean mServiceBound;
    private ValueAnimator mSnoozeAnimator;
    private ImageView mSnoozeButton;
    private DataModel.AlarmVolumeButtonBehavior mVolumeBehavior;
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AlarmActivity");
    private static final TimeInterpolator PULSE_INTERPOLATOR = PathInterpolatorCompat.create(0.4f, 0.0f, 0.2f, 1.0f);
    private static final TimeInterpolator REVEAL_INTERPOLATOR = PathInterpolatorCompat.create(0.0f, 0.0f, 0.2f, 1.0f);
    private final Handler mHandler = new Handler();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws Exception {
            String action = intent.getAction();
            AlarmActivity.LOGGER.v("Received broadcast: %s", action);
            if (AlarmActivity.this.mAlarmHandled) {
                AlarmActivity.LOGGER.v("Ignored broadcast: %s", action);
            }
            byte b = -1;
            int iHashCode = action.hashCode();
            if (iHashCode != -620878759) {
                if (iHashCode != 1013431989) {
                    if (iHashCode == 1660414551 && action.equals(AlarmService.ALARM_DISMISS_ACTION)) {
                        b = 1;
                    }
                } else if (action.equals(AlarmService.ALARM_DONE_ACTION)) {
                    b = 2;
                }
            } else if (action.equals(AlarmService.ALARM_SNOOZE_ACTION)) {
                b = 0;
            }
            switch (b) {
                case 0:
                    AlarmActivity.this.snooze();
                    break;
                case 1:
                    AlarmActivity.this.dismiss();
                    break;
                case 2:
                    AlarmActivity.this.finish();
                    break;
                default:
                    AlarmActivity.LOGGER.i("Unknown broadcast: %s", action);
                    break;
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AlarmActivity.LOGGER.i("Finished binding to AlarmService", new Object[0]);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            AlarmActivity.LOGGER.i("Disconnected from AlarmService", new Object[0]);
        }
    };
    private int mInitialPointerIndex = -1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setVolumeControlStream(4);
        this.mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), AlarmInstance.getId(getIntent().getData()));
        if (this.mAlarmInstance == null) {
            LOGGER.e("Error displaying alarm for intent: %s", getIntent());
            finish();
            return;
        }
        if (this.mAlarmInstance.mAlarmState != 5) {
            LOGGER.i("Skip displaying alarm for instance: %s", this.mAlarmInstance);
            finish();
            return;
        }
        LOGGER.i("Displaying alarm for instance: %s", this.mAlarmInstance);
        this.mVolumeBehavior = DataModel.getDataModel().getAlarmVolumeButtonBehavior();
        getWindow().addFlags(6815873);
        hideNavigationBar();
        sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        if (!getResources().getBoolean(R.bool.rotateAlarmAlert)) {
            setRequestedOrientation(5);
        }
        this.mAccessibilityManager = (AccessibilityManager) getSystemService("accessibility");
        setContentView(R.layout.alarm_activity);
        this.mAlertView = (ViewGroup) findViewById(R.id.alert);
        this.mAlertTitleView = (TextView) this.mAlertView.findViewById(R.id.alert_title);
        this.mAlertInfoView = (TextView) this.mAlertView.findViewById(R.id.alert_info);
        this.mContentView = (ViewGroup) findViewById(R.id.content);
        this.mAlarmButton = (ImageView) this.mContentView.findViewById(R.id.alarm);
        this.mSnoozeButton = (ImageView) this.mContentView.findViewById(R.id.snooze);
        this.mDismissButton = (ImageView) this.mContentView.findViewById(R.id.dismiss);
        this.mHintView = (TextView) this.mContentView.findViewById(R.id.hint);
        TextView textView = (TextView) this.mContentView.findViewById(R.id.title);
        TextClock textClock = (TextClock) this.mContentView.findViewById(R.id.digital_clock);
        CircleView circleView = (CircleView) this.mContentView.findViewById(R.id.pulse);
        textView.setText(this.mAlarmInstance.getLabelOrDefault(this));
        Utils.setTimeFormat(textClock, false);
        this.mCurrentHourColor = ThemeUtils.resolveColor(this, android.R.attr.windowBackground);
        getWindow().setBackgroundDrawable(new ColorDrawable(this.mCurrentHourColor));
        this.mAlarmButton.setOnTouchListener(this);
        this.mSnoozeButton.setOnClickListener(this);
        this.mDismissButton.setOnClickListener(this);
        this.mAlarmAnimator = AnimatorUtils.getScaleAnimator(this.mAlarmButton, 1.0f, 0.0f);
        this.mSnoozeAnimator = getButtonAnimator(this.mSnoozeButton, -1);
        this.mDismissAnimator = getButtonAnimator(this.mDismissButton, this.mCurrentHourColor);
        this.mPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(circleView, PropertyValuesHolder.ofFloat(CircleView.RADIUS, 0.0f, circleView.getRadius()), PropertyValuesHolder.ofObject(CircleView.FILL_COLOR, AnimatorUtils.ARGB_EVALUATOR, Integer.valueOf(ColorUtils.setAlphaComponent(circleView.getFillColor(), 0))));
        this.mPulseAnimator.setDuration(1000L);
        this.mPulseAnimator.setInterpolator(PULSE_INTERPOLATOR);
        this.mPulseAnimator.setRepeatCount(-1);
        this.mPulseAnimator.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        long id = AlarmInstance.getId(getIntent().getData());
        this.mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), id);
        if (this.mAlarmInstance == null) {
            LOGGER.i("No alarm instance for instanceId: %d", Long.valueOf(id));
            finish();
            return;
        }
        if (this.mAlarmInstance.mAlarmState != 5) {
            LOGGER.i("Skip displaying alarm for instance: %s", this.mAlarmInstance);
            finish();
            return;
        }
        if (!this.mReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
            intentFilter.addAction(AlarmService.ALARM_SNOOZE_ACTION);
            intentFilter.addAction(AlarmService.ALARM_DISMISS_ACTION);
            registerReceiver(this.mReceiver, intentFilter);
            this.mReceiverRegistered = true;
        }
        bindAlarmService();
        resetAnimations();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindAlarmService();
        if (this.mReceiverRegistered) {
            unregisterReceiver(this.mReceiver);
            this.mReceiverRegistered = false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent keyEvent) throws Exception {
        LOGGER.v("dispatchKeyEvent: %s", keyEvent);
        switch (keyEvent.getKeyCode()) {
            case 24:
            case 25:
            case 27:
            case 79:
            case 80:
            case 164:
                if (!this.mAlarmHandled) {
                    switch (this.mVolumeBehavior) {
                        case SNOOZE:
                            if (keyEvent.getAction() == 1) {
                                snooze();
                            }
                            break;
                        case DISMISS:
                            if (keyEvent.getAction() == 1) {
                                dismiss();
                            }
                            break;
                    }
                    return true;
                }
                break;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onClick(View view) throws Exception {
        if (this.mAlarmHandled) {
            LOGGER.v("onClick ignored: %s", view);
            return;
        }
        LOGGER.v("onClick: %s", view);
        if (isAccessibilityEnabled()) {
            if (view == this.mSnoozeButton) {
                snooze();
                return;
            } else {
                if (view == this.mDismissButton) {
                    dismiss();
                    return;
                }
                return;
            }
        }
        if (view == this.mSnoozeButton) {
            hintSnooze();
        } else if (view == this.mDismissButton) {
            hintDismiss();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) throws Exception {
        float fraction;
        float fraction2;
        if (this.mAlarmHandled) {
            LOGGER.v("onTouch ignored: %s", motionEvent);
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            LOGGER.v("onTouch started: %s", motionEvent);
            this.mInitialPointerIndex = motionEvent.getPointerId(motionEvent.getActionIndex());
            this.mPulseAnimator.setRepeatCount(0);
        } else if (actionMasked == 3) {
            LOGGER.v("onTouch canceled: %s", motionEvent);
            this.mInitialPointerIndex = -1;
            resetAnimations();
        }
        int actionIndex = motionEvent.getActionIndex();
        if (this.mInitialPointerIndex == -1 || this.mInitialPointerIndex != motionEvent.getPointerId(actionIndex)) {
            return true;
        }
        this.mContentView.getLocationOnScreen(new int[]{0, 0});
        float rawX = motionEvent.getRawX() - r5[0];
        float rawY = motionEvent.getRawY() - r5[1];
        int left = this.mAlarmButton.getLeft() + this.mAlarmButton.getPaddingLeft();
        int right = this.mAlarmButton.getRight() - this.mAlarmButton.getPaddingRight();
        if (this.mContentView.getLayoutDirection() == 1) {
            float fraction3 = getFraction(right, this.mSnoozeButton.getLeft(), rawX);
            fraction2 = getFraction(left, this.mDismissButton.getRight(), rawX);
            fraction = fraction3;
        } else {
            fraction = getFraction(left, this.mSnoozeButton.getRight(), rawX);
            fraction2 = getFraction(right, this.mDismissButton.getLeft(), rawX);
        }
        setAnimatedFractions(fraction, fraction2);
        if (actionMasked == 1 || actionMasked == 6) {
            LOGGER.v("onTouch ended: %s", motionEvent);
            this.mInitialPointerIndex = -1;
            if (fraction == 1.0f) {
                snooze();
            } else if (fraction2 == 1.0f) {
                dismiss();
            } else {
                if (fraction > 0.0f || fraction2 > 0.0f) {
                    AnimatorUtils.reverse(this.mAlarmAnimator, this.mSnoozeAnimator, this.mDismissAnimator);
                } else if (this.mAlarmButton.getTop() <= rawY && rawY <= this.mAlarmButton.getBottom()) {
                    hintDismiss();
                }
                this.mPulseAnimator.setRepeatCount(-1);
                if (!this.mPulseAnimator.isStarted()) {
                    this.mPulseAnimator.start();
                }
            }
        }
        return true;
    }

    private void hideNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(4610);
    }

    private boolean isAccessibilityEnabled() {
        if (this.mAccessibilityManager == null || !this.mAccessibilityManager.isEnabled()) {
            return false;
        }
        if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
            return true;
        }
        return !this.mAccessibilityManager.getEnabledAccessibilityServiceList(16).isEmpty();
    }

    private void hintSnooze() {
        float fMax = Math.max(this.mSnoozeButton.getLeft() - (this.mAlarmButton.getRight() - this.mAlarmButton.getPaddingRight()), 0) + Math.min(this.mSnoozeButton.getRight() - (this.mAlarmButton.getLeft() + this.mAlarmButton.getPaddingLeft()), 0);
        getAlarmBounceAnimator(fMax, fMax < 0.0f ? R.string.description_direction_left : R.string.description_direction_right).start();
    }

    private void hintDismiss() {
        float fMax = Math.max(this.mDismissButton.getLeft() - (this.mAlarmButton.getRight() - this.mAlarmButton.getPaddingRight()), 0) + Math.min(this.mDismissButton.getRight() - (this.mAlarmButton.getLeft() + this.mAlarmButton.getPaddingLeft()), 0);
        getAlarmBounceAnimator(fMax, fMax < 0.0f ? R.string.description_direction_left : R.string.description_direction_right).start();
    }

    private void resetAnimations() {
        setAnimatedFractions(0.0f, 0.0f);
        this.mPulseAnimator.setRepeatCount(-1);
        if (!this.mPulseAnimator.isStarted()) {
            this.mPulseAnimator.start();
        }
    }

    private void snooze() {
        this.mAlarmHandled = true;
        LOGGER.v("Snoozed: %s", this.mAlarmInstance);
        int iResolveColor = ThemeUtils.resolveColor(this, R.attr.colorAccent);
        setAnimatedFractions(1.0f, 0.0f);
        int snoozeLength = DataModel.getDataModel().getSnoozeLength();
        getAlertAnimator(this.mSnoozeButton, R.string.alarm_alert_snoozed_text, getResources().getQuantityString(R.plurals.alarm_alert_snooze_duration, snoozeLength, Integer.valueOf(snoozeLength)), getResources().getQuantityString(R.plurals.alarm_alert_snooze_set, snoozeLength, Integer.valueOf(snoozeLength)), iResolveColor, iResolveColor).start();
        AlarmStateManager.setSnoozeState(this, this.mAlarmInstance, false);
        Events.sendAlarmEvent(R.string.action_snooze, R.string.label_deskclock);
        unbindAlarmService();
    }

    private void dismiss() throws Exception {
        this.mAlarmHandled = true;
        LOGGER.v("Dismissed: %s", this.mAlarmInstance);
        setAnimatedFractions(0.0f, 1.0f);
        getAlertAnimator(this.mDismissButton, R.string.alarm_alert_off_text, null, getString(R.string.alarm_alert_off_text), -1, this.mCurrentHourColor).start();
        AlarmStateManager.deleteInstanceAndUpdateParent(this, this.mAlarmInstance);
        Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_deskclock);
        unbindAlarmService();
    }

    private void bindAlarmService() {
        if (!this.mServiceBound) {
            bindService(new Intent(this, (Class<?>) AlarmService.class), this.mConnection, 1);
            this.mServiceBound = true;
        }
    }

    private void unbindAlarmService() {
        if (this.mServiceBound) {
            unbindService(this.mConnection);
            this.mServiceBound = false;
        }
    }

    private void setAnimatedFractions(float f, float f2) {
        AnimatorUtils.setAnimatedFraction(this.mAlarmAnimator, Math.max(f, f2));
        AnimatorUtils.setAnimatedFraction(this.mSnoozeAnimator, f);
        AnimatorUtils.setAnimatedFraction(this.mDismissAnimator, f2);
    }

    private float getFraction(float f, float f2, float f3) {
        return Math.max(Math.min((f3 - f) / (f2 - f), 1.0f), 0.0f);
    }

    private ValueAnimator getButtonAnimator(ImageView imageView, int i) {
        return ObjectAnimator.ofPropertyValuesHolder(imageView, PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, BUTTON_SCALE_DEFAULT, 1.0f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, BUTTON_SCALE_DEFAULT, 1.0f), PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 0, 255), PropertyValuesHolder.ofInt(AnimatorUtils.DRAWABLE_ALPHA, BUTTON_DRAWABLE_ALPHA_DEFAULT, 255), PropertyValuesHolder.ofObject(AnimatorUtils.DRAWABLE_TINT, AnimatorUtils.ARGB_EVALUATOR, -1, Integer.valueOf(i)));
    }

    private ValueAnimator getAlarmBounceAnimator(float f, final int i) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mAlarmButton, (Property<ImageView, Float>) View.TRANSLATION_X, this.mAlarmButton.getTranslationX(), f, 0.0f);
        objectAnimatorOfFloat.setInterpolator(AnimatorUtils.DECELERATE_ACCELERATE_INTERPOLATOR);
        objectAnimatorOfFloat.setDuration(500L);
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                AlarmActivity.this.mHintView.setText(i);
                if (AlarmActivity.this.mHintView.getVisibility() != 0) {
                    AlarmActivity.this.mHintView.setVisibility(0);
                    ObjectAnimator.ofFloat(AlarmActivity.this.mHintView, (Property<TextView, Float>) View.ALPHA, 0.0f, 1.0f).start();
                }
            }
        });
        return objectAnimatorOfFloat;
    }

    private Animator getAlertAnimator(View view, final int i, final String str, final String str2, int i2, final int i3) {
        final ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content);
        Rect rect = new Rect(0, 0, view.getHeight(), view.getWidth());
        viewGroup.offsetDescendantRectToMyCoords(view, rect);
        int iCenterX = rect.centerX();
        int iCenterY = rect.centerY();
        int iMax = Math.max(iCenterX, viewGroup.getWidth() - iCenterX);
        int iMax2 = Math.max(iCenterY, viewGroup.getHeight() - iCenterY);
        float fSqrt = (float) Math.sqrt((iMax * iMax) + (iMax2 * iMax2));
        final CircleView fillColor = new CircleView(this).setCenterX(iCenterX).setCenterY(iCenterY).setFillColor(i2);
        viewGroup.addView(fillColor);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(fillColor, CircleView.RADIUS, Math.max(rect.width(), rect.height()) / 2.0f, fSqrt);
        objectAnimatorOfFloat.setDuration(500L);
        objectAnimatorOfFloat.setInterpolator(REVEAL_INTERPOLATOR);
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                AlarmActivity.this.mAlertView.setVisibility(0);
                AlarmActivity.this.mAlertTitleView.setText(i);
                if (str != null) {
                    AlarmActivity.this.mAlertInfoView.setText(str);
                    AlarmActivity.this.mAlertInfoView.setVisibility(0);
                }
                AlarmActivity.this.mContentView.setVisibility(8);
                AlarmActivity.this.getWindow().setBackgroundDrawable(new ColorDrawable(i3));
            }
        });
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(fillColor, (Property<CircleView, Float>) View.ALPHA, 0.0f);
        objectAnimatorOfFloat2.setDuration(500L);
        objectAnimatorOfFloat2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                viewGroup.removeView(fillColor);
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(objectAnimatorOfFloat).before(objectAnimatorOfFloat2);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                AlarmActivity.this.mAlertView.announceForAccessibility(str2);
                AlarmActivity.this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AlarmActivity.this.finish();
                    }
                }, 2000L);
            }
        });
        return animatorSet;
    }
}
