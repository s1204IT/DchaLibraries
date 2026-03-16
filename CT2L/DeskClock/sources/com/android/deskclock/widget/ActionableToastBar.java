package com.android.deskclock.widget;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.deskclock.R;

public class ActionableToastBar extends LinearLayout {
    private View mActionButton;
    private ImageView mActionDescriptionIcon;
    private TextView mActionDescriptionView;
    private View mActionIcon;
    private TextView mActionText;
    private final int mBottomMarginSizeInConversation;
    private boolean mHidden;
    private Animator mHideAnimation;
    private Animator mShowAnimation;

    public interface ActionClickedListener {
        void onActionClicked();
    }

    public ActionableToastBar(Context context) {
        this(context, null);
    }

    public ActionableToastBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionableToastBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHidden = false;
        this.mBottomMarginSizeInConversation = context.getResources().getDimensionPixelSize(R.dimen.toast_bar_bottom_margin_in_conversation);
        LayoutInflater.from(context).inflate(R.layout.actionable_toast_row, (ViewGroup) this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mActionDescriptionIcon = (ImageView) findViewById(R.id.description_icon);
        this.mActionDescriptionView = (TextView) findViewById(R.id.description_text);
        this.mActionButton = findViewById(R.id.action_button);
        this.mActionIcon = findViewById(R.id.action_icon);
        this.mActionText = (TextView) findViewById(R.id.action_text);
    }

    public void show(final ActionClickedListener listener, int descriptionIconResourceId, CharSequence descriptionText, boolean showActionIcon, int actionTextResource, boolean replaceVisibleToast) {
        if (this.mHidden || replaceVisibleToast) {
            this.mActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View widget) {
                    if (listener != null) {
                        listener.onActionClicked();
                    }
                    ActionableToastBar.this.hide(true);
                }
            });
            if (descriptionIconResourceId == 0) {
                this.mActionDescriptionIcon.setVisibility(8);
            } else {
                this.mActionDescriptionIcon.setVisibility(0);
                this.mActionDescriptionIcon.setImageResource(descriptionIconResourceId);
            }
            this.mActionDescriptionView.setText(descriptionText);
            this.mActionIcon.setVisibility(showActionIcon ? 0 : 8);
            this.mActionText.setText(actionTextResource);
            this.mHidden = false;
            getShowAnimation().start();
        }
    }

    public void hide(boolean animate) {
        if (!this.mHidden && !getShowAnimation().isRunning()) {
            this.mHidden = true;
            if (getVisibility() == 0) {
                this.mActionDescriptionView.setText("");
                this.mActionButton.setOnClickListener(null);
                if (animate) {
                    getHideAnimation().start();
                } else {
                    setAlpha(0.0f);
                    setVisibility(8);
                }
            }
        }
    }

    private Animator getShowAnimation() {
        if (this.mShowAnimation == null) {
            this.mShowAnimation = AnimatorInflater.loadAnimator(getContext(), R.animator.fade_in);
            this.mShowAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ActionableToastBar.this.setVisibility(0);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ActionableToastBar.this.setVisibility(0);
                }
            });
            this.mShowAnimation.setTarget(this);
        }
        return this.mShowAnimation;
    }

    private Animator getHideAnimation() {
        if (this.mHideAnimation == null) {
            this.mHideAnimation = AnimatorInflater.loadAnimator(getContext(), R.animator.fade_out);
            this.mHideAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ActionableToastBar.this.setVisibility(8);
                }
            });
            this.mHideAnimation.setTarget(this);
        }
        return this.mHideAnimation;
    }

    public boolean isEventInToastBar(MotionEvent event) {
        if (!isShown()) {
            return false;
        }
        int[] xy = new int[2];
        float x = event.getX();
        float y = event.getY();
        getLocationOnScreen(xy);
        return x > ((float) xy[0]) && x < ((float) (xy[0] + getWidth())) && y > ((float) xy[1]) && y < ((float) (xy[1] + getHeight()));
    }
}
