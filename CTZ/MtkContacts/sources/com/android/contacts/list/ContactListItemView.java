package com.android.contacts.list;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatImageButton;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactPresenceIconUtil;
import com.android.contacts.ContactStatusUtil;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.format.TextHighlighter;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.android.contacts.list.PhoneNumberListAdapter;
import com.android.contacts.util.ContactDisplayUtils;
import com.android.contacts.util.SearchUtil;
import com.android.contacts.util.ViewUtil;
import com.google.common.collect.Lists;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.list.ContactListItemViewEx;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactListItemView extends ViewGroup implements AbsListView.SelectionBoundsAdjuster {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("([\\w-\\.]+)@((?:[\\w]+\\.)+)([a-zA-Z]{2,4})|[\\w]+");
    private static final String TAG = "ContactListItemView";
    private Drawable mActivatedBackgroundDrawable;
    private boolean mActivatedStateSupported;
    private boolean mAdjustSelectionBoundsEnabled;
    private int mAvatarOffsetTop;
    private Rect mBoundsWithoutHeader;
    private AppCompatCheckBox mCheckBox;
    private int mCheckBoxHeight;
    private int mCheckBoxWidth;
    private ContactListItemViewEx mContactListItemViewEx;
    private final CharArrayBuffer mDataBuffer;
    private TextView mDataView;
    private int mDataViewHeight;
    private int mDataViewWidthWeight;
    private int mDefaultPhotoViewSize;
    private AppCompatImageButton mDeleteImageButton;
    private int mDeleteImageButtonHeight;
    private int mDeleteImageButtonWidth;
    private int mGapBetweenImageAndText;
    private int mGapBetweenIndexerAndImage;
    private int mGapBetweenLabelAndData;
    private int mGapFromScrollBar;
    private View mHeaderView;
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
    private PhoneNumberListAdapter.Listener mPhoneNumberListAdapterListener;
    private final CharArrayBuffer mPhoneticNameBuffer;
    private TextView mPhoneticNameTextView;
    private int mPhoneticNameTextViewHeight;
    private PhotoPosition mPhotoPosition;
    private ImageView mPhotoView;
    private int mPhotoViewHeight;
    private int mPhotoViewWidth;
    private boolean mPhotoViewWidthAndHeightAreReady;
    private int mPosition;
    private int mPreferredHeight;
    private ImageView mPresenceIcon;
    private int mPresenceIconMargin;
    private int mPresenceIconSize;
    private QuickContactBadge mQuickContact;
    private boolean mQuickContactEnabled;
    private int mRightOffset;
    private ColorStateList mSecondaryTextColor;
    private boolean mShowVideoCallIcon;
    private int mSnippetTextViewHeight;
    private TextView mSnippetView;
    private int mStatusTextViewHeight;
    private TextView mStatusView;
    private boolean mSupportVideoCallIcon;
    private final TextHighlighter mTextHighlighter;
    private int mTextIndent;
    private int mTextOffsetTop;
    private CharSequence mUnknownNameText;
    private ImageView mVideoCallIcon;
    private int mVideoCallIconMargin;
    private int mVideoCallIconSize;
    private ImageView mWorkProfileIcon;

    public enum PhotoPosition {
        LEFT,
        RIGHT
    }

    protected static class HighlightSequence {
        private final int end;
        private final int start;

        HighlightSequence(int i, int i2) {
            this.start = i;
            this.end = i2;
        }
    }

    public static final PhotoPosition getDefaultPhotoPosition(boolean z) {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) != 1 ? z ? PhotoPosition.RIGHT : PhotoPosition.LEFT : z ? PhotoPosition.LEFT : PhotoPosition.RIGHT;
    }

    public ContactListItemView(Context context) {
        super(context);
        this.mPreferredHeight = 0;
        this.mGapBetweenImageAndText = 0;
        this.mGapBetweenIndexerAndImage = 0;
        this.mGapBetweenLabelAndData = 0;
        this.mPresenceIconMargin = 4;
        this.mPresenceIconSize = 16;
        this.mTextIndent = 0;
        this.mVideoCallIconSize = 32;
        this.mVideoCallIconMargin = 16;
        this.mGapFromScrollBar = 20;
        this.mLabelViewWidthWeight = 3;
        this.mDataViewWidthWeight = 5;
        this.mShowVideoCallIcon = false;
        this.mSupportVideoCallIcon = false;
        this.mPhotoPosition = getDefaultPhotoPosition(false);
        this.mQuickContactEnabled = true;
        this.mDefaultPhotoViewSize = 0;
        this.mPhotoViewWidthAndHeightAreReady = false;
        this.mNameTextViewTextColor = -16777216;
        this.mDataBuffer = new CharArrayBuffer(128);
        this.mPhoneticNameBuffer = new CharArrayBuffer(128);
        this.mAdjustSelectionBoundsEnabled = true;
        this.mBoundsWithoutHeader = new Rect();
        this.mContactListItemViewEx = new ContactListItemViewEx(this);
        this.mTextHighlighter = new TextHighlighter(1);
        this.mNameHighlightSequence = new ArrayList<>();
        this.mNumberHighlightSequence = new ArrayList<>();
    }

    public ContactListItemView(Context context, AttributeSet attributeSet, boolean z) {
        this(context, attributeSet);
        this.mSupportVideoCallIcon = z;
    }

    public ContactListItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPreferredHeight = 0;
        this.mGapBetweenImageAndText = 0;
        this.mGapBetweenIndexerAndImage = 0;
        this.mGapBetweenLabelAndData = 0;
        this.mPresenceIconMargin = 4;
        this.mPresenceIconSize = 16;
        this.mTextIndent = 0;
        this.mVideoCallIconSize = 32;
        this.mVideoCallIconMargin = 16;
        this.mGapFromScrollBar = 20;
        this.mLabelViewWidthWeight = 3;
        this.mDataViewWidthWeight = 5;
        this.mShowVideoCallIcon = false;
        this.mSupportVideoCallIcon = false;
        this.mPhotoPosition = getDefaultPhotoPosition(false);
        this.mQuickContactEnabled = true;
        this.mDefaultPhotoViewSize = 0;
        this.mPhotoViewWidthAndHeightAreReady = false;
        this.mNameTextViewTextColor = -16777216;
        this.mDataBuffer = new CharArrayBuffer(128);
        this.mPhoneticNameBuffer = new CharArrayBuffer(128);
        this.mAdjustSelectionBoundsEnabled = true;
        this.mBoundsWithoutHeader = new Rect();
        this.mContactListItemViewEx = new ContactListItemViewEx(this);
        if (R.styleable.ContactListItemView != null) {
            TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.ContactListItemView);
            this.mPreferredHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(11, this.mPreferredHeight);
            this.mActivatedBackgroundDrawable = typedArrayObtainStyledAttributes.getDrawable(0);
            this.mGapBetweenImageAndText = typedArrayObtainStyledAttributes.getDimensionPixelOffset(4, this.mGapBetweenImageAndText);
            this.mGapBetweenIndexerAndImage = typedArrayObtainStyledAttributes.getDimensionPixelOffset(5, this.mGapBetweenIndexerAndImage);
            this.mGapBetweenLabelAndData = typedArrayObtainStyledAttributes.getDimensionPixelOffset(6, this.mGapBetweenLabelAndData);
            this.mPresenceIconMargin = typedArrayObtainStyledAttributes.getDimensionPixelOffset(21, this.mPresenceIconMargin);
            this.mPresenceIconSize = typedArrayObtainStyledAttributes.getDimensionPixelOffset(22, this.mPresenceIconSize);
            this.mDefaultPhotoViewSize = typedArrayObtainStyledAttributes.getDimensionPixelOffset(19, this.mDefaultPhotoViewSize);
            this.mTextIndent = typedArrayObtainStyledAttributes.getDimensionPixelOffset(24, this.mTextIndent);
            this.mTextOffsetTop = typedArrayObtainStyledAttributes.getDimensionPixelOffset(25, this.mTextOffsetTop);
            this.mAvatarOffsetTop = typedArrayObtainStyledAttributes.getDimensionPixelOffset(1, this.mAvatarOffsetTop);
            this.mDataViewWidthWeight = typedArrayObtainStyledAttributes.getInteger(3, this.mDataViewWidthWeight);
            this.mLabelViewWidthWeight = typedArrayObtainStyledAttributes.getInteger(12, this.mLabelViewWidthWeight);
            this.mNameTextViewTextColor = typedArrayObtainStyledAttributes.getColor(13, this.mNameTextViewTextColor);
            this.mNameTextViewTextSize = (int) typedArrayObtainStyledAttributes.getDimension(14, (int) getResources().getDimension(R.dimen.contact_browser_list_item_text_size));
            this.mVideoCallIconSize = typedArrayObtainStyledAttributes.getDimensionPixelOffset(27, this.mVideoCallIconSize);
            this.mVideoCallIconMargin = typedArrayObtainStyledAttributes.getDimensionPixelOffset(26, this.mVideoCallIconMargin);
            setPaddingRelative(typedArrayObtainStyledAttributes.getDimensionPixelOffset(16, 0), typedArrayObtainStyledAttributes.getDimensionPixelOffset(18, 0), typedArrayObtainStyledAttributes.getDimensionPixelOffset(17, 0), typedArrayObtainStyledAttributes.getDimensionPixelOffset(15, 0));
            typedArrayObtainStyledAttributes.recycle();
        }
        this.mTextHighlighter = new TextHighlighter(1);
        if (R.styleable.Theme != null) {
            TypedArray typedArrayObtainStyledAttributes2 = getContext().obtainStyledAttributes(R.styleable.Theme);
            this.mSecondaryTextColor = typedArrayObtainStyledAttributes2.getColorStateList(0);
            typedArrayObtainStyledAttributes2.recycle();
        }
        this.mHeaderWidth = getResources().getDimensionPixelSize(R.dimen.contact_list_section_header_width);
        if (this.mActivatedBackgroundDrawable != null) {
            this.mActivatedBackgroundDrawable.setCallback(this);
        }
        this.mNameHighlightSequence = new ArrayList<>();
        this.mNumberHighlightSequence = new ArrayList<>();
        setLayoutDirection(3);
    }

    public void setUnknownNameText(CharSequence charSequence) {
        this.mUnknownNameText = charSequence;
    }

    public void setQuickContactEnabled(boolean z) {
        this.mQuickContactEnabled = z;
    }

    public void setShowVideoCallIcon(boolean z, PhoneNumberListAdapter.Listener listener, int i) {
        this.mShowVideoCallIcon = z;
        this.mPhoneNumberListAdapterListener = listener;
        this.mPosition = i;
        if (this.mShowVideoCallIcon) {
            if (this.mVideoCallIcon == null) {
                this.mVideoCallIcon = new ImageView(getContext());
                addView(this.mVideoCallIcon);
            }
            this.mVideoCallIcon.setContentDescription(getContext().getString(R.string.description_search_video_call));
            this.mVideoCallIcon.setImageResource(R.drawable.quantum_ic_videocam_vd_theme_24);
            this.mVideoCallIcon.setScaleType(ImageView.ScaleType.CENTER);
            this.mVideoCallIcon.setVisibility(0);
            this.mVideoCallIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ContactListItemView.this.mPhoneNumberListAdapterListener != null) {
                        ContactListItemView.this.mPhoneNumberListAdapterListener.onVideoCallIconClicked(ContactListItemView.this.mPosition);
                    }
                }
            });
            return;
        }
        if (this.mVideoCallIcon != null) {
            this.mVideoCallIcon.setVisibility(8);
        }
    }

    public void setSupportVideoCallIcon(boolean z) {
        this.mSupportVideoCallIcon = z;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int paddingLeft;
        int i3;
        int i4;
        int i5;
        int iResolveSize = resolveSize(0, i);
        int i6 = this.mPreferredHeight;
        this.mNameTextViewHeight = 0;
        this.mPhoneticNameTextViewHeight = 0;
        this.mLabelViewHeight = 0;
        this.mDataViewHeight = 0;
        this.mLabelAndDataViewMaxHeight = 0;
        this.mSnippetTextViewHeight = 0;
        this.mStatusTextViewHeight = 0;
        this.mCheckBoxWidth = 0;
        this.mCheckBoxHeight = 0;
        this.mDeleteImageButtonWidth = 0;
        this.mDeleteImageButtonHeight = 0;
        ensurePhotoViewSize();
        if (this.mPhotoViewWidth > 0 || this.mKeepHorizontalPaddingForPhotoView) {
            paddingLeft = ((iResolveSize - getPaddingLeft()) - getPaddingRight()) - ((this.mPhotoViewWidth + this.mGapBetweenImageAndText) + this.mGapBetweenIndexerAndImage);
        } else {
            paddingLeft = (iResolveSize - getPaddingLeft()) - getPaddingRight();
        }
        if (this.mIsSectionHeaderEnabled) {
            paddingLeft -= this.mHeaderWidth;
        }
        if (this.mSupportVideoCallIcon) {
            paddingLeft -= this.mVideoCallIconSize + this.mVideoCallIconMargin;
        }
        if (isVisible(this.mCheckBox)) {
            this.mCheckBox.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mCheckBoxWidth = this.mCheckBox.getMeasuredWidth();
            this.mCheckBoxHeight = this.mCheckBox.getMeasuredHeight();
            paddingLeft -= this.mCheckBoxWidth + this.mGapBetweenImageAndText;
        }
        if (isVisible(this.mDeleteImageButton)) {
            this.mDeleteImageButton.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mDeleteImageButtonWidth = this.mDeleteImageButton.getMeasuredWidth();
            this.mDeleteImageButtonHeight = this.mDeleteImageButton.getMeasuredHeight();
            paddingLeft -= this.mDeleteImageButtonWidth + this.mGapBetweenImageAndText;
        }
        ExtensionManager.getInstance();
        int widthWithPadding = paddingLeft - ExtensionManager.getRcsExtension().getContactListItemRcsView().getWidthWithPadding();
        if (isVisible(this.mNameTextView)) {
            if (this.mPhotoPosition != PhotoPosition.LEFT) {
                i5 = widthWithPadding - this.mTextIndent;
            } else {
                i5 = widthWithPadding;
            }
            this.mNameTextView.measure(View.MeasureSpec.makeMeasureSpec(i5, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mNameTextViewHeight = this.mNameTextView.getMeasuredHeight();
        }
        if (isVisible(this.mPhoneticNameTextView)) {
            this.mPhoneticNameTextView.measure(View.MeasureSpec.makeMeasureSpec(widthWithPadding, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mPhoneticNameTextViewHeight = this.mPhoneticNameTextView.getMeasuredHeight();
        }
        if (isVisible(this.mDataView)) {
            if (isVisible(this.mLabelView)) {
                int i7 = widthWithPadding - this.mGapBetweenLabelAndData;
                i4 = (this.mDataViewWidthWeight * i7) / (this.mDataViewWidthWeight + this.mLabelViewWidthWeight);
                i3 = (i7 * this.mLabelViewWidthWeight) / (this.mDataViewWidthWeight + this.mLabelViewWidthWeight);
            } else {
                i3 = 0;
                i4 = widthWithPadding;
            }
        } else if (isVisible(this.mLabelView)) {
            i4 = 0;
            i3 = widthWithPadding;
        } else {
            i3 = 0;
            i4 = 0;
        }
        if (isVisible(this.mDataView)) {
            this.mDataView.measure(View.MeasureSpec.makeMeasureSpec(i4, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mDataViewHeight = this.mDataView.getMeasuredHeight();
        }
        if (isVisible(this.mLabelView)) {
            this.mLabelView.measure(View.MeasureSpec.makeMeasureSpec(i3, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mLabelViewHeight = this.mLabelView.getMeasuredHeight();
        }
        this.mLabelAndDataViewMaxHeight = Math.max(this.mLabelViewHeight, this.mDataViewHeight);
        if (isVisible(this.mSnippetView)) {
            this.mSnippetView.measure(View.MeasureSpec.makeMeasureSpec(widthWithPadding, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mSnippetTextViewHeight = this.mSnippetView.getMeasuredHeight();
        }
        if (isVisible(this.mPresenceIcon)) {
            this.mPresenceIcon.measure(View.MeasureSpec.makeMeasureSpec(this.mPresenceIconSize, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mPresenceIconSize, 1073741824));
            this.mStatusTextViewHeight = this.mPresenceIcon.getMeasuredHeight();
        }
        if (this.mSupportVideoCallIcon && isVisible(this.mVideoCallIcon)) {
            this.mVideoCallIcon.measure(View.MeasureSpec.makeMeasureSpec(this.mVideoCallIconSize, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mVideoCallIconSize, 1073741824));
        }
        if (isVisible(this.mWorkProfileIcon)) {
            this.mWorkProfileIcon.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mNameTextViewHeight = Math.max(this.mNameTextViewHeight, this.mWorkProfileIcon.getMeasuredHeight());
        }
        if (isVisible(this.mStatusView)) {
            if (isVisible(this.mPresenceIcon)) {
                widthWithPadding = (widthWithPadding - this.mPresenceIcon.getMeasuredWidth()) - this.mPresenceIconMargin;
            }
            this.mStatusView.measure(View.MeasureSpec.makeMeasureSpec(widthWithPadding, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
            this.mStatusTextViewHeight = Math.max(this.mStatusTextViewHeight, this.mStatusView.getMeasuredHeight());
        }
        int iMax = Math.max(Math.max(this.mNameTextViewHeight + this.mPhoneticNameTextViewHeight + this.mLabelAndDataViewMaxHeight + this.mSnippetTextViewHeight + this.mStatusTextViewHeight + getPaddingBottom() + getPaddingTop(), this.mPhotoViewHeight + getPaddingBottom() + getPaddingTop()), i6);
        if (this.mHeaderView != null && this.mHeaderView.getVisibility() == 0) {
            this.mHeaderView.measure(View.MeasureSpec.makeMeasureSpec(this.mHeaderWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
        }
        ExtensionManager.getInstance();
        ExtensionManager.getViewCustomExtension().getContactListItemViewCustom().onMeasure(iResolveSize, iMax);
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().getContactListItemRcsView().onMeasure(iResolveSize, iMax);
        ExtensionManager.getInstance();
        ExtensionManager.getContactsCommonPresenceExtension().onMeasure(iResolveSize, iMax);
        setMeasuredDimension(iResolveSize, iMax);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int measuredWidth;
        int i5;
        int measuredWidth2;
        int i6;
        int i7;
        int i8 = i4 - i2;
        int paddingLeft = getPaddingLeft();
        int paddingRight = (i3 - i) - getPaddingRight();
        boolean zIsViewLayoutRtl = ViewUtil.isViewLayoutRtl(this);
        if (this.mIsSectionHeaderEnabled) {
            if (this.mHeaderView != null) {
                int measuredHeight = this.mHeaderView.getMeasuredHeight();
                int i9 = (((i8 + 0) - measuredHeight) / 2) + this.mTextOffsetTop;
                this.mHeaderView.layout(zIsViewLayoutRtl ? paddingRight - this.mHeaderWidth : paddingLeft, i9, zIsViewLayoutRtl ? paddingRight : this.mHeaderWidth + paddingLeft, measuredHeight + i9);
            }
            if (zIsViewLayoutRtl) {
                paddingRight -= this.mHeaderWidth;
            } else {
                paddingLeft += this.mHeaderWidth;
            }
        }
        int i10 = i + paddingLeft;
        int i11 = i + paddingRight;
        this.mBoundsWithoutHeader.set(i10, 0, i11, i8);
        this.mLeftOffset = i10;
        this.mRightOffset = i11;
        if (zIsViewLayoutRtl) {
            paddingRight -= this.mGapBetweenIndexerAndImage;
        } else {
            paddingLeft += this.mGapBetweenIndexerAndImage;
        }
        if (this.mActivatedStateSupported && isActivated()) {
            if (GlobalEnv.isUsingTwoPanes()) {
                Rect rect = new Rect();
                rect.set(paddingLeft, 0, i + paddingRight, i8);
                this.mActivatedBackgroundDrawable.setBounds(rect);
            } else {
                this.mActivatedBackgroundDrawable.setBounds(this.mBoundsWithoutHeader);
            }
        }
        if (isVisible(this.mCheckBox)) {
            int i12 = (((i8 + 0) - this.mCheckBoxHeight) / 2) + 0;
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                this.mCheckBox.layout((paddingRight - this.mGapFromScrollBar) - this.mCheckBoxWidth, i12, paddingRight - this.mGapFromScrollBar, this.mCheckBoxHeight + i12);
            } else {
                this.mCheckBox.layout(this.mGapFromScrollBar + paddingLeft, i12, this.mGapFromScrollBar + paddingLeft + this.mCheckBoxWidth, this.mCheckBoxHeight + i12);
            }
        }
        if (isVisible(this.mDeleteImageButton)) {
            int i13 = (((i8 + 0) - this.mDeleteImageButtonHeight) / 2) + 0;
            int i14 = this.mDeleteImageButtonHeight > this.mDeleteImageButtonWidth ? this.mDeleteImageButtonHeight : this.mDeleteImageButtonWidth;
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                int i15 = paddingRight - i14;
                this.mDeleteImageButton.layout(i15, i13, paddingRight, i14 + i13);
                paddingRight = i15;
            } else {
                int i16 = paddingLeft + i14;
                this.mDeleteImageButton.layout(paddingLeft, i13, i16, i14 + i13);
                paddingLeft = i16;
            }
        }
        View view = this.mQuickContact != null ? this.mQuickContact : this.mPhotoView;
        if (this.mPhotoPosition == PhotoPosition.LEFT) {
            if (view != null) {
                int i17 = (((i8 + 0) - this.mPhotoViewHeight) / 2) + 0 + this.mAvatarOffsetTop;
                view.layout(paddingLeft, i17, this.mPhotoViewWidth + paddingLeft, this.mPhotoViewHeight + i17);
                paddingLeft += this.mPhotoViewWidth + this.mGapBetweenImageAndText;
            } else if (this.mKeepHorizontalPaddingForPhotoView) {
                paddingLeft += this.mPhotoViewWidth + this.mGapBetweenImageAndText;
            }
        } else {
            if (view != null) {
                int i18 = (((i8 + 0) - this.mPhotoViewHeight) / 2) + 0 + this.mAvatarOffsetTop;
                view.layout(paddingRight - this.mPhotoViewWidth, i18, paddingRight, this.mPhotoViewHeight + i18);
                paddingRight -= this.mPhotoViewWidth + this.mGapBetweenImageAndText;
            } else if (this.mKeepHorizontalPaddingForPhotoView) {
                paddingRight -= this.mPhotoViewWidth + this.mGapBetweenImageAndText;
            }
            paddingLeft += this.mTextIndent;
        }
        if (this.mSupportVideoCallIcon) {
            if (isVisible(this.mVideoCallIcon)) {
                int i19 = (((i8 + 0) - this.mVideoCallIconSize) / 2) + 0;
                if (!zIsViewLayoutRtl) {
                    this.mVideoCallIcon.layout(paddingRight - this.mVideoCallIconSize, i19, paddingRight, this.mVideoCallIconSize + i19);
                } else {
                    this.mVideoCallIcon.layout(paddingLeft, i19, this.mVideoCallIconSize + paddingLeft, this.mVideoCallIconSize + i19);
                }
            }
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                paddingRight -= this.mVideoCallIconSize + this.mVideoCallIconMargin;
            } else {
                paddingLeft += this.mVideoCallIconSize + this.mVideoCallIconMargin;
            }
        }
        ExtensionManager.getInstance();
        int i20 = paddingLeft;
        ExtensionManager.getContactsCommonPresenceExtension().onLayout(z, i20, 0, paddingRight, i8);
        ExtensionManager.getInstance();
        int widthWithPadding = paddingRight - ExtensionManager.getContactsCommonPresenceExtension().getWidthWithPadding();
        ExtensionManager.getInstance();
        ExtensionManager.getViewCustomExtension().getContactListItemViewCustom().onLayout(z, i20, 0, widthWithPadding, i8);
        ExtensionManager.getInstance();
        int widthWithPadding2 = widthWithPadding - ExtensionManager.getViewCustomExtension().getContactListItemViewCustom().getWidthWithPadding();
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().getContactListItemRcsView().onLayout(z, i20, 0, widthWithPadding2 - this.mCheckBoxWidth, i8);
        ExtensionManager.getInstance();
        int widthWithPadding3 = widthWithPadding2 - ExtensionManager.getRcsExtension().getContactListItemRcsView().getWidthWithPadding();
        int i21 = (((i8 + 0) - ((((this.mNameTextViewHeight + this.mPhoneticNameTextViewHeight) + this.mLabelAndDataViewMaxHeight) + this.mSnippetTextViewHeight) + this.mStatusTextViewHeight)) / 2) + this.mTextOffsetTop;
        if (isVisible(this.mWorkProfileIcon)) {
            measuredWidth = this.mWorkProfileIcon.getMeasuredWidth();
            if (this.mCheckBoxWidth > 0) {
                i7 = this.mCheckBoxWidth + this.mGapBetweenImageAndText;
            } else {
                i7 = 0;
            }
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                this.mWorkProfileIcon.layout((widthWithPadding3 - measuredWidth) - i7, i21, widthWithPadding3 - i7, this.mNameTextViewHeight + i21);
            } else {
                this.mWorkProfileIcon.layout(paddingLeft + i7, i21, paddingLeft + measuredWidth + i7, this.mNameTextViewHeight + i21);
            }
        } else {
            measuredWidth = 0;
        }
        if (isVisible(this.mNameTextView)) {
            int i22 = (this.mCheckBoxWidth > 0 ? this.mCheckBoxWidth + this.mGapBetweenImageAndText : 0) + measuredWidth;
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                this.mNameTextView.layout(paddingLeft, i21, widthWithPadding3 - i22, this.mNameTextViewHeight + i21);
            } else {
                this.mNameTextView.layout(i22 + paddingLeft, i21, widthWithPadding3, this.mNameTextViewHeight + i21);
            }
            this.mContactListItemViewEx.measureTextView(this.mNameTextView);
        }
        if (isVisible(this.mNameTextView) || isVisible(this.mWorkProfileIcon)) {
            i21 += this.mNameTextViewHeight;
        }
        if (zIsViewLayoutRtl) {
            if (isVisible(this.mPresenceIcon)) {
                int measuredWidth3 = this.mPresenceIcon.getMeasuredWidth();
                this.mPresenceIcon.layout(widthWithPadding3 - measuredWidth3, i21, widthWithPadding3, this.mStatusTextViewHeight + i21);
                i6 = widthWithPadding3 - (measuredWidth3 + this.mPresenceIconMargin);
            } else {
                i6 = widthWithPadding3;
            }
            if (isVisible(this.mStatusView)) {
                this.mStatusView.layout(paddingLeft, i21, i6, this.mStatusTextViewHeight + i21);
            }
        } else {
            if (isVisible(this.mPresenceIcon)) {
                int measuredWidth4 = this.mPresenceIcon.getMeasuredWidth();
                this.mPresenceIcon.layout(paddingLeft, i21, paddingLeft + measuredWidth4, this.mStatusTextViewHeight + i21);
                i5 = measuredWidth4 + this.mPresenceIconMargin + paddingLeft;
            } else {
                i5 = paddingLeft;
            }
            if (isVisible(this.mStatusView)) {
                this.mStatusView.layout(i5, i21, widthWithPadding3, this.mStatusTextViewHeight + i21);
            }
        }
        if (isVisible(this.mStatusView) || isVisible(this.mPresenceIcon)) {
            i21 += this.mStatusTextViewHeight;
        }
        if (isVisible(this.mPhoneticNameTextView)) {
            this.mPhoneticNameTextView.layout(paddingLeft, i21, widthWithPadding3, this.mPhoneticNameTextViewHeight + i21);
            i21 += this.mPhoneticNameTextViewHeight;
        }
        if (isVisible(this.mLabelView)) {
            if (!zIsViewLayoutRtl) {
                this.mLabelView.layout(paddingLeft, (this.mLabelAndDataViewMaxHeight + i21) - this.mLabelViewHeight, this.mLabelView.getMeasuredWidth() + paddingLeft, this.mLabelAndDataViewMaxHeight + i21);
                measuredWidth2 = this.mLabelView.getMeasuredWidth() + this.mGapBetweenLabelAndData + paddingLeft;
            } else {
                measuredWidth2 = this.mLabelView.getMeasuredWidth() + paddingLeft;
                this.mLabelView.layout(widthWithPadding3 - this.mLabelView.getMeasuredWidth(), (this.mLabelAndDataViewMaxHeight + i21) - this.mLabelViewHeight, widthWithPadding3, this.mLabelAndDataViewMaxHeight + i21);
                widthWithPadding3 -= this.mLabelView.getMeasuredWidth() + this.mGapBetweenLabelAndData;
            }
        } else {
            measuredWidth2 = paddingLeft;
        }
        if (isVisible(this.mDataView)) {
            int i23 = (this.mCheckBoxWidth > 0 ? this.mCheckBoxWidth + this.mGapBetweenImageAndText : 0) + measuredWidth;
            if (!zIsViewLayoutRtl) {
                this.mDataView.layout(measuredWidth2, (this.mLabelAndDataViewMaxHeight + i21) - this.mDataViewHeight, widthWithPadding3 - i23, this.mLabelAndDataViewMaxHeight + i21);
            } else {
                this.mDataView.layout(widthWithPadding3 - this.mDataView.getMeasuredWidth(), (this.mLabelAndDataViewMaxHeight + i21) - this.mDataViewHeight, widthWithPadding3, this.mLabelAndDataViewMaxHeight + i21);
            }
            this.mContactListItemViewEx.measureTextView(this.mDataView);
        }
        if (isVisible(this.mLabelView) || isVisible(this.mDataView)) {
            i21 += this.mLabelAndDataViewMaxHeight;
        }
        if (isVisible(this.mSnippetView)) {
            int i24 = measuredWidth + (this.mCheckBoxWidth > 0 ? this.mCheckBoxWidth + this.mGapBetweenImageAndText : 0);
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                this.mSnippetView.layout(paddingLeft, i21, widthWithPadding3 - i24, this.mSnippetTextViewHeight + i21);
            } else {
                this.mSnippetView.layout(paddingLeft + i24, i21, widthWithPadding3, this.mSnippetTextViewHeight + i21);
            }
        }
    }

    @Override
    public void adjustListItemSelectionBounds(Rect rect) {
        if (this.mAdjustSelectionBoundsEnabled) {
            rect.top += this.mBoundsWithoutHeader.top;
            rect.bottom = rect.top + this.mBoundsWithoutHeader.height();
            rect.left = this.mBoundsWithoutHeader.left;
            rect.right = this.mBoundsWithoutHeader.right;
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
        ViewGroup.LayoutParams layoutParamsGenerateDefaultLayoutParams = generateDefaultLayoutParams();
        layoutParamsGenerateDefaultLayoutParams.width = getDefaultPhotoViewSize();
        layoutParamsGenerateDefaultLayoutParams.height = layoutParamsGenerateDefaultLayoutParams.width;
        return layoutParamsGenerateDefaultLayoutParams;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mActivatedStateSupported) {
            this.mActivatedBackgroundDrawable.setState(getDrawableState());
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        return drawable == this.mActivatedBackgroundDrawable || super.verifyDrawable(drawable);
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

    public void setSectionHeader(String str) {
        if (str != null) {
            if (str.isEmpty()) {
                if (this.mHeaderView == null) {
                    addStarImageHeader();
                    return;
                } else if (this.mHeaderView instanceof TextView) {
                    removeView(this.mHeaderView);
                    addStarImageHeader();
                    return;
                } else {
                    this.mHeaderView.setVisibility(0);
                    return;
                }
            }
            if (this.mHeaderView == null) {
                addTextHeader(str);
                return;
            } else if (this.mHeaderView instanceof ImageView) {
                removeView(this.mHeaderView);
                addTextHeader(str);
                return;
            } else {
                updateHeaderText((TextView) this.mHeaderView, str);
                return;
            }
        }
        if (this.mHeaderView != null) {
            this.mHeaderView.setVisibility(8);
        }
    }

    private void addTextHeader(String str) {
        this.mHeaderView = new TextView(getContext());
        TextView textView = (TextView) this.mHeaderView;
        textView.setTextAppearance(getContext(), R.style.SectionHeaderStyle);
        textView.setGravity(1);
        updateHeaderText(textView, str);
        addView(textView);
    }

    private void updateHeaderText(TextView textView, String str) {
        setMarqueeText(textView, str);
        textView.setAllCaps(true);
        if ("…".equals(str)) {
            textView.setContentDescription(getContext().getString(R.string.description_no_name_header));
        } else {
            textView.setContentDescription(str);
        }
        textView.setVisibility(0);
    }

    private void addStarImageHeader() {
        this.mHeaderView = new ImageView(getContext());
        ImageView imageView = (ImageView) this.mHeaderView;
        imageView.setImageDrawable(getResources().getDrawable(R.drawable.quantum_ic_star_vd_theme_24, getContext().getTheme()));
        imageView.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.material_star_pink)));
        imageView.setContentDescription(getContext().getString(R.string.contactsFavoritesLabel));
        imageView.setVisibility(0);
        addView(imageView);
    }

    public void setIsSectionHeaderEnabled(boolean z) {
        this.mIsSectionHeaderEnabled = z;
    }

    public QuickContactBadge getQuickContact() {
        if (!this.mQuickContactEnabled) {
            throw new IllegalStateException("QuickContact is disabled for this view");
        }
        if (this.mQuickContact == null) {
            this.mQuickContact = new QuickContactBadge(getContext());
            if (CompatUtils.isLollipopCompatible()) {
                this.mQuickContact.setOverlay(null);
            }
            this.mQuickContact.setLayoutParams(getDefaultPhotoLayoutParams());
            if (this.mNameTextView != null) {
                this.mQuickContact.setContentDescription(getContext().getString(R.string.description_quick_contact_for, this.mNameTextView.getText()));
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

    public void removePhotoView(boolean z, boolean z2) {
        this.mPhotoViewWidthAndHeightAreReady = false;
        this.mKeepHorizontalPaddingForPhotoView = z;
        this.mKeepVerticalPaddingForPhotoView = z2;
        if (this.mPhotoView != null) {
            removeView(this.mPhotoView);
            this.mPhotoView = null;
        }
        if (this.mQuickContact != null) {
            removeView(this.mQuickContact);
            this.mQuickContact = null;
        }
    }

    public void setHighlightedPrefix(String str) {
        this.mHighlightedPrefix = str;
    }

    public void clearHighlightSequences() {
        this.mNameHighlightSequence.clear();
        this.mNumberHighlightSequence.clear();
        this.mHighlightedPrefix = null;
    }

    public void addNameHighlightSequence(int i, int i2) {
        this.mNameHighlightSequence.add(new HighlightSequence(i, i2));
    }

    public void addNumberHighlightSequence(int i, int i2) {
        this.mNumberHighlightSequence.add(new HighlightSequence(i, i2));
    }

    public TextView getNameTextView() {
        if (this.mNameTextView == null) {
            this.mNameTextView = new TextView(getContext());
            this.mNameTextView.setSingleLine(true);
            this.mNameTextView.setEllipsize(getTextEllipsis());
            this.mNameTextView.setTextColor(ResourcesCompat.getColorStateList(getResources(), R.color.contact_list_name_text_color, getContext().getTheme()));
            this.mNameTextView.setTextSize(0, this.mNameTextViewTextSize);
            this.mNameTextView.setActivated(isActivated());
            this.mNameTextView.setGravity(16);
            this.mNameTextView.setTextAlignment(5);
            this.mNameTextView.setId(R.id.cliv_name_textview);
            if (CompatUtils.isLollipopCompatible()) {
                this.mNameTextView.setElegantTextHeight(false);
            }
            addView(this.mNameTextView);
        }
        return this.mNameTextView;
    }

    public void setPhoneticName(char[] cArr, int i) {
        if (cArr == null || i == 0) {
            if (this.mPhoneticNameTextView != null) {
                this.mPhoneticNameTextView.setVisibility(8);
            }
        } else {
            getPhoneticNameTextView();
            setMarqueeText(this.mPhoneticNameTextView, cArr, i);
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
            this.mPhoneticNameTextView.setId(R.id.cliv_phoneticname_textview);
            addView(this.mPhoneticNameTextView);
        }
        return this.mPhoneticNameTextView;
    }

    public void setLabel(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            if (this.mLabelView != null) {
                this.mLabelView.setVisibility(8);
            }
        } else {
            getLabelView();
            setMarqueeText(this.mLabelView, charSequence);
            this.mLabelView.setVisibility(0);
        }
    }

    public TextView getLabelView() {
        if (this.mLabelView == null) {
            this.mLabelView = new TextView(getContext());
            this.mLabelView.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
            this.mLabelView.setSingleLine(true);
            this.mLabelView.setEllipsize(getTextEllipsis());
            this.mLabelView.setTextAppearance(getContext(), R.style.TextAppearanceSmall);
            if (this.mPhotoPosition == PhotoPosition.LEFT) {
                this.mLabelView.setAllCaps(true);
            } else {
                this.mLabelView.setTypeface(this.mLabelView.getTypeface(), 1);
            }
            this.mLabelView.setActivated(isActivated());
            this.mLabelView.setId(R.id.cliv_label_textview);
            this.mLabelView.setTextAlignment(5);
            addView(this.mLabelView);
        }
        return this.mLabelView;
    }

    public void setData(char[] cArr, int i) {
        if (cArr == null || i == 0) {
            if (this.mDataView != null) {
                this.mDataView.setVisibility(8);
            }
        } else {
            getDataView();
            setMarqueeText(this.mDataView, cArr, i);
            this.mDataView.setTextDirection(3);
            this.mDataView.setVisibility(0);
        }
    }

    public void setPhoneNumber(String str, String str2) {
        if (str == null) {
            if (this.mDataView != null) {
                this.mDataView.setVisibility(8);
                return;
            }
            return;
        }
        getDataView();
        SpannableString spannableString = new SpannableString(str);
        if (this.mNumberHighlightSequence.size() != 0) {
            HighlightSequence highlightSequence = this.mNumberHighlightSequence.get(0);
            this.mTextHighlighter.applyMaskingHighlight(spannableString, highlightSequence.start, highlightSequence.end);
        }
        setMarqueeText(this.mDataView, spannableString);
        this.mDataView.setVisibility(0);
        this.mDataView.setTextDirection(3);
        this.mDataView.setTextAlignment(5);
    }

    private void setMarqueeText(TextView textView, char[] cArr, int i) {
        if (getTextEllipsis() == TextUtils.TruncateAt.MARQUEE) {
            setMarqueeText(textView, new String(cArr, 0, i));
        } else {
            textView.setText(cArr, 0, i);
        }
    }

    private void setMarqueeText(TextView textView, CharSequence charSequence) {
        if (getTextEllipsis() == TextUtils.TruncateAt.MARQUEE) {
            SpannableString spannableString = new SpannableString(charSequence);
            spannableString.setSpan(TextUtils.TruncateAt.MARQUEE, 0, spannableString.length(), 33);
            textView.setText(spannableString);
            return;
        }
        textView.setText(charSequence);
    }

    public AppCompatCheckBox getCheckBox() {
        if (this.mCheckBox == null) {
            this.mCheckBox = new AppCompatCheckBox(getContext());
            this.mCheckBox.setFocusable(false);
            addView(this.mCheckBox);
        }
        return this.mCheckBox;
    }

    public AppCompatImageButton getDeleteImageButton(final MultiSelectEntryContactListAdapter.DeleteContactListener deleteContactListener, final int i) {
        if (this.mDeleteImageButton == null) {
            this.mDeleteImageButton = new AppCompatImageButton(getContext());
            this.mDeleteImageButton.setImageResource(R.drawable.quantum_ic_cancel_vd_theme_24);
            this.mDeleteImageButton.setScaleType(ImageView.ScaleType.CENTER);
            this.mDeleteImageButton.setBackgroundColor(0);
            this.mDeleteImageButton.setContentDescription(getResources().getString(R.string.description_delete_contact));
            if (CompatUtils.isLollipopCompatible()) {
                TypedValue typedValue = new TypedValue();
                getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true);
                this.mDeleteImageButton.setBackgroundResource(typedValue.resourceId);
            }
            addView(this.mDeleteImageButton);
        }
        this.mDeleteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (deleteContactListener != null) {
                    deleteContactListener.onContactDeleteClicked(i);
                }
            }
        });
        return this.mDeleteImageButton;
    }

    public TextView getDataView() {
        if (this.mDataView == null) {
            this.mDataView = new TextView(getContext());
            this.mDataView.setSingleLine(true);
            this.mDataView.setEllipsize(getTextEllipsis());
            this.mDataView.setTextAppearance(getContext(), R.style.TextAppearanceSmall);
            this.mDataView.setTextAlignment(5);
            this.mDataView.setActivated(isActivated());
            this.mDataView.setId(R.id.cliv_data_view);
            if (CompatUtils.isLollipopCompatible()) {
                this.mDataView.setElegantTextHeight(false);
            }
            this.mDataView.setTextAlignment(5);
            addView(this.mDataView);
        }
        return this.mDataView;
    }

    public void setSnippet(String str) {
        if (TextUtils.isEmpty(str)) {
            if (this.mSnippetView != null) {
                this.mSnippetView.setVisibility(8);
            }
        } else {
            this.mTextHighlighter.setPrefixText(getSnippetView(), str, this.mHighlightedPrefix);
            this.mSnippetView.setVisibility(0);
            if (ContactDisplayUtils.isPossiblePhoneNumber(str)) {
                this.mSnippetView.setContentDescription(PhoneNumberUtilsCompat.createTtsSpannable(str));
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

    public void setStatus(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            if (this.mStatusView != null) {
                this.mStatusView.setVisibility(8);
            }
        } else {
            getStatusView();
            setMarqueeText(this.mStatusView, charSequence);
            this.mStatusView.setVisibility(0);
        }
    }

    public void setPresence(Drawable drawable) {
        if (drawable != null) {
            if (this.mPresenceIcon == null) {
                this.mPresenceIcon = new ImageView(getContext());
                addView(this.mPresenceIcon);
            }
            this.mPresenceIcon.setImageDrawable(drawable);
            this.mPresenceIcon.setScaleType(ImageView.ScaleType.CENTER);
            this.mPresenceIcon.setVisibility(0);
            return;
        }
        if (this.mPresenceIcon != null) {
            this.mPresenceIcon.setVisibility(8);
        }
    }

    public void setWorkProfileIconEnabled(boolean z) {
        if (this.mWorkProfileIcon != null) {
            this.mWorkProfileIcon.setVisibility(z ? 0 : 8);
            return;
        }
        if (z) {
            this.mWorkProfileIcon = new ImageView(getContext());
            addView(this.mWorkProfileIcon);
            this.mWorkProfileIcon.setImageResource(R.drawable.ic_work_profile);
            this.mWorkProfileIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            this.mWorkProfileIcon.setVisibility(0);
        }
    }

    private TextUtils.TruncateAt getTextEllipsis() {
        return TextUtils.TruncateAt.MARQUEE;
    }

    public void showDisplayName(Cursor cursor, int i, int i2) {
        setDisplayName(cursor.getString(i));
        if (this.mQuickContact != null) {
            this.mQuickContact.setContentDescription(getContext().getString(R.string.description_quick_contact_for, this.mNameTextView.getText()));
        }
    }

    public void setDisplayName(CharSequence charSequence, boolean z) {
        if (!TextUtils.isEmpty(charSequence) && z) {
            clearHighlightSequences();
            addNameHighlightSequence(0, charSequence.length());
        }
        setDisplayName(charSequence);
    }

    public void setDisplayName(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            if (this.mHighlightedPrefix != null) {
                charSequence = this.mTextHighlighter.applyPrefixHighlight(charSequence, this.mHighlightedPrefix);
            } else if (this.mNameHighlightSequence.size() != 0) {
                SpannableString spannableString = new SpannableString(charSequence);
                for (HighlightSequence highlightSequence : this.mNameHighlightSequence) {
                    this.mTextHighlighter.applyMaskingHighlight(spannableString, highlightSequence.start, highlightSequence.end);
                }
                charSequence = spannableString;
            }
        } else {
            charSequence = this.mUnknownNameText;
        }
        setMarqueeText(getNameTextView(), charSequence);
        if (ContactDisplayUtils.isPossiblePhoneNumber(charSequence)) {
            this.mNameTextView.setTextDirection(3);
            this.mNameTextView.setContentDescription(PhoneNumberUtilsCompat.createTtsSpannable(charSequence.toString()));
        } else {
            this.mNameTextView.setContentDescription(charSequence.toString());
        }
    }

    public void hideCheckBox() {
        if (this.mCheckBox != null) {
            removeView(this.mCheckBox);
            this.mCheckBox = null;
        }
    }

    public void hideDeleteImageButton() {
        if (this.mDeleteImageButton != null) {
            removeView(this.mDeleteImageButton);
            this.mDeleteImageButton = null;
        }
    }

    public void hideDisplayName() {
        if (this.mNameTextView != null) {
            removeView(this.mNameTextView);
            this.mNameTextView = null;
        }
    }

    public void showPhoneticName(Cursor cursor, int i) {
        cursor.copyStringToBuffer(i, this.mPhoneticNameBuffer);
        int i2 = this.mPhoneticNameBuffer.sizeCopied;
        if (i2 != 0) {
            setPhoneticName(this.mPhoneticNameBuffer.data, i2);
        } else {
            setPhoneticName(null, 0);
        }
    }

    public void hidePhoneticName() {
        if (this.mPhoneticNameTextView != null) {
            removeView(this.mPhoneticNameTextView);
            this.mPhoneticNameTextView = null;
        }
    }

    public void showPresenceAndStatusMessage(Cursor cursor, int i, int i2) {
        int i3;
        Drawable presenceIcon;
        String statusString = null;
        if (!cursor.isNull(i)) {
            i3 = cursor.getInt(i);
            presenceIcon = ContactPresenceIconUtil.getPresenceIcon(getContext(), i3);
        } else {
            i3 = 0;
            presenceIcon = null;
        }
        setPresence(presenceIcon);
        if (i2 != 0 && !cursor.isNull(i2)) {
            statusString = cursor.getString(i2);
        }
        if (statusString == null && i3 != 0) {
            statusString = ContactStatusUtil.getStatusString(getContext(), i3);
        }
        setStatus(statusString);
    }

    public void showSnippet(Cursor cursor, String str, int i) {
        String string;
        String string2 = cursor.getString(i);
        if (string2 == null) {
            setSnippet(null);
            return;
        }
        if (cursor.getColumnIndex("display_name") >= 0) {
            string = cursor.getString(cursor.getColumnIndex("display_name"));
        } else {
            string = null;
        }
        if (string2.equals(string)) {
            setSnippet(null);
        } else {
            setSnippet(updateSnippet(string2, str, string));
        }
    }

    public void showSnippet(Cursor cursor, int i) {
        int iIndexOf;
        if (cursor.getColumnCount() <= i || !"snippet".equals(cursor.getColumnName(i))) {
            setSnippet(null);
            return;
        }
        String string = cursor.getString(i);
        Bundle extras = cursor.getExtras();
        if (extras.getBoolean("deferred_snippeting")) {
            String string2 = extras.getString("deferred_snippeting_query");
            int columnIndex = cursor.getColumnIndex("display_name");
            string = updateSnippet(string, string2, columnIndex >= 0 ? cursor.getString(columnIndex) : null);
        } else if (string != null) {
            int i2 = 0;
            int length = string.length();
            int iIndexOf2 = string.indexOf(91);
            if (iIndexOf2 != -1) {
                int iLastIndexOf = string.lastIndexOf(10, iIndexOf2);
                if (iLastIndexOf != -1) {
                    i2 = iLastIndexOf + 1;
                }
                int iLastIndexOf2 = string.lastIndexOf(93);
                if (iLastIndexOf2 != -1 && (iIndexOf = string.indexOf(10, iLastIndexOf2)) != -1) {
                    length = iIndexOf;
                }
                StringBuilder sb = new StringBuilder();
                while (i2 < length) {
                    char cCharAt = string.charAt(i2);
                    if (cCharAt != '[' && cCharAt != ']') {
                        sb.append(cCharAt);
                    }
                    i2++;
                }
                string = sb.toString();
            }
        } else {
            string = string;
        }
        setSnippet(string);
    }

    private String updateSnippet(String str, String str2, String str3) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return null;
        }
        String strCleanStartAndEndOfSearchQuery = SearchUtil.cleanStartAndEndOfSearchQuery(str2.toLowerCase());
        if (!TextUtils.isEmpty(str3)) {
            Iterator<String> it = split(str3.toLowerCase()).iterator();
            while (it.hasNext()) {
                if (it.next().startsWith(strCleanStartAndEndOfSearchQuery)) {
                    return null;
                }
            }
        }
        SearchUtil.MatchedLine matchedLineFindMatchingLine = SearchUtil.findMatchingLine(str, strCleanStartAndEndOfSearchQuery);
        if (matchedLineFindMatchingLine == null || matchedLineFindMatchingLine.line == null) {
            return null;
        }
        int integer = getResources().getInteger(R.integer.snippet_length_before_tokenize);
        if (matchedLineFindMatchingLine.line.length() > integer) {
            return snippetize(matchedLineFindMatchingLine.line, matchedLineFindMatchingLine.startIndex, integer);
        }
        return matchedLineFindMatchingLine.line;
    }

    private String snippetize(String str, int i, int i2) {
        int i3 = i;
        int i4 = i2;
        while (true) {
            if (i3 < str.length()) {
                if (Character.isLetterOrDigit(str.charAt(i3))) {
                    i4--;
                    i3++;
                } else {
                    i2 = i4;
                    break;
                }
            } else {
                i3 = i;
                break;
            }
        }
        int i5 = i;
        int i6 = i2;
        for (int i7 = i - 1; i7 > -1 && i2 > 0; i7--) {
            if (!Character.isLetterOrDigit(str.charAt(i7))) {
                i6 = i2;
                i5 = i7;
            }
            i2--;
        }
        int i8 = i3;
        while (i3 < str.length() && i6 > 0) {
            if (!Character.isLetterOrDigit(str.charAt(i3))) {
                i8 = i3;
            }
            i6--;
            i3++;
        }
        StringBuilder sb = new StringBuilder();
        if (i5 > 0) {
            sb.append("...");
        }
        sb.append(str.substring(i5, i8));
        if (i8 < str.length()) {
            sb.append("...");
        }
        return sb.toString();
    }

    private static List<String> split(String str) {
        Matcher matcher = SPLIT_PATTERN.matcher(str);
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        while (matcher.find()) {
            arrayListNewArrayList.add(matcher.group());
        }
        return arrayListNewArrayList;
    }

    public void showData(Cursor cursor, int i) {
        cursor.copyStringToBuffer(i, this.mDataBuffer);
        setData(this.mDataBuffer.data, this.mDataBuffer.sizeCopied);
    }

    public void setActivatedStateSupported(boolean z) {
        this.mActivatedStateSupported = z;
    }

    public void setAdjustSelectionBoundsEnabled(boolean z) {
        this.mAdjustSelectionBoundsEnabled = z;
    }

    @Override
    public void requestLayout() {
        forceLayout();
    }

    public void setPhotoPosition(PhotoPosition photoPosition) {
        this.mPhotoPosition = photoPosition;
    }

    public PhotoPosition getPhotoPosition() {
        return this.mPhotoPosition;
    }

    public void setDrawableResource(int i) {
        ImageView photoView = getPhotoView();
        photoView.setScaleType(ImageView.ScaleType.CENTER);
        Drawable drawable = ContextCompat.getDrawable(getContext(), i);
        int color = ContextCompat.getColor(getContext(), R.color.search_shortcut_icon_color);
        if (CompatUtils.isLollipopCompatible()) {
            photoView.setImageDrawable(drawable);
            photoView.setImageTintList(ColorStateList.valueOf(color));
        } else {
            Drawable drawableMutate = DrawableCompat.wrap(drawable).mutate();
            DrawableCompat.setTint(drawableMutate, color);
            photoView.setImageDrawable(drawableMutate);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (this.mBoundsWithoutHeader.contains((int) x, (int) y) || !pointIsInView(x, y)) {
            return super.onTouchEvent(motionEvent);
        }
        return true;
    }

    private final boolean pointIsInView(float f, float f2) {
        return f >= ((float) this.mLeftOffset) && f < ((float) this.mRightOffset) && f2 >= ContactPhotoManager.OFFSET_DEFAULT && f2 < ((float) (getBottom() - getTop()));
    }

    public void bindDataForCustomView(long j) {
        ExtensionManager.getInstance();
        ExtensionManager.getViewCustomExtension().getContactListItemViewCustom().addCustomView(j, this);
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().getContactListItemRcsView().addCustomView(j, this);
    }

    public void bindDataForCommonPresenceView(long j) {
        ExtensionManager.getInstance();
        ExtensionManager.getContactsCommonPresenceExtension().addVideoCallView(j, this);
    }

    public void setCheckable(boolean z) {
        this.mContactListItemViewEx.setCheckable(z);
    }
}
