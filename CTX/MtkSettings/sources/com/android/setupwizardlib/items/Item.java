package com.android.setupwizardlib.items;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.setupwizardlib.R;

public class Item extends AbstractItem {
    private boolean mEnabled;
    private Drawable mIcon;
    private int mLayoutRes;
    private CharSequence mSummary;
    private CharSequence mTitle;
    private boolean mVisible;

    public Item() {
        this.mEnabled = true;
        this.mVisible = true;
        this.mLayoutRes = getDefaultLayoutResource();
    }

    public Item(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEnabled = true;
        this.mVisible = true;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwItem);
        this.mEnabled = typedArrayObtainStyledAttributes.getBoolean(R.styleable.SuwItem_android_enabled, true);
        this.mIcon = typedArrayObtainStyledAttributes.getDrawable(R.styleable.SuwItem_android_icon);
        this.mTitle = typedArrayObtainStyledAttributes.getText(R.styleable.SuwItem_android_title);
        this.mSummary = typedArrayObtainStyledAttributes.getText(R.styleable.SuwItem_android_summary);
        this.mLayoutRes = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwItem_android_layout, getDefaultLayoutResource());
        this.mVisible = typedArrayObtainStyledAttributes.getBoolean(R.styleable.SuwItem_android_visible, true);
        typedArrayObtainStyledAttributes.recycle();
    }

    protected int getDefaultLayoutResource() {
        return R.layout.suw_items_default;
    }

    @Override
    public int getCount() {
        return isVisible() ? 1 : 0;
    }

    @Override
    public boolean isEnabled() {
        return this.mEnabled;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    @Override
    public int getLayoutResource() {
        return this.mLayoutRes;
    }

    public CharSequence getSummary() {
        return this.mSummary;
    }

    public CharSequence getTitle() {
        return this.mTitle;
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    @Override
    public int getViewId() {
        return getId();
    }

    public void onBindView(View view) {
        ((TextView) view.findViewById(R.id.suw_items_title)).setText(getTitle());
        TextView textView = (TextView) view.findViewById(R.id.suw_items_summary);
        CharSequence summary = getSummary();
        if (summary != null && summary.length() > 0) {
            textView.setText(summary);
            textView.setVisibility(0);
        } else {
            textView.setVisibility(8);
        }
        View viewFindViewById = view.findViewById(R.id.suw_items_icon_container);
        Drawable icon = getIcon();
        if (icon != null) {
            ImageView imageView = (ImageView) view.findViewById(R.id.suw_items_icon);
            imageView.setImageDrawable(null);
            onMergeIconStateAndLevels(imageView, icon);
            imageView.setImageDrawable(icon);
            viewFindViewById.setVisibility(0);
        } else {
            viewFindViewById.setVisibility(8);
        }
        view.setId(getViewId());
    }

    protected void onMergeIconStateAndLevels(ImageView imageView, Drawable drawable) {
        imageView.setImageState(drawable.getState(), false);
        imageView.setImageLevel(drawable.getLevel());
    }
}
