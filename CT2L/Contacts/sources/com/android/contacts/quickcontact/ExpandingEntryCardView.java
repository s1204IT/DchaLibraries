package com.android.contacts.quickcontact;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.text.Spannable;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.ChangeScroll;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.contacts.R;
import java.util.ArrayList;
import java.util.List;

public class ExpandingEntryCardView extends CardView {
    private boolean mAllEntriesInflated;
    private ViewGroup mAnimationViewGroup;
    private LinearLayout mBadgeContainer;
    private final List<Integer> mBadgeIds;
    private final List<ImageView> mBadges;
    private CharSequence mCollapseButtonText;
    private int mCollapsedEntriesCount;
    private LinearLayout mContainer;
    private List<List<Entry>> mEntries;
    private LinearLayout mEntriesViewGroup;
    private List<List<View>> mEntryViews;
    private CharSequence mExpandButtonText;
    private final ImageView mExpandCollapseArrow;
    private View mExpandCollapseButton;
    private final View.OnClickListener mExpandCollapseButtonListener;
    private TextView mExpandCollapseTextView;
    private boolean mIsAlwaysExpanded;
    private boolean mIsExpanded;
    private ExpandingEntryCardViewListener mListener;
    private int mNumEntries;
    private View.OnClickListener mOnClickListener;
    private View.OnCreateContextMenuListener mOnCreateContextMenuListener;
    private List<View> mSeparators;
    private int mThemeColor;
    private ColorFilter mThemeColorFilter;
    private TextView mTitleTextView;

    public interface ExpandingEntryCardViewListener {
        void onCollapse(int i);

        void onExpand(int i);
    }

    public static final class Entry {
        private final String mAlternateContentDescription;
        private final Drawable mAlternateIcon;
        private final Intent mAlternateIntent;
        private final EntryContextMenuInfo mEntryContextMenuInfo;
        private final String mHeader;
        private final Drawable mIcon;
        private final int mIconResourceId;
        private final int mId;
        private final Intent mIntent;
        private final boolean mIsEditable;
        private Spannable mPrimaryContentDescription;
        private final boolean mShouldApplyColor;
        private final String mSubHeader;
        private final Drawable mSubHeaderIcon;
        private final String mText;
        private final Drawable mTextIcon;
        private final String mThirdContentDescription;
        private final Drawable mThirdIcon;
        private final Intent mThirdIntent;

        public Entry(int id, Drawable mainIcon, String header, String subHeader, Drawable subHeaderIcon, String text, Drawable textIcon, Spannable primaryContentDescription, Intent intent, Drawable alternateIcon, Intent alternateIntent, String alternateContentDescription, boolean shouldApplyColor, boolean isEditable, EntryContextMenuInfo entryContextMenuInfo, Drawable thirdIcon, Intent thirdIntent, String thirdContentDescription, int iconResourceId) {
            this.mId = id;
            this.mIcon = mainIcon;
            this.mHeader = header;
            this.mSubHeader = subHeader;
            this.mSubHeaderIcon = subHeaderIcon;
            this.mText = text;
            this.mTextIcon = textIcon;
            this.mPrimaryContentDescription = primaryContentDescription;
            this.mIntent = intent;
            this.mAlternateIcon = alternateIcon;
            this.mAlternateIntent = alternateIntent;
            this.mAlternateContentDescription = alternateContentDescription;
            this.mShouldApplyColor = shouldApplyColor;
            this.mIsEditable = isEditable;
            this.mEntryContextMenuInfo = entryContextMenuInfo;
            this.mThirdIcon = thirdIcon;
            this.mThirdIntent = thirdIntent;
            this.mThirdContentDescription = thirdContentDescription;
            this.mIconResourceId = iconResourceId;
        }

        Drawable getIcon() {
            return this.mIcon;
        }

        String getHeader() {
            return this.mHeader;
        }

        String getSubHeader() {
            return this.mSubHeader;
        }

        Drawable getSubHeaderIcon() {
            return this.mSubHeaderIcon;
        }

        public String getText() {
            return this.mText;
        }

        Drawable getTextIcon() {
            return this.mTextIcon;
        }

        Spannable getPrimaryContentDescription() {
            return this.mPrimaryContentDescription;
        }

        Intent getIntent() {
            return this.mIntent;
        }

        Drawable getAlternateIcon() {
            return this.mAlternateIcon;
        }

        Intent getAlternateIntent() {
            return this.mAlternateIntent;
        }

        String getAlternateContentDescription() {
            return this.mAlternateContentDescription;
        }

        boolean shouldApplyColor() {
            return this.mShouldApplyColor;
        }

        int getId() {
            return this.mId;
        }

        EntryContextMenuInfo getEntryContextMenuInfo() {
            return this.mEntryContextMenuInfo;
        }

        Drawable getThirdIcon() {
            return this.mThirdIcon;
        }

        Intent getThirdIntent() {
            return this.mThirdIntent;
        }

        String getThirdContentDescription() {
            return this.mThirdContentDescription;
        }

        int getIconResourceId() {
            return this.mIconResourceId;
        }
    }

    public ExpandingEntryCardView(Context context) {
        this(context, null);
    }

    public ExpandingEntryCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsExpanded = false;
        this.mNumEntries = 0;
        this.mAllEntriesInflated = false;
        this.mExpandCollapseButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ExpandingEntryCardView.this.mIsExpanded) {
                    ExpandingEntryCardView.this.collapse();
                } else {
                    ExpandingEntryCardView.this.expand();
                }
            }
        };
        LayoutInflater inflater = LayoutInflater.from(context);
        View expandingEntryCardView = inflater.inflate(R.layout.expanding_entry_card_view, this);
        this.mEntriesViewGroup = (LinearLayout) expandingEntryCardView.findViewById(R.id.content_area_linear_layout);
        this.mTitleTextView = (TextView) expandingEntryCardView.findViewById(R.id.title);
        this.mContainer = (LinearLayout) expandingEntryCardView.findViewById(R.id.container);
        this.mExpandCollapseButton = inflater.inflate(R.layout.quickcontact_expanding_entry_card_button, (ViewGroup) this, false);
        this.mExpandCollapseTextView = (TextView) this.mExpandCollapseButton.findViewById(R.id.text);
        this.mExpandCollapseArrow = (ImageView) this.mExpandCollapseButton.findViewById(R.id.arrow);
        this.mExpandCollapseButton.setOnClickListener(this.mExpandCollapseButtonListener);
        this.mBadgeContainer = (LinearLayout) this.mExpandCollapseButton.findViewById(R.id.badge_container);
        this.mBadges = new ArrayList();
        this.mBadgeIds = new ArrayList();
    }

    public void initialize(List<List<Entry>> entries, int numInitialVisibleEntries, boolean isExpanded, boolean isAlwaysExpanded, ExpandingEntryCardViewListener listener, ViewGroup animationViewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        this.mIsExpanded = isExpanded;
        this.mIsAlwaysExpanded = isAlwaysExpanded;
        this.mIsExpanded |= this.mIsAlwaysExpanded;
        this.mEntryViews = new ArrayList(entries.size());
        this.mEntries = entries;
        this.mNumEntries = 0;
        this.mAllEntriesInflated = false;
        for (List<Entry> entryList : this.mEntries) {
            this.mNumEntries += entryList.size();
            this.mEntryViews.add(new ArrayList());
        }
        this.mCollapsedEntriesCount = Math.min(numInitialVisibleEntries, this.mNumEntries);
        if (entries.size() > 1) {
            this.mSeparators = new ArrayList(entries.size() - 1);
        }
        this.mListener = listener;
        this.mAnimationViewGroup = animationViewGroup;
        if (this.mIsExpanded) {
            updateExpandCollapseButton(getCollapseButtonText(), 0L);
            inflateAllEntries(layoutInflater);
        } else {
            updateExpandCollapseButton(getExpandButtonText(), 0L);
            inflateInitialEntries(layoutInflater);
        }
        insertEntriesIntoViewGroup();
        applyColor();
    }

    public void setExpandButtonText(CharSequence expandButtonText) {
        this.mExpandButtonText = expandButtonText;
        if (this.mExpandCollapseTextView != null && !this.mIsExpanded) {
            this.mExpandCollapseTextView.setText(expandButtonText);
        }
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener) {
        this.mOnClickListener = listener;
    }

    @Override
    public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener listener) {
        this.mOnCreateContextMenuListener = listener;
    }

    private void insertEntriesIntoViewGroup() {
        View separator;
        View separator2;
        this.mEntriesViewGroup.removeAllViews();
        if (this.mIsExpanded) {
            for (int i = 0; i < this.mEntryViews.size(); i++) {
                List<View> viewList = this.mEntryViews.get(i);
                if (i > 0) {
                    if (this.mSeparators.size() <= i - 1) {
                        separator2 = generateSeparator(viewList.get(0));
                        this.mSeparators.add(separator2);
                    } else {
                        separator2 = this.mSeparators.get(i - 1);
                    }
                    this.mEntriesViewGroup.addView(separator2);
                }
                for (View view : viewList) {
                    addEntry(view);
                }
            }
        } else {
            int numInViewGroup = 0;
            int extraEntries = this.mCollapsedEntriesCount - this.mEntryViews.size();
            for (int i2 = 0; i2 < this.mEntryViews.size() && numInViewGroup < this.mCollapsedEntriesCount; i2++) {
                List<View> entryViewList = this.mEntryViews.get(i2);
                if (i2 > 0) {
                    if (this.mSeparators.size() <= i2 - 1) {
                        separator = generateSeparator(entryViewList.get(0));
                        this.mSeparators.add(separator);
                    } else {
                        separator = this.mSeparators.get(i2 - 1);
                    }
                    this.mEntriesViewGroup.addView(separator);
                }
                addEntry(entryViewList.get(0));
                numInViewGroup++;
                for (int j = 1; j < entryViewList.size() && numInViewGroup < this.mCollapsedEntriesCount && extraEntries > 0; j++) {
                    addEntry(entryViewList.get(j));
                    numInViewGroup++;
                    extraEntries--;
                }
            }
        }
        removeView(this.mExpandCollapseButton);
        if (this.mCollapsedEntriesCount < this.mNumEntries && this.mExpandCollapseButton.getParent() == null && !this.mIsAlwaysExpanded) {
            this.mContainer.addView(this.mExpandCollapseButton, -1);
        }
    }

    private void addEntry(View entry) {
        if (TextUtils.isEmpty(this.mTitleTextView.getText()) && this.mEntriesViewGroup.getChildCount() == 0) {
            entry.setPadding(entry.getPaddingLeft(), getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_item_padding_top) + getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_null_title_top_extra_padding), entry.getPaddingRight(), entry.getPaddingBottom());
        }
        this.mEntriesViewGroup.addView(entry);
    }

    private View generateSeparator(View entry) {
        View separator = new View(getContext());
        Resources res = getResources();
        separator.setBackgroundColor(res.getColor(R.color.divider_line_color_light));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, res.getDimensionPixelSize(R.dimen.divider_line_height));
        int marginStart = res.getDimensionPixelSize(R.dimen.expanding_entry_card_item_padding_start);
        ImageView entryIcon = (ImageView) entry.findViewById(R.id.icon);
        if (entryIcon.getVisibility() == 0) {
            int imageWidthAndMargin = res.getDimensionPixelSize(R.dimen.expanding_entry_card_item_icon_width) + res.getDimensionPixelSize(R.dimen.expanding_entry_card_item_image_spacing);
            marginStart += imageWidthAndMargin;
        }
        layoutParams.setMarginStart(marginStart);
        separator.setLayoutParams(layoutParams);
        return separator;
    }

    private CharSequence getExpandButtonText() {
        return !TextUtils.isEmpty(this.mExpandButtonText) ? this.mExpandButtonText : getResources().getText(R.string.expanding_entry_card_view_see_more);
    }

    private CharSequence getCollapseButtonText() {
        return !TextUtils.isEmpty(this.mCollapseButtonText) ? this.mCollapseButtonText : getResources().getText(R.string.expanding_entry_card_view_see_less);
    }

    private void inflateInitialEntries(LayoutInflater layoutInflater) {
        if (this.mCollapsedEntriesCount == this.mNumEntries) {
            inflateAllEntries(layoutInflater);
            return;
        }
        int numInflated = 0;
        int extraEntries = this.mCollapsedEntriesCount - this.mEntries.size();
        for (int i = 0; i < this.mEntries.size() && numInflated < this.mCollapsedEntriesCount; i++) {
            List<Entry> entryList = this.mEntries.get(i);
            List<View> entryViewList = this.mEntryViews.get(i);
            entryViewList.add(createEntryView(layoutInflater, entryList.get(0), 0));
            numInflated++;
            for (int j = 1; j < entryList.size() && numInflated < this.mCollapsedEntriesCount && extraEntries > 0; j++) {
                entryViewList.add(createEntryView(layoutInflater, entryList.get(j), 4));
                numInflated++;
                extraEntries--;
            }
        }
    }

    private void inflateAllEntries(LayoutInflater layoutInflater) {
        int iconVisibility;
        if (!this.mAllEntriesInflated) {
            for (int i = 0; i < this.mEntries.size(); i++) {
                List<Entry> entryList = this.mEntries.get(i);
                List<View> viewList = this.mEntryViews.get(i);
                for (int j = viewList.size(); j < entryList.size(); j++) {
                    Entry entry = entryList.get(j);
                    if (entry.getIcon() == null) {
                        iconVisibility = 8;
                    } else if (j == 0) {
                        iconVisibility = 0;
                    } else {
                        iconVisibility = 4;
                    }
                    viewList.add(createEntryView(layoutInflater, entry, iconVisibility));
                }
            }
            this.mAllEntriesInflated = true;
        }
    }

    public void setColorAndFilter(int color, ColorFilter colorFilter) {
        this.mThemeColor = color;
        this.mThemeColorFilter = colorFilter;
        applyColor();
    }

    public void setEntryHeaderColor(int color) {
        if (this.mEntries != null) {
            for (List<View> entryList : this.mEntryViews) {
                for (View entryView : entryList) {
                    TextView header = (TextView) entryView.findViewById(R.id.header);
                    if (header != null) {
                        header.setTextColor(color);
                    }
                }
            }
        }
    }

    public void applyColor() {
        Drawable icon;
        if (this.mThemeColor != 0 && this.mThemeColorFilter != null) {
            if (this.mTitleTextView != null) {
                this.mTitleTextView.setTextColor(this.mThemeColor);
            }
            if (this.mEntries != null) {
                for (List<Entry> entryList : this.mEntries) {
                    for (Entry entry : entryList) {
                        if (entry.shouldApplyColor() && (icon = entry.getIcon()) != null) {
                            icon.mutate();
                            icon.setColorFilter(this.mThemeColorFilter);
                        }
                        Drawable alternateIcon = entry.getAlternateIcon();
                        if (alternateIcon != null) {
                            alternateIcon.mutate();
                            alternateIcon.setColorFilter(this.mThemeColorFilter);
                        }
                        Drawable thirdIcon = entry.getThirdIcon();
                        if (thirdIcon != null) {
                            thirdIcon.mutate();
                            thirdIcon.setColorFilter(this.mThemeColorFilter);
                        }
                    }
                }
            }
            this.mExpandCollapseTextView.setTextColor(this.mThemeColor);
            this.mExpandCollapseArrow.setColorFilter(this.mThemeColorFilter);
        }
    }

    private View createEntryView(LayoutInflater layoutInflater, Entry entry, int iconVisibility) {
        EntryView view = (EntryView) layoutInflater.inflate(R.layout.expanding_entry_card_item, (ViewGroup) this, false);
        view.setContextMenuInfo(entry.getEntryContextMenuInfo());
        if (!TextUtils.isEmpty(entry.getPrimaryContentDescription())) {
            view.setContentDescription(entry.getPrimaryContentDescription());
        }
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setVisibility(iconVisibility);
        if (entry.getIcon() != null) {
            icon.setImageDrawable(entry.getIcon());
        }
        TextView header = (TextView) view.findViewById(R.id.header);
        if (!TextUtils.isEmpty(entry.getHeader())) {
            header.setText(entry.getHeader());
        } else {
            header.setVisibility(8);
        }
        TextView subHeader = (TextView) view.findViewById(R.id.sub_header);
        if (!TextUtils.isEmpty(entry.getSubHeader())) {
            subHeader.setText(entry.getSubHeader());
        } else {
            subHeader.setVisibility(8);
        }
        ImageView subHeaderIcon = (ImageView) view.findViewById(R.id.icon_sub_header);
        if (entry.getSubHeaderIcon() != null) {
            subHeaderIcon.setImageDrawable(entry.getSubHeaderIcon());
        } else {
            subHeaderIcon.setVisibility(8);
        }
        TextView text = (TextView) view.findViewById(R.id.text);
        if (!TextUtils.isEmpty(entry.getText())) {
            text.setText(entry.getText());
        } else {
            text.setVisibility(8);
        }
        ImageView textIcon = (ImageView) view.findViewById(R.id.icon_text);
        if (entry.getTextIcon() != null) {
            textIcon.setImageDrawable(entry.getTextIcon());
        } else {
            textIcon.setVisibility(8);
        }
        if (entry.getIntent() != null) {
            view.setOnClickListener(this.mOnClickListener);
            view.setTag(new EntryTag(entry.getId(), entry.getIntent()));
        }
        if (entry.getIntent() == null && entry.getEntryContextMenuInfo() == null) {
            view.setBackground(null);
        }
        if (header.getVisibility() == 0 && subHeader.getVisibility() == 8 && text.getVisibility() == 8) {
            RelativeLayout.LayoutParams headerLayoutParams = (RelativeLayout.LayoutParams) header.getLayoutParams();
            headerLayoutParams.topMargin = (int) getResources().getDimension(R.dimen.expanding_entry_card_item_header_only_margin_top);
            headerLayoutParams.bottomMargin += (int) getResources().getDimension(R.dimen.expanding_entry_card_item_header_only_margin_bottom);
            header.setLayoutParams(headerLayoutParams);
        }
        if (iconVisibility == 4 && (!TextUtils.isEmpty(entry.getSubHeader()) || !TextUtils.isEmpty(entry.getText()))) {
            view.setPaddingRelative(view.getPaddingStart(), getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_item_no_icon_margin_top), view.getPaddingEnd(), view.getPaddingBottom());
        } else if (iconVisibility == 4 && TextUtils.isEmpty(entry.getSubHeader()) && TextUtils.isEmpty(entry.getText())) {
            view.setPaddingRelative(view.getPaddingStart(), 0, view.getPaddingEnd(), view.getPaddingBottom());
        }
        ImageView alternateIcon = (ImageView) view.findViewById(R.id.icon_alternate);
        ImageView thirdIcon = (ImageView) view.findViewById(R.id.third_icon);
        if (entry.getAlternateIcon() != null && entry.getAlternateIntent() != null) {
            alternateIcon.setImageDrawable(entry.getAlternateIcon());
            alternateIcon.setOnClickListener(this.mOnClickListener);
            alternateIcon.setTag(new EntryTag(entry.getId(), entry.getAlternateIntent()));
            alternateIcon.setVisibility(0);
            alternateIcon.setContentDescription(entry.getAlternateContentDescription());
        }
        if (entry.getThirdIcon() != null && entry.getThirdIntent() != null) {
            thirdIcon.setImageDrawable(entry.getThirdIcon());
            thirdIcon.setOnClickListener(this.mOnClickListener);
            thirdIcon.setTag(new EntryTag(entry.getId(), entry.getThirdIntent()));
            thirdIcon.setVisibility(0);
            thirdIcon.setContentDescription(entry.getThirdContentDescription());
        }
        view.setOnTouchListener(new EntryTouchListener(view, alternateIcon, thirdIcon));
        view.setOnCreateContextMenuListener(this.mOnCreateContextMenuListener);
        return view;
    }

    private void updateExpandCollapseButton(CharSequence buttonText, long duration) {
        if (this.mIsExpanded) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(this.mExpandCollapseArrow, "rotation", 180.0f);
            animator.setDuration(duration);
            animator.start();
        } else {
            ObjectAnimator animator2 = ObjectAnimator.ofFloat(this.mExpandCollapseArrow, "rotation", 0.0f);
            animator2.setDuration(duration);
            animator2.start();
        }
        updateBadges();
        this.mExpandCollapseTextView.setText(buttonText);
    }

    private void updateBadges() {
        if (this.mIsExpanded) {
            this.mBadgeContainer.removeAllViews();
            return;
        }
        if (this.mBadges.size() < this.mEntries.size() - this.mCollapsedEntriesCount) {
            for (int i = this.mCollapsedEntriesCount; i < this.mEntries.size(); i++) {
                Drawable badgeDrawable = this.mEntries.get(i).get(0).getIcon();
                int badgeResourceId = this.mEntries.get(i).get(0).getIconResourceId();
                if ((badgeResourceId == 0 || !this.mBadgeIds.contains(Integer.valueOf(badgeResourceId))) && badgeDrawable != null) {
                    ImageView badgeView = new ImageView(getContext());
                    LinearLayout.LayoutParams badgeViewParams = new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.expanding_entry_card_item_icon_width), (int) getResources().getDimension(R.dimen.expanding_entry_card_item_icon_height));
                    badgeViewParams.setMarginEnd((int) getResources().getDimension(R.dimen.expanding_entry_card_badge_separator_margin));
                    badgeView.setLayoutParams(badgeViewParams);
                    badgeView.setImageDrawable(badgeDrawable);
                    this.mBadges.add(badgeView);
                    this.mBadgeIds.add(Integer.valueOf(badgeResourceId));
                }
            }
        }
        this.mBadgeContainer.removeAllViews();
        for (ImageView badge : this.mBadges) {
            this.mBadgeContainer.addView(badge);
        }
    }

    private void expand() {
        ChangeBounds boundsTransition = new ChangeBounds();
        boundsTransition.setDuration(300L);
        Fade fadeIn = new Fade(1);
        fadeIn.setDuration(200L);
        fadeIn.setStartDelay(100L);
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(boundsTransition);
        transitionSet.addTransition(fadeIn);
        transitionSet.excludeTarget(R.id.text, true);
        ViewGroup transitionViewContainer = this.mAnimationViewGroup == null ? this : this.mAnimationViewGroup;
        transitionSet.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                ExpandingEntryCardView.this.mListener.onExpand(0);
            }

            @Override
            public void onTransitionEnd(Transition transition) {
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });
        TransitionManager.beginDelayedTransition(transitionViewContainer, transitionSet);
        this.mIsExpanded = true;
        inflateAllEntries(LayoutInflater.from(getContext()));
        insertEntriesIntoViewGroup();
        updateExpandCollapseButton(getCollapseButtonText(), 300L);
    }

    private void collapse() {
        final int startingHeight = this.mEntriesViewGroup.getMeasuredHeight();
        this.mIsExpanded = false;
        updateExpandCollapseButton(getExpandButtonText(), 300L);
        ChangeBounds boundsTransition = new ChangeBounds();
        boundsTransition.setDuration(300L);
        ChangeScroll scrollTransition = new ChangeScroll();
        scrollTransition.setDuration(300L);
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(boundsTransition);
        transitionSet.addTransition(scrollTransition);
        transitionSet.excludeTarget(R.id.text, true);
        ViewGroup transitionViewContainer = this.mAnimationViewGroup == null ? this : this.mAnimationViewGroup;
        boundsTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                int finishingHeight = ExpandingEntryCardView.this.mEntriesViewGroup.getMeasuredHeight();
                ExpandingEntryCardView.this.mListener.onCollapse(startingHeight - finishingHeight);
            }

            @Override
            public void onTransitionEnd(Transition transition) {
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });
        TransitionManager.beginDelayedTransition(transitionViewContainer, transitionSet);
        insertEntriesIntoViewGroup();
    }

    public boolean isExpanded() {
        return this.mIsExpanded;
    }

    public void setTitle(String title) {
        if (this.mTitleTextView == null) {
            Log.e("ExpandingEntryCardView", "mTitleTextView is null");
        }
        this.mTitleTextView.setText(title);
        this.mTitleTextView.setVisibility(TextUtils.isEmpty(title) ? 8 : 0);
        findViewById(R.id.title_separator).setVisibility(TextUtils.isEmpty(title) ? 8 : 0);
        if (!TextUtils.isEmpty(title) && this.mEntriesViewGroup.getChildCount() > 0) {
            View firstEntry = this.mEntriesViewGroup.getChildAt(0);
            firstEntry.setPadding(firstEntry.getPaddingLeft(), getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_item_padding_top), firstEntry.getPaddingRight(), firstEntry.getPaddingBottom());
        } else if (!TextUtils.isEmpty(title) && this.mEntriesViewGroup.getChildCount() > 0) {
            View firstEntry2 = this.mEntriesViewGroup.getChildAt(0);
            firstEntry2.setPadding(firstEntry2.getPaddingLeft(), getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_item_padding_top) + getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_null_title_top_extra_padding), firstEntry2.getPaddingRight(), firstEntry2.getPaddingBottom());
        }
    }

    public boolean shouldShow() {
        return this.mEntries != null && this.mEntries.size() > 0;
    }

    public static final class EntryView extends RelativeLayout {
        private EntryContextMenuInfo mEntryContextMenuInfo;

        public EntryView(Context context) {
            super(context);
        }

        public EntryView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setContextMenuInfo(EntryContextMenuInfo info) {
            this.mEntryContextMenuInfo = info;
        }

        @Override
        protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
            return this.mEntryContextMenuInfo;
        }
    }

    public static final class EntryContextMenuInfo implements ContextMenu.ContextMenuInfo {
        private final String mCopyLabel;
        private final String mCopyText;
        private final long mId;
        private final boolean mIsSuperPrimary;
        private final String mMimeType;

        public EntryContextMenuInfo(String copyText, String copyLabel, String mimeType, long id, boolean isSuperPrimary) {
            this.mCopyText = copyText;
            this.mCopyLabel = copyLabel;
            this.mMimeType = mimeType;
            this.mId = id;
            this.mIsSuperPrimary = isSuperPrimary;
        }

        public String getCopyText() {
            return this.mCopyText;
        }

        public String getCopyLabel() {
            return this.mCopyLabel;
        }

        public String getMimeType() {
            return this.mMimeType;
        }

        public long getId() {
            return this.mId;
        }

        public boolean isSuperPrimary() {
            return this.mIsSuperPrimary;
        }
    }

    static final class EntryTag {
        private final int mId;
        private final Intent mIntent;

        public EntryTag(int id, Intent intent) {
            this.mId = id;
            this.mIntent = intent;
        }

        public int getId() {
            return this.mId;
        }

        public Intent getIntent() {
            return this.mIntent;
        }
    }

    private static final class EntryTouchListener implements View.OnTouchListener {
        private final ImageView mAlternateIcon;
        private final View mEntry;
        private int mSlop;
        private final ImageView mThirdIcon;
        private View mTouchedView;

        public EntryTouchListener(View entry, ImageView alternateIcon, ImageView thirdIcon) {
            this.mEntry = entry;
            this.mAlternateIcon = alternateIcon;
            this.mThirdIcon = thirdIcon;
            this.mSlop = ViewConfiguration.get(entry.getContext()).getScaledTouchSlop();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            View touchedView = this.mTouchedView;
            boolean sendToTouched = false;
            boolean hit = true;
            switch (event.getAction()) {
                case 0:
                    if (hitThirdIcon(event)) {
                        this.mTouchedView = this.mThirdIcon;
                        sendToTouched = true;
                    } else if (hitAlternateIcon(event)) {
                        this.mTouchedView = this.mAlternateIcon;
                        sendToTouched = true;
                    } else {
                        this.mTouchedView = this.mEntry;
                        sendToTouched = false;
                    }
                    touchedView = this.mTouchedView;
                    break;
                case 1:
                case 2:
                    sendToTouched = (this.mTouchedView == null || this.mTouchedView == this.mEntry) ? false : true;
                    if (sendToTouched) {
                        Rect slopBounds = new Rect();
                        touchedView.getHitRect(slopBounds);
                        slopBounds.inset(-this.mSlop, -this.mSlop);
                        if (!slopBounds.contains((int) event.getX(), (int) event.getY())) {
                            hit = false;
                        }
                    }
                    break;
                case 3:
                    sendToTouched = (this.mTouchedView == null || this.mTouchedView == this.mEntry) ? false : true;
                    this.mTouchedView = null;
                    break;
            }
            if (!sendToTouched) {
                return false;
            }
            if (hit) {
                event.setLocation(touchedView.getWidth() / 2, touchedView.getHeight() / 2);
            } else {
                event.setLocation(-(this.mSlop * 2), -(this.mSlop * 2));
            }
            boolean handled = touchedView.dispatchTouchEvent(event);
            return handled;
        }

        private boolean hitThirdIcon(MotionEvent event) {
            return this.mEntry.isLayoutRtl() ? this.mThirdIcon.getVisibility() == 0 && event.getX() < ((float) this.mThirdIcon.getRight()) : this.mThirdIcon.getVisibility() == 0 && event.getX() > ((float) this.mThirdIcon.getLeft());
        }

        private boolean hitAlternateIcon(MotionEvent event) {
            RelativeLayout.LayoutParams alternateIconParams = (RelativeLayout.LayoutParams) this.mAlternateIcon.getLayoutParams();
            return this.mEntry.isLayoutRtl() ? this.mAlternateIcon.getVisibility() == 0 && event.getX() < ((float) (this.mAlternateIcon.getRight() + alternateIconParams.rightMargin)) : this.mAlternateIcon.getVisibility() == 0 && event.getX() > ((float) (this.mAlternateIcon.getLeft() - alternateIconParams.leftMargin));
        }
    }
}
