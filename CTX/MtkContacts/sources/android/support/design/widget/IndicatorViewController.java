package android.support.design.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.support.design.animation.AnimationUtils;
import android.support.design.animation.AnimatorSetCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.Space;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.contacts.ContactPhotoManager;
import java.util.ArrayList;
import java.util.List;

final class IndicatorViewController {
    private Animator captionAnimator;
    private FrameLayout captionArea;
    private int captionDisplayed;
    private int captionToShow;
    private final float captionTranslationYPx;
    private int captionViewsAdded;
    private final Context context;
    private boolean errorEnabled;
    private CharSequence errorText;
    private int errorTextAppearance;
    private TextView errorView;
    private CharSequence helperText;
    private boolean helperTextEnabled;
    private int helperTextTextAppearance;
    private TextView helperTextView;
    private LinearLayout indicatorArea;
    private int indicatorsAdded;
    private final TextInputLayout textInputView;
    private Typeface typeface;

    public IndicatorViewController(TextInputLayout textInputView) {
        this.context = textInputView.getContext();
        this.textInputView = textInputView;
        this.captionTranslationYPx = this.context.getResources().getDimensionPixelSize(R.dimen.design_textinput_caption_translate_y);
    }

    void showHelper(CharSequence helperText) {
        cancelCaptionAnimator();
        this.helperText = helperText;
        this.helperTextView.setText(helperText);
        if (this.captionDisplayed != 2) {
            this.captionToShow = 2;
        }
        updateCaptionViewsVisibility(this.captionDisplayed, this.captionToShow, shouldAnimateCaptionView(this.helperTextView, helperText));
    }

    void hideHelperText() {
        cancelCaptionAnimator();
        if (this.captionDisplayed == 2) {
            this.captionToShow = 0;
        }
        updateCaptionViewsVisibility(this.captionDisplayed, this.captionToShow, shouldAnimateCaptionView(this.helperTextView, null));
    }

    void showError(CharSequence errorText) {
        cancelCaptionAnimator();
        this.errorText = errorText;
        this.errorView.setText(errorText);
        if (this.captionDisplayed != 1) {
            this.captionToShow = 1;
        }
        updateCaptionViewsVisibility(this.captionDisplayed, this.captionToShow, shouldAnimateCaptionView(this.errorView, errorText));
    }

    void hideError() {
        this.errorText = null;
        cancelCaptionAnimator();
        if (this.captionDisplayed == 1) {
            if (this.helperTextEnabled && !TextUtils.isEmpty(this.helperText)) {
                this.captionToShow = 2;
            } else {
                this.captionToShow = 0;
            }
        }
        updateCaptionViewsVisibility(this.captionDisplayed, this.captionToShow, shouldAnimateCaptionView(this.errorView, null));
    }

    private boolean shouldAnimateCaptionView(TextView captionView, CharSequence captionText) {
        return ViewCompat.isLaidOut(this.textInputView) && this.textInputView.isEnabled() && !(this.captionToShow == this.captionDisplayed && captionView != null && TextUtils.equals(captionView.getText(), captionText));
    }

    private void updateCaptionViewsVisibility(final int captionToHide, final int captionToShow, boolean animate) {
        if (animate) {
            AnimatorSet captionAnimator = new AnimatorSet();
            this.captionAnimator = captionAnimator;
            List<Animator> captionAnimatorList = new ArrayList<>();
            createCaptionAnimators(captionAnimatorList, this.helperTextEnabled, this.helperTextView, 2, captionToHide, captionToShow);
            createCaptionAnimators(captionAnimatorList, this.errorEnabled, this.errorView, 1, captionToHide, captionToShow);
            AnimatorSetCompat.playTogether(captionAnimator, captionAnimatorList);
            final TextView captionViewToHide = getCaptionViewFromDisplayState(captionToHide);
            final TextView captionViewToShow = getCaptionViewFromDisplayState(captionToShow);
            captionAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    IndicatorViewController.this.captionDisplayed = captionToShow;
                    IndicatorViewController.this.captionAnimator = null;
                    if (captionViewToHide != null) {
                        captionViewToHide.setVisibility(4);
                        if (captionToHide == 1 && IndicatorViewController.this.errorView != null) {
                            IndicatorViewController.this.errorView.setText((CharSequence) null);
                        }
                    }
                }

                @Override
                public void onAnimationStart(Animator animator) {
                    if (captionViewToShow != null) {
                        captionViewToShow.setVisibility(0);
                    }
                }
            });
            captionAnimator.start();
        } else {
            setCaptionViewVisibilities(captionToHide, captionToShow);
        }
        this.textInputView.updateEditTextBackground();
        this.textInputView.updateLabelState(animate);
        this.textInputView.updateTextInputBoxState();
    }

    private void setCaptionViewVisibilities(int captionToHide, int captionToShow) {
        TextView captionViewDisplayed;
        TextView captionViewToShow;
        if (captionToHide == captionToShow) {
            return;
        }
        if (captionToShow != 0 && (captionViewToShow = getCaptionViewFromDisplayState(captionToShow)) != null) {
            captionViewToShow.setVisibility(0);
            captionViewToShow.setAlpha(1.0f);
        }
        if (captionToHide != 0 && (captionViewDisplayed = getCaptionViewFromDisplayState(captionToHide)) != null) {
            captionViewDisplayed.setVisibility(4);
            if (captionToHide == 1) {
                captionViewDisplayed.setText((CharSequence) null);
            }
        }
        this.captionDisplayed = captionToShow;
    }

    private void createCaptionAnimators(List<Animator> captionAnimatorList, boolean captionEnabled, TextView captionView, int captionState, int captionToHide, int captionToShow) {
        if (captionView == null || !captionEnabled) {
            return;
        }
        if (captionState == captionToShow || captionState == captionToHide) {
            captionAnimatorList.add(createCaptionOpacityAnimator(captionView, captionToShow == captionState));
            if (captionToShow == captionState) {
                captionAnimatorList.add(createCaptionTranslationYAnimator(captionView));
            }
        }
    }

    private ObjectAnimator createCaptionOpacityAnimator(TextView captionView, boolean display) {
        float endValue = display ? 1.0f : ContactPhotoManager.OFFSET_DEFAULT;
        ObjectAnimator opacityAnimator = ObjectAnimator.ofFloat(captionView, (Property<TextView, Float>) View.ALPHA, endValue);
        opacityAnimator.setDuration(167L);
        opacityAnimator.setInterpolator(AnimationUtils.LINEAR_INTERPOLATOR);
        return opacityAnimator;
    }

    private ObjectAnimator createCaptionTranslationYAnimator(TextView captionView) {
        ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(captionView, (Property<TextView, Float>) View.TRANSLATION_Y, -this.captionTranslationYPx, ContactPhotoManager.OFFSET_DEFAULT);
        translationYAnimator.setDuration(217L);
        translationYAnimator.setInterpolator(AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR);
        return translationYAnimator;
    }

    void cancelCaptionAnimator() {
        if (this.captionAnimator != null) {
            this.captionAnimator.cancel();
        }
    }

    boolean isCaptionView(int index) {
        return index == 0 || index == 1;
    }

    private TextView getCaptionViewFromDisplayState(int captionDisplayState) {
        switch (captionDisplayState) {
            case 1:
                return this.errorView;
            case 2:
                return this.helperTextView;
            default:
                return null;
        }
    }

    void adjustIndicatorPadding() {
        if (canAdjustIndicatorPadding()) {
            ViewCompat.setPaddingRelative(this.indicatorArea, ViewCompat.getPaddingStart(this.textInputView.getEditText()), 0, ViewCompat.getPaddingEnd(this.textInputView.getEditText()), this.textInputView.getEditText().getPaddingBottom());
        }
    }

    private boolean canAdjustIndicatorPadding() {
        return (this.indicatorArea == null || this.textInputView.getEditText() == null) ? false : true;
    }

    void addIndicator(TextView indicator, int index) {
        if (this.indicatorArea == null && this.captionArea == null) {
            this.indicatorArea = new LinearLayout(this.context);
            this.indicatorArea.setOrientation(0);
            this.textInputView.addView(this.indicatorArea, -1, -2);
            this.captionArea = new FrameLayout(this.context);
            this.indicatorArea.addView(this.captionArea, -1, new FrameLayout.LayoutParams(-2, -2));
            Space spacer = new Space(this.context);
            ViewGroup.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 0, 1.0f);
            this.indicatorArea.addView(spacer, spacerLp);
            if (this.textInputView.getEditText() != null) {
                adjustIndicatorPadding();
            }
        }
        if (isCaptionView(index)) {
            this.captionArea.setVisibility(0);
            this.captionArea.addView(indicator);
            this.captionViewsAdded++;
        } else {
            this.indicatorArea.addView(indicator, index);
        }
        this.indicatorArea.setVisibility(0);
        this.indicatorsAdded++;
    }

    void removeIndicator(TextView indicator, int index) {
        if (this.indicatorArea == null) {
            return;
        }
        if (isCaptionView(index) && this.captionArea != null) {
            this.captionViewsAdded--;
            setViewGroupGoneIfEmpty(this.captionArea, this.captionViewsAdded);
            this.captionArea.removeView(indicator);
        } else {
            this.indicatorArea.removeView(indicator);
        }
        this.indicatorsAdded--;
        setViewGroupGoneIfEmpty(this.indicatorArea, this.indicatorsAdded);
    }

    private void setViewGroupGoneIfEmpty(ViewGroup viewGroup, int indicatorsAdded) {
        if (indicatorsAdded == 0) {
            viewGroup.setVisibility(8);
        }
    }

    void setErrorEnabled(boolean enabled) {
        if (this.errorEnabled == enabled) {
            return;
        }
        cancelCaptionAnimator();
        if (enabled) {
            this.errorView = new AppCompatTextView(this.context);
            this.errorView.setId(R.id.textinput_error);
            if (this.typeface != null) {
                this.errorView.setTypeface(this.typeface);
            }
            setErrorTextAppearance(this.errorTextAppearance);
            this.errorView.setVisibility(4);
            ViewCompat.setAccessibilityLiveRegion(this.errorView, 1);
            addIndicator(this.errorView, 0);
        } else {
            hideError();
            removeIndicator(this.errorView, 0);
            this.errorView = null;
            this.textInputView.updateEditTextBackground();
            this.textInputView.updateTextInputBoxState();
        }
        this.errorEnabled = enabled;
    }

    boolean isErrorEnabled() {
        return this.errorEnabled;
    }

    boolean isHelperTextEnabled() {
        return this.helperTextEnabled;
    }

    void setHelperTextEnabled(boolean enabled) {
        if (this.helperTextEnabled == enabled) {
            return;
        }
        cancelCaptionAnimator();
        if (enabled) {
            this.helperTextView = new AppCompatTextView(this.context);
            this.helperTextView.setId(R.id.textinput_helper_text);
            if (this.typeface != null) {
                this.helperTextView.setTypeface(this.typeface);
            }
            this.helperTextView.setVisibility(4);
            ViewCompat.setAccessibilityLiveRegion(this.helperTextView, 1);
            setHelperTextAppearance(this.helperTextTextAppearance);
            addIndicator(this.helperTextView, 1);
        } else {
            hideHelperText();
            removeIndicator(this.helperTextView, 1);
            this.helperTextView = null;
            this.textInputView.updateEditTextBackground();
            this.textInputView.updateTextInputBoxState();
        }
        this.helperTextEnabled = enabled;
    }

    boolean errorIsDisplayed() {
        return isCaptionStateError(this.captionDisplayed);
    }

    boolean errorShouldBeShown() {
        return isCaptionStateError(this.captionToShow);
    }

    private boolean isCaptionStateError(int captionState) {
        return (captionState != 1 || this.errorView == null || TextUtils.isEmpty(this.errorText)) ? false : true;
    }

    boolean helperTextIsDisplayed() {
        return isCaptionStateHelperText(this.captionDisplayed);
    }

    private boolean isCaptionStateHelperText(int captionState) {
        return (captionState != 2 || this.helperTextView == null || TextUtils.isEmpty(this.helperText)) ? false : true;
    }

    CharSequence getErrorText() {
        return this.errorText;
    }

    CharSequence getHelperText() {
        return this.helperText;
    }

    int getErrorViewCurrentTextColor() {
        if (this.errorView != null) {
            return this.errorView.getCurrentTextColor();
        }
        return -1;
    }

    ColorStateList getErrorViewTextColors() {
        if (this.errorView != null) {
            return this.errorView.getTextColors();
        }
        return null;
    }

    void setErrorTextAppearance(int resId) {
        this.errorTextAppearance = resId;
        if (this.errorView != null) {
            this.textInputView.setTextAppearanceCompatWithErrorFallback(this.errorView, resId);
        }
    }

    void setHelperTextAppearance(int resId) {
        this.helperTextTextAppearance = resId;
        if (this.helperTextView != null) {
            TextViewCompat.setTextAppearance(this.helperTextView, resId);
        }
    }
}
