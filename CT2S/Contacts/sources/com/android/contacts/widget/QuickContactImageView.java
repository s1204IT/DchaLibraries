package com.android.contacts.widget;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.contacts.R;
import com.android.contacts.common.lettertiles.LetterTileDrawable;

public class QuickContactImageView extends ImageView {
    private BitmapDrawable mBitmapDrawable;
    private boolean mIsBusiness;
    private Drawable mOriginalDrawable;
    private int mTintColor;

    public QuickContactImageView(Context context) {
        this(context, null);
    }

    public QuickContactImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickContactImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QuickContactImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setTint(int color) {
        if (this.mBitmapDrawable == null || this.mBitmapDrawable.getBitmap() == null || this.mBitmapDrawable.getBitmap().hasAlpha()) {
            setBackgroundColor(color);
        } else {
            setBackground(null);
        }
        this.mTintColor = color;
        postInvalidate();
    }

    public boolean isBasedOffLetterTile() {
        return this.mOriginalDrawable instanceof LetterTileDrawable;
    }

    public void setIsBusiness(boolean isBusiness) {
        this.mIsBusiness = isBusiness;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        BitmapDrawable bitmapDrawable;
        if (drawable == null || (drawable instanceof BitmapDrawable)) {
            bitmapDrawable = (BitmapDrawable) drawable;
        } else if (drawable instanceof LetterTileDrawable) {
            if (!this.mIsBusiness) {
                bitmapDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.person_white_540dp);
            } else {
                bitmapDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.generic_business_white_540dp);
            }
        } else {
            throw new IllegalArgumentException("Does not support this type of drawable");
        }
        this.mOriginalDrawable = drawable;
        this.mBitmapDrawable = bitmapDrawable;
        setTint(this.mTintColor);
        super.setImageDrawable(bitmapDrawable);
    }

    @Override
    public Drawable getDrawable() {
        return this.mOriginalDrawable;
    }
}
