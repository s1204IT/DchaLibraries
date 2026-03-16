package com.android.contacts.common.list;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import com.android.contacts.common.ContactPresenceIconUtil;
import com.android.contacts.common.ContactStatusUtil;
import com.android.contacts.common.R;
import com.android.contacts.common.format.TextHighlighter;
import com.android.contacts.common.util.ContactDisplayUtils;
import com.android.contacts.common.util.SearchUtil;
import com.android.contacts.common.util.ViewUtil;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactListItemView extends ViewGroup implements AbsListView.SelectionBoundsAdjuster {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("([\\w-\\.]+)@((?:[\\w]+\\.)+)([a-zA-Z]{2,4})|[\\w]+");
    private Drawable mActivatedBackgroundDrawable;
    private boolean mActivatedStateSupported;
    private boolean mAdjustSelectionBoundsEnabled;
    private Rect mBoundsWithoutHeader;
    private final CharArrayBuffer mDataBuffer;
    private TextView mDataView;
    private int mDataViewHeight;
    private int mDataViewWidthWeight;
    private int mDefaultPhotoViewSize;
    private int mGapBetweenImageAndText;
    private int mGapBetweenLabelAndData;
    private TextView mHeaderTextView;
    private int mHeaderWidth;
    private String mHighlightedPrefix;
    private boolean mIsSectionHeaderEnabled;
    private boolean mKeepHorizontalPaddingForPhotoView;
    private boolean mKeepVerticalPaddingForPhotoView;
    private int mLabelAndDataViewMaxHeight;
    private TextView mLabelView;
    private int mLabelViewHeight;
    private int mLabelViewWidthWeight;
    private int mLeftOffset;
    private ArrayList<HighlightSequence> mNameHighlightSequence;
    private TextView mNameTextView;
    private int mNameTextViewHeight;
    private int mNameTextViewTextColor;
    private int mNameTextViewTextSize;
    private ArrayList<HighlightSequence> mNumberHighlightSequence;
    private final CharArrayBuffer mPhoneticNameBuffer;
    private TextView mPhoneticNameTextView;
    private int mPhoneticNameTextViewHeight;
    private PhotoPosition mPhotoPosition;
    private ImageView mPhotoView;
    private int mPhotoViewHeight;
    private int mPhotoViewWidth;
    private boolean mPhotoViewWidthAndHeightAreReady;
    private int mPreferredHeight;
    private ImageView mPresenceIcon;
    private int mPresenceIconMargin;
    private int mPresenceIconSize;
    private QuickContactBadge mQuickContact;
    private boolean mQuickContactEnabled;
    private int mRightOffset;
    private ColorStateList mSecondaryTextColor;
    private int mSnippetTextViewHeight;
    private TextView mSnippetView;
    private int mStatusTextViewHeight;
    private TextView mStatusView;
    private final TextHighlighter mTextHighlighter;
    private int mTextIndent;
    private int mTextOffsetTop;
    private CharSequence mUnknownNameText;

    public enum PhotoPosition {
        LEFT,
        RIGHT
    }

    protected static class HighlightSequence {
        private final int end;
        private final int start;
    }

    public static final PhotoPosition getDefaultPhotoPosition(boolean opposite) {
        Locale locale = Locale.getDefault();
        int layoutDirection = TextUtils.getLayoutDirectionFromLocale(locale);
        switch (layoutDirection) {
            case 1:
                return opposite ? PhotoPosition.LEFT : PhotoPosition.RIGHT;
            default:
                return opposite ? PhotoPosition.RIGHT : PhotoPosition.LEFT;
        }
    }

    public ContactListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPreferredHeight = 0;
        this.mGapBetweenImageAndText = 0;
        this.mGapBetweenLabelAndData = 0;
        this.mPresenceIconMargin = 4;
        this.mPresenceIconSize = 16;
        this.mTextIndent = 0;
        this.mLabelViewWidthWeight = 3;
        this.mDataViewWidthWeight = 5;
        this.mPhotoPosition = getDefaultPhotoPosition(false);
        this.mQuickContactEnabled = true;
        this.mDefaultPhotoViewSize = 0;
        this.mPhotoViewWidthAndHeightAreReady = false;
        this.mNameTextViewTextColor = -16777216;
        this.mDataBuffer = new CharArrayBuffer(128);
        this.mPhoneticNameBuffer = new CharArrayBuffer(128);
        this.mAdjustSelectionBoundsEnabled = true;
        this.mBoundsWithoutHeader = new Rect();
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ContactListItemView);
        this.mPreferredHeight = a.getDimensionPixelSize(0, this.mPreferredHeight);
        this.mActivatedBackgroundDrawable = a.getDrawable(2);
        this.mGapBetweenImageAndText = a.getDimensionPixelOffset(8, this.mGapBetweenImageAndText);
        this.mGapBetweenLabelAndData = a.getDimensionPixelOffset(9, this.mGapBetweenLabelAndData);
        this.mPresenceIconMargin = a.getDimensionPixelOffset(10, this.mPresenceIconMargin);
        this.mPresenceIconSize = a.getDimensionPixelOffset(11, this.mPresenceIconSize);
        this.mDefaultPhotoViewSize = a.getDimensionPixelOffset(12, this.mDefaultPhotoViewSize);
        this.mTextIndent = a.getDimensionPixelOffset(22, this.mTextIndent);
        this.mTextOffsetTop = a.getDimensionPixelOffset(23, this.mTextOffsetTop);
        this.mDataViewWidthWeight = a.getInteger(24, this.mDataViewWidthWeight);
        this.mLabelViewWidthWeight = a.getInteger(25, this.mLabelViewWidthWeight);
        this.mNameTextViewTextColor = a.getColor(20, this.mNameTextViewTextColor);
        this.mNameTextViewTextSize = (int) a.getDimension(21, (int) getResources().getDimension(com.android.contacts.R.dimen.contact_browser_list_item_text_size));
        setPaddingRelative(a.getDimensionPixelOffset(7, 0), a.getDimensionPixelOffset(4, 0), a.getDimensionPixelOffset(5, 0), a.getDimensionPixelOffset(6, 0));
        this.mTextHighlighter = new TextHighlighter(1);
        a.recycle();
        TypedArray a2 = getContext().obtainStyledAttributes(R.styleable.Theme);
        this.mSecondaryTextColor = a2.getColorStateList(0);
        a2.recycle();
        this.mHeaderWidth = getResources().getDimensionPixelSize(com.android.contacts.R.dimen.contact_list_section_header_width);
        if (this.mActivatedBackgroundDrawable != null) {
            this.mActivatedBackgroundDrawable.setCallback(this);
        }
        this.mNameHighlightSequence = new ArrayList<>();
        this.mNumberHighlightSequence = new ArrayList<>();
        setLayoutDirection(3);
    }

    public void setUnknownNameText(CharSequence unknownNameText) {
        this.mUnknownNameText = unknownNameText;
    }

    public void setQuickContactEnabled(boolean flag) {
        this.mQuickContactEnabled = flag;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int effectiveWidth;
        int dataWidth;
        int labelWidth;
        int statusWidth;
        int specWidth = resolveSize(0, widthMeasureSpec);
        int preferredHeight = this.mPreferredHeight;
        this.mNameTextViewHeight = 0;
        this.mPhoneticNameTextViewHeight = 0;
        this.mLabelViewHeight = 0;
        this.mDataViewHeight = 0;
        this.mLabelAndDataViewMaxHeight = 0;
        this.mSnippetTextViewHeight = 0;
        this.mStatusTextViewHeight = 0;
        ensurePhotoViewSize();
        if (this.mPhotoViewWidth > 0 || this.mKeepHorizontalPaddingForPhotoView) {
            effectiveWidth = ((specWidth - getPaddingLeft()) - getPaddingRight()) - (this.mPhotoViewWidth + this.mGapBetweenImageAndText);
        } else {
            effectiveWidth = (specWidth - getPaddingLeft()) - getPaddingRight();
        }
        if (this.mIsSectionHeaderEnabled) {
            effectiveWidth -= this.mHeaderWidth + this.mGapBetweenImageAndText;
        }
        if (isVisible(this.mNameTextView)) {
            int nameTextWidth = effectiveWidth;
            if (this.mPhotoPosition != PhotoPosition.LEFT) {
                nameTextWidth -= this.mTextIndent;
            }
            this.mNameTextView.measure(View.MeasureSpec.makeMeasureSpec(nameTextWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mNameTextViewHeight = this.mNameTextView.getMeasuredHeight();
        }
        if (isVisible(this.mPhoneticNameTextView)) {
            this.mPhoneticNameTextView.measure(View.MeasureSpec.makeMeasureSpec(effectiveWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mPhoneticNameTextViewHeight = this.mPhoneticNameTextView.getMeasuredHeight();
        }
        if (isVisible(this.mDataView)) {
            if (isVisible(this.mLabelView)) {
                int totalWidth = effectiveWidth - this.mGapBetweenLabelAndData;
                dataWidth = (this.mDataViewWidthWeight * totalWidth) / (this.mDataViewWidthWeight + this.mLabelViewWidthWeight);
                labelWidth = (this.mLabelViewWidthWeight * totalWidth) / (this.mDataViewWidthWeight + this.mLabelViewWidthWeight);
            } else {
                dataWidth = effectiveWidth;
                labelWidth = 0;
            }
        } else {
            dataWidth = 0;
            if (isVisible(this.mLabelView)) {
                labelWidth = effectiveWidth;
            } else {
                labelWidth = 0;
            }
        }
        if (isVisible(this.mDataView)) {
            this.mDataView.measure(View.MeasureSpec.makeMeasureSpec(dataWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mDataViewHeight = this.mDataView.getMeasuredHeight();
        }
        if (isVisible(this.mLabelView)) {
            int mode = this.mPhotoPosition == PhotoPosition.LEFT ? 1073741824 : Integer.MIN_VALUE;
            this.mLabelView.measure(View.MeasureSpec.makeMeasureSpec(labelWidth, mode), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mLabelViewHeight = this.mLabelView.getMeasuredHeight();
        }
        this.mLabelAndDataViewMaxHeight = Math.max(this.mLabelViewHeight, this.mDataViewHeight);
        if (isVisible(this.mSnippetView)) {
            this.mSnippetView.measure(View.MeasureSpec.makeMeasureSpec(effectiveWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mSnippetTextViewHeight = this.mSnippetView.getMeasuredHeight();
        }
        if (isVisible(this.mPresenceIcon)) {
            this.mPresenceIcon.measure(View.MeasureSpec.makeMeasureSpec(this.mPresenceIconSize, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mPresenceIconSize, 1073741824));
            this.mStatusTextViewHeight = this.mPresenceIcon.getMeasuredHeight();
        }
        if (isVisible(this.mStatusView)) {
            if (isVisible(this.mPresenceIcon)) {
                statusWidth = (effectiveWidth - this.mPresenceIcon.getMeasuredWidth()) - this.mPresenceIconMargin;
            } else {
                statusWidth = effectiveWidth;
            }
            this.mStatusView.measure(View.MeasureSpec.makeMeasureSpec(statusWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mStatusTextViewHeight = Math.max(this.mStatusTextViewHeight, this.mStatusView.getMeasuredHeight());
        }
        int height = this.mNameTextViewHeight + this.mPhoneticNameTextViewHeight + this.mLabelAndDataViewMaxHeight + this.mSnippetTextViewHeight + this.mStatusTextViewHeight;
        int height2 = Math.max(Math.max(height, this.mPhotoViewHeight + getPaddingBottom() + getPaddingTop()), preferredHeight);
        if (this.mHeaderTextView != null && this.mHeaderTextView.getVisibility() == 0) {
            this.mHeaderTextView.measure(View.MeasureSpec.makeMeasureSpec(this.mHeaderWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
        }
        setMeasuredDimension(specWidth, height2);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int width = right - left;
        int leftBound = getPaddingLeft();
        int rightBound = width - getPaddingRight();
        boolean isLayoutRtl = ViewUtil.isViewLayoutRtl(this);
        if (this.mIsSectionHeaderEnabled) {
            if (this.mHeaderTextView != null) {
                int headerHeight = this.mHeaderTextView.getMeasuredHeight();
                int headerTopBound = (((height + 0) - headerHeight) / 2) + this.mTextOffsetTop;
                this.mHeaderTextView.layout(isLayoutRtl ? rightBound - this.mHeaderWidth : leftBound, headerTopBound, isLayoutRtl ? rightBound : this.mHeaderWidth + leftBound, headerTopBound + headerHeight);
            }
            if (isLayoutRtl) {
                rightBound -= this.mHeaderWidth;
            } else {
                leftBound += this.mHeaderWidth;
            }
        }
        this.mBoundsWithoutHeader.set(left + leftBound, 0, left + rightBound, height);
        this.mLeftOffset = left + leftBound;
        this.mRightOffset = left + rightBound;
        if (this.mIsSectionHeaderEnabled) {
            if (isLayoutRtl) {
                rightBound -= this.mGapBetweenImageAndText;
            } else {
                leftBound += this.mGapBetweenImageAndText;
            }
        }
        if (this.mActivatedStateSupported && isActivated()) {
            this.mActivatedBackgroundDrawable.setBounds(this.mBoundsWithoutHeader);
        }
        View photoView = this.mQuickContact != null ? this.mQuickContact : this.mPhotoView;
        if (this.mPhotoPosition == PhotoPosition.LEFT) {
            if (photoView != null) {
                int photoTop = 0 + (((height - 0) - this.mPhotoViewHeight) / 2);
                photoView.layout(leftBound, photoTop, this.mPhotoViewWidth + leftBound, this.mPhotoViewHeight + photoTop);
                leftBound += this.mPhotoViewWidth + this.mGapBetweenImageAndText;
            } else if (this.mKeepHorizontalPaddingForPhotoView) {
                leftBound += this.mPhotoViewWidth + this.mGapBetweenImageAndText;
            }
        } else {
            if (photoView != null) {
                int photoTop2 = 0 + (((height - 0) - this.mPhotoViewHeight) / 2);
                photoView.layout(rightBound - this.mPhotoViewWidth, photoTop2, rightBound, this.mPhotoViewHeight + photoTop2);
                rightBound -= this.mPhotoViewWidth + this.mGapBetweenImageAndText;
            } else if (this.mKeepHorizontalPaddingForPhotoView) {
                rightBound -= this.mPhotoViewWidth + this.mGapBetweenImageAndText;
            }
            leftBound += this.mTextIndent;
        }
        int totalTextHeight = this.mNameTextViewHeight + this.mPhoneticNameTextViewHeight + this.mLabelAndDataViewMaxHeight + this.mSnippetTextViewHeight + this.mStatusTextViewHeight;
        int textTopBound = (((height + 0) - totalTextHeight) / 2) + this.mTextOffsetTop;
        if (isVisible(this.mNameTextView)) {
            this.mNameTextView.layout(leftBound, textTopBound, rightBound, this.mNameTextViewHeight + textTopBound);
            textTopBound += this.mNameTextViewHeight;
        }
        if (isLayoutRtl) {
            int statusRightBound = rightBound;
            if (isVisible(this.mPresenceIcon)) {
                int iconWidth = this.mPresenceIcon.getMeasuredWidth();
                this.mPresenceIcon.layout(rightBound - iconWidth, textTopBound, rightBound, this.mStatusTextViewHeight + textTopBound);
                statusRightBound -= this.mPresenceIconMargin + iconWidth;
            }
            if (isVisible(this.mStatusView)) {
                this.mStatusView.layout(leftBound, textTopBound, statusRightBound, this.mStatusTextViewHeight + textTopBound);
            }
        } else {
            int statusLeftBound = leftBound;
            if (isVisible(this.mPresenceIcon)) {
                int iconWidth2 = this.mPresenceIcon.getMeasuredWidth();
                this.mPresenceIcon.layout(leftBound, textTopBound, leftBound + iconWidth2, this.mStatusTextViewHeight + textTopBound);
                statusLeftBound += this.mPresenceIconMargin + iconWidth2;
            }
            if (isVisible(this.mStatusView)) {
                this.mStatusView.layout(statusLeftBound, textTopBound, rightBound, this.mStatusTextViewHeight + textTopBound);
            }
        }
        if (isVisible(this.mStatusView) || isVisible(this.mPresenceIcon)) {
            textTopBound += this.mStatusTextViewHeight;
        }
        int dataLeftBound = leftBound;
        if (isVisible(this.mPhoneticNameTextView)) {
            this.mPhoneticNameTextView.layout(leftBound, textTopBound, rightBound, this.mPhoneticNameTextViewHeight + textTopBound);
            textTopBound += this.mPhoneticNameTextViewHeight;
        }
        if (isVisible(this.mLabelView)) {
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                this.mLabelView.layout(rightBound - this.mLabelView.getMeasuredWidth(), (this.mLabelAndDataViewMaxHeight + textTopBound) - this.mLabelViewHeight, rightBound, this.mLabelAndDataViewMaxHeight + textTopBound);
                rightBound -= this.mLabelView.getMeasuredWidth();
            } else {
                int dataLeftBound2 = leftBound + this.mLabelView.getMeasuredWidth();
                this.mLabelView.layout(leftBound, (this.mLabelAndDataViewMaxHeight + textTopBound) - this.mLabelViewHeight, dataLeftBound2, this.mLabelAndDataViewMaxHeight + textTopBound);
                dataLeftBound = dataLeftBound2 + this.mGapBetweenLabelAndData;
            }
        }
        if (isVisible(this.mDataView)) {
            this.mDataView.layout(dataLeftBound, (this.mLabelAndDataViewMaxHeight + textTopBound) - this.mDataViewHeight, rightBound, this.mLabelAndDataViewMaxHeight + textTopBound);
        }
        if (isVisible(this.mLabelView) || isVisible(this.mDataView)) {
            textTopBound += this.mLabelAndDataViewMaxHeight;
        }
        if (isVisible(this.mSnippetView)) {
            this.mSnippetView.layout(leftBound, textTopBound, rightBound, this.mSnippetTextViewHeight + textTopBound);
        }
    }

    @Override
    public void adjustListItemSelectionBounds(Rect bounds) {
        if (this.mAdjustSelectionBoundsEnabled) {
            bounds.top += this.mBoundsWithoutHeader.top;
            bounds.bottom = bounds.top + this.mBoundsWithoutHeader.height();
            bounds.left = this.mBoundsWithoutHeader.left;
            bounds.right = this.mBoundsWithoutHeader.right;
        }
    }

    protected boolean isVisible(View view) {
        return view != null && view.getVisibility() == 0;
    }

    private void ensurePhotoViewSize() {
        if (!this.mPhotoViewWidthAndHeightAreReady) {
            int defaultPhotoViewSize = getDefaultPhotoViewSize();
            this.mPhotoViewHeight = defaultPhotoViewSize;
            this.mPhotoViewWidth = defaultPhotoViewSize;
            if (!this.mQuickContactEnabled && this.mPhotoView == null) {
                if (!this.mKeepHorizontalPaddingForPhotoView) {
                    this.mPhotoViewWidth = 0;
                }
                if (!this.mKeepVerticalPaddingForPhotoView) {
                    this.mPhotoViewHeight = 0;
                }
            }
            this.mPhotoViewWidthAndHeightAreReady = true;
        }
    }

    protected int getDefaultPhotoViewSize() {
        return this.mDefaultPhotoViewSize;
    }

    private ViewGroup.LayoutParams getDefaultPhotoLayoutParams() {
        ViewGroup.LayoutParams params = generateDefaultLayoutParams();
        params.width = getDefaultPhotoViewSize();
        params.height = params.width;
        return params;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mActivatedStateSupported) {
            this.mActivatedBackgroundDrawable.setState(getDrawableState());
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == this.mActivatedBackgroundDrawable || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mActivatedStateSupported) {
            this.mActivatedBackgroundDrawable.jumpToCurrentState();
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        if (this.mActivatedStateSupported && isActivated()) {
            this.mActivatedBackgroundDrawable.draw(canvas);
        }
        super.dispatchDraw(canvas);
    }

    public void setSectionHeader(String title) {
        if (!TextUtils.isEmpty(title)) {
            if (this.mHeaderTextView == null) {
                this.mHeaderTextView = new TextView(getContext());
                this.mHeaderTextView.setTextAppearance(getContext(), com.android.contacts.R.style.SectionHeaderStyle);
                this.mHeaderTextView.setGravity(ViewUtil.isViewLayoutRtl(this) ? 5 : 3);
                addView(this.mHeaderTextView);
            }
            setMarqueeText(this.mHeaderTextView, title);
            this.mHeaderTextView.setVisibility(0);
            this.mHeaderTextView.setAllCaps(true);
            return;
        }
        if (this.mHeaderTextView != null) {
            this.mHeaderTextView.setVisibility(8);
        }
    }

    public void setIsSectionHeaderEnabled(boolean isSectionHeaderEnabled) {
        this.mIsSectionHeaderEnabled = isSectionHeaderEnabled;
    }

    public QuickContactBadge getQuickContact() {
        if (!this.mQuickContactEnabled) {
            throw new IllegalStateException("QuickContact is disabled for this view");
        }
        if (this.mQuickContact == null) {
            this.mQuickContact = new QuickContactBadge(getContext());
            this.mQuickContact.setOverlay(null);
            this.mQuickContact.setLayoutParams(getDefaultPhotoLayoutParams());
            if (this.mNameTextView != null) {
                this.mQuickContact.setContentDescription(getContext().getString(com.android.contacts.R.string.description_quick_contact_for, this.mNameTextView.getText()));
            }
            addView(this.mQuickContact);
            this.mPhotoViewWidthAndHeightAreReady = false;
        }
        return this.mQuickContact;
    }

    public ImageView getPhotoView() {
        if (this.mPhotoView == null) {
            this.mPhotoView = new ImageView(getContext());
            this.mPhotoView.setLayoutParams(getDefaultPhotoLayoutParams());
            this.mPhotoView.setBackground(null);
            addView(this.mPhotoView);
            this.mPhotoViewWidthAndHeightAreReady = false;
        }
        return this.mPhotoView;
    }

    public void removePhotoView() {
        removePhotoView(false, true);
    }

    public void removePhotoView(boolean keepHorizontalPadding, boolean keepVerticalPadding) {
        this.mPhotoViewWidthAndHeightAreReady = false;
        this.mKeepHorizontalPaddingForPhotoView = keepHorizontalPadding;
        this.mKeepVerticalPaddingForPhotoView = keepVerticalPadding;
        if (this.mPhotoView != null) {
            removeView(this.mPhotoView);
            this.mPhotoView = null;
        }
        if (this.mQuickContact != null) {
            removeView(this.mQuickContact);
            this.mQuickContact = null;
        }
    }

    public void setHighlightedPrefix(String upperCasePrefix) {
        this.mHighlightedPrefix = upperCasePrefix;
    }

    public TextView getNameTextView() {
        if (this.mNameTextView == null) {
            this.mNameTextView = new TextView(getContext());
            this.mNameTextView.setSingleLine(true);
            this.mNameTextView.setEllipsize(getTextEllipsis());
            this.mNameTextView.setTextColor(this.mNameTextViewTextColor);
            this.mNameTextView.setTextSize(0, this.mNameTextViewTextSize);
            this.mNameTextView.setActivated(isActivated());
            this.mNameTextView.setGravity(16);
            this.mNameTextView.setTextAlignment(5);
            this.mNameTextView.setId(com.android.contacts.R.id.cliv_name_textview);
            this.mNameTextView.setElegantTextHeight(false);
            addView(this.mNameTextView);
        }
        return this.mNameTextView;
    }

    public void setPhoneticName(char[] text, int size) {
        if (text == null || size == 0) {
            if (this.mPhoneticNameTextView != null) {
                this.mPhoneticNameTextView.setVisibility(8);
            }
        } else {
            getPhoneticNameTextView();
            setMarqueeText(this.mPhoneticNameTextView, text, size);
            this.mPhoneticNameTextView.setVisibility(0);
        }
    }

    public TextView getPhoneticNameTextView() {
        if (this.mPhoneticNameTextView == null) {
            this.mPhoneticNameTextView = new TextView(getContext());
            this.mPhoneticNameTextView.setSingleLine(true);
            this.mPhoneticNameTextView.setEllipsize(getTextEllipsis());
            this.mPhoneticNameTextView.setTextAppearance(getContext(), android.R.style.TextAppearance.Small);
            this.mPhoneticNameTextView.setTextAlignment(5);
            this.mPhoneticNameTextView.setTypeface(this.mPhoneticNameTextView.getTypeface(), 1);
            this.mPhoneticNameTextView.setActivated(isActivated());
            this.mPhoneticNameTextView.setId(com.android.contacts.R.id.cliv_phoneticname_textview);
            addView(this.mPhoneticNameTextView);
        }
        return this.mPhoneticNameTextView;
    }

    public void setLabel(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            if (this.mLabelView != null) {
                this.mLabelView.setVisibility(8);
            }
        } else {
            getLabelView();
            setMarqueeText(this.mLabelView, text);
            this.mLabelView.setVisibility(0);
        }
    }

    public TextView getLabelView() {
        if (this.mLabelView == null) {
            this.mLabelView = new TextView(getContext());
            this.mLabelView.setSingleLine(true);
            this.mLabelView.setEllipsize(getTextEllipsis());
            this.mLabelView.setTextAppearance(getContext(), com.android.contacts.R.style.TextAppearanceSmall);
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                this.mLabelView.setAllCaps(true);
                this.mLabelView.setGravity(8388613);
            } else {
                this.mLabelView.setTypeface(this.mLabelView.getTypeface(), 1);
            }
            this.mLabelView.setActivated(isActivated());
            this.mLabelView.setId(com.android.contacts.R.id.cliv_label_textview);
            addView(this.mLabelView);
        }
        return this.mLabelView;
    }

    public void setData(char[] text, int size) {
        if (text == null || size == 0) {
            if (this.mDataView != null) {
                this.mDataView.setVisibility(8);
            }
        } else {
            getDataView();
            setMarqueeText(this.mDataView, text, size);
            this.mDataView.setVisibility(0);
        }
    }

    public void setPhoneNumber(String text, String countryIso) {
        if (text == null) {
            if (this.mDataView != null) {
                this.mDataView.setVisibility(8);
                return;
            }
            return;
        }
        getDataView();
        SpannableString textToSet = new SpannableString(text);
        if (this.mNumberHighlightSequence.size() != 0) {
            HighlightSequence highlightSequence = this.mNumberHighlightSequence.get(0);
            this.mTextHighlighter.applyMaskingHighlight(textToSet, highlightSequence.start, highlightSequence.end);
        }
        setMarqueeText(this.mDataView, textToSet);
        this.mDataView.setVisibility(0);
        this.mDataView.setTextDirection(3);
        this.mDataView.setTextAlignment(5);
    }

    private void setMarqueeText(TextView textView, char[] text, int size) {
        if (getTextEllipsis() == TextUtils.TruncateAt.MARQUEE) {
            setMarqueeText(textView, new String(text, 0, size));
        } else {
            textView.setText(text, 0, size);
        }
    }

    private void setMarqueeText(TextView textView, CharSequence text) {
        if (getTextEllipsis() == TextUtils.TruncateAt.MARQUEE) {
            SpannableString spannable = new SpannableString(text);
            spannable.setSpan(TextUtils.TruncateAt.MARQUEE, 0, spannable.length(), 33);
            textView.setText(spannable);
            return;
        }
        textView.setText(text);
    }

    public TextView getDataView() {
        if (this.mDataView == null) {
            this.mDataView = new TextView(getContext());
            this.mDataView.setSingleLine(true);
            this.mDataView.setEllipsize(getTextEllipsis());
            this.mDataView.setTextAppearance(getContext(), com.android.contacts.R.style.TextAppearanceSmall);
            this.mDataView.setTextAlignment(5);
            this.mDataView.setActivated(isActivated());
            this.mDataView.setId(com.android.contacts.R.id.cliv_data_view);
            this.mDataView.setElegantTextHeight(false);
            addView(this.mDataView);
        }
        return this.mDataView;
    }

    public void setSnippet(String text) {
        if (TextUtils.isEmpty(text)) {
            if (this.mSnippetView != null) {
                this.mSnippetView.setVisibility(8);
            }
        } else {
            this.mTextHighlighter.setPrefixText(getSnippetView(), text, this.mHighlightedPrefix);
            this.mSnippetView.setVisibility(0);
            if (ContactDisplayUtils.isPossiblePhoneNumber(text)) {
                this.mSnippetView.setContentDescription(ContactDisplayUtils.getTelephoneTtsSpannable(text));
            } else {
                this.mSnippetView.setContentDescription(null);
            }
        }
    }

    public TextView getSnippetView() {
        if (this.mSnippetView == null) {
            this.mSnippetView = new TextView(getContext());
            this.mSnippetView.setSingleLine(true);
            this.mSnippetView.setEllipsize(getTextEllipsis());
            this.mSnippetView.setTextAppearance(getContext(), android.R.style.TextAppearance.Small);
            this.mSnippetView.setTextAlignment(5);
            this.mSnippetView.setActivated(isActivated());
            addView(this.mSnippetView);
        }
        return this.mSnippetView;
    }

    public TextView getStatusView() {
        if (this.mStatusView == null) {
            this.mStatusView = new TextView(getContext());
            this.mStatusView.setSingleLine(true);
            this.mStatusView.setEllipsize(getTextEllipsis());
            this.mStatusView.setTextAppearance(getContext(), android.R.style.TextAppearance.Small);
            this.mStatusView.setTextColor(this.mSecondaryTextColor);
            this.mStatusView.setActivated(isActivated());
            this.mStatusView.setTextAlignment(5);
            addView(this.mStatusView);
        }
        return this.mStatusView;
    }

    public void setStatus(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            if (this.mStatusView != null) {
                this.mStatusView.setVisibility(8);
            }
        } else {
            getStatusView();
            setMarqueeText(this.mStatusView, text);
            this.mStatusView.setVisibility(0);
        }
    }

    public void setPresence(Drawable icon) {
        if (icon != null) {
            if (this.mPresenceIcon == null) {
                this.mPresenceIcon = new ImageView(getContext());
                addView(this.mPresenceIcon);
            }
            this.mPresenceIcon.setImageDrawable(icon);
            this.mPresenceIcon.setScaleType(ImageView.ScaleType.CENTER);
            this.mPresenceIcon.setVisibility(0);
            return;
        }
        if (this.mPresenceIcon != null) {
            this.mPresenceIcon.setVisibility(8);
        }
    }

    private TextUtils.TruncateAt getTextEllipsis() {
        return TextUtils.TruncateAt.MARQUEE;
    }

    public void showDisplayName(Cursor cursor, int nameColumnIndex, int displayOrder) {
        CharSequence name = cursor.getString(nameColumnIndex);
        setDisplayName(name);
        if (this.mQuickContact != null) {
            this.mQuickContact.setContentDescription(getContext().getString(com.android.contacts.R.string.description_quick_contact_for, this.mNameTextView.getText()));
        }
    }

    public void setDisplayName(CharSequence name) {
        if (!TextUtils.isEmpty(name)) {
            if (this.mHighlightedPrefix != null) {
                name = this.mTextHighlighter.applyPrefixHighlight(name, this.mHighlightedPrefix);
            } else if (this.mNameHighlightSequence.size() != 0) {
                SpannableString spannableName = new SpannableString(name);
                for (HighlightSequence highlightSequence : this.mNameHighlightSequence) {
                    this.mTextHighlighter.applyMaskingHighlight(spannableName, highlightSequence.start, highlightSequence.end);
                }
                name = spannableName;
            }
        } else {
            name = this.mUnknownNameText;
        }
        setMarqueeText(getNameTextView(), name);
        if (ContactDisplayUtils.isPossiblePhoneNumber(name)) {
            this.mNameTextView.setContentDescription(ContactDisplayUtils.getTelephoneTtsSpannable(name.toString()));
        } else {
            this.mNameTextView.setContentDescription(null);
        }
    }

    public void showSimDisplayName(int subId, Cursor cursor, int nameColumnIndex, int displayOrder) {
        CharSequence name;
        CharSequence name2 = cursor.getString(nameColumnIndex);
        if (!TextUtils.isEmpty(name2)) {
            name = this.mTextHighlighter.applyPrefixHighlight(name2, this.mHighlightedPrefix);
        } else {
            name = this.mUnknownNameText;
        }
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(this.mContext);
        SubscriptionInfo subinfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (subinfo != null) {
            name = ((Object) name) + " [" + ((Object) subinfo.getDisplayName()) + "]";
        }
        setMarqueeText(getNameTextView(), name);
        if (this.mQuickContact != null) {
            this.mQuickContact.setContentDescription(this.mContext.getString(com.android.contacts.R.string.description_quick_contact_for, this.mNameTextView.getText()));
        }
    }

    public void hideDisplayName() {
        if (this.mNameTextView != null) {
            removeView(this.mNameTextView);
            this.mNameTextView = null;
        }
    }

    public void showPhoneticName(Cursor cursor, int phoneticNameColumnIndex) {
        cursor.copyStringToBuffer(phoneticNameColumnIndex, this.mPhoneticNameBuffer);
        int phoneticNameSize = this.mPhoneticNameBuffer.sizeCopied;
        if (phoneticNameSize != 0) {
            setPhoneticName(this.mPhoneticNameBuffer.data, phoneticNameSize);
        } else {
            setPhoneticName(null, 0);
        }
    }

    public void showPresenceAndStatusMessage(Cursor cursor, int presenceColumnIndex, int contactStatusColumnIndex) {
        Drawable icon = null;
        int presence = 0;
        if (!cursor.isNull(presenceColumnIndex)) {
            presence = cursor.getInt(presenceColumnIndex);
            icon = ContactPresenceIconUtil.getPresenceIcon(getContext(), presence);
        }
        setPresence(icon);
        String statusMessage = null;
        if (contactStatusColumnIndex != 0 && !cursor.isNull(contactStatusColumnIndex)) {
            statusMessage = cursor.getString(contactStatusColumnIndex);
        }
        if (statusMessage == null && presence != 0) {
            statusMessage = ContactStatusUtil.getStatusString(getContext(), presence);
        }
        setStatus(statusMessage);
    }

    public void showSnippet(Cursor cursor, int summarySnippetColumnIndex) {
        int lastNl;
        if (cursor.getColumnCount() <= summarySnippetColumnIndex) {
            setSnippet(null);
            return;
        }
        String snippet = cursor.getString(summarySnippetColumnIndex);
        Bundle extras = cursor.getExtras();
        if (extras.getBoolean("deferred_snippeting")) {
            String query = extras.getString("deferred_snippeting_query");
            String displayName = null;
            int displayNameIndex = cursor.getColumnIndex("display_name");
            if (displayNameIndex >= 0) {
                displayName = cursor.getString(displayNameIndex);
            }
            snippet = updateSnippet(snippet, query, displayName);
        } else if (snippet != null) {
            int from = 0;
            int to = snippet.length();
            int start = snippet.indexOf(91);
            if (start == -1) {
                snippet = null;
            } else {
                int firstNl = snippet.lastIndexOf(10, start);
                if (firstNl != -1) {
                    from = firstNl + 1;
                }
                int end = snippet.lastIndexOf(93);
                if (end != -1 && (lastNl = snippet.indexOf(10, end)) != -1) {
                    to = lastNl;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = from; i < to; i++) {
                    char c = snippet.charAt(i);
                    if (c != '[' && c != ']') {
                        sb.append(c);
                    }
                }
                snippet = sb.toString();
            }
        }
        setSnippet(snippet);
    }

    private String updateSnippet(String snippet, String query, String displayName) {
        if (TextUtils.isEmpty(snippet) || TextUtils.isEmpty(query)) {
            return null;
        }
        String query2 = SearchUtil.cleanStartAndEndOfSearchQuery(query.toLowerCase());
        if (!TextUtils.isEmpty(displayName)) {
            String lowerDisplayName = displayName.toLowerCase();
            List<String> nameTokens = split(lowerDisplayName);
            for (String nameToken : nameTokens) {
                if (nameToken.startsWith(query2)) {
                    return null;
                }
            }
        }
        SearchUtil.MatchedLine matched = SearchUtil.findMatchingLine(snippet, query2);
        if (matched == null || matched.line == null) {
            return null;
        }
        int lengthThreshold = getResources().getInteger(com.android.contacts.R.integer.snippet_length_before_tokenize);
        if (matched.line.length() > lengthThreshold) {
            return snippetize(matched.line, matched.startIndex, lengthThreshold);
        }
        return matched.line;
    }

    private String snippetize(String line, int matchIndex, int maxLength) {
        int remainingLength = maxLength;
        int tempRemainingLength = remainingLength;
        int index = matchIndex;
        int endTokenIndex = index;
        while (true) {
            if (index >= line.length()) {
                break;
            }
            if (!Character.isLetterOrDigit(line.charAt(index))) {
                endTokenIndex = index;
                remainingLength = tempRemainingLength;
                break;
            }
            tempRemainingLength--;
            index++;
        }
        int tempRemainingLength2 = remainingLength;
        int startTokenIndex = matchIndex;
        for (int index2 = matchIndex - 1; index2 > -1 && tempRemainingLength2 > 0; index2--) {
            if (!Character.isLetterOrDigit(line.charAt(index2))) {
                startTokenIndex = index2;
                remainingLength = tempRemainingLength2;
            }
            tempRemainingLength2--;
        }
        int tempRemainingLength3 = remainingLength;
        for (int index3 = endTokenIndex; index3 < line.length() && tempRemainingLength3 > 0; index3++) {
            if (!Character.isLetterOrDigit(line.charAt(index3))) {
                endTokenIndex = index3;
            }
            tempRemainingLength3--;
        }
        StringBuilder sb = new StringBuilder();
        if (startTokenIndex > 0) {
            sb.append("...");
        }
        sb.append(line.substring(startTokenIndex, endTokenIndex));
        if (endTokenIndex < line.length()) {
            sb.append("...");
        }
        return sb.toString();
    }

    private static List<String> split(String content) {
        Matcher matcher = SPLIT_PATTERN.matcher(content);
        ArrayList<String> tokens = Lists.newArrayList();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    public void showData(Cursor cursor, int dataColumnIndex) {
        cursor.copyStringToBuffer(dataColumnIndex, this.mDataBuffer);
        setData(this.mDataBuffer.data, this.mDataBuffer.sizeCopied);
    }

    public void setActivatedStateSupported(boolean flag) {
        this.mActivatedStateSupported = flag;
    }

    public void setAdjustSelectionBoundsEnabled(boolean enabled) {
        this.mAdjustSelectionBoundsEnabled = enabled;
    }

    @Override
    public void requestLayout() {
        forceLayout();
    }

    public void setPhotoPosition(PhotoPosition photoPosition) {
        this.mPhotoPosition = photoPosition;
    }

    public void setDrawableResource(int backgroundId, int drawableId) {
        ImageView photo = getPhotoView();
        photo.setScaleType(ImageView.ScaleType.CENTER);
        photo.setBackgroundResource(backgroundId);
        photo.setImageResource(drawableId);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (this.mBoundsWithoutHeader.contains((int) x, (int) y) || !pointIsInView(x, y)) {
            return super.onTouchEvent(event);
        }
        return true;
    }

    private final boolean pointIsInView(float localX, float localY) {
        return localX >= ((float) this.mLeftOffset) && localX < ((float) this.mRightOffset) && localY >= 0.0f && localY < ((float) (getBottom() - getTop()));
    }
}
