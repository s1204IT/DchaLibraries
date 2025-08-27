package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.fingerprint.FingerprintEnrollSidecar;

/* loaded from: classes.dex */
public class FingerprintEnrollEnrolling extends FingerprintEnrollBase implements FingerprintEnrollSidecar.Listener {
    private boolean mAnimationCancelled;
    private TextView mErrorText;
    private Interpolator mFastOutLinearInInterpolator;
    private Interpolator mFastOutSlowInInterpolator;
    private AnimatedVectorDrawable mIconAnimationDrawable;
    private AnimatedVectorDrawable mIconBackgroundBlinksDrawable;
    private int mIconTouchCount;
    private Interpolator mLinearOutSlowInInterpolator;
    private ObjectAnimator mProgressAnim;
    private ProgressBar mProgressBar;
    private TextView mRepeatMessage;
    private boolean mRestoring;
    private FingerprintEnrollSidecar mSidecar;
    private TextView mStartMessage;
    private Vibrator mVibrator;
    private static final VibrationEffect VIBRATE_EFFECT_ERROR = VibrationEffect.createWaveform(new long[]{0, 5, 55, 60}, -1);
    private static final AudioAttributes FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private final Animator.AnimatorListener mProgressAnimationListener = new Animator.AnimatorListener() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.2
        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (FingerprintEnrollEnrolling.this.mProgressBar.getProgress() >= 10000) {
                FingerprintEnrollEnrolling.this.mProgressBar.postDelayed(FingerprintEnrollEnrolling.this.mDelayedFinishRunnable, 250L);
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
        }
    };
    private final Runnable mDelayedFinishRunnable = new Runnable() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.3
        @Override // java.lang.Runnable
        public void run() {
            FingerprintEnrollEnrolling.this.launchFinish(FingerprintEnrollEnrolling.this.mToken);
        }
    };
    private final Animatable2.AnimationCallback mIconAnimationCallback = new Animatable2.AnimationCallback() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.4
        @Override // android.graphics.drawable.Animatable2.AnimationCallback
        public void onAnimationEnd(Drawable drawable) {
            if (!FingerprintEnrollEnrolling.this.mAnimationCancelled) {
                FingerprintEnrollEnrolling.this.mProgressBar.post(new Runnable() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.4.1
                    @Override // java.lang.Runnable
                    public void run() {
                        FingerprintEnrollEnrolling.this.startIconAnimation();
                    }
                });
            }
        }
    };
    private final Runnable mShowDialogRunnable = new Runnable() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.5
        @Override // java.lang.Runnable
        public void run() {
            FingerprintEnrollEnrolling.this.showIconTouchDialog();
        }
    };
    private final Runnable mTouchAgainRunnable = new Runnable() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.6
        @Override // java.lang.Runnable
        public void run() {
            FingerprintEnrollEnrolling.this.showError(FingerprintEnrollEnrolling.this.getString(R.string.security_settings_fingerprint_enroll_lift_touch_again));
        }
    };

    static /* synthetic */ int access$008(FingerprintEnrollEnrolling fingerprintEnrollEnrolling) {
        int i = fingerprintEnrollEnrolling.mIconTouchCount;
        fingerprintEnrollEnrolling.mIconTouchCount = i + 1;
        return i;
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollBase, com.android.settings.core.InstrumentedActivity, com.android.settingslib.core.lifecycle.ObservableActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.fingerprint_enroll_enrolling);
        setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title);
        this.mStartMessage = (TextView) findViewById(R.id.start_message);
        this.mRepeatMessage = (TextView) findViewById(R.id.repeat_message);
        this.mErrorText = (TextView) findViewById(R.id.error_text);
        this.mProgressBar = (ProgressBar) findViewById(R.id.fingerprint_progress_bar);
        this.mVibrator = (Vibrator) getSystemService(Vibrator.class);
        ((Button) findViewById(R.id.skip_button)).setOnClickListener(this);
        LayerDrawable layerDrawable = (LayerDrawable) this.mProgressBar.getBackground();
        this.mIconAnimationDrawable = (AnimatedVectorDrawable) layerDrawable.findDrawableByLayerId(R.id.fingerprint_animation);
        this.mIconBackgroundBlinksDrawable = (AnimatedVectorDrawable) layerDrawable.findDrawableByLayerId(R.id.fingerprint_background);
        this.mIconAnimationDrawable.registerAnimationCallback(this.mIconAnimationCallback);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.linear_out_slow_in);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_linear_in);
        this.mProgressBar.setOnTouchListener(new View.OnTouchListener() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.1
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == 0) {
                    FingerprintEnrollEnrolling.access$008(FingerprintEnrollEnrolling.this);
                    if (FingerprintEnrollEnrolling.this.mIconTouchCount == 3) {
                        FingerprintEnrollEnrolling.this.showIconTouchDialog();
                    } else {
                        FingerprintEnrollEnrolling.this.mProgressBar.postDelayed(FingerprintEnrollEnrolling.this.mShowDialogRunnable, 500L);
                    }
                } else if (motionEvent.getActionMasked() == 3 || motionEvent.getActionMasked() == 1) {
                    FingerprintEnrollEnrolling.this.mProgressBar.removeCallbacks(FingerprintEnrollEnrolling.this.mShowDialogRunnable);
                }
                return true;
            }
        });
        this.mRestoring = bundle != null;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableActivity, android.app.Activity
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
        if (this.mRestoring) {
            startIconAnimation();
        }
    }

    @Override // android.app.Activity
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        this.mAnimationCancelled = false;
        startIconAnimation();
    }

    private void startIconAnimation() {
        this.mIconAnimationDrawable.start();
    }

    private void stopIconAnimation() {
        this.mAnimationCancelled = true;
        this.mIconAnimationDrawable.stop();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableActivity, android.app.Activity
    protected void onStop() {
        super.onStop();
        if (this.mSidecar != null) {
            this.mSidecar.setListener(null);
        }
        stopIconAnimation();
        if (!isChangingConfigurations()) {
            if (this.mSidecar != null) {
                this.mSidecar.cancelEnrollment();
                getFragmentManager().beginTransaction().remove(this.mSidecar).commitAllowingStateLoss();
            }
            finish();
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.mSidecar != null) {
            this.mSidecar.setListener(null);
            this.mSidecar.cancelEnrollment();
            getFragmentManager().beginTransaction().remove(this.mSidecar).commitAllowingStateLoss();
            this.mSidecar = null;
        }
        super.onBackPressed();
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollBase, android.view.View.OnClickListener
    public void onClick(View view) {
        if (view.getId() == R.id.skip_button) {
            setResult(2);
            finish();
        } else {
            super.onClick(view);
        }
    }

    private void animateProgress(int i) {
        if (this.mProgressAnim != null) {
            this.mProgressAnim.cancel();
        }
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this.mProgressBar, "progress", this.mProgressBar.getProgress(), i);
        objectAnimatorOfInt.addListener(this.mProgressAnimationListener);
        objectAnimatorOfInt.setInterpolator(this.mFastOutSlowInInterpolator);
        objectAnimatorOfInt.setDuration(250L);
        objectAnimatorOfInt.start();
        this.mProgressAnim = objectAnimatorOfInt;
    }

    private void animateFlash() {
        this.mIconBackgroundBlinksDrawable.start();
    }

    private void launchFinish(byte[] bArr) {
        Intent finishIntent = getFinishIntent();
        finishIntent.addFlags(637534208);
        finishIntent.putExtra("hw_auth_token", bArr);
        if (this.mUserId != -10000) {
            finishIntent.putExtra("android.intent.extra.USER_ID", this.mUserId);
        }
        startActivity(finishIntent);
        overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);
        finish();
    }

    protected Intent getFinishIntent() {
        return new Intent(this, (Class<?>) FingerprintEnrollFinish.class);
    }

    private void updateDescription() {
        if (this.mSidecar.getEnrollmentSteps() == -1) {
            this.mStartMessage.setVisibility(0);
            this.mRepeatMessage.setVisibility(4);
        } else {
            this.mStartMessage.setVisibility(4);
            this.mRepeatMessage.setVisibility(0);
        }
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollSidecar.Listener
    public void onEnrollmentHelp(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            this.mErrorText.removeCallbacks(this.mTouchAgainRunnable);
            showError(charSequence);
        }
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollSidecar.Listener
    public void onEnrollmentError(int i, CharSequence charSequence) {
        int i2;
        if (i == 3) {
            i2 = R.string.security_settings_fingerprint_enroll_error_timeout_dialog_message;
        } else {
            i2 = R.string.security_settings_fingerprint_enroll_error_generic_dialog_message;
        }
        showErrorDialog(getText(i2), i);
        stopIconAnimation();
        this.mErrorText.removeCallbacks(this.mTouchAgainRunnable);
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollSidecar.Listener
    public void onEnrollmentProgressChange(int i, int i2) {
        updateProgress(true);
        updateDescription();
        clearError();
        animateFlash();
        this.mErrorText.removeCallbacks(this.mTouchAgainRunnable);
        this.mErrorText.postDelayed(this.mTouchAgainRunnable, 2500L);
    }

    private void updateProgress(boolean z) {
        int progress = getProgress(this.mSidecar.getEnrollmentSteps(), this.mSidecar.getEnrollmentRemaining());
        if (z) {
            animateProgress(progress);
            return;
        }
        this.mProgressBar.setProgress(progress);
        if (progress >= 10000) {
            this.mDelayedFinishRunnable.run();
        }
    }

    private int getProgress(int i, int i2) {
        if (i == -1) {
            return 0;
        }
        int i3 = i + 1;
        return (10000 * Math.max(0, i3 - i2)) / i3;
    }

    private void showErrorDialog(CharSequence charSequence, int i) {
        ErrorDialog.newInstance(charSequence, i).show(getFragmentManager(), ErrorDialog.class.getName());
    }

    private void showIconTouchDialog() {
        this.mIconTouchCount = 0;
        new IconTouchDialog().show(getFragmentManager(), (String) null);
    }

    private void showError(CharSequence charSequence) {
        this.mErrorText.setText(charSequence);
        if (this.mErrorText.getVisibility() == 4) {
            this.mErrorText.setVisibility(0);
            this.mErrorText.setTranslationY(getResources().getDimensionPixelSize(R.dimen.fingerprint_error_text_appear_distance));
            this.mErrorText.setAlpha(0.0f);
            this.mErrorText.animate().alpha(1.0f).translationY(0.0f).setDuration(200L).setInterpolator(this.mLinearOutSlowInInterpolator).start();
        } else {
            this.mErrorText.animate().cancel();
            this.mErrorText.setAlpha(1.0f);
            this.mErrorText.setTranslationY(0.0f);
        }
        if (isResumed()) {
            this.mVibrator.vibrate(VIBRATE_EFFECT_ERROR, FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES);
        }
    }

    private void clearError() {
        if (this.mErrorText.getVisibility() == 0) {
            this.mErrorText.animate().alpha(0.0f).translationY(getResources().getDimensionPixelSize(R.dimen.fingerprint_error_text_disappear_distance)).setDuration(100L).setInterpolator(this.mFastOutLinearInInterpolator).withEndAction(new Runnable() { // from class: com.android.settings.fingerprint.-$$Lambda$FingerprintEnrollEnrolling$aLk12WuaBTV2piitP3fdnB0w-eM
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.mErrorText.setVisibility(4);
                }
            }).start();
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 240;
    }

    public static class IconTouchDialog extends InstrumentedDialogFragment {
        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.security_settings_fingerprint_enroll_touch_dialog_title).setMessage(R.string.security_settings_fingerprint_enroll_touch_dialog_message).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.IconTouchDialog.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            return builder.create();
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 568;
        }
    }

    public static class ErrorDialog extends InstrumentedDialogFragment {
        static ErrorDialog newInstance(CharSequence charSequence, int i) {
            ErrorDialog errorDialog = new ErrorDialog();
            Bundle bundle = new Bundle();
            bundle.putCharSequence("error_msg", charSequence);
            bundle.putInt("error_id", i);
            errorDialog.setArguments(bundle);
            return errorDialog;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            CharSequence charSequence = getArguments().getCharSequence("error_msg");
            final int i = getArguments().getInt("error_id");
            builder.setTitle(R.string.security_settings_fingerprint_enroll_error_dialog_title).setMessage(charSequence).setCancelable(false).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.fingerprint.FingerprintEnrollEnrolling.ErrorDialog.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    boolean z;
                    dialogInterface.dismiss();
                    if (i != 3) {
                        z = false;
                    } else {
                        z = true;
                    }
                    Activity activity = ErrorDialog.this.getActivity();
                    activity.setResult(z ? 3 : 1);
                    activity.finish();
                }
            });
            AlertDialog alertDialogCreate = builder.create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            return alertDialogCreate;
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 569;
        }
    }
}
