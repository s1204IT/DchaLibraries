package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewHierarchyEncoder;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import com.android.internal.R;

public abstract class CompoundButton extends Button implements Checkable {
    private boolean mBroadcasting;
    private Drawable mButtonDrawable;
    private ColorStateList mButtonTintList;
    private PorterDuff.Mode mButtonTintMode;
    private boolean mChecked;
    private boolean mCheckedFromResource;
    private boolean mHasButtonTint;
    private boolean mHasButtonTintMode;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeWidgetListener;
    private static final String LOG_TAG = CompoundButton.class.getSimpleName();
    private static final int[] CHECKED_STATE_SET = {16842912};

    public interface OnCheckedChangeListener {
        void onCheckedChanged(CompoundButton compoundButton, boolean z);
    }

    public CompoundButton(Context context) {
        this(context, null);
    }

    public CompoundButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CompoundButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public CompoundButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mButtonTintList = null;
        this.mButtonTintMode = null;
        this.mHasButtonTint = false;
        this.mHasButtonTintMode = false;
        this.mCheckedFromResource = false;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CompoundButton, i, i2);
        Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(1);
        if (drawable != null) {
            setButtonDrawable(drawable);
        }
        if (typedArrayObtainStyledAttributes.hasValue(3)) {
            this.mButtonTintMode = Drawable.parseTintMode(typedArrayObtainStyledAttributes.getInt(3, -1), this.mButtonTintMode);
            this.mHasButtonTintMode = true;
        }
        if (typedArrayObtainStyledAttributes.hasValue(2)) {
            this.mButtonTintList = typedArrayObtainStyledAttributes.getColorStateList(2);
            this.mHasButtonTint = true;
        }
        setChecked(typedArrayObtainStyledAttributes.getBoolean(0, false));
        this.mCheckedFromResource = true;
        typedArrayObtainStyledAttributes.recycle();
        applyButtonTint();
    }

    @Override
    public void toggle() {
        setChecked(!this.mChecked);
    }

    @Override
    public boolean performClick() {
        toggle();
        boolean zPerformClick = super.performClick();
        if (!zPerformClick) {
            playSoundEffect(0);
        }
        return zPerformClick;
    }

    @Override
    @ViewDebug.ExportedProperty
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public void setChecked(boolean z) {
        if (this.mChecked != z) {
            this.mCheckedFromResource = false;
            this.mChecked = z;
            refreshDrawableState();
            notifyViewAccessibilityStateChangedIfNeeded(0);
            if (this.mBroadcasting) {
                return;
            }
            this.mBroadcasting = true;
            if (this.mOnCheckedChangeListener != null) {
                this.mOnCheckedChangeListener.onCheckedChanged(this, this.mChecked);
            }
            if (this.mOnCheckedChangeWidgetListener != null) {
                this.mOnCheckedChangeWidgetListener.onCheckedChanged(this, this.mChecked);
            }
            AutofillManager autofillManager = (AutofillManager) this.mContext.getSystemService(AutofillManager.class);
            if (autofillManager != null) {
                autofillManager.notifyValueChanged(this);
            }
            this.mBroadcasting = false;
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.mOnCheckedChangeListener = onCheckedChangeListener;
    }

    void setOnCheckedChangeWidgetListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.mOnCheckedChangeWidgetListener = onCheckedChangeListener;
    }

    public void setButtonDrawable(int i) {
        Drawable drawable;
        if (i != 0) {
            drawable = getContext().getDrawable(i);
        } else {
            drawable = null;
        }
        setButtonDrawable(drawable);
    }

    public void setButtonDrawable(Drawable drawable) {
        if (this.mButtonDrawable != drawable) {
            if (this.mButtonDrawable != null) {
                this.mButtonDrawable.setCallback(null);
                unscheduleDrawable(this.mButtonDrawable);
            }
            this.mButtonDrawable = drawable;
            if (drawable != null) {
                drawable.setCallback(this);
                drawable.setLayoutDirection(getLayoutDirection());
                if (drawable.isStateful()) {
                    drawable.setState(getDrawableState());
                }
                drawable.setVisible(getVisibility() == 0, false);
                setMinHeight(drawable.getIntrinsicHeight());
                applyButtonTint();
            }
        }
    }

    @Override
    public void onResolveDrawables(int i) {
        super.onResolveDrawables(i);
        if (this.mButtonDrawable != null) {
            this.mButtonDrawable.setLayoutDirection(i);
        }
    }

    public Drawable getButtonDrawable() {
        return this.mButtonDrawable;
    }

    public void setButtonTintList(ColorStateList colorStateList) {
        this.mButtonTintList = colorStateList;
        this.mHasButtonTint = true;
        applyButtonTint();
    }

    public ColorStateList getButtonTintList() {
        return this.mButtonTintList;
    }

    public void setButtonTintMode(PorterDuff.Mode mode) {
        this.mButtonTintMode = mode;
        this.mHasButtonTintMode = true;
        applyButtonTint();
    }

    public PorterDuff.Mode getButtonTintMode() {
        return this.mButtonTintMode;
    }

    private void applyButtonTint() {
        if (this.mButtonDrawable != null) {
            if (this.mHasButtonTint || this.mHasButtonTintMode) {
                this.mButtonDrawable = this.mButtonDrawable.mutate();
                if (this.mHasButtonTint) {
                    this.mButtonDrawable.setTintList(this.mButtonTintList);
                }
                if (this.mHasButtonTintMode) {
                    this.mButtonDrawable.setTintMode(this.mButtonTintMode);
                }
                if (this.mButtonDrawable.isStateful()) {
                    this.mButtonDrawable.setState(getDrawableState());
                }
            }
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return CompoundButton.class.getName();
    }

    @Override
    public void onInitializeAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEventInternal(accessibilityEvent);
        accessibilityEvent.setChecked(this.mChecked);
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        accessibilityNodeInfo.setCheckable(true);
        accessibilityNodeInfo.setChecked(this.mChecked);
    }

    @Override
    public int getCompoundPaddingLeft() {
        Drawable drawable;
        int compoundPaddingLeft = super.getCompoundPaddingLeft();
        if (!isLayoutRtl() && (drawable = this.mButtonDrawable) != null) {
            return compoundPaddingLeft + drawable.getIntrinsicWidth();
        }
        return compoundPaddingLeft;
    }

    @Override
    public int getCompoundPaddingRight() {
        Drawable drawable;
        int compoundPaddingRight = super.getCompoundPaddingRight();
        if (isLayoutRtl() && (drawable = this.mButtonDrawable) != null) {
            return compoundPaddingRight + drawable.getIntrinsicWidth();
        }
        return compoundPaddingRight;
    }

    @Override
    public int getHorizontalOffsetForDrawables() {
        Drawable drawable = this.mButtonDrawable;
        if (drawable != null) {
            return drawable.getIntrinsicWidth();
        }
        return 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int height;
        Drawable drawable = this.mButtonDrawable;
        if (drawable != null) {
            int gravity = getGravity() & 112;
            int intrinsicHeight = drawable.getIntrinsicHeight();
            int intrinsicWidth = drawable.getIntrinsicWidth();
            if (gravity == 16) {
                height = (getHeight() - intrinsicHeight) / 2;
            } else if (gravity == 80) {
                height = getHeight() - intrinsicHeight;
            } else {
                height = 0;
            }
            int i = intrinsicHeight + height;
            int width = isLayoutRtl() ? getWidth() - intrinsicWidth : 0;
            if (isLayoutRtl()) {
                intrinsicWidth = getWidth();
            }
            drawable.setBounds(width, height, intrinsicWidth, i);
            Drawable background = getBackground();
            if (background != null) {
                background.setHotspotBounds(width, height, intrinsicWidth, i);
            }
        }
        super.onDraw(canvas);
        if (drawable != null) {
            int i2 = this.mScrollX;
            int i3 = this.mScrollY;
            if (i2 == 0 && i3 == 0) {
                drawable.draw(canvas);
                return;
            }
            canvas.translate(i2, i3);
            drawable.draw(canvas);
            canvas.translate(-i2, -i3);
        }
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(i + 1);
        if (isChecked()) {
            mergeDrawableStates(iArrOnCreateDrawableState, CHECKED_STATE_SET);
        }
        return iArrOnCreateDrawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable drawable = this.mButtonDrawable;
        if (drawable != null && drawable.isStateful() && drawable.setState(getDrawableState())) {
            invalidateDrawable(drawable);
        }
    }

    @Override
    public void drawableHotspotChanged(float f, float f2) {
        super.drawableHotspotChanged(f, f2);
        if (this.mButtonDrawable != null) {
            this.mButtonDrawable.setHotspot(f, f2);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.mButtonDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mButtonDrawable != null) {
            this.mButtonDrawable.jumpToCurrentState();
        }
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        boolean checked;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.checked = ((Boolean) parcel.readValue(null)).booleanValue();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeValue(Boolean.valueOf(this.checked));
        }

        public String toString() {
            return "CompoundButton.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " checked=" + this.checked + "}";
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.checked = isChecked();
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        setChecked(savedState.checked);
        requestLayout();
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.addProperty("checked", isChecked());
    }

    @Override
    public void onProvideAutofillStructure(ViewStructure viewStructure, int i) {
        super.onProvideAutofillStructure(viewStructure, i);
        viewStructure.setDataIsSensitive(!this.mCheckedFromResource);
    }

    @Override
    public void autofill(AutofillValue autofillValue) {
        if (isEnabled()) {
            if (!autofillValue.isToggle()) {
                Log.w(LOG_TAG, autofillValue + " could not be autofilled into " + this);
                return;
            }
            setChecked(autofillValue.getToggleValue());
        }
    }

    @Override
    public int getAutofillType() {
        return isEnabled() ? 2 : 0;
    }

    @Override
    public AutofillValue getAutofillValue() {
        if (isEnabled()) {
            return AutofillValue.forToggle(isChecked());
        }
        return null;
    }
}
