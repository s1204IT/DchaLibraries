package com.android.systemui.fingerprint;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class FingerprintDialogView extends LinearLayout {
    private boolean mAnimatingAway;
    private final float mAnimationTranslationOffset;
    private Bundle mBundle;
    private final LinearLayout mDialog;
    private final float mDisplayWidth;
    private final int mErrorColor;
    private final TextView mErrorText;
    private final int mFingerprintColor;
    private Handler mHandler;
    private int mLastState;
    private ViewGroup mLayout;
    private final Interpolator mLinearOutSlowIn;
    private final Runnable mShowAnimationRunnable;
    private final int mTextColor;
    private boolean mWasForceRemoved;
    private final WindowManager mWindowManager;
    private final IBinder mWindowToken;

    public FingerprintDialogView(Context context, Handler handler) {
        super(context);
        this.mWindowToken = new Binder();
        this.mShowAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                FingerprintDialogView.this.mLayout.animate().alpha(1.0f).setDuration(250L).setInterpolator(FingerprintDialogView.this.mLinearOutSlowIn).withLayer().start();
                FingerprintDialogView.this.mDialog.animate().translationY(0.0f).setDuration(250L).setInterpolator(FingerprintDialogView.this.mLinearOutSlowIn).withLayer().start();
            }
        };
        this.mHandler = handler;
        this.mLinearOutSlowIn = Interpolators.LINEAR_OUT_SLOW_IN;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mAnimationTranslationOffset = getResources().getDimension(R.dimen.fingerprint_dialog_animation_translation_offset);
        this.mErrorColor = Color.parseColor(getResources().getString(R.color.fingerprint_dialog_error_color));
        this.mTextColor = Color.parseColor(getResources().getString(R.color.fingerprint_dialog_text_light_color));
        this.mFingerprintColor = Color.parseColor(getResources().getString(R.color.fingerprint_dialog_fingerprint_color));
        this.mWindowManager.getDefaultDisplay().getMetrics(new DisplayMetrics());
        this.mDisplayWidth = r5.widthPixels;
        this.mLayout = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.fingerprint_dialog, (ViewGroup) this, false);
        addView(this.mLayout);
        this.mDialog = (LinearLayout) this.mLayout.findViewById(R.id.dialog);
        this.mErrorText = (TextView) this.mLayout.findViewById(R.id.error);
        this.mLayout.setOnKeyListener(new View.OnKeyListener() {
            boolean downPressed = false;

            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i != 4) {
                    return false;
                }
                if (keyEvent.getAction() == 0 && !this.downPressed) {
                    this.downPressed = true;
                } else if (keyEvent.getAction() == 0) {
                    this.downPressed = false;
                } else if (keyEvent.getAction() == 1 && this.downPressed) {
                    this.downPressed = false;
                    FingerprintDialogView.this.mHandler.obtainMessage(7).sendToTarget();
                }
                return true;
            }
        });
        View viewFindViewById = this.mLayout.findViewById(R.id.space);
        View viewFindViewById2 = this.mLayout.findViewById(R.id.left_space);
        View viewFindViewById3 = this.mLayout.findViewById(R.id.right_space);
        Button button = (Button) this.mLayout.findViewById(R.id.button2);
        Button button2 = (Button) this.mLayout.findViewById(R.id.button1);
        setDismissesDialog(viewFindViewById);
        setDismissesDialog(viewFindViewById2);
        setDismissesDialog(viewFindViewById3);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.mHandler.obtainMessage(6).sendToTarget();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.mHandler.obtainMessage(8).sendToTarget();
            }
        });
        this.mLayout.setFocusableInTouchMode(true);
        this.mLayout.requestFocus();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        TextView textView = (TextView) this.mLayout.findViewById(R.id.title);
        TextView textView2 = (TextView) this.mLayout.findViewById(R.id.subtitle);
        TextView textView3 = (TextView) this.mLayout.findViewById(R.id.description);
        Button button = (Button) this.mLayout.findViewById(R.id.button2);
        Button button2 = (Button) this.mLayout.findViewById(R.id.button1);
        this.mDialog.getLayoutParams().width = (int) this.mDisplayWidth;
        this.mLastState = 0;
        updateFingerprintIcon(1);
        textView.setText(this.mBundle.getCharSequence("title"));
        textView.setSelected(true);
        CharSequence charSequence = this.mBundle.getCharSequence("subtitle");
        if (TextUtils.isEmpty(charSequence)) {
            textView2.setVisibility(8);
        } else {
            textView2.setVisibility(0);
            textView2.setText(charSequence);
        }
        CharSequence charSequence2 = this.mBundle.getCharSequence("description");
        if (TextUtils.isEmpty(charSequence2)) {
            textView3.setVisibility(8);
        } else {
            textView3.setVisibility(0);
            textView3.setText(charSequence2);
        }
        button.setText(this.mBundle.getCharSequence("negative_text"));
        CharSequence charSequence3 = this.mBundle.getCharSequence("positive_text");
        button2.setText(charSequence3);
        if (charSequence3 != null) {
            button2.setVisibility(0);
        } else {
            button2.setVisibility(8);
        }
        if (!this.mWasForceRemoved) {
            this.mDialog.setTranslationY(this.mAnimationTranslationOffset);
            this.mLayout.setAlpha(0.0f);
            postOnAnimation(this.mShowAnimationRunnable);
        } else {
            this.mLayout.animate().cancel();
            this.mDialog.animate().cancel();
            this.mDialog.setAlpha(1.0f);
            this.mDialog.setTranslationY(0.0f);
            this.mLayout.setAlpha(1.0f);
        }
        this.mWasForceRemoved = false;
    }

    private void setDismissesDialog(View view) {
        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view2, MotionEvent motionEvent) {
                return FingerprintDialogView.lambda$setDismissesDialog$2(this.f$0, view2, motionEvent);
            }
        });
    }

    public static boolean lambda$setDismissesDialog$2(FingerprintDialogView fingerprintDialogView, View view, MotionEvent motionEvent) {
        fingerprintDialogView.mHandler.obtainMessage(5, true).sendToTarget();
        return true;
    }

    public void startDismiss() {
        this.mAnimatingAway = true;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                FingerprintDialogView.this.mWindowManager.removeView(FingerprintDialogView.this);
                FingerprintDialogView.this.mAnimatingAway = false;
            }
        };
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                FingerprintDialogView.this.mLayout.animate().alpha(0.0f).setDuration(350L).setInterpolator(FingerprintDialogView.this.mLinearOutSlowIn).withLayer().start();
                FingerprintDialogView.this.mDialog.animate().translationY(FingerprintDialogView.this.mAnimationTranslationOffset).setDuration(350L).setInterpolator(FingerprintDialogView.this.mLinearOutSlowIn).withLayer().withEndAction(runnable).start();
            }
        });
    }

    public void forceRemove() {
        this.mLayout.animate().cancel();
        this.mDialog.animate().cancel();
        this.mWindowManager.removeView(this);
        this.mAnimatingAway = false;
        this.mWasForceRemoved = true;
    }

    public boolean isAnimatingAway() {
        return this.mAnimatingAway;
    }

    public void setBundle(Bundle bundle) {
        this.mBundle = bundle;
    }

    protected void resetMessage() {
        updateFingerprintIcon(1);
        this.mErrorText.setText(R.string.fingerprint_dialog_touch_sensor);
        this.mErrorText.setTextColor(this.mTextColor);
    }

    private void showTemporaryMessage(String str) {
        this.mHandler.removeMessages(9);
        updateFingerprintIcon(2);
        this.mErrorText.setText(str);
        this.mErrorText.setTextColor(this.mErrorColor);
        this.mErrorText.setContentDescription(str);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(9), 2000L);
    }

    public void showHelpMessage(String str) {
        showTemporaryMessage(str);
    }

    public void showErrorMessage(String str) {
        showTemporaryMessage(str);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, false), 2000L);
    }

    private void updateFingerprintIcon(int i) {
        AnimatedVectorDrawable animatedVectorDrawable;
        Drawable animationForTransition = getAnimationForTransition(this.mLastState, i);
        if (animationForTransition == null) {
            Log.e("FingerprintDialogView", "Animation not found");
            return;
        }
        if (animationForTransition instanceof AnimatedVectorDrawable) {
            animatedVectorDrawable = (AnimatedVectorDrawable) animationForTransition;
        } else {
            animatedVectorDrawable = null;
        }
        ((ImageView) this.mLayout.findViewById(R.id.fingerprint_icon)).setImageDrawable(animationForTransition);
        if (animatedVectorDrawable != null && shouldAnimateForTransition(this.mLastState, i)) {
            animatedVectorDrawable.forceAnimationOnUI();
            animatedVectorDrawable.start();
        }
        this.mLastState = i;
    }

    private boolean shouldAnimateForTransition(int i, int i2) {
        if (i == 0 && i2 == 1) {
            return false;
        }
        if (i == 1 && i2 == 2) {
            return true;
        }
        if (i == 2 && i2 == 1) {
            return true;
        }
        return (i != 1 || i2 == 3) ? false : false;
    }

    private Drawable getAnimationForTransition(int i, int i2) {
        int i3 = R.drawable.fingerprint_dialog_error_to_fp;
        if ((i != 0 || i2 != 1) && (i != 1 || i2 != 2)) {
            if ((i != 2 || i2 != 1) && (i != 1 || i2 != 3)) {
                return null;
            }
        } else {
            i3 = R.drawable.fingerprint_dialog_fp_to_error;
        }
        return this.mContext.getDrawable(i3);
    }

    @Override
    public WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2014, 16777216, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle("FingerprintDialogView");
        layoutParams.token = this.mWindowToken;
        return layoutParams;
    }
}
