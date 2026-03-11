package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.fingerprint.FingerprintEnrollSidecar;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class FingerprintEnrollEnrolling extends FingerprintEnrollBase implements FingerprintEnrollSidecar.Listener {
    private boolean mAnimationCancelled;
    private TextView mErrorText;
    private Interpolator mFastOutLinearInInterpolator;
    private Interpolator mFastOutSlowInInterpolator;
    private ImageView mFingerprintAnimator;
    private AnimatedVectorDrawable mIconAnimationDrawable;
    private int mIconTouchCount;
    private int mIndicatorBackgroundActivatedColor;
    private int mIndicatorBackgroundRestingColor;
    private Interpolator mLinearOutSlowInInterpolator;
    private ObjectAnimator mProgressAnim;
    private ProgressBar mProgressBar;
    private TextView mRepeatMessage;
    private boolean mRestoring;
    private FingerprintEnrollSidecar mSidecar;
    private TextView mStartMessage;
    private final Animator.AnimatorListener mProgressAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (FingerprintEnrollEnrolling.this.mProgressBar.getProgress() < 10000) {
                return;
            }
            FingerprintEnrollEnrolling.this.mProgressBar.postDelayed(FingerprintEnrollEnrolling.this.mDelayedFinishRunnable, 250L);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };
    private final Runnable mDelayedFinishRunnable = new Runnable() {
        @Override
        public void run() {
            FingerprintEnrollEnrolling.this.launchFinish(FingerprintEnrollEnrolling.this.mToken);
        }
    };
    private final Animatable2.AnimationCallback mIconAnimationCallback = new Animatable2.AnimationCallback() {
        @Override
        public void onAnimationEnd(Drawable d) {
            if (FingerprintEnrollEnrolling.this.mAnimationCancelled) {
                return;
            }
            FingerprintEnrollEnrolling.this.mFingerprintAnimator.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintEnrollEnrolling.this.startIconAnimation();
                }
            });
        }
    };
    private final Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            FingerprintEnrollEnrolling.this.showIconTouchDialog();
        }
    };
    private final Runnable mTouchAgainRunnable = new Runnable() {
        @Override
        public void run() {
            FingerprintEnrollEnrolling.this.showError(FingerprintEnrollEnrolling.this.getString(R.string.security_settings_fingerprint_enroll_lift_touch_again));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.TYPE.equals("eng")) {
            getFragmentManager();
            FragmentManager.enableDebugLogging(true);
        }
        setContentView(R.layout.fingerprint_enroll_enrolling);
        setHeaderText(R.string.security_settings_fingerprint_enroll_start_title);
        this.mStartMessage = (TextView) findViewById(R.id.start_message);
        this.mRepeatMessage = (TextView) findViewById(R.id.repeat_message);
        this.mErrorText = (TextView) findViewById(R.id.error_text);
        this.mProgressBar = (ProgressBar) findViewById(R.id.fingerprint_progress_bar);
        this.mFingerprintAnimator = (ImageView) findViewById(R.id.fingerprint_animator);
        this.mIconAnimationDrawable = (AnimatedVectorDrawable) this.mFingerprintAnimator.getDrawable();
        this.mIconAnimationDrawable.registerAnimationCallback(this.mIconAnimationCallback);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.linear_out_slow_in);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_linear_in);
        this.mFingerprintAnimator.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == 0) {
                    FingerprintEnrollEnrolling.this.mIconTouchCount++;
                    if (FingerprintEnrollEnrolling.this.mIconTouchCount == 3) {
                        FingerprintEnrollEnrolling.this.showIconTouchDialog();
                    } else {
                        FingerprintEnrollEnrolling.this.mFingerprintAnimator.postDelayed(FingerprintEnrollEnrolling.this.mShowDialogRunnable, 500L);
                    }
                } else if (event.getActionMasked() == 3 || event.getActionMasked() == 1) {
                    FingerprintEnrollEnrolling.this.mFingerprintAnimator.removeCallbacks(FingerprintEnrollEnrolling.this.mShowDialogRunnable);
                }
                return true;
            }
        });
        this.mIndicatorBackgroundRestingColor = getColor(R.color.fingerprint_indicator_background_resting);
        this.mIndicatorBackgroundActivatedColor = getColor(R.color.fingerprint_indicator_background_activated);
        this.mRestoring = savedInstanceState != null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mSidecar = (FingerprintEnrollSidecar) getFragmentManager().findFragmentByTag("sidecar");
        if (this.mSidecar == null) {
            this.mSidecar = new FingerprintEnrollSidecar();
            getFragmentManager().beginTransaction().add(this.mSidecar, "sidecar").commit();
        }
        this.mSidecar.setListener(this);
        updateProgress(false);
        updateDescription();
        if (!this.mRestoring) {
            return;
        }
        startIconAnimation();
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        this.mAnimationCancelled = false;
        startIconAnimation();
    }

    public void startIconAnimation() {
        this.mIconAnimationDrawable.start();
    }

    private void stopIconAnimation() {
        this.mAnimationCancelled = true;
        this.mIconAnimationDrawable.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mSidecar != null) {
            this.mSidecar.setListener(null);
        }
        stopIconAnimation();
        if (isChangingConfigurations()) {
            return;
        }
        if (this.mSidecar != null) {
            this.mSidecar.cancelEnrollment();
            getFragmentManager().beginTransaction().remove(this.mSidecar).commitAllowingStateLoss();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        if (this.mSidecar != null) {
            this.mSidecar.setListener(null);
            this.mSidecar.cancelEnrollment();
            getFragmentManager().beginTransaction().remove(this.mSidecar).commitAllowingStateLoss();
            this.mSidecar = null;
        }
        super.onBackPressed();
    }

    private void animateProgress(int progress) {
        if (this.mProgressAnim != null) {
            this.mProgressAnim.cancel();
        }
        ObjectAnimator anim = ObjectAnimator.ofInt(this.mProgressBar, "progress", this.mProgressBar.getProgress(), progress);
        anim.addListener(this.mProgressAnimationListener);
        anim.setInterpolator(this.mFastOutSlowInInterpolator);
        anim.setDuration(250L);
        anim.start();
        this.mProgressAnim = anim;
    }

    private void animateFlash() {
        ValueAnimator anim = ValueAnimator.ofArgb(this.mIndicatorBackgroundRestingColor, this.mIndicatorBackgroundActivatedColor);
        final ValueAnimator.AnimatorUpdateListener listener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                FingerprintEnrollEnrolling.this.mFingerprintAnimator.setBackgroundTintList(ColorStateList.valueOf(((Integer) animation.getAnimatedValue()).intValue()));
            }
        };
        anim.addUpdateListener(listener);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ValueAnimator anim2 = ValueAnimator.ofArgb(FingerprintEnrollEnrolling.this.mIndicatorBackgroundActivatedColor, FingerprintEnrollEnrolling.this.mIndicatorBackgroundRestingColor);
                anim2.addUpdateListener(listener);
                anim2.setDuration(300L);
                anim2.setInterpolator(FingerprintEnrollEnrolling.this.mLinearOutSlowInInterpolator);
                anim2.start();
            }
        });
        anim.setInterpolator(this.mFastOutSlowInInterpolator);
        anim.setDuration(300L);
        anim.start();
    }

    public void launchFinish(byte[] token) {
        Intent intent = getFinishIntent();
        intent.addFlags(33554432);
        intent.putExtra("hw_auth_token", token);
        if (this.mUserId != -10000) {
            intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
        }
        startActivity(intent);
        finish();
    }

    protected Intent getFinishIntent() {
        return new Intent(this, (Class<?>) FingerprintEnrollFinish.class);
    }

    private void updateDescription() {
        if (this.mSidecar.getEnrollmentSteps() == -1) {
            setHeaderText(R.string.security_settings_fingerprint_enroll_start_title);
            this.mStartMessage.setVisibility(0);
            this.mRepeatMessage.setVisibility(4);
        } else {
            setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title, true);
            this.mStartMessage.setVisibility(4);
            this.mRepeatMessage.setVisibility(0);
        }
    }

    @Override
    public void onEnrollmentHelp(CharSequence helpString) {
        this.mErrorText.setText(helpString);
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        int msgId;
        switch (errMsgId) {
            case DefaultWfcSettingsExt.DESTROY:
                msgId = R.string.security_settings_fingerprint_enroll_error_timeout_dialog_message;
                break;
            default:
                msgId = R.string.security_settings_fingerprint_enroll_error_generic_dialog_message;
                break;
        }
        showErrorDialog(getText(msgId), errMsgId);
        stopIconAnimation();
        this.mErrorText.removeCallbacks(this.mTouchAgainRunnable);
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        updateProgress(true);
        updateDescription();
        clearError();
        animateFlash();
        this.mErrorText.removeCallbacks(this.mTouchAgainRunnable);
        this.mErrorText.postDelayed(this.mTouchAgainRunnable, 2500L);
    }

    private void updateProgress(boolean animate) {
        int progress = getProgress(this.mSidecar.getEnrollmentSteps(), this.mSidecar.getEnrollmentRemaining());
        if (animate) {
            animateProgress(progress);
        } else {
            this.mProgressBar.setProgress(progress);
        }
    }

    private int getProgress(int steps, int remaining) {
        if (steps == -1) {
            return 0;
        }
        int progress = Math.max(0, (steps + 1) - remaining);
        return (progress * 10000) / (steps + 1);
    }

    private void showErrorDialog(CharSequence msg, int msgId) {
        ErrorDialog dlg = ErrorDialog.newInstance(msg, msgId);
        dlg.show(getFragmentManager(), ErrorDialog.class.getName());
    }

    public void showIconTouchDialog() {
        this.mIconTouchCount = 0;
        new IconTouchDialog().show(getFragmentManager(), (String) null);
    }

    public void showError(CharSequence error) {
        this.mErrorText.setText(error);
        if (this.mErrorText.getVisibility() == 4) {
            this.mErrorText.setVisibility(0);
            this.mErrorText.setTranslationY(getResources().getDimensionPixelSize(R.dimen.fingerprint_error_text_appear_distance));
            this.mErrorText.setAlpha(0.0f);
            this.mErrorText.animate().alpha(1.0f).translationY(0.0f).setDuration(200L).setInterpolator(this.mLinearOutSlowInInterpolator).start();
            return;
        }
        this.mErrorText.animate().cancel();
        this.mErrorText.setAlpha(1.0f);
        this.mErrorText.setTranslationY(0.0f);
    }

    private void clearError() {
        if (this.mErrorText.getVisibility() == 0) {
            this.mErrorText.animate().alpha(0.0f).translationY(getResources().getDimensionPixelSize(R.dimen.fingerprint_error_text_disappear_distance)).setDuration(100L).setInterpolator(this.mFastOutLinearInInterpolator).withEndAction(new Runnable() {
                @Override
                public void run() {
                    FingerprintEnrollEnrolling.this.mErrorText.setVisibility(4);
                }
            }).start();
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 240;
    }

    public static class IconTouchDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.security_settings_fingerprint_enroll_touch_dialog_title).setMessage(R.string.security_settings_fingerprint_enroll_touch_dialog_message).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            return builder.create();
        }
    }

    public static class ErrorDialog extends DialogFragment {
        static ErrorDialog newInstance(CharSequence msg, int msgId) {
            ErrorDialog dlg = new ErrorDialog();
            Bundle args = new Bundle();
            args.putCharSequence("error_msg", msg);
            args.putInt("error_id", msgId);
            dlg.setArguments(args);
            return dlg;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            CharSequence errorString = getArguments().getCharSequence("error_msg");
            final int errMsgId = getArguments().getInt("error_id");
            builder.setTitle(R.string.security_settings_fingerprint_enroll_error_dialog_title).setMessage(errorString).setCancelable(false).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    boolean wasTimeout = errMsgId == 3;
                    Activity activity = ErrorDialog.this.getActivity();
                    activity.setResult(wasTimeout ? 3 : 1);
                    activity.finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }
}
