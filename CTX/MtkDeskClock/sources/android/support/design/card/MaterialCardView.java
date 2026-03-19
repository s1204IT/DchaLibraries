package android.support.design.card;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.internal.ThemeEnforcement;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

public class MaterialCardView extends CardView {
    public MaterialCardView(Context context) {
        this(context, null);
    }

    public MaterialCardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.materialCardViewStyle);
    }

    public MaterialCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributes = ThemeEnforcement.obtainStyledAttributes(context, attrs, R.styleable.MaterialCardView, defStyleAttr, R.style.Widget_MaterialComponents_CardView);
        MaterialCardViewHelper cardViewHelper = new MaterialCardViewHelper(this);
        cardViewHelper.loadFromAttributes(attributes);
        attributes.recycle();
    }
}
