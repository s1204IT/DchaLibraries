package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.UserManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

class KeyguardMultiUserAvatar extends FrameLayout {
    private static final String TAG = KeyguardMultiUserAvatar.class.getSimpleName();
    private boolean mActive;
    private final float mActiveAlpha;
    private final float mActiveScale;
    private final float mActiveTextAlpha;
    private final int mFrameColor;
    private final int mFrameShadowColor;
    private KeyguardCircleFramedDrawable mFramed;
    private final int mHighlightColor;
    private final float mIconSize;
    private final float mInactiveAlpha;
    private final float mInactiveTextAlpha;
    private boolean mInit;
    private boolean mPressLock;
    private final float mShadowRadius;
    private final float mStroke;
    private final int mTextColor;
    private boolean mTouched;
    private ImageView mUserImage;
    private UserInfo mUserInfo;
    private UserManager mUserManager;
    private TextView mUserName;
    private KeyguardMultiUserSelectorView mUserSelector;

    public static KeyguardMultiUserAvatar fromXml(int resId, Context context, KeyguardMultiUserSelectorView userSelector, UserInfo info) {
        KeyguardMultiUserAvatar icon = (KeyguardMultiUserAvatar) LayoutInflater.from(context).inflate(resId, (ViewGroup) userSelector, false);
        icon.init(info, userSelector);
        return icon;
    }

    public KeyguardMultiUserAvatar(Context context) {
        this(context, null, 0);
    }

    public KeyguardMultiUserAvatar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardMultiUserAvatar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mInit = true;
        Resources res = this.mContext.getResources();
        this.mTextColor = res.getColor(R.color.keyguard_avatar_nick_color);
        this.mIconSize = res.getDimension(R.dimen.keyguard_avatar_size);
        this.mStroke = res.getDimension(R.dimen.keyguard_avatar_frame_stroke_width);
        this.mShadowRadius = res.getDimension(R.dimen.keyguard_avatar_frame_shadow_radius);
        this.mFrameColor = res.getColor(R.color.keyguard_avatar_frame_color);
        this.mFrameShadowColor = res.getColor(R.color.keyguard_avatar_frame_shadow_color);
        this.mHighlightColor = res.getColor(R.color.keyguard_avatar_frame_pressed_color);
        this.mActiveTextAlpha = 0.0f;
        this.mInactiveTextAlpha = 0.5f;
        this.mActiveScale = 1.5f;
        this.mActiveAlpha = 1.0f;
        this.mInactiveAlpha = 1.0f;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mTouched = false;
        setLayerType(1, null);
    }

    public void init(UserInfo user, KeyguardMultiUserSelectorView userSelector) {
        this.mUserInfo = user;
        this.mUserSelector = userSelector;
        this.mUserImage = (ImageView) findViewById(R.id.keyguard_user_avatar);
        this.mUserName = (TextView) findViewById(R.id.keyguard_user_name);
        this.mFramed = (KeyguardCircleFramedDrawable) MultiUserAvatarCache.getInstance().get(user.id);
        if (this.mFramed == null || !this.mFramed.verifyParams(this.mIconSize, this.mFrameColor, this.mStroke, this.mFrameShadowColor, this.mShadowRadius, this.mHighlightColor)) {
            Bitmap icon = null;
            try {
                icon = this.mUserManager.getUserIcon(user.id);
            } catch (Exception e) {
            }
            if (icon == null) {
                icon = BitmapFactory.decodeResource(this.mContext.getResources(), android.R.drawable.emo_im_laughing);
            }
            this.mFramed = new KeyguardCircleFramedDrawable(icon, (int) this.mIconSize, this.mFrameColor, this.mStroke, this.mFrameShadowColor, this.mShadowRadius, this.mHighlightColor);
            MultiUserAvatarCache.getInstance().put(user.id, this.mFramed);
        }
        this.mFramed.reset();
        this.mUserImage.setImageDrawable(this.mFramed);
        this.mUserName.setText(this.mUserInfo.name);
        setOnClickListener(this.mUserSelector);
        this.mInit = false;
    }

    public void setActive(boolean active, boolean animate, Runnable onComplete) {
        if (this.mActive != active || this.mInit) {
            this.mActive = active;
            if (active) {
                KeyguardLinearLayout parent = (KeyguardLinearLayout) getParent();
                parent.setTopChild(this);
                setContentDescription(((Object) this.mUserName.getText()) + ". " + this.mContext.getString(R.string.user_switched, ""));
            } else {
                setContentDescription(this.mUserName.getText());
            }
        }
        updateVisualsForActive(this.mActive, animate, 150, onComplete);
    }

    void updateVisualsForActive(boolean active, boolean animate, int duration, final Runnable onComplete) {
        final float finalAlpha = active ? this.mActiveAlpha : this.mInactiveAlpha;
        final float initAlpha = active ? this.mInactiveAlpha : this.mActiveAlpha;
        final float finalScale = active ? 1.0f : 1.0f / this.mActiveScale;
        final float initScale = this.mFramed.getScale();
        final int finalTextAlpha = active ? (int) (this.mActiveTextAlpha * 255.0f) : (int) (this.mInactiveTextAlpha * 255.0f);
        final int initTextAlpha = active ? (int) (this.mInactiveTextAlpha * 255.0f) : (int) (this.mActiveTextAlpha * 255.0f);
        int textColor = this.mTextColor;
        this.mUserName.setTextColor(textColor);
        if (animate && this.mTouched) {
            ValueAnimator va = ValueAnimator.ofFloat(0.0f, 1.0f);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = animation.getAnimatedFraction();
                    float scale = ((1.0f - r) * initScale) + (finalScale * r);
                    float alpha = ((1.0f - r) * initAlpha) + (finalAlpha * r);
                    int textAlpha = (int) (((1.0f - r) * initTextAlpha) + (finalTextAlpha * r));
                    KeyguardMultiUserAvatar.this.mFramed.setScale(scale);
                    KeyguardMultiUserAvatar.this.mUserImage.setAlpha(alpha);
                    KeyguardMultiUserAvatar.this.mUserName.setTextColor(Color.argb(textAlpha, 255, 255, 255));
                    KeyguardMultiUserAvatar.this.mUserImage.invalidate();
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            va.setDuration(duration);
            va.start();
        } else {
            this.mFramed.setScale(finalScale);
            this.mUserImage.setAlpha(finalAlpha);
            this.mUserName.setTextColor(Color.argb(finalTextAlpha, 255, 255, 255));
            if (onComplete != null) {
                post(onComplete);
            }
        }
        this.mTouched = true;
    }

    @Override
    public void setPressed(boolean pressed) {
        if (!this.mPressLock || pressed) {
            if (this.mPressLock || !pressed || isClickable()) {
                super.setPressed(pressed);
                this.mFramed.setPressed(pressed);
                this.mUserImage.invalidate();
            }
        }
    }

    public void lockPressed(boolean pressed) {
        this.mPressLock = pressed;
        setPressed(pressed);
    }

    public UserInfo getUserInfo() {
        return this.mUserInfo;
    }
}
