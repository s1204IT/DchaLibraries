package com.android.systemui.tv.pip;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.R;

public class PipControlButtonView extends RelativeLayout {
    private Animator mButtonFocusGainAnimator;
    private Animator mButtonFocusLossAnimator;
    ImageView mButtonImageView;
    private TextView mDescriptionTextView;
    private View.OnFocusChangeListener mFocusChangeListener;
    private ImageView mIconImageView;
    private final View.OnFocusChangeListener mInternalFocusChangeListener;
    private Animator mTextFocusGainAnimator;
    private Animator mTextFocusLossAnimator;

    public PipControlButtonView(Context context) {
        this(context, null, 0, 0);
    }

    public PipControlButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public PipControlButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PipControlButtonView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mInternalFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    PipControlButtonView.this.startFocusGainAnimation();
                } else {
                    PipControlButtonView.this.startFocusLossAnimation();
                }
                if (PipControlButtonView.this.mFocusChangeListener == null) {
                    return;
                }
                PipControlButtonView.this.mFocusChangeListener.onFocusChange(PipControlButtonView.this, hasFocus);
            }
        };
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        inflater.inflate(R.layout.tv_pip_control_button, this);
        this.mIconImageView = (ImageView) findViewById(R.id.icon);
        this.mButtonImageView = (ImageView) findViewById(R.id.button);
        this.mDescriptionTextView = (TextView) findViewById(R.id.desc);
        int[] values = {android.R.attr.src, android.R.attr.text};
        TypedArray typedArray = context.obtainStyledAttributes(attrs, values, defStyleAttr, defStyleRes);
        setImageResource(typedArray.getResourceId(0, 0));
        setText(typedArray.getResourceId(1, 0));
        typedArray.recycle();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mButtonImageView.setOnFocusChangeListener(this.mInternalFocusChangeListener);
        this.mTextFocusGainAnimator = AnimatorInflater.loadAnimator(getContext(), R.anim.tv_pip_controls_focus_gain_animation);
        this.mTextFocusGainAnimator.setTarget(this.mDescriptionTextView);
        this.mButtonFocusGainAnimator = AnimatorInflater.loadAnimator(getContext(), R.anim.tv_pip_controls_focus_gain_animation);
        this.mButtonFocusGainAnimator.setTarget(this.mButtonImageView);
        this.mTextFocusLossAnimator = AnimatorInflater.loadAnimator(getContext(), R.anim.tv_pip_controls_focus_loss_animation);
        this.mTextFocusLossAnimator.setTarget(this.mDescriptionTextView);
        this.mButtonFocusLossAnimator = AnimatorInflater.loadAnimator(getContext(), R.anim.tv_pip_controls_focus_loss_animation);
        this.mButtonFocusLossAnimator.setTarget(this.mButtonImageView);
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener) {
        this.mButtonImageView.setOnClickListener(listener);
    }

    @Override
    public void setOnFocusChangeListener(View.OnFocusChangeListener listener) {
        this.mFocusChangeListener = listener;
    }

    public void setImageResource(int resId) {
        this.mIconImageView.setImageResource(resId);
    }

    public void setText(int resId) {
        this.mButtonImageView.setContentDescription(getContext().getString(resId));
        this.mDescriptionTextView.setText(resId);
    }

    private static void cancelAnimator(Animator animator) {
        if (!animator.isStarted()) {
            return;
        }
        animator.cancel();
    }

    public void startFocusGainAnimation() {
        cancelAnimator(this.mButtonFocusLossAnimator);
        cancelAnimator(this.mTextFocusLossAnimator);
        this.mTextFocusGainAnimator.start();
        if (this.mButtonImageView.getAlpha() >= 1.0f) {
            return;
        }
        this.mButtonFocusGainAnimator.start();
    }

    public void startFocusLossAnimation() {
        cancelAnimator(this.mButtonFocusGainAnimator);
        cancelAnimator(this.mTextFocusGainAnimator);
        this.mTextFocusLossAnimator.start();
        if (!this.mButtonImageView.hasFocus()) {
            return;
        }
        this.mButtonFocusLossAnimator.start();
    }

    public void reset() {
        cancelAnimator(this.mButtonFocusGainAnimator);
        cancelAnimator(this.mTextFocusGainAnimator);
        cancelAnimator(this.mButtonFocusLossAnimator);
        cancelAnimator(this.mTextFocusLossAnimator);
        this.mButtonImageView.setAlpha(1.0f);
        this.mDescriptionTextView.setAlpha(this.mButtonImageView.hasFocus() ? 1.0f : 0.0f);
    }
}
