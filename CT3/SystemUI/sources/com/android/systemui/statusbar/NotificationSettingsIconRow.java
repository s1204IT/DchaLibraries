package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class NotificationSettingsIconRow extends FrameLayout implements View.OnClickListener {
    private boolean mAnimating;
    private boolean mDismissing;
    private ValueAnimator mFadeAnimator;
    private AlphaOptimizedImageView mGearIcon;
    private int[] mGearLocation;
    private float mHorizSpaceForGear;
    private boolean mIconPlaced;
    private SettingsIconRowListener mListener;
    private boolean mOnLeft;
    private ExpandableNotificationRow mParent;
    private int[] mParentLocation;
    private boolean mSettingsFadedIn;
    private boolean mSnapping;
    private int mVertSpaceForGear;

    public interface SettingsIconRowListener {
        void onGearTouched(ExpandableNotificationRow expandableNotificationRow, int i, int i2);

        void onSettingsIconRowReset(ExpandableNotificationRow expandableNotificationRow);
    }

    public NotificationSettingsIconRow(Context context) {
        this(context, null);
    }

    public NotificationSettingsIconRow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationSettingsIconRow(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationSettingsIconRow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs);
        this.mSettingsFadedIn = false;
        this.mAnimating = false;
        this.mOnLeft = true;
        this.mDismissing = false;
        this.mSnapping = false;
        this.mIconPlaced = false;
        this.mGearLocation = new int[2];
        this.mParentLocation = new int[2];
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mGearIcon = (AlphaOptimizedImageView) findViewById(R.id.gear_icon);
        this.mGearIcon.setOnClickListener(this);
        setOnClickListener(this);
        this.mHorizSpaceForGear = getResources().getDimensionPixelOffset(R.dimen.notification_gear_width);
        this.mVertSpaceForGear = getResources().getDimensionPixelOffset(R.dimen.notification_min_height);
        resetState();
    }

    public void resetState() {
        setGearAlpha(0.0f);
        this.mIconPlaced = false;
        this.mSettingsFadedIn = false;
        this.mAnimating = false;
        this.mSnapping = false;
        this.mDismissing = false;
        setIconLocation(true);
        if (this.mListener == null) {
            return;
        }
        this.mListener.onSettingsIconRowReset(this.mParent);
    }

    public void setGearListener(SettingsIconRowListener listener) {
        this.mListener = listener;
    }

    public void setNotificationRowParent(ExpandableNotificationRow parent) {
        this.mParent = parent;
        setIconLocation(this.mOnLeft);
    }

    public void setAppName(String appName) {
        Resources res = getResources();
        String description = String.format(res.getString(R.string.notification_gear_accessibility), appName);
        this.mGearIcon.setContentDescription(description);
    }

    public void setGearAlpha(float alpha) {
        if (alpha == 0.0f) {
            this.mSettingsFadedIn = false;
            setVisibility(4);
        } else {
            setVisibility(0);
        }
        this.mGearIcon.setAlpha(alpha);
    }

    public boolean isIconOnLeft() {
        return this.mOnLeft;
    }

    public float getSpaceForGear() {
        return this.mHorizSpaceForGear;
    }

    public boolean isVisible() {
        return this.mGearIcon.getAlpha() > 0.0f;
    }

    public void cancelFadeAnimator() {
        if (this.mFadeAnimator == null) {
            return;
        }
        this.mFadeAnimator.cancel();
    }

    public void updateSettingsIcons(float transX, float size) {
        float desiredAlpha;
        if (this.mAnimating || !this.mSettingsFadedIn) {
            return;
        }
        float fadeThreshold = size * 0.3f;
        float absTrans = Math.abs(transX);
        if (absTrans == 0.0f) {
            desiredAlpha = 0.0f;
        } else if (absTrans <= fadeThreshold) {
            desiredAlpha = 1.0f;
        } else {
            desiredAlpha = 1.0f - ((absTrans - fadeThreshold) / (size - fadeThreshold));
        }
        setGearAlpha(desiredAlpha);
    }

    public void fadeInSettings(final boolean fromLeft, final float transX, final float notiThreshold) {
        if (this.mDismissing || this.mAnimating) {
            return;
        }
        if (isIconLocationChange(transX)) {
            setGearAlpha(0.0f);
        }
        setIconLocation(transX > 0.0f);
        this.mFadeAnimator = ValueAnimator.ofFloat(this.mGearIcon.getAlpha(), 1.0f);
        this.mFadeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                boolean pastGear = true;
                float absTrans = Math.abs(transX);
                if ((!fromLeft || transX > notiThreshold) && (fromLeft || absTrans > notiThreshold)) {
                    pastGear = false;
                }
                if (!pastGear || NotificationSettingsIconRow.this.mSettingsFadedIn) {
                    return;
                }
                NotificationSettingsIconRow.this.setGearAlpha(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        this.mFadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                NotificationSettingsIconRow.this.mAnimating = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                NotificationSettingsIconRow.this.mGearIcon.setAlpha(0.0f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                NotificationSettingsIconRow.this.mAnimating = false;
                NotificationSettingsIconRow.this.mSettingsFadedIn = NotificationSettingsIconRow.this.mGearIcon.getAlpha() == 1.0f;
            }
        });
        this.mFadeAnimator.setInterpolator(Interpolators.ALPHA_IN);
        this.mFadeAnimator.setDuration(200L);
        this.mFadeAnimator.start();
    }

    public void updateVerticalLocation() {
        if (this.mParent == null) {
            return;
        }
        int parentHeight = this.mParent.getCollapsedHeight();
        if (parentHeight < this.mVertSpaceForGear) {
            this.mGearIcon.setTranslationY((parentHeight / 2) - (this.mGearIcon.getHeight() / 2));
        } else {
            this.mGearIcon.setTranslationY((this.mVertSpaceForGear - this.mGearIcon.getHeight()) / 2);
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        setIconLocation(this.mOnLeft);
    }

    public void setIconLocation(boolean onLeft) {
        if ((this.mIconPlaced && onLeft == this.mOnLeft) || this.mSnapping || this.mParent == null || this.mGearIcon.getWidth() == 0) {
            return;
        }
        boolean isRtl = this.mParent.isLayoutRtl();
        float left = isRtl ? -(this.mParent.getWidth() - this.mHorizSpaceForGear) : 0.0f;
        float right = isRtl ? 0.0f : this.mParent.getWidth() - this.mHorizSpaceForGear;
        float centerX = (this.mHorizSpaceForGear - this.mGearIcon.getWidth()) / 2.0f;
        setTranslationX(onLeft ? left + centerX : right + centerX);
        this.mOnLeft = onLeft;
        this.mIconPlaced = true;
    }

    public boolean isIconLocationChange(float translation) {
        boolean onLeft = translation > ((float) this.mGearIcon.getPaddingStart());
        boolean onRight = translation < ((float) (-this.mGearIcon.getPaddingStart()));
        if (this.mOnLeft && onRight) {
            return true;
        }
        if (!this.mOnLeft && onLeft) {
            return true;
        }
        return false;
    }

    public void setSnapping(boolean snapping) {
        this.mSnapping = snapping;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.gear_icon || this.mListener == null) {
            return;
        }
        this.mGearIcon.getLocationOnScreen(this.mGearLocation);
        this.mParent.getLocationOnScreen(this.mParentLocation);
        int centerX = (int) (this.mHorizSpaceForGear / 2.0f);
        int centerY = ((int) ((this.mGearIcon.getTranslationY() * 2.0f) + this.mGearIcon.getHeight())) / 2;
        int x = (this.mGearLocation[0] - this.mParentLocation[0]) + centerX;
        int y = (this.mGearLocation[1] - this.mParentLocation[1]) + centerY;
        this.mListener.onGearTouched(this.mParent, x, y);
    }
}
