package com.android.deskclock.alarms;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Property;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.AlarmInstance;

public class AlarmActivity extends Activity implements View.OnClickListener, View.OnTouchListener {
    private ValueAnimator mAlarmAnimator;
    private ImageView mAlarmButton;
    private boolean mAlarmHandled;
    private AlarmInstance mAlarmInstance;
    private TextView mAlertInfoView;
    private TextView mAlertTitleView;
    private ViewGroup mAlertView;
    private ViewGroup mContainerView;
    private ViewGroup mContentView;
    private int mCurrentHourColor;
    private ValueAnimator mDismissAnimator;
    private ImageView mDismissButton;
    private TextView mHintView;
    private ValueAnimator mPulseAnimator;
    private boolean mReceiverRegistered;
    private ValueAnimator mSnoozeAnimator;
    private ImageView mSnoozeButton;
    private String mVolumeBehavior;
    public static long sPowerOnAlarmId = -1;
    private static final String LOGTAG = AlarmActivity.class.getSimpleName();
    private static final Interpolator PULSE_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    private static final Interpolator REVEAL_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
    private final Handler mHandler = new Handler();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.v(AlarmActivity.LOGTAG, "Received broadcast: %s", action);
            if (AlarmActivity.this.mAlarmHandled) {
                LogUtils.v(AlarmActivity.LOGTAG, "Ignored broadcast: %s", action);
                return;
            }
            switch (action) {
                case "com.android.deskclock.ALARM_SNOOZE":
                    AlarmActivity.this.snooze();
                    break;
                case "com.android.deskclock.ALARM_DISMISS":
                    AlarmActivity.this.dismiss();
                    break;
                case "com.android.deskclock.ALARM_DONE":
                    AlarmActivity.this.finish();
                    break;
                default:
                    LogUtils.i(AlarmActivity.LOGTAG, "Unknown broadcast: %s", action);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long instanceId = AlarmInstance.getId(getIntent().getData());
        this.mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);
        if (this.mAlarmInstance == null) {
            LogUtils.e(LOGTAG, "Error displaying alarm for intent: %s", getIntent());
            finish();
            return;
        }
        if (this.mAlarmInstance.mAlarmState != 5) {
            LogUtils.i(LOGTAG, "Skip displaying alarm for instance: %s", this.mAlarmInstance);
            finish();
            return;
        }
        LogUtils.i(LOGTAG, "Displaying alarm for instance: %s", this.mAlarmInstance);
        this.mVolumeBehavior = PreferenceManager.getDefaultSharedPreferences(this).getString("volume_button_setting", "0");
        getWindow().addFlags(6815873);
        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(5);
        }
        setContentView(R.layout.alarm_activity);
        this.mContainerView = (ViewGroup) findViewById(android.R.id.content);
        this.mAlertView = (ViewGroup) this.mContainerView.findViewById(R.id.alert);
        this.mAlertTitleView = (TextView) this.mAlertView.findViewById(R.id.alert_title);
        this.mAlertInfoView = (TextView) this.mAlertView.findViewById(R.id.alert_info);
        this.mContentView = (ViewGroup) this.mContainerView.findViewById(R.id.content);
        this.mAlarmButton = (ImageView) this.mContentView.findViewById(R.id.alarm);
        this.mSnoozeButton = (ImageView) this.mContentView.findViewById(R.id.snooze);
        this.mDismissButton = (ImageView) this.mContentView.findViewById(R.id.dismiss);
        this.mHintView = (TextView) this.mContentView.findViewById(R.id.hint);
        TextView titleView = (TextView) this.mContentView.findViewById(R.id.title);
        TextClock digitalClock = (TextClock) this.mContentView.findViewById(R.id.digital_clock);
        View pulseView = this.mContentView.findViewById(R.id.pulse);
        titleView.setText(this.mAlarmInstance.getLabelOrDefault(this));
        Utils.setTimeFormat(digitalClock, getResources().getDimensionPixelSize(R.dimen.main_ampm_font_size));
        this.mCurrentHourColor = Utils.getCurrentHourColor();
        this.mContainerView.setBackgroundColor(this.mCurrentHourColor);
        this.mAlarmButton.setOnTouchListener(this);
        this.mSnoozeButton.setOnClickListener(this);
        this.mDismissButton.setOnClickListener(this);
        this.mAlarmAnimator = AnimatorUtils.getScaleAnimator(this.mAlarmButton, 1.0f, 0.0f);
        this.mSnoozeAnimator = getButtonAnimator(this.mSnoozeButton, -1);
        this.mDismissAnimator = getButtonAnimator(this.mDismissButton, this.mCurrentHourColor);
        this.mPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(pulseView, PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, 0.0f, 1.0f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, 0.0f, 1.0f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.ALPHA, 1.0f, 0.0f));
        this.mPulseAnimator.setDuration(1000L);
        this.mPulseAnimator.setInterpolator(PULSE_INTERPOLATOR);
        this.mPulseAnimator.setRepeatCount(-1);
        this.mPulseAnimator.start();
        setAnimatedFractions(0.0f, 0.0f);
        IntentFilter filter = new IntentFilter("com.android.deskclock.ALARM_DONE");
        filter.addAction("com.android.deskclock.ALARM_SNOOZE");
        filter.addAction("com.android.deskclock.ALARM_DISMISS");
        registerReceiver(this.mReceiver, filter);
        this.mReceiverRegistered = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mReceiverRegistered) {
            unregisterReceiver(this.mReceiver);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        LogUtils.v(LOGTAG, "dispatchKeyEvent: %s", keyEvent);
        switch (keyEvent.getKeyCode()) {
            case 24:
            case 25:
            case 26:
            case 27:
            case 80:
            case 164:
                if (this.mAlarmHandled || keyEvent.getAction() != 1) {
                    return true;
                }
                switch (this.mVolumeBehavior) {
                    case "1":
                        snooze();
                        return true;
                    case "2":
                        dismiss();
                        return true;
                    default:
                        return true;
                }
            default:
                return super.dispatchKeyEvent(keyEvent);
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onClick(View view) {
        if (this.mAlarmHandled) {
            LogUtils.v(LOGTAG, "onClick ignored: %s", view);
            return;
        }
        LogUtils.v(LOGTAG, "onClick: %s", view);
        int alarmLeft = this.mAlarmButton.getLeft() + this.mAlarmButton.getPaddingLeft();
        int alarmRight = this.mAlarmButton.getRight() - this.mAlarmButton.getPaddingRight();
        float translationX = Math.max(view.getLeft() - alarmRight, 0) + Math.min(view.getRight() - alarmLeft, 0);
        getAlarmBounceAnimator(translationX, translationX < 0.0f ? R.string.description_direction_left : R.string.description_direction_right).start();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        float snoozeFraction;
        float dismissFraction;
        if (this.mAlarmHandled) {
            LogUtils.v(LOGTAG, "onTouch ignored: %s", motionEvent);
            return false;
        }
        int[] contentLocation = {0, 0};
        this.mContentView.getLocationOnScreen(contentLocation);
        float x = motionEvent.getRawX() - contentLocation[0];
        float y = motionEvent.getRawY() - contentLocation[1];
        int alarmLeft = this.mAlarmButton.getLeft() + this.mAlarmButton.getPaddingLeft();
        int alarmRight = this.mAlarmButton.getRight() - this.mAlarmButton.getPaddingRight();
        if (this.mContentView.getLayoutDirection() == 1) {
            snoozeFraction = getFraction(alarmRight, this.mSnoozeButton.getLeft(), x);
            dismissFraction = getFraction(alarmLeft, this.mDismissButton.getRight(), x);
        } else {
            snoozeFraction = getFraction(alarmLeft, this.mSnoozeButton.getRight(), x);
            dismissFraction = getFraction(alarmRight, this.mDismissButton.getLeft(), x);
        }
        setAnimatedFractions(snoozeFraction, dismissFraction);
        switch (motionEvent.getActionMasked()) {
            case 0:
                LogUtils.v(LOGTAG, "onTouch started: %s", motionEvent);
                this.mPulseAnimator.setRepeatCount(0);
                return true;
            case 1:
                LogUtils.v(LOGTAG, "onTouch ended: %s", motionEvent);
                if (snoozeFraction == 1.0f) {
                    snooze();
                } else if (dismissFraction == 1.0f) {
                    dismiss();
                } else {
                    if (snoozeFraction > 0.0f || dismissFraction > 0.0f) {
                        AnimatorUtils.reverse(this.mAlarmAnimator, this.mSnoozeAnimator, this.mDismissAnimator);
                    } else if (this.mAlarmButton.getTop() <= y && y <= this.mAlarmButton.getBottom()) {
                        this.mDismissButton.performClick();
                    }
                    this.mPulseAnimator.setRepeatCount(-1);
                    if (!this.mPulseAnimator.isStarted()) {
                        this.mPulseAnimator.start();
                    }
                }
                return true;
            default:
                return true;
        }
    }

    private void snooze() {
        this.mAlarmHandled = true;
        LogUtils.v(LOGTAG, "Snoozed: %s", this.mAlarmInstance);
        int alertColor = getResources().getColor(R.color.hot_pink);
        setAnimatedFractions(1.0f, 0.0f);
        int snoozeMinutes = AlarmStateManager.getSnoozedMinutes(this);
        String infoText = getResources().getQuantityString(R.plurals.alarm_alert_snooze_duration, snoozeMinutes, Integer.valueOf(snoozeMinutes));
        String accessibilityText = getResources().getQuantityString(R.plurals.alarm_alert_snooze_set, snoozeMinutes, Integer.valueOf(snoozeMinutes));
        getAlertAnimator(this.mSnoozeButton, R.string.alarm_alert_snoozed_text, infoText, accessibilityText, alertColor, alertColor).start();
        AlarmStateManager.setSnoozeState(this, this.mAlarmInstance, false);
        if (this.mAlarmInstance.mId == sPowerOnAlarmId) {
            sPowerOnAlarmId = -1L;
            shutdown(false);
        }
    }

    private void dismiss() {
        if (this.mAlarmInstance.mId == sPowerOnAlarmId) {
            sPowerOnAlarmId = -1L;
            shutdown(true);
            return;
        }
        this.mAlarmHandled = true;
        LogUtils.v(LOGTAG, "Dismissed: %s", this.mAlarmInstance);
        setAnimatedFractions(0.0f, 1.0f);
        getAlertAnimator(this.mDismissButton, R.string.alarm_alert_off_text, null, getString(R.string.alarm_alert_off_text), -1, this.mCurrentHourColor).start();
        AlarmStateManager.setDismissState(this, this.mAlarmInstance);
    }

    private void startShutdownActivity() {
        Intent requestShutdown = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
        requestShutdown.putExtra("android.intent.extra.KEY_CONFIRM", false);
        startActivity(requestShutdown);
    }

    private void shutdown(Boolean confirm) {
        if (confirm.booleanValue()) {
            AlertDialog.Builder AlertDialog = new AlertDialog.Builder(this);
            AlertDialog.setIcon(android.R.drawable.ic_menu_info_details);
            AlertDialog.setTitle(R.string.shutdown_title);
            AlertDialog.setMessage(R.string.shutdown_text);
            AlertDialog.setPositiveButton(R.string.shutdown_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlarmStateManager.setDismissState(AlarmActivity.this, AlarmActivity.this.mAlarmInstance);
                    AlarmActivity.this.finish();
                }
            });
            AlertDialog.setNegativeButton(R.string.shutdown_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlarmStateManager.setDismissState(AlarmActivity.this, AlarmActivity.this.mAlarmInstance);
                    AlarmActivity.this.startShutdownActivity();
                }
            });
            AlertDialog.show();
            return;
        }
        startShutdownActivity();
    }

    private void setAnimatedFractions(float snoozeFraction, float dismissFraction) {
        float alarmFraction = Math.max(snoozeFraction, dismissFraction);
        this.mAlarmAnimator.setCurrentFraction(alarmFraction);
        this.mSnoozeAnimator.setCurrentFraction(snoozeFraction);
        this.mDismissAnimator.setCurrentFraction(dismissFraction);
    }

    private float getFraction(float x0, float x1, float x) {
        return Math.max(Math.min((x - x0) / (x1 - x0), 1.0f), 0.0f);
    }

    private ValueAnimator getButtonAnimator(ImageView button, int tintColor) {
        return ObjectAnimator.ofPropertyValuesHolder(button, PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, 0.7f, 1.0f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, 0.7f, 1.0f), PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 0, 255), PropertyValuesHolder.ofInt(AnimatorUtils.DRAWABLE_ALPHA, 165, 255), PropertyValuesHolder.ofObject(AnimatorUtils.DRAWABLE_TINT, AnimatorUtils.ARGB_EVALUATOR, -1, Integer.valueOf(tintColor)));
    }

    private ValueAnimator getAlarmBounceAnimator(float translationX, final int hintResId) {
        ValueAnimator bounceAnimator = ObjectAnimator.ofFloat(this.mAlarmButton, (Property<ImageView, Float>) View.TRANSLATION_X, this.mAlarmButton.getTranslationX(), translationX, 0.0f);
        bounceAnimator.setInterpolator(AnimatorUtils.DECELERATE_ACCELERATE_INTERPOLATOR);
        bounceAnimator.setDuration(500L);
        bounceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                AlarmActivity.this.mHintView.setText(hintResId);
                if (AlarmActivity.this.mHintView.getVisibility() != 0) {
                    AlarmActivity.this.mHintView.setVisibility(0);
                    ObjectAnimator.ofFloat(AlarmActivity.this.mHintView, (Property<TextView, Float>) View.ALPHA, 0.0f, 1.0f).start();
                }
            }
        });
        return bounceAnimator;
    }

    private Animator getAlertAnimator(final View source, final int titleResId, final String infoText, final String accessibilityText, int revealColor, final int backgroundColor) {
        final ViewGroupOverlay overlay = this.mContainerView.getOverlay();
        final View revealView = new View(this);
        revealView.setRight(this.mContainerView.getWidth());
        revealView.setBottom(this.mContainerView.getHeight());
        revealView.setBackgroundColor(revealColor);
        overlay.add(revealView);
        overlay.add(source);
        int centerX = Math.round((source.getLeft() + source.getRight()) / 2.0f);
        int centerY = Math.round((source.getTop() + source.getBottom()) / 2.0f);
        float startRadius = Math.max(source.getWidth(), source.getHeight()) / 2.0f;
        int xMax = Math.max(centerX, this.mContainerView.getWidth() - centerX);
        int yMax = Math.max(centerY, this.mContainerView.getHeight() - centerY);
        float endRadius = (float) Math.sqrt(Math.pow(xMax, 2.0d) + Math.pow(yMax, 2.0d));
        ValueAnimator sourceAnimator = ObjectAnimator.ofFloat(source, (Property<View, Float>) View.ALPHA, 0.0f);
        sourceAnimator.setDuration(250L);
        sourceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(source);
            }
        });
        Animator revealAnimator = ViewAnimationUtils.createCircularReveal(revealView, centerX, centerY, startRadius, endRadius);
        revealAnimator.setDuration(500L);
        revealAnimator.setInterpolator(REVEAL_INTERPOLATOR);
        revealAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                AlarmActivity.this.mAlertView.setVisibility(0);
                AlarmActivity.this.mAlertTitleView.setText(titleResId);
                if (infoText != null) {
                    AlarmActivity.this.mAlertInfoView.setText(infoText);
                    AlarmActivity.this.mAlertInfoView.setVisibility(0);
                }
                AlarmActivity.this.mAlertView.announceForAccessibility(accessibilityText);
                AlarmActivity.this.mContentView.setVisibility(8);
                AlarmActivity.this.mContainerView.setBackgroundColor(backgroundColor);
            }
        });
        ValueAnimator fadeAnimator = ObjectAnimator.ofFloat(revealView, (Property<View, Float>) View.ALPHA, 0.0f);
        fadeAnimator.setDuration(500L);
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(revealView);
            }
        });
        AnimatorSet alertAnimator = new AnimatorSet();
        alertAnimator.play(revealAnimator).with(sourceAnimator).before(fadeAnimator);
        alertAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                AlarmActivity.this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AlarmActivity.this.finish();
                    }
                }, 2000L);
            }
        });
        return alertAnimator;
    }
}
