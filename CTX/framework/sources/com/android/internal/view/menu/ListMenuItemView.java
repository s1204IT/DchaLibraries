package com.android.internal.view.menu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.view.menu.MenuView;

public class ListMenuItemView extends LinearLayout implements MenuView.ItemView, AbsListView.SelectionBoundsAdjuster {
    private static final String TAG = "ListMenuItemView";
    private Drawable mBackground;
    private CheckBox mCheckBox;
    private LinearLayout mContent;
    private boolean mForceShowIcon;
    private ImageView mGroupDivider;
    private boolean mHasListDivider;
    private ImageView mIconView;
    private LayoutInflater mInflater;
    private MenuItemImpl mItemData;
    private int mMenuType;
    private boolean mPreserveIconSpacing;
    private RadioButton mRadioButton;
    private TextView mShortcutView;
    private Drawable mSubMenuArrow;
    private ImageView mSubMenuArrowView;
    private int mTextAppearance;
    private Context mTextAppearanceContext;
    private TextView mTitleView;

    public ListMenuItemView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.MenuView, i, i2);
        this.mBackground = typedArrayObtainStyledAttributes.getDrawable(5);
        this.mTextAppearance = typedArrayObtainStyledAttributes.getResourceId(1, -1);
        this.mPreserveIconSpacing = typedArrayObtainStyledAttributes.getBoolean(8, false);
        this.mTextAppearanceContext = context;
        this.mSubMenuArrow = typedArrayObtainStyledAttributes.getDrawable(7);
        TypedArray typedArrayObtainStyledAttributes2 = context.getTheme().obtainStyledAttributes(null, new int[]{16843049}, 16842861, 0);
        this.mHasListDivider = typedArrayObtainStyledAttributes2.hasValue(0);
        typedArrayObtainStyledAttributes.recycle();
        typedArrayObtainStyledAttributes2.recycle();
    }

    public ListMenuItemView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ListMenuItemView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16844018);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setBackgroundDrawable(this.mBackground);
        this.mTitleView = (TextView) findViewById(16908310);
        if (this.mTextAppearance != -1) {
            this.mTitleView.setTextAppearance(this.mTextAppearanceContext, this.mTextAppearance);
        }
        this.mShortcutView = (TextView) findViewById(R.id.shortcut);
        this.mSubMenuArrowView = (ImageView) findViewById(R.id.submenuarrow);
        if (this.mSubMenuArrowView != null) {
            this.mSubMenuArrowView.setImageDrawable(this.mSubMenuArrow);
        }
        this.mGroupDivider = (ImageView) findViewById(R.id.group_divider);
        this.mContent = (LinearLayout) findViewById(16908290);
    }

    @Override
    public void initialize(MenuItemImpl menuItemImpl, int i) {
        this.mItemData = menuItemImpl;
        this.mMenuType = i;
        setVisibility(menuItemImpl.isVisible() ? 0 : 8);
        setTitle(menuItemImpl.getTitleForItemView(this));
        setCheckable(menuItemImpl.isCheckable());
        setShortcut(menuItemImpl.shouldShowShortcut(), menuItemImpl.getShortcut());
        setIcon(menuItemImpl.getIcon());
        setEnabled(menuItemImpl.isEnabled());
        setSubMenuArrowVisible(menuItemImpl.hasSubMenu());
        setContentDescription(menuItemImpl.getContentDescription());
    }

    private void addContentView(View view) {
        addContentView(view, -1);
    }

    private void addContentView(View view, int i) {
        if (this.mContent != null) {
            this.mContent.addView(view, i);
        } else {
            addView(view, i);
        }
    }

    public void setForceShowIcon(boolean z) {
        this.mForceShowIcon = z;
        this.mPreserveIconSpacing = z;
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        if (charSequence != null) {
            this.mTitleView.setText(charSequence);
            if (this.mTitleView.getVisibility() != 0) {
                this.mTitleView.setVisibility(0);
                return;
            }
            return;
        }
        if (this.mTitleView.getVisibility() != 8) {
            this.mTitleView.setVisibility(8);
        }
    }

    @Override
    public MenuItemImpl getItemData() {
        return this.mItemData;
    }

    @Override
    public void setCheckable(boolean z) {
        CompoundButton compoundButton;
        View view;
        if (!z && this.mRadioButton == null && this.mCheckBox == null) {
            return;
        }
        if (this.mItemData.isExclusiveCheckable()) {
            if (this.mRadioButton == null) {
                insertRadioButton();
            }
            compoundButton = this.mRadioButton;
            view = this.mCheckBox;
        } else {
            if (this.mCheckBox == null) {
                insertCheckBox();
            }
            compoundButton = this.mCheckBox;
            view = this.mRadioButton;
        }
        if (z) {
            compoundButton.setChecked(this.mItemData.isChecked());
            int i = z ? 0 : 8;
            if (compoundButton.getVisibility() != i) {
                compoundButton.setVisibility(i);
            }
            if (view != null && view.getVisibility() != 8) {
                view.setVisibility(8);
                return;
            }
            return;
        }
        if (this.mCheckBox != null) {
            this.mCheckBox.setVisibility(8);
        }
        if (this.mRadioButton != null) {
            this.mRadioButton.setVisibility(8);
        }
    }

    @Override
    public void setChecked(boolean z) {
        Checkable checkable;
        if (this.mItemData.isExclusiveCheckable()) {
            if (this.mRadioButton == null) {
                insertRadioButton();
            }
            checkable = this.mRadioButton;
        } else {
            if (this.mCheckBox == null) {
                insertCheckBox();
            }
            checkable = this.mCheckBox;
        }
        checkable.setChecked(z);
    }

    private void setSubMenuArrowVisible(boolean z) {
        if (this.mSubMenuArrowView != null) {
            this.mSubMenuArrowView.setVisibility(z ? 0 : 8);
        }
    }

    @Override
    public void setShortcut(boolean z, char c) {
        int i = (z && this.mItemData.shouldShowShortcut()) ? 0 : 8;
        if (i == 0) {
            this.mShortcutView.setText(this.mItemData.getShortcutLabel());
        }
        if (this.mShortcutView.getVisibility() != i) {
            this.mShortcutView.setVisibility(i);
        }
    }

    @Override
    public void setIcon(Drawable drawable) {
        boolean z;
        if (this.mItemData.shouldShowIcon() || this.mForceShowIcon) {
            z = true;
        } else {
            z = false;
        }
        if (!z && !this.mPreserveIconSpacing) {
            return;
        }
        if (this.mIconView == null && drawable == null && !this.mPreserveIconSpacing) {
            return;
        }
        if (this.mIconView == null) {
            insertIconView();
        }
        if (drawable != null || this.mPreserveIconSpacing) {
            ImageView imageView = this.mIconView;
            if (!z) {
                drawable = null;
            }
            imageView.setImageDrawable(drawable);
            if (this.mIconView.getVisibility() != 0) {
                this.mIconView.setVisibility(0);
                return;
            }
            return;
        }
        this.mIconView.setVisibility(8);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        if (this.mIconView != null && this.mPreserveIconSpacing) {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) this.mIconView.getLayoutParams();
            if (layoutParams.height > 0 && layoutParams2.width <= 0) {
                layoutParams2.width = layoutParams.height;
            }
        }
        super.onMeasure(i, i2);
    }

    private void insertIconView() {
        this.mIconView = (ImageView) getInflater().inflate(R.layout.list_menu_item_icon, (ViewGroup) this, false);
        addContentView(this.mIconView, 0);
    }

    private void insertRadioButton() {
        this.mRadioButton = (RadioButton) getInflater().inflate(R.layout.list_menu_item_radio, (ViewGroup) this, false);
        addContentView(this.mRadioButton);
    }

    private void insertCheckBox() {
        this.mCheckBox = (CheckBox) getInflater().inflate(R.layout.list_menu_item_checkbox, (ViewGroup) this, false);
        addContentView(this.mCheckBox);
    }

    @Override
    public boolean prefersCondensedTitle() {
        return false;
    }

    @Override
    public boolean showsIcon() {
        return this.mForceShowIcon;
    }

    private LayoutInflater getInflater() {
        if (this.mInflater == null) {
            this.mInflater = LayoutInflater.from(this.mContext);
        }
        return this.mInflater;
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        if (this.mItemData != null && this.mItemData.hasSubMenu()) {
            accessibilityNodeInfo.setCanOpenPopup(true);
        }
    }

    public void setGroupDividerEnabled(boolean z) {
        if (this.mGroupDivider != null) {
            this.mGroupDivider.setVisibility((this.mHasListDivider || !z) ? 8 : 0);
        }
    }

    @Override
    public void adjustListItemSelectionBounds(Rect rect) {
        if (this.mGroupDivider != null && this.mGroupDivider.getVisibility() == 0) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mGroupDivider.getLayoutParams();
            rect.top += this.mGroupDivider.getHeight() + layoutParams.topMargin + layoutParams.bottomMargin;
        }
    }
}
