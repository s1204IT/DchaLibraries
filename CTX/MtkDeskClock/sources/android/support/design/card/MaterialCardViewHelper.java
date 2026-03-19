package android.support.design.card;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.RestrictTo;
import android.support.v4.view.ViewCompat;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
class MaterialCardViewHelper {
    private static final int DEFAULT_STROKE_VALUE = -1;
    private float cornerRadius;
    private final MaterialCardView materialCardView;
    private int strokeColor;
    private int strokeWidth;

    public MaterialCardViewHelper(MaterialCardView card) {
        this.materialCardView = card;
    }

    public void loadFromAttributes(TypedArray attributes) {
        this.cornerRadius = attributes.getDimensionPixelSize(R.styleable.CardView_cardCornerRadius, 0);
        this.strokeColor = attributes.getColor(R.styleable.MaterialCardView_strokeColor, -1);
        this.strokeWidth = attributes.getDimensionPixelSize(R.styleable.MaterialCardView_strokeWidth, 0);
        ViewCompat.setBackground(this.materialCardView, createBackgroundDrawable());
        adjustContentPadding(this.strokeWidth);
    }

    private Drawable createBackgroundDrawable() {
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setCornerRadius(this.cornerRadius);
        if (this.strokeColor != -1) {
            bgDrawable.setStroke(this.strokeWidth, this.strokeColor);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            bgDrawable.setColor(this.materialCardView.getCardBackgroundColor());
        } else {
            bgDrawable.setColor(this.materialCardView.getCardBackgroundColor().getDefaultColor());
        }
        return bgDrawable;
    }

    private void adjustContentPadding(int strokeWidth) {
        int contentPaddingLeft = this.materialCardView.getContentPaddingLeft() + strokeWidth;
        int contentPaddingTop = this.materialCardView.getContentPaddingTop() + strokeWidth;
        int contentPaddingRight = this.materialCardView.getContentPaddingRight() + strokeWidth;
        int contentPaddingBottom = this.materialCardView.getContentPaddingBottom() + strokeWidth;
        this.materialCardView.setContentPadding(contentPaddingLeft, contentPaddingTop, contentPaddingRight, contentPaddingBottom);
    }
}
