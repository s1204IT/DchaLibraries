package com.android.internal.view.menu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewDebug;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.view.menu.IconMenuView;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuView;

public final class IconMenuItemView extends TextView implements MenuView.ItemView {
    private static final int NO_ALPHA = 255;
    private static String sPrependShortcutLabel;
    private float mDisabledAlpha;
    private Drawable mIcon;
    private IconMenuView mIconMenuView;
    private MenuItemImpl mItemData;
    private MenuBuilder.ItemInvoker mItemInvoker;
    private Rect mPositionIconAvailable;
    private Rect mPositionIconOutput;
    private String mShortcutCaption;
    private boolean mShortcutCaptionMode;
    private int mTextAppearance;
    private Context mTextAppearanceContext;

    public IconMenuItemView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mPositionIconAvailable = new Rect();
        this.mPositionIconOutput = new Rect();
        if (sPrependShortcutLabel == null) {
            sPrependShortcutLabel = getResources().getString(R.string.prepend_shortcut_label);
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.MenuView, i, i2);
        this.mDisabledAlpha = typedArrayObtainStyledAttributes.getFloat(6, 0.8f);
        this.mTextAppearance = typedArrayObtainStyledAttributes.getResourceId(1, -1);
        this.mTextAppearanceContext = context;
        typedArrayObtainStyledAttributes.recycle();
    }

    public IconMenuItemView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public IconMenuItemView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    void initialize(CharSequence charSequence, Drawable drawable) {
        setClickable(true);
        setFocusable(true);
        if (this.mTextAppearance != -1) {
            setTextAppearance(this.mTextAppearanceContext, this.mTextAppearance);
        }
        setTitle(charSequence);
        setIcon(drawable);
        if (this.mItemData != null) {
            CharSequence contentDescription = this.mItemData.getContentDescription();
            if (TextUtils.isEmpty(contentDescription)) {
                setContentDescription(charSequence);
            } else {
                setContentDescription(contentDescription);
            }
            setTooltipText(this.mItemData.getTooltipText());
        }
    }

    @Override
    public void initialize(MenuItemImpl menuItemImpl, int i) {
        this.mItemData = menuItemImpl;
        initialize(menuItemImpl.getTitleForItemView(this), menuItemImpl.getIcon());
        setVisibility(menuItemImpl.isVisible() ? 0 : 8);
        setEnabled(menuItemImpl.isEnabled());
    }

    public void setItemData(MenuItemImpl menuItemImpl) {
        this.mItemData = menuItemImpl;
    }

    @Override
    public boolean performClick() {
        if (super.performClick()) {
            return true;
        }
        if (this.mItemInvoker == null || !this.mItemInvoker.invokeItem(this.mItemData)) {
            return false;
        }
        playSoundEffect(0);
        return true;
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        if (this.mShortcutCaptionMode) {
            setCaptionMode(true);
        } else if (charSequence != null) {
            setText(charSequence);
        }
    }

    void setCaptionMode(boolean z) {
        if (this.mItemData == null) {
            return;
        }
        this.mShortcutCaptionMode = z && this.mItemData.shouldShowShortcut();
        CharSequence titleForItemView = this.mItemData.getTitleForItemView(this);
        if (this.mShortcutCaptionMode) {
            if (this.mShortcutCaption == null) {
                this.mShortcutCaption = this.mItemData.getShortcutLabel();
            }
            titleForItemView = this.mShortcutCaption;
        }
        setText(titleForItemView);
    }

    @Override
    public void setIcon(Drawable drawable) {
        this.mIcon = drawable;
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            setCompoundDrawables(null, drawable, null, null);
            setGravity(81);
            requestLayout();
            return;
        }
        setCompoundDrawables(null, null, null, null);
        setGravity(17);
    }

    public void setItemInvoker(MenuBuilder.ItemInvoker itemInvoker) {
        this.mItemInvoker = itemInvoker;
    }

    @Override
    @ViewDebug.CapturedViewProperty(retrieveReturn = true)
    public MenuItemImpl getItemData() {
        return this.mItemData;
    }

    @Override
    public void setVisibility(int i) {
        super.setVisibility(i);
        if (this.mIconMenuView != null) {
            this.mIconMenuView.markStaleChildren();
        }
    }

    void setIconMenuView(IconMenuView iconMenuView) {
        this.mIconMenuView = iconMenuView;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mItemData != null && this.mIcon != null) {
            this.mIcon.setAlpha(!this.mItemData.isEnabled() && (isPressed() || !isFocused()) ? (int) (this.mDisabledAlpha * 255.0f) : 255);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        positionIcon();
    }

    @Override
    protected void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        super.onTextChanged(charSequence, i, i2, i3);
        setLayoutParams(getTextAppropriateLayoutParams());
    }

    IconMenuView.LayoutParams getTextAppropriateLayoutParams() {
        IconMenuView.LayoutParams layoutParams = (IconMenuView.LayoutParams) getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new IconMenuView.LayoutParams(-1, -1);
        }
        layoutParams.desiredWidth = (int) Layout.getDesiredWidth(getText(), 0, getText().length(), getPaint(), getTextDirectionHeuristic());
        return layoutParams;
    }

    private void positionIcon() {
        if (this.mIcon == null) {
            return;
        }
        Rect rect = this.mPositionIconOutput;
        getLineBounds(0, rect);
        this.mPositionIconAvailable.set(0, 0, getWidth(), rect.top);
        Gravity.apply(8388627, this.mIcon.getIntrinsicWidth(), this.mIcon.getIntrinsicHeight(), this.mPositionIconAvailable, this.mPositionIconOutput, getLayoutDirection());
        this.mIcon.setBounds(this.mPositionIconOutput);
    }

    @Override
    public void setCheckable(boolean z) {
    }

    @Override
    public void setChecked(boolean z) {
    }

    @Override
    public void setShortcut(boolean z, char c) {
        if (this.mShortcutCaptionMode) {
            this.mShortcutCaption = null;
            setCaptionMode(true);
        }
    }

    @Override
    public boolean prefersCondensedTitle() {
        return true;
    }

    @Override
    public boolean showsIcon() {
        return true;
    }
}
