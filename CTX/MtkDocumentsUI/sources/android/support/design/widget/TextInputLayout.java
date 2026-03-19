package android.support.design.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.animation.AnimationUtils;
import android.support.design.internal.ThemeEnforcement;
import android.support.design.internal.ViewUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.TintTypedArray;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextInputLayout extends LinearLayout {
    private ValueAnimator animator;
    private GradientDrawable boxBackground;
    private int boxBackgroundColor;
    private int boxBackgroundMode;
    private final int boxBottomOffsetPx;
    private int boxCollapsedPaddingBottomPx;
    private int boxCollapsedPaddingTopPx;
    private float boxCornerRadiusBottomLeft;
    private float boxCornerRadiusBottomRight;
    private float boxCornerRadiusTopLeft;
    private float boxCornerRadiusTopRight;
    private int boxExpandedPaddingBottomPx;
    private int boxExpandedPaddingTopPx;
    private final int boxLabelCutoutPaddingPx;
    private int boxPaddingLeftPx;
    private int boxPaddingRightPx;
    private int boxStrokeColor;
    private final int boxStrokeWidthDefaultPx;
    private final int boxStrokeWidthFocusedPx;
    private int boxStrokeWidthPx;
    final CollapsingTextHelper collapsingTextHelper;
    boolean counterEnabled;
    private int counterMaxLength;
    private final int counterOverflowTextAppearance;
    private boolean counterOverflowed;
    private final int counterTextAppearance;
    private TextView counterView;
    private int defaultBoxBackgroundColor;
    private final int defaultStrokeColor;
    private ColorStateList defaultTextColor;
    private final int disabledColor;
    EditText editText;
    private Drawable editTextOriginalDrawable;
    private int focusedStrokeColor;
    private ColorStateList focusedTextColor;
    private boolean hasPasswordToggleTintList;
    private boolean hasPasswordToggleTintMode;
    private boolean hasReconstructedEditTextBackground;
    private CharSequence hint;
    private boolean hintAnimationEnabled;
    private boolean hintEnabled;
    private boolean hintExpanded;
    private final int hoveredStrokeColor;
    private boolean inDrawableStateChanged;
    private final IndicatorViewController indicatorViewController;
    private final FrameLayout inputFrame;
    private Drawable originalEditTextEndDrawable;
    private CharSequence originalHint;
    private CharSequence passwordToggleContentDesc;
    private Drawable passwordToggleDrawable;
    private Drawable passwordToggleDummyDrawable;
    private boolean passwordToggleEnabled;
    private ColorStateList passwordToggleTintList;
    private PorterDuff.Mode passwordToggleTintMode;
    private CheckableImageButton passwordToggleView;
    private boolean passwordToggledVisible;
    private boolean restoringSavedState;
    private final Rect tmpRect;
    private final RectF tmpRectF;
    private Typeface typeface;

    public TextInputLayout(Context context) {
        this(context, null);
    }

    public TextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.textInputStyle);
    }

    public TextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.indicatorViewController = new IndicatorViewController(this);
        this.tmpRect = new Rect();
        this.tmpRectF = new RectF();
        this.collapsingTextHelper = new CollapsingTextHelper(this);
        setOrientation(1);
        setWillNotDraw(false);
        setAddStatesFromChildren(true);
        this.inputFrame = new FrameLayout(context);
        this.inputFrame.setAddStatesFromChildren(true);
        addView(this.inputFrame);
        this.collapsingTextHelper.setTextSizeInterpolator(AnimationUtils.LINEAR_INTERPOLATOR);
        this.collapsingTextHelper.setPositionInterpolator(AnimationUtils.LINEAR_INTERPOLATOR);
        this.collapsingTextHelper.setCollapsedTextGravity(8388659);
        TintTypedArray a = ThemeEnforcement.obtainTintedStyledAttributes(context, attrs, R.styleable.TextInputLayout, defStyleAttr, R.style.Widget_Design_TextInputLayout);
        this.hintEnabled = a.getBoolean(R.styleable.TextInputLayout_hintEnabled, true);
        setHint(a.getText(R.styleable.TextInputLayout_android_hint));
        this.hintAnimationEnabled = a.getBoolean(R.styleable.TextInputLayout_hintAnimationEnabled, true);
        this.boxPaddingLeftPx = a.getDimensionPixelOffset(R.styleable.TextInputLayout_boxPaddingLeft, 0);
        this.boxCollapsedPaddingTopPx = a.getDimensionPixelOffset(R.styleable.TextInputLayout_boxCollapsedPaddingTop, 0);
        this.boxCollapsedPaddingBottomPx = a.getDimensionPixelOffset(R.styleable.TextInputLayout_boxCollapsedPaddingBottom, 0);
        this.boxExpandedPaddingTopPx = a.getDimensionPixelOffset(R.styleable.TextInputLayout_boxExpandedPaddingTop, 0);
        this.boxPaddingRightPx = a.getDimensionPixelOffset(R.styleable.TextInputLayout_boxPaddingRight, 0);
        this.boxExpandedPaddingBottomPx = a.getDimensionPixelOffset(R.styleable.TextInputLayout_boxExpandedPaddingBottom, 0);
        this.boxBottomOffsetPx = context.getResources().getDimensionPixelOffset(R.dimen.mtrl_textinput_box_bottom_offset);
        this.boxLabelCutoutPaddingPx = context.getResources().getDimensionPixelOffset(R.dimen.mtrl_textinput_box_label_cutout_padding);
        this.boxCornerRadiusTopLeft = a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusTopLeft, 0.0f);
        this.boxCornerRadiusTopRight = a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusTopRight, 0.0f);
        this.boxCornerRadiusBottomRight = a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusBottomRight, 0.0f);
        this.boxCornerRadiusBottomLeft = a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusBottomLeft, 0.0f);
        this.defaultBoxBackgroundColor = a.getColor(R.styleable.TextInputLayout_boxBackgroundColor, 0);
        this.boxBackgroundColor = this.defaultBoxBackgroundColor;
        this.focusedStrokeColor = a.getColor(R.styleable.TextInputLayout_boxStrokeColor, 0);
        this.boxStrokeWidthDefaultPx = context.getResources().getDimensionPixelSize(R.dimen.mtrl_textinput_box_stroke_width_default);
        this.boxStrokeWidthFocusedPx = context.getResources().getDimensionPixelSize(R.dimen.mtrl_textinput_box_stroke_width_focused);
        this.boxStrokeWidthPx = this.boxStrokeWidthDefaultPx;
        int boxBackgroundMode = a.getInt(R.styleable.TextInputLayout_boxBackgroundMode, 0);
        setBoxBackgroundMode(boxBackgroundMode);
        if (a.hasValue(R.styleable.TextInputLayout_android_textColorHint)) {
            ColorStateList colorStateList = a.getColorStateList(R.styleable.TextInputLayout_android_textColorHint);
            this.focusedTextColor = colorStateList;
            this.defaultTextColor = colorStateList;
        }
        this.defaultStrokeColor = ContextCompat.getColor(context, R.color.mtrl_textinput_default_box_stroke_color);
        this.disabledColor = ContextCompat.getColor(context, R.color.mtrl_textinput_disabled_color);
        this.hoveredStrokeColor = ContextCompat.getColor(context, R.color.mtrl_textinput_hovered_box_stroke_color);
        int hintAppearance = a.getResourceId(R.styleable.TextInputLayout_hintTextAppearance, -1);
        if (hintAppearance != -1) {
            setHintTextAppearance(a.getResourceId(R.styleable.TextInputLayout_hintTextAppearance, 0));
        }
        int mErrorTextAppearance = a.getResourceId(R.styleable.TextInputLayout_errorTextAppearance, 0);
        boolean errorEnabled = a.getBoolean(R.styleable.TextInputLayout_errorEnabled, false);
        int mHelperTextTextAppearance = a.getResourceId(R.styleable.TextInputLayout_helperTextTextAppearance, 0);
        boolean helperTextEnabled = a.getBoolean(R.styleable.TextInputLayout_helperTextEnabled, false);
        CharSequence helperText = a.getText(R.styleable.TextInputLayout_helperText);
        boolean counterEnabled = a.getBoolean(R.styleable.TextInputLayout_counterEnabled, false);
        setCounterMaxLength(a.getInt(R.styleable.TextInputLayout_counterMaxLength, -1));
        this.counterTextAppearance = a.getResourceId(R.styleable.TextInputLayout_counterTextAppearance, 0);
        this.counterOverflowTextAppearance = a.getResourceId(R.styleable.TextInputLayout_counterOverflowTextAppearance, 0);
        this.passwordToggleEnabled = a.getBoolean(R.styleable.TextInputLayout_passwordToggleEnabled, false);
        this.passwordToggleDrawable = a.getDrawable(R.styleable.TextInputLayout_passwordToggleDrawable);
        this.passwordToggleContentDesc = a.getText(R.styleable.TextInputLayout_passwordToggleContentDescription);
        if (a.hasValue(R.styleable.TextInputLayout_passwordToggleTint)) {
            this.hasPasswordToggleTintList = true;
            this.passwordToggleTintList = a.getColorStateList(R.styleable.TextInputLayout_passwordToggleTint);
        }
        if (a.hasValue(R.styleable.TextInputLayout_passwordToggleTintMode)) {
            this.hasPasswordToggleTintMode = true;
            this.passwordToggleTintMode = ViewUtils.parseTintMode(a.getInt(R.styleable.TextInputLayout_passwordToggleTintMode, -1), null);
        }
        a.recycle();
        setHelperTextEnabled(helperTextEnabled);
        setHelperText(helperText);
        setHelperTextTextAppearance(mHelperTextTextAppearance);
        setErrorEnabled(errorEnabled);
        setErrorTextAppearance(mErrorTextAppearance);
        setCounterEnabled(counterEnabled);
        applyPasswordToggleTint();
        if (ViewCompat.getImportantForAccessibility(this) == 0) {
            ViewCompat.setImportantForAccessibility(this, 1);
        }
        ViewCompat.setAccessibilityDelegate(this, new TextInputAccessibilityDelegate());
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof EditText) {
            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(params);
            flp.gravity = 16 | (flp.gravity & (-113));
            this.inputFrame.addView(child, flp);
            this.inputFrame.setLayoutParams(params);
            updateInputLayoutMargins();
            setEditText((EditText) child);
            return;
        }
        super.addView(child, index, params);
    }

    private Drawable getBoxBackground() {
        if (this.boxBackgroundMode == 1 || this.boxBackgroundMode == 2) {
            return this.boxBackground;
        }
        throw new IllegalStateException();
    }

    public void setBoxBackgroundMode(int boxBackgroundMode) {
        if (boxBackgroundMode == this.boxBackgroundMode) {
            return;
        }
        this.boxBackgroundMode = boxBackgroundMode;
        onApplyBoxBackgroundMode();
    }

    private void onApplyBoxBackgroundMode() {
        assignBoxBackgroundByMode();
        if (this.boxBackgroundMode != 0) {
            updateInputLayoutMargins();
        }
        updateTextInputBoxBounds();
        applyEditTextBoxPadding();
    }

    private void assignBoxBackgroundByMode() {
        if (this.boxBackgroundMode == 0) {
            this.boxBackground = null;
            return;
        }
        if (this.boxBackgroundMode == 2 && this.hintEnabled && !(this.boxBackground instanceof CutoutDrawable)) {
            this.boxBackground = new CutoutDrawable();
        } else if (!(this.boxBackground instanceof GradientDrawable)) {
            this.boxBackground = new GradientDrawable();
        }
    }

    private void applyEditTextBoxPadding() {
        if (this.boxBackgroundMode != 0 && this.boxBackgroundMode != 0 && this.editText != null) {
            this.editText.setPadding(this.boxPaddingLeftPx, this.boxExpandedPaddingTopPx, this.boxPaddingRightPx, this.boxExpandedPaddingBottomPx);
        }
    }

    private float[] getCornerRadiiAsArray() {
        float[] cornerRadii = {this.boxCornerRadiusTopLeft, this.boxCornerRadiusTopLeft, this.boxCornerRadiusTopRight, this.boxCornerRadiusTopRight, this.boxCornerRadiusBottomRight, this.boxCornerRadiusBottomRight, this.boxCornerRadiusBottomLeft, this.boxCornerRadiusBottomLeft};
        return cornerRadii;
    }

    @Override
    public void dispatchProvideAutofillStructure(ViewStructure structure, int flags) {
        if (this.originalHint == null || this.editText == null) {
            super.dispatchProvideAutofillStructure(structure, flags);
            return;
        }
        CharSequence hint = this.editText.getHint();
        this.editText.setHint(this.originalHint);
        try {
            super.dispatchProvideAutofillStructure(structure, flags);
        } finally {
            this.editText.setHint(hint);
        }
    }

    private void setEditText(EditText editText) {
        if (this.editText != null) {
            throw new IllegalArgumentException("We already have an EditText, can only have one");
        }
        if (!(editText instanceof TextInputEditText)) {
            Log.i("TextInputLayout", "EditText added is not a TextInputEditText. Please switch to using that class instead.");
        }
        this.editText = editText;
        onApplyBoxBackgroundMode();
        boolean hasPasswordTransformation = hasPasswordTransformation();
        if (!hasPasswordTransformation) {
            this.collapsingTextHelper.setTypefaces(this.editText.getTypeface());
        }
        this.collapsingTextHelper.setExpandedTextSize(this.editText.getTextSize());
        int editTextGravity = this.editText.getGravity();
        this.collapsingTextHelper.setCollapsedTextGravity(48 | (editTextGravity & (-113)));
        this.collapsingTextHelper.setExpandedTextGravity(editTextGravity);
        this.editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                TextInputLayout.this.updateLabelState(!TextInputLayout.this.restoringSavedState);
                if (TextInputLayout.this.counterEnabled) {
                    TextInputLayout.this.updateCounter(s.length());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        if (this.defaultTextColor == null) {
            this.defaultTextColor = this.editText.getHintTextColors();
        }
        if (this.hintEnabled && TextUtils.isEmpty(this.hint)) {
            this.originalHint = this.editText.getHint();
            setHint(this.originalHint);
            setHint(this.editText.getHint());
            this.editText.setHint((CharSequence) null);
        }
        if (this.counterView != null) {
            updateCounter(this.editText.getText().length());
        }
        this.indicatorViewController.adjustIndicatorPadding();
        updatePasswordToggleView();
        updateLabelState(false, true);
    }

    private void updateInputLayoutMargins() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.inputFrame.getLayoutParams();
        int newTopMargin = calculateLabelMarginTop();
        if (newTopMargin != lp.topMargin) {
            lp.topMargin = newTopMargin;
            this.inputFrame.requestLayout();
        }
    }

    void updateLabelState(boolean animate) {
        updateLabelState(animate, false);
    }

    private void updateLabelState(boolean animate, boolean force) {
        boolean isEnabled = isEnabled();
        boolean hasFocus = false;
        boolean hasText = (this.editText == null || TextUtils.isEmpty(this.editText.getText())) ? false : true;
        if (this.editText != null && this.editText.hasFocus()) {
            hasFocus = true;
        }
        boolean errorShouldBeShown = this.indicatorViewController.errorShouldBeShown();
        if (this.defaultTextColor != null) {
            this.collapsingTextHelper.setCollapsedTextColor(this.defaultTextColor);
            this.collapsingTextHelper.setExpandedTextColor(this.defaultTextColor);
        }
        if (!isEnabled) {
            this.collapsingTextHelper.setCollapsedTextColor(ColorStateList.valueOf(this.disabledColor));
            this.collapsingTextHelper.setExpandedTextColor(ColorStateList.valueOf(this.disabledColor));
        } else if (errorShouldBeShown) {
            this.collapsingTextHelper.setCollapsedTextColor(this.indicatorViewController.getErrorViewTextColors());
        } else if (this.counterOverflowed && this.counterView != null) {
            this.collapsingTextHelper.setCollapsedTextColor(this.counterView.getTextColors());
        } else if (hasFocus && this.focusedTextColor != null) {
            this.collapsingTextHelper.setCollapsedTextColor(this.focusedTextColor);
        }
        if (hasText || (isEnabled() && (hasFocus || errorShouldBeShown))) {
            if (force || this.hintExpanded) {
                collapseHint(animate);
                return;
            }
            return;
        }
        if (force || !this.hintExpanded) {
            expandHint(animate);
        }
    }

    public EditText getEditText() {
        return this.editText;
    }

    public void setHint(CharSequence hint) {
        if (this.hintEnabled) {
            setHintInternal(hint);
            sendAccessibilityEvent(2048);
        }
    }

    private void setHintInternal(CharSequence hint) {
        if (!TextUtils.equals(hint, this.hint)) {
            this.hint = hint;
            this.collapsingTextHelper.setText(hint);
            if (!this.hintExpanded) {
                openCutout();
            }
        }
    }

    public CharSequence getHint() {
        if (this.hintEnabled) {
            return this.hint;
        }
        return null;
    }

    public void setHintTextAppearance(int resId) {
        this.collapsingTextHelper.setCollapsedTextAppearance(resId);
        this.focusedTextColor = this.collapsingTextHelper.getCollapsedTextColor();
        if (this.editText != null) {
            updateLabelState(false);
            updateInputLayoutMargins();
        }
    }

    public void setErrorEnabled(boolean enabled) {
        this.indicatorViewController.setErrorEnabled(enabled);
    }

    public void setErrorTextAppearance(int resId) {
        this.indicatorViewController.setErrorTextAppearance(resId);
    }

    public void setHelperTextTextAppearance(int resId) {
        this.indicatorViewController.setHelperTextAppearance(resId);
    }

    public void setHelperTextEnabled(boolean enabled) {
        this.indicatorViewController.setHelperTextEnabled(enabled);
    }

    public void setHelperText(CharSequence helperText) {
        if (TextUtils.isEmpty(helperText)) {
            if (isHelperTextEnabled()) {
                setHelperTextEnabled(false);
            }
        } else {
            if (!isHelperTextEnabled()) {
                setHelperTextEnabled(true);
            }
            this.indicatorViewController.showHelper(helperText);
        }
    }

    public boolean isHelperTextEnabled() {
        return this.indicatorViewController.isHelperTextEnabled();
    }

    public void setError(CharSequence errorText) {
        if (!this.indicatorViewController.isErrorEnabled()) {
            if (TextUtils.isEmpty(errorText)) {
                return;
            } else {
                setErrorEnabled(true);
            }
        }
        if (!TextUtils.isEmpty(errorText)) {
            this.indicatorViewController.showError(errorText);
        } else {
            this.indicatorViewController.hideError();
        }
    }

    public void setCounterEnabled(boolean enabled) {
        if (this.counterEnabled != enabled) {
            if (!enabled) {
                this.indicatorViewController.removeIndicator(this.counterView, 2);
                this.counterView = null;
            } else {
                this.counterView = new AppCompatTextView(getContext());
                this.counterView.setId(R.id.textinput_counter);
                if (this.typeface != null) {
                    this.counterView.setTypeface(this.typeface);
                }
                this.counterView.setMaxLines(1);
                setTextAppearanceCompatWithErrorFallback(this.counterView, this.counterTextAppearance);
                this.indicatorViewController.addIndicator(this.counterView, 2);
                if (this.editText == null) {
                    updateCounter(0);
                } else {
                    updateCounter(this.editText.getText().length());
                }
            }
            this.counterEnabled = enabled;
        }
    }

    public void setCounterMaxLength(int maxLength) {
        if (this.counterMaxLength != maxLength) {
            if (maxLength > 0) {
                this.counterMaxLength = maxLength;
            } else {
                this.counterMaxLength = -1;
            }
            if (this.counterEnabled) {
                updateCounter(this.editText == null ? 0 : this.editText.getText().length());
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        recursiveSetEnabled(this, enabled);
        super.setEnabled(enabled);
    }

    private static void recursiveSetEnabled(ViewGroup vg, boolean enabled) {
        int count = vg.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = vg.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup) {
                recursiveSetEnabled((ViewGroup) child, enabled);
            }
        }
    }

    void updateCounter(int length) {
        boolean wasCounterOverflowed = this.counterOverflowed;
        if (this.counterMaxLength == -1) {
            this.counterView.setText(String.valueOf(length));
            this.counterOverflowed = false;
        } else {
            this.counterOverflowed = length > this.counterMaxLength;
            if (wasCounterOverflowed != this.counterOverflowed) {
                setTextAppearanceCompatWithErrorFallback(this.counterView, this.counterOverflowed ? this.counterOverflowTextAppearance : this.counterTextAppearance);
            }
            this.counterView.setText(getContext().getString(R.string.character_counter_pattern, Integer.valueOf(length), Integer.valueOf(this.counterMaxLength)));
        }
        if (this.editText != null && wasCounterOverflowed != this.counterOverflowed) {
            updateLabelState(false);
            updateEditTextBackground();
        }
    }

    void setTextAppearanceCompatWithErrorFallback(TextView textView, int textAppearance) {
        boolean useDefaultColor = false;
        try {
            TextViewCompat.setTextAppearance(textView, textAppearance);
            if (Build.VERSION.SDK_INT >= 23) {
                if (textView.getTextColors().getDefaultColor() == -65281) {
                    useDefaultColor = true;
                }
            }
        } catch (Exception e) {
            useDefaultColor = true;
        }
        if (useDefaultColor) {
            TextViewCompat.setTextAppearance(textView, R.style.TextAppearance_AppCompat_Caption);
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.design_error));
        }
    }

    private void updateTextInputBoxBounds() {
        if (this.boxBackgroundMode == 0 || this.boxBackground == null || this.editText == null || getRight() == 0) {
            return;
        }
        int left = this.editText.getLeft();
        int top = calculateBoxBackgroundTop();
        int right = this.editText.getRight();
        int bottom = this.editText.getBottom() + this.boxBottomOffsetPx;
        if (this.boxBackgroundMode == 2) {
            left += this.boxStrokeWidthFocusedPx / 2;
            top -= this.boxStrokeWidthFocusedPx / 2;
            right -= this.boxStrokeWidthFocusedPx / 2;
            bottom += this.boxStrokeWidthFocusedPx / 2;
        }
        this.boxBackground.setBounds(left, top, right, bottom);
        applyBoxAttributes();
        updateEditTextBackgroundBounds();
    }

    private int calculateBoxBackgroundTop() {
        if (this.editText == null) {
            return 0;
        }
        switch (this.boxBackgroundMode) {
        }
        return 0;
    }

    private int calculateLabelMarginTop() {
        if (!this.hintEnabled) {
            return 0;
        }
        switch (this.boxBackgroundMode) {
        }
        return 0;
    }

    private int calculateCollapsedTextTopBounds() {
        switch (this.boxBackgroundMode) {
            case 1:
                return getBoxBackground().getBounds().top + this.boxCollapsedPaddingTopPx;
            case 2:
                return getBoxBackground().getBounds().top - calculateLabelMarginTop();
            default:
                return getPaddingTop();
        }
    }

    private void updateEditTextBackgroundBounds() {
        Drawable editTextBackground;
        if (this.editText == null || (editTextBackground = this.editText.getBackground()) == null) {
            return;
        }
        if (android.support.v7.widget.DrawableUtils.canSafelyMutateDrawable(editTextBackground)) {
            editTextBackground = editTextBackground.mutate();
        }
        Rect editTextBounds = new Rect();
        DescendantOffsetUtils.getDescendantRect(this, this.editText, editTextBounds);
        Rect editTextBackgroundBounds = editTextBackground.getBounds();
        if (editTextBackgroundBounds.left != editTextBackgroundBounds.right) {
            Rect editTextBackgroundPadding = new Rect();
            editTextBackground.getPadding(editTextBackgroundPadding);
            int left = editTextBackgroundBounds.left - editTextBackgroundPadding.left;
            int right = editTextBackgroundBounds.right + (editTextBackgroundPadding.right * 2);
            editTextBackground.setBounds(left, editTextBackgroundBounds.top, right, this.editText.getBottom());
        }
    }

    private void setBoxAttributes() {
        switch (this.boxBackgroundMode) {
            case 1:
                this.boxStrokeWidthPx = 0;
                break;
            case 2:
                if (this.focusedStrokeColor == 0) {
                    this.focusedStrokeColor = this.focusedTextColor.getColorForState(getDrawableState(), this.focusedTextColor.getDefaultColor());
                }
                break;
        }
    }

    private void applyBoxAttributes() {
        if (this.boxBackground == null) {
            return;
        }
        setBoxAttributes();
        if (this.editText != null && this.boxBackgroundMode == 2) {
            if (this.editText.getBackground() != null) {
                this.editTextOriginalDrawable = this.editText.getBackground();
            }
            ViewCompat.setBackground(this.editText, null);
        }
        if (this.editText != null && this.boxBackgroundMode == 1 && this.editTextOriginalDrawable != null) {
            ViewCompat.setBackground(this.editText, this.editTextOriginalDrawable);
        }
        if (this.boxStrokeWidthPx > -1 && this.boxStrokeColor != 0) {
            this.boxBackground.setStroke(this.boxStrokeWidthPx, this.boxStrokeColor);
        }
        this.boxBackground.setCornerRadii(getCornerRadiiAsArray());
        this.boxBackground.setColor(this.boxBackgroundColor);
        invalidate();
    }

    void updateEditTextBackground() {
        Drawable editTextBackground;
        if (this.editText == null || (editTextBackground = this.editText.getBackground()) == null) {
            return;
        }
        ensureBackgroundDrawableStateWorkaround();
        if (android.support.v7.widget.DrawableUtils.canSafelyMutateDrawable(editTextBackground)) {
            editTextBackground = editTextBackground.mutate();
        }
        if (this.indicatorViewController.errorShouldBeShown()) {
            editTextBackground.setColorFilter(AppCompatDrawableManager.getPorterDuffColorFilter(this.indicatorViewController.getErrorViewCurrentTextColor(), PorterDuff.Mode.SRC_IN));
        } else if (this.counterOverflowed && this.counterView != null) {
            editTextBackground.setColorFilter(AppCompatDrawableManager.getPorterDuffColorFilter(this.counterView.getCurrentTextColor(), PorterDuff.Mode.SRC_IN));
        } else {
            DrawableCompat.clearColorFilter(editTextBackground);
            this.editText.refreshDrawableState();
        }
    }

    private void ensureBackgroundDrawableStateWorkaround() {
        Drawable bg;
        int sdk = Build.VERSION.SDK_INT;
        if ((sdk == 21 || sdk == 22) && (bg = this.editText.getBackground()) != null && !this.hasReconstructedEditTextBackground) {
            Drawable newBg = bg.getConstantState().newDrawable();
            if (bg instanceof DrawableContainer) {
                this.hasReconstructedEditTextBackground = DrawableUtils.setContainerConstantState((DrawableContainer) bg, newBg.getConstantState());
            }
            if (!this.hasReconstructedEditTextBackground) {
                ViewCompat.setBackground(this.editText, newBg);
                this.hasReconstructedEditTextBackground = true;
                onApplyBoxBackgroundMode();
            }
        }
    }

    static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        CharSequence error;
        boolean isPasswordToggledVisible;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            this.error = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            this.isPasswordToggledVisible = source.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            TextUtils.writeToParcel(this.error, parcel, i);
            parcel.writeInt(this.isPasswordToggledVisible ? 1 : 0);
        }

        public String toString() {
            return "TextInputLayout.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " error=" + ((Object) this.error) + "}";
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        if (this.indicatorViewController.errorShouldBeShown()) {
            ss.error = getError();
        }
        ss.isPasswordToggledVisible = this.passwordToggledVisible;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setError(ss.error);
        if (ss.isPasswordToggledVisible) {
            passwordVisibilityToggleRequested(true);
        }
        requestLayout();
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        this.restoringSavedState = true;
        super.dispatchRestoreInstanceState(container);
        this.restoringSavedState = false;
    }

    public CharSequence getError() {
        if (this.indicatorViewController.isErrorEnabled()) {
            return this.indicatorViewController.getErrorText();
        }
        return null;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.hintEnabled) {
            this.collapsingTextHelper.draw(canvas);
        }
        if (this.boxBackground != null) {
            this.boxBackground.draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        updatePasswordToggleView();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void updatePasswordToggleView() {
        if (this.editText == null) {
            return;
        }
        if (shouldShowPasswordIcon()) {
            if (this.passwordToggleView == null) {
                this.passwordToggleView = (CheckableImageButton) LayoutInflater.from(getContext()).inflate(R.layout.design_text_input_password_icon, (ViewGroup) this.inputFrame, false);
                this.passwordToggleView.setImageDrawable(this.passwordToggleDrawable);
                this.passwordToggleView.setContentDescription(this.passwordToggleContentDesc);
                this.inputFrame.addView(this.passwordToggleView);
                this.passwordToggleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TextInputLayout.this.passwordVisibilityToggleRequested(false);
                    }
                });
            }
            if (this.editText != null && ViewCompat.getMinimumHeight(this.editText) <= 0) {
                this.editText.setMinimumHeight(ViewCompat.getMinimumHeight(this.passwordToggleView));
            }
            this.passwordToggleView.setVisibility(0);
            this.passwordToggleView.setChecked(this.passwordToggledVisible);
            if (this.passwordToggleDummyDrawable == null) {
                this.passwordToggleDummyDrawable = new ColorDrawable();
            }
            this.passwordToggleDummyDrawable.setBounds(0, 0, this.passwordToggleView.getMeasuredWidth(), 1);
            Drawable[] compounds = TextViewCompat.getCompoundDrawablesRelative(this.editText);
            if (compounds[2] != this.passwordToggleDummyDrawable) {
                this.originalEditTextEndDrawable = compounds[2];
            }
            TextViewCompat.setCompoundDrawablesRelative(this.editText, compounds[0], compounds[1], this.passwordToggleDummyDrawable, compounds[3]);
            this.passwordToggleView.setPadding(this.editText.getPaddingLeft(), this.editText.getPaddingTop(), this.editText.getPaddingRight(), this.editText.getPaddingBottom());
            return;
        }
        if (this.passwordToggleView != null && this.passwordToggleView.getVisibility() == 0) {
            this.passwordToggleView.setVisibility(8);
        }
        if (this.passwordToggleDummyDrawable != null) {
            Drawable[] compounds2 = TextViewCompat.getCompoundDrawablesRelative(this.editText);
            if (compounds2[2] == this.passwordToggleDummyDrawable) {
                TextViewCompat.setCompoundDrawablesRelative(this.editText, compounds2[0], compounds2[1], this.originalEditTextEndDrawable, compounds2[3]);
                this.passwordToggleDummyDrawable = null;
            }
        }
    }

    private void passwordVisibilityToggleRequested(boolean shouldSkipAnimations) {
        if (this.passwordToggleEnabled) {
            int selection = this.editText.getSelectionEnd();
            if (hasPasswordTransformation()) {
                this.editText.setTransformationMethod(null);
                this.passwordToggledVisible = true;
            } else {
                this.editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                this.passwordToggledVisible = false;
            }
            this.passwordToggleView.setChecked(this.passwordToggledVisible);
            if (shouldSkipAnimations) {
                this.passwordToggleView.jumpDrawablesToCurrentState();
            }
            this.editText.setSelection(selection);
        }
    }

    private boolean hasPasswordTransformation() {
        return this.editText != null && (this.editText.getTransformationMethod() instanceof PasswordTransformationMethod);
    }

    private boolean shouldShowPasswordIcon() {
        return this.passwordToggleEnabled && (hasPasswordTransformation() || this.passwordToggledVisible);
    }

    private void applyPasswordToggleTint() {
        if (this.passwordToggleDrawable != null) {
            if (this.hasPasswordToggleTintList || this.hasPasswordToggleTintMode) {
                this.passwordToggleDrawable = DrawableCompat.wrap(this.passwordToggleDrawable).mutate();
                if (this.hasPasswordToggleTintList) {
                    DrawableCompat.setTintList(this.passwordToggleDrawable, this.passwordToggleTintList);
                }
                if (this.hasPasswordToggleTintMode) {
                    DrawableCompat.setTintMode(this.passwordToggleDrawable, this.passwordToggleTintMode);
                }
                if (this.passwordToggleView != null && this.passwordToggleView.getDrawable() != this.passwordToggleDrawable) {
                    this.passwordToggleView.setImageDrawable(this.passwordToggleDrawable);
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (this.boxBackground != null) {
            updateTextInputBoxBounds();
        }
        if (this.hintEnabled && this.editText != null) {
            Rect rect = this.tmpRect;
            DescendantOffsetUtils.getDescendantRect(this, this.editText, rect);
            int l = rect.left + this.editText.getCompoundPaddingLeft();
            int r = rect.right - this.editText.getCompoundPaddingRight();
            int t = calculateCollapsedTextTopBounds();
            this.collapsingTextHelper.setExpandedBounds(l, rect.top + this.editText.getCompoundPaddingTop(), r, rect.bottom - this.editText.getCompoundPaddingBottom());
            this.collapsingTextHelper.setCollapsedBounds(l, t, r, (bottom - top) - getPaddingBottom());
            this.collapsingTextHelper.recalculate();
            if (cutoutEnabled() && !this.hintExpanded) {
                openCutout();
            }
        }
    }

    private void collapseHint(boolean animate) {
        if (this.animator != null && this.animator.isRunning()) {
            this.animator.cancel();
        }
        if (!animate || !this.hintAnimationEnabled) {
            this.collapsingTextHelper.setExpansionFraction(1.0f);
        } else {
            animateToExpansionFraction(1.0f);
        }
        this.hintExpanded = false;
        if (cutoutEnabled()) {
            openCutout();
        }
    }

    private boolean cutoutEnabled() {
        return this.hintEnabled && !TextUtils.isEmpty(this.hint) && (this.boxBackground instanceof CutoutDrawable);
    }

    private void openCutout() {
        if (!cutoutEnabled()) {
            return;
        }
        RectF cutoutBounds = this.tmpRectF;
        this.collapsingTextHelper.getCollapsedTextActualBounds(cutoutBounds);
        applyCutoutPadding(cutoutBounds);
        ((CutoutDrawable) this.boxBackground).setCutout(cutoutBounds);
    }

    private void closeCutout() {
        if (cutoutEnabled()) {
            ((CutoutDrawable) this.boxBackground).removeCutout();
        }
    }

    private void applyCutoutPadding(RectF cutoutBounds) {
        cutoutBounds.left -= this.boxLabelCutoutPaddingPx;
        cutoutBounds.top -= this.boxLabelCutoutPaddingPx;
        cutoutBounds.right += this.boxLabelCutoutPaddingPx;
        cutoutBounds.bottom += this.boxLabelCutoutPaddingPx;
    }

    boolean cutoutIsOpen() {
        return cutoutEnabled() && ((CutoutDrawable) this.boxBackground).hasCutout();
    }

    @Override
    protected void drawableStateChanged() {
        if (this.inDrawableStateChanged) {
            return;
        }
        this.inDrawableStateChanged = true;
        super.drawableStateChanged();
        int[] state = getDrawableState();
        updateLabelState(ViewCompat.isLaidOut(this) && isEnabled());
        updateEditTextBackground();
        updateTextInputBoxBounds();
        updateTextInputBoxState();
        boolean changed = this.collapsingTextHelper != null ? false | this.collapsingTextHelper.setState(state) : false;
        if (changed) {
            invalidate();
        }
        this.inDrawableStateChanged = false;
    }

    void updateTextInputBoxState() {
        if (this.boxBackground == null || this.boxBackgroundMode == 0) {
            return;
        }
        boolean isHovered = false;
        boolean hasFocus = this.editText != null && this.editText.hasFocus();
        if (this.editText != null && this.editText.isHovered()) {
            isHovered = true;
        }
        if (this.boxBackgroundMode == 2) {
            if (!isEnabled()) {
                this.boxStrokeColor = this.disabledColor;
            } else if (this.indicatorViewController.errorShouldBeShown()) {
                this.boxStrokeColor = this.indicatorViewController.getErrorViewCurrentTextColor();
            } else if (hasFocus) {
                this.boxStrokeColor = this.focusedStrokeColor;
            } else if (isHovered) {
                this.boxStrokeColor = this.hoveredStrokeColor;
            } else {
                this.boxStrokeColor = this.defaultStrokeColor;
            }
            if ((isHovered || hasFocus) && isEnabled()) {
                this.boxStrokeWidthPx = this.boxStrokeWidthFocusedPx;
            } else {
                this.boxStrokeWidthPx = this.boxStrokeWidthDefaultPx;
            }
            applyBoxAttributes();
        }
    }

    private void expandHint(boolean animate) {
        if (this.animator != null && this.animator.isRunning()) {
            this.animator.cancel();
        }
        if (!animate || !this.hintAnimationEnabled) {
            this.collapsingTextHelper.setExpansionFraction(0.0f);
        } else {
            animateToExpansionFraction(0.0f);
        }
        if (cutoutEnabled() && ((CutoutDrawable) this.boxBackground).hasCutout()) {
            closeCutout();
        }
        this.hintExpanded = true;
    }

    void animateToExpansionFraction(float target) {
        if (this.collapsingTextHelper.getExpansionFraction() == target) {
            return;
        }
        if (this.animator == null) {
            this.animator = new ValueAnimator();
            this.animator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
            this.animator.setDuration(167L);
            this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    TextInputLayout.this.collapsingTextHelper.setExpansionFraction(((Float) animator.getAnimatedValue()).floatValue());
                }
            });
        }
        this.animator.setFloatValues(this.collapsingTextHelper.getExpansionFraction(), target);
        this.animator.start();
    }

    final boolean isHintExpanded() {
        return this.hintExpanded;
    }

    final boolean isHelperTextDisplayed() {
        return this.indicatorViewController.helperTextIsDisplayed();
    }

    final int getHintCurrentCollapsedTextColor() {
        return this.collapsingTextHelper.getCurrentCollapsedTextColor();
    }

    final float getHintCollapsedTextHeight() {
        return this.collapsingTextHelper.getCollapsedTextHeight();
    }

    final int getErrorTextCurrentColor() {
        return this.indicatorViewController.getErrorViewCurrentTextColor();
    }

    private class TextInputAccessibilityDelegate extends AccessibilityDelegateCompat {
        TextInputAccessibilityDelegate() {
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            event.setClassName(TextInputLayout.class.getSimpleName());
        }

        @Override
        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onPopulateAccessibilityEvent(host, event);
            CharSequence hint = TextInputLayout.this.collapsingTextHelper.getText();
            if (!TextUtils.isEmpty(hint)) {
                event.getText().add(hint);
            }
            CharSequence helperText = TextInputLayout.this.indicatorViewController.getHelperText();
            if (!TextUtils.isEmpty(helperText)) {
                event.getText().add(helperText);
            }
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(TextInputLayout.class.getSimpleName());
            CharSequence hint = TextInputLayout.this.collapsingTextHelper.getText();
            if (!TextUtils.isEmpty(hint)) {
                info.setText(hint);
            }
            if (TextInputLayout.this.editText != null) {
                info.setLabelFor(TextInputLayout.this.editText);
            }
            if (TextInputLayout.this.indicatorViewController.errorIsDisplayed()) {
                info.setContentInvalid(true);
                info.setError(TextInputLayout.this.indicatorViewController.getErrorText());
            }
        }
    }
}
