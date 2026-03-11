package android.support.v17.leanback.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ImageCardView extends BaseCardView {
    private boolean mAttachedToWindow;
    private ImageView mBadgeImage;
    private TextView mContentView;
    ObjectAnimator mFadeInAnimator;
    private ImageView mImageView;
    private ViewGroup mInfoArea;
    private TextView mTitleView;

    public ImageCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        buildImageCardView(attrs, defStyleAttr, R.style.Widget_Leanback_ImageCardView);
    }

    private void buildImageCardView(AttributeSet attrs, int defStyleAttr, int defStyle) {
        boolean hasImageOnly;
        boolean hasTitle;
        boolean hasContent;
        boolean hasIconRight;
        boolean hasIconLeft;
        setFocusable(true);
        setFocusableInTouchMode(true);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.lb_image_card_view, this);
        TypedArray cardAttrs = getContext().obtainStyledAttributes(attrs, R.styleable.lbImageCardView, defStyleAttr, defStyle);
        int cardType = cardAttrs.getInt(R.styleable.lbImageCardView_lbImageCardViewType, 0);
        if (cardType == 0) {
            hasImageOnly = true;
        } else {
            hasImageOnly = false;
        }
        if ((cardType & 1) == 1) {
            hasTitle = true;
        } else {
            hasTitle = false;
        }
        if ((cardType & 2) == 2) {
            hasContent = true;
        } else {
            hasContent = false;
        }
        if ((cardType & 4) == 4) {
            hasIconRight = true;
        } else {
            hasIconRight = false;
        }
        if (!hasIconRight && (cardType & 8) == 8) {
            hasIconLeft = true;
        } else {
            hasIconLeft = false;
        }
        this.mImageView = (ImageView) findViewById(R.id.main_image);
        if (this.mImageView.getDrawable() == null) {
            this.mImageView.setVisibility(4);
        }
        this.mFadeInAnimator = ObjectAnimator.ofFloat(this.mImageView, "alpha", 1.0f);
        this.mFadeInAnimator.setDuration(this.mImageView.getResources().getInteger(android.R.integer.config_shortAnimTime));
        this.mInfoArea = (ViewGroup) findViewById(R.id.info_field);
        if (hasImageOnly) {
            removeView(this.mInfoArea);
            cardAttrs.recycle();
            return;
        }
        if (hasTitle) {
            this.mTitleView = (TextView) inflater.inflate(R.layout.lb_image_card_view_themed_title, this.mInfoArea, false);
            this.mInfoArea.addView(this.mTitleView);
        }
        if (hasContent) {
            this.mContentView = (TextView) inflater.inflate(R.layout.lb_image_card_view_themed_content, this.mInfoArea, false);
            this.mInfoArea.addView(this.mContentView);
        }
        if (hasIconRight || hasIconLeft) {
            int layoutId = R.layout.lb_image_card_view_themed_badge_right;
            if (hasIconLeft) {
                layoutId = R.layout.lb_image_card_view_themed_badge_left;
            }
            this.mBadgeImage = (ImageView) inflater.inflate(layoutId, this.mInfoArea, false);
            this.mInfoArea.addView(this.mBadgeImage);
        }
        if (hasTitle && !hasContent && this.mBadgeImage != null) {
            RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) this.mTitleView.getLayoutParams();
            if (!hasIconLeft) {
                relativeLayoutParams.addRule(16, this.mBadgeImage.getId());
            } else {
                relativeLayoutParams.addRule(17, this.mBadgeImage.getId());
            }
            this.mTitleView.setLayoutParams(relativeLayoutParams);
        }
        if (hasContent) {
            RelativeLayout.LayoutParams relativeLayoutParams2 = (RelativeLayout.LayoutParams) this.mContentView.getLayoutParams();
            if (!hasTitle) {
                relativeLayoutParams2.addRule(10);
            }
            if (hasIconLeft) {
                relativeLayoutParams2.removeRule(16);
                relativeLayoutParams2.removeRule(20);
                relativeLayoutParams2.addRule(17, this.mBadgeImage.getId());
            }
            this.mContentView.setLayoutParams(relativeLayoutParams2);
        }
        if (this.mBadgeImage != null) {
            RelativeLayout.LayoutParams relativeLayoutParams3 = (RelativeLayout.LayoutParams) this.mBadgeImage.getLayoutParams();
            if (hasContent) {
                relativeLayoutParams3.addRule(8, this.mContentView.getId());
            } else if (hasTitle) {
                relativeLayoutParams3.addRule(8, this.mTitleView.getId());
            }
            this.mBadgeImage.setLayoutParams(relativeLayoutParams3);
        }
        Drawable background = cardAttrs.getDrawable(R.styleable.lbImageCardView_infoAreaBackground);
        if (background != null) {
            setInfoAreaBackground(background);
        }
        if (this.mBadgeImage != null && this.mBadgeImage.getDrawable() == null) {
            this.mBadgeImage.setVisibility(8);
        }
        cardAttrs.recycle();
    }

    public ImageCardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.imageCardViewStyle);
    }

    public void setInfoAreaBackground(Drawable drawable) {
        if (this.mInfoArea != null) {
            this.mInfoArea.setBackground(drawable);
        }
    }

    private void fadeIn() {
        this.mImageView.setAlpha(0.0f);
        if (this.mAttachedToWindow) {
            this.mFadeInAnimator.start();
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttachedToWindow = true;
        if (this.mImageView.getAlpha() == 0.0f) {
            fadeIn();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mAttachedToWindow = false;
        this.mFadeInAnimator.cancel();
        this.mImageView.setAlpha(1.0f);
        super.onDetachedFromWindow();
    }
}
